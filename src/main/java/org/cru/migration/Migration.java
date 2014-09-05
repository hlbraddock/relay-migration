package org.cru.migration;

import com.google.common.collect.Sets;
import org.cru.migration.dao.CssDao;
import org.cru.migration.dao.DaoFactory;
import org.cru.migration.dao.PSHRDao;
import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.domain.PSHRStaff;

import org.cru.migration.domain.RelayUser;
import org.cru.migration.exception.MoreThanOneUserFoundException;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.ldap.RelayLdap;
import org.cru.migration.ldap.TheKeyLdap;
import org.cru.migration.support.FileHelper;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;

import javax.naming.NamingException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.Set;

/**
 * TODO
 * Get Relay account only if it's been logged into.
 * Get Donors Relay Accounts
 * Get Last Logged in time from audit trail
 */
public class Migration
{
	private MigrationProperties migrationProperties;
	private RelayLdap relayLdap;
	private CssDao cssDao;

	public Migration() throws Exception
	{
		migrationProperties = new MigrationProperties();

		relayLdap = new RelayLdap(migrationProperties);

		cssDao = DaoFactory.getCssDao(new MigrationProperties());
	}

	/**
	 * Create Relay system account entries in the Key
	 *
	 * @throws Exception
	 */
	public void createSystemEntries() throws Exception
	{
		TheKeyLdap theKeyLdap = new TheKeyLdap(migrationProperties);

		theKeyLdap.createSystemEntries();
	}

	public Set<RelayUser> getAuthoritativeRelayUsers() throws Exception
	{
		// get US Staff Relay Users
		Set<RelayUser> usStaffRelayUsers = getUSStaffRelayUsers();

		// get Google Relay Users
		Set<RelayUser> googleRelayUsers = getGoogleRelayUsers();

//		if(usStaffRelayUsers != null)
//			return usStaffRelayUsers;

		// compare us staff and google users
		usStaffGoogleComparison(usStaffRelayUsers, googleRelayUsers);

		// build set of authoritative relay users
		Set<RelayUser> authoritativeRelayUsers = Sets.newHashSet();
		authoritativeRelayUsers.addAll(usStaffRelayUsers);
		authoritativeRelayUsers.addAll(googleRelayUsers);

		Output.println("U.S. staff and google relay users size is " + authoritativeRelayUsers.size());
		Output.logRelayUser(googleRelayUsers,
				FileHelper.getFile(migrationProperties.getNonNullProperty("googleAndUSStaffRelayUsersLogFile")));

		// set Relay User passwords
		boolean setPasswords = true;
		if(setPasswords)
		{
			setRelayUserPasswords(authoritativeRelayUsers);
		}

		return authoritativeRelayUsers;
	}

	private void setRelayUserPasswords(Set<RelayUser> relayUsers) throws IOException
	{
		Output.println("Set Relay user passwords");

		Output.println("Relay user size is " + relayUsers.size());

		Set<CssRelayUser> cssRelayUsers = cssDao.getCssRelayUsers(RelayUser.getSsoguids(relayUsers));

		Output.println("CSS relay users size is " + cssRelayUsers.size());

		Output.println("Setting relay users passwords ...");

		RelayUser relayUser;
		Set<RelayUser> relayUsersWithPasswordSet = Sets.newHashSet();
		for(CssRelayUser cssRelayUser : cssRelayUsers)
		{
			relayUser = RelayUser.havingSsoguid(relayUsers, cssRelayUser.getSsoguid());

			if(relayUser != null)
			{
				relayUser.setPassword(cssRelayUser.getPassword());
				relayUsersWithPasswordSet.add(relayUser);
			}
		}

		Output.println("Done setting relay users passwords.");

		Set<RelayUser> relayUsersWithoutPasswordSet = Sets.newHashSet();
		relayUsersWithoutPasswordSet.addAll(relayUsers);
		relayUsersWithoutPasswordSet.removeAll(relayUsersWithPasswordSet);

		Output.println("Relay users with password set size " + relayUsersWithPasswordSet.size());
		Output.println("Relay users without password set size " + relayUsersWithoutPasswordSet.size());
		Output.logRelayUser(relayUsersWithPasswordSet,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersWithPasswordSet")));
		Output.logRelayUser(relayUsersWithoutPasswordSet,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersWithoutPasswordSet")));
	}

	public Set<RelayUser> getUSStaffRelayUsers() throws Exception
	{
		Output.println("Getting staff from PSHR ...");
		Set<PSHRStaff> pshrUSStaff = getPshrUSStaff();
		Output.println("PSHR staff count " + pshrUSStaff.size());
		Output.logPSHRStaff(pshrUSStaff,
				FileHelper.getFile(migrationProperties.getNonNullProperty("usStaffLogFile")));

		Output.println("Getting staff from Relay ...");
		Set<PSHRStaff> notFoundInRelay = Sets.newHashSet();
		Set<PSHRStaff> moreThanOneFoundWithEmployeeId = Sets.newHashSet();
		Set<RelayUser> duplicateRelayUsers = Sets.newHashSet();
		Set<RelayUser> relayUsers = getRelayUsersFromPshrSet(pshrUSStaff, notFoundInRelay,
				moreThanOneFoundWithEmployeeId, duplicateRelayUsers);

		Output.println("Staff Relay user count " + relayUsers.size());
		Output.println("Not found in Relay user count " + notFoundInRelay.size());
		Output.println("More than one found with employee id user count " + moreThanOneFoundWithEmployeeId.size());
		Output.println("Duplicate relay user count " + duplicateRelayUsers.size());

		Output.logPSHRStaff(moreThanOneFoundWithEmployeeId, FileHelper.getFile(migrationProperties.getNonNullProperty
				("moreThanOneRelayUserWithEmployeeId")));

		// log US Staff Relay Users
		Output.println("U.S. staff relay users size is " + relayUsers.size());
		Output.logRelayUser(relayUsers,
				FileHelper.getFile(migrationProperties.getNonNullProperty("usStaffRelayUsersLogFile")));

		return relayUsers;
	}

	/**
	 * Google non U.S. staff users fall into at least two categories:
	 * 	1. U.S. staff who have two (or more) Relay accounts, one of which has their employee id, and another,
	 * 	namely the Google non U.S. staff one, which is provisioned as their Google account
	 * 	2. National Staff
	 */
	private void usStaffGoogleComparison(Set<RelayUser> usStaffRelayUsers, Set<RelayUser> googleRelayUsers)
			throws IOException
	{
		Set<RelayUser> usStaffNotInGoogle = Sets.newHashSet();
		usStaffNotInGoogle.addAll(usStaffRelayUsers);
		usStaffNotInGoogle.removeAll(googleRelayUsers);

		Set<RelayUser> usStaffInGoogle = Sets.newHashSet();
		usStaffInGoogle.addAll(usStaffRelayUsers);
		usStaffInGoogle.removeAll(usStaffNotInGoogle);

		Output.println("US Staff size is " + usStaffRelayUsers.size());
		Output.println("US Staff in google size is " + usStaffInGoogle.size());
		Output.println("US Staff not in google size is " + usStaffNotInGoogle.size());

		Output.logRelayUser(usStaffInGoogle,
				FileHelper.getFile(migrationProperties.getNonNullProperty("usStaffInGoogleRelayUsersLogFile")));
		Output.logRelayUser(usStaffNotInGoogle,
				FileHelper.getFile(migrationProperties.getNonNullProperty
						("usStaffNotInGoogleRelayUsersLogFile")));

		Set<RelayUser> googleUserNotUSStaff = Sets.newHashSet();
		googleUserNotUSStaff.addAll(googleRelayUsers);
		googleUserNotUSStaff.removeAll(usStaffRelayUsers);

		Set<RelayUser> googleUserUSStaff = Sets.newHashSet();
		googleUserUSStaff.addAll(googleRelayUsers);
		googleUserUSStaff.removeAll(googleUserNotUSStaff);

		Set<RelayUser> googleUserNotUSStaffHavingEmployeeId =
				RelayUser.getRelayUsersHavingEmployeeId(googleUserNotUSStaff);
		Set<RelayUser> googleUserNotUSStaffNotHavingEmployeeId =
				RelayUser.getRelayUsersNotHavingEmployeeId(googleUserNotUSStaff);

		Output.println("Google size is " + googleRelayUsers.size());
		Output.println("Google non US staff size is " + googleUserNotUSStaff.size());
		Output.println("Google non US Staff size having employee id is " + googleUserNotUSStaffHavingEmployeeId.size());
		Output.println("Google non US Staff size not having employee id is " +
				googleUserNotUSStaffNotHavingEmployeeId.size());
		Output.println("Google US Staff size is " + googleUserUSStaff.size());

		Output.logRelayUser(googleUserNotUSStaff,
				FileHelper.getFile(migrationProperties.getNonNullProperty
						("googleNotUSStaffRelayUsersLogFile")));
		Output.logRelayUser(googleUserNotUSStaffHavingEmployeeId,
				FileHelper.getFile(migrationProperties.getNonNullProperty
						("googleNotUSStaffHavingEmployeeIdRelayUsersLogFile")));
		Output.logRelayUser(googleUserNotUSStaffNotHavingEmployeeId,
				FileHelper.getFile(migrationProperties.getNonNullProperty
						("googleNotUSStaffNotHavingEmployeeIdRelayUsersLogFile")));
	}

	private Set<RelayUser> getGoogleRelayUsers() throws NamingException, UserNotFoundException,
		MoreThanOneUserFoundException, IOException
	{
		Set<String> members = relayLdap.getGroupMembers(
				migrationProperties.getNonNullProperty("relayGoogleGroupsRoot"),
				migrationProperties.getNonNullProperty("relayGoogleGroupsMailNode"));

		Output.println("Google set members size is " + members.size());

		Set<RelayUser> relayUsers = getRelayUsersFromListOfDistinguishedNames(members);

		Output.println("Google relay users size is " + relayUsers.size());

		Output.logRelayUser(relayUsers,
				FileHelper.getFile(migrationProperties.getNonNullProperty("googleRelayUsersLogFile")));

		return relayUsers;
	}

	private Set<RelayUser> getRelayUsersFromPshrSet(Set<PSHRStaff> pshrStaffList,
													Set<PSHRStaff> notFoundInRelay,
													Set<PSHRStaff> moreThanOneFoundWithEmployeeId,
													Set<RelayUser> duplicateRelayUsers)
	{
		Set<RelayUser> relayUsers = Sets.newHashSet();

		int counter = 0;
		for (PSHRStaff pshrStaff : pshrStaffList)
		{
			try
			{
				RelayUser relayUser = relayLdap.getRelayUserFromEmployeeId(pshrStaff.getEmployeeId());
				int size = relayUsers.size();
				relayUsers.add(relayUser);
				if(relayUsers.size() == size)
				{
					duplicateRelayUsers.add(relayUser);
				}
			}
			catch (UserNotFoundException e)
			{
				notFoundInRelay.add(pshrStaff);
			}
			catch (MoreThanOneUserFoundException e)
			{
				moreThanOneFoundWithEmployeeId.add(pshrStaff);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			if (counter++ % 1000 == 0)
			{
				Output.println("Getting staff from Relay count is " + relayUsers.size() + " of total PSHR staff "
						+ counter);
			}
		}

		return relayUsers;
	}

	private Set<RelayUser> getRelayUsersFromListOfDistinguishedNames(Set<String> entries)
	{
		Set<RelayUser> relayUsers = Sets.newHashSet();

		int counter = 0;
		for (String entry : entries)
		{
			try
			{
				RelayUser relayUser = relayLdap.getRelayUserFromDn(entry);
				relayUsers.add(relayUser);
			}
			catch (UserNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (MoreThanOneUserFoundException e)
			{
				e.printStackTrace();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			if (counter++ % 1000 == 0)
			{
				Output.println("Getting users from Relay count is " + relayUsers.size() + " of total "
						+ counter);
			}
		}

		return relayUsers;
	}

	private Set<PSHRStaff> getPshrUSStaff() throws IOException, PropertyVetoException
	{
		PSHRDao pshrDao = DaoFactory.getPshrDao(migrationProperties);

		return pshrDao.getAllUSStaff();
	}

	public void test()
	{
	}

	enum Action
	{
		SystemEntries, USStaff, GoogleUsers, USStaffAndGoogleUsers, Test
	}

	public static void main(String[] args) throws Exception
	{
		Migration migration = new Migration();

		try
		{
			Action action = Action.USStaffAndGoogleUsers;

			if (action.equals(Action.SystemEntries))
			{
				migration.createSystemEntries();
			}
			else if (action.equals(Action.USStaff))
			{
				migration.getUSStaffRelayUsers();
			}
			else if (action.equals(Action.GoogleUsers))
			{
				migration.getGoogleRelayUsers();
			}
			else if (action.equals(Action.USStaffAndGoogleUsers))
			{
				migration.getAuthoritativeRelayUsers();
			}
			else if (action.equals(Action.Test))
			{
				migration.test();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

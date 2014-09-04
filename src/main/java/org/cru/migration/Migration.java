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
		setRelayUserPasswords(authoritativeRelayUsers);

		return authoritativeRelayUsers;
	}

	private void setRelayUserPasswords(Set<RelayUser> relayUsers)
	{
		Output.println("Set Relay user passwords");

		Output.println("Relay user size is " + relayUsers.size());

		Set<CssRelayUser> cssRelayUsers = cssDao.getCssRelayUsers(RelayUser.getSsoguids(relayUsers));

		Output.println("CSS relay users size is " + cssRelayUsers.size());

		Output.println("Setting relay users passwords ...");

		RelayUser relayUser;
		for(CssRelayUser cssRelayUser : cssRelayUsers)
		{
			relayUser = RelayUser.getRelayUserHavingSsoguid(relayUsers, cssRelayUser.getSsoguid());

			if(relayUser != null)
			{
				relayUser.setPassword(cssRelayUser.getPassword());
			}
		}

		Output.println("Done setting relay users passwords ...");
	}

	public Set<RelayUser> getUSStaffRelayUsers() throws Exception
	{
		Output.println("Getting staff from PSHR ...");
		Set<PSHRStaff> pshrUSStaff = getPshrUSStaff();
		Output.println("PSHR staff count " + pshrUSStaff.size());

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

	private void usStaffGoogleComparison(Set<RelayUser> usStaffRelayUsers, Set<RelayUser> googleRelayUsers)
			throws IOException
	{
		Set<RelayUser> nonGoogleUSStaff = Sets.newHashSet();
		nonGoogleUSStaff.addAll(usStaffRelayUsers);
		nonGoogleUSStaff.removeAll(googleRelayUsers);

		Output.println("Non Google US Staff size is " + nonGoogleUSStaff.size());
		Output.logRelayUser(nonGoogleUSStaff,
				FileHelper.getFile(migrationProperties.getNonNullProperty("nonGoogleUSStaffRelayUsersLogFile")));

		Set<RelayUser> nonUSStaffGoogle = Sets.newHashSet();
		nonUSStaffGoogle.addAll(googleRelayUsers);
		nonUSStaffGoogle.removeAll(usStaffRelayUsers);

		Output.println("Non US Staff Google users size is " + nonUSStaffGoogle.size());
		Output.logRelayUser(nonGoogleUSStaff,
				FileHelper.getFile(migrationProperties.getNonNullProperty("nonUSStaffGoogleRelayUsersLogFile")));
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

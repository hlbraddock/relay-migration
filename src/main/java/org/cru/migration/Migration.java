package org.cru.migration;

import com.google.common.collect.Sets;
import org.cru.migration.dao.CasAuditDao;
import org.cru.migration.dao.CssDao;
import org.cru.migration.dao.DaoFactory;
import org.cru.migration.dao.PSHRDao;
import org.cru.migration.domain.CasAuditUser;
import org.cru.migration.domain.PSHRStaff;

import org.cru.migration.domain.RelayUser;
import org.cru.migration.exception.MoreThanOneUserFoundException;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.ldap.RelayLdap;
import org.cru.migration.ldap.TheKeyLdap;
import org.cru.migration.service.RelayUserService;
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
 */
public class Migration
{
	private MigrationProperties migrationProperties;
	private RelayLdap relayLdap;
	private CasAuditDao casAuditDao;
	private RelayUserService relayUserService;

	public Migration() throws Exception
	{
		migrationProperties = new MigrationProperties();

		relayLdap = new RelayLdap(migrationProperties);

		CssDao cssDao = DaoFactory.getCssDao(new MigrationProperties());

		relayUserService = new RelayUserService();
		relayUserService.setCssDao(cssDao);
		relayUserService.setRelayLdap(relayLdap);

		casAuditDao = DaoFactory.getCasAuditDao(new MigrationProperties());
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

//		if(usStaffRelayUsers != null)
//			return usStaffRelayUsers;

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
		boolean setPasswords = true;
		if(setPasswords)
		{
			relayUserService.setPasswords(authoritativeRelayUsers);
		}

		setRelayLastLogonTimestamp(usStaffRelayUsers);

		return authoritativeRelayUsers;
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
		Set<RelayUser> relayUsers = relayUserService.getFromPshrSet(pshrUSStaff, notFoundInRelay,
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

	private void setRelayLastLogonTimestamp(Set<RelayUser> relayUsers)
	{
		Output.println("Setting relay last logon timestamp (from audit) ... for relay user set size " + relayUsers.size());
		CasAuditUser casAuditUser;
		int count = 0;
		for(RelayUser relayUser : relayUsers)
		{
			if(count++ % 1000 == 0)
			{
				Output.println("Set " + count + " relay users.");
			}
			casAuditUser = casAuditDao.getCasAuditUser(relayUser.getUsername());
			if(casAuditUser != null)
			{
				if(casAuditUser.getDate() != null)
				{
					relayUser.setLastLogonTimestamp(casAuditUser.getDate());
				}
			}
		}
		Output.println("Setting relay last logon timestamp (from audit) complete.");
		Output.println("Number of relay users with audit last logon time stamp " + count);
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

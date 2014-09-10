package org.cru.migration;

import com.google.common.collect.Sets;
import org.cru.migration.dao.CasAuditDao;
import org.cru.migration.dao.CssDao;
import org.cru.migration.dao.DaoFactory;
import org.cru.migration.domain.PSHRStaff;

import org.cru.migration.domain.RelayUser;
import org.cru.migration.exception.MoreThanOneUserFoundException;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.ldap.RelayLdap;
import org.cru.migration.ldap.TheKeyLdap;
import org.cru.migration.service.PSHRService;
import org.cru.migration.service.RelayUserService;
import org.cru.migration.support.FileHelper;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;
import org.joda.time.DateTime;

import javax.naming.NamingException;
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
	private RelayUserService relayUserService;
	private PSHRService pshrService;

	public Migration() throws Exception
	{
		migrationProperties = new MigrationProperties();

		relayLdap = new RelayLdap(migrationProperties);

		CssDao cssDao = DaoFactory.getCssDao(new MigrationProperties());

		CasAuditDao casAuditDao = DaoFactory.getCasAuditDao(new MigrationProperties());

		relayUserService = new RelayUserService();
		relayUserService.setCssDao(cssDao);
		relayUserService.setRelayLdap(relayLdap);
		relayUserService.setCasAuditDao(casAuditDao);

		pshrService = new PSHRService();
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
		boolean setPasswords = true;
		Set<RelayUser> relayUsersWithPassword = Sets.newHashSet();
		Set<RelayUser> relayUsersWithoutPassword = Sets.newHashSet();
		if(setPasswords)
		{
			relayUserService.setPasswords(authoritativeRelayUsers, relayUsersWithPassword, relayUsersWithoutPassword);
		}

		// set last logon timestamp
		Set<RelayUser> relayUsersWithLastLoginTimestamp = relayUserService.getWithLoginTimestamp(authoritativeRelayUsers);
		Output.println("Relay users with last login timestamp before setting last login timestamp (from CSS) " +
				relayUsersWithLastLoginTimestamp.size());
		relayUserService.setLastLogonTimestamp(authoritativeRelayUsers);
		relayUsersWithLastLoginTimestamp = relayUserService.getWithLoginTimestamp(authoritativeRelayUsers);
		Output.println("Relay users with last login timestamp after setting last login timestamp (from CSS) " +
				relayUsersWithLastLoginTimestamp.size());

		// determine users logged in since
		DateTime loggedInSince = (new DateTime()).minusMonths(3);
		Set<RelayUser> relayUsersLoggedInSince = relayUserService.getLoggedInSince(authoritativeRelayUsers, loggedInSince);
		Output.println("U.S. staff and google relay users logged in since " + loggedInSince + " size is " +
				relayUsersLoggedInSince.size());
		Set<RelayUser> relayUsersNotLoggedInSince = Sets.newHashSet();
		relayUsersNotLoggedInSince.addAll(authoritativeRelayUsers);
		relayUsersNotLoggedInSince.removeAll(relayUsersLoggedInSince);
		Output.println("U.S. staff and google relay users not logged in since " + loggedInSince + " size is " +
				relayUsersNotLoggedInSince.size());
		Output.logRelayUser(relayUsersLoggedInSince,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersLoggedInSince")));
		Output.logRelayUser(relayUsersNotLoggedInSince,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersNotLoggedInSince")));

		// relay users without password having logged in since
		Set<RelayUser> relayUsersWithoutPasswordHavingLoggedInSince = Sets.newHashSet();
		relayUsersWithoutPasswordHavingLoggedInSince.addAll(relayUsersWithoutPassword);
		relayUsersWithoutPasswordHavingLoggedInSince.removeAll(relayUsersNotLoggedInSince);
		Output.println("U.S. staff and google relay users without password having logged in since " + loggedInSince +
				" size is " + relayUsersWithoutPasswordHavingLoggedInSince.size());
		Output.logRelayUser(relayUsersWithoutPasswordHavingLoggedInSince,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersWithoutPasswordHavingLoggedInSince")));

		return authoritativeRelayUsers;
	}

	public Set<RelayUser> getUSStaffRelayUsers() throws Exception
	{
		Output.println("Getting staff from PSHR ...");
		Set<PSHRStaff> pshrUSStaff = pshrService.getPshrUSStaff();
		Output.println("PSHR staff count " + pshrUSStaff.size());
		Output.logPSHRStaff(pshrUSStaff,
				FileHelper.getFile(migrationProperties.getNonNullProperty("usStaffLogFile")));

		Output.println("Getting staff from Relay ...");
		Set<PSHRStaff> notFoundInRelay = Sets.newHashSet();
		Set<PSHRStaff> moreThanOneFoundWithEmployeeId = Sets.newHashSet();
		Set<RelayUser> duplicateRelayUsers = Sets.newHashSet();
		Set<RelayUser> relayUsers = relayUserService.fromPshrData(pshrUSStaff, notFoundInRelay,
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

		Set<RelayUser> relayUsers = relayUserService.fromDistinguishedNames(members);

		Output.println("Google relay users size is " + relayUsers.size());

		Output.logRelayUser(relayUsers,
				FileHelper.getFile(migrationProperties.getNonNullProperty("googleRelayUsersLogFile")));

		return relayUsers;
	}

	public void test() throws NamingException
	{
		Set<String> members = relayLdap.getGroupMembers(
				migrationProperties.getNonNullProperty("relayGoogleGroupsRoot"),
				migrationProperties.getNonNullProperty("relayGoogleGroupsMailNode"));

		Output.println("Google set members size is " + members.size());
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

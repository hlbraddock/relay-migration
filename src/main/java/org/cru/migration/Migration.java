package org.cru.migration;

import com.google.common.collect.Sets;
import org.cru.migration.dao.CasAuditDao;
import org.cru.migration.dao.CssDao;
import org.cru.migration.dao.DaoFactory;
import org.cru.migration.domain.PSHRStaff;

import org.cru.migration.domain.RelayUser;
import org.cru.migration.domain.RelayUsersGroupings;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private Logger logger;
	private TheKeyLdap theKeyLdap;

	public Migration() throws Exception
	{
		logger = LoggerFactory.getLogger(getClass());

		migrationProperties = new MigrationProperties();

		relayLdap = new RelayLdap(migrationProperties);

		theKeyLdap = new TheKeyLdap(migrationProperties);

		CssDao cssDao = DaoFactory.getCssDao(new MigrationProperties());

		CasAuditDao casAuditDao = DaoFactory.getCasAuditDao(new MigrationProperties());

		relayUserService = new RelayUserService(migrationProperties, cssDao, relayLdap, casAuditDao);

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

	public RelayUsersGroupings getRelayUsersGroupings() throws Exception
	{
		// get US Staff Relay Users
		Set<RelayUser> usStaffRelayUsers = getUSStaffRelayUsers();

		// get Google Relay Users
		Set<RelayUser> googleRelayUsers = getGoogleRelayUsers();

		// get relay users groupings from the collected us staff and google relay users
		return getRelayUsersGroupings(usStaffRelayUsers, googleRelayUsers);
	}

	private void setRelayUsersMetaData(RelayUsersGroupings relayUsersGroupings)
	{
		// set passwords
		setRelayUsersPassword(relayUsersGroupings);

		// set last logon timestamp
		setRelayUsersLastLoginTimeStamp(relayUsersGroupings.getRelayUsersAuthoritative());

		DateTime loggedInSince =
				(new DateTime()).minusMonths(
						Integer.valueOf(migrationProperties.getNonNullProperty("numberMonthsLoggedInSince")));

		// set logged in status
		setRelayUsersLoggedInStatus(relayUsersGroupings, loggedInSince);

		// log analysis of relay users groupings
		logDataAnalysis(relayUsersGroupings);
	}

	public void provisionUsers() throws Exception
	{
		RelayUsersGroupings relayUsersGroupings = getRelayUsersGroupings();

		setRelayUsersMetaData(relayUsersGroupings);

		boolean provisionUsers = false;

		if (provisionUsers)
		{
			theKeyLdap.provisionUsers(relayUsersGroupings.getRelayUsersLoggedIn());
		}
	}

	private void logDataAnalysis(RelayUsersGroupings relayUsersGroupings)
	{
		// relay users without password having logged in since
		Set<RelayUser> relayUsersWithoutPasswordHavingLoggedInSince = Sets.newHashSet();
		relayUsersWithoutPasswordHavingLoggedInSince.addAll(relayUsersGroupings.getRelayUsersWithoutPassword());
		relayUsersWithoutPasswordHavingLoggedInSince.removeAll(relayUsersGroupings.getRelayUsersNotLoggedIn());

		logger.debug("U.S. staff and google relay users without password having logged in since " +
				relayUsersGroupings.getLoggedInSince() +
				" size is " + relayUsersWithoutPasswordHavingLoggedInSince.size());
		Output.logRelayUser(relayUsersWithoutPasswordHavingLoggedInSince,
				FileHelper.getFile(migrationProperties.getNonNullProperty
						("relayUsersWithoutPasswordHavingLoggedInSince")));
	}

	public Set<RelayUser> getUSStaffRelayUsers() throws Exception
	{
		logger.debug("Getting staff from PSHR ...");
		Set<PSHRStaff> pshrUSStaff = pshrService.getPshrUSStaff();
		logger.debug("PSHR staff count " + pshrUSStaff.size());
		Output.logPSHRStaff(pshrUSStaff,
				FileHelper.getFile(migrationProperties.getNonNullProperty("usStaffLogFile")));

		logger.debug("Getting staff from Relay ...");
		Set<PSHRStaff> notFoundInRelay = Sets.newHashSet();
		Set<PSHRStaff> moreThanOneFoundWithEmployeeId = Sets.newHashSet();
		Set<RelayUser> duplicateRelayUsers = Sets.newHashSet();
		Set<RelayUser> relayUsers = relayUserService.fromPshrData(pshrUSStaff, notFoundInRelay,
				moreThanOneFoundWithEmployeeId, duplicateRelayUsers);

		logger.debug("Staff Relay user count " + relayUsers.size());
		logger.debug("Not found in Relay user count " + notFoundInRelay.size());
		logger.debug("More than one found with employee id user count " + moreThanOneFoundWithEmployeeId.size());
		logger.debug("Duplicate relay user count " + duplicateRelayUsers.size());

		Output.logPSHRStaff(moreThanOneFoundWithEmployeeId, FileHelper.getFile(migrationProperties.getNonNullProperty
				("moreThanOneRelayUserWithEmployeeId")));

		// log US Staff Relay Users
		logger.debug("U.S. staff relay users size is " + relayUsers.size());
		Output.logRelayUser(relayUsers,
				FileHelper.getFile(migrationProperties.getNonNullProperty("usStaffRelayUsersLogFile")));

		return relayUsers;
	}

	/**
	 * Google non U.S. staff users fall into at least two categories:
	 * 1. U.S. staff who have two (or more) Relay accounts, one of which has their employee id, and another,
	 * namely the Google non U.S. staff one, which is provisioned as their Google account
	 * 2. National Staff
	 */
	private RelayUsersGroupings getRelayUsersGroupings(Set<RelayUser> usStaffRelayUsers, Set<RelayUser>
			googleRelayUsers)
	{
		RelayUsersGroupings relayUsersGroupings = new RelayUsersGroupings();

		Set<RelayUser> usStaffNotInGoogle = Sets.newHashSet();
		usStaffNotInGoogle.addAll(usStaffRelayUsers);
		usStaffNotInGoogle.removeAll(googleRelayUsers);

		Set<RelayUser> usStaffInGoogle = Sets.newHashSet();
		usStaffInGoogle.addAll(usStaffRelayUsers);
		usStaffInGoogle.removeAll(usStaffNotInGoogle);

		logger.debug("US Staff size is " + usStaffRelayUsers.size());
		logger.debug("US Staff in google size is " + usStaffInGoogle.size());
		logger.debug("US Staff not in google size is " + usStaffNotInGoogle.size());

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

		logger.debug("Google size is " + googleRelayUsers.size());
		logger.debug("Google non US staff size is " + googleUserNotUSStaff.size());
		logger.debug("Google non US Staff size having employee id is " + googleUserNotUSStaffHavingEmployeeId.size());
		logger.debug("Google non US Staff size not having employee id is " +
				googleUserNotUSStaffNotHavingEmployeeId.size());
		logger.debug("Google US Staff size is " + googleUserUSStaff.size());

		Output.logRelayUser(googleUserNotUSStaff,
				FileHelper.getFile(migrationProperties.getNonNullProperty
						("googleNotUSStaffRelayUsersLogFile")));
		Output.logRelayUser(googleUserNotUSStaffHavingEmployeeId,
				FileHelper.getFile(migrationProperties.getNonNullProperty
						("googleNotUSStaffHavingEmployeeIdRelayUsersLogFile")));
		Output.logRelayUser(googleUserNotUSStaffNotHavingEmployeeId,
				FileHelper.getFile(migrationProperties.getNonNullProperty
						("googleNotUSStaffNotHavingEmployeeIdRelayUsersLogFile")));

		logger.debug("U.S. staff and google relay users size is " + relayUsersGroupings.getRelayUsersAuthoritative()
				.size());
		Output.logRelayUser(googleRelayUsers,
				FileHelper.getFile(migrationProperties.getNonNullProperty("googleAndUSStaffRelayUsersLogFile")));

		relayUsersGroupings.setRelayUsersStaff(usStaffRelayUsers);
		relayUsersGroupings.setRelayUsersGoogle(googleRelayUsers);
		relayUsersGroupings.setUsStaffInGoogle(usStaffInGoogle);
		relayUsersGroupings.setUsStaffNotInGoogle(usStaffNotInGoogle);
		relayUsersGroupings.setGoogleUserUSStaff(googleUserUSStaff);
		relayUsersGroupings.setGoogleUserNotUSStaff(googleUserNotUSStaff);
		relayUsersGroupings.setGoogleUserNotUSStaffHavingEmployeeId(googleUserNotUSStaffHavingEmployeeId);
		relayUsersGroupings.setGoogleUserNotUSStaffNotHavingEmployeeId(googleUserNotUSStaffNotHavingEmployeeId);

		return relayUsersGroupings;
	}

	private void setRelayUsersLoggedInStatus(RelayUsersGroupings relayUsersGroupings, DateTime loggedInSince)
	{
		Set<RelayUser> relayUsersLoggedInSince =
				relayUserService.getLoggedInSince(relayUsersGroupings.getRelayUsersAuthoritative(), loggedInSince);
		Set<RelayUser> relayUsersNotLoggedInSince = Sets.newHashSet();
		relayUsersNotLoggedInSince.addAll(relayUsersGroupings.getRelayUsersAuthoritative());
		relayUsersNotLoggedInSince.removeAll(relayUsersLoggedInSince);

		logger.debug("U.S. staff and google relay users logged in since " + loggedInSince + " size is " +
				relayUsersLoggedInSince.size());
		logger.debug("U.S. staff and google relay users not logged in since " + loggedInSince + " size is " +
				relayUsersNotLoggedInSince.size());

		Output.logRelayUser(relayUsersLoggedInSince,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersLoggedInSince")));
		Output.logRelayUser(relayUsersNotLoggedInSince,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersNotLoggedInSince")));

		relayUsersGroupings.setRelayUsersLoggedIn(relayUsersLoggedInSince);
		relayUsersGroupings.setRelayUsersNotLoggedIn(relayUsersNotLoggedInSince);
		relayUsersGroupings.setLoggedInSince(loggedInSince);
	}

	private void setRelayUsersPassword(RelayUsersGroupings relayUsersGroupings)
	{
		relayUserService.setPasswords(relayUsersGroupings);

		logger.debug("Relay users with password set size " + relayUsersGroupings.getRelayUsersWithPassword().size());
		logger.debug("Relay users without password set size " + relayUsersGroupings.getRelayUsersWithoutPassword()
				.size());
		Output.logRelayUser(relayUsersGroupings.getRelayUsersWithPassword(),
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersWithPasswordSet")));
		Output.logRelayUser(relayUsersGroupings.getRelayUsersWithoutPassword(),
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersWithoutPasswordSet")));
	}

	private void setRelayUsersLastLoginTimeStamp(Set<RelayUser> relayUsers)
	{
		// set last logon timestamp
		Set<RelayUser> relayUsersWithLastLoginTimestamp = relayUserService.getWithLoginTimestamp(relayUsers);
		logger.debug("Relay users with last login timestamp before setting last login timestamp (from CSS) " +
				relayUsersWithLastLoginTimestamp.size());
		relayUserService.setLastLogonTimestamp(relayUsers);
		relayUsersWithLastLoginTimestamp = relayUserService.getWithLoginTimestamp(relayUsers);
		logger.debug("Relay users with last login timestamp after setting last login timestamp (from CSS) " +
				relayUsersWithLastLoginTimestamp.size());
	}

	private Set<RelayUser> getGoogleRelayUsers() throws NamingException, UserNotFoundException,
			MoreThanOneUserFoundException, IOException
	{
		Set<String> members = relayLdap.getGroupMembers(
				migrationProperties.getNonNullProperty("relayGoogleGroupsRoot"),
				migrationProperties.getNonNullProperty("relayGoogleGroupsMailNode"));

		logger.debug("Google set members size is " + members.size());

		Set<RelayUser> relayUsers = relayUserService.fromDistinguishedNames(members);

		logger.debug("Google relay users size is " + relayUsers.size());

		Output.logRelayUser(relayUsers,
				FileHelper.getFile(migrationProperties.getNonNullProperty("googleRelayUsersLogFile")));

		return relayUsers;
	}

	public void test() throws NamingException
	{
		Set<String> members = relayLdap.getGroupMembers(
				migrationProperties.getNonNullProperty("relayGoogleGroupsRoot"),
				migrationProperties.getNonNullProperty("relayGoogleGroupsMailNode"));

		logger.debug("Google set members size is " + members.size());
	}

	public void createUser() throws Exception
	{
		RelayUser relayUser = new RelayUser("lee.braddock@cru.org", "Password1", "Lee", "Braddock", "", "", null);

		theKeyLdap.createUser(relayUser);
	}

	public void deleteUser() throws Exception
	{
		RelayUser relayUser = new RelayUser("lee.braddock@cru.org", "Password1", "Lee", "Braddock", "", "", null);

		theKeyLdap.deleteUser(relayUser);
	}

	public void removeUserDn() throws Exception
	{
		theKeyLdap.removeDn("");
	}

	enum Action
	{
		SystemEntries, USStaff, GoogleUsers, USStaffAndGoogleUsers, CreateUser, Test, ProvisionUsers, DeleteUser,
		RemoveAllKeyUserEntries
	}

	public static void main(String[] args) throws Exception
	{
		Migration migration = new Migration();

		try
		{
			Action action = Action.ProvisionUsers;

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
				migration.getRelayUsersGroupings();
			}
			else if (action.equals(Action.CreateUser))
			{
				migration.createUser();
			}
			else if (action.equals(Action.DeleteUser))
			{
				migration.deleteUser();
			}
			else if (action.equals(Action.RemoveAllKeyUserEntries))
			{
				migration.removeUserDn();
			}
			else if (action.equals(Action.ProvisionUsers))
			{
				migration.provisionUsers();
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

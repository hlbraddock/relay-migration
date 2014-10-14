package org.cru.migration;

import com.google.common.collect.Sets;
import org.ccci.gcx.idm.core.model.impl.GcxUser;
import org.cru.migration.dao.CasAuditDao;
import org.cru.migration.dao.CssDao;
import org.cru.migration.dao.DaoFactory;
import org.cru.migration.domain.PSHRStaff;

import org.cru.migration.domain.RelayUser;
import org.cru.migration.domain.RelayUserGroups;
import org.cru.migration.exception.MoreThanOneUserFoundException;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.ldap.RelayLdap;
import org.cru.migration.ldap.TheKeyLdap;
import org.cru.migration.service.PSHRService;
import org.cru.migration.service.RelayUserService;
import org.cru.migration.support.FileHelper;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;
import org.cru.migration.support.StringUtilities;
import org.joda.time.DateTime;
import org.joda.time.Duration;
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

	public RelayUserGroups getRelayUserGroups() throws Exception
	{
		// get US Staff Relay Users
		Set<RelayUser> usStaffRelayUsers = getUSStaffRelayUsers();

		// get Google Relay Users
		Set<RelayUser> googleRelayUsers = Sets.newHashSet();
		Boolean collectGoogleUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("collectGoogleUsers"));
		if(collectGoogleUsers)
		{
			googleRelayUsers = getGoogleRelayUsers();
		}

		// get relay users groupings from the collected us staff and google relay users
		return getRelayUserGroups(usStaffRelayUsers, googleRelayUsers);
	}

	public void provisionUsers() throws Exception
	{
		Boolean serializeRelayUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("serializeRelayUsers"));
		Boolean collectRelayUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("collectRelayUsers"));
		Boolean useSerializedRelayUsers =
				Boolean.valueOf(migrationProperties.getNonNullProperty("useSerializedRelayUsers"));
		Boolean provisionUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("callProvisionUsers"));
		Boolean collectMetaData = Boolean.valueOf(migrationProperties.getNonNullProperty("collectMetaData"));
		Boolean compareSerializedUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("compareSerializedUsers"));

		RelayUserGroups relayUserGroups = new RelayUserGroups();

		if(collectRelayUsers)
		{
			relayUserGroups = getRelayUserGroups();

			if(collectMetaData)
			{
				setRelayUsersMetaData(relayUserGroups);
			}
		}

		Set<RelayUser> relayUsersToSerialize = Sets.newHashSet();

		if(serializeRelayUsers)
		{
			relayUsersToSerialize =
					collectMetaData ? relayUserGroups.getLoggedIn() : relayUserGroups.getAuthoritative();

			logger.info("serializing relay users " + relayUsersToSerialize.size());

			Output.logRelayUsers(relayUsersToSerialize,
					FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("serializedRelayUsers")), true);
		}

		if(useSerializedRelayUsers)
		{
			logger.info("getting serialized relay users");

			Set<RelayUser> serializedRelayUsers = relayUserService.fromSerialized(
					FileHelper.getFileToRead(migrationProperties.getNonNullProperty("serializedRelayUsers")));

			Output.logRelayUsers(serializedRelayUsers,
					FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("readFromSerializedRelayUsers"))
					, true);

			logger.info("got serialized relay users " + serializedRelayUsers.size());

			relayUserGroups.setSerializedRelayUsers(serializedRelayUsers);

			if(compareSerializedUsers)
			{
				Output.logRelayUsers(relayUserService.compare(relayUsersToSerialize, serializedRelayUsers),
						FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("serializedComparison")));
			}
		}

		if (provisionUsers)
		{
			boolean authoritative = true;
			theKeyLdap.provisionUsers(
					useSerializedRelayUsers ? relayUserGroups.getSerializedRelayUsers() : relayUserGroups.getLoggedIn(),
					authoritative);
		}
	}

	public Set<RelayUser> getUSStaffRelayUsers() throws Exception
	{
		Boolean getAllPSHRStaff = Boolean.valueOf(migrationProperties.getNonNullProperty("getAllPSHRStaff"));

		logger.debug("Getting staff from PSHR ...");
		Set<PSHRStaff> pshrUSStaff = getAllPSHRStaff ? pshrService.getPshrUSStaff() : pshrService.getSomePshrUSStaff();
		logger.debug("PSHR staff count " + pshrUSStaff.size());
		Output.logPSHRStaff(pshrUSStaff,
				FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("usStaffLogFile")));

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

		Output.logPSHRStaff(moreThanOneFoundWithEmployeeId, FileHelper.getFileToWrite(migrationProperties
				.getNonNullProperty
				("moreThanOneRelayUserWithEmployeeId")));

		// log US Staff Relay Users
		logger.debug("U.S. staff relay users size is " + relayUsers.size());
		Output.logRelayUsers(relayUsers,
				FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("usStaffRelayUsersLogFile")));

		return relayUsers;
	}

	private void setRelayUsersMetaData(RelayUserGroups relayUserGroups)
	{
		// set passwords
		setRelayUsersPassword(relayUserGroups);

		// set last logon timestamp
		Boolean getLastLogonTimestampFromCasAudit =
				Boolean.valueOf(migrationProperties.getNonNullProperty("getLastLogonTimestampFromCasAudit"));

		if (getLastLogonTimestampFromCasAudit)
		{
			setRelayUsersLastLoginTimeStamp(relayUserGroups);
		}

		DateTime loggedInSince =
				(new DateTime()).minusMonths(
						Integer.valueOf(migrationProperties.getNonNullProperty("numberMonthsLoggedInSince")));

		// set logged in status
		setRelayUsersLoggedInStatus(relayUserGroups, loggedInSince);

		// log analysis of relay users groupings
		Output.logDataAnalysis(relayUserGroups);
	}

	/**
	 * Google non U.S. staff users fall into at least two categories:
	 * 1. U.S. staff who have two (or more) Relay accounts, one of which has their employee id, and another,
	 * namely the Google non U.S. staff one, which is provisioned as their Google account
	 * 2. National Staff
	 */
	private RelayUserGroups getRelayUserGroups(Set<RelayUser> usStaffRelayUsers, Set<RelayUser>
			googleRelayUsers)
	{
		RelayUserGroups relayUserGroups = new RelayUserGroups();

		Set<RelayUser> usStaffNotInGoogle = Sets.newHashSet();
		usStaffNotInGoogle.addAll(usStaffRelayUsers);
		usStaffNotInGoogle.removeAll(googleRelayUsers);

		Set<RelayUser> usStaffInGoogle = Sets.newHashSet();
		usStaffInGoogle.addAll(usStaffRelayUsers);
		usStaffInGoogle.removeAll(usStaffNotInGoogle);

		logger.debug("US Staff size is " + usStaffRelayUsers.size());
		logger.debug("US Staff in google size is " + usStaffInGoogle.size());
		logger.debug("US Staff not in google size is " + usStaffNotInGoogle.size());

		Output.logRelayUsers(usStaffInGoogle,
				FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("usStaffInGoogleRelayUsersLogFile")));
		Output.logRelayUsers(usStaffNotInGoogle,
				FileHelper.getFileToWrite(migrationProperties.getNonNullProperty
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

		Output.logRelayUsers(googleUserNotUSStaff,
				FileHelper.getFileToWrite(migrationProperties.getNonNullProperty
						("googleNotUSStaffRelayUsersLogFile")));
		Output.logRelayUsers(googleUserNotUSStaffHavingEmployeeId,
				FileHelper.getFileToWrite(migrationProperties.getNonNullProperty
						("googleNotUSStaffHavingEmployeeIdRelayUsersLogFile")));
		Output.logRelayUsers(googleUserNotUSStaffNotHavingEmployeeId,
				FileHelper.getFileToWrite(migrationProperties.getNonNullProperty
						("googleNotUSStaffNotHavingEmployeeIdRelayUsersLogFile")));

		relayUserGroups.setUsStaff(usStaffRelayUsers);
		relayUserGroups.setGoogleUsers(googleRelayUsers);
		relayUserGroups.setUsStaffInGoogle(usStaffInGoogle);
		relayUserGroups.setUsStaffNotInGoogle(usStaffNotInGoogle);
		relayUserGroups.setGoogleUserUSStaff(googleUserUSStaff);
		relayUserGroups.setGoogleUserNotUSStaff(googleUserNotUSStaff);
		relayUserGroups.setGoogleUsersNotUSStaffHavingEmployeeId(googleUserNotUSStaffHavingEmployeeId);
		relayUserGroups.setGoogleUsersNotUSStaffNotHavingEmployeeId(googleUserNotUSStaffNotHavingEmployeeId);

		logger.debug("U.S. staff and google relay users size is " + relayUserGroups.getAuthoritative()
				.size());
		Output.logRelayUsers(relayUserGroups.getAuthoritative(),
				FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("googleAndUSStaffRelayUsersLogFile")));

		return relayUserGroups;
	}

	private void setRelayUsersLoggedInStatus(RelayUserGroups relayUserGroups, DateTime loggedInSince)
	{
		Set<RelayUser> relayUsersLoggedInSince =
				relayUserService.getLoggedInSince(relayUserGroups.getAuthoritative(), loggedInSince);
		Set<RelayUser> relayUsersNotLoggedInSince = Sets.newHashSet();
		relayUsersNotLoggedInSince.addAll(relayUserGroups.getAuthoritative());
		relayUsersNotLoggedInSince.removeAll(relayUsersLoggedInSince);

		logger.debug("U.S. staff and google relay users logged in since " + loggedInSince + " size is " +
				relayUsersLoggedInSince.size());
		logger.debug("U.S. staff and google relay users not logged in since " + loggedInSince + " size is " +
				relayUsersNotLoggedInSince.size());

		Output.logRelayUsers(relayUsersLoggedInSince,
				FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("relayUsersLoggedInSince")));
		Output.logRelayUsers(relayUsersNotLoggedInSince,
				FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("relayUsersNotLoggedInSince")));

		relayUserGroups.setLoggedIn(relayUsersLoggedInSince);
		relayUserGroups.setNotLoggedIn(relayUsersNotLoggedInSince);
		relayUserGroups.setLoggedInSince(loggedInSince);
	}

	private void setRelayUsersPassword(RelayUserGroups relayUserGroups)
	{
		relayUserService.setPasswords(relayUserGroups);

		logger.debug("Relay users with password set size " + relayUserGroups.getWithPassword().size());
		logger.debug("Relay users without password set size " + relayUserGroups.getWithoutPassword()
				.size());
		Output.logRelayUsers(relayUserGroups.getWithPassword(),
                FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("relayUsersWithPasswordSet")));
		Output.logRelayUsers(relayUserGroups.getWithoutPassword(),
                FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("relayUsersWithoutPasswordSet")));
	}

	private void setRelayUsersLastLoginTimeStamp(RelayUserGroups relayUserGroups)
	{
		// set last logon timestamp
		Set<RelayUser> relayUsersWithLastLoginTimestamp = relayUserService.getWithLoginTimestamp(relayUserGroups.getAuthoritative());
		Set<RelayUser> relayUsersWithoutLastLoginTimestamp = Sets.newHashSet(relayUserGroups.getAuthoritative());
		relayUsersWithoutLastLoginTimestamp.removeAll(relayUsersWithLastLoginTimestamp);

		logger.debug("Relay users with last login timestamp before setting last login timestamp from CSS " +
                relayUsersWithLastLoginTimestamp.size());
		logger.debug("Relay users without last login timestamp before setting last login timestamp from CSS " +
				relayUsersWithoutLastLoginTimestamp.size());

		relayUserService.setLastLogonTimestamp(relayUserGroups);

		relayUsersWithLastLoginTimestamp = relayUserService.getWithLoginTimestamp(relayUserGroups.getAuthoritative());
		relayUsersWithoutLastLoginTimestamp.removeAll(relayUsersWithLastLoginTimestamp);

		logger.debug("Relay users with last login timestamp after setting last login timestamp from CSS " +
				relayUsersWithLastLoginTimestamp.size());
		logger.debug("Relay users without last login timestamp after setting last login timestamp from CSS " +
				relayUsersWithoutLastLoginTimestamp.size());
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

		Output.logRelayUsers(relayUsers,
				FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("googleRelayUsersLogFile")));

		return relayUsers;
	}

	public void createUser() throws Exception
	{
		RelayUser relayUser = new RelayUser("lee.braddock@cru.org", "Password1", "Lee", "Braddock", "", "", null);

		theKeyLdap.createUser(relayUser);
	}

	public void createCruPersonObject(Boolean deleteFirst) throws Exception
	{
		if(deleteFirst)
		{
			try
			{
				theKeyLdap.deleteCruPersonObject();
			}
			catch(Exception e)
			{
				logger.info("exception on delete cru person object " + e.getMessage());
			}
		}

		theKeyLdap.createCruPersonObject();
	}

	public void deleteCruPersonObject() throws Exception
	{
		theKeyLdap.deleteCruPersonObject();
	}

    public void removeUserDn() throws Exception
    {
        theKeyLdap.removeDn("");
    }

    public void getUserCount() throws Exception
    {
        System.out.println(theKeyLdap.getUserCount());
    }


    public void verifyProvisionedUsers() throws Exception
    {
        Set<RelayUser> relayUsersProvisioned = relayUserService.fromSerialized(
                FileHelper.getFileToRead(migrationProperties.getNonNullProperty("relayUsersProvisioned")));

        System.out.println("relay users provisioned is " + relayUsersProvisioned.size());

        Integer provisioned = 0;
        Integer notProvisioned = 0;
        for(RelayUser relayUser : relayUsersProvisioned)
        {
            if(provisioned % 100 == 0)
            {
                System.out.print("provisioned check " + provisioned + "\r");
            }

            try
            {
                GcxUser gcxUser = theKeyLdap.getGcxUser(relayUser.getUsername());

                if (gcxUser == null)
                {
                    System.out.println("not provisioned " + relayUser.getUsername());
                    notProvisioned++;
                }
                else
                {
                    provisioned++;
                }
            }
            catch (Exception e)
            {
                System.out.println("exception for user " + relayUser.getUsername() + " " + e.getMessage());
                notProvisioned++;
            }
        }

        System.out.println("provisioned " + provisioned);
        System.out.println("not provisioned " + notProvisioned);
        System.out.println("total " + (notProvisioned + provisioned));
    }

	public void test() throws NamingException, Exception
	{
        Thread.sleep(4000);
	}

	enum Action
	{
		SystemEntries, USStaff, GoogleUsers, USStaffAndGoogleUsers, CreateUser, Test, ProvisionUsers,
		RemoveAllKeyUserEntries, CreateCruPersonObjectClass, DeleteCruPersonObjectClass, GetUserCount, CheckUsers
	}

	public static void main(String[] args) throws Exception
	{
		Migration migration = new Migration();

		Logger logger = LoggerFactory.getLogger(Migration.class);

        DateTime start = DateTime.now();
		logger.debug("start time " + start);

		try
		{
			Action action = Action.CheckUsers;

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
				migration.getRelayUserGroups();
			}
			else if (action.equals(Action.CreateUser))
			{
				migration.createUser();
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
			else if (action.equals(Action.CreateCruPersonObjectClass))
			{
				migration.createCruPersonObject(true);
			}
            else if (action.equals(Action.DeleteCruPersonObjectClass))
            {
                migration.deleteCruPersonObject();
            }
            else if (action.equals(Action.GetUserCount))
            {
                migration.getUserCount();
            }
            else if (action.equals(Action.CheckUsers))
            {
                migration.verifyProvisionedUsers();
            }
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

        DateTime finish = DateTime.now();
        logger.debug("finish time " + finish);

        Duration duration = new Duration(start, finish);
        logger.debug("duration " + StringUtilities.toString(duration));
	}
}

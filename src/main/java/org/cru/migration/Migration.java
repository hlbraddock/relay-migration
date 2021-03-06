package org.cru.migration;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.ccci.idm.user.User;
import org.ccci.idm.user.UserManager;
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
import org.cru.migration.service.AuthenticationService;
import org.cru.migration.service.FindKeyAccountsMatchingMultipleRelayAccountsService;
import org.cru.migration.service.PshrService;
import org.cru.migration.service.RelayUserService;
import org.cru.migration.support.FileHelper;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;
import org.cru.migration.support.StringUtilities;
import org.cru.migration.thekey.GcxUserService;
import org.cru.migration.thekey.TheKeyBeans;
import org.cru.silc.service.LinkingServiceImpl;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Migration
{
	private MigrationProperties migrationProperties;
	private RelayLdap relayLdap;
	private RelayUserService relayUserService;
	private PshrService pshrService;
	private Logger logger;
	private TheKeyLdap theKeyLdap;
	private GcxUserService gcxUserService;

	public Migration() throws Exception
	{
		logger = LoggerFactory.getLogger(getClass());

		migrationProperties = new MigrationProperties();

		relayLdap = new RelayLdap(migrationProperties);

		theKeyLdap = new TheKeyLdap(migrationProperties);

		CssDao cssDao = DaoFactory.getCssDao(new MigrationProperties());

		CasAuditDao casAuditDao = DaoFactory.getCasAuditDao(new MigrationProperties());

		relayUserService = new RelayUserService(migrationProperties, cssDao, relayLdap, casAuditDao);

		pshrService = new PshrService();

		UserManager userManager = TheKeyBeans.getUserManager();

		LinkingServiceImpl linkingServiceImpl = new LinkingServiceImpl();
		linkingServiceImpl.setResource(migrationProperties.getNonNullProperty("identityLinkingResource"));
		linkingServiceImpl.setIdentitiesAccessToken(migrationProperties.getNonNullProperty("identityLinkingAccessToken"));

		gcxUserService = new GcxUserService(userManager, linkingServiceImpl);
	}

	/**
	 * Create Relay system account entries in the Key
	 *
	 * @throws Exception
	 */
	public void createSystemEntries() throws Exception
	{
		theKeyLdap.createSystemEntries();
	}

	public RelayUserGroups getRelayUserGroups() throws Exception
	{
        // get US Staff Relay Users
		Set<RelayUser> usStaffRelayUsers = Sets.newHashSet();
		Boolean collectStaffUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("collectStaffUsers"));
		if(collectStaffUsers)
		{
			usStaffRelayUsers = getUSStaffRelayUsers();
		}

		// get Google Relay Users
		Set<RelayUser> googleRelayUsers = Sets.newHashSet();
		Boolean collectGoogleUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("collectGoogleUsers"));
		if(collectGoogleUsers)
		{
			googleRelayUsers = getGoogleRelayUsers();
		}

		Set<RelayUser> allRelayUsers = Sets.newHashSet();
		Boolean collectNonStaffUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("collectNonStaffUsers"));
		if(collectNonStaffUsers)
		{
			allRelayUsers = relayUserService.getAllRelayUsers();
		}

		// get relay users groupings from the collected us staff and google relay users
		RelayUserGroups relayUserGroups = getRelayUserGroups(usStaffRelayUsers,
				googleRelayUsers, allRelayUsers);

		logger.debug("Non authoritative relay users size is " + relayUserGroups.getNonStaffUsers().size());
		Output.serializeRelayUsers(relayUserGroups.getNonStaffUsers(),
				migrationProperties.getNonNullProperty("nonAuthoritativeRelayUsers"));

		return relayUserGroups;
	}

	/**
	 * Google non U.S. staff users fall into at least two categories:
	 * 1. U.S. staff who have two (or more) Relay accounts, one of which has their employee id, and another,
	 * namely the Google non U.S. staff one, which is provisioned as their Google account
	 * 2. National Staff
	 */
	private RelayUserGroups getRelayUserGroups(Set<RelayUser> usStaffRelayUsers,
											   Set<RelayUser> googleRelayUsers,
											   Set<RelayUser> allRelayUsers)
	{
		RelayUserGroups relayUserGroups = new RelayUserGroups();

		relayUserGroups.setAllRelayUsers(allRelayUsers);

		Set<RelayUser> usStaffNotInGoogle = Sets.newHashSet();
		usStaffNotInGoogle.addAll(usStaffRelayUsers);
		usStaffNotInGoogle.removeAll(googleRelayUsers);

		Set<RelayUser> usStaffInGoogle = Sets.newHashSet();
		usStaffInGoogle.addAll(usStaffRelayUsers);
		usStaffInGoogle.removeAll(usStaffNotInGoogle);

		logger.debug("US Staff size is " + usStaffRelayUsers.size());
		logger.debug("US Staff in google size is " + usStaffInGoogle.size());
		logger.debug("US Staff not in google size is " + usStaffNotInGoogle.size());

		Output.serializeRelayUsers(usStaffInGoogle,
				migrationProperties.getNonNullProperty("usStaffInGoogleRelayUsersLogFile"));
		Output.serializeRelayUsers(usStaffNotInGoogle,
				migrationProperties.getNonNullProperty
						("usStaffNotInGoogleRelayUsersLogFile"));

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

		Output.serializeRelayUsers(googleUserNotUSStaff,
				migrationProperties.getNonNullProperty
						("googleNotUSStaffRelayUsersLogFile"));
		Output.serializeRelayUsers(googleUserNotUSStaffHavingEmployeeId,
				migrationProperties.getNonNullProperty
						("googleNotUSStaffHavingEmployeeIdRelayUsersLogFile"));
		Output.serializeRelayUsers(googleUserNotUSStaffNotHavingEmployeeId,
				migrationProperties.getNonNullProperty
						("googleNotUSStaffNotHavingEmployeeIdRelayUsersLogFile"));

		Set<RelayUser> nonStaffUsers = Sets.newHashSet();
		nonStaffUsers.addAll(allRelayUsers);
		nonStaffUsers.removeAll(usStaffRelayUsers);
		nonStaffUsers.removeAll(googleRelayUsers);

		logger.debug("Non US Staff size is " + nonStaffUsers.size());
		Output.serializeRelayUsers(nonStaffUsers, migrationProperties.getNonNullProperty("nonUSStaffLogFile"));

		relayUserGroups.setUsStaff(usStaffRelayUsers);
		relayUserGroups.setGoogleUsers(googleRelayUsers);
		relayUserGroups.setNonStaffUsers(nonStaffUsers);
		relayUserGroups.setUsStaffInGoogle(usStaffInGoogle);
		relayUserGroups.setUsStaffNotInGoogle(usStaffNotInGoogle);
		relayUserGroups.setGoogleUserUSStaff(googleUserUSStaff);
		relayUserGroups.setGoogleUserNotUSStaff(googleUserNotUSStaff);
		relayUserGroups.setGoogleUsersNotUSStaffHavingEmployeeId(googleUserNotUSStaffHavingEmployeeId);
		relayUserGroups.setGoogleUsersNotUSStaffNotHavingEmployeeId(googleUserNotUSStaffNotHavingEmployeeId);

		return relayUserGroups;
	}

	public void provisionUsers() throws Exception
	{
		Boolean serializeRelayUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("serializeRelayUsers"));
		Boolean collectRelayUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("collectRelayUsers"));
		Boolean useSerializedRelayUsers =
				Boolean.valueOf(migrationProperties.getNonNullProperty("useSerializedRelayUsers"));
		Boolean callProvisionUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("callProvisionUsers"));
		Boolean collectMetaData = Boolean.valueOf(migrationProperties.getNonNullProperty("collectMetaData"));
		Boolean compareSerializedUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("compareSerializedUsers"));
		Boolean collectMultipleRelayUsersMatchingKeyUser =
				Boolean.valueOf(migrationProperties.getNonNullProperty("collectMultipleRelayUserMatchesOnKeyUser"));

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

		if(collectRelayUsers && serializeRelayUsers)
		{
			relayUsersToSerialize = relayUserGroups.getAllUsers();

			logger.info("serializing relay users " + relayUsersToSerialize.size());

			Output.serializeRelayUsers(relayUsersToSerialize, migrationProperties.getNonNullProperty
					("serializedRelayUsers"), true);
		}

		if(useSerializedRelayUsers)
		{
			logger.info("getting serialized relay users");

			Set<RelayUser> serializedRelayUsers = Output.deserializeRelayUsers(
					migrationProperties.getNonNullProperty("serializedRelayUsers"));

			logger.info("got serialized relay users " + serializedRelayUsers.size());

			Output.serializeRelayUsers(serializedRelayUsers,
					migrationProperties.getNonNullProperty("readFromSerializedRelayUsers"), true);

			relayUserGroups.setSerializedRelayUsers(serializedRelayUsers);

			if(compareSerializedUsers && collectRelayUsers)
			{
				logger.info("Comparing LDAP relay users with deserialized relay users ...");

				Set<RelayUser> differing = relayUserService.compare(relayUsersToSerialize, serializedRelayUsers);

				if(differing.size() != 0)
				{
					throw new Exception("Serialized users comparison not equal!");
				}

				logger.info("Comparing LDAP relay users with deserialized relay users ... done");

				Output.serializeRelayUsers(differing,
						migrationProperties.getNonNullProperty("serializedComparison"), true);
			}
		}

		if(collectMultipleRelayUsersMatchingKeyUser)
		{
			Set<RelayUser> relayUsers = useSerializedRelayUsers ?
					relayUserGroups.getSerializedRelayUsers() :
					relayUserGroups.getAllUsers();

			Map<String, RelayUser> relayUserMapGuid = useSerializedRelayUsers ?
					RelayUser.getRelayUserMapGuid(relayUserGroups.getSerializedRelayUsers()) :
					relayUserGroups.getAllUsersSsoguidKey();

			Map<String, RelayUser> relayUserMapUsername = useSerializedRelayUsers ?
					RelayUser.getRelayUserMapUsername(relayUserGroups.getSerializedRelayUsers()) :
					relayUserGroups.getAllUsersUsernameKey();

			determineKeyAccountsMatchingMultipleRelayAccounts(
					relayUsers, relayUserGroups, relayUserMapGuid, relayUserMapUsername);

			Output.logMessage(relayUserGroups.getMultipleRelayUsersMatchingKeyUser(),
					FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("multipleRelayUsersMatchingKeyUser")));
		}

		if (callProvisionUsers)
		{
			theKeyLdap.provisionUsers(useSerializedRelayUsers ? relayUserGroups.getSerializedRelayUsers() :
                    relayUserGroups.getAllUsers(), relayUserGroups.getKeyUserMatchingRelayUsers());
		}
	}

	private void getTheKeyLegacyUserCount() throws Exception
	{
		logger.info("getting key user count");
		List<User> theKeyLegacyUsers = theKeyLdap.getKeyLegacyUsers();

		logger.info("key user count" + theKeyLegacyUsers.size());
	}

	private void determineKeyAccountsMatchingMultipleRelayAccounts(
			Set<RelayUser> relayUsers, RelayUserGroups relayUserGroups,
			Map<String, RelayUser> relayUserMapGuid,
			Map<String, RelayUser> relayUserMapUsername) throws NamingException
	{
		logger.info("Getting all the Key ldap entries");

		List<User> theKeyLegacyUsers = theKeyLdap.getKeyLegacyUsers();

		logger.info("Found the Key legacy users size " + theKeyLegacyUsers.size());

		FindKeyAccountsMatchingMultipleRelayAccountsService findKeyAccountsMatchingMultipleRelayAccountsService =
				new FindKeyAccountsMatchingMultipleRelayAccountsService(gcxUserService);

		logger.info("Checking for multiple relay users matching one key account");

		FindKeyAccountsMatchingMultipleRelayAccountsService.Result result  =
			findKeyAccountsMatchingMultipleRelayAccountsService.run(theKeyLegacyUsers, relayUsers,
					relayUserMapGuid, relayUserMapUsername);

		logger.info("Done checking for multiple relay users matching one key account");

		relayUserGroups.setMultipleRelayUsersMatchingKeyUser(result.getMultipleRelayUsersMatchingKeyUser());
		relayUserGroups.setKeyUserMatchingRelayUsers(result.getKeyUserMatchingRelayUsers());
	}

	public void updateKeyUserFromRelay() throws Exception {
		Set<RelayUser> relayUsers;
		relayUsers = relayUserService.getAllRelayUsers();

		//	relayUsers = getUSStaffRelayUsers();
		//	relayUsers = getGoogleRelayUsers();
		//	relayUsers = relayLdap.getRelayUsersWithDesignation();

		logger.info("relay users size is " + relayUsers.size());

		final AtomicInteger count = new AtomicInteger();
		ExecutorService exec = Executors.newFixedThreadPool(50);
		final UserManager userManagerMerge = TheKeyBeans.getUserManagerMerge();

		boolean execute = true;
		final boolean update = true;

		if(execute) {
			for (final RelayUser relayUser : relayUsers) {
				exec.execute(new Runnable() {
					@Override
					public void run() {
						try {
							if (!Strings.isNullOrEmpty(relayUser.getSsoguid())) {
								User findUser = userManagerMerge.findUserByRelayGuid(relayUser.getSsoguid());
								if (findUser != null) {
									findUser.setRelayGuid(relayUser.getSsoguid());
									logger.info("updating (" + count.incrementAndGet() + ") " +
											findUser.getEmail() + "," +
											findUser.getRawRelayGuid());
									if (update) {
										userManagerMerge.updateUser(findUser, User.Attr.RELAY_GUID);
									}
								}
							}
						} catch (Exception e) {
							logger.error("error ", e);
						}
					}
				});
			}

			exec.shutdown();

			while (!exec.awaitTermination(1, TimeUnit.HOURS)) {
			}
		}
	}

	private Collection<String> removePrefix(Collection<String> collection, String prefix) {
		List<String> newCollection = Lists.newArrayList();

		for(String string : collection) {
			newCollection.add(string.replaceAll("(?i)" + prefix, ""));
		}

		return newCollection;
	}

	public void checkStaff() throws Exception {
		Set<RelayUser> relayUsers = getUSStaffRelayUsers();

		final AtomicInteger count = new AtomicInteger();
		ExecutorService exec = Executors.newFixedThreadPool(50);
		for (final RelayUser relayUser : relayUsers) {

			exec.execute(new Runnable() {
				@Override
				public void run() {
					try {
						if(relayUser.getUsername().equalsIgnoreCase("andy.dixon@cru.org")) {
							logger.info("found " + relayUser.getUsername());
						}

						if(count.getAndIncrement() % 1000 == 0) {
							System.out.printf("Merged users " + count.get() + "\r");
						}
					} catch (Exception e) {
						logger.error("error ", e);
					}
				}
			});
		}

		exec.shutdown();

		while (!exec.awaitTermination(1, TimeUnit.HOURS)) {
		}
	}

	public Set<RelayUser> getUSStaffRelayUsers() throws Exception {
		Boolean getAllPSHRStaff = Boolean.valueOf(migrationProperties.getNonNullProperty("getAllPSHRStaff"));
        Boolean collectSpecificUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("collectSpecificUsers"));

        Set<RelayUser> relayUsers;

        if(!collectSpecificUsers)
        {
            logger.debug("Getting staff from PSHR ...");
            Set<PSHRStaff> pshrUSStaff = getAllPSHRStaff ? pshrService.getPshrUSStaff() : pshrService.getSomePshrUSStaff();
            logger.debug("PSHR staff count " + pshrUSStaff.size());
            Output.logPSHRStaff(pshrUSStaff,
                    FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("usStaffLogFile")));

            logger.debug("Getting staff from Relay ...");
            Set<PSHRStaff> notFoundInRelay = Sets.newHashSet();
            Set<PSHRStaff> moreThanOneFoundWithEmployeeId = Sets.newHashSet();
            Set<RelayUser> duplicateRelayUsers = Sets.newHashSet();
            relayUsers = relayUserService.fromPshrData(pshrUSStaff, notFoundInRelay,
                    moreThanOneFoundWithEmployeeId, duplicateRelayUsers);

            logger.debug("Staff Relay user count " + relayUsers.size());
            logger.debug("Not found in Relay user count " + notFoundInRelay.size());
            logger.debug("More than one found with employee id user count " + moreThanOneFoundWithEmployeeId.size());
            logger.debug("Duplicate relay user count " + duplicateRelayUsers.size());

            Output.logPSHRStaff(moreThanOneFoundWithEmployeeId, FileHelper.getFileToWrite(migrationProperties
                    .getNonNullProperty
                            ("moreThanOneRelayUserWithEmployeeId")));
        }
        else
        {
            File file = FileHelper.getFileToRead(migrationProperties.getNonNullProperty("userDistinguishedNames"));
            relayUsers = relayUserService.
					fromDistinguishedNames(Sets.newHashSet(Files.readLines(file, Charsets.UTF_8)), false);
        }

		// log US Staff Relay Users
		logger.debug("U.S. staff relay users size is " + relayUsers.size());
		Output.serializeRelayUsers(relayUsers,
				migrationProperties.getNonNullProperty("usStaffRelayUsersLogFile"));

		return relayUsers;
	}

	private void setRelayUsersMetaData(RelayUserGroups relayUserGroups) throws NamingException
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

	private void loggedInSince() throws Exception
	{
		DateTime loggedInSince = (new DateTime()).minusMonths(1);

		Set<RelayUser> allRelayUsers = relayUserService.getAllRelayUsers();

		System.out.println("all relay users size is " + allRelayUsers.size());

		Set<RelayUser> relayUsersLoggedInSince =
				relayUserService.getLoggedInSince(allRelayUsers, loggedInSince);

		System.out.println("relay users logged in since is " + relayUsersLoggedInSince.size());

		Output.serializeRelayUsers(relayUsersLoggedInSince, "/work/relay/loggedInSince");
	}

	private void setRelayUsersLoggedInStatus(RelayUserGroups relayUserGroups, DateTime loggedInSince)
	{
		Set<RelayUser> relayUsersLoggedInSince =
				relayUserService.getLoggedInSince(relayUserGroups.getAllUsers(), loggedInSince);
		Set<RelayUser> relayUsersNotLoggedInSince = Sets.newHashSet();
		relayUsersNotLoggedInSince.addAll(relayUserGroups.getAllUsers());
		relayUsersNotLoggedInSince.removeAll(relayUsersLoggedInSince);

		logger.debug("Of 'all' relay users (to be provisioned) logged in since " + loggedInSince + " size is " +
				relayUsersLoggedInSince.size());
		logger.debug("Of 'all' relay users (to be provisioned) logged in since " + loggedInSince + " size is " +
				relayUsersNotLoggedInSince.size());

		Output.serializeRelayUsers(relayUsersLoggedInSince,
				migrationProperties.getNonNullProperty("relayUsersLoggedInSince"));
		Output.serializeRelayUsers(relayUsersNotLoggedInSince,
				migrationProperties.getNonNullProperty("relayUsersNotLoggedInSince"));

		relayUserGroups.setLoggedIn(relayUsersLoggedInSince);
		relayUserGroups.setNotLoggedIn(relayUsersNotLoggedInSince);
		relayUserGroups.setLoggedInSince(loggedInSince);
	}

	private void setRelayUsersPassword(RelayUserGroups relayUserGroups) throws NamingException
	{
		relayUserService.setPasswords(relayUserGroups);

		logger.debug("Relay users with password set size " + relayUserGroups.getWithPassword().size());
		logger.debug("Relay users without password set size " + relayUserGroups.getWithoutPassword()
				.size());
		Output.serializeRelayUsers(relayUserGroups.getWithPassword(),
                migrationProperties.getNonNullProperty("relayUsersWithPasswordSet"));
		Output.serializeRelayUsers(relayUserGroups.getWithoutPassword(),
                migrationProperties.getNonNullProperty("relayUsersWithoutPasswordSet"));
	}

	private void setRelayUsersLastLoginTimeStamp(RelayUserGroups relayUserGroups) throws NamingException
	{
		// set last logon timestamp
		Set<RelayUser> relayUsersWithLastLoginTimestamp =
				relayUserService.getWithLoginTimestamp(relayUserGroups.getAllUsers());
		Set<RelayUser> relayUsersWithoutLastLoginTimestamp = Sets.newHashSet(relayUserGroups.getAllUsers());
		relayUsersWithoutLastLoginTimestamp.removeAll(relayUsersWithLastLoginTimestamp);

		logger.debug("Relay users with last login timestamp before setting last login timestamp from CSS " +
                relayUsersWithLastLoginTimestamp.size());
		logger.debug("Relay users without last login timestamp before setting last login timestamp from CSS " +
				relayUsersWithoutLastLoginTimestamp.size());

		relayUserService.setLastLogonTimestamp(relayUserGroups);

		relayUsersWithLastLoginTimestamp = relayUserService.getWithLoginTimestamp(relayUserGroups.getAllUsers());
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

		Boolean collectSpecificUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("collectSpecificUsers"));
		if(collectSpecificUsers)
		{
			File file = FileHelper.getFileToRead(migrationProperties.getNonNullProperty("userDistinguishedNames"));
			Set<String> userDns = Sets.newHashSet(Files.readLines(file, Charsets.UTF_8));

			for(String member : userDns)
			{
				logger.info("user dns " + member);
			}

			Set<String> distinguishedMembers = Sets.newHashSet();
			for(String member : members)
			{
				if(userDns.contains(member))
				{
					logger.info("Adding member " + member);
					distinguishedMembers.add(member);
				}
			}

			members.clear();
			members.addAll(distinguishedMembers);

			for(String member : members)
			{
				logger.info("members " + member);
			}
		}

		logger.debug("Google set members size is " + members.size());


		Set<RelayUser> relayUsers = relayUserService.fromDistinguishedNames(members, true);

		logger.debug("Google relay users size is " + relayUsers.size());

		if(collectSpecificUsers)
		{
			for(RelayUser relayUser : relayUsers)
			{
				logger.info("Google relay users " + relayUser.getUsername() + "," + relayUser.isGoogle());
			}
		}

		Output.serializeRelayUsers(relayUsers,
				migrationProperties.getNonNullProperty("googleRelayUsersLogFile"));

		return relayUsers;
	}

	public void createUser() throws Exception
	{
		RelayUser relayUser = new RelayUser("test.user@ccci.org", "Password1", "Lee", "Braddock", "",
				UUID.randomUUID().toString(), new DateTime());

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

	public void createRelayObject(Boolean deleteFirst) throws Exception
	{
		if(deleteFirst)
		{
			try
			{
				theKeyLdap.deleteRelayObject();
			}
			catch(Exception e)
			{
				logger.info("exception on delete relay object " + e.getMessage());
			}
		}

		theKeyLdap.createRelayAttributesObject();
	}

	public void createCruPersonAttributes(Boolean deleteFirst) throws Exception
	{
		if(deleteFirst)
		{
			try
			{
				theKeyLdap.deleteCruPersonAttributes();
			}
			catch(Exception e)
			{
				logger.info("exception on delete cru person attributes " + e.getMessage());
			}
		}

		theKeyLdap.createCruPersonAttributes();
	}

	public void deleteCruPersonAttributes() throws Exception
	{
		try
		{
			theKeyLdap.deleteCruPersonAttributes();
		}
		catch (Exception e)
		{
			logger.info("exception on delete cru person attributes " + e.getMessage());
		}
	}

	public void createRelayAttributes(Boolean deleteFirst) throws Exception
	{
		if(deleteFirst)
		{
			try
			{
				theKeyLdap.deleteRelayAttributes();
			}
			catch(Exception e)
			{
				logger.info("exception on delete relay attributes " + e.getMessage());
			}
		}

		theKeyLdap.createRelayAttributes();
	}

    public void authenticateRelayUsers() throws Exception
    {
        logger.info("Reading serialized users ...");

        Set<RelayUser> serializedRelayUsers = Output.deserializeRelayUsers(
                migrationProperties.getNonNullProperty("serializedRelayUsers"));

        Set<RelayUser> relayUsers = Sets.newHashSet();

        File authoritativeUsersFile =
                FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("authoritativeUsers"));

        Set<String> authoritativeUsers = Sets.newHashSet();
        for(String authoritativeUser : Files.readLines(authoritativeUsersFile, Charsets.UTF_8))
        {
            authoritativeUsers.add(authoritativeUser.toLowerCase());
        }

        logger.info("Setting authoritative ...");

        for(RelayUser relayUser : serializedRelayUsers)
        {
            if (relayUser.isUsstaff() || relayUser.isGoogle() ||
                    authoritativeUsers.contains(relayUser.getUsername().toLowerCase())) {
                relayUsers.add(relayUser);
            }
        }

        logger.info("Authoritative size is " + relayUsers.size());

        logger.info("Authenticating ...");

        AuthenticationService authenticationService = new AuthenticationService(migrationProperties.getNonNullProperty
                ("theKeyLdapHost"), migrationProperties.getNonNullProperty("theKeyMergeUserRootDn"), "cn");

        AuthenticationService.Results results = authenticationService.authenticate(relayUsers);

        File successAuthenticationFile =
                FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("successAuthentication"));
        File failedAuthenticationFile =
                FileHelper.getFileToWrite(migrationProperties.getNonNullProperty("failedAuthentication"));

        Output.logMessage(results.successAuthentication, successAuthenticationFile);
        Output.logMessage(results.failedAuthentication, failedAuthenticationFile);
    }

    public void determineMergedAccountPasswordStatus() throws Exception
    {
        Set<RelayUser> relayUsers = Sets.newHashSet();
        Set<RelayUser> serializedRelayUsers = Output.deserializeRelayUsers(
                migrationProperties.getNonNullProperty("serializedRelayUsers"));

        File mergedUsersFile = FileHelper.getFileToRead(migrationProperties.getNonNullProperty("mergedUsers"));

        List<String> mergedUsers = Files.readLines(mergedUsersFile, Charsets.UTF_8);

        logger.info("collecting merged user passwords for merged users " + mergedUsers.size() + " ...");
        int counter = 0;
        for(String mergedUser : mergedUsers) {
            try {
                String[] split = mergedUser.split(",");
                String relayUsername = split[0].toLowerCase();
                String keyUsername = split[1].toLowerCase();

                RelayUser serializedRelayUser = RelayUser.havingUsername(serializedRelayUsers, relayUsername);
                if (serializedRelayUser != null) {
                    RelayUser relayUser = new RelayUser();
                    relayUser.setUsername(keyUsername);
                    relayUser.setPassword(serializedRelayUser.getPassword());
                    relayUsers.add(relayUser);
                } else {
                    System.out.println("no serialized user found for relay username " + relayUsername);
                }
            } catch (Exception e) {
                System.out.println("exception for merged user " + mergedUser + " is " + e.getMessage());
            }

            if (counter++ % 1000 == 0) {
                System.out.printf("Merged users " + counter + "\r");
            }
        }

        logger.info("After iterating through " + counter + " merged users");
        logger.info("Authenticating " + relayUsers.size() + " relay users ...");

        AuthenticationService authenticationService = new AuthenticationService(
                migrationProperties.getNonNullProperty("theKeyLdapHost"),
                migrationProperties.getNonNullProperty("theKeySourceUserRootDn"),
                "cn");

        AuthenticationService.Results results = authenticationService.authenticate(relayUsers);

        logger.info("authentication results " + results.successAuthentication.size() + "," + results
                .failedAuthentication.size());

//        logger.info("success users ..._");
//        for(String username : results.successAuthentication) {
//            logger.info("success users " + username);
//        }
//
//        logger.info("failed users ..._");
//        for(String username : results.failedAuthentication) {
//            logger.info("failed users " + username);
//        }
//
//        logger.info("failed reason users ..._");
//        for(String username : results.failedAuthenticationReason) {
//            logger.info("failed users " + username);
//        }

        File mergedUsersPasswordStateFile = FileHelper.getFileToWrite(migrationProperties.getNonNullProperty
                ("mergedUsersPasswordState"));

        Set<String> mergedUsersPasswordStateSet = Sets.newHashSet();

        counter = 0;
        for(String mergedUser : mergedUsers)
        {
            try {
                String[] split = mergedUser.split(",");
                String relayUsername = split[0].toLowerCase();
                String keyUsername = split[1].toLowerCase();
                String mergedUsername = split[2].toLowerCase();
                String usernamesMatch = split[3];
                String sourceSystem = split[4];

                String passwordState = "Unknown";

                if (results.successAuthentication.contains(keyUsername)) {
                    passwordState = "Yes";
                } else if (results.failedAuthentication.contains(keyUsername)) {
                    passwordState = "No";
                }

                mergedUsersPasswordStateSet.add(
                        relayUsername + "," + keyUsername + "," + passwordState + "," + sourceSystem);

                if (counter++ % 1000 == 0) {
                    System.out.printf("Authenticated users " + counter + "\r");
                }
            } catch (Exception e) {
                System.out.println("exception authenticating user " + mergedUser + " is " + e.getMessage());
            }
        }

        Output.logMessage(mergedUsersPasswordStateSet, mergedUsersPasswordStateFile);

        logger.info("Done authenticating");
    }


    public void removeMergeUserDn() throws Exception
	{
		logger.info("remove merge users. getting merge entries ... ");

		Map<String, Attributes> entries = theKeyLdap.getMergeEntries();

		logger.info("remove merge user dn: got entries count " + entries.size());

		theKeyLdap.removeEntries(entries);

		logger.info("finished removing entries");
	}

	public void getTheKeyProvisionedUserCount() throws Exception
    {
        logger.info("the key user count " + theKeyLdap.getMergeUserCount());
    }

	public void createCruGroups() throws Exception
    {
        logger.info("create cru groups ");
        theKeyLdap.createGroups();
    }

	/*
    	This method does not take into account users provisioned whose usernames were changed.
	 */
    public void verifyProvisionedUsers() throws Exception
    {
        Set<RelayUser> relayUsersProvisioned = Output.deserializeRelayUsers(migrationProperties.getNonNullProperty("relayUsersProvisioned"));

        logger.info("relay users provisioned is " + relayUsersProvisioned.size());

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
                User gcxUser = theKeyLdap.getUser(relayUser.getUsername());

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

        logger.info("provisioned " + provisioned);
        logger.info("not provisioned " + notProvisioned);
        logger.info("total " + (notProvisioned + provisioned));
    }

    public void test() throws Exception
    {
//        String string = "RELAY:shawnee.marie@uscm.org, KEY:Shawnee.Marie@uscm.org, MERGED:shawnee.marie@uscm.org:";
//
//        String[] split = string.split(", ");
//        String relay = split[0].split(":")[1];
//        String key = split[1].split(":")[1];
//        String merged = split[2].split(":")[1];
//        System.out.println(relay + ":");
//        System.out.println(key + ":");
//        System.out.println(merged + ":");
//
		List<String> list = Lists.newArrayList();
		list.add("smtp:asdf");
		list.add("SMTP:asasddf");
		list.add("smTp:asd123f");
		list.add("smp:axcvsdf");
		list.add("smTP:axcvsdf");

		for(String string : list) {
			logger.info(string);
		}

		logger.info("   ");

		for(String string : removePrefix(list, "smtp:")) {
			logger.info(string);
		}

	}

    public void test2() throws Exception
    {
        Set<RelayUser> serializedRelayUsers = Output.deserializeRelayUsers(
                migrationProperties.getNonNullProperty("serializedRelayUsers"));

        List<String> users = Arrays.asList();

        for(String user : users)
        {
            RelayUser relayUser = RelayUser.havingUsername(serializedRelayUsers, user);
            logger.info("password length " + relayUser.getPassword().length());
        }
    }

    enum Action
	{
		SystemEntries, USStaff, GoogleUsers, USStaffAndGoogleUsers, CreateUser, Test, ProvisionUsers,
		RemoveAllKeyMergeUserEntries, GetTheKeyProvisionedUserCount, VerifyProvisionedUsers,
		CreateCruPersonAttributes,
		CreateCruPersonObjectClass, CreateRelayAttributes, CreateRelayAttributesObjectClass, DeleteCruPersonAttributes,
        CreateCruGroups, AuthenticateRelayUsers, AuthenticateRelayUsersAgainstKey, LoggedInSince,
		KeyUserCount, UpdateKeyFromRelay, CheckStaff
	}

	public static void main(String[] args) throws Exception
	{
        Migration migration = new Migration();

		Logger logger = LoggerFactory.getLogger(Migration.class);

        DateTime start = DateTime.now();
		logger.debug("start time " + start);

		try
		{
			Action action = Action.UpdateKeyFromRelay;

            if (action.equals(Action.CreateCruGroups))
            {
                migration.createCruGroups();
            }
			else if (action.equals(Action.CheckStaff))
			{
				migration.checkStaff();
			}
			else if (action.equals(Action.Test))
			{
				migration.test();
			}
			else if (action.equals(Action.KeyUserCount))
			{
				migration.getTheKeyLegacyUserCount();
			}
			else if (action.equals(Action.UpdateKeyFromRelay))
			{
				migration.updateKeyUserFromRelay();
			}
			else if (action.equals(Action.LoggedInSince))
			{
				migration.loggedInSince();
			}
			else if (action.equals(Action.SystemEntries))
			{
				migration.createSystemEntries();
			}
            else if (action.equals(Action.AuthenticateRelayUsers))
            {
                migration.authenticateRelayUsers();
            }
            else if (action.equals(Action.AuthenticateRelayUsersAgainstKey))
            {
                migration.determineMergedAccountPasswordStatus();
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
			else if (action.equals(Action.RemoveAllKeyMergeUserEntries))
			{
				migration.removeMergeUserDn();
			}
			else if (action.equals(Action.ProvisionUsers))
			{
				migration.provisionUsers();
			}
			else if (action.equals(Action.CreateCruPersonObjectClass))
			{
				migration.createCruPersonObject(true);
			}
			else if (action.equals(Action.CreateCruPersonAttributes))
			{
				migration.createCruPersonAttributes(true);
			}
			else if (action.equals(Action.DeleteCruPersonAttributes))
			{
				migration.deleteCruPersonAttributes();
			}
			else if (action.equals(Action.CreateRelayAttributesObjectClass))
			{
				migration.createRelayObject(true);
			}
			else if (action.equals(Action.CreateRelayAttributes))
			{
				migration.createRelayAttributes(true);
			}
			else if (action.equals(Action.GetTheKeyProvisionedUserCount))
			{
				migration.getTheKeyProvisionedUserCount();
			}
            else if (action.equals(Action.VerifyProvisionedUsers))
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

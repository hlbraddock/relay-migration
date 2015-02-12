package org.cru.migration.service;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.User;
import org.ccci.idm.user.exception.RelayGuidAlreadyExistsException;
import org.ccci.idm.user.exception.TheKeyGuidAlreadyExistsException;
import org.ccci.idm.user.exception.UserAlreadyExistsException;
import org.ccci.idm.user.UserManager;
import org.ccci.idm.user.ldaptive.dao.io.GroupValueTranscoder;
import org.ccci.idm.user.migration.MigrationUserDao;
import org.ccci.idm.user.migration.MigrationUserManager;
import org.cru.migration.domain.MatchingUsers;
import org.cru.migration.domain.RelayGcxUsers;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;
import org.cru.migration.support.FileHelper;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Misc;
import org.cru.migration.support.Output;
import org.cru.migration.support.StringUtilities;
import org.cru.migration.thekey.GcxUserService;
import org.cru.migration.thekey.TheKeyBeans;
import org.cru.silc.service.LinkingServiceImpl;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.ldaptive.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ProvisionUsersService
{
    private MigrationProperties properties;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private MigrationUserManager userManagerMerge;

    private GcxUserService gcxUserService;

    Set<RelayUser> relayUsersProvisioned = Sets.newSetFromMap(new ConcurrentHashMap<RelayUser, Boolean>());
    Set<User> gcxUsersProvisioned = Sets.newSetFromMap(new ConcurrentHashMap<User, Boolean>());
    Map<RelayUser, Exception> relayUsersFailedToProvision = Maps.newConcurrentMap();
    Map<User, Exception> gcxUsersFailedToProvision = Maps.newConcurrentMap();
    Map<RelayUser, Exception> relayUsersFailedToProvisionGroup = Maps.newConcurrentMap();
    Map<RelayUser, User> matchingRelayGcxUsers = Maps.newConcurrentMap();
    Set<RelayUser> relayUsersMatchedMoreThanOneGcxUser = Sets.newSetFromMap(new ConcurrentHashMap<RelayUser,
            Boolean>());
    Set<RelayUser> relayUsersWithNewSsoguid = Sets.newSetFromMap(new ConcurrentHashMap<RelayUser, Boolean>());

    Set<RelayGcxUsers> relayUsersWithGcxUsersMatchedMoreThanOneGcxUser = Sets.newSetFromMap(new
            ConcurrentHashMap<RelayGcxUsers, Boolean>());

    Set<RelayGcxUsers> relayUsersWithGcxMatchAndGcxUsers = Sets.newSetFromMap(new
            ConcurrentHashMap<RelayGcxUsers, Boolean>());

    Set<RelayGcxUsers> userAlreadyExists = Sets.newSetFromMap(new
            ConcurrentHashMap<RelayGcxUsers, Boolean>());
    Set<RelayGcxUsers> theKeyGuidUserAlreadyExists = Sets.newSetFromMap(new
            ConcurrentHashMap<RelayGcxUsers, Boolean>());
    Set<RelayGcxUsers> relayGuidUserAlreadyExists = Sets.newSetFromMap(new
            ConcurrentHashMap<RelayGcxUsers, Boolean>());

    Boolean provisionUsers;
    Boolean provisioningFailureStackTrace;
    Boolean logProvisioningRealTime;
    Integer provisionUsersLimit;
    Boolean provisionGroups;

    File provisioningRelayUsersFile;
    File failingProvisioningRelayUsersFile;
    File userProvisionState;
    File usernameChanges;
    File mergedUsers;
    File authoritativeUsers;

    private Set<String> usernameChangesSet = Sets.newConcurrentHashSet();
    private Set<String> mergedUsersSet = Sets.newConcurrentHashSet();
    private Set<String> userProvisionStateSet = Sets.newConcurrentHashSet();
    private Set<String> authoritativeUsersSet = Sets.newConcurrentHashSet();

    private GroupValueTranscoder groupValueTranscoder;

    private MigrationUserDao migrationUserDaoMerge;

    String sourceGroupRootDn;
    String targetGroupRootDn;

    public ProvisionUsersService(MigrationProperties properties) throws Exception
    {
        this.properties = properties;

        sourceGroupRootDn = properties.getNonNullProperty("relayGroupRootDn");
        targetGroupRootDn = properties.getNonNullProperty("theKeyGroupRootDn").toLowerCase();


        Boolean eDirectoryAvailable = Boolean.valueOf(properties.getNonNullProperty("eDirectoryAvailable"));
        UserManager userManager = null;
        if(eDirectoryAvailable)
        {
            userManager = TheKeyBeans.getUserManager();
            userManagerMerge = TheKeyBeans.getUserManagerMerge();
            migrationUserDaoMerge = TheKeyBeans.getUserDaoMerge();
            groupValueTranscoder = TheKeyBeans.getGroupValueTranscoder();
        }

        LinkingServiceImpl linkingServiceImpl = new LinkingServiceImpl();
        linkingServiceImpl.setResource(properties.getNonNullProperty("identityLinkingResource"));
        linkingServiceImpl.setIdentitiesAccessToken(properties.getNonNullProperty("identityLinkingAccessToken"));

        gcxUserService = new GcxUserService(userManager, linkingServiceImpl);

        provisionGroups = Boolean.valueOf(properties.getNonNullProperty("provisionGroups"));
        userProvisionState = FileHelper.getFileToWrite(properties.getNonNullProperty
                ("userProvisionState"));

        provisionUsers = Boolean.valueOf(properties.getNonNullProperty("provisionUsers"));
        provisioningFailureStackTrace = Boolean.valueOf(properties.getNonNullProperty
                ("provisioningFailureStackTrace"));
        logProvisioningRealTime = Boolean.valueOf(properties.getNonNullProperty
                ("logProvisioningRealTime"));
        provisionUsersLimit = Integer.valueOf(properties.getNonNullProperty
                ("provisionUsersLimit"));

        provisioningRelayUsersFile = FileHelper.getFileToWrite(properties.getNonNullProperty("relayUsersProvisioning"));
        failingProvisioningRelayUsersFile = FileHelper.getFileToWrite(properties.getNonNullProperty
                ("relayUsersFailingProvisioning"));

        usernameChanges = FileHelper.getFileToWrite(properties.getNonNullProperty("usernameChanges"));
        mergedUsers = FileHelper.getFileToWrite(properties.getNonNullProperty("mergedUsers"));
        authoritativeUsers = FileHelper.getFileToWrite(properties.getNonNullProperty("authoritativeUsers"));

		mergedUsersSet.add("Relay,the Key,Merged,Usernames Match,Source");
    }

    private class ProvisionUsersData
    {
        private AtomicInteger counter = new AtomicInteger(0);
        private Set<RelayUser> relayUsers;
        private Map<String, Set<RelayUser>> keyUserMatchingRelayUsers;

        public ProvisionUsersData(Set<RelayUser> relayUsers, Map<String, Set<RelayUser>> keyUserMatchingRelayUsers)
                throws LdapException
        {
            this.relayUsers = relayUsers;
            this.keyUserMatchingRelayUsers = keyUserMatchingRelayUsers;
        }

        public void incrementCounter()
        {
            counter.getAndAdd(1);
        }

        public AtomicInteger getCounter()
        {
            return counter;
        }

        public Set<RelayUser> getRelayUsers()
        {
            return relayUsers;
        }

        public Map<String, Set<RelayUser>> getKeyUserMatchingRelayUsers()
        {
            return keyUserMatchingRelayUsers;
        }
    }

    public void provisionUsers(Set<RelayUser> relayUsers, Map<String, Set<RelayUser>> keyUserMatchingRelayUsers) throws
            NamingException, LdapException
    {
        logger.info("provisioning relay users to the key of size " + relayUsers.size());

        Long totalProvisioningTime = 0L;
        DateTime start = DateTime.now();

        ExecutionService executionService = new ExecutionService();

        ProvisionUsersData provisionUsersData = new ProvisionUsersData(relayUsers, keyUserMatchingRelayUsers);

        executionService.execute(new ProvisionUsers(), provisionUsersData, 50);

        Integer counter = provisionUsersData.getCounter().get();
        totalProvisioningTime += (new Duration(start, DateTime.now())).getMillis();
        if(counter > 0)
        {
            logger.info("provisioned " + counter + " users at an average milliseconds of (" +
                    totalProvisioningTime + "/" + counter + ")" + totalProvisioningTime / counter + " per user " +
                    "and a total of " + StringUtilities.toString(new Duration(totalProvisioningTime)));
        }

        logger.info("provisioning relay users to the key done ");

        try
        {
            Output.serializeRelayUsers(relayUsersProvisioned,
                    properties.getNonNullProperty("relayUsersProvisioned"));

            Output.logMessage(usernameChangesSet, usernameChanges);
            Output.logMessage(mergedUsersSet, mergedUsers);
            Output.logMessage(userProvisionStateSet, userProvisionState);
            Output.logMessage(authoritativeUsersSet, authoritativeUsers);

            Output.logGcxUsers(gcxUsersProvisioned,
                    FileHelper.getFileToWrite(properties.getNonNullProperty("gcxUsersProvisioned")));
            Output.serializeRelayUsers(relayUsersFailedToProvision,
                    properties.getNonNullProperty("relayUsersFailedToProvision"));
            Output.logGcxUsers(gcxUsersFailedToProvision,
                    FileHelper.getFileToWrite(properties.getNonNullProperty("gcxUsersFailedToProvision")));
            Output.serializeRelayUsers(relayUsersFailedToProvisionGroup,
                    properties.getNonNullProperty("relayUsersFailedToProvisionGroup"));
            Output.logRelayGcxUsersMap(matchingRelayGcxUsers,
                    properties.getNonNullProperty("matchingRelayGcxUsers"));

            logger.info("Size of relayUsersMatchedMoreThanOneGcxUser " +
                    relayUsersMatchedMoreThanOneGcxUser.size());
            Output.serializeRelayUsers(relayUsersMatchedMoreThanOneGcxUser,
                    properties.getNonNullProperty("relayUsersMatchedMoreThanOneGcxUser"));

            logger.info("Size of relayUsersWithGcxUsersMatchedMoreThanOneGcxUser " +
                    relayUsersWithGcxUsersMatchedMoreThanOneGcxUser.size());
            Output.logRelayUserGcxUsers(relayUsersWithGcxUsersMatchedMoreThanOneGcxUser,
                    FileHelper.getFileToWrite(properties.getNonNullProperty
                            ("relayUsersWithGcxUsersMatchedMoreThanOneGcxUser")));

            logger.info("Size of userAlreadyExists " + userAlreadyExists.size());
            Output.logRelayUserGcxUsers(userAlreadyExists,
                    FileHelper.getFileToWrite(properties.getNonNullProperty("userAlreadyExists")));
            logger.info("Size of theKeyGuidUserAlreadyExists " + theKeyGuidUserAlreadyExists.size());
            Output.logRelayUserGcxUsers(theKeyGuidUserAlreadyExists,
                    FileHelper.getFileToWrite(properties.getNonNullProperty("theKeyGuidUserAlreadyExists")));
            logger.info("Size of relayGuidUserAlreadyExists " + relayGuidUserAlreadyExists.size());
            Output.logRelayUserGcxUsers(relayGuidUserAlreadyExists,
                    FileHelper.getFileToWrite(properties.getNonNullProperty("relayGuidUserAlreadyExists")));

            logger.info("Size of relayUsersWithGcxMatchAndGcxUsers " +
                    relayUsersWithGcxMatchAndGcxUsers.size());
            Output.logRelayUserGcxUsers(relayUsersWithGcxMatchAndGcxUsers,
                    FileHelper.getFileToWrite(properties.getNonNullProperty("relayUsersWithGcxMatchAndGcxUsers")));

            Output.serializeRelayUsers(relayUsersWithNewSsoguid,
                    properties.getNonNullProperty("relayUsersWithNewSsoguid"));
        }
        catch(Exception e)
        {
            logger.error("Caught final exception " + e);
            e.printStackTrace();
        }
    }

    public class ProvisionUsers implements ExecuteAction
    {
        @Override
        public void execute(ExecutorService executorService, Object object)
        {
            ProvisionUsersData provisionUsersData = (ProvisionUsersData) object;

            for(RelayUser relayUser : provisionUsersData.getRelayUsers())
            {
                executorService.execute(
                        new ProvisionUsersWorkerThread(relayUser, provisionUsersData.getKeyUserMatchingRelayUsers()));

                provisionUsersData.incrementCounter();
                if(provisionUsersLimit > -1 && (provisionUsersData.getCounter().get() >= provisionUsersLimit))
                {
                    break;
                }
            }
        }
    }

    private class ProvisionUsersWorkerThread implements Runnable
    {
        private RelayUser relayUser;
        private Map<String, Set<RelayUser>> keyUserMatchingRelayUsers;

        public ProvisionUsersWorkerThread(RelayUser relayUser, Map<String, Set<RelayUser>> keyUserMatchingRelayUsers)
        {
            this.relayUser = relayUser;
            this.keyUserMatchingRelayUsers = keyUserMatchingRelayUsers;
        }

        @Override
        public void run()
        {
            User user = new User();

            ResolveData resolveData = new ResolveData();

            try
            {
                resolveData = resolveUser(relayUser);

                user = resolveData.gcxUser;

                // ensure valid ssoguid
                String validRelayUserSsoguid =
                        Misc.isValidUUID(relayUser.getSsoguid()) ? relayUser.getSsoguid() : UUID.randomUUID().toString();

                // if matching key user found
                if(user != null)
                {
                    User originalMatchedKeyUser = user.clone();

                    Boolean relayAuthoritative = Boolean.FALSE;

                    relayUsersWithGcxMatchAndGcxUsers.
                            add(new RelayGcxUsers(relayUser, user, resolveData.matchingUsers.toSet(), resolveData
                                    .matchResult));
                    matchingRelayGcxUsers.put(relayUser, user);

                    // set necessary user data
                    user.setGuid(validRelayUserSsoguid);
                    user.setRelayGuid(validRelayUserSsoguid);
                    relayUser.setUserFromRelayAttributes(user);

                    Boolean mostRecentLoginToRelay = true;
                    if(relayUser.getLastLogonTimestamp() != null && user.getLoginTime() != null)
                    {
                        mostRecentLoginToRelay = relayUser.getLastLogonTimestamp().isAfter(user.getLoginTime());
                    }

                    if(relayUser.isAuthoritative() || mostRecentLoginToRelay)
                    {
                        relayUser.setUserFromRelayIdentity(user);
                        relayAuthoritative = Boolean.TRUE;
                        if(!Strings.isNullOrEmpty(relayUser.getUsername()))
                        {
                            authoritativeUsersSet.add(relayUser.getUsername());
                        }
                    }

                    // Manage user when the Key account matches multiple Relay users
                    ManageResult manageResult = manageKeyUserWhenMatchesMultipleRelayUsers(user, originalMatchedKeyUser,
                            gcxUserService.getGcxUserFromRelayUser(relayUser, validRelayUserSsoguid), relayUser);
                    user = manageResult.user != null ? manageResult.user : user;

					if(manageResult.user == null || !manageResult.newUser)
					{
						if(!relayUser.getUsername().equalsIgnoreCase(originalMatchedKeyUser.getEmail()))
						{
							usernameChangesSet.add("RELAY:" + relayUser.getUsername() + ", KEY:" +
									originalMatchedKeyUser.getEmail() + ", MERGED TO " +
									(relayAuthoritative ? "RELAY " : "THEKEY ") + "USERNAME:" +
									user.getEmail() + ":");
						}

						moveAndMergeKeyUser(user, originalMatchedKeyUser.clone(), relayAuthoritative);

						mergedUsersSet.add(
								relayUser.getUsername() + "," +
								originalMatchedKeyUser.getEmail() + "," +
								user.getEmail() + "," +
								(relayUser.getUsername().equalsIgnoreCase(originalMatchedKeyUser.getEmail())) + "," +
								(relayAuthoritative ? "Relay" : "the Key")
						);
					}
					else
					{
						createUser(user);
					}
                }

                // no matching the key user
                else
                {
                    if(!validRelayUserSsoguid.equals(relayUser.getSsoguid()))
                        relayUsersWithNewSsoguid.add(relayUser);

                    user = gcxUserService.getGcxUserFromRelayUser(relayUser, validRelayUserSsoguid);

                    createUser(user);
                }

                if(provisionUsers)
                {
                    gcxUsersProvisioned.add(user);
                    relayUsersProvisioned.add(relayUser);

                    // Provision group membership
                    if(provisionGroups)
                    {
                        provisionGroup(relayUser, user);
                    }
                }
            }
            catch(TheKeyGuidAlreadyExistsException theKeyGuidAlreadyExistsException)
            {
                theKeyGuidUserAlreadyExists.add(new RelayGcxUsers(relayUser, user, resolveData.matchingUsers.toSet(),
                        resolveData.matchResult));
            }
            catch(RelayGuidAlreadyExistsException relayGuidAlreadyExistsException)
            {
                relayGuidUserAlreadyExists.add(new RelayGcxUsers(relayUser, user, resolveData.matchingUsers.toSet(), resolveData.matchResult));
            }
            catch(UserAlreadyExistsException userAlreadyExistsException)
            {
                userAlreadyExists.add(new RelayGcxUsers(relayUser, user, resolveData.matchingUsers.toSet(), resolveData.matchResult));
            }
            catch(Exception e)
            {
                relayUsersFailedToProvision.put(relayUser, e);
                if(user != null)
                {
                    gcxUsersFailedToProvision.put(user, e);
                }
                Output.logMessage(StringUtils.join(relayUser.toList(), ",") + " " + e.getMessage() + "," +
                        e.getCause() + "," + e.toString(), failingProvisioningRelayUsersFile);
                if(provisioningFailureStackTrace)
                {
                    e.printStackTrace();
                }
            }
        }

        private void createUser(User user) throws org.ccci.idm.user.exception.UserException
        {
            userProvisionStateSet.add("CREATE: " + user.toString() + "," + user.getPassword());

			if(provisionUsers)
			{
				userManagerMerge.createUser(user);
			}
        }

        private void moveAndMergeKeyUser(User user, User originalMatchedKeyUser, Boolean relayAuthoritative) throws Exception
        {
            try
            {
				if(provisionUsers)
				{
					userManagerMerge.moveLegacyKeyUser(originalMatchedKeyUser, user.getEmail());
				}
            }
            catch(Exception e)
            {
                logger.error("Problem moving key user " + originalMatchedKeyUser.getEmail() + " to email "
                        + user.getEmail(), e);
                throw e;
            }

            // set the primary guid
            originalMatchedKeyUser.setGuid(user.getGuid());
			if(provisionUsers)
			{
				migrationUserDaoMerge.updateGuid(originalMatchedKeyUser);
			}

            // set the attributes you want to update
            User.Attr attributes[] = new User.Attr[]{User.Attr.RELAY_GUID};
            if(relayAuthoritative)
            {
                attributes = new User.Attr[]{User.Attr.RELAY_GUID, User.Attr.CRU_PERSON,
                        User.Attr.PASSWORD, User.Attr.NAME, User.Attr.LOCATION};
            }

            userProvisionStateSet.add("MERGE: " + user.toString() + "," +
                            (relayAuthoritative ? user.getPassword() : "KEY**PASSWORD"));

            // update moved key user with appropriate attributes, updateUser() finds by guid
			if(provisionUsers)
			{
				userManagerMerge.updateUser(user, attributes);
			}
        }

        private class ResolveData
        {
            private User gcxUser = new User();
            private MatchingUsers matchingUsers = new MatchingUsers();
            private GcxUserService.MatchResult matchResult = new GcxUserService.MatchResult();

            public ResolveData()
            {
            }

            public ResolveData(User gcxUser, MatchingUsers matchingUsers, GcxUserService.MatchResult matchResult)
            {
                this.gcxUser = gcxUser;
                this.matchingUsers = matchingUsers;
                this.matchResult = matchResult;
            }
        }

        private ResolveData resolveUser(RelayUser relayUser)
        {
            GcxUserService.MatchResult matchResult = new GcxUserService.MatchResult();
            MatchingUsers matchingUsers = gcxUserService.findGcxUsers(relayUser, matchResult);
            User user = gcxUserService.resolveGcxUser(matchResult, matchingUsers);

            if(matchResult.multiples())
            {
                relayUsersMatchedMoreThanOneGcxUser.add(relayUser);
                relayUsersWithGcxUsersMatchedMoreThanOneGcxUser.add(new RelayGcxUsers(relayUser,
                        matchingUsers.toSet(), new GcxUserService.MatchDifferentGcxUsersException(matchResult
                        .matchType.toString())));
            }

            return new ResolveData(user, matchingUsers, matchResult);
        }

        private ManageResult manageKeyUserWhenMatchesMultipleRelayUsers(User gcxUser, User originalMatchingKeyUser, User
                possibleNewUser, RelayUser relayUser)
        {
            Set<RelayUser> relayUsersMatchingKeyUser = keyUserMatchingRelayUsers.get(originalMatchingKeyUser.getTheKeyGuid());

            if(relayUsersMatchingKeyUser != null)
            {
                return manageKeyUserWhenMatchesMultipleRelayUsers(gcxUser, originalMatchingKeyUser, possibleNewUser,
                        relayUser, relayUsersMatchingKeyUser);
            }

            return new ManageResult();
        }

        private class ManageResult
        {
            User user = null;
            Boolean newUser = Boolean.FALSE;
            User.Attr attributes[] = new User.Attr[]{};

            public ManageResult()
            {
            }
        }

        // handle when multiple relay accounts match one key account
        private ManageResult manageKeyUserWhenMatchesMultipleRelayUsers(User gcxUser, User originalMatchingKeyUser,
                                                                        User possibleNewUser, RelayUser relayUser,
                                                                Set<RelayUser> relayUsersMatchingKeyUser)
        {
            RelayUser relayUserMatchingEmail = null;
            RelayUser relayUserMatchingSsoguid = null;
            RelayUser relayUserMatchingLink = null;

            // Determine what type matches on relay accounts you have
            for(RelayUser relayUserMatchingKeyUser : relayUsersMatchingKeyUser)
            {
                if(relayUserMatchingKeyUser.getUsername().equalsIgnoreCase(originalMatchingKeyUser.getEmail()))
                {
                    relayUserMatchingEmail = relayUserMatchingKeyUser;
                }
                else if(relayUserMatchingKeyUser.getSsoguid().equalsIgnoreCase(originalMatchingKeyUser.getTheKeyGuid()))
                {
                    relayUserMatchingSsoguid = relayUserMatchingKeyUser;
                }
                else
                {
                    relayUserMatchingLink = relayUserMatchingKeyUser;
                }
            }

            // modify the key user depending on the type of relay user match

            /**
             * Use case: Matching one relay account on guid and another on email

             * For example:

             relay accounts:
             82900761-8729-66CB-3BFE-DCA7906710DA / TRAVIS.GARRISON@USCM.ORG
             A56D871D-C828-4534-B251-B8B2FADBB4BB / TRAVIS@SHARONANDTRAVIS.ORG

             thekey accounts:
             82900761-8729-66CB-3BFE-DCA7906710DA / travis@sharonandtravis.org
             no account for travis.garrison@uscm.org

             MERGE:
             email / authoritative common guid / relay / thekey
             travis@sharonandtravis.org / A56D871D-C828-4534-B251-B8B2FADBB4BB / A56D871D-C828-4534-B251-B8B2FADBB4BB / 82900761-8729-66CB-3BFE-DCA7906710DA
             TRAVIS.GARRISON@USCM.ORG / (new guid) / 82900761-8729-66CB-3BFE-DCA7906710DA / (none)
             */

            ManageResult manageResult = new ManageResult();

            // if this key account matches relay accounts by email and guid
            if(relayUserMatchingEmail != null && relayUserMatchingSsoguid != null)
            {
                // if the current relay user is the one matching the key by email
                if(relayUser.getUsername().equalsIgnoreCase(originalMatchingKeyUser.getEmail()))
                {
                    gcxUser.setGuid(gcxUser.getRawRelayGuid());

                    manageResult.user = gcxUser;
                }

                // if the current relay user is the one matching the key by guid
                else if(relayUser.getSsoguid().equalsIgnoreCase(originalMatchingKeyUser.getTheKeyGuid()))
                {
                    possibleNewUser.setGuid(UUID.randomUUID().toString());
                    possibleNewUser.setEmail(relayUser.getUsername());

                    manageResult.user = possibleNewUser;
                    manageResult.newUser = true;
                }
            }
            else
            {
                // TODO handle these edge cases as well.
            }

            return manageResult;
        }
    }

    private void provisionGroup(RelayUser relayUser, User gcxUser)
    {
        if(logger.isTraceEnabled())
        {
            int size = -1;
            if(relayUser.getMemberOf() != null)
                size = relayUser.getMemberOf().size();

            logger.trace("relay user " + relayUser.getUsername() + " member of size " + size + " member of " + relayUser
                    .getMemberOf());

            logger.trace("group base dn " + groupValueTranscoder.getBaseDn());
        }

        if(relayUser.getMemberOf() != null)
        {
            for(String groupDn : relayUser.getMemberOf())
            {
                if(!isValidGroup(groupDn))
                {
                    continue;
                }

                try
                {
                    groupDn = relayToTheKeyGroupDn(groupDn);

                    // if the user is member of a google group, we consider their email verified
                    if(!gcxUser.isEmailVerified() && groupDn.toLowerCase().contains("googleapps"))
                    {
                        gcxUser.setEmailVerified(true);

                        User.Attr attributes[] = new User.Attr[]{User.Attr.EMAIL};

						if(provisionGroups)
						{
							userManagerMerge.updateUser(gcxUser, attributes);
						}
                    }

                    Group group = groupValueTranscoder.decodeStringValue(groupDn);

                    if(logger.isTraceEnabled())
                    {
                        logger.trace("group dn : " + groupDn + " and path " + StringUtils.join(group.getPath()) +
                                " and name " + group.getName());
                    }

					if(provisionGroups)
					{
						userManagerMerge.addToGroup(gcxUser, group);
					}
                }
                catch(Exception e)
                {
                    relayUsersFailedToProvisionGroup.put(relayUser, e);
                }
            }
        }
    }

    private String relayToTheKeyGroupDn(String groupDn)
    {
        return groupDn.replace(sourceGroupRootDn, targetGroupRootDn).
                replaceAll("CN=", "ou=").replaceAll("cn=", "ou=").replaceFirst("ou=", "cn=").replaceAll("DC=", "dc=");
    }

    private static final List<String> validGroupNames =
            Arrays.asList("cn=Stellent", "cn=GoogleApps");

    private Boolean isValidGroup(String groupDn)
    {
        for(String groupName : validGroupNames)
        {
            if(groupDn.toLowerCase().contains(groupName.toLowerCase()))
            {
                return true;
            }
        }

        return false;
    }

    private void logDuration(DateTime start, String message)
    {
        Duration duration = new Duration(start, DateTime.now());

        logger.trace(message + StringUtilities.toString(duration));
    }
}

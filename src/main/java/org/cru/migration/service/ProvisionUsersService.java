package org.cru.migration.service;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ProvisionUsersService
{
    private MigrationProperties properties;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private UserManager userManagerMerge;

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

    File provisioningRelayUsersFile;
    File failingProvisioningRelayUsersFile;

    private GroupValueTranscoder groupValueTranscoder;

    public ProvisionUsersService(MigrationProperties properties) throws Exception
    {
        this.properties = properties;

        Boolean eDirectoryAvailable = Boolean.valueOf(properties.getNonNullProperty("eDirectoryAvailable"));
        UserManager userManager = null;
        if(eDirectoryAvailable)
        {
            userManager = TheKeyBeans.getUserManager();
            userManagerMerge = TheKeyBeans.getUserManagerMerge();
            groupValueTranscoder = TheKeyBeans.getGroupValueTranscoder();
        }

        LinkingServiceImpl linkingServiceImpl = new LinkingServiceImpl();
        linkingServiceImpl.setResource(properties.getNonNullProperty("identityLinkingResource"));
        linkingServiceImpl.setIdentitiesAccessToken(properties.getNonNullProperty("identityLinkingAccessToken"));

        gcxUserService = new GcxUserService(userManager, linkingServiceImpl);

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
    }

    private class ProvisionUsersData
    {
        private AtomicInteger counter;
        private Set<RelayUser> relayUsers;
        private Map<User, Set<RelayUser>> keyUserMatchingRelayUsers;
        private Set<User> keyUsersMatching;

        private ProvisionUsersData(AtomicInteger counter, Set<RelayUser> relayUsers, Map<User, Set<RelayUser>> keyUserMatchingRelayUsers, Set<User> keyUsersMatching)
        {
            this.counter = counter;
            this.relayUsers = relayUsers;
            this.keyUserMatchingRelayUsers = keyUserMatchingRelayUsers;
            this.keyUsersMatching = keyUsersMatching;
        }

        public AtomicInteger getCounter()
        {
            return counter;
        }

        public void incrementCounter()
        {
            counter.getAndAdd(1);
        }

        public Set<RelayUser> getRelayUsers()
        {
            return relayUsers;
        }

        public Map<User, Set<RelayUser>> getKeyUserMatchingRelayUsers()
        {
            return keyUserMatchingRelayUsers;
        }

        public Set<User> getKeyUsersMatching()
        {
            return keyUsersMatching;
        }
    }

    public void provisionUsers(Set<RelayUser> relayUsers, Map<User, Set<RelayUser>> keyUserMatchingRelayUsers) throws NamingException
    {
        logger.info("provisioning relay users to the key of size " + relayUsers.size());

        Long totalProvisioningTime = 0L;
        DateTime start = DateTime.now();

        ExecutionService executionService = new ExecutionService();

        ProvisionUsersData provisionUsersData =
                new ProvisionUsersData(new AtomicInteger(0), relayUsers, keyUserMatchingRelayUsers,
                        keyUserMatchingRelayUsers.keySet());
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
            Map<User, Set<RelayUser>> keyUserMatchingRelayUsers = provisionUsersData.getKeyUserMatchingRelayUsers();
            Set<User> keyUsersMatching = provisionUsersData.getKeyUsersMatching();

            for(RelayUser relayUser : provisionUsersData.getRelayUsers())
            {
                executorService.execute(
                        new ProvisionUsersWorkerThread(relayUser, keyUserMatchingRelayUsers, keyUsersMatching));

                provisionUsersData.incrementCounter();
                if(provisionUsersLimit > 0 && (provisionUsersData.getCounter().get() >= provisionUsersLimit))
                {
                    break;
                }
            }
        }
    }

    private class ProvisionUsersWorkerThread implements Runnable
    {
        private RelayUser relayUser;
        private Map<User, Set<RelayUser>> keyUserMatchingRelayUsers;
        private Set<User> keyUsersMatching;

        private ProvisionUsersWorkerThread(RelayUser relayUser, Map<User, Set<RelayUser>> keyUserMatchingRelayUsers, Set<User> keyUsersMatching)
        {
            this.relayUser = relayUser;
            this.keyUserMatchingRelayUsers = keyUserMatchingRelayUsers;
            this.keyUsersMatching = keyUsersMatching;
        }

        @Override
        public void run()
        {
            User gcxUser = new User();
            MatchingUsers matchingUsers = new MatchingUsers();

            DateTime startLookup = null;
            DateTime startProvisioning = null;
            GcxUserService.MatchResult matchResult = null;

            try
            {
                if(logger.isTraceEnabled())
                {
                    startLookup = DateTime.now();
                }

                matchResult = new GcxUserService.MatchResult();
                matchingUsers = gcxUserService.findGcxUsers(relayUser, matchResult);
                gcxUser = gcxUserService.resolveGcxUser(relayUser, matchResult, matchingUsers);

                if(matchResult.multiples())
                {
                    relayUsersMatchedMoreThanOneGcxUser.add(relayUser);
                    relayUsersWithGcxUsersMatchedMoreThanOneGcxUser.add(new RelayGcxUsers(relayUser,
                            matchingUsers.toSet(), new GcxUserService.MatchDifferentGcxUsersException(matchResult
                            .matchType.toString())));
                }

                if(logger.isTraceEnabled())
                {
                    logDuration(startLookup, "gcx user lookup : ");
                }

                // ensure valid ssoguid
                String validRelayUserSsoguid =
                        Misc.isValidUUID(relayUser.getSsoguid()) ? relayUser.getSsoguid() : UUID.randomUUID().toString();

                // if matching gcx user found
                if(gcxUser != null)
                {
                    relayUsersWithGcxMatchAndGcxUsers.
                            add(new RelayGcxUsers(relayUser, gcxUser, matchingUsers.toSet(), matchResult));
                    matchingRelayGcxUsers.put(relayUser, gcxUser);

                    if(relayUser.isAuthoritative() || relayUser.getLastLogonTimestamp().isAfter(gcxUser.getLoginTime()))
                    {
                        gcxUser.setGuid(validRelayUserSsoguid);
                        relayUser.setUserFromRelayIdentity(gcxUser);
                    }

                    relayUser.setUserFromRelayAttributes(gcxUser);

                    manageKeyUserWhenKeyMatchesMultipleRelay(gcxUser, relayUser);
                }
                else
                {
                    if(!validRelayUserSsoguid.equals(relayUser.getSsoguid()))
                        relayUsersWithNewSsoguid.add(relayUser);

                    gcxUser = gcxUserService.getGcxUserFromRelayUser(relayUser, validRelayUserSsoguid);
                }

                if(provisionUsers)
                {
                    if(logger.isTraceEnabled())
                    {
                        logger.trace("user manager create user " + gcxUser.toString());
                    }

                    if(logger.isTraceEnabled())
                    {
                        startProvisioning = DateTime.now();
                    }

                    // Provision (create) the new relay / key user
                    userManagerMerge.createUser(gcxUser);

                    // Provision group membership
                    provisionGroup(relayUser, gcxUser);

                    if(logger.isTraceEnabled())
                    {
                        logDuration(startProvisioning, "provisioned user : ");
                    }

                    gcxUsersProvisioned.add(gcxUser);
                    relayUsersProvisioned.add(relayUser);

                    if(logProvisioningRealTime)
                    {
                        Output.logMessage(StringUtils.join(relayUser.toList(), ","), provisioningRelayUsersFile);
                    }
                }
            }
            catch(TheKeyGuidAlreadyExistsException theKeyGuidAlreadyExistsException)
            {
                theKeyGuidUserAlreadyExists.add(new RelayGcxUsers(relayUser, gcxUser, matchingUsers.toSet(), matchResult));
            }
            catch(RelayGuidAlreadyExistsException relayGuidAlreadyExistsException)
            {
                relayGuidUserAlreadyExists.add(new RelayGcxUsers(relayUser, gcxUser, matchingUsers.toSet(), matchResult));
            }
            catch(UserAlreadyExistsException userAlreadyExistsException)
            {
                userAlreadyExists.add(new RelayGcxUsers(relayUser, gcxUser, matchingUsers.toSet(), matchResult));
            }
            catch(Exception e)
            {
                relayUsersFailedToProvision.put(relayUser, e);
                if(gcxUser != null)
                {
                    gcxUsersFailedToProvision.put(gcxUser, e);
                }
                Output.logMessage(StringUtils.join(relayUser.toList(), ",") + " " + e.getMessage() + "," +
                        e.getCause() + "," + e.toString(), failingProvisioningRelayUsersFile);
                if(provisioningFailureStackTrace)
                {
                    e.printStackTrace();
                }
            }
        }

        // handle when multiple relay accounts matching the one key account
        private void manageKeyUserWhenKeyMatchesMultipleRelay(User gcxUser, RelayUser relayUser)
        {
            // if the key account is a member of our pre calculated set of key users matching multiple relay users
            User keyUser = havingEmail(keyUsersMatching, gcxUser.getEmail());
            if(keyUser != null)
            {
                Set<RelayUser> relayUsersMatchingKeyUser = keyUserMatchingRelayUsers.get(keyUser);
                if(relayUsersMatchingKeyUser == null)
                {
                    logger.error("!!!!!!!!!!!!We should never get a null for a relay user set from a key entry!!!!!!");
                }
                else
                {
                    RelayUser relayUserMatchingEmail = null;
                    RelayUser relayUserMatchingSsoguid = null;
                    RelayUser relayUserMatchingLink = null;
                    for(RelayUser relayUserMatchingKeyUser : relayUsersMatchingKeyUser)
                    {
                        if(relayUserMatchingKeyUser.getUsername().equalsIgnoreCase(keyUser.getEmail()))
                        {
                            relayUserMatchingEmail = relayUserMatchingKeyUser;
                        }
                        else if(relayUserMatchingKeyUser.getSsoguid().equalsIgnoreCase(keyUser.getGuid()))
                        {
                            relayUserMatchingSsoguid = relayUserMatchingKeyUser;
                        }
                        else
                        {
                            relayUserMatchingLink = relayUserMatchingKeyUser;
                        }
                    }

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

                    // if relay account match on key account by email and guid
                    if(relayUserMatchingEmail != null && relayUserMatchingSsoguid != null)
                    {
                        // if this relay user is the one matching the key by email
                        if(relayUser.getUsername().equalsIgnoreCase(relayUserMatchingEmail.getUsername()))
                        {
                            gcxUser.setGuid(gcxUser.getRawRelayGuid());
                        }
                        // if this relay user is the one matching the key by guid
                        else if(relayUser.getSsoguid().equalsIgnoreCase(relayUserMatchingSsoguid.getSsoguid()))
                        {
                            gcxUser.setGuid(UUID.randomUUID().toString());
                            gcxUser.setTheKeyGuid("");
                            gcxUser.setEmail(relayUser.getUsername());
                        }
                    }
                    else
                    {
                        // TODO handle these edge cases as well.
                    }
                }
            }
        }
    }

    public static User havingEmail(Set<User> users, final String email)
    {
        try
        {
            return Iterables.find(users, new Predicate<User>()
            {
                public boolean apply(User user)
                {
                    return user.getEmail().equalsIgnoreCase(email);
                }
            });
        }
        catch(NoSuchElementException e)
        {
            return null;
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

                    Group group = groupValueTranscoder.decodeStringValue(groupDn);

                    if(logger.isTraceEnabled())
                    {
                        logger.trace("group dn : " + groupDn + " and path " + StringUtils.join(group.getPath()) +
                                " and name " + group.getName());
                    }

                    userManagerMerge.addToGroup(gcxUser, group);
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
        return groupDn.replaceAll("CN=", "ou=").replaceAll("cn=", "ou=").replaceFirst("ou=", "cn=").replaceAll("DC=",
                "dc=");
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

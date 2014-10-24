package org.cru.migration.ldap;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import me.thekey.cas.service.UserAlreadyExistsException;
import me.thekey.cas.service.UserManager;
import org.apache.commons.lang.StringUtils;
import org.ccci.gcx.idm.core.model.impl.GcxUser;
import org.ccci.idm.ldap.Ldap;
import org.cru.migration.dao.LdapDao;
import org.cru.migration.domain.RelayGcxUsers;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;
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
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TheKeyLdap
{
	private MigrationProperties properties;

	private Ldap ldap;

	private LdapDao ldapDao;

	private Logger logger = LoggerFactory.getLogger(getClass());

	private UserManager userManager;
	private UserManager userManagerMerge;

	private GcxUserService gcxUserService;

    private MigrationProperties migrationProperties;

	Set<RelayUser> relayUsersProvisioned = Sets.newSetFromMap(new ConcurrentHashMap<RelayUser, Boolean>());
	Set<GcxUser> gcxUsersProvisioned = Sets.newSetFromMap(new ConcurrentHashMap<GcxUser, Boolean>());
	Map<RelayUser, Exception> relayUsersFailedToProvision = Maps.newConcurrentMap();
	Map<GcxUser, Exception> gcxUsersFailedToProvision = Maps.newConcurrentMap();
	Map<RelayUser, GcxUser> matchingRelayGcxUsers = Maps.newConcurrentMap();
	Set<RelayUser> relayUsersMatchedMoreThanOneGcxUser = Sets.newSetFromMap(new ConcurrentHashMap<RelayUser,
			Boolean>());

	Set<RelayGcxUsers> relayUsersWithGcxUsersMatchedMoreThanOneGcxUser = Sets.newSetFromMap(new
			ConcurrentHashMap<RelayGcxUsers, Boolean>());

	Boolean provisionUsers;
	Boolean provisioningFailureStackTrace;
	Boolean logProvisioningRealTime;
	Integer provisionUsersLimit;

	File provisioningRelayUsersFile;
	File failingProvisioningRelayUsersFile;

	public TheKeyLdap(MigrationProperties properties) throws Exception
	{
		this.properties = properties;

        migrationProperties = new MigrationProperties();

        ldap = new Ldap(properties.getNonNullProperty("theKeyLdapHost"),
				properties.getNonNullProperty("theKeyLdapUser"),
				properties.getNonNullProperty("theKeyLdapPassword"));

		ldapDao = new LdapDao(ldap);

		userManager = TheKeyBeans.getUserManager();
		userManagerMerge = TheKeyBeans.getUserManagerMerge();

        LinkingServiceImpl linkingServiceImpl = new LinkingServiceImpl();
        linkingServiceImpl.setResource(properties.getNonNullProperty("identityLinkingResource"));
        linkingServiceImpl.setIdentitiesAccessToken(properties.getNonNullProperty("identityLinkingAccessToken"));

		gcxUserService = new GcxUserService(userManager, linkingServiceImpl);

		provisionUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("provisionUsers"));
		provisioningFailureStackTrace = Boolean.valueOf(migrationProperties.getNonNullProperty
				("provisioningFailureStackTrace"));
		logProvisioningRealTime = Boolean.valueOf(migrationProperties.getNonNullProperty
				("logProvisioningRealTime"));
		provisionUsersLimit = Integer.valueOf(migrationProperties.getNonNullProperty
				("provisionUsersLimit"));

		provisioningRelayUsersFile = FileHelper.getFileToWrite(properties.getNonNullProperty("relayUsersProvisioning"));
		failingProvisioningRelayUsersFile = FileHelper.getFileToWrite(properties.getNonNullProperty
				("relayUsersFailingProvisioning"));
	}

	private static List<String> systems =
			Arrays.asList("AnswersBindUser", "ApacheBindUser", "CCCIBIPUSER", "CCCIBIPUSERCNCPRD", "CasBindUser",
					"CssUnhashPwd", "DssUcmUser", "EFTBIPUSER", "EasyVista", "GADS", "GUESTCNC", "GUESTCNCPRD",
					"GrouperAdmin", "GuidUsernameLookup", "IdentityLinking", "LDAPUSER", "LDAPUSERCNCPRD",
					"MigrationProcess", "Portfolio", "PshrReconUser", "RulesService", "SADMIN", "SADMINCNCPRD",
					"SHAREDCNC", "SHAREDCNCOUIPRD", "SHAREDCNCPRD", "SHAREDCNCSTG", "SHAREDCNCTST",
					"SelfServiceAdmin",
					"StellentSystem");

	private List<String> cruPersonAttributeNames = Arrays.asList("cruDesignation", "cruEmployeeStatus", "cruGender",
			"cruHrStatusCode", "cruJobCode", "cruManagerID", "cruMinistryCode", "cruPayGroup",
			"cruPreferredName", "cruSubMinistryCode", "proxyAddresses");

	private List<String> relayAttributeNames = Arrays.asList("relayGuid");

	private final static String cruPersonObjectId = "1.3.6.1.4.1.100.100.100.1.1.1";

	public void createCruPersonObject()
	{
		logger.info("creating cru person object class and attributes ...");
		try
		{
			Integer iterator = 0;
			for(String attributeName : cruPersonAttributeNames)
			{
				ldapDao.createAttribute(attributeName, "cru person attribute", cruPersonObjectId + "." + iterator++);
			}

			String className = "cruPerson";

			List<String> requiredAttributes = Arrays.asList("cn");

			ldapDao.createStructuralObjectClass(className, "Cru Person", requiredAttributes, cruPersonObjectId, "inetOrgPerson");

			for(String attributeName : cruPersonAttributeNames)
			{
				ldapDao.addAttributeToClass(className, attributeName, "MAY");
			}
		}
		catch (NamingException namingException)
		{
			namingException.printStackTrace();
		}
	}

	public void deleteCruPersonObject()
	{
		logger.info("deleting cru person object class and attributes ...");
		try
		{
			String className = "cruPerson";

			ldapDao.deleteClass(className);

			for(String attributeName : cruPersonAttributeNames)
			{
				logger.info("deleting attribute " + attributeName);
				ldapDao.deleteAttribute(attributeName);
			}

		}
		catch (NamingException namingException)
		{
			namingException.printStackTrace();
		}
	}

	public void createRelayAttributes()
	{
		logger.info("creating relay attributes ...");
		try
		{
			for(String attributeName : relayAttributeNames)
			{
				ldapDao.createAttribute(attributeName, "relay attribute", cruPersonObjectId + "." + "100");
			}
		}
		catch (NamingException namingException)
		{
			namingException.printStackTrace();
		}
	}

	public void deleteRelayAttributes()
	{
		logger.info("deleting relay attributes ...");
		try
		{
			for(String attributeName : relayAttributeNames)
			{
				logger.info("deleting attribute " + attributeName);
				ldapDao.deleteAttribute(attributeName);
			}

		}
		catch (NamingException namingException)
		{
			namingException.printStackTrace();
		}
	}

	public void createSystemEntries() throws NamingException
	{
		for(String system : systems)
		{
			Map<String,String> attributeMap = Maps.newHashMap();

			attributeMap.put("cn", system);
			attributeMap.put("sn", system);
			attributeMap.put("userPassword", systemPassword(system));

			ldap.createEntity("cn=" + system + "," + properties.getNonNullProperty("theKeySystemUsersDn"), attributeMap, systemEntryClasses());
		}
	}

	public void provisionUsers(Set<RelayUser> relayUsers)
	{
		logger.info("provisioning relay users to the key of size " + relayUsers.size());

        int counter = 0;
		Long totalProvisioningTime = 0L;
		DateTime start = DateTime.now();

		ExecutorService executorService = Executors.newFixedThreadPool(50);

		for(RelayUser relayUser : relayUsers)
		{
			Runnable worker = new WorkerThread(relayUser);

			executorService.execute(worker);

			counter++;
			if (provisionUsersLimit > 0 && (counter >= provisionUsersLimit))
			{
				break;
			}
		}

		executorService.shutdown();

		logger.info("provisioning relay users done firing off worker threads");

		try
		{
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException e)
		{
			logger.error("provisioning relay users executor service exception on awaitTermination() " + e);
		}

		totalProvisioningTime += (new Duration(start, DateTime.now())).getMillis();
		logger.info("provisioned " + counter + " users at an average milliseconds of (" +
				totalProvisioningTime + "/" + counter + ")" + totalProvisioningTime / counter + " per user " +
				"and a total of " + StringUtilities.toString(new Duration(totalProvisioningTime)));

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
            Output.logRelayGcxUsers(matchingRelayGcxUsers,
                    properties.getNonNullProperty("matchingRelayGcxUsers"));

			logger.info("Size of relayUsersMatchedMoreThanOneGcxUser " +
					relayUsersMatchedMoreThanOneGcxUser.size());
			Output.serializeRelayUsers(relayUsersMatchedMoreThanOneGcxUser,
					properties.getNonNullProperty("relayUsersMatchedMoreThanOneGcxUser"));

			logger.info("Size of relayUsersWithGcxUsersMatchedMoreThanOneGcxUser " +
					relayUsersWithGcxUsersMatchedMoreThanOneGcxUser.size());
			Output.serializeRelayGcxUsers(relayUsersWithGcxUsersMatchedMoreThanOneGcxUser,
					properties.getNonNullProperty("relayUsersWithGcxUsersMatchedMoreThanOneGcxUser"), false);
		}
		catch (Exception e)
		{}
	}

	private class WorkerThread implements Runnable
	{
		private RelayUser relayUser;

		private WorkerThread(RelayUser relayUser)
		{
			this.relayUser = relayUser;
		}

		@Override
		public void run()
		{
			if(logger.isTraceEnabled())
			{
				logger.trace(Thread.currentThread().getName() + " Start ");
			}

			processCommand();

			if(logger.isTraceEnabled())
			{
				logger.trace(Thread.currentThread().getName() + " End ");
			}
		}

		private void processCommand()
		{
			GcxUser gcxUser = null;
			Set<GcxUser> gcxUsers = null;

			DateTime startLookup = null;
			DateTime startProvisioning = null;

			try
			{
				if(logger.isTraceEnabled())
				{
					startLookup = DateTime.now();
				}

				// TODO capture match result somewhere
				GcxUserService.MatchResult matchResult = new GcxUserService.MatchResult();
				gcxUsers = gcxUserService.findGcxUsers(relayUser, matchResult);
				gcxUser = gcxUserService.resolveGcxUser(relayUser, matchResult, gcxUsers);

				if (logger.isTraceEnabled())
				{
					logDuration(startLookup, "gcx user lookup : ");
				}

				// if matching gcx user found
				if(gcxUser != null)
				{
					matchingRelayGcxUsers.put(relayUser, gcxUser);

					if(relayUser.isAuthoritative())
					{
						relayUser.setGcxUserFromRelayIdentity(gcxUser);
					}

					relayUser.setGcxUserFromRelayAttributes(gcxUser);
				}
				else
				{
					gcxUser = gcxUserService.getGcxUser(relayUser);
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

					userManagerMerge.createUser(gcxUser);

					if (logger.isTraceEnabled())
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
			catch(GcxUserService.MatchDifferentGcxUsersException matchDifferentGcxUsersException)
			{
				relayUsersMatchedMoreThanOneGcxUser.add(relayUser);
				relayUsersWithGcxUsersMatchedMoreThanOneGcxUser.add(new RelayGcxUsers(relayUser, gcxUsers,
						matchDifferentGcxUsersException));
				relayUsersFailedToProvision.put(relayUser, matchDifferentGcxUsersException);
				for(GcxUser fromGcxUsers : gcxUsers)
				{
					if(fromGcxUsers != null) // TODO find out why this could be null
					{
						gcxUsersFailedToProvision.put(fromGcxUsers, matchDifferentGcxUsersException);
					}
				}

				Output.logMessage(StringUtils.join(relayUser.toList(), ",") + " " + matchDifferentGcxUsersException.getMessage(),
						failingProvisioningRelayUsersFile);

				if(provisioningFailureStackTrace)
				{
					matchDifferentGcxUsersException.printStackTrace();
				}
			}
			catch (Exception e)
			{
				relayUsersFailedToProvision.put(relayUser, e);
				gcxUsersFailedToProvision.put(gcxUser, e);
				Output.logMessage(StringUtils.join(relayUser.toList(), ",") + " " + e.getMessage(),
						failingProvisioningRelayUsersFile);
				if(provisioningFailureStackTrace)
				{
					e.printStackTrace();
				}
			}
		}
	}

	private void logDuration(DateTime start, String message)
	{
		Duration duration = new Duration(start, DateTime.now());

		logger.trace(message + StringUtilities.toString(duration));
	}

	public void removeDn(String dn)
	{
		DistinguishedName distinguishedName = new DistinguishedName(dn);

		LdapTemplate ldapTemplate = TheKeyBeans.getLdapTemplateMerge();

		ldapTemplate.unbind(distinguishedName, true);
	}

	public void createUser(RelayUser relayUser) throws UserAlreadyExistsException
	{
		UserManager userManager = TheKeyBeans.getUserManager();

		GcxUser gcxUser = gcxUserService.getGcxUser(relayUser);

		userManager.createUser(gcxUser);
	}

    public GcxUser getGcxUser(String email) throws NamingException
    {
        return userManagerMerge.findUserByEmail(email);
    }

    public Integer getUserCount() throws NamingException
    {
        String theKeyUserRootDn = migrationProperties.getNonNullProperty("theKeyUserRootDn");

		Integer count = ldapDao.getUserCount(theKeyUserRootDn, "cn");

        return count;
    }

	public Map<String, Attributes> getEntries() throws NamingException
	{
		String theKeyUserRootDn = migrationProperties.getNonNullProperty("theKeyUserRootDn");

		return ldapDao.getEntries(theKeyUserRootDn, "cn", 2);
	}

	public void removeEntries(Map<String, Attributes> entries) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

		RemoveEntriesData getUserCountData = new RemoveEntriesData(entries);

		executionService.execute(new RemoveEntries(), getUserCountData, 200);
	}

	private class RemoveEntriesData
	{
		private Map<String, Attributes> entries;
		private LdapTemplate ldapTemplate = TheKeyBeans.getLdapTemplateMerge();

		private RemoveEntriesData(Map<String, Attributes> entries)
		{
			this.entries = entries;
		}

		public LdapTemplate getLdapTemplate()
		{
			return ldapTemplate;
		}

		public Map<String, Attributes> getEntries()
		{
			return entries;
		}
	}

	public class RemoveEntries implements ExecuteAction
	{
		@Override
		public void execute(ExecutorService executorService, Object object)
		{
			RemoveEntriesData removeEntriesData = (RemoveEntriesData)object;

			for (Map.Entry<String, Attributes> entry : removeEntriesData.getEntries().entrySet())
			{
				String key = entry.getKey();
				String[] nodes = key.split(",");
				executorService.execute(new RemoveEntryWorkerThread(nodes[0], removeEntriesData.getLdapTemplate()));
			}
		}
	}

	private class RemoveEntryWorkerThread implements Runnable
	{
		private String dn;
		private LdapTemplate ldapTemplate;

		private RemoveEntryWorkerThread(String dn, LdapTemplate ldapTemplate)
		{
			this.dn = dn;
			this.ldapTemplate = ldapTemplate;
		}

		@Override
		public void run()
		{
			try
			{
				ldapTemplate.unbind(new DistinguishedName(dn), true);
			}
			catch(Exception e)
			{}
		}
	}

	private String systemPassword(String system)
	{
		return system + "!" + system;
	}

	private String[] systemEntryClasses()
	{
		String[] userClasses = new String[5];

		userClasses[0] = "Top";
		userClasses[1] = "Person";
		userClasses[2] = "inetOrgPerson";
		userClasses[3] = "organizationalPerson";
		userClasses[4] = "ndsLoginProperties";

		return userClasses;
	}
}

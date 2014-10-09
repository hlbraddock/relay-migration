package org.cru.migration.ldap;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import me.thekey.cas.service.UserAlreadyExistsException;
import me.thekey.cas.service.UserManager;
import org.ccci.gcx.idm.core.model.impl.GcxUser;
import org.ccci.idm.ldap.Ldap;
import org.cru.migration.dao.LdapDao;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.support.FileHelper;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;
import org.cru.migration.thekey.GcxUserService;
import org.cru.migration.thekey.TheKeyBeans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;

import javax.naming.NamingException;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

		gcxUserService = new GcxUserService(userManager);
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
			"cruPreferredName", "cruSubMinistryCode");

	private List<String> getCruPersonAttributeNames()
	{
		return cruPersonAttributeNames;
	}

	private final static String cruPersonObjectId = "1.3.6.1.4.1.100.100.100.1.1.1";

	public void createCruPersonObject()
	{
		logger.info("creating cru person object class and attributes ...");
		try
		{
			Integer iterator = 0;
			for(String attributeName : getCruPersonAttributeNames())
			{
				ldapDao.createAttribute(attributeName, "cru person attribute", cruPersonObjectId + "." + iterator++);
			}

			String className = "cruPerson";

			List<String> requiredAttributes = Arrays.asList("cn");

			ldapDao.createStructuralObjectClass(className, "Cru Person", requiredAttributes, cruPersonObjectId);

			for(String attributeName : getCruPersonAttributeNames())
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

			for(String attributeName : getCruPersonAttributeNames())
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

	public void provisionUsers(Set<RelayUser> relayUsers, boolean authoritative)
	{
		logger.info("provisioning relay users to the key of size " + relayUsers.size());

		Set<RelayUser> relayUsersProvisioned = Sets.newHashSet();
		Set<GcxUser> gcxUsersProvisioned = Sets.newHashSet();
		Map<RelayUser, Exception> relayUsersFailedToProvision = Maps.newHashMap();
		Map<GcxUser, Exception> gcxUsersFailedToProvision = Maps.newHashMap();
		Map<RelayUser, GcxUser> matchingRelayGcxUsers = Maps.newHashMap();
		Set<RelayUser> relayUsersMatchedMoreThanOneGcxUser = Sets.newHashSet();

		Boolean provisionUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("provisionUsers"));
		Boolean provisioningFailureStackTrace = Boolean.valueOf(migrationProperties.getNonNullProperty
				("provisioningFailureStackTrace"));
		Boolean logProvisioningRealTime = Boolean.valueOf(migrationProperties.getNonNullProperty
				("logProvisioningRealTime"));

		File provisioningRelayUsersFile = FileHelper.getFileToWrite(properties.getNonNullProperty("relayUsersProvisioning"));
		File failingProvisioningRelayUsersFile = FileHelper.getFileToWrite(properties.getNonNullProperty
				("relayUsersFailingProvisioning"));

        int counter = 0;
		for(RelayUser relayUser : relayUsers)
		{
			GcxUser gcxUser = null;

			if(counter++ % 10 == 0)
			{
				System.out.println("provisioning user " + counter + "\r");
			}

			try
			{
				// find possible matching gcx user
				gcxUser = gcxUserService.findGcxUser(relayUser);
				logger.trace("got matching gcx user " + (gcxUser != null ? gcxUser.toString() : gcxUser));

				// if matching gcx user found
				if(gcxUser != null)
				{
					matchingRelayGcxUsers.put(relayUser, gcxUser);
					if(authoritative)
					{
						gcxUser.setEmail(relayUser.getUsername());
						gcxUser.setPassword(relayUser.getPassword());
					}

					gcxUser.setRelayGuid(relayUser.getSsoguid());
					gcxUserService.setMetaData(gcxUser);
				}
				else
				{
					gcxUser = gcxUserService.getGcxUser(relayUser);
				}

                if(provisionUsers)
                {
					logger.trace("user manager create user " + gcxUser.toString());

					userManagerMerge.createUser(gcxUser);

					relayUsersProvisioned.add(relayUser);

					if(logProvisioningRealTime)
					{
						Output.logMessage(relayUser.toCsvFormattedString(true), provisioningRelayUsersFile);
					}
				}
			}
			catch(GcxUserService.MatchDifferentGcxUsersException matchDifferentGcxUsersException)
			{
				relayUsersMatchedMoreThanOneGcxUser.add(relayUser);
				relayUsersFailedToProvision.put(relayUser, matchDifferentGcxUsersException);
				gcxUsersFailedToProvision.put(gcxUser, matchDifferentGcxUsersException);
				Output.logMessage(relayUser.toCsvFormattedString(true) + " " + matchDifferentGcxUsersException.getMessage(),
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
				Output.logMessage(relayUser.toCsvFormattedString(true) + " " + e.getMessage(),
						failingProvisioningRelayUsersFile);
				if(provisioningFailureStackTrace)
				{
					e.printStackTrace();
				}
			}
		}

		logger.info("provisioning relay users to the key done ");

		try
		{
			Output.logRelayUsers(relayUsersProvisioned,
					FileHelper.getFileToWrite(properties.getNonNullProperty("relayUsersProvisioned")));
			Output.logGcxUsers(gcxUsersProvisioned,
					FileHelper.getFileToWrite(properties.getNonNullProperty("gcxUsersProvisioned")));
			Output.logRelayUsers(relayUsersFailedToProvision,
					FileHelper.getFileToWrite(properties.getNonNullProperty("relayUsersFailedToProvision")));
			Output.logGcxUsers(gcxUsersFailedToProvision,
					FileHelper.getFileToWrite(properties.getNonNullProperty("gcxUsersFailedToProvision")));
            Output.logRelayUsers(relayUsersMatchedMoreThanOneGcxUser,
                    FileHelper.getFileToWrite(properties.getNonNullProperty("relayUsersMatchedMoreThanOneGcxUser")));
            Output.logRelayGcxUsers(matchingRelayGcxUsers,
                    FileHelper.getFileToWrite(properties.getNonNullProperty("matchingRelayGcxUsers")));
		}
		catch (Exception e)
		{}
	}

	public void removeDn(String dn)
	{
		DistinguishedName distinguishedName = new DistinguishedName(dn);

		LdapTemplate ldapTemplate = TheKeyBeans.getLdapTemplate();

		ldapTemplate.unbind(distinguishedName, true);
	}

	public void createUser(RelayUser relayUser) throws UserAlreadyExistsException
	{
		UserManager userManager = TheKeyBeans.getUserManager();

		GcxUser gcxUser = gcxUserService.getGcxUser(relayUser);

		userManager.createUser(gcxUser);
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

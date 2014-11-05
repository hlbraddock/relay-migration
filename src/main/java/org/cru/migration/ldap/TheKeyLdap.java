package org.cru.migration.ldap;

import com.google.common.collect.Maps;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.user.User;
import org.ccci.idm.user.exception.UserException;
import org.ccci.idm.user.UserManager;
import org.cru.migration.dao.LdapDao;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.service.ProvisionUsersService;
import org.cru.migration.service.RemoveEntriesService;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.thekey.GcxUserService;
import org.cru.migration.thekey.TheKeyBeans;
import org.cru.silc.service.LinkingServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TheKeyLdap
{
	private MigrationProperties properties;

	private Ldap ldap;

	private LdapDao ldapDao;

	private UserManager userManagerMerge;

	private GcxUserService gcxUserService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public TheKeyLdap(MigrationProperties properties) throws Exception
	{
		this.properties = properties;

		ldap = new Ldap(properties.getNonNullProperty("theKeyLdapHost"),
				properties.getNonNullProperty("theKeyLdapUser"),
				properties.getNonNullProperty("theKeyLdapPassword"));

		ldapDao = new LdapDao(ldap);

		UserManager userManager = TheKeyBeans.getUserManager();
		userManagerMerge = TheKeyBeans.getUserManagerMerge();

		LinkingServiceImpl linkingServiceImpl = new LinkingServiceImpl();
		linkingServiceImpl.setResource(properties.getNonNullProperty("identityLinkingResource"));
		linkingServiceImpl.setIdentitiesAccessToken(properties.getNonNullProperty("identityLinkingAccessToken"));

		gcxUserService = new GcxUserService(userManager, linkingServiceImpl);
	}

	public void createUser(RelayUser relayUser) throws UserException
	{
		UserManager userManager = TheKeyBeans.getUserManager();

		User gcxUser = gcxUserService.getGcxUserFromRelayUser(relayUser, relayUser.getSsoguid());

		userManager.createUser(gcxUser);
	}

    public User getUser(String email) throws NamingException
    {
        return userManagerMerge.findUserByEmail(email,false);
    }

	public void provisionUsers(Set<RelayUser> relayUsers) throws Exception
	{
		ProvisionUsersService provisionUsersService = new ProvisionUsersService(properties);

		provisionUsersService.provisionUsers(relayUsers);
	}

	public Integer getUserCount() throws NamingException
    {
        String theKeyUserRootDn = properties.getNonNullProperty("theKeyUserRootDn");

		return ldapDao.getUserCount(theKeyUserRootDn, "cn");
    }

	public Map<String, Attributes> getEntries() throws NamingException
	{
		String theKeyUserRootDn = properties.getNonNullProperty("theKeyUserRootDn");

		return ldapDao.getEntries(theKeyUserRootDn, "cn", 2);
	}

	public void removeEntries(Map<String, Attributes> entries) throws NamingException
	{
		RemoveEntriesService removeEntriesService = new RemoveEntriesService(ldap);

		removeEntriesService.removeEntries(entries);
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
	private final static String relayAttributesObjectId = "1.3.6.1.4.1.100.100.101.1.1.1";

	public void createCruPersonAttributes()
	{
		logger.info("creating cru person attributes ...");

		createAttributes(cruPersonAttributeNames, "cru person attribute", cruPersonObjectId);
	}

	public void createRelayAttributes()
	{
		logger.info("creating relay attributes ...");

		createAttributes(relayAttributeNames, "relay attribute", relayAttributesObjectId);
	}

	private void createAttributes(List<String> attributes, String description, String objectId)
	{
		logger.info("creating attributes ...");

		try
		{
			Integer iterator = 0;

			for(String attributeName : attributes)
			{
				ldapDao.createAttribute(attributeName, description, objectId + "." + iterator++);
			}

			// doesn't seem to work:
//			for(String attributeName : cruPersonAttributeNames)
//			{
//				ldapDao.addAttributeToClass(className, attributeName, "MAY");
//			}
		}
		catch (NamingException namingException)
		{
			namingException.printStackTrace();
		}
	}

	public void createCruPersonObject()
	{
		logger.info("creating cru person object class  ...");

		List<String> requiredAttributes = Arrays.asList("cn");

		createObjectClass("cruPerson", "Cru Person", requiredAttributes, cruPersonObjectId, "inetOrgPerson",
				LdapDao.ObjectClassType.Auxiliary);
	}

	public void createRelayAttributesObject()
	{
		logger.info("creating relay attributes object class  ...");

		List<String> requiredAttributes = Arrays.asList("cn");

		createObjectClass("relayAttributes", "Relay Attributes", requiredAttributes, relayAttributesObjectId,
				"inetOrgPerson", LdapDao.ObjectClassType.Auxiliary);
	}

	private void createObjectClass(String className, String description, List<String> requiredAttributes,
								   String objectId, String superClass, LdapDao.ObjectClassType objectClassType)
	{
		logger.info("creating object class  ...");
		try
		{
			ldapDao.createObjectClass(className, description, requiredAttributes, objectId,
					superClass, objectClassType);
		}
		catch (NamingException namingException)
		{
			namingException.printStackTrace();
		}
	}

	public void deleteCruPersonAttributes()
	{
		logger.info("deleting cru person attributes ...");

		deleteAttributes(cruPersonAttributeNames);
	}

	public void deleteRelayAttributes()
	{
		logger.info("deleting relay attributes ...");

		deleteAttributes(relayAttributeNames);
	}

	private void deleteAttributes(List<String> attributeNames)
	{
		logger.info("deleting attributes ...");
		try
		{
			for(String attributeName : attributeNames)
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

	public void deleteCruPersonObject()
	{
		logger.info("deleting cru person object class ...");

		deleteObject("cruPerson");
	}

	public void deleteRelayObject()
	{
		logger.info("deleting cru person object class ...");

		deleteObject("relayAttributes");
	}

	public void deleteObject(String className)
	{
		logger.info("deleting object class ...");
		try
		{
			ldapDao.deleteClass(className);
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

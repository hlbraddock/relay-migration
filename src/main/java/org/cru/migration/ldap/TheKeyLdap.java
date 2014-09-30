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
import org.cru.migration.thekey.TheKeyBeans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;

import javax.naming.NamingException;
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

	public TheKeyLdap(MigrationProperties properties) throws Exception
	{
		this.properties = properties;

		ldap = new Ldap(properties.getNonNullProperty("theKeyLdapHost"),
				properties.getNonNullProperty("theKeyLdapUser"),
				properties.getNonNullProperty("theKeyLdapPassword"));

		ldapDao = new LdapDao(ldap);
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

	public void provisionUsers(Set<RelayUser> relayUsers)
	{
		logger.info("provisioning relay users to the key of size " + relayUsers.size());

		UserManager userManager = TheKeyBeans.getUserManager();

		Set<RelayUser> relayUsersProvisioned = Sets.newHashSet();
		Map<RelayUser, Exception> relayUsersFailedToProvision = Maps.newHashMap();

		int counter = 0;
		for(RelayUser relayUser : relayUsers)
		{
			if(counter++ % 100 == 0)
			{
				System.out.println("provisioning user " + counter + "\r");
			}

			try
			{
				GcxUser gcxUser = getGcxUser(relayUser);
				userManager.createUser(gcxUser);
				relayUsersProvisioned.add(relayUser);
			}
			catch (Exception e)
			{
				relayUsersFailedToProvision.put(relayUser, e);
			}
		}

		logger.info("provisioning relay users to the key done ");

		try
		{
			Output.logRelayUser(relayUsersProvisioned,
					FileHelper.getFile(properties.getNonNullProperty("relayUsersProvisioned")));
			Output.logRelayUser(relayUsersFailedToProvision,
					FileHelper.getFile(properties.getNonNullProperty("relayUsersFailedToProvision")));
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

	public void deleteUser(RelayUser relayUser)
	{
		UserManager userManager = TheKeyBeans.getUserManager();

		GcxUser gcxUser = getGcxUser(relayUser);

		userManager.deleteUser(gcxUser);
	}

	public void createUser(RelayUser relayUser) throws UserAlreadyExistsException
	{
		UserManager userManager = TheKeyBeans.getUserManager();

		GcxUser gcxUser = getGcxUser(relayUser);

		userManager.createUser(gcxUser);
	}

	private GcxUser getGcxUser(RelayUser relayUser)
	{
		final GcxUser gcxUser = relayUser.toGcxUser();

		gcxUser.setSignupKey(TheKeyBeans.getRandomStringGenerator().getNewString());

		gcxUser.setPasswordAllowChange(true);
		gcxUser.setForcePasswordChange(false);
		gcxUser.setLoginDisabled(false);
		gcxUser.setVerified(false);

		return gcxUser;
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

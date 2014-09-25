package org.cru.migration.ldap;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import me.thekey.cas.service.UserAlreadyExistsException;
import me.thekey.cas.service.UserManager;
import org.ccci.gcx.idm.core.model.impl.GcxUser;
import org.ccci.idm.ldap.Ldap;
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
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TheKeyLdap
{
	private MigrationProperties properties;

	private Ldap ldap;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public TheKeyLdap(MigrationProperties properties) throws Exception
	{
		this.properties = properties;

		ldap = new Ldap(properties.getNonNullProperty("theKeyLdapHost"),
				properties.getNonNullProperty("theKeyLdapUser"),
				properties.getNonNullProperty("theKeyLdapPassword"));
	}

	private static List<String> systems =
			Arrays.asList("AnswersBindUser", "ApacheBindUser", "CCCIBIPUSER", "CCCIBIPUSERCNCPRD", "CasBindUser",
					"CssUnhashPwd", "DssUcmUser", "EFTBIPUSER", "EasyVista", "GADS", "GUESTCNC", "GUESTCNCPRD",
					"GrouperAdmin", "GuidUsernameLookup", "IdentityLinking", "LDAPUSER", "LDAPUSERCNCPRD",
					"MigrationProcess", "Portfolio", "PshrReconUser", "RulesService", "SADMIN", "SADMINCNCPRD",
					"SHAREDCNC", "SHAREDCNCOUIPRD", "SHAREDCNCPRD", "SHAREDCNCSTG", "SHAREDCNCTST",
					"SelfServiceAdmin",
					"StellentSystem");

	public void createCruPersonObject()
	{
		try
		{
			String objectClassName = "cruPerson";

			DirContext schema = ldap.getContext().getSchema("");

			DirContext objectClass = createCruPersonObjectClass(objectClassName, schema);

			createCruPersonObjectClassAttributes(objectClassName, schema);

			addCruPersonAttributes(schema);
		}
		catch (NamingException namingException)
		{
			namingException.printStackTrace();
		}
	}

	private DirContext createCruPersonObjectClass(String objectClassName, DirContext schema) throws NamingException
	{
		// Specify attributes for the schema object
		Attributes attributes = new BasicAttributes(true); // Ignore case
		attributes.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.1.1");
		attributes.put("NAME", objectClassName);
		attributes.put("DESC", "for JNDITutorial example only");
		attributes.put("SUP", "top");
		attributes.put("STRUCTURAL", "true");
		Attribute must = new BasicAttribute("MUST", "cn");
		must.add("objectclass");
		attributes.put(must);

		// Add the new schema object for the object class
		return schema.createSubcontext("ClassDefinition/" + objectClassName, attributes);
	}

	private void createCruPersonObjectClassAttributes(String objectClassName, DirContext schema) throws NamingException
	{
		// Specify new MAY attribute for schema object
		Attribute may = new BasicAttribute("MAY", "description");
		Attributes attributes = new BasicAttributes(false);
		attributes.put(may);

		// Modify schema object
		schema.modifyAttributes("ClassDefinition/" + objectClassName,
				DirContext.ADD_ATTRIBUTE, attributes);
	}

	private void addCruPersonAttributes(DirContext schema) throws NamingException
	{
		DirContext attribute = addAttribute("cruDesignation", schema);
	}

	private DirContext addAttribute(String attributeName, DirContext schema) throws NamingException
	{
		// Specify attributes for the schema object
		Attributes attributes = new BasicAttributes(true); // Ignore case
		attributes.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.1.2");
		attributes.put("NAME", attributeName);
		attributes.put("DESC", "for JNDITutorial example only");
		attributes.put("SYNTAX", "1.3.6.1.4.1.1466.115.121.1.15");

		// Add the new schema object
		return schema.createSubcontext("AttributeDefinition/" + attributeName, attributes);
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

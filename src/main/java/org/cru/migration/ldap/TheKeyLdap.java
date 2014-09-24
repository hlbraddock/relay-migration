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

import javax.naming.NamingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TheKeyLdap
{
	private MigrationProperties properties;

	private Ldap ldap;

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

	public void provisionUsers(Set<RelayUser> relayUserSet)
	{
		UserManager userManager = TheKeyBeans.getUserManager();

		Set<RelayUser> relayUsersProvisioned = Sets.newHashSet();
		Map<RelayUser, Exception> relayUsersFailedToProvision = Maps.newHashMap();

		for(RelayUser relayUser : relayUserSet)
		{
			try
			{
				userManager.createUser(getGcxUser(relayUser));
				relayUsersProvisioned.add(relayUser);
			}
			catch (Exception e)
			{
				relayUsersFailedToProvision.put(relayUser, e);
			}
		}

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


	public void createUser() throws UserAlreadyExistsException
	{
		UserManager userManager = TheKeyBeans.getUserManager();

		RelayUser relayUser = new RelayUser("lee.braddock@cru.org", "Password1", "Lee", "Braddock", "", "", null);

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

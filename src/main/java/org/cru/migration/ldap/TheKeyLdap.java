package org.cru.migration.ldap;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.ccci.idm.ldap.Ldap;
import org.cru.migration.support.MigrationProperties;

import javax.naming.NamingException;
import java.io.File;

public class TheKeyLdap
{
	private MigrationProperties properties;

	private Ldap ldap;

	public TheKeyLdap(MigrationProperties properties) throws Exception
	{
		this.properties = properties;

		String password = Files.readFirstLine(new File(properties.getNonNullProperty("theKeyLdapPassword")),
				Charsets.UTF_8);

		ldap = new Ldap(properties.getNonNullProperty("theKeyLdapHost"), properties.getNonNullProperty("theKeyLdapUser")
				, password);
	}

	public void createSystemUsers() throws NamingException
	{
		String[] attributeNames = new String[3];
		attributeNames[0] = "cn";
		attributeNames[1] = "sn";
		attributeNames[2] = "userPassword";

//		Arrays.asList("AnswersBindUser", "ApacheBindUser", "La Plata");

		String[] attributeValues = new String[3];
		attributeValues[0] = "CasBindUser";
		attributeValues[1] = "CasBindUser";
		attributeValues[2] = "asdfasdaf";

		String[] userClasses = new String[5];
		userClasses[0] = "Top";
		userClasses[1] = "Person";
		userClasses[2] = "inetOrgPerson";
		userClasses[3] = "organizationalPerson";
		userClasses[4] = "ndsLoginProperties";

		ldap.createEntity("cn=CasBindUser," +  properties.getNonNullProperty("theKeySystemUsersDn"), attributeNames, attributeValues, userClasses);
	}
}

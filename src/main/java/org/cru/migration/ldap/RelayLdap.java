package org.cru.migration.ldap;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.ldap.attributes.LdapAttributesActiveDirectory;
import org.cru.migration.support.MigrationProperties;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.io.File;
import java.util.Map;

public class RelayLdap
{
	private Ldap ldap;

	private LdapAttributesActiveDirectory ldapAttributes = new LdapAttributesActiveDirectory();

	private String userDn;

	public RelayLdap(MigrationProperties properties) throws Exception
	{
		String password = Files.readFirstLine(new File(properties.getNonNullProperty("relayLdapPassword")),
				Charsets.UTF_8);

		ldap = new Ldap(properties.getNonNullProperty("relayLdapHost"), properties.getNonNullProperty("relayLdapUser")
				, password);

		userDn = properties.getNonNullProperty("relayUserRootDn");
	}

	public Map<String, Attributes> getUSStaff() throws NamingException
	{
		return ldap.searchAttributes(userDn, ldapAttributes.employeeNumber + "=*", ldapAttributes.getAll());
	}
}

package org.cru.migration.dao;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.ccci.idm.dao.IdentityDAO;
import org.ccci.idm.dao.impl.IdentityDAOLDAPImpl;
import org.ccci.idm.ldap.attributes.LdapAttributesActiveDirectory;
import org.cru.migration.support.MigrationProperties;

import java.io.File;

public class IdentityDaoFactory
{
	static public IdentityDAO getInstance(MigrationProperties properties) throws Exception
	{
		LdapAttributesActiveDirectory ldapAttributes = new LdapAttributesActiveDirectory();

		String password = Files.readFirstLine(new File(properties.getNonNullProperty("relayLdapPassword")),
				Charsets.UTF_8);

		return new IdentityDAOLDAPImpl(properties.getNonNullProperty("relayLdapHost"), ldapAttributes,
				properties.getNonNullProperty("relayLdapUser"), password);
	}
}

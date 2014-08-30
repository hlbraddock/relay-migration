package org.cru.migration.dao;

import org.ccci.idm.dao.IdentityDAO;
import org.ccci.idm.dao.impl.IdentityDAOLDAPImpl;
import org.ccci.idm.ldap.attributes.LdapAttributesActiveDirectory;
import org.cru.migration.support.MigrationProperties;

public class IdentityDaoFactory
{
	static public IdentityDAO getInstance(MigrationProperties properties) throws Exception
	{
		LdapAttributesActiveDirectory ldapAttributes = new LdapAttributesActiveDirectory();

		return new IdentityDAOLDAPImpl(properties.getNonNullProperty("relayLdapHost"), ldapAttributes,
				properties.getNonNullProperty("relayLdapUser"),
				properties.getNonNullProperty("relayLdapPassword"));
	}
}

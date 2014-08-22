package org.cru.migration.ldap;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.ldap.attributes.LdapAttributesActiveDirectory;
import org.ccci.idm.util.DataMngr;
import org.ccci.idm.util.MappedProperties;
import org.ccci.idm.util.Time;
import org.cru.migration.domain.StaffRelayUser;
import org.cru.migration.domain.StaffRelayUserMap;
import org.cru.migration.support.MigrationProperties;
import org.joda.time.DateTime;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.io.File;
import java.util.Map;

public class RelayLdap
{
	private Ldap ldap;

	private LdapAttributesActiveDirectory ldapAttributes;

	private StaffRelayUserMap staffRelayUserMap;

	private String userRootDn;

	public RelayLdap(MigrationProperties properties) throws Exception
	{
		String password = Files.readFirstLine(new File(properties.getNonNullProperty("relayLdapPassword")),
				Charsets.UTF_8);

		ldap = new Ldap(properties.getNonNullProperty("relayLdapHost"), properties.getNonNullProperty("relayLdapUser")
				, password);

		userRootDn = properties.getNonNullProperty("relayUserRootDn");

		ldapAttributes = new LdapAttributesActiveDirectory();

		staffRelayUserMap = new StaffRelayUserMap(ldapAttributes);
	}

	public StaffRelayUser getStaff(String employeeId) throws NamingException
	{
		StaffRelayUser staffRelayUser = new StaffRelayUser();
		staffRelayUser.setEmployeeId(employeeId);

		Map<String,String> searchMap = Maps.newHashMap();
		searchMap.put(ldapAttributes.employeeNumber, employeeId);

		String[] returnAttributes = { ldapAttributes.username, ldapAttributes.lastLogonTimeStamp };

		Map<String,Attributes> results = ldap.searchAttributes(userRootDn, searchMap, returnAttributes);

		MappedProperties<StaffRelayUser> mappedProperties = new MappedProperties<StaffRelayUser>(staffRelayUserMap, staffRelayUser);

		for (Map.Entry<String, Attributes> entry : results.entrySet())
		{
			Attributes attributes = entry.getValue();

			for (String attributeName : returnAttributes)
			{
				String attributeValue = DataMngr.getAttribute(attributes, attributeName);

				if(attributeName.equals(ldapAttributes.lastLogonTimeStamp))
				{
					staffRelayUser.setLastLogonTimestamp(new DateTime(Time.windowsToUnixTime(Long.parseLong(attributeValue))));
				}
				else if(attributeName.equals(ldapAttributes.username))
				{
					mappedProperties.setProperty(attributeName, attributeValue);
				}
			}
		}

		return staffRelayUser;
	}
}

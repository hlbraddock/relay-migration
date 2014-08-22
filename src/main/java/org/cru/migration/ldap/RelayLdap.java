package org.cru.migration.ldap;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.ldap.attributes.LdapAttributesActiveDirectory;
import org.ccci.idm.util.DataMngr;
import org.ccci.idm.util.MappedProperties;
import org.ccci.idm.util.Time;
import org.cru.migration.domain.StaffRelayUser;
import org.cru.migration.domain.StaffRelayUserMap;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;
import org.joda.time.DateTime;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.io.File;
import java.util.List;
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

	public StaffRelayUser getStaff(String employeeId) throws NamingException, UserNotFoundException
	{
		String[] returnAttributes = {ldapAttributes.username, ldapAttributes.lastLogonTimeStamp};

		Map<String, Attributes> results = ldap.searchAttributes(userRootDn, searchMap(employeeId), returnAttributes);

		List<StaffRelayUser> staffRelayUsers = getStaffRelayUser(returnAttributes, results);

		if(staffRelayUsers.size() <= 0)
			throw new UserNotFoundException();

		if(staffRelayUsers.size() > 1)
			Output.println("Found more than one " + staffRelayUsers.size() + " Relay user with employee id " + employeeId);

		StaffRelayUser staffRelayUser = staffRelayUsers.get(0);

		staffRelayUser.setEmployeeId(employeeId);

		return staffRelayUser;
	}

	private List<StaffRelayUser> getStaffRelayUser(String[] returnAttributes, Map<String, Attributes> results)
	{
		List<StaffRelayUser> staffRelayUsers = Lists.newArrayList();

		for (Map.Entry<String, Attributes> entry : results.entrySet())
		{
			Attributes attributes = entry.getValue();

			staffRelayUsers.add(getStaffRelayUser(returnAttributes, attributes));
		}

		return staffRelayUsers;
	}

	private StaffRelayUser getStaffRelayUser(String[] returnAttributes, Attributes attributes)
	{
		StaffRelayUser staffRelayUser = new StaffRelayUser();

		MappedProperties<StaffRelayUser> mappedProperties = new MappedProperties<StaffRelayUser>(staffRelayUserMap,
				staffRelayUser);

		for (String attributeName : returnAttributes)
		{
			String attributeValue = DataMngr.getAttribute(attributes, attributeName);

			if (attributeName.equals(ldapAttributes.lastLogonTimeStamp))
			{
				if(!Strings.isNullOrEmpty(attributeValue))
				{
					staffRelayUser.setLastLogonTimestamp(new DateTime(Time.windowsToUnixTime(Long.parseLong
							(attributeValue))));
				}
			}
			else
			{
				mappedProperties.setProperty(attributeName, attributeValue);
			}
		}

		return staffRelayUser;
	}

	private Map<String, String> searchMap(String employeeId)
	{
		Map<String, String> searchMap = Maps.newHashMap();
		searchMap.put(ldapAttributes.employeeNumber, employeeId);
		return searchMap;
	}
}

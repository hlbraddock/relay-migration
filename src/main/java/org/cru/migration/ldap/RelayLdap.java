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
import org.cru.migration.domain.RelayStaffUser;
import org.cru.migration.domain.StaffRelayUserMap;
import org.cru.migration.exception.MoreThanOneUserFoundException;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.support.MigrationProperties;
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

	public RelayStaffUser getStaff(String employeeId) throws NamingException, UserNotFoundException, MoreThanOneUserFoundException
	{
		String[] returnAttributes = {ldapAttributes.username, ldapAttributes.lastLogonTimeStamp};

		Map<String, Attributes> results = ldap.searchAttributes(userRootDn, searchMap(employeeId), returnAttributes);

		List<RelayStaffUser> relayStaffUsers = getStaffRelayUser(returnAttributes, results);

		if(relayStaffUsers.size() <= 0)
			throw new UserNotFoundException();

		if(relayStaffUsers.size() > 1)
			throw new MoreThanOneUserFoundException();

		RelayStaffUser relayStaffUser = relayStaffUsers.get(0);

		relayStaffUser.setEmployeeId(employeeId);

		return relayStaffUser;
	}

	private List<RelayStaffUser> getStaffRelayUser(String[] returnAttributes, Map<String, Attributes> results)
	{
		List<RelayStaffUser> relayStaffUsers = Lists.newArrayList();

		for (Map.Entry<String, Attributes> entry : results.entrySet())
		{
			Attributes attributes = entry.getValue();

			relayStaffUsers.add(getStaffRelayUser(returnAttributes, attributes));
		}

		return relayStaffUsers;
	}

	private RelayStaffUser getStaffRelayUser(String[] returnAttributes, Attributes attributes)
	{
		RelayStaffUser relayStaffUser = new RelayStaffUser();

		MappedProperties<RelayStaffUser> mappedProperties = new MappedProperties<RelayStaffUser>(staffRelayUserMap,
				relayStaffUser);

		for (String attributeName : returnAttributes)
		{
			String attributeValue = DataMngr.getAttribute(attributes, attributeName);

			if (attributeName.equals(ldapAttributes.lastLogonTimeStamp))
			{
				if(!Strings.isNullOrEmpty(attributeValue))
				{
					relayStaffUser.setLastLogonTimestamp(new DateTime(Time.windowsToUnixTime(Long.parseLong
							(attributeValue))));
				}
			}
			else
			{
				mappedProperties.setProperty(attributeName, attributeValue);
			}
		}

		return relayStaffUser;
	}

	private Map<String, String> searchMap(String employeeId)
	{
		Map<String, String> searchMap = Maps.newHashMap();
		searchMap.put(ldapAttributes.employeeNumber, employeeId);
		return searchMap;
	}
}

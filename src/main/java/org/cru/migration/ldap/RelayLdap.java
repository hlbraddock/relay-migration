package org.cru.migration.ldap;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.ldap.attributes.LdapAttributesActiveDirectory;
import org.ccci.idm.util.DataMngr;
import org.ccci.idm.util.MappedProperties;
import org.ccci.idm.util.Time;
import org.cru.migration.domain.RelayStaff;
import org.cru.migration.domain.StaffRelayUserMap;
import org.cru.migration.exception.MoreThanOneUserFoundException;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.support.MigrationProperties;
import org.joda.time.DateTime;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RelayLdap
{
	private Ldap ldap;

	private LdapAttributesActiveDirectory ldapAttributes;

	private StaffRelayUserMap staffRelayUserMap;

	private String userRootDn;

	public RelayLdap(MigrationProperties migrationProperties) throws Exception
	{
		String password = Files.readFirstLine(new File(migrationProperties.getNonNullProperty("relayLdapPassword")),
				Charsets.UTF_8);

		ldap = new Ldap(migrationProperties.getNonNullProperty("relayLdapHost"), migrationProperties.getNonNullProperty("relayLdapUser")
				, password);

		userRootDn = migrationProperties.getNonNullProperty("relayUserRootDn");

		ldapAttributes = new LdapAttributesActiveDirectory();

		staffRelayUserMap = new StaffRelayUserMap(ldapAttributes);
	}

	public Set<String> getGroupMembers(String groupRoot, String filter) throws NamingException
	{
		Set<String> members = Sets.newHashSet();

		List<SearchResult> searchResults = ldap.search2(groupRoot, filter, new String[0]);

		for (SearchResult searchResult : searchResults)
		{
			String groupDn = searchResult.getName() + "," + groupRoot;

			Set<String> groupMembers = ldap.getGroupMembers(groupDn);

			members.addAll(groupMembers);
		}

		return members;
	}

	public RelayStaff getStaff(String employeeId) throws NamingException, UserNotFoundException, MoreThanOneUserFoundException
	{
		String[] returnAttributes = {ldapAttributes.username, ldapAttributes.lastLogonTimeStamp};

		Map<String, Attributes> results = ldap.searchAttributes(userRootDn, searchMap(employeeId), returnAttributes);

		List<RelayStaff> relayStaffs = getStaffRelayUser(returnAttributes, results);

		if(relayStaffs.size() <= 0)
			throw new UserNotFoundException();

		if(relayStaffs.size() > 1)
			throw new MoreThanOneUserFoundException();

		RelayStaff relayStaff = relayStaffs.get(0);

		relayStaff.setEmployeeId(employeeId);

		return relayStaff;
	}

	private List<RelayStaff> getStaffRelayUser(String[] returnAttributes, Map<String, Attributes> results)
	{
		List<RelayStaff> relayStaffs = Lists.newArrayList();

		for (Map.Entry<String, Attributes> entry : results.entrySet())
		{
			Attributes attributes = entry.getValue();

			relayStaffs.add(getStaffRelayUser(returnAttributes, attributes));
		}

		return relayStaffs;
	}

	private RelayStaff getStaffRelayUser(String[] returnAttributes, Attributes attributes)
	{
		RelayStaff relayStaff = new RelayStaff();

		MappedProperties<RelayStaff> mappedProperties = new MappedProperties<RelayStaff>(staffRelayUserMap,
				relayStaff);

		for (String attributeName : returnAttributes)
		{
			String attributeValue = DataMngr.getAttribute(attributes, attributeName);

			if (attributeName.equals(ldapAttributes.lastLogonTimeStamp))
			{
				if(!Strings.isNullOrEmpty(attributeValue))
				{
					relayStaff.setLastLogonTimestamp(new DateTime(Time.windowsToUnixTime(Long.parseLong
							(attributeValue))));
				}
			}
			else
			{
				mappedProperties.setProperty(attributeName, attributeValue);
			}
		}

		return relayStaff;
	}

	private Map<String, String> searchMap(String employeeId)
	{
		Map<String, String> searchMap = Maps.newHashMap();
		searchMap.put(ldapAttributes.employeeNumber, employeeId);
		return searchMap;
	}
}

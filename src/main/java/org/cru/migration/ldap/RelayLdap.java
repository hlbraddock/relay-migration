package org.cru.migration.ldap;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.ldap.attributes.LdapAttributesActiveDirectory;
import org.ccci.idm.util.DataMngr;
import org.ccci.idm.util.MappedProperties;
import org.ccci.idm.util.Time;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.domain.StaffRelayUserMap;
import org.cru.migration.exception.MoreThanOneUserFoundException;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;
import org.joda.time.DateTime;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
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
		ldap = new Ldap(migrationProperties.getNonNullProperty("relayLdapHost"),
				migrationProperties.getNonNullProperty("relayLdapUser"),
				migrationProperties.getNonNullProperty("relayLdapPassword"));

		userRootDn = migrationProperties.getNonNullProperty("relayUserRootDn");

		ldapAttributes = new LdapAttributesActiveDirectory();

		staffRelayUserMap = new StaffRelayUserMap(ldapAttributes);
	}

	public Set<String> getGroupMembers(String groupRoot, String filter) throws NamingException
	{
		Set<String> members = Sets.newHashSet();
		List<String> membersList = Lists.newArrayList();

		List<SearchResult> searchResults = ldap.search2(groupRoot, filter, new String[0]);

		for (SearchResult searchResult : searchResults)
		{
			String groupDn = searchResult.getName() + "," + groupRoot;

			List<String> listGroupMembers = ldap.getGroupMembers(groupDn);

			membersList.addAll(listGroupMembers);
		}

		Output.println("list members size is " + membersList.size() + " for groups in root " + groupRoot);

		// ensure unique membership (no duplicates)
		members.addAll(membersList);

		return members;
	}

	public RelayUser getRelayUserFromEmployeeId(String employeeId) throws NamingException, UserNotFoundException,
			MoreThanOneUserFoundException
	{
		String[] returnAttributes = { ldapAttributes.username, ldapAttributes.lastLogonTimeStamp,
				ldapAttributes.commonName};

		Map<String, Attributes> results = ldap.searchAttributes(userRootDn, searchMap(employeeId), returnAttributes);

		List<RelayUser> relayUsers = getRelayUsers(returnAttributes, results);

		if(relayUsers.size() <= 0)
			throw new UserNotFoundException();

		if(relayUsers.size() > 1)
			throw new MoreThanOneUserFoundException();

		RelayUser relayUser = relayUsers.get(0);

		relayUser.setEmployeeId(employeeId);

		return relayUser;
	}

	public RelayUser getRelayUserFromDn(String dn) throws NamingException, UserNotFoundException,
			MoreThanOneUserFoundException
	{
		String[] returnAttributes = { ldapAttributes.username, ldapAttributes.lastLogonTimeStamp,
				ldapAttributes.commonName};

		Map<String, Attributes> results = ldap.searchAttributes(userRootDn, dn.split(",")[0], returnAttributes);

		List<RelayUser> relayUsers = getRelayUsers(returnAttributes, results);

		if(relayUsers.size() <= 0)
			throw new UserNotFoundException();

		if(relayUsers.size() > 1)
			throw new MoreThanOneUserFoundException();

		return relayUsers.get(0);
	}

	private List<RelayUser> getRelayUsers(String[] returnAttributes, Map<String, Attributes> results)
	{
		List<RelayUser> relayUsers = Lists.newArrayList();

		for (Map.Entry<String, Attributes> entry : results.entrySet())
		{
			Attributes attributes = entry.getValue();

			RelayUser relayUser = getRelayUser(returnAttributes, attributes);

			relayUsers.add(relayUser);
		}

		return relayUsers;
	}

	private RelayUser getRelayUser(String[] returnAttributes, Attributes attributes)
	{
		RelayUser relayUser = new RelayUser();

		MappedProperties<RelayUser> mappedProperties = new MappedProperties<RelayUser>(staffRelayUserMap,
				relayUser);

		for (String attributeName : returnAttributes)
		{
			String attributeValue = DataMngr.getAttribute(attributes, attributeName);

			if (attributeName.equals(ldapAttributes.lastLogonTimeStamp))
			{
				if(!Strings.isNullOrEmpty(attributeValue))
				{
					relayUser.setLastLogonTimestamp(new DateTime(Time.windowsToUnixTime(Long.parseLong
							(attributeValue))));
				}
			}
			else
			{
				mappedProperties.setProperty(attributeName, attributeValue);
			}
		}

		return relayUser;
	}

	private Map<String, String> searchMap(String employeeId)
	{
		Map<String, String> searchMap = Maps.newHashMap();

		searchMap.put(ldapAttributes.employeeNumber, employeeId);

		return searchMap;
	}
}

package org.cru.migration.ldap;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.ldap.attributes.LdapAttributesActiveDirectory;
import org.ccci.idm.util.DataMngr;
import org.ccci.idm.util.MappedProperties;
import org.ccci.idm.util.Time;
import org.cru.migration.dao.LdapDao;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.domain.StaffRelayUserMap;
import org.cru.migration.exception.MoreThanOneUserFoundException;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.support.MigrationProperties;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RelayLdap
{
	private Logger logger = LoggerFactory.getLogger(getClass());

	private MigrationProperties migrationProperties;

	private Ldap ldap;

	private LdapDao ldapDao;

	private LdapAttributesActiveDirectory ldapAttributes;

	private StaffRelayUserMap staffRelayUserMap;

	private String userRootDn;

	private String[] attributeNames;

	public RelayLdap(MigrationProperties migrationProperties) throws Exception
	{
		this.migrationProperties = migrationProperties;

		ldap = new Ldap(migrationProperties.getNonNullProperty("relayLdapHost"),
				migrationProperties.getNonNullProperty("relayLdapUser"),
				migrationProperties.getNonNullProperty("relayLdapPassword"));

		ldapDao = new LdapDao(ldap);

		userRootDn = migrationProperties.getNonNullProperty("relayUserRootDn");

		ldapAttributes = new LdapAttributesActiveDirectory();

		staffRelayUserMap = new StaffRelayUserMap(ldapAttributes);

		List<String> attributeNamesList = Arrays.asList(
                ldapAttributes.city,
                ldapAttributes.commonName,
                ldapAttributes.country,
                ldapAttributes.departmentNumber,
                ldapAttributes.designationId,
                ldapAttributes.employeeNumber,
                ldapAttributes.employeeStatus,
                ldapAttributes.gender,
                ldapAttributes.givenname,
                ldapAttributes.hrStatusCode,
                ldapAttributes.jobCode,
                ldapAttributes.lastLogonTimeStamp,
                ldapAttributes.managerId,
                ldapAttributes.ministryCode,
                ldapAttributes.mobile,
                ldapAttributes.payGroup,
                ldapAttributes.ipPhone,
                ldapAttributes.postalCode,
                ldapAttributes.preferredName,
				ldapAttributes.proxyAddresses,
                ldapAttributes.state,
                ldapAttributes.surname,
                ldapAttributes.subMinistryCode,
                ldapAttributes.telephone,
                ldapAttributes.username
                );

		attributeNames = (String[]) attributeNamesList.toArray();
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

		logger.debug("Google group list members size is " + membersList.size() + " for groups in root " + groupRoot);

		// ensure unique membership (no duplicates)
		members.addAll(membersList);

		logger.debug("Google group set members size is " + members.size() + " for groups in root " + groupRoot);

		return members;
	}

	public RelayUser getRelayUserFromEmployeeId(String employeeId) throws NamingException, UserNotFoundException,
			MoreThanOneUserFoundException
	{
		Map<String, Attributes> results = ldap.searchAttributes(userRootDn, searchMap(employeeId), attributeNames);

		Set<RelayUser> relayUsers = getRelayUsers(attributeNames, results);

		if(relayUsers.size() <= 0)
			throw new UserNotFoundException();

		if(relayUsers.size() > 1)
			throw new MoreThanOneUserFoundException();

		RelayUser relayUser = Iterables.getOnlyElement(relayUsers);

		relayUser.setEmployeeId(employeeId);

		return relayUser;
	}

	public RelayUser getRelayUserFromDn(String dn) throws NamingException, UserNotFoundException,
			MoreThanOneUserFoundException
	{
		Map<String, Attributes> results = ldap.searchAttributes(userRootDn, dn.split(",")[0], attributeNames);

		Set<RelayUser> relayUsers = getRelayUsers(attributeNames, results);

		if(relayUsers.size() <= 0)
			throw new UserNotFoundException();

		if(relayUsers.size() > 1)
			throw new MoreThanOneUserFoundException();

		return Iterables.getOnlyElement(relayUsers);
	}

	public Map<String, Attributes> getEntries() throws NamingException
	{
		return getEntries(attributeNames);
	}

	private Map<String, Attributes> getEntries(String[] attributeNames) throws NamingException
	{
		String relayUserRootDn = migrationProperties.getNonNullProperty("relayUserRootDn");

		return ldapDao.getEntries(relayUserRootDn, "cn", attributeNames, 3);
	}

	public Set<RelayUser> getRelayUsers(Map<String, Attributes> results)
	{
		return getRelayUsers(attributeNames, results);
	}

	private Set<RelayUser> getRelayUsers(String[] returnAttributes, Map<String, Attributes> results)
	{
		Set<RelayUser> relayUsers = Sets.newHashSet();

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
			// handle multi valued attributes
			if (attributeName.equals(ldapAttributes.proxyAddresses))
			{
				relayUser.setProxyAddresses(DataMngr.getAttributes(attributes, attributeName));
				continue;
			}

			// handle single valued attributes
			String attributeValue = DataMngr.getAttribute(attributes, attributeName);

			if(Strings.isNullOrEmpty(attributeValue))
            {
                continue;
            }

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

package org.cru.migration.ldap;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.ldap.attributes.LdapAttributesActiveDirectory;
import org.cru.migration.dao.LdapDao;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.exception.MoreThanOneUserFoundException;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.service.RelayUsersFromLdapAttributesService;
import org.cru.migration.support.MigrationProperties;
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
                ldapAttributes.username,
                ldapAttributes.memberOf
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
		Map<String, Attributes> entries = ldap.searchAttributes(userRootDn, searchMapEmployeeId(employeeId), attributeNames);

        Set<RelayUser> invalidRelayUsers = Sets.newHashSet();
		Set<RelayUser> relayUsers = Sets.newHashSet();

        if(entries.size() >= 1)
        {
            relayUsers = getRelayUsersFromAttributes(entries, attributeNames, invalidRelayUsers);
        }

        if(relayUsers.size() <= 0)
			throw new UserNotFoundException();

		if(relayUsers.size() > 1)
			throw new MoreThanOneUserFoundException();

		RelayUser relayUser = Iterables.getOnlyElement(relayUsers);

		relayUser.setEmployeeId(employeeId);

		return relayUser;
	}

	public Set<RelayUser> getRelayUsersWithDesignation() throws NamingException,
			UserNotFoundException,
			MoreThanOneUserFoundException
	{
		Map<String, Attributes> entries = ldap.searchAttributes(userRootDn, searchMapDesignation("01*"),
				attributeNames);

		Set<RelayUser> invalidRelayUsers = Sets.newHashSet();

		Set<RelayUser> relayUsers = getRelayUsersFromAttributes(entries, attributeNames, invalidRelayUsers);

		return relayUsers;
	}

	public RelayUser getRelayUserFromDn(String dn) throws NamingException, UserNotFoundException,
			MoreThanOneUserFoundException
	{
		Map<String, Attributes> entries = ldap.searchAttributes(userRootDn, dn.split(",")[0], attributeNames);

        Set<RelayUser> invalidRelayUsers = Sets.newHashSet();
		Set<RelayUser> relayUsers = getRelayUsersFromAttributes(entries, attributeNames, invalidRelayUsers);

		if(relayUsers.size() <= 0)
			throw new UserNotFoundException();

		if(relayUsers.size() > 1)
			throw new MoreThanOneUserFoundException();

		return Iterables.getOnlyElement(relayUsers);
	}

	public Map<String, Attributes> getAllEntries() throws NamingException
	{
		return getAllEntries(attributeNames);
	}

	private Map<String, Attributes> getAllEntries(String[] attributeNames) throws NamingException
	{
		String relayUserRootDn = migrationProperties.getNonNullProperty("relayUserRootDn");

		return ldapDao.getEntries(relayUserRootDn, "cn", attributeNames, 3);
	}

	public Set<RelayUser> getRelayUsersFromAttributes(Map<String, Attributes> entries, Set<RelayUser> invalidRelayUsers) throws
            NamingException
	{
		return getRelayUsersFromAttributes(entries, attributeNames, invalidRelayUsers);
	}

	private Set<RelayUser> getRelayUsersFromAttributes(Map<String, Attributes> entries, String[] returnAttributes,
                                                       Set<RelayUser> invalidRelayUsers) throws NamingException
	{
        RelayUsersFromLdapAttributesService relayUsersFromLdapAttributesService = new
                RelayUsersFromLdapAttributesService();

        RelayUsersFromLdapAttributesService.Results results =
            relayUsersFromLdapAttributesService.getRelayUsers(entries, returnAttributes);

        invalidRelayUsers.clear();
        invalidRelayUsers.addAll(results.getInvalidRelayUsers());

		return results.getRelayUsers();
	}

	private Map<String, String> searchMapEmployeeId(String id)
	{
		Map<String, String> searchMap = Maps.newHashMap();

		searchMap.put(ldapAttributes.employeeNumber, id);

		return searchMap;
	}

	private Map<String, String> searchMapDesignation(String id)
	{
		Map<String, String> searchMap = Maps.newHashMap();

		searchMap.put(ldapAttributes.designationId, id);

		return searchMap;
	}
}

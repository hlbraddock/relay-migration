package org.cru.migration.service;

import com.google.common.collect.Sets;
import org.cru.migration.dao.CasAuditDao;
import org.cru.migration.dao.CssDao;
import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.domain.RelayUserGroups;
import org.cru.migration.exception.MoreThanOneUserFoundException;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.ldap.RelayLdap;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.Map;
import java.util.Set;

public class RelayUserService
{
	private Logger logger = LoggerFactory.getLogger(getClass());
	private MigrationProperties migrationProperties;
	private CssDao cssDao;
	private RelayLdap relayLdap;
	private CasAuditDao casAuditDao;

	public RelayUserService(MigrationProperties migrationProperties, CssDao cssDao, RelayLdap relayLdap,
							CasAuditDao casAuditDao)
	{
		this.migrationProperties = migrationProperties;
		this.cssDao = cssDao;
		this.relayLdap = relayLdap;
		this.casAuditDao = casAuditDao;
	}

	public Set<RelayUser> getAllRelayUsers() throws NamingException
	{
        logger.debug("Getting all Relay user entries ...");

        Map<String, Attributes> results = relayLdap.getAllEntries();

        logger.debug("Got all Relay user entries " + results.size());

        logger.debug("Getting all Relay users ...");

        Set<RelayUser> invalidRelayUsers = Sets.newHashSet();

        Set<RelayUser> allRelayUsers = relayLdap.getRelayUsersFromAttributes(results, invalidRelayUsers);

        logger.debug("Got all Relay users " + allRelayUsers.size());

        logger.debug("Got all Relay users with invalid relay users size " + invalidRelayUsers.size());

        Output.serializeRelayUsers(invalidRelayUsers,
                migrationProperties.getNonNullProperty("invalidRelayUsers"));

        return allRelayUsers;
	}

	public Set<RelayUser> compare(Set<RelayUser> relayUsers, Set<RelayUser> relayUsers1)
	{
		Set<RelayUser> differ = Sets.newHashSet();

		if(relayUsers.size() != relayUsers1.size())
		{
			logger.info("relay users size is different " + relayUsers.size() + " and " + relayUsers1.size());
			return relayUsers;
		}

		int counter = 0;

        logger.info("Comparing relay users " + relayUsers.size() + " and " + relayUsers1.size());

		Map<String, RelayUser> relayUser1MapSsoguid = RelayUser.getRelayUserMapGuid(relayUsers1);

		for(RelayUser relayUser : relayUsers)
		{
			if (counter++ % 1000 == 0)
			{
				System.out.printf("Comparing users " + counter + "\r");
			}

			RelayUser relayUser1 = relayUser1MapSsoguid.get(relayUser.getSsoguid());

			if(relayUser1 == null)
			{
				logger.info("difference from null !!!!!!");
				differ.add(relayUser);
			}
			else
			{
				StringBuffer difference = new StringBuffer();
				if(!relayUser.equals(relayUser1, true, difference))
				{
					differ.add(relayUser);
					logger.info("difference is !!!!!!" + difference);
				}
			}
		}

		logger.info("Done with comparison");

		return differ;
	}

	public Set<RelayUser> fromPshrData(Set<PSHRStaff> pshrStaffList,
													Set<PSHRStaff> notFoundInRelay,
													Set<PSHRStaff> moreThanOneFoundWithEmployeeId,
													Set<RelayUser> duplicateRelayUsers)
	{
		Set<RelayUser> relayUsers = Sets.newHashSet();

		int counter = 0;
		for (PSHRStaff pshrStaff : pshrStaffList)
		{
			try
			{
				RelayUser relayUser = relayLdap.getRelayUserFromEmployeeId(pshrStaff.getEmployeeId());
				relayUser.setUsstaff(true);
				int size = relayUsers.size();
				relayUsers.add(relayUser);
				if(relayUsers.size() == size)
				{
					duplicateRelayUsers.add(relayUser);
				}
			}
			catch (UserNotFoundException e)
			{
				notFoundInRelay.add(pshrStaff);
			}
			catch (MoreThanOneUserFoundException e)
			{
				moreThanOneFoundWithEmployeeId.add(pshrStaff);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			if (counter++ % 1000 == 0)
			{
				System.out.printf("Getting staff from Relay count is " + relayUsers.size() + " of total PSHR staff "
						+ counter + "\r");
			}
		}

		System.out.println();

		logger.info("Done with getting staff.");

		return relayUsers;
	}

	public Set<RelayUser> getWithLoginTimestamp(Set<RelayUser> relayUsers)
	{
		Set<RelayUser> relayUsersWithLogonTimestamp = Sets.newHashSet();

		for(RelayUser relayUser : relayUsers)
		{
			if(relayUser.getLastLogonTimestamp() != null)
			{
				relayUsersWithLogonTimestamp.add(relayUser);
			}
		}

		return relayUsersWithLogonTimestamp;
	}

	// TODO verify
	public Set<RelayUser> getLoggedInSince(Set<RelayUser> relayUsers, DateTime loggedInSince)
	{
		Set<RelayUser> relayUsersLoggedInSince = Sets.newHashSet();
		for(RelayUser relayUser : relayUsers)
		{
			if(relayUser.getLastLogonTimestamp() != null && (relayUser.getLastLogonTimestamp().isAfter(loggedInSince
					.getMillis())))
			{
				relayUsersLoggedInSince.add(relayUser);
			}
		}

		return relayUsersLoggedInSince;
	}


	public Set<RelayUser> fromDistinguishedNames(Set<String> entries, Boolean isGoogle)
	{
		Set<RelayUser> relayUsers = Sets.newHashSet();

		int counter = 0;
		for (String entry : entries)
		{
			try
			{
				RelayUser relayUser = relayLdap.getRelayUserFromDn(entry);
				relayUser.setGoogle(isGoogle);
				relayUsers.add(relayUser);
			}
			catch (UserNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (MoreThanOneUserFoundException e)
			{
				e.printStackTrace();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			if (counter++ % 1000 == 0)
			{
				System.out.printf("fromDistinguishedNames() getting users from Relay by distinguished name count is " +
						relayUsers.size() + " of " + "total " + counter + "\r");
			}
		}

		System.out.println();

		logger.debug("Done with total relay count " + counter);

		return relayUsers;
	}

	public void setPasswords(RelayUserGroups relayUserGroupsGroupings) throws NamingException
	{
		logger.debug("Set Relay user passwords");

		Set<RelayUser> relayUsers = relayUserGroupsGroupings.getAllUsers();
		Set<RelayUser> relayUsersWithPassword = Sets.newHashSet();
		Set<RelayUser> relayUsersWithoutPassword = Sets.newHashSet();

		logger.debug("Relay user size is " + relayUsers.size());

		Set<CssRelayUser> cssRelayUsers = cssDao.getCssRelayUsers(RelayUser.getSsoguids(relayUsers));

		logger.debug("CSS relay users size is " + cssRelayUsers.size());

		logger.debug("Setting relay users passwords ...");

        SetRelayUsersPasswordService setRelayUsersPasswordService = new SetRelayUsersPasswordService();

        SetRelayUsersPasswordService.Results results =
                setRelayUsersPasswordService.setRelayUsersPassword(cssRelayUsers,
						relayUserGroupsGroupings.getAllUsersSsoguidKey());

		logger.debug("Done setting relay users passwords.");

        relayUsersWithPassword.clear();
        relayUsersWithPassword.addAll(results.getRelayUsersWithPassword());
        relayUsersWithoutPassword.addAll(relayUsers);
		relayUsersWithoutPassword.removeAll(relayUsersWithPassword);

		relayUserGroupsGroupings.setWithPassword(relayUsersWithPassword);
		relayUserGroupsGroupings.setWithoutPassword(relayUsersWithoutPassword);
	}

	public void setLastLogonTimestamp(RelayUserGroups relayUserGroups) throws NamingException
	{
		logger.debug("Setting relay last logon timestamp (from audit) ... for relay user set size " +
                relayUserGroups.getAllUsers().size());

        LastLogonService lastLogonService = new LastLogonService(casAuditDao);

        LastLogonService.Results lastLoginServiceResults = lastLogonService.setLastLogon(relayUserGroups.getAllUsers());

		logger.debug("Setting relay last logon timestamp (from audit) complete.");
		logger.debug("Number of relay users with audit last logon time stamp " + lastLoginServiceResults.getNumberRelayUsersSet());
		logger.debug("Number of relay users not found in cas audit table " + lastLoginServiceResults.getNotFound().size());
		logger.debug("Number of relay users with audit last logon time stamp NULL " + lastLoginServiceResults.getNullDateCount());

		Output.serializeRelayUsers(lastLoginServiceResults.getNotFound(),
				migrationProperties.getNonNullProperty("relayUsersNotFoundInCasAudit"));

		relayUserGroups.setNotFoundInCasAuditLog(lastLoginServiceResults.getNotFound());
	}
}

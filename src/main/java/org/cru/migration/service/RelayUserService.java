package org.cru.migration.service;

import com.google.common.collect.Sets;
import org.cru.migration.dao.CasAuditDao;
import org.cru.migration.dao.CssDao;
import org.cru.migration.domain.CasAuditUser;
import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.exception.MoreThanOneUserFoundException;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.ldap.RelayLdap;
import org.cru.migration.support.FileHelper;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;
import org.cru.migration.support.StringUtilities;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Set;

public class RelayUserService
{
	private MigrationProperties migrationProperties = new MigrationProperties();
	private CssDao cssDao;
	private RelayLdap relayLdap;
	private CasAuditDao casAuditDao;

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

		Output.println("");
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


	public Set<RelayUser> fromDistinguishedNames(Set<String> entries)
	{
		Set<RelayUser> relayUsers = Sets.newHashSet();

		int counter = 0;
		for (String entry : entries)
		{
			try
			{
				RelayUser relayUser = relayLdap.getRelayUserFromDn(entry);
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
				System.out.printf("Runtime counter: Getting users from Relay count is " + relayUsers.size() + " of " +
						"total "
						+ counter + "\r");
			}
		}

		Output.println("Done with total relay count " + counter);

		return relayUsers;
	}

	public void setPasswords(Set<RelayUser> relayUsers, Set<RelayUser> relayUsersWithPassword,
							 Set<RelayUser> relayUsersWithoutPassword) throws IOException
	{
		Output.println("Set Relay user passwords");

		Output.println("Relay user size is " + relayUsers.size());

		Set<CssRelayUser> cssRelayUsers = cssDao.getCssRelayUsers(RelayUser.getSsoguids(relayUsers));

		Output.println("CSS relay users size is " + cssRelayUsers.size());

		Output.println("Setting relay users passwords ...");

		RelayUser relayUser;
		for(CssRelayUser cssRelayUser : cssRelayUsers)
		{
			relayUser = RelayUser.havingSsoguid(relayUsers, cssRelayUser.getSsoguid());

			if(relayUser != null)
			{
				relayUser.setPassword(cssRelayUser.getPassword());
				relayUsersWithPassword.add(relayUser);
			}
		}

		Output.println("Done setting relay users passwords.");

		relayUsersWithoutPassword.addAll(relayUsers);
		relayUsersWithoutPassword.removeAll(relayUsersWithPassword);

		Output.println("Relay users with password set size " + relayUsersWithPassword.size());
		Output.println("Relay users without password set size " + relayUsersWithoutPassword.size());
		Output.logRelayUser(relayUsersWithPassword,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersWithPasswordSet")));
		Output.logRelayUser(relayUsersWithoutPassword,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersWithoutPasswordSet")));
	}

	public void setLastLogonTimestamp(Set<RelayUser> relayUsers) throws IOException
	{
		Output.println("Setting relay last logon timestamp (from audit) ... for relay user set size " + relayUsers.size());

		Set<RelayUser> notFound = Sets.newHashSet();

		CasAuditUser casAuditUser;
		int count = 0;
		int setLastLogonTimestampCount = 0;
		int nullCasAuditUserCount = 0;
		int nullDateCount = 0;

		for(RelayUser relayUser : relayUsers)
		{
			if(count++ % 1000 == 0)
			{
				System.out.printf("Set " + count + " relay users.\r");
			}

			casAuditUser = getCasAuditUser(relayUser.getUsername());
			if(casAuditUser != null)
			{
				if(casAuditUser.getDate() != null)
				{
					setLastLogonTimestampCount++;
					relayUser.setLastLogonTimestamp(casAuditUser.getDate());
				}
				else
				{
					nullDateCount++;
				}
			}
			else
			{
				nullCasAuditUserCount++;
				notFound.add(relayUser);
			}
		}

		Output.println("");
		Output.println("Setting relay last logon timestamp (from audit) complete.");
		Output.println("Number of relay users with audit last logon time stamp " + setLastLogonTimestampCount);
		Output.println("Number of relay users not found in cas audit table " + nullCasAuditUserCount);
		Output.println("Number of relay users with audit last logon time stamp NULL " + nullDateCount);
		Output.logRelayUser(notFound,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersNotFoundInCasAudit")));
	}

	private CasAuditUser getCasAuditUser(String username)
	{
		CasAuditUser casAuditUser = casAuditDao.getCasAuditUser(username);
		if(casAuditUser == null)
		{
			casAuditUser = casAuditDao.getCasAuditUser(username.toLowerCase());
		}
		if(casAuditUser == null)
		{
			casAuditUser = casAuditDao.getCasAuditUser(username.toUpperCase());
		}
		if(casAuditUser == null)
		{
			casAuditUser = casAuditDao.getCasAuditUser(StringUtilities.isEmail(username) ? StringUtilities
					.capitalizeEmail(username) : StringUtilities.capitalize(username, ".", "\\."));
		}

		return casAuditUser;
	}

	public void setCssDao(CssDao cssDao)
	{
		this.cssDao = cssDao;
	}

	public void setRelayLdap(RelayLdap relayLdap)
	{
		this.relayLdap = relayLdap;
	}

	public void setCasAuditDao(CasAuditDao casAuditDao)
	{
		this.casAuditDao = casAuditDao;
	}
}
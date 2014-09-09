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
				Output.println("Getting staff from Relay count is " + relayUsers.size() + " of total PSHR staff "
						+ counter);
			}
		}

		return relayUsers;
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
				Output.println("Getting users from Relay count is " + relayUsers.size() + " of total "
						+ counter);
			}
		}

		return relayUsers;
	}

	public void setPasswords(Set<RelayUser> relayUsers) throws IOException
	{
		Output.println("Set Relay user passwords");

		Output.println("Relay user size is " + relayUsers.size());

		Set<CssRelayUser> cssRelayUsers = cssDao.getCssRelayUsers(RelayUser.getSsoguids(relayUsers));

		Output.println("CSS relay users size is " + cssRelayUsers.size());

		Output.println("Setting relay users passwords ...");

		RelayUser relayUser;
		Set<RelayUser> relayUsersWithPasswordSet = Sets.newHashSet();
		for(CssRelayUser cssRelayUser : cssRelayUsers)
		{
			relayUser = RelayUser.havingSsoguid(relayUsers, cssRelayUser.getSsoguid());

			if(relayUser != null)
			{
				relayUser.setPassword(cssRelayUser.getPassword());
				relayUsersWithPasswordSet.add(relayUser);
			}
		}

		Output.println("Done setting relay users passwords.");

		Set<RelayUser> relayUsersWithoutPasswordSet = Sets.newHashSet();
		relayUsersWithoutPasswordSet.addAll(relayUsers);
		relayUsersWithoutPasswordSet.removeAll(relayUsersWithPasswordSet);

		Output.println("Relay users with password set size " + relayUsersWithPasswordSet.size());
		Output.println("Relay users without password set size " + relayUsersWithoutPasswordSet.size());
		Output.logRelayUser(relayUsersWithPasswordSet,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersWithPasswordSet")));
		Output.logRelayUser(relayUsersWithoutPasswordSet,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersWithoutPasswordSet")));
	}

	public void setLastLogonTimestamp(Set<RelayUser> relayUsers)
	{
		Output.println("Setting relay last logon timestamp (from audit) ... for relay user set size " + relayUsers.size());
		CasAuditUser casAuditUser;
		int count = 0;
		for(RelayUser relayUser : relayUsers)
		{
			if(count++ % 1000 == 0)
			{
				Output.println("Set " + count + " relay users.");
			}
			casAuditUser = casAuditDao.getCasAuditUser(relayUser.getUsername());
			if(casAuditUser != null)
			{
				if(casAuditUser.getDate() != null)
				{
					relayUser.setLastLogonTimestamp(casAuditUser.getDate());
				}
			}
		}

		Output.println("Setting relay last logon timestamp (from audit) complete.");
		Output.println("Number of relay users with audit last logon time stamp " + count);
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

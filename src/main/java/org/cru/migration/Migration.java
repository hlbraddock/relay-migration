package org.cru.migration;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.cru.migration.dao.PSHRDao;
import org.cru.migration.dao.PSHRDaoFactory;
import org.cru.migration.domain.PSHRStaff;

import org.cru.migration.domain.RelayUser;
import org.cru.migration.exception.MoreThanOneUserFoundException;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.ldap.RelayLdap;
import org.cru.migration.ldap.TheKeyLdap;
import org.cru.migration.support.FileHelper;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;

import javax.naming.NamingException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class Migration
{
	public static void main(String[] args) throws Exception
	{
		Migration migration = new Migration();

		try
		{
			Action action = Action.Staff;

			if (action.equals(Action.SystemEntries))
				migration.createSystemEntries();
			else if (action.equals(Action.Staff))
				migration.getRelayUser();
			else if (action.equals(Action.GoogleUsers))
				migration.getGoogleUsers();
			else if (action.equals(Action.AuthoritativeRelayUsers))
				migration.getAuthoritativeRelayUsers();
			else if (action.equals(Action.Test))
				migration.test();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private enum Action
	{
		SystemEntries, Staff, GoogleUsers, AuthoritativeRelayUsers, Test
	}

	private MigrationProperties migrationProperties;
	private RelayLdap relayLdap;

	public Migration() throws Exception
	{
		migrationProperties = new MigrationProperties();

		relayLdap = new RelayLdap(migrationProperties);
	}

	/**
	 * Create Relay system account entries in the Key
	 *
	 * @throws Exception
	 */
	public void createSystemEntries() throws Exception
	{
		TheKeyLdap theKeyLdap = new TheKeyLdap(migrationProperties);

		theKeyLdap.createSystemEntries();
	}

	public void getAuthoritativeRelayUsers() throws Exception
	{
		Set<RelayUser> relayUserUsers = Sets.newHashSet();

		relayUserUsers.addAll(getRelayUser());

		Set<RelayUser> relayGoogleUsers = getGoogleUsers();

		relayGoogleUsers.addAll(relayUserUsers);
	}

	public Set<RelayUser> getRelayUser() throws Exception
	{
		Output.println("Getting staff from PSHR ...");
		List<PSHRStaff> pshrUSStaff = getPshrUSStaff();
		Output.println("PSHR staff count " + pshrUSStaff.size());

		Output.println("Getting staff from Relay ...");
		List<PSHRStaff> notFoundInRelay = Lists.newArrayList();
		List<PSHRStaff> moreThanOneFoundWithEmployeeId = Lists.newArrayList();
		List<RelayUser> duplicateRelayUsers = Lists.newArrayList();
		Set<RelayUser> relayUsers = getRelayUsersFromPshrList(pshrUSStaff, notFoundInRelay,
				moreThanOneFoundWithEmployeeId, duplicateRelayUsers);

		Output.println("Staff Relay user count " + relayUsers.size());
		Output.println("Not found in Relay user count " + notFoundInRelay.size());
		Output.println("More than one found with employee id user count " + moreThanOneFoundWithEmployeeId.size());
		Output.println("Duplicate relay user count " +  duplicateRelayUsers.size());
		Output.logRelayUser(relayUsers, FileHelper.getFile(migrationProperties.getNonNullProperty
				("staffRelayUsersLogFile")));
		Output.logPSHRStaff(moreThanOneFoundWithEmployeeId, FileHelper.getFile(migrationProperties.getNonNullProperty
				("moreThanOneRelayUserWithEmployeeId")));

		return relayUsers;
	}

	private Set<RelayUser> getGoogleUsers() throws NamingException, UserNotFoundException,
		MoreThanOneUserFoundException
	{
		Set<String> members = relayLdap.getGroupMembers(
				migrationProperties.getNonNullProperty("relayGoogleGroupsRoot"),
				migrationProperties.getNonNullProperty("relayGoogleGroupsMailNode"));

		Output.println("set members size is " + members.size());

		Set<RelayUser> relayUsers = getRelayUsersFromListOfDistinguishedNames(members);

		Output.println("relay users size is " + relayUsers.size());

		return relayUsers;
	}

	private Set<RelayUser> getRelayUsersFromPshrList(List<PSHRStaff> pshrStaffList, List<PSHRStaff> notFoundInRelay,
													 List<PSHRStaff> moreThanOneFoundWithEmployeeId,
													 List<RelayUser> duplicateRelayUsers)
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

	private Set<RelayUser> getRelayUsersFromListOfDistinguishedNames(Set<String> entries)
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

	private List<PSHRStaff> getPshrUSStaff() throws IOException, PropertyVetoException
	{
		PSHRDao pshrDao = PSHRDaoFactory.getInstance(migrationProperties);

		return pshrDao.getAllUSStaff();
	}

	public void test()
	{
	}
}

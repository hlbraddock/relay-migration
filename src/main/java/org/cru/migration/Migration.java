package org.cru.migration;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.cru.migration.dao.PSHRDao;
import org.cru.migration.dao.PSHRDaoFactory;
import org.cru.migration.domain.PSHRStaff;

import org.cru.migration.domain.RelayStaff;
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
			Action action = Action.Test;

			if (action.equals(Action.SystemEntries))
				migration.createSystemEntries();
			else if (action.equals(Action.Staff))
				migration.getRelayStaff();
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

	public void test()
	{
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
		Set<RelayStaff> relayStaffUsers = Sets.newHashSet();

		relayStaffUsers.addAll(getRelayStaff());

		Set<RelayUser> relayGoogleUsers = getGoogleUsers();

		relayGoogleUsers.addAll(relayStaffUsers);
	}

	public List<RelayStaff> getRelayStaff() throws Exception
	{
		Output.println("Getting staff from PSHR ...");
		List<PSHRStaff> pshrUSStaff = getPshrUSStaff();
		Output.println("PSHR staff count " + pshrUSStaff.size());

		Output.println("Getting staff from Relay ...");
		List<PSHRStaff> notFoundInRelay = Lists.newArrayList();
		List<PSHRStaff> moreThanOneFoundWithEmployeeId = Lists.newArrayList();
		List<RelayStaff> relayStaffs = getRelayStaff(pshrUSStaff, notFoundInRelay, moreThanOneFoundWithEmployeeId);

		Output.println("Staff Relay user count " + relayStaffs.size());
		Output.println("Not found in Relay user count " + notFoundInRelay.size());
		Output.println("More than one found with employee id user count " + moreThanOneFoundWithEmployeeId.size());
		Output.logRelayStaff(relayStaffs, FileHelper.getFile(migrationProperties.getNonNullProperty
				("staffRelayUsersLogFile")));
		Output.logPSHRStaff(moreThanOneFoundWithEmployeeId, FileHelper.getFile(migrationProperties.getNonNullProperty
				("moreThanOneRelayUserWithEmployeeId")));

		return relayStaffs;
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

	private List<RelayStaff> getRelayStaff(List<PSHRStaff> pshrStaffList, List<PSHRStaff> notFoundInRelay,
										   List<PSHRStaff> moreThanOneFoundWithEmployeeId)
	{
		List<RelayStaff> relayStaffs = Lists.newArrayList();

		int counter = 0;
		for (PSHRStaff pshrStaff : pshrStaffList)
		{
			try
			{
				RelayStaff relayStaff = relayLdap.getRelayStaffFromEmployeeId(pshrStaff.getEmployeeId());
				relayStaffs.add(relayStaff);
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
				Output.println("Getting staff from Relay count is " + relayStaffs.size() + " of total PSHR staff "
						+ counter);
			}
		}

		return relayStaffs;
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
}

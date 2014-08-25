package org.cru.migration;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.cru.migration.dao.PSHRDao;
import org.cru.migration.dao.PSHRDaoFactory;
import org.cru.migration.domain.PSHRStaff;

import org.cru.migration.domain.RelayStaffUser;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.ldap.RelayLdap;
import org.cru.migration.ldap.TheKeyLdap;
import org.cru.migration.support.FileHelper;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.List;

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
				migration.getRelayStaff();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private MigrationProperties migrationProperties;
	private RelayLdap relayLdap;

	private enum Action
	{
		SystemEntries, Staff
	}

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

	public void getRelayStaff() throws Exception
	{
		Output.println("Getting staff from PSHR ...");

		List<PSHRStaff> pshrUSStaff = getPshrUSStaff();

		Output.println("PSHR staff count " + pshrUSStaff.size());

		Output.println("Getting staff from Relay ...");

		List<PSHRStaff> notFoundInRelay = Lists.newArrayList();
		List<RelayStaffUser> relayStaffUsers = getRelayUsers(pshrUSStaff, notFoundInRelay);

		Output.println("Staff Relay users count " + relayStaffUsers.size());
		Output.println("Not found in Relay users count " + notFoundInRelay.size());

		logStaffRelayUsers(relayStaffUsers);
	}

	private List<RelayStaffUser> getRelayUsers(List<PSHRStaff> pshrStaffList, List<PSHRStaff> notFoundInRelay)
	{
		List<RelayStaffUser> relayStaffUsers = Lists.newArrayList();

		int counter = 0;
		for (PSHRStaff pshrStaffRole : pshrStaffList)
		{
			try
			{
				relayStaffUsers.add(relayLdap.getStaff(pshrStaffRole.getEmployeeId()));
			}
			catch (UserNotFoundException e)
			{
				notFoundInRelay.add(pshrStaffRole);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			if (counter++ % 1000 == 0)
			{
				Output.println("Getting staff from Relay count is " + relayStaffUsers.size() + " of total PSHR staff "
						+ counter);
			}
		}

		return relayStaffUsers;
	}

	private List<PSHRStaff> getPshrUSStaff() throws IOException, PropertyVetoException
	{
		PSHRDao pshrDao = PSHRDaoFactory.getInstance(migrationProperties);

		return pshrDao.getAllUSStaff();
	}

	private void logStaffRelayUsers(List<RelayStaffUser> relayStaffUsers) throws IOException
	{
		File staffRelayUsersLogFile = FileHelper.getFile(migrationProperties.getNonNullProperty
				("staffRelayUsersLogFile"));
		for (RelayStaffUser relayStaffUser : relayStaffUsers)
		{
			Files.append(relayStaffUser.getUsername() + " " + relayStaffUser.getEmployeeId() + "\n",
					staffRelayUsersLogFile, Charsets.UTF_8);
		}
	}
}

package org.cru.migration;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.cru.migration.dao.PSHRDao;
import org.cru.migration.dao.PSHRDaoFactory;
import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.domain.RelayStaffList;
import org.cru.migration.domain.StaffRelayUser;
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

		RelayStaffList relayStaffList = getRelayUsers(pshrUSStaff);

		Output.println("Staff Relay users count " + relayStaffList.getStaffRelayUsers().size());
		Output.println("Not found in Relay users count " + relayStaffList.getNotFoundInRelay().size());

		logStaffRelayUsers(relayStaffList.getStaffRelayUsers());
	}

	private RelayStaffList getRelayUsers(List<PSHRStaff> pshrStaffList)
	{
		List<PSHRStaff> notFoundInRelay = Lists.newArrayList();
		List<StaffRelayUser> staffRelayUsers = Lists.newArrayList();

		int counter = 0;
		for (PSHRStaff pshrStaffRole : pshrStaffList)
		{
			try
			{
				staffRelayUsers.add(relayLdap.getStaff(pshrStaffRole.getEmployeeId()));
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
				Output.println("Getting staff from Relay count is " + staffRelayUsers.size() + " of total PSHR staff "
						+ counter);
			}
		}

		return new RelayStaffList(staffRelayUsers, notFoundInRelay);
	}

	private List<PSHRStaff> getPshrUSStaff() throws IOException, PropertyVetoException
	{
		PSHRDao pshrDao = PSHRDaoFactory.getInstance(migrationProperties);

		return pshrDao.getAllUSStaff();
	}

	private void logStaffRelayUsers(List<StaffRelayUser> staffRelayUsers) throws IOException
	{
		File staffRelayUsersLogFile = FileHelper.getFile(migrationProperties.getNonNullProperty
				("staffRelayUsersLogFile"));
		for (StaffRelayUser staffRelayUser : staffRelayUsers)
		{
			Files.append(staffRelayUser.getUsername() + " " + staffRelayUser.getEmployeeId() + "\n",
					staffRelayUsersLogFile, Charsets.UTF_8);
		}
	}
}

package org.cru.migration;

import com.google.common.collect.Lists;
import org.ccci.idm.dao.IdentityDAO;
import org.ccci.idm.dao.entity.PSHRStaffRole;
import org.ccci.idm.obj.IdentityUser;
import org.cru.migration.dao.IdentityDaoFactory;
import org.cru.migration.dao.PSHRDao;
import org.cru.migration.dao.PSHRDaoFactory;
import org.cru.migration.domain.StaffRelayUser;
import org.cru.migration.exception.UserNotFoundException;
import org.cru.migration.ldap.RelayLdap;
import org.cru.migration.ldap.TheKeyLdap;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;

import java.util.List;

public class Migration
{
	public static void main(String[] args)
	{
		Migration migration = new Migration();

		try
		{
			Action action = Action.Staff;

			if(action.equals(Action.SystemEntries))
				migration.createSystemEntries();
			else if(action.equals(Action.Users))
				migration.getUsers();
			else if(action.equals(Action.Staff))
				migration.getRelayStaff();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private MigrationProperties migrationProperties;

	private enum Action
	{
		SystemEntries, Users, Staff
	}

	public Migration()
	{
		migrationProperties = new MigrationProperties();
	}

	public void createSystemEntries() throws Exception
	{
		TheKeyLdap theKeyLdap = new TheKeyLdap(migrationProperties);

		theKeyLdap.createSystemEntries();
	}

	public void getRelayStaff() throws Exception
	{
		RelayLdap relayLdap = new RelayLdap(migrationProperties);

		PSHRDao pshrDao = PSHRDaoFactory.getInstance(migrationProperties);

		Output.println("Getting staff from PSHR ...");

		List<PSHRStaffRole> pshrStaffRoles = pshrDao.getAllUSStaff();

		Output.println("PSHR staff count " + pshrStaffRoles.size());

		List<StaffRelayUser> staffRelayUsers = Lists.newArrayList();

		Output.println("Getting staff from Relay ...");

		List<PSHRStaffRole> notFoundInRelay = Lists.newArrayList();
		int counter = 0;
		for(PSHRStaffRole pshrStaffRole : pshrStaffRoles)
		{
			try
			{
				staffRelayUsers.add(relayLdap.getStaff(pshrStaffRole.getEmployeeId()));
			}
			catch(UserNotFoundException e)
			{
				notFoundInRelay.add(pshrStaffRole);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

			if(counter++%1000 == 0)
			{
				Output.println("Getting staff from Relay count is " + staffRelayUsers.size() + " of total PSHR staff " + counter);
			}
		}

		Output.println("Not found in Relay users count " + notFoundInRelay.size());

		Output.println("Staff Relay users count " + staffRelayUsers.size());

		counter = 0;
		for(StaffRelayUser staffRelayUser : staffRelayUsers)
		{
			Output.println("Staff Relay user " + staffRelayUser.getUsername() + " " + staffRelayUser.getEmployeeId() + " : " + counter++);
		}
	}


	public void getUsers() throws Exception
	{
		IdentityDAO identityDAO = IdentityDaoFactory.getInstance(migrationProperties);

		String username = "lee.braddock@cru.org";

		IdentityUser identityUser = new IdentityUser();
		identityUser.getAccount().setUsername(username);
		identityUser = identityDAO.load(identityUser);

		if(identityUser == null)
			System.out.println("no identity user");

		Output.print(identityUser);
	}
}

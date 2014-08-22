package org.cru.migration;

import org.ccci.idm.dao.IdentityDAO;
import org.ccci.idm.obj.IdentityUser;
import org.cru.migration.dao.IdentityDaoFactory;
import org.cru.migration.domain.StaffRelayUser;
import org.cru.migration.ldap.RelayLdap;
import org.cru.migration.ldap.TheKeyLdap;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;

public class Migration
{
	private enum Action
	{
		SystemEntries, Users, Staff
	}

	public static void main(String[] args)
	{
		Migration migration = new Migration();

		Action action = Action.Staff;

		try
		{
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

	public void createSystemEntries() throws Exception
	{
		MigrationProperties properties = new MigrationProperties();

		TheKeyLdap theKeyLdap = new TheKeyLdap(properties);

		theKeyLdap.createSystemEntries();
	}

	public void getRelayStaff() throws Exception
	{
		MigrationProperties properties = new MigrationProperties();

		RelayLdap relayLdap = new RelayLdap(properties);

		String employeeId = "000593885";

		StaffRelayUser staffRelayUser = relayLdap.getStaff(employeeId);

		System.out.println(staffRelayUser);
	}


	public void getUsers() throws Exception
	{
		MigrationProperties properties = new MigrationProperties();

		IdentityDAO identityDAO = IdentityDaoFactory.getInstance(properties);

		String username = "lee.braddock@cru.org";

		IdentityUser identityUser = new IdentityUser();
		identityUser.getAccount().setUsername(username);
		identityUser = identityDAO.load(identityUser);

		if(identityUser == null)
			System.out.println("no identity user");

		Output.print(identityUser);
	}
}

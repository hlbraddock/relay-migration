package org.cru.migration;

import org.cru.migration.ldap.TheKeyLdap;
import org.cru.migration.support.MigrationProperties;

public class Migration
{
	public static void main(String[] args)
	{
		Migration migration = new Migration();

		try
		{
			migration.createSystemUsers();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void createSystemUsers() throws Exception
	{
		MigrationProperties properties = new MigrationProperties();

		TheKeyLdap theKeyLdap = new TheKeyLdap(properties);

		theKeyLdap.createSystemEntries();
	}

	public void migrateUsers()
	{
		MigrationProperties properties = new MigrationProperties();

		// get all U.S. Staff

		// get all Relay google users

		// get previously linked users
	}

}

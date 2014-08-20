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

		/**
			Part One : U.S. staff

			get all U.S. staff from PSHR

			get U.S. staff Relay users (from employee id)

			get all non U.S. staff Relay google users

			get all passwords for collected Relay users

			if (ssoguid match and/or username match and/or already linked)
				update matching Key entry with Relay username and password
		 		add Relay corporate staff data to the Key entry
		 	else (no matching entry in the Key)
				create the Key user from Relay user


			Part Two : All others (non U.S. staff)

			get all other (non u.s. staff) Relay users

		 	if (ssoguid match and/or username match and/or already linked)
		 		merge user data?
		 		if (Relay last login is more recent than the Key)
		 			update matching Key entry with Relay username and password
		 	else (no matching entry in the Key)
		 		create the Key user from Relay user
		 */
	}

}

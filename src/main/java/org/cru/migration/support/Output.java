package org.cru.migration.support;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.ccci.idm.obj.IdentityUser;
import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.domain.RelayStaff;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Output
{
	public static void println(String string)
	{
		System.out.println(string);
	}

	public static void print(IdentityUser identityUser)
	{
		System.out.println("ssoguid:" + identityUser.getAccount().getSsoguid());
		System.out.println("cn:" + identityUser.getAccount().getCn());
		System.out.println("comment:" + identityUser.getAccount().getComment());
		System.out.println("hashed pwd:" + identityUser.getAccount().getHashedPwd());
		System.out.println("last login:" + identityUser.getAccount().getLastLogonTimestamp());
		System.out.println("pwd last set:" + identityUser.getAccount().getPwdLastSet());
		System.out.println("username:" + identityUser.getAccount().getUsername());
		System.out.println("password:" + identityUser.getAccount().getPassword());
	}

	public static void logRelayStaff(List<RelayStaff> relayStaffs, File logFile) throws IOException
	{
		for (RelayStaff relayStaff : relayStaffs)
		{
			Files.append(relayStaff.getUsername() + " " + relayStaff.getEmployeeId() + "\n",
					logFile, Charsets.UTF_8);
		}
	}

	public static void logPSHRStaff(List<PSHRStaff> pshrStaffList, File logFile) throws IOException
	{
		for (PSHRStaff pshrStaff : pshrStaffList)
		{
			Files.append(pshrStaff.getFirstName() + " " + pshrStaff.getLastName() + ", " + pshrStaff.getEmployeeId() + "\n",
					logFile, Charsets.UTF_8);
		}
	}
}

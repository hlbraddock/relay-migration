package org.cru.migration.support;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.domain.RelayUser;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Output
{
	public static void println(String string)
	{
		System.out.println(string);
	}

	public static void logRelayUser(List<RelayUser> relayUsers, File logFile) throws IOException
	{
		for (RelayUser relayUser : relayUsers)
		{
			Files.append(relayUser.getUsername() + " " + relayUser.getEmployeeId() + "\n",
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

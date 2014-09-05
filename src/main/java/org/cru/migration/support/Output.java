package org.cru.migration.support;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.domain.RelayUser;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class Output
{
	public static void println(String string)
	{
		System.out.println(string);
	}

	public static void logRelayUser(Set<RelayUser> relayUsers, File logFile) throws IOException
	{
		for (RelayUser relayUser : relayUsers)
		{
			Files.append(string(relayUser.getLast()) + "," +
							string(relayUser.getFirst()) + "," +
							string(relayUser.getUsername()) + "," +
							string(relayUser.getEmployeeId()) + "," +
							string(relayUser.getSsoguid()) + "," +
							string(relayUser.getLastLogonTimestamp()) +
							"\n",
					logFile, Charsets.UTF_8);
		}
	}

	public static void logPSHRStaff(Set<PSHRStaff> pshrStaffList, File logFile) throws IOException
	{
		for (PSHRStaff pshrStaff : pshrStaffList)
		{
			Files.append(string(pshrStaff.getLastName()) + "," +
							string(pshrStaff.getFirstName()) + "," +
							string(pshrStaff.getEmployeeId()) +
							"\n",
					logFile, Charsets.UTF_8);
		}
	}

	private static String string(String string)
	{
		return Strings.isNullOrEmpty(string) ? "" : string;
	}

	private static String string(DateTime dateTime)
	{
		return dateTime != null ? dateTime.toString() : "";
	}
}

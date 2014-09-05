package org.cru.migration.support;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.domain.RelayUser;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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
			Files.append(format(relayUser.getLast()) + "," +
							format(relayUser.getFirst()) + "," +
							format(relayUser.getUsername()) + "," +
							format(relayUser.getEmployeeId()) + "," +
							format(relayUser.getSsoguid()) + "," +
							format(relayUser.getLastLogonTimestamp()) +
							"\n",
					logFile, Charsets.UTF_8);
		}
	}

	public static void logPSHRStaff(Set<PSHRStaff> pshrStaffList, File logFile) throws IOException
	{
		for (PSHRStaff pshrStaff : pshrStaffList)
		{
			Files.append(format(pshrStaff.getLastName()) + "," +
							format(pshrStaff.getFirstName()) + "," +
							format(pshrStaff.getEmployeeId()) +
							"\n",
					logFile, Charsets.UTF_8);
		}
	}

	private static String format(String string)
	{
		return Strings.isNullOrEmpty(string) ? "''" : "'" +
				string.replaceAll("'", "\\\\'").replaceAll(",", "\\\\,") + "'";
	}

	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

	private static final DateTime oldDateTime = new DateTime().minusYears(53);

	public static String format(DateTime dateTime)
	{
		return dateTimeFormatter.print(dateTime != null ? dateTime : oldDateTime);
	}
}

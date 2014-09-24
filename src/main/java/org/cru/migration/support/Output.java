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
import java.util.Map;
import java.util.Set;

public class Output
{
	public static void logRelayUser(Set<RelayUser> relayUsers, File logFile) throws IOException
	{
		logRelayUser(relayUsers, "", logFile);
	}

	public static void logRelayUser(Set<RelayUser> relayUsers, String message, File logFile) throws IOException
	{
		for (RelayUser relayUser : relayUsers)
		{
			logRelayUser(relayUser, message, logFile);
		}
	}

	public static void logRelayUser(Map<RelayUser, Exception> relayUsers, File logFile) throws IOException
	{
		for (Map.Entry<RelayUser, Exception> entry : relayUsers.entrySet())
		{
			RelayUser relayUser = entry.getKey();
			Exception exception = entry.getValue();

			logRelayUser(relayUser, exception.getMessage(), logFile);
		}
	}

	private static void logRelayUser(RelayUser relayUser, String message, File logFile) throws IOException
	{
		Files.append(format(relayUser.getLast()) + "," +
						format(relayUser.getFirst()) + "," +
						format(relayUser.getUsername()) + "," +
						format(relayUser.getEmployeeId()) + "," +
						format(relayUser.getSsoguid()) + "," +
						format(relayUser.getLastLogonTimestamp()) +
						(!Strings.isNullOrEmpty(message) ? "," + message : "") +
						"\n",
				logFile, Charsets.UTF_8);
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

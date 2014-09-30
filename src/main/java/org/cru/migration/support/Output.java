package org.cru.migration.support;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.domain.RelayUserGroups;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class Output
{
	public static void logRelayUser(Set<RelayUser> relayUsers, File logFile)
	{
		logRelayUser(relayUsers, "", logFile);
	}

	private static Logger logger = LoggerFactory.getLogger(Output.class);

	private static MigrationProperties migrationProperties = new MigrationProperties();

	public static void logRelayUser(Set<RelayUser> relayUsers, String message, File logFile)
	{
		for (RelayUser relayUser : relayUsers)
		{
			logRelayUser(relayUser, message, logFile);
		}
	}

	public static void logRelayUser(Map<RelayUser, Exception> relayUsers, File logFile)
	{
		for (Map.Entry<RelayUser, Exception> entry : relayUsers.entrySet())
		{
			RelayUser relayUser = entry.getKey();
			Exception exception = entry.getValue();

			logRelayUser(relayUser, exception.getMessage(), logFile);
		}
	}

	private static void logRelayUser(RelayUser relayUser, String message, File logFile)
	{
		try
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
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void logPSHRStaff(Set<PSHRStaff> pshrStaffList, File logFile)
	{
		for (PSHRStaff pshrStaff : pshrStaffList)
		{
			try
			{
				Files.append(format(pshrStaff.getLastName()) + "," +
								format(pshrStaff.getFirstName()) + "," +
								format(pshrStaff.getEmployeeId()) +
								"\n",
						logFile, Charsets.UTF_8);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static void logDataAnalysis(RelayUserGroups relayUserGroups)
	{
		// relay users without password having logged in since
		Set<RelayUser> relayUsersWithoutPasswordHavingLoggedInSince = Sets.newHashSet();
		relayUsersWithoutPasswordHavingLoggedInSince.addAll(relayUserGroups.getWithoutPassword());
		relayUsersWithoutPasswordHavingLoggedInSince.removeAll(relayUserGroups.getNotLoggedIn());

		logger.debug("U.S. staff and google relay users without password having logged in since " +
				relayUserGroups.getLoggedInSince() +
				" size is " + relayUsersWithoutPasswordHavingLoggedInSince.size());
		Output.logRelayUser(relayUsersWithoutPasswordHavingLoggedInSince,
				FileHelper.getFile(migrationProperties.getNonNullProperty
						("relayUsersWithoutPasswordHavingLoggedInSince")));

		Set<RelayUser> usStaffNotFoundInCasAudit = Sets.newHashSet();
		usStaffNotFoundInCasAudit.addAll(relayUserGroups.getNotFoundInCasAuditLog());
		usStaffNotFoundInCasAudit.removeAll(relayUserGroups.getGoogleUserNotUSStaff());
		Output.logRelayUser(usStaffNotFoundInCasAudit,
				FileHelper.getFile(migrationProperties.getNonNullProperty
						("usStaffNotFoundInCasAudit")));

		Set<RelayUser> nonUSStaffNotFoundInCasAudit = Sets.newHashSet();
		nonUSStaffNotFoundInCasAudit.addAll(relayUserGroups.getNotFoundInCasAuditLog());
		nonUSStaffNotFoundInCasAudit.removeAll(relayUserGroups.getUsStaff());
		Output.logRelayUser(nonUSStaffNotFoundInCasAudit,
				FileHelper.getFile(migrationProperties.getNonNullProperty
						("nonUSStaffNotFoundInCasAudit")));
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

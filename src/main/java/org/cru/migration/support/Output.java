package org.cru.migration.support;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.domain.RelayUserGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class Output
{
    private static Logger logger = LoggerFactory.getLogger(Output.class);

	private static MigrationProperties migrationProperties = new MigrationProperties();

    public static void logRelayUsers(Set<RelayUser> relayUsers, File logFile)
    {
        for (RelayUser relayUser : relayUsers)
        {
            logMessage(relayUser.toCsvFormattedString(), logFile);
        }
    }

    public static void logRelayUsers(Map<RelayUser, Exception> relayUsers, File logFile)
    {
        for (Map.Entry<RelayUser, Exception> entry : relayUsers.entrySet())
        {
            RelayUser relayUser = entry.getKey();
            Exception exception = entry.getValue();

            logMessage(relayUser.toCsvFormattedString() + "," + exception.getMessage(), logFile);
        }
    }

    public static void logPSHRStaff(Set<PSHRStaff> pshrStaffList, File logFile)
	{
		for (PSHRStaff pshrStaff : pshrStaffList)
		{
            logMessage(pshrStaff.toCvsFormattedString(), logFile);
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
		Output.logRelayUsers(relayUsersWithoutPasswordHavingLoggedInSince,
                FileHelper.getFile(migrationProperties.getNonNullProperty
                        ("relayUsersWithoutPasswordHavingLoggedInSince")));

		Set<RelayUser> usStaffNotFoundInCasAudit = Sets.newHashSet();
		usStaffNotFoundInCasAudit.addAll(relayUserGroups.getNotFoundInCasAuditLog());
		usStaffNotFoundInCasAudit.removeAll(relayUserGroups.getGoogleUserNotUSStaff());
		Output.logRelayUsers(usStaffNotFoundInCasAudit,
                FileHelper.getFile(migrationProperties.getNonNullProperty
                        ("usStaffNotFoundInCasAudit")));

		Set<RelayUser> nonUSStaffNotFoundInCasAudit = Sets.newHashSet();
		nonUSStaffNotFoundInCasAudit.addAll(relayUserGroups.getNotFoundInCasAuditLog());
		nonUSStaffNotFoundInCasAudit.removeAll(relayUserGroups.getUsStaff());
		Output.logRelayUsers(nonUSStaffNotFoundInCasAudit,
                FileHelper.getFile(migrationProperties.getNonNullProperty
                        ("nonUSStaffNotFoundInCasAudit")));
	}

    private static void logMessage(String message, File logFile)
    {
        try
        {
            Files.append(message + "\n", logFile, Charsets.UTF_8);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}

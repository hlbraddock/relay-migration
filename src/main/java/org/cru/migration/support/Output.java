package org.cru.migration.support;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.ccci.gcx.idm.core.model.impl.GcxUser;
import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.domain.RelayGcxUsers;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.domain.RelayUserGroups;
import org.cru.migration.thekey.GcxUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Output
{
    private static Logger logger = LoggerFactory.getLogger(Output.class);

	private static MigrationProperties migrationProperties = new MigrationProperties();

	public static void serializeRelayUsers(Set<RelayUser> relayUsers, String filename)
	{
		serializeRelayUsers(relayUsers, filename, false);
	}

	public static void serializeRelayUsers(Set<RelayUser> relayUsers, String filename, Boolean withPassword)
	{
		try
		{
			CSVWriter csvWriter = new CSVWriter(new FileWriter(filename));

			for (RelayUser relayUser : relayUsers)
			{
				serialize(csvWriter, relayUser.toList(withPassword).toArray(new String[0]));
			}

			csvWriter.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static void serialize(CSVWriter csvWriter, String[] array)
	{
		csvWriter.writeNext(array);
	}

	public static Set<RelayUser> deserializeRelayUsers(String filename) throws
			IOException
	{
		Set<RelayUser> relayUsers = Sets.newHashSet();

		CSVReader csvReader = new CSVReader(new FileReader(filename));
		String [] nextLine;
		while ((nextLine = csvReader.readNext()) != null)
		{
			relayUsers.add(RelayUser.fromList(Arrays.asList(nextLine)));
		}

		return relayUsers;
	}

	public static void logGcxUsers(Set<GcxUser> gcxUsers, File file)
	{
		for (GcxUser gcxUser : gcxUsers)
		{
			logMessage(gcxUser.toString(), file);
		}
	}

	public static void serializeRelayUsers(Map<RelayUser, Exception> relayUsers, String filename)
	{
		serializeRelayUsers(relayUsers, filename, false);
	}

	public static void serializeRelayUsers(Map<RelayUser, Exception> relayUsers, String filename, Boolean withPassword)
	{
		try
		{
			CSVWriter csvWriter = new CSVWriter(new FileWriter(filename));

			for (Map.Entry<RelayUser, Exception> entry : relayUsers.entrySet())
			{
				RelayUser relayUser = entry.getKey();
				Exception exception = entry.getValue();

				List<String> relayUserList = relayUser.toList(withPassword);
				relayUserList.add(exception.getMessage());
				serialize(csvWriter, relayUserList.toArray(new String[0]));
			}

			csvWriter.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static void logRelayGcxUsers(Set<RelayGcxUsers> relayGcxUsersSet, File file)
	{
		String message = "";

		for(RelayGcxUsers relayGcxUsers : relayGcxUsersSet)
		{
			RelayUser relayUser = relayGcxUsers.getRelayUser();

			message += "Relay user: ";

			if (relayUser != null)
			{
				message += relayUser.getUsername() + "," + relayUser.getSsoguid();
			}

			message += ": ";

			GcxUser gcxUser = relayGcxUsers.getGcxUser();
			message += "Gcx user: ";

			if(gcxUser != null)
			{
				message += gcxUser.getEmail() + "," + gcxUser.getGUID();
			}

			message += ": ";

			GcxUserService.MatchType matchType = relayGcxUsers.getMatchResult().matchType;
			message += "Match Type: " + matchType.toString() + ": ";

			message += "Gcx users: ";

			Set<GcxUser> gcxUsers = relayGcxUsers.getGcxUsers();
			for (GcxUser gcxUser1 : gcxUsers)
			{
				if(gcxUser != null)
				{
					message += gcxUser1.getEmail() + "," + gcxUser1.getGUID();
				}

				message += ", ";
			}

			message += ": ";
		}

		if(!Strings.isNullOrEmpty(message))
		{
			logMessage(message, file);
		}
	}

	public static void serializeRelayGcxUsers(Set<RelayGcxUsers> relayGcxUsersSet, String filename)
	{
		serializeRelayGcxUsers(relayGcxUsersSet, filename, false);
	}

	public static void serializeRelayGcxUsers(Set<RelayGcxUsers> relayGcxUsersSet, String filename,
											  Boolean withPassword)
	{
		try
		{
			CSVWriter csvWriter = new CSVWriter(new FileWriter(filename));

			for(RelayGcxUsers relayGcxUsers : relayGcxUsersSet)
			{
				if(relayGcxUsers.getRelayUser() != null)
				{
					List<String> relayUserList = relayGcxUsers.getRelayUser().toList(withPassword);

					if(relayGcxUsers.getException() != null)
					{
						relayUserList.add(relayGcxUsers.getException().getMessage());
					}

					serialize(csvWriter, relayUserList.toArray(new String[0]));
				}

				for(GcxUser gcxUser : relayGcxUsers.getGcxUsers())
				{
					serialize(csvWriter, new String[] {gcxUser.toString()});
				}

				serialize(csvWriter, new String[0]);
			}

			csvWriter.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static void logGcxUsers(Map<GcxUser, Exception> relayUsers, File file)
	{
		for (Map.Entry<GcxUser, Exception> entry : relayUsers.entrySet())
		{
			GcxUser gcxUser = entry.getKey();
			Exception exception = entry.getValue();

			logMessage(gcxUser == null ? "null gcx user " : gcxUser.toString() + "," + exception.getMessage(), file);
		}
	}

	public static void logPSHRStaff(Set<PSHRStaff> pshrStaffList, File file)
	{
		for (PSHRStaff pshrStaff : pshrStaffList)
		{
            logMessage(pshrStaff.toCvsFormattedString(), file);
		}
	}

    public static void logRelayGcxUsersMap(Map<RelayUser, GcxUser> matchingRelayGcxUsers, String filename)
    {
		Set<RelayUser> relayUsers = Sets.newHashSet();
        for (Map.Entry<RelayUser, GcxUser> entry : matchingRelayGcxUsers.entrySet())
        {
            RelayUser relayUser = entry.getKey();
			relayUsers.add(relayUser);
        }

		serializeRelayUsers(relayUsers, filename, false);
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
		Output.serializeRelayUsers(relayUsersWithoutPasswordHavingLoggedInSince,
				migrationProperties.getNonNullProperty
						("relayUsersWithoutPasswordHavingLoggedInSince"), false);

		Set<RelayUser> usStaffNotFoundInCasAudit = Sets.newHashSet();
		usStaffNotFoundInCasAudit.addAll(relayUserGroups.getNotFoundInCasAuditLog());
		usStaffNotFoundInCasAudit.removeAll(relayUserGroups.getGoogleUserNotUSStaff());
		Output.serializeRelayUsers(usStaffNotFoundInCasAudit,
				migrationProperties.getNonNullProperty
						("usStaffNotFoundInCasAudit"), false);

		Set<RelayUser> nonUSStaffNotFoundInCasAudit = Sets.newHashSet();
		nonUSStaffNotFoundInCasAudit.addAll(relayUserGroups.getNotFoundInCasAuditLog());
		nonUSStaffNotFoundInCasAudit.removeAll(relayUserGroups.getUsStaff());
		Output.serializeRelayUsers(nonUSStaffNotFoundInCasAudit,
				migrationProperties.getNonNullProperty
						("nonUSStaffNotFoundInCasAudit"), false);
	}

	public static void logMessage(String message, File file)
    {
        try
        {
            Files.append(message + "\n", file, Charsets.UTF_8);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}

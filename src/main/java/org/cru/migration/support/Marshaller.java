package org.cru.migration.support;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.cru.migration.domain.RelayUser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class Marshaller
{
	public static Set<RelayUser> unmarshallRelayUsers(File file) throws IOException
	{
		Set<RelayUser> relayUsers = Sets.newHashSet();

		List<String> lines = Files.readLines(file, Charsets.UTF_8);

		for(String line : lines)
		{
			String[] split = line.split(",");
			try
			{
				RelayUser relayUser = new RelayUser();
				relayUser.setUsername(split[0]);
				relayUser.setFirst(split[1]);
				relayUser.setLast(split[2]);
				relayUser.setEmployeeId(split[3]);
				relayUser.setSsoguid(split[4]);

				relayUsers.add(relayUser);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				Output.println("exception in unmarshall" + e.getMessage());
			}
		}

		return relayUsers;

	}

}

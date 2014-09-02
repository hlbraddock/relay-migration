package org.cru.migration.support;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.cru.migration.domain.RelayUser;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Marshaller
{
	public static List<RelayUser> unmarshallRelayUsers(File file) throws IOException
	{
		List<RelayUser> relayUsers = Lists.newArrayList();

		List<String> lines = Files.readLines(file, Charsets.UTF_8);


		for(String line : lines)
		{
			String[] split = line.split(",");
			try
			{
				RelayUser relayUser = new RelayUser();
				relayUser.setUsername(split[0]);
				relayUser.setEmployeeId(split[2]);
				relayUser.setSsoguid(split[3]);

				relayUsers.add(relayUser);
			}
			catch(Exception e)
			{

			}
		}

		return relayUsers;

	}

}

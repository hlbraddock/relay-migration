package org.cru.migration.support;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

public class FileHelper
{
	public static File getFile(String filename) throws IOException
	{
		File staffRelayUsersLogFile = new File(filename);

		Files.touch(staffRelayUsersLogFile);

		Files.write("".getBytes(), staffRelayUsersLogFile);

		return staffRelayUsersLogFile;
	}
}

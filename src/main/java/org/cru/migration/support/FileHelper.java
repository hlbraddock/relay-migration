package org.cru.migration.support;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

public class FileHelper
{
	public static File getFileToRead(String filename)
	{
		return getFile(filename, false);
	}

	public static File getFileToWrite(String filename)
	{
		return getFile(filename, true);
	}

	private static File getFile(String filename, Boolean truncate)
	{
		try
		{
			File file = new File(filename);

			if(truncate)
			{
				Files.touch(file);
				Files.write("".getBytes(), file);
			}

			return file;
		}
		catch (IOException e)
		{
			e.printStackTrace();

			return new File(filename);
		}
	}
}

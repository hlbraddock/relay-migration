package org.cru.migration.support;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

public class FileHelper
{
	public static File getFile(String filename)
	{
		try
		{
			File file = new File(filename);

			Files.touch(file);

			Files.write("".getBytes(), file);

			return file;
		}
		catch (IOException e)
		{
			e.printStackTrace();

			return new File(filename);
		}
	}
}

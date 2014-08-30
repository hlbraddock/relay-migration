package org.cru.migration.support;

import java.util.List;

public class StringUtilities
{
	public static String delimitAndSurround(List<String> strings, char delimiter, char surroundedBy)
	{
		String delimited = "";

		for(String string : strings)
		{
			delimited += (delimited.equals("") ? "" : delimiter);
			delimited += surroundedBy + string + surroundedBy;
		}

		return delimited;
	}
}

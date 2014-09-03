package org.cru.migration.support;

import java.util.Set;

public class StringUtilities
{
	public static String delimitAndSurround(Set<String> strings, char delimiter, char surroundedBy)
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

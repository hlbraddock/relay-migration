package org.cru.migration.support;

import org.ccci.util.strings.Strings;

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

	public static String capitalize(String line)
	{
		return Character.toUpperCase(line.charAt(0)) + line.substring(1);
	}

	public static String capitalize(String string, String delimiter, String regex)
	{
		String[] split = string.split(regex);

		String result = "";

		for(String value : split)
		{
			String capitalized = StringUtilities.capitalize(value);

			result += Strings.isEmpty(result) ? capitalized : delimiter + capitalized;
		}

		return result;
	}

	public static boolean isEmail(String email)
	{
		return email.contains("@");
	}

	public static String capitalizeEmail(String email)
	{
		String[] result = email.split("@");

		return capitalize(result[0], ".", "\\.") + '@' + result[1];
	}
}

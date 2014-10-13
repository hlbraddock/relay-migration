package org.cru.migration.support;

import org.ccci.util.strings.Strings;
import org.joda.time.Duration;

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

	public static String[] commaDelimitedListToStringArray(String string, String escapeChar)
	{
		// these characters need to be escaped in a regular expression
		String regularExpressionSpecialChars = "/.*+?|()[]{}\\";

		String escapedEscapeChar = escapeChar;

		// if the escape char for our comma separated list needs to be escaped
		// for the regular expression, escape it using the \ char
		if (regularExpressionSpecialChars.contains(escapeChar))
		{
			escapedEscapeChar = "\\" + escapeChar;
		}

		// see http://stackoverflow.com/questions/820172/how-to-split-a-comma-separated-string-while-ignoring-escaped
		// -commas
		String[] temp = string.split("(?<!" + escapedEscapeChar + "),", -1);

		// remove the escapeChar for the end result
		String[] result = new String[temp.length];
		for (int i = 0; i < temp.length; i++)
		{
			result[i] = temp[i].replaceAll(escapedEscapeChar + ",", ",");
		}

		return result;
	}

	public static String toString(Duration duration)
	{
		return toString(duration, true);
	}

	public static String toString(Duration duration, Boolean withMillis)
	{
		return duration.getStandardDays() + ":" + duration.getStandardHours() +
				":" + duration.getStandardMinutes() + ":" + duration.getStandardSeconds() +
				(withMillis ? " (milliseconds:" + duration.getMillis() + ")" : "");
	}
}

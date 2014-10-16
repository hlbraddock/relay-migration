package org.cru.migration.support;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

public class Misc
{
	public static Integer nonNullCount(Object... objects)
	{
		Integer count = 0;

		for(Object object : objects)
		{
			if(object != null)
			{
				count ++;
			}
		}

		return count;
	}

	public static Object firstNonNull(Object... objects)
	{
		for(Object object : objects)
		{
			if(object != null)
			{
				return object;
			}
		}

		return null;
	}

	private static final char DEFAULT_MULTI_VALUE_DELIMETER = '(';

	public static String format(String string)
	{
		return Strings.isNullOrEmpty(string) ? "" :
				string.replaceAll("'", "\\\\'").replaceAll(",", "\\\\,").replaceAll("\\\\", "\\\\\\\\");
	}

	public static String escape(String string)
	{
		return Strings.isNullOrEmpty(string) ? "" :
				string.replaceAll("\\\\", "\\\\\\\\");
	}

	public static String format(List<String> strings)
	{
		return format(strings, DEFAULT_MULTI_VALUE_DELIMETER);
	}

	public static String format(List<String> strings, char delimiter)
	{
		if(strings == null || strings.isEmpty())
		{
			return format("");
		}

		String formatted = "";

		Integer counter = 0;

		for(String string : strings)
		{
			formatted += format(string);

			if(++counter < strings.size())
			{
				formatted += delimiter;
			}
		}

		return formatted;
	}

	public static List<String> unformatMultiValue(String string)
	{
		return unformatMultiValue(string, DEFAULT_MULTI_VALUE_DELIMETER);
	}

	public static List<String> unformatMultiValue(String multiValueString, char delimiter)
	{
		List<String> strings = Lists.newArrayList();

		String split[] = multiValueString.split("\\" + delimiter);

		for(String string : split)
		{
			strings.add(string);
		}

		return strings;
	}

	public static String unformat(String string)
	{
		if(Strings.isNullOrEmpty(string))
		{
			return "";
		}

		return string.replaceAll("\\\\'", "'").replaceAll("\\\\,", ",");
	}

	public static String format(DateTime dateTime)
	{
		return dateTimeFormatter.print(dateTime != null ? dateTime : oldDateTime);
	}

	public static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");

	public static final DateTime oldDateTime = new DateTime().minusYears(53);

	public static String getNonNullField(Integer indices, String[] fields)
	{
		if(fields.length > indices)
		{
			if (!Strings.isNullOrEmpty(fields[indices]))
			{
				return fields[indices];
			}
		}

		return "";
	}

	public static Boolean equals(String string, String string2)
	{
		return (Strings.isNullOrEmpty(string) && Strings.isNullOrEmpty(string2)) ||
				(string != null && string.equals(string2));
	}

	public static Boolean equals(Object object, Object object2)
	{
		return (object == null && object2 == null) || (object != null && object.equals(object2));
	}
}

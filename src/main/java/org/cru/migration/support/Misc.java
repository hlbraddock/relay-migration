package org.cru.migration.support;

import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Misc
{
	public static <T> Integer nonNullCount(T... objects)
	{
		Integer count = 0;

		for(T t : objects)
		{
			if(t != null)
			{
				count ++;
			}
		}

		return count;
	}

	public static <T> Boolean areNonNull(T... objects)
	{
		for(T t : objects)
		{
			if(t == null)
			{
				return false;
			}
		}

		return true;
	}

	public static String escape(String string)
	{
		return Strings.isNullOrEmpty(string) ? "" :
				string.replaceAll("\\\\", "\\\\\\\\");
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

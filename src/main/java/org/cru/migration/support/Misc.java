package org.cru.migration.support;

import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public static String format(String string)
	{
		return Strings.isNullOrEmpty(string) ? "''" : "'" +
				string.replaceAll("'", "\\\\'").replaceAll(",", "\\\\,") + "'";
	}

	private static Logger logger = LoggerFactory.getLogger(Misc.class);

	public static String unformat(String string)
	{
		if(Strings.isNullOrEmpty(string))
		{
			return "";
		}

		if(string.length() > 0)
		{
			if(string.charAt(0) == '\'')
			{
				string = string.substring(1);
			}

			if(string.charAt(string.length()-1) == '\'')
			{
				string = string.substring(0, string.length()-1);
			}
		}

		string = string.replaceAll("\\\\'", "'").replaceAll("\\\\,", ",");

		return string;
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

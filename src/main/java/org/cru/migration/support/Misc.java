package org.cru.migration.support;

import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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

	public static String unformat(String string)
	{
		if(Strings.isNullOrEmpty(string))
		{
			return "";
		}

		String unformatted = string.replaceAll("\\\\'", "'").replaceAll("\\\\,", ",");

		if(unformatted.length() > 0)
		{
			if(unformatted.charAt(0) == '\'')
			{
				unformatted = unformatted.substring(1);
			}

			if(unformatted.charAt(unformatted.length()-1) == '\'')
			{
				unformatted = unformatted.substring(0, unformatted.length()-1);
			}
		}

		return unformatted;
	}

	public static String format(DateTime dateTime)
    {
        return dateTimeFormatter.print(dateTime != null ? dateTime : oldDateTime);
    }

    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final DateTime oldDateTime = new DateTime().minusYears(53);

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

	public static Boolean equals(Object object, Object object2)
	{
		return (object == null && object2 == null) || (object != null && object.equals(object2));
	}
}

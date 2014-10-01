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

    public static String format(DateTime dateTime)
    {
        return dateTimeFormatter.print(dateTime != null ? dateTime : oldDateTime);
    }

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTime oldDateTime = new DateTime().minusYears(53);
}

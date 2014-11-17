package org.cru.migration.support;

import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
       return equals(string, string2, false);
    }

    Pattern p = Pattern.compile("[\\p{Alpha}]*[\\p{Punct}][\\p{Alpha}]*");

    public static Boolean equals(String string, String string2, Boolean allowSpecialCharacterDifference)
	{
        if(allowSpecialCharacterDifference)
        {
            string = string.replaceAll("\\s","");
            string2 = string2.replaceAll("\\s","");
        }

        Boolean result = (Strings.isNullOrEmpty(string) && Strings.isNullOrEmpty(string2)) ||
				(string != null && string.equals(string2));

        if(!result && allowSpecialCharacterDifference)
        {
            Pattern p = Pattern.compile("[\\p{Alpha}]*[\\p{Punct}][\\p{Alpha}]*");
            Matcher m = p.matcher(string);
            Matcher m2 = p.matcher(string);
            System.out.println("matches " + m.matches() + "," + m2.matches());
            return m.matches() || m2.matches();
        }

        return result;
	}

    public static Boolean equals(DateTime dateTime, DateTime dateTime2, Boolean allowHourDifferential)
    {
        if(dateTime == null && dateTime2 == null)
        {
            return true;
        }

        if(dateTime == null)
        {
            return false;
        }

        if(dateTime2 == null)
        {
            return false;
        }

        // within an hour
        return dateTime.equals(dateTime2) ||
                (allowHourDifferential &&
                        (dateTime.minusHours(1).equals(dateTime2) || dateTime.equals(dateTime2.minusHours(1))));
    }

    public static Boolean equals(Object object, Object object2)
	{
		return (object == null && object2 == null) || (object != null && object.equals(object2));
	}
}

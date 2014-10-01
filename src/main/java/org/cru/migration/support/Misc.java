package org.cru.migration.support;

public class Misc
{
	public static Integer nullCount(Object ...objects)
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

		return nullCount();
	}

}

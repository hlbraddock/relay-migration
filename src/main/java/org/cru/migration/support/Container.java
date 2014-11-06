package org.cru.migration.support;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

public class Container
{
	public static <T> Set<T> toSet(List<T> list)
	{
		Set<T> set = Sets.newHashSet();

		set.addAll(list);

		return set;
	}

	public static <T> List<T> toList(Set<T> set)
	{
		List<T> list = Lists.newArrayList();

		list.addAll(set);

		return list;
	}

	public static <T> Boolean equals(List<T> first, List<T> second)
	{
		if(first == null && second == null)
		{
			return true;
		}

		if(first == null)
		{
			return second.size() == 0;
		}

		if(second == null)
		{
			return first.size() == 0;
		}

		return first.containsAll(second) && second.containsAll(first);
	}

	public static Set<String> uppercase(Set<String> set)
	{
		Set<String> uppercase = Sets.newHashSet();

		for(String string : set)
		{
			uppercase.add(string.toUpperCase());
		}

		return uppercase;
	}

	public static Set<String> getListByRange(List<String> string, int begin, int end)
	{
		Set<String> list = Sets.newHashSet();

		for(int iterator = begin; iterator <= end; iterator++)
		{
			if(string.size() < iterator)
			{
				break;
			}

			list.add(string.get(iterator));
		}

		return list;
	}
}

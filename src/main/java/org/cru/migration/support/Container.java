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

	public static Set<String> andUpperAndLower(Set<String> set)
	{
		Set<String> andUpperAndLower = Sets.newHashSet();

		for(String string : set)
		{
			andUpperAndLower.add(string);
			andUpperAndLower.add(string.toLowerCase());
			andUpperAndLower.add(string.toUpperCase());
		}

		return andUpperAndLower;
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

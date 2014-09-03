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
}

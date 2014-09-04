package org.cru.migration.support;

import java.util.Map;

/**
 * @author Lee Braddock
 */
public class Evaluation
{
	private static final String default_prefix = "\\$\\{";
	private static final String default_suffix = "\\}";

	public static String evaluate(String expression, Map<String, String> namedValues)
	{
		return evaluate(expression, namedValues, default_prefix, default_suffix);
	}

	private static String evaluate(String expression, Map<String, String> namedValues, String prefix,
										String suffix)
	{
		String evaluated = expression;
		for (Map.Entry<String, String> entry : namedValues.entrySet())
			evaluated = evaluated.replaceAll(prefix + entry.getKey() + suffix, entry.getValue());

		return evaluated;
	}
}

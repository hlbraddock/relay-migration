package org.cru.migration.support;

import org.ccci.util.properties.PropertiesWithFallback;

public class MigrationProperties extends PropertiesWithFallback
{
	public MigrationProperties()
	{
		super(null, true, propertiesFile);
	}

	public String getNonNullProperty(String property)
	{
		return getProperty(property) == null ? "" : getProperty(property);
	}

	private static String propertiesFile = "/default-properties.xml";
}

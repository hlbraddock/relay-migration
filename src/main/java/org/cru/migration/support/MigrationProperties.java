package org.cru.migration.support;

import org.ccci.util.properties.PropertiesWithFallback;

public class MigrationProperties extends PropertiesWithFallback
{
	public MigrationProperties()
	{
		super(null, true, propertiesFile, propertiesFileDefault);
	}

	public String getNonNullProperty(String property)
	{
		return getProperty(property) == null ? "" : getProperty(property);
	}

	private static String propertiesFile = "/apps/apps-config/migration-properties.xml";
	private static String propertiesFileDefault = "/default-properties.xml";
}

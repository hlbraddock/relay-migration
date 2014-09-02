package org.cru.migration.support;

import org.ccci.util.properties.CcciProperties.PropertyEncryptionSetup;
import org.ccci.util.properties.PropertiesWithFallback;

public class MigrationProperties extends PropertiesWithFallback
{
	public MigrationProperties()
	{
		super(new PropertyEncryptionSetup("adwb3C%*&c87raJ#$Yw8)Lp8w9xap23s"), true, propertiesFile,
				propertiesFileDefault);
	}

	public String getNonNullProperty(String property)
	{
		return getProperty(property) == null ? "" : getProperty(property);
	}

	private static String propertiesFile = "/apps/apps-config/migration-properties.xml";
	private static String propertiesFileDefault = "/default-properties.xml";
}

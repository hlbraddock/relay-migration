package org.cru.migration.dao;

import org.apache.commons.dbcp.BasicDataSource;
import org.ccci.idm.dao.datasource.BasicDataSourceFactory;
import org.cru.migration.support.MigrationProperties;

import java.beans.PropertyVetoException;
import java.io.IOException;

public class DaoFactory
{
	public static PSHRDao getPshrDao(MigrationProperties properties) throws IOException, PropertyVetoException
	{
		BasicDataSource basicDataSource = getBasicDataSource(
				properties.getNonNullProperty("pshrDriverClass"),
				properties.getNonNullProperty("pshrJdbcUrl"),
				properties.getNonNullProperty("pshrUser"),
				properties.getNonNullProperty("pshrPassword"));

		PSHRDao pshrDao = new PSHRDao();
		pshrDao.setDataSource(basicDataSource);

		return pshrDao;
	}

	private static BasicDataSource getBasicDataSource
			(String driverClass, String jdbcUrl, String username, String password)
			throws IOException,
			PropertyVetoException
	{
		BasicDataSource basicDataSource = (new BasicDataSourceFactory()).createInstance();

		basicDataSource.setDriverClassName(driverClass);
		basicDataSource.setUrl(jdbcUrl);
		basicDataSource.setUsername(username);
		basicDataSource.setPassword(password);

		return basicDataSource;
	}
}

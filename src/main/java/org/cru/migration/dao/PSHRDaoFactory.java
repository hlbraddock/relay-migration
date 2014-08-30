package org.cru.migration.dao;

import org.apache.commons.dbcp.BasicDataSource;
import org.ccci.idm.dao.datasource.BasicDataSourceFactory;
import org.cru.migration.support.MigrationProperties;

import java.beans.PropertyVetoException;
import java.io.IOException;

public class PSHRDaoFactory
{
	public static PSHRDao getInstance(MigrationProperties properties) throws IOException, PropertyVetoException
	{
		BasicDataSource basicDataSource = (new BasicDataSourceFactory()).createInstance();

		basicDataSource.setDriverClassName(properties.getNonNullProperty("pshrDriverClass"));
		basicDataSource.setUrl(properties.getNonNullProperty("pshrJdbcUrl"));
		basicDataSource.setUsername(properties.getNonNullProperty("pshrUser"));
		basicDataSource.setPassword(properties.getNonNullProperty("pshrPassword"));

		PSHRDao pshrDao = new PSHRDao();
		pshrDao.setDataSource(basicDataSource);

		return pshrDao;
	}
}

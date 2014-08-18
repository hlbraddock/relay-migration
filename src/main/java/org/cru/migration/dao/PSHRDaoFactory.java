package org.cru.migration.dao;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.dbcp.BasicDataSource;
import org.ccci.idm.dao.datasource.BasicDataSourceFactory;
import org.cru.migration.support.MigrationProperties;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;

public class PSHRDaoFactory
{
	public static PSHRDao getInstance(MigrationProperties properties) throws IOException, PropertyVetoException
	{
		String password = Files.readLines(new File(properties.getNonNullProperty("pshrPassword")), Charsets.UTF_8).get(0);

		BasicDataSource basicDataSource = (new BasicDataSourceFactory()).createInstance();

		basicDataSource.setDriverClassName(properties.getNonNullProperty("pshrDriverClass"));
		basicDataSource.setUrl(properties.getNonNullProperty("pshrJdbcUrl"));
		basicDataSource.setUsername(properties.getNonNullProperty("pshrUser"));
		basicDataSource.setPassword(password);

		PSHRDao pshrDao = new PSHRDao();
		pshrDao.setDataSource(basicDataSource);

		return pshrDao;
	}
}

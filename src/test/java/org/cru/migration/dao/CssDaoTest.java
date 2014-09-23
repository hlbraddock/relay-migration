package org.cru.migration.dao;

import org.cru.migration.ManualTest;
import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.support.Marshaller;
import org.cru.migration.support.MigrationProperties;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;

@Category(ManualTest.class)
public class CssDaoTest
{
	CssDao cssDao;

	Logger logger = LoggerFactory.getLogger(getClass());

	@Before
	public void before() throws Exception
	{
		cssDao = DaoFactory.getCssDao(new MigrationProperties());
	}

	@After
	public void after() throws Exception
	{
	}

	@Test
	public void hasLotsOfCssData() throws Exception
	{
		MigrationProperties migrationProperties = new MigrationProperties();

		Set<RelayUser> relayUsers = Marshaller.unmarshallRelayUsers(new File(migrationProperties.getNonNullProperty
				("usStaffRelayUsersLogFile")));

		logger.debug("US staff relay users size " + relayUsers.size());

		Set<CssRelayUser> cssRelayUsers = cssDao.getCssRelayUsers(RelayUser.getSsoguids(relayUsers));

		logger.debug("CSS Relay User staff users size " + cssRelayUsers.size());

		Assert.assertTrue(cssRelayUsers.size() > 10000);
	}
}

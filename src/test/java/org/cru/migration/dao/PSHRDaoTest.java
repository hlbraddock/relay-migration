package org.cru.migration.dao;

import org.cru.migration.ManualTest;
import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.support.MigrationProperties;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

@Category(ManualTest.class)
public class PSHRDaoTest
{
	PSHRDao pshrDao;

	@Before
	public void before() throws Exception
	{
		pshrDao = DaoFactory.getPshrDao(new MigrationProperties());
	}

	@After
	public void after() throws Exception
	{
	}

	@Test
	public void hasLotsOfStaff() throws Exception
	{
		List<PSHRStaff> usStaff = pshrDao.getAllUSStaff();

		Assert.assertTrue(usStaff.size() > 20000);
	}
}

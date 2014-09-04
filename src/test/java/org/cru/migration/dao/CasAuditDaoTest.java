package org.cru.migration.dao;

import junit.framework.Assert;
import org.cru.migration.ManualTest;
import org.cru.migration.domain.CasAuditUser;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;


@Category(ManualTest.class)
public class CasAuditDaoTest
{
	CasAuditDao casAuditDao;

	@Before
	public void before() throws Exception
	{
		casAuditDao = DaoFactory.getCasAuditDao(new MigrationProperties());
	}

	@After
	public void after() throws Exception
	{
	}

	@Test
	public void getCasAuditUser() throws Exception
	{
		String username = "lee.braddock@cru.org";

		CasAuditUser casAuditUser = casAuditDao.getCasAuditUser(username);

		Assert.assertEquals(casAuditUser.getUsername(), username);

		Output.println("Cas audit user " + casAuditUser.toString());
	}
}

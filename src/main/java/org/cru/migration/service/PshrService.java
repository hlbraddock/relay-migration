package org.cru.migration.service;

import org.cru.migration.dao.DaoFactory;
import org.cru.migration.dao.PSHRDao;
import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.support.MigrationProperties;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.Set;

public class PSHRService
{
	private MigrationProperties migrationProperties = new MigrationProperties();

	public Set<PSHRStaff> getPshrUSStaff() throws IOException, PropertyVetoException
	{
		PSHRDao pshrDao = DaoFactory.getPshrDao(migrationProperties);

		return pshrDao.getAllUSStaff();
	}
}
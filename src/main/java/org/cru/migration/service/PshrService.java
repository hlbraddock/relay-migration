package org.cru.migration.service;

import com.google.common.collect.Sets;
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

	public Set<PSHRStaff> getSomePshrUSStaff() throws IOException, PropertyVetoException
	{
		Set<PSHRStaff> pshrStaffSet = Sets.newHashSet();

		pshrStaffSet.add(new PSHRStaff("000593885", "unknown"));
		pshrStaffSet.add(new PSHRStaff("000498469", "unknown"));
		pshrStaffSet.add(new PSHRStaff("000467139", "unknown"));

		return pshrStaffSet;
	}

}

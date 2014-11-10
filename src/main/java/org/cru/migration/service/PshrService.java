package org.cru.migration.service;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.cru.migration.dao.DaoFactory;
import org.cru.migration.dao.PSHRDao;
import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.support.FileHelper;
import org.cru.migration.support.MigrationProperties;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class PshrService
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

		File file = FileHelper.getFileToRead(migrationProperties.getNonNullProperty("employeeids"));

		List<String> lines = Files.readLines(file, Charsets.UTF_8);

		for(String line : lines)
		{
			pshrStaffSet.add(new PSHRStaff(line, "None"));
		}

		return pshrStaffSet;
	}

}

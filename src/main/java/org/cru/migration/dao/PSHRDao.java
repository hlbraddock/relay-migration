package org.cru.migration.dao;

import org.ccci.idm.dao.entity.PSHRStaffRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PSHRDao
{
	private JdbcTemplate jdbcTemplate;

	final String allUSStaffQuery =
			"SELECT A.EMPLID FROM (SYSADM.PS_EMPLOYEES2 A LEFT " +
					"OUTER " +
					"JOIN  SYSADM.PS_DESIG_POINTERS B ON  A.EMPLID = B.EMPLID )" +
					"  WHERE ( ( B.EFFDT = " +
					"        (SELECT MAX(B_ED.EFFDT) FROM SYSADM.PS_DESIG_POINTERS B_ED " +
					"        WHERE B.EMPLID = B_ED.EMPLID) " +
					"     AND A.STATUS_CODE not in ('NC', 'NA', 'NF') " +
					"     AND A.EMPL_RCD = 0 ) )";

	public List<PSHRStaffRole> getAllUSStaff()
	{
		return getAllUSStaff(allUSStaffQuery);
	}

	private List<PSHRStaffRole> getAllUSStaff(String query)
	{
		RowMapper<PSHRStaffRole> rowMapper =
				new RowMapper<PSHRStaffRole>()
				{
					public PSHRStaffRole mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						PSHRStaffRole pshrStaffRole = new PSHRStaffRole();

						pshrStaffRole.setEmployeeId(rs.getString("emplid"));

						return pshrStaffRole;
					}
				};

		return jdbcTemplate.query(query, rowMapper);
	}


	public void setDataSource(DataSource dataSource)
	{
		jdbcTemplate = new JdbcTemplate(dataSource);
	}
}

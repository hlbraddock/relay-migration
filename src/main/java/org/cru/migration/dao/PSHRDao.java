package org.cru.migration.dao;

import org.cru.migration.domain.PSHRStaff;
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
			"SELECT A.FIRST_NAME, A.LAST_NAME, A.EMPLID FROM (SYSADM.PS_EMPLOYEES2 A LEFT " +
					"OUTER " +
					"JOIN  SYSADM.PS_DESIG_POINTERS B ON  A.EMPLID = B.EMPLID )" +
					"  WHERE ( ( B.EFFDT = " +
					"        (SELECT MAX(B_ED.EFFDT) FROM SYSADM.PS_DESIG_POINTERS B_ED " +
					"        WHERE B.EMPLID = B_ED.EMPLID) " +
					"     AND A.STATUS_CODE not in ('NC', 'NA', 'NF') " +
					"     AND A.EMPL_RCD = 0 ) )";

	public List<PSHRStaff> getAllUSStaff()
	{
		return getAllUSStaff(allUSStaffQuery);
	}

	private List<PSHRStaff> getAllUSStaff(String query)
	{
		RowMapper<PSHRStaff> rowMapper =
				new RowMapper<PSHRStaff>()
				{
					public PSHRStaff mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						PSHRStaff pshrStaffRole = new PSHRStaff();

						pshrStaffRole.setEmployeeId(rs.getString("emplid"));
						pshrStaffRole.setFirstName(rs.getString("first_name"));
						pshrStaffRole.setLastName(rs.getString("last_name"));

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

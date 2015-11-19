package org.cru.migration.dao;

import org.cru.migration.domain.PSHRStaff;
import org.cru.migration.support.Container;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class PSHRDao
{
	private JdbcTemplate jdbcTemplate;

	final String allUSStaffQuery =
			"SELECT A.FIRST_NAME, A.LAST_NAME, A.EMPLID FROM SYSADM.PS_EMPLOYEES2 A " +
			"WHERE ((A.EMPL_RCD = 0) AND A.STATUS_CODE not in ('NC', 'NA', 'NF') )";

	public Set<PSHRStaff> getAllUSStaff()
	{
		return getAllUSStaff(allUSStaffQuery);
	}

	private Set<PSHRStaff> getAllUSStaff(String query)
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

		return Container.toSet(jdbcTemplate.query(query, rowMapper));
	}

	public void setDataSource(DataSource dataSource)
	{
		jdbcTemplate = new JdbcTemplate(dataSource);
	}
}

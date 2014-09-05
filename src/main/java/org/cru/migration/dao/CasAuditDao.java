package org.cru.migration.dao;

import com.google.common.collect.ImmutableMap;
import org.cru.migration.domain.CasAuditUser;
import org.cru.migration.support.Evaluation;
import org.joda.time.DateTime;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Lee Braddock
 */
public class CasAuditDao
{
	private JdbcTemplate jdbcTemplate;

	private final String usernameMappedData = "username";

	private final String queryTemplate =
			"select aud_user, max(aud_date) as aud_date from com_audit_trail where aud_user = '${" +
					usernameMappedData +
					"}' group by aud_user";

	public CasAuditUser getCasAuditUser(String username)
	{
		ImmutableMap<String,String> mappedData = ImmutableMap.<String, String>builder()
				.put(usernameMappedData, username)
				.build();

		String query = Evaluation.evaluate(queryTemplate, mappedData);

		return getCasAuditUserQuery(query);
	}

	private CasAuditUser getCasAuditUserQuery(String query)
	{
		RowMapper<CasAuditUser> rowMapper =
				new RowMapper<CasAuditUser>()
				{
					public CasAuditUser mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						CasAuditUser casAuditUser = new CasAuditUser();

						casAuditUser.setUsername(rs.getString("aud_user"));
						casAuditUser.setDate(new DateTime(rs.getDate("aud_date")));

						return casAuditUser;
					}
				};

		try
		{
			return jdbcTemplate.queryForObject(query, rowMapper);
		}
		catch (EmptyResultDataAccessException e)
		{
			return null;
		}
	}

	public void setDataSource(DataSource dataSource)
	{
		jdbcTemplate = new JdbcTemplate(dataSource);
	}
}

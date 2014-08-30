package org.cru.migration.dao;

import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.domain.RelayUser;
import org.joda.time.DateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CssDao
{
	private JdbcTemplate jdbcTemplate;

	final String query = "SELECT ssoguid, username, lastChanged, password from css_password";

	public List<CssRelayUser> getCssRelayUsers(List<RelayUser> relayUsers)
	{
		List<CssRelayUser> cssRelayUsers = getCssRelayUsers(query);

		return cssRelayUsers;
	}

	private List<CssRelayUser> getCssRelayUsers(String query)
	{
		RowMapper<CssRelayUser> rowMapper =
				new RowMapper<CssRelayUser>()
				{
					public CssRelayUser mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						CssRelayUser cssRelayUser = new CssRelayUser();

						cssRelayUser.setSsoguid(rs.getString("ssoguid"));
						cssRelayUser.setUsername(rs.getString("username"));

						// TODO CHeck this conversion
						cssRelayUser.setLastChanged(new DateTime(rs.getDate("lastChanged")));
						cssRelayUser.setPassword(rs.getString("password"));

						return cssRelayUser;
					}
				};

		return jdbcTemplate.query(query, rowMapper);
	}

	public void setDataSource(DataSource dataSource)
	{
		jdbcTemplate = new JdbcTemplate(dataSource);
	}
}

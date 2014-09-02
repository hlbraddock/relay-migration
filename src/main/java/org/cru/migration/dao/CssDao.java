package org.cru.migration.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.support.Output;
import org.cru.migration.support.StringUtilities;
import org.joda.time.DateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class CssDao
{
	private JdbcTemplate jdbcTemplate;

	private final String query = "SELECT ssoguid, username, lastChanged, encPassword from idm_passwords";

	private static final int MaxWhereClauseInLimit = 1000;

	public Set<CssRelayUser> getCssRelayUsers(List<RelayUser> relayUsers)
	{
		Set<CssRelayUser> allCssRelayUsers = Sets.newHashSet();

		Output.println("Size of relay user list is " + relayUsers.size());

		for(int iterator = 0; (relayUsers.size() > 0) && (relayUsers.size() > iterator);
			iterator+= MaxWhereClauseInLimit)
		{
			int end = iterator +
					(relayUsers.size() - iterator >= MaxWhereClauseInLimit ? MaxWhereClauseInLimit - 1 :
							(relayUsers.size() % MaxWhereClauseInLimit)-1);

			Output.println("range " + iterator + ", "  + end);
			List<String> ssoguidList = getSsoguidListByRange(relayUsers, iterator, end);

			Output.println("Size of ssoguid list is " + ssoguidList.size());

			String delimitedSsoguidString = StringUtilities.delimitAndSurround(ssoguidList, ',', '\'');

			List<CssRelayUser> cssRelayUsers =
					getCssRelayUsers(query + " where ssoguid in (" + delimitedSsoguidString + ")" + "");

			Output.println("Size of all css relay users list is (before) " + allCssRelayUsers.size());
			allCssRelayUsers.addAll(cssRelayUsers);
			Output.println("Size of all css relay users list is (after) " + allCssRelayUsers.size());
		}

		return allCssRelayUsers;
	}

	private List<String> getSsoguidListByRange(List<RelayUser> relayUsers, int begin, int end)
	{
		List<String> ssoguidList = Lists.newArrayList();

		for(int iterator = begin; iterator <= end; iterator++)
		{
			if(relayUsers.size() < iterator)
			{
				break;
			}

			ssoguidList.add(relayUsers.get(iterator).getSsoguid());
		}

		return ssoguidList;
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

						// TODO Check this conversion, need windows to unix conversion?
						cssRelayUser.setLastChanged(new DateTime(rs.getDate("lastChanged")));
						cssRelayUser.setPassword(rs.getString("encPassword"));

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

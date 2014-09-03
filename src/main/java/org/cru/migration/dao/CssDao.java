package org.cru.migration.dao;

import com.google.common.collect.Sets;
import org.ccci.util.properties.CcciPropsTextEncryptor;
import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.support.Container;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;
import org.cru.migration.support.StringUtilities;
import org.jasypt.util.text.TextEncryptor;
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

	private MigrationProperties migrationProperties = new MigrationProperties();

	public Set<CssRelayUser> getCssRelayUsers(Set<RelayUser> relayUsers)
	{
		Set<CssRelayUser> cssRelayUsers = getEncryptedPasswordCssRelayUsers(relayUsers);

		TextEncryptor textEncryptor = new CcciPropsTextEncryptor(migrationProperties.getNonNullProperty
				("encryptionPassword"), true);

		for(CssRelayUser cssRelayUser : cssRelayUsers)
		{
			cssRelayUser.setPassword(textEncryptor.decrypt(cssRelayUser.getPassword()));
		}

		return cssRelayUsers;
	}

	private Set<CssRelayUser> getEncryptedPasswordCssRelayUsers(Set<RelayUser> relayUsers)
	{
		Set<CssRelayUser> allCssRelayUsers = Sets.newHashSet();

		for(int iterator = 0; (relayUsers.size() > 0) && (relayUsers.size() > iterator);
			iterator+= MaxWhereClauseInLimit)
		{
			int end = iterator +
					(relayUsers.size() - iterator >= MaxWhereClauseInLimit ? MaxWhereClauseInLimit - 1 :
							(relayUsers.size() % MaxWhereClauseInLimit)-1);

			Output.println("range " + iterator + ", "  + end);
			Set<String> ssoguidList = getSsoguidListByRange(Container.toList(relayUsers), iterator, end);

			String delimitedSsoguidString = StringUtilities.delimitAndSurround(ssoguidList, ',', '\'');

			List<CssRelayUser> cssRelayUsers =
					getCssRelayUsers(query + " where ssoguid in (" + delimitedSsoguidString + ")" + "");

			allCssRelayUsers.addAll(cssRelayUsers);
		}

		return allCssRelayUsers;
	}

	private Set<String> getSsoguidListByRange(List<RelayUser> relayUsers, int begin, int end)
	{
		Set<String> ssoguidList = Sets.newHashSet();

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

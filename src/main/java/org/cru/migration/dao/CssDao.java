package org.cru.migration.dao;

import com.google.common.collect.Sets;
import org.ccci.util.properties.CcciPropsTextEncryptor;
import org.cru.migration.domain.CssRelayUser;
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

	public Set<CssRelayUser> getCssRelayUsers(Set<String> ssoguids)
	{
		Set<CssRelayUser> cssRelayUsers = getEncryptedPasswordCssRelayUsers(ssoguids);

		TextEncryptor textEncryptor = new CcciPropsTextEncryptor(migrationProperties.getNonNullProperty
				("encryptionPassword"), true);

		for(CssRelayUser cssRelayUser : cssRelayUsers)
		{
			cssRelayUser.setPassword(textEncryptor.decrypt(cssRelayUser.getPassword()));
		}

		return cssRelayUsers;
	}

	private Set<CssRelayUser> getEncryptedPasswordCssRelayUsers(Set<String> ssoguids)
	{
		Set<CssRelayUser> allCssRelayUsers = Sets.newHashSet();

		for(int iterator = 0; (ssoguids.size() > 0) && (ssoguids.size() > iterator); iterator+= MaxWhereClauseInLimit)
		{
			int end = iterator +
					(ssoguids.size() - iterator >= MaxWhereClauseInLimit ? MaxWhereClauseInLimit - 1 :
							(ssoguids.size() % MaxWhereClauseInLimit)-1);

			System.out.printf("CSS DAO query range " + iterator + ", "  + end + "\r");

			String ssoguidQuery =
					StringUtilities.delimitAndSurround(
							Container.getListByRange(Container.toList(ssoguids), iterator, end), ',', '\'');

			List<CssRelayUser> cssRelayUsers =
					getCssRelayUsers(query + " where ssoguid in (" + ssoguidQuery + ")" + "");

			allCssRelayUsers.addAll(cssRelayUsers);
		}

		Output.println("");

		return allCssRelayUsers;
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

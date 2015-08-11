package org.cru.migration.service;

import com.google.common.collect.Sets;
import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;
import org.cru.migration.support.Container;
import org.cru.migration.support.StringUtilities;
import org.joda.time.DateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.naming.NamingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class CssDaoService
{
    private JdbcTemplate jdbcTemplate;

    private static final int MaxWhereClauseInLimit = 1000;

    public CssDaoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

	public Set<CssRelayUser> getCssRelayUsers(Set<String> ssoguids) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

		CssRelayUsersData cssRelayUsersData = new CssRelayUsersData(ssoguids);

		executionService.execute(new GetCssRelayUsers(), cssRelayUsersData, 200);

		return cssRelayUsersData.getCssRelayUsers();
	}

	private class CssRelayUsersData
	{
        private Set<String> ssoguids;
		private Set<CssRelayUser> cssRelayUsers = Sets.newConcurrentHashSet();

        private CssRelayUsersData(Set<String> ssoguids)
        {
            this.ssoguids = ssoguids;
        }

        public Set<String> getSsoguids()
        {
            return ssoguids;
        }

        public Set<CssRelayUser> getCssRelayUsers()
        {
            return cssRelayUsers;
        }
    }

	public class GetCssRelayUsers implements ExecuteAction
	{
		@Override
		public void execute(ExecutorService executorService, Object object)
		{
			CssRelayUsersData cssRelayUsersData = (CssRelayUsersData)object;

            final String queryBase =  "SELECT idm_passwords.ssoguid, username, lastchanged, encpassword, question, " +
                    "encanswer, createddate FROM idm_passwords FULL OUTER JOIN idm_security_q_and_a " +
                    "ON idm_passwords.ssoguid = idm_security_q_and_a.ssoguid ";

            for(int iterator = 0; (cssRelayUsersData.getSsoguids().size() > 0) && (cssRelayUsersData.getSsoguids().size() > iterator); iterator+=
                MaxWhereClauseInLimit)
            {
                int end = iterator +
                        (cssRelayUsersData.getSsoguids().size() - iterator >= MaxWhereClauseInLimit ? MaxWhereClauseInLimit - 1 :
                                (cssRelayUsersData.getSsoguids().size() % MaxWhereClauseInLimit)-1);

                String ssoguidQuery =
                        StringUtilities.delimitAndSurround(
                                Container.getListByRange(Container.toList(cssRelayUsersData.getSsoguids()), iterator, end), ',', '\'');

                String query = queryBase + " where upper(idm_passwords.ssoguid) in (" + ssoguidQuery + ")" + "";

                executorService.execute(new CssDaoQueryWorkerThread(query, cssRelayUsersData.getCssRelayUsers()));
            }
        }
	}

	private class CssDaoQueryWorkerThread implements Runnable
	{
        private String query;
        private Set<CssRelayUser> cssRelayUsers;

        private CssDaoQueryWorkerThread(String query, Set<CssRelayUser> cssRelayUsers)
        {
            this.query = query;
            this.cssRelayUsers = cssRelayUsers;
        }

        @Override
		public void run()
		{
            try
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
                                cssRelayUser.setQuestion(rs.getString("question"));
                                cssRelayUser.setAnswer(rs.getString("encanswer"));
                                cssRelayUser.setCreatedDate(new DateTime(rs.getDate("createddate")));

                                return cssRelayUser;
                            }
                        };

                cssRelayUsers.addAll(jdbcTemplate.query(query, rowMapper));
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}

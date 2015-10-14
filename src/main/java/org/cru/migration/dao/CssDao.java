package org.cru.migration.dao;

import com.google.common.collect.Sets;
import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.service.CssDaoService;
import org.cru.migration.service.SetCssRelayUsersDecryptedPasswordService;
import org.cru.migration.support.Container;
import org.cru.migration.support.MigrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.Set;

public class CssDao
{
	private JdbcTemplate jdbcTemplate;

	private MigrationProperties migrationProperties = new MigrationProperties();

	private Logger logger = LoggerFactory.getLogger(getClass());

	public Set<CssRelayUser> getCssRelayUsers(Set<String> ssoguids) throws NamingException
    {
        logger.info("Getting CSS relay users from ssoguids size  ... " + ssoguids.size());

		Set<CssRelayUser> cssRelayUsers = getEncryptedPasswordCssRelayUsers(Container.uppercase(ssoguids));

        logger.info("Got CSS relay users size " + cssRelayUsers.size());

        logger.info("Setting CSS relay users passwords from retrieved password data ...");

        SetCssRelayUsersDecryptedPasswordService setCssRelayUsersDecryptedPasswordService = new
                SetCssRelayUsersDecryptedPasswordService(migrationProperties);

        Set<CssRelayUser> threadSafeCssRelayUsers = Sets.newConcurrentHashSet();
        threadSafeCssRelayUsers.addAll(cssRelayUsers);
        setCssRelayUsersDecryptedPasswordService.setRelayUsersPassword(threadSafeCssRelayUsers);

        logger.info("Finished setting CSS relay users passwords from retrieved password data.");

        return threadSafeCssRelayUsers;
	}

	private Set<CssRelayUser> getEncryptedPasswordCssRelayUsers(Set<String> ssoguids) throws NamingException
	{
        CssDaoService cssDaoService = new CssDaoService(jdbcTemplate);

        return cssDaoService.getCssRelayUsers(ssoguids);
	}

	public void setDataSource(DataSource dataSource)
	{
		jdbcTemplate = new JdbcTemplate(dataSource);
	}
}

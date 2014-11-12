package org.cru.migration.dao;

import org.ccci.util.properties.CcciPropsTextEncryptor;
import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.service.CssDaoService;
import org.cru.migration.support.Container;
import org.cru.migration.support.MigrationProperties;
import org.jasypt.util.text.TextEncryptor;
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
        logger.info("Getting CSS relay users ...");

		Set<CssRelayUser> cssRelayUsers = getEncryptedPasswordCssRelayUsers(Container.uppercase(ssoguids));

        logger.info("Got CSS relay users size " + cssRelayUsers.size());

		TextEncryptor textEncryptor = new CcciPropsTextEncryptor(migrationProperties.getNonNullProperty
				("encryptionPassword"), true);

        logger.info("Setting CSS relay users passwords from retrieved password data ...");

        for(CssRelayUser cssRelayUser : cssRelayUsers)
		{
			cssRelayUser.setPassword(textEncryptor.decrypt(cssRelayUser.getPassword()));
		}

        logger.info("Finished setting CSS relay users passwords from retrieved password data.");

        return cssRelayUsers;
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

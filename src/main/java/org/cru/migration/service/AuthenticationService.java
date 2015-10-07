package org.cru.migration.service;

import com.google.common.collect.Sets;
import org.ccci.idm.ldap.Ldap;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;
import org.cru.migration.support.MigrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class AuthenticationService
{
	private Set<String> successAuthentication = Sets.newConcurrentHashSet();
	private Set<String> failedAuthentication = Sets.newConcurrentHashSet();
    private Set<String> failedAuthenticationReason = Sets.newConcurrentHashSet();

    private String ldapServer;
    private String userRootDn;
    private String usernameAttribute;

    private Logger logger = LoggerFactory.getLogger(getClass());

    public class Results
    {
        public Set<String> successAuthentication;
        public Set<String> failedAuthentication;
        public Set<String> failedAuthenticationReason;

        public Results(final Set<String> successAuthentication, final Set<String> failedAuthentication,
                       final Set<String> failedAuthenticationReason) {
            this.failedAuthentication = failedAuthentication;
            this.failedAuthenticationReason = failedAuthenticationReason;
            this.successAuthentication = successAuthentication;
        }
    }

    public AuthenticationService(String ldapServer, String userRootDn, String usernameAttribute)
    {
        this.ldapServer = ldapServer;
        this.userRootDn = userRootDn;
        this.usernameAttribute = usernameAttribute;
    }

    public Results authenticate(Set<RelayUser> relayUsers) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

		AuthenticateData authenticateData = new AuthenticateData(relayUsers);

		executionService.execute(new Authenticate(), authenticateData, 50);

        return new Results(successAuthentication, failedAuthentication, failedAuthenticationReason);
	}

	private class AuthenticateData
	{
		private Set<RelayUser> relayUsers;

		public AuthenticateData(Set<RelayUser> relayUsers)
		{
			this.relayUsers = relayUsers;
		}
	}

	public class Authenticate implements ExecuteAction
	{
		@Override
		public void execute(ExecutorService executorService, Object object)
		{
			AuthenticateData authenticateData = (AuthenticateData)object;

			for (RelayUser relayUser : authenticateData.relayUsers)
			{
				executorService.execute(new AuthenticationThread(relayUser));
			}
		}
	}

	private class AuthenticationThread implements Runnable
	{
		private RelayUser relayUser;

		public AuthenticationThread(RelayUser relayUser)
		{
			this.relayUser = relayUser;
		}

		@Override
		public void run()
		{
			Ldap ldap = null;

			try
			{
				String dn = usernameAttribute + "=" + relayUser.getUsername() + "," + userRootDn;

				ldap = new Ldap(ldapServer, dn, relayUser.getPassword());

				successAuthentication.add("" + relayUser.getUsername());
			}
			catch(Exception e)
			{
                failedAuthentication.add("" + relayUser.getUsername());
                failedAuthenticationReason.add("" + relayUser.getUsername() + ": " + e.getMessage());

                if(!e.getMessage().contains("LDAP: error code 49 - NDS error: failed authentication (-669)")) {
                    logger.info("failed authentication for unexpected reason : " + e.getMessage());
                }
			}
			finally
			{
    			if(ldap != null)
				{
					try
					{
						ldap.close();
					}
					catch(Exception e)
					{
					}
				}
			}
		}
	}
}

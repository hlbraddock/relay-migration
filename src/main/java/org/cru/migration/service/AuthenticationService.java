package org.cru.migration.service;

import com.google.common.collect.Sets;
import org.ccci.idm.ldap.Ldap;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;
import org.cru.migration.support.FileHelper;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;

import javax.naming.NamingException;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class AuthenticationService
{
	private MigrationProperties properties;

	private Set<String> successAuthentication = Sets.newConcurrentHashSet();
	private Set<String> failedAuthentication = Sets.newConcurrentHashSet();

    private String ldapServer;
    private String userRootDn;
    private String usernameAttribute;

    public AuthenticationService(String ldapServer, String userRootDn, String usernameAttribute)
    {
        this.properties = new MigrationProperties();

        this.ldapServer = ldapServer;
        this.userRootDn = userRootDn;
        this.usernameAttribute = usernameAttribute;
    }

    public void authenticate(Set<RelayUser> relayUsers) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

		AuthenticateData authenticateData = new AuthenticateData(relayUsers);

		executionService.execute(new Authenticate(), authenticateData, 50);

		Output.logMessage(successAuthentication,
				FileHelper.getFileToWrite(properties.getNonNullProperty("successAuthentication")));
		Output.logMessage(failedAuthentication,
				FileHelper.getFileToWrite(properties.getNonNullProperty("failedAuthentication")));
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
				String dn = usernameAttribute + relayUser.getUsername() + "," + userRootDn;

				ldap = new Ldap(ldapServer, dn, relayUser.getPassword());

				successAuthentication.add("" + relayUser.getUsername());
			}
			catch(Exception e)
			{
				failedAuthentication.add("" + relayUser.getUsername());
			}
			finally
			{

			}
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

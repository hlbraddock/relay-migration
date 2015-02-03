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
import java.io.File;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class AuthenticationService
{
	private static MigrationProperties properties = new MigrationProperties();

	private Set<String> successAuthentication = Sets.newConcurrentHashSet();
	private Set<String> failedAuthentication = Sets.newConcurrentHashSet();

    private String ldapServer;
    private String userRootDn;
    private String usernameAttribute;

    private File successAuthenticationFile;
    private File failedAuthenticationFile;

    public AuthenticationService(String ldapServer, String userRootDn, String usernameAttribute)
    {
        this(ldapServer, userRootDn, usernameAttribute,
                FileHelper.getFileToWrite(properties.getNonNullProperty("successAuthentication")),
                FileHelper.getFileToWrite(properties.getNonNullProperty("failedAuthentication")));
    }

    public AuthenticationService(String ldapServer, String userRootDn, String usernameAttribute, File successAuthenticationFile, File failedAuthenticationFile)
    {
        this.ldapServer = ldapServer;
        this.userRootDn = userRootDn;
        this.usernameAttribute = usernameAttribute;
        this.successAuthenticationFile = successAuthenticationFile;
        this.failedAuthenticationFile = failedAuthenticationFile;
    }

    public void authenticate(Set<RelayUser> relayUsers) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

		AuthenticateData authenticateData = new AuthenticateData(relayUsers);

		executionService.execute(new Authenticate(), authenticateData, 50);

		Output.logMessage(successAuthentication, successAuthenticationFile);
		Output.logMessage(failedAuthentication, failedAuthenticationFile);
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

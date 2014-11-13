package org.cru.migration.service;

import org.ccci.util.properties.CcciPropsTextEncryptor;
import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;
import org.cru.migration.support.MigrationProperties;
import org.jasypt.util.text.TextEncryptor;

import javax.naming.NamingException;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class SetCssRelayUsersDecryptedPasswordService
{
    private MigrationProperties migrationProperties;

    public SetCssRelayUsersDecryptedPasswordService(MigrationProperties migrationProperties)
    {
        this.migrationProperties = migrationProperties;
    }

    public void setRelayUsersPassword(Set<CssRelayUser> cssRelayUsers) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

        TextEncryptor textEncryptor = new CcciPropsTextEncryptor(migrationProperties.getNonNullProperty
                ("encryptionPassword"), true);

        ServiceData serviceData = new ServiceData(cssRelayUsers, textEncryptor);

		executionService.execute(new SetRelayUsersPassword(), serviceData, 1000);
	}

	private class ServiceData
	{
        private Set<CssRelayUser> cssRelayUsers;
        private TextEncryptor textEncryptor;

        private ServiceData(Set<CssRelayUser> cssRelayUsers, TextEncryptor textEncryptor)
        {
            this.cssRelayUsers = cssRelayUsers;
            this.textEncryptor = textEncryptor;
        }

        public Set<CssRelayUser> getCssRelayUsers()
        {
            return cssRelayUsers;
        }

        public TextEncryptor getTextEncryptor()
        {
            return textEncryptor;
        }
    }

	public class SetRelayUsersPassword implements ExecuteAction
	{
		@Override
		public void execute(ExecutorService executorService, Object object)
		{
			ServiceData serviceData = (ServiceData)object;

            for(CssRelayUser cssRelayUser : serviceData.getCssRelayUsers())
            {
                executorService.execute(new WorkerThread(cssRelayUser, serviceData.getTextEncryptor()));
            }
        }
	}

	private class WorkerThread implements Runnable
	{
        private CssRelayUser cssRelayUser;
        private TextEncryptor textEncryptor;

        private WorkerThread(CssRelayUser cssRelayUser, TextEncryptor textEncryptor)
        {
            this.cssRelayUser = cssRelayUser;
            this.textEncryptor = textEncryptor;
        }

        @Override
		public void run()
		{
            try
			{
                cssRelayUser.setPassword(textEncryptor.decrypt(cssRelayUser.getPassword()));
            }
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}

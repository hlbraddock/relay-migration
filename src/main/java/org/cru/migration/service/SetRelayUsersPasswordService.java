package org.cru.migration.service;

import com.google.common.collect.Sets;
import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;

import javax.naming.NamingException;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class SetRelayUsersPasswordService
{
    public SetRelayUsersPasswordService()
    {
    }

    public class Results
    {
        Set<RelayUser> relayUsersWithPassword = Sets.newConcurrentHashSet();

        public Set<RelayUser> getRelayUsersWithPassword()
        {
            return relayUsersWithPassword;
        }
    }

	public Results setRelayUsersPassword(Set<CssRelayUser> cssRelayUsers, Set<RelayUser> relayUsers) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

        Results results = new Results();

		ServiceData serviceData = new ServiceData(cssRelayUsers, relayUsers, results);

		executionService.execute(new SetRelayUsersPassword(), serviceData, 400);

		return serviceData.getResults();
	}

	private class ServiceData
	{
        private Set<CssRelayUser> cssRelayUsers;
        private Set<RelayUser> relayUsers;
        private Results results;

        private ServiceData(Set<CssRelayUser> cssRelayUsers, Set<RelayUser> relayUsers, Results results)
        {
            this.cssRelayUsers = cssRelayUsers;
            this.relayUsers = relayUsers;
            this.results = results;
        }

        public Set<CssRelayUser> getCssRelayUsers()
        {
            return cssRelayUsers;
        }

        public Set<RelayUser> getRelayUsers()
        {
            return relayUsers;
        }

        public Results getResults()
        {
            return results;
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
                executorService.execute(new WorkerThread(cssRelayUser, serviceData.getRelayUsers(),
                        serviceData.getResults()));

            }

        }
	}

	private class WorkerThread implements Runnable
	{
        private CssRelayUser cssRelayUser;
        private Set<RelayUser> relayUsers;
        private Results results;

        private WorkerThread(CssRelayUser cssRelayUser, Set<RelayUser> relayUsers, Results results)
        {
            this.cssRelayUser = cssRelayUser;
            this.relayUsers = relayUsers;
            this.results = results;
        }

        @Override
		public void run()
		{
            try
			{
                RelayUser relayUser = RelayUser.havingSsoguid(relayUsers, cssRelayUser.getSsoguid());

                if(relayUser != null)
                {
                    relayUser.setPassword(cssRelayUser.getPassword());
                    relayUser.setSecurityQuestion(cssRelayUser.getQuestion());
                    relayUser.setSecurityAnswer(cssRelayUser.getAnswer());
                    results.getRelayUsersWithPassword().add(relayUser);
                }
            }
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}

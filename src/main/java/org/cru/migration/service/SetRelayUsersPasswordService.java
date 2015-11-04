package org.cru.migration.service;

import com.google.common.collect.Sets;
import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;

import javax.naming.NamingException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class SetRelayUsersPasswordService
{
    public SetRelayUsersPasswordService()
    {
    }

    public class Results
    {
        public Set<RelayUser> relayUsersWithPassword = Sets.newConcurrentHashSet();

        public Set<RelayUser> getRelayUsersWithPassword()
        {
            return relayUsersWithPassword;
        }
    }

    static AtomicInteger counter = new AtomicInteger();

	public Results setRelayUsersPassword(Set<CssRelayUser> cssRelayUsers, Map<String, RelayUser> relayUsers) throws
            NamingException
	{
		ExecutionService executionService = new ExecutionService();

        Results results = new Results();

        counter.set(0);

		ServiceData serviceData = new ServiceData(cssRelayUsers, relayUsers, results);

		executionService.execute(new SetRelayUsersPassword(), serviceData, 200);

		return serviceData.results;
	}

	private class ServiceData
	{
        public Set<CssRelayUser> cssRelayUsers;
        public Map<String, RelayUser> relayUsers;
        public Results results;

        private ServiceData(Set<CssRelayUser> cssRelayUsers, Map<String, RelayUser> relayUsers, Results results)
        {
            this.cssRelayUsers = cssRelayUsers;
            this.relayUsers = relayUsers;
            this.results = results;
        }
    }

	public class SetRelayUsersPassword implements ExecuteAction
	{
		@Override
		public void execute(ExecutorService executorService, Object object)
		{
			ServiceData serviceData = (ServiceData)object;

            for(CssRelayUser cssRelayUser : serviceData.cssRelayUsers)
            {
                executorService.execute(
                        new WorkerThread(cssRelayUser, serviceData.relayUsers, serviceData.results));
            }
        }
	}

	private class WorkerThread implements Runnable
	{
        private CssRelayUser cssRelayUser;
        private Map<String, RelayUser> relayUsers;
        private Results results;

        private WorkerThread(CssRelayUser cssRelayUser, Map<String, RelayUser> relayUsers, Results results)
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
                if (counter.addAndGet(1) % 1000 == 0)
                {
                    System.out.printf("Runtime counter: setting relay user password " + relayUsers.size() + " of " +
                            "total " + counter + "\r");
                }

                RelayUser relayUser = relayUsers.get(cssRelayUser.getSsoguid());

                if(relayUser != null)
                {
                    relayUser.setPassword(cssRelayUser.getPassword());
                    relayUser.setSecurityQuestion(cssRelayUser.getQuestion());
                    relayUser.setSecurityAnswer(cssRelayUser.getAnswer());
                    results.relayUsersWithPassword.add(relayUser);
                }
            }
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}

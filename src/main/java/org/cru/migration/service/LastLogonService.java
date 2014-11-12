package org.cru.migration.service;

import com.google.common.collect.Sets;
import org.cru.migration.dao.CasAuditDao;
import org.cru.migration.domain.CasAuditUser;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;
import org.cru.migration.support.StringUtilities;

import javax.naming.NamingException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class LastLogonService
{
    private CasAuditDao casAuditDao;

    public LastLogonService(CasAuditDao casAuditDao)
    {
        this.casAuditDao = casAuditDao;
    }

    public class Results
    {
        private AtomicInteger numberRelayUsersSet = new AtomicInteger(0);
        private Set<RelayUser> notFound = Sets.newConcurrentHashSet();
        private AtomicInteger nullDateCount = new AtomicInteger(0);

        public Set<RelayUser> getNotFound()
        {
            return notFound;
        }

        public AtomicInteger getNullDateCount()
        {
            return nullDateCount;
        }

        public AtomicInteger getNumberRelayUsersSet()
        {
            return numberRelayUsersSet;
        }

        public void incrementNullDateCount()
        {
            this.nullDateCount.getAndAdd(1);
        }

        public void incrementRelayUsersSet()
        {
            this.numberRelayUsersSet.getAndAdd(1);
        }
    }

    public Results setLastLogon(Set<RelayUser> relayUsers) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

        Results results = new Results();

		LastLogonData lastLogonData = new LastLogonData(relayUsers, results);

		executionService.execute(new SetLastLogon(), lastLogonData, 200);

        return lastLogonData.getResults();
	}

	private class LastLogonData
	{
        private Set<RelayUser> relayUsers;
        private Results results;

        private LastLogonData(Set<RelayUser> relayUsers, Results results)
        {
            this.relayUsers = relayUsers;
            this.results = results;
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

	public class SetLastLogon implements ExecuteAction
	{
		@Override
		public void execute(ExecutorService executorService, Object object)
		{
			LastLogonData lastLogonData = (LastLogonData)object;

            for(RelayUser relayUser : lastLogonData.getRelayUsers())
            {
                executorService.execute(new LastLogonWorkerThread(relayUser, lastLogonData.getResults()));
            }
        }
	}

	private class LastLogonWorkerThread implements Runnable
	{
        private RelayUser relayUser;
        private Results results;

        private LastLogonWorkerThread(RelayUser relayUser, Results results)
        {
            this.relayUser = relayUser;
            this.results = results;
        }

        @Override
		public void run()
		{
            try
			{
                CasAuditUser casAuditUser = getCasAuditUser(relayUser.getUsername());

                if(casAuditUser != null)
                {
                    if(casAuditUser.getDate() != null)
                    {
                        relayUser.setLastLogonTimestamp(casAuditUser.getDate());
                        results.incrementRelayUsersSet();
                    }
                    else
                    {
                        results.incrementNullDateCount();
                    }
                }
                else
                {
                    results.getNotFound().add(relayUser);
                }
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

    private CasAuditUser getCasAuditUser(String username)
    {
        CasAuditUser casAuditUser = casAuditDao.getCasAuditUser(username);
        if(casAuditUser == null)
        {
            casAuditUser = casAuditDao.getCasAuditUser(username.toLowerCase());
        }
        if(casAuditUser == null)
        {
            casAuditUser = casAuditDao.getCasAuditUser(username.toUpperCase());
        }
        if(casAuditUser == null)
        {
            casAuditUser = casAuditDao.getCasAuditUser(StringUtilities.isEmail(username) ? StringUtilities
                    .capitalizeEmail(username) : StringUtilities.capitalize(username, ".", "\\."));
        }

        return casAuditUser;
    }
}

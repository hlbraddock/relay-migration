package org.cru.migration.service;

import org.ccci.idm.ldap.Ldap;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class UserCountService
{
	private Ldap ldap;

	public UserCountService(Ldap ldap)
	{
		this.ldap = ldap;
	}

	public Integer getUserCount(String rootDn, String searchAttribute) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

		GetUserCountData getUserCountData = new GetUserCountData(searchAttribute, rootDn);

		executionService.execute(new GetUserCount(), getUserCountData, 50);

		return getUserCountData.getUserCount().get();
	}

	private char[] extendedAlphabet = {'-','.', '$',
			'a','b','c','d','e','f','g','h'
			,'i','j','k','l','m','n','o','p','q'
			,'r','s','t','u','v','w','x','y','z',
			'0','1','2','3','4','5','6','7','8','9'};

	private char[] lessExtendedAlphabet = {'_','@','-','.',
			'a','b','c','d','e','f','g','h'
			,'i','j','k','l','m','n','o','p','q'
			,'r','s','t','u','v','w','x','y','z',
			'0','1','2','3','4','5','6','7','8','9'};

	private List<String> searchExclude = Arrays.asList("__");

	private class GetUserCountData
	{
		private String searchAttribute;
		private String rootDn;
		private AtomicInteger userCount = new AtomicInteger(0);

		private GetUserCountData(String searchAttribute, String rootDn)
		{
			this.searchAttribute = searchAttribute;
			this.rootDn = rootDn;
		}

		public String getSearchAttribute()
		{
			return searchAttribute;
		}

		public String getRootDn()
		{
			return rootDn;
		}

		public AtomicInteger getUserCount()
		{
			return userCount;
		}
	}

	public class GetUserCount implements ExecuteAction
	{
		@Override
		public void execute(ExecutorService executorService, Object object)
		{
			GetUserCountData getUserCountData = (GetUserCountData)object;

			for(char first : extendedAlphabet)
			{
				for (char second : lessExtendedAlphabet)
				{
                    for(char third : lessExtendedAlphabet)
                    {
                        String searchValue = "" + first + second + third;
                        String searchFilter = getUserCountData.getSearchAttribute() + "=" + searchValue + "*";

                        if(searchExclude.contains(searchValue))
                        {
                            continue;
                        }

                        executorService.execute(new LdapSearchCounterWorkerThread(getUserCountData.getRootDn(),
                                searchFilter, getUserCountData.getUserCount()));
                    }
                }
			}
		}
	}

	private class LdapSearchCounterWorkerThread implements Runnable
	{
		private String rootDn;
		private String searchFilter;
		private AtomicInteger userCount;

		private LdapSearchCounterWorkerThread(String rootDn, String searchFilter, AtomicInteger userCount)
		{
			this.rootDn = rootDn;
			this.searchFilter = searchFilter;
			this.userCount = userCount;
		}

		@Override
		public void run()
		{
			try
			{
				String[] returningAttributes = new String[]{};
				Map<String, Attributes> results =
						ldap.searchAttributes(rootDn, searchFilter, returningAttributes);

				userCount.addAndGet(results.size());
			}
			catch(Exception e)
			{}
		}
	}
}



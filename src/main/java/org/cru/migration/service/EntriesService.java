package org.cru.migration.service;

import com.google.common.collect.Maps;
import org.ccci.idm.ldap.Ldap;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class EntriesService
{
	private Ldap ldap;

	public EntriesService(Ldap ldap)
	{
		this.ldap = ldap;
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

	public Map<String, Attributes> getEntries(String rootDn, String searchAttribute, int depth) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

		GetEntriesData getEntriesData = new GetEntriesData(searchAttribute, rootDn, depth);

		executionService.execute(new GetEntries(), getEntriesData, 50);

		return getEntriesData.getQueryResults();
	}

	private class GetEntriesData
	{
		private String searchAttribute;
		private String rootDn;
		private Integer depth;
		private Map<String, Attributes> queryResults = Maps.newConcurrentMap();

		private GetEntriesData(String searchAttribute, String rootDn, Integer depth)
		{
			this.searchAttribute = searchAttribute;
			this.rootDn = rootDn;
			this.depth = depth;
		}

		public String getSearchAttribute()
		{
			return searchAttribute;
		}

		public String getRootDn()
		{
			return rootDn;
		}

		public Integer getDepth()
		{
			return depth;
		}

		public Map<String, Attributes> getQueryResults()
		{
			return queryResults;
		}
	}

	public class GetEntries implements ExecuteAction
	{
		@Override
		public void execute(ExecutorService executorService, Object object)
		{
			GetEntriesData getEntriesData = (GetEntriesData)object;

			for(int index=0; index< extendedAlphabet.length; index++)
			{
				for (int index2 = 0; index2 < lessExtendedAlphabet.length; index2++)
				{
					if (getEntriesData.getDepth() >= 3)
					{
						for (int index3 = 0; index3 < lessExtendedAlphabet.length; index3++)
						{
							String searchValue = "" + extendedAlphabet[index] + lessExtendedAlphabet[index2] +
									lessExtendedAlphabet[index3];
							String searchFilter = getEntriesData.getSearchAttribute() + "=" + searchValue + "*";

							if (searchExclude.contains(searchValue))
							{
								continue;
							}

							executorService.execute(new LdapQueryWorkerThread(getEntriesData.getRootDn(),
									searchFilter, getEntriesData.getQueryResults()));
						}
					}
					else
					{
						String searchValue = "" + extendedAlphabet[index] + lessExtendedAlphabet[index2];
						String searchFilter = getEntriesData.getSearchAttribute() + "=" + searchValue + "*";

						if (searchExclude.contains(searchValue))
						{
							continue;
						}

						executorService.execute(new LdapQueryWorkerThread(getEntriesData.getRootDn(), searchFilter,
								getEntriesData.getQueryResults()));
					}
				}
			}
		}
	}

	private class LdapQueryWorkerThread implements Runnable
	{
		private String rootDn;
		private String searchFilter;
		private Map<String, Attributes> queryResults;

		private LdapQueryWorkerThread(String rootDn, String searchFilter, Map<String, Attributes> queryResults)
		{
			this.rootDn = rootDn;
			this.searchFilter = searchFilter;
			this.queryResults = queryResults;
		}

		@Override
		public void run()
		{
			try
			{
				String[] returningAttributes = new String[]{};
				Map<String, Attributes> results =
						ldap.searchAttributes(rootDn, searchFilter, returningAttributes);

				queryResults.putAll(results);
			}
			catch(Exception e)
			{}
		}
	}
}

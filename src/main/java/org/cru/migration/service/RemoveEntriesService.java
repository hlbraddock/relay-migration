package org.cru.migration.service;

import org.ccci.idm.ldap.Ldap;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class RemoveEntriesService
{
	private Ldap ldap;

	public RemoveEntriesService(Ldap ldap)
	{
		this.ldap = ldap;
	}

	public void removeEntries(Map<String, Attributes> entries) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

		RemoveEntriesData getUserCountData = new RemoveEntriesData(entries, ldap);

		executionService.execute(new RemoveEntries(), getUserCountData, 200);
	}

	private class RemoveEntriesData
	{
		private Map<String, Attributes> entries;
		private Ldap ldap;

		private RemoveEntriesData(Map<String, Attributes> entries, Ldap ldap)
		{
			this.entries = entries;
			this.ldap = ldap;
		}

		public Map<String, Attributes> getEntries()
		{
			return entries;
		}

		public Ldap getLdap()
		{
			return ldap;
		}
	}

	public class RemoveEntries implements ExecuteAction
	{
		@Override
		public void execute(ExecutorService executorService, Object object)
		{
			RemoveEntriesData removeEntriesData = (RemoveEntriesData)object;

			for (Map.Entry<String, Attributes> entry : removeEntriesData.getEntries().entrySet())
			{
				String fullDn = entry.getKey();
				executorService.execute(new RemoveEntryWorkerThread(fullDn, removeEntriesData.getLdap()));
			}
		}
	}

	private class RemoveEntryWorkerThread implements Runnable
	{
		private String dn;
		private Ldap ldap;

		private RemoveEntryWorkerThread(String dn, Ldap ldap)
		{
			this.dn = dn;
			this.ldap = ldap;
		}

		@Override
		public void run()
		{
			try
			{
				ldap.deleteEntity(dn);
			}
			catch(Exception e)
			{}
		}
	}
}

package org.cru.migration.service;

import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;
import org.cru.migration.thekey.TheKeyBeans;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class RemoveEntriesService
{
	public void removeEntries(Map<String, Attributes> entries) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

		RemoveEntriesData getUserCountData = new RemoveEntriesData(entries);

		executionService.execute(new RemoveEntries(), getUserCountData, 200);
	}

	private class RemoveEntriesData
	{
		private Map<String, Attributes> entries;
		private LdapTemplate ldapTemplate = TheKeyBeans.getLdapTemplateMerge();

		private RemoveEntriesData(Map<String, Attributes> entries)
		{
			this.entries = entries;
		}

		public LdapTemplate getLdapTemplate()
		{
			return ldapTemplate;
		}

		public Map<String, Attributes> getEntries()
		{
			return entries;
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
				String key = entry.getKey();
				String[] nodes = key.split(",");
				executorService.execute(new RemoveEntryWorkerThread(nodes[0], removeEntriesData.getLdapTemplate()));
			}
		}
	}

	private class RemoveEntryWorkerThread implements Runnable
	{
		private String dn;
		private LdapTemplate ldapTemplate;

		private RemoveEntryWorkerThread(String dn, LdapTemplate ldapTemplate)
		{
			this.dn = dn;
			this.ldapTemplate = ldapTemplate;
		}

		@Override
		public void run()
		{
			try
			{
				ldapTemplate.unbind(new DistinguishedName(dn), true);
			}
			catch(Exception e)
			{}
		}
	}
}

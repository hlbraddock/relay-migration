package org.cru.migration.dao;

import com.google.common.collect.Maps;
import org.ccci.idm.ldap.Ldap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LdapDao
{
	private Ldap ldap;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public LdapDao(Ldap ldap)
	{
		this.ldap = ldap;
	}

	public DirContext createStructuralObjectClass
			(String className, String description, List<String> requiredAttributes,
			 String numericOid, String superClass) throws NamingException
	{
		DirContext schema = ldap.getContext().getSchema("");

		// Specify attributes for the schema object
		Attributes attributes = new BasicAttributes(true); // Ignore case
		attributes.put("NUMERICOID", numericOid);
		attributes.put("NAME", className);
		attributes.put("DESC", description);
		attributes.put("SUP", superClass);
		attributes.put("STRUCTURAL", "true");

		for(String requiredAttribute : requiredAttributes)
		{
			Attribute attribute = new BasicAttribute("MUST", requiredAttribute);
			logger.info("adding required attribute " + requiredAttribute);
			attributes.put(attribute);
		}

		// Add the new schema object for the object class
		return schema.createSubcontext(classDefinitionContextName(className), attributes);
	}

	/**
	 * WARNING: The server does not seem to honor these requests
	 */
	public void addAttributeToClass(String className, String attributeName, String type) throws NamingException
	{
		DirContext schema = ldap.getContext().getSchema("");

		Attributes attributes = new BasicAttributes(false);

		Attribute attribute = new BasicAttribute(type, attributeName);
		attributes.put(attribute);

		// Modify schema object
		schema.modifyAttributes(classDefinitionContextName(className), DirContext.ADD_ATTRIBUTE, attributes);
	}

	public DirContext createAttribute(String attributeName, String description,
									  String numericOid) throws NamingException
	{
		logger.info("creating attribute " + attributeName);
		DirContext schema = ldap.getContext().getSchema("");

		// Specify attributes for the schema object
		Attributes attributes = new BasicAttributes(true); // Ignore case

		attributes.put("NUMERICOID", numericOid);
		attributes.put("NAME", attributeName);
		attributes.put("DESC", description);
		attributes.put("SYNTAX", "1.3.6.1.4.1.1466.115.121.1.15");

		// Add the new schema object
		return schema.createSubcontext(attributeDefinitionContextName(attributeName), attributes);
	}

	public void deleteClass(String className) throws NamingException
	{
		DirContext schema = ldap.getContext().getSchema("");

		schema.destroySubcontext(classDefinitionContextName(className));
	}

	public void deleteAttribute(String attributeName) throws NamingException
	{
		DirContext schema = ldap.getContext().getSchema("");

		schema.destroySubcontext(attributeDefinitionContextName(attributeName));
	}

	private String attributeDefinitionContextName(String name)
	{
		return "AttributeDefinition/" + name;
	}

	private String classDefinitionContextName(String name)
	{
		return "ClassDefinition/" + name;
	}

	private char[] extendedAlphabet = {'-','.',
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

	private static AtomicInteger userCount = new AtomicInteger();
	public Integer getUserCount(String rootDn, String searchAttribute) throws NamingException
	{
		userCount = new AtomicInteger(0);

		ExecutorService executorService = Executors.newFixedThreadPool(50);

		for(int index=0; index< extendedAlphabet.length-1; index++)
		{
			for (int index2 = 0; index2 < lessExtendedAlphabet.length - 1; index2++)
			{
				String searchValue = "" + extendedAlphabet[index] + lessExtendedAlphabet[index2];
				String searchFilter = searchAttribute + "=" + searchValue + "*";

				if (searchExclude.contains(searchValue))
				{
					continue;
				}

				Runnable worker = new LdapSearchCounterWorkerThread(rootDn, searchFilter);

				executorService.execute(worker);
			}
		}

		executorService.shutdown();

		logger.info("done firing off worker threads");

		try
		{
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException e)
		{
			logger.error("executor service exception on awaitTermination() " + e);
		}

		return userCount.get();
	}

	private class LdapSearchCounterWorkerThread implements Runnable
	{
		private String rootDn;
		private String searchFilter;

		private LdapSearchCounterWorkerThread(String rootDn, String searchFilter)
		{
			this.rootDn = rootDn;
			this.searchFilter = searchFilter;
		}

		@Override
		public void run()
		{
			if(logger.isTraceEnabled())
			{
				logger.trace(Thread.currentThread().getName() + " Start ");
			}

			processCommand();

			if(logger.isTraceEnabled())
			{
				logger.trace(Thread.currentThread().getName() + " End ");
			}
		}

		private void processCommand()
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

	private Map<String, Attributes> queryResults;
	public Map<String, Attributes> getEntries(String rootDn, String searchAttribute) throws NamingException
	{
		queryResults = Maps.newConcurrentMap();

		ExecutorService executorService = Executors.newFixedThreadPool(50);

		for(int index=0; index< extendedAlphabet.length-1; index++)
		{
			for (int index2 = 0; index2 < lessExtendedAlphabet.length - 1; index2++)
			{
				for (int index3 = 0; index3 < lessExtendedAlphabet.length - 1; index3++)
				{
					String searchValue = "" + extendedAlphabet[index] + lessExtendedAlphabet[index2] + extendedAlphabet[index3];
					String searchFilter = searchAttribute + "=" + searchValue + "*";

					if (searchExclude.contains(searchValue))
					{
						continue;
					}

					Runnable worker = new LdapQueryWorkerThread(rootDn, searchFilter);

					executorService.execute(worker);
				}
			}
		}

		executorService.shutdown();

		logger.info("done firing off worker threads");

		try
		{
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException e)
		{
			logger.error("executor service exception on awaitTermination() " + e);
		}

		return queryResults;
	}

	private class LdapQueryWorkerThread implements Runnable
	{
		private String rootDn;
		private String searchFilter;

		private LdapQueryWorkerThread(String rootDn, String searchFilter)
		{
			this.rootDn = rootDn;
			this.searchFilter = searchFilter;
		}

		@Override
		public void run()
		{
			if(logger.isTraceEnabled())
			{
				logger.trace(Thread.currentThread().getName() + " Start ");
			}

			processCommand();

			if(logger.isTraceEnabled())
			{
				logger.trace(Thread.currentThread().getName() + " End ");
			}
		}

		private void processCommand()
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

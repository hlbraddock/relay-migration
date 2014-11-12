package org.cru.migration.service.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExecutionService
{
	private Logger logger = LoggerFactory.getLogger(getClass());

	public void execute(ExecuteAction action, Object object, Integer threadCount) throws NamingException
	{
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

		action.execute(executorService, object);

		executorService.shutdown();

		try
		{
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException e)
		{
			logger.error("executor service exception on awaitTermination() " + e);
		}
	}
}

package org.cru.migration.service.execution;

import java.util.concurrent.ExecutorService;

public interface ExecuteAction
{
	public void execute(ExecutorService executorService, Object object);
}

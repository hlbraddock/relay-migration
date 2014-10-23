package org.cru.migration.service.execution;

import java.util.concurrent.ExecutorService;

public interface Action
{
	public void execute(ExecutorService executorService);
}

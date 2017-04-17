package org.cru.migration.thekey;

import org.ccci.idm.user.UserManager;
import org.ccci.idm.user.dao.UserDao;
import org.ccci.idm.user.ldaptive.dao.io.GroupValueTranscoder;
import org.ldaptive.pool.PooledConnectionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TheKeyBeans
{
	// TODO make sure these beans aren't being instantiated on each call the application context

	static private ApplicationContext applicationContext =
			new ClassPathXmlApplicationContext("classpath*:spring/*.xml");

	public static UserManager getUserManager()
	{
		return (UserManager) applicationContext.getBean("userManager");
	}

	public static GroupValueTranscoder getGroupValueTranscoder()
	{
		return (GroupValueTranscoder) applicationContext.getBean("groupValueTranscoder");
	}

	public static PooledConnectionFactory getPooledConnectionFactory()
	{
		return (PooledConnectionFactory) applicationContext.getBean("ldap.connection.factory.pooled.management");
	}
}

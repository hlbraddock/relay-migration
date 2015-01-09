package org.cru.migration.thekey;

import org.ccci.idm.user.UserManager;
import org.ccci.idm.user.dao.UserDao;
import org.ccci.idm.user.ldaptive.dao.io.GroupValueTranscoder;
import org.ccci.idm.user.migration.MigrationUserDao;
import org.ccci.idm.user.migration.MigrationUserManager;
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

	public static MigrationUserManager getUserManagerMerge()
	{
		return (MigrationUserManager) applicationContext.getBean("userManagerMerge");
	}

	public static MigrationUserDao getUserDaoMerge()
	{
		return (MigrationUserDao) applicationContext.getBean("ldap.userDao.merge");
	}

	public static UserDao getUserDaoCopy()
	{
		return (UserDao) applicationContext.getBean("ldap.userDao.copy");
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

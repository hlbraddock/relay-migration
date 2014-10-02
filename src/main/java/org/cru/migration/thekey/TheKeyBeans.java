package org.cru.migration.thekey;

import me.thekey.cas.service.UserManager;
import me.thekey.cas.util.Base64RandomStringGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.ldap.core.LdapTemplate;

public class TheKeyBeans
{
	// TODO make these beans aren't being instantiated on each call the application context

	static private ApplicationContext applicationContext =
			new ClassPathXmlApplicationContext("classpath*:spring/*.xml");

	public static UserManager getUserManager()
	{
		return (UserManager) applicationContext.getBean("service.gcxUserService");
	}

	public static UserManager getUserManagerMerge()
	{
		return (UserManager) applicationContext.getBean("service.gcxUserService.merge");
	}

	public static Base64RandomStringGenerator getRandomStringGenerator()
	{
		return (Base64RandomStringGenerator) applicationContext.getBean("base64RandomStringGenerator");
	}

	public static LdapTemplate getLdapTemplate()
	{
		return (LdapTemplate) applicationContext.getBean("ldap.template");
	}
}

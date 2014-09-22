package org.cru.migration.thekey;

import me.thekey.cas.service.UserManager;
import me.thekey.cas.util.Base64RandomStringGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TheKeyBeans
{
	static private ApplicationContext applicationContext =
			new ClassPathXmlApplicationContext("classpath*:spring/*.xml");

	public static UserManager getUserManager()
	{
		return (UserManager) applicationContext.getBean("service.gcxUserService");
	}

	public static Base64RandomStringGenerator getRandomStringGenerator()
	{
		return (Base64RandomStringGenerator) applicationContext.getBean("base64RandomStringGenerator");
	}
}

package org.cru.migration.thekey;

import me.thekey.cas.service.UserManager;
import org.cru.migration.support.Output;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TheKeyBeans
{
	public static UserManager getUserManager()
	{
		Output.println("Getting spring application context");
		ApplicationContext context =
				new ClassPathXmlApplicationContext(
						"classpath*:spring/*.xml");

		Output.println("Getting spring bean user manager");
		UserManager userManager = (UserManager) context.getBean("service.gcxUserService");

		return userManager;
	}
}

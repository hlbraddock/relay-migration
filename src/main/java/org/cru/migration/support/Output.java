package org.cru.migration.support;

import org.ccci.idm.obj.IdentityUser;

public class Output
{
	public static void println(String string)
	{
		System.out.println(string);
	}

	public static void print(IdentityUser identityUser)
	{
		System.out.println("ssoguid:" + identityUser.getAccount().getSsoguid());
		System.out.println("cn:" + identityUser.getAccount().getCn());
		System.out.println("comment:" + identityUser.getAccount().getComment());
		System.out.println("hashed pwd:" + identityUser.getAccount().getHashedPwd());
		System.out.println("last login:" + identityUser.getAccount().getLastLogonTimestamp());
		System.out.println("pwd last set:" + identityUser.getAccount().getPwdLastSet());
		System.out.println("username:" + identityUser.getAccount().getUsername());
		System.out.println("password:" + identityUser.getAccount().getPassword());
	}
}

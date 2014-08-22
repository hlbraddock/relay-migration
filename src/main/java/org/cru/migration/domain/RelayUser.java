package org.cru.migration.domain;

import org.joda.time.DateTime;

public class RelayUser
{
	private String username;
	private String password;
	private DateTime lastLogonTimestamp;


	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public DateTime getLastLogonTimestamp()
	{
		return lastLogonTimestamp;
	}

	public void setLastLogonTimestamp(DateTime lastLogonTimestamp)
	{
		this.lastLogonTimestamp = lastLogonTimestamp;
	}

	@Override
	public String toString()
	{
		return "RelayUser{" +
				"username='" + username + '\'' +
				", password='" + password + '\'' +
				", lastLogonTimestamp=" + lastLogonTimestamp +
				'}';
	}
}

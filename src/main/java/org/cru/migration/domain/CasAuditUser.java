package org.cru.migration.domain;

import org.joda.time.DateTime;

public class CasAuditUser
{
	private String username;
	private DateTime date;

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public DateTime getDate()
	{
		return date;
	}

	public void setDate(DateTime date)
	{
		this.date = date;
	}

	@Override
	public String toString()
	{
		return "CasAuditUser{" +
				"username='" + username + '\'' +
				", date=" + date +
				'}';
	}
}

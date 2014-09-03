package org.cru.migration.domain;

import com.google.common.collect.Sets;
import org.joda.time.DateTime;

import java.util.Set;

public class RelayUser
{
	private String username;
	private String password;
	private String employeeId;
	private DateTime lastLogonTimestamp;
	private String ssoguid;

	public RelayUser()
	{
	}

	public RelayUser(String username, String password, String employeeId, String ssoguid, DateTime lastLogonTimestamp)
	{
		this.username = username;
		this.password = password;
		this.employeeId = employeeId;
		this.ssoguid = ssoguid;
		this.lastLogonTimestamp = lastLogonTimestamp;
	}

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

	public String getSsoguid()
	{
		return ssoguid;
	}

	public void setSsoguid(String ssoguid)
	{
		this.ssoguid = ssoguid;
	}

	public String getEmployeeId()
	{
		return employeeId;
	}

	public void setEmployeeId(String employeeId)
	{
		this.employeeId = employeeId;
	}

	/**
	 * Convenience method(s)
	 */
	public static Set<String> getSsoguid(Set<RelayUser> relayUsers)
	{
		Set<String> set = Sets.newHashSet();

		for(RelayUser relayUser : relayUsers)
		{
			set.add(relayUser.getSsoguid());
		}

		return set;
	}

	@Override
	public String toString()
	{
		System.out.println("relay user toString");
		return "RelayUser{" +
				"username='" + username + '\'' +
				", password='" + password + '\'' +
				", lastLogonTimestamp=" + lastLogonTimestamp +
				", ssoguid='" + ssoguid + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RelayUser relayUser = (RelayUser) o;

		if (username != null ? !username.equals(relayUser.username) : relayUser.username != null) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return username != null ? username.hashCode() : 0;
	}
}

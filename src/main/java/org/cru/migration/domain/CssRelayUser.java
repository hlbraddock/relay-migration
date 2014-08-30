package org.cru.migration.domain;

import org.joda.time.DateTime;

public class CssRelayUser
{
	private String ssoguid;
	private String username;
	private String password;
	private DateTime lastChanged;

	public CssRelayUser()
	{
	}

	public String getSsoguid()
	{
		return ssoguid;
	}

	public void setSsoguid(String ssoguid)
	{
		this.ssoguid = ssoguid;
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

	public DateTime getLastChanged()
	{
		return lastChanged;
	}

	public void setLastChanged(DateTime lastChanged)
	{
		this.lastChanged = lastChanged;
	}

	@Override
	public String toString()
	{
		return "CssRelayUser{" +
				"ssoguid='" + ssoguid + '\'' +
				", username='" + username + '\'' +
				", password='" + password + '\'' +
				", lastChanged=" + lastChanged +
				'}';
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		CssRelayUser relayUser = (CssRelayUser) o;

		if (username != null ? !username.equals(relayUser.username) : relayUser.username != null) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return username != null ? username.hashCode() : 0;
	}
}

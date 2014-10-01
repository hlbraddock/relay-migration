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
		this.ssoguid = ssoguid.toUpperCase();
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username.toLowerCase();
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
				", lastChanged=" + lastChanged +
				'}';
	}


	public String toString(Boolean secure)
	{
		return "CssRelayUser{" +
				"ssoguid='" + ssoguid + '\'' +
				", username='" + username + '\'' +
				", lastChanged=" + lastChanged +
				(secure ? ", password=" + password : "") +
				'}';
	}


	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		CssRelayUser relayUser = (CssRelayUser) o;

		if (ssoguid != null ? !ssoguid.equals(relayUser.ssoguid) : relayUser.ssoguid != null) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return ssoguid != null ? ssoguid.hashCode() : 0;
	}
}

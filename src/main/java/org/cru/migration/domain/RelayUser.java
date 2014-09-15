package org.cru.migration.domain;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;

import java.util.NoSuchElementException;
import java.util.Set;

public class RelayUser
{
	private String username;
	private String password;
	private String employeeId;
	private DateTime lastLogonTimestamp;
	private String ssoguid;
	private String first;
	private String last;

	public RelayUser()
	{
	}

	public RelayUser(String username, String password, String employeeId, String ssoguid, DateTime lastLogonTimestamp)
	{
		this.username = username.toLowerCase();
		this.password = password;
		this.employeeId = employeeId.toUpperCase();
		this.ssoguid = ssoguid.toUpperCase();
		this.lastLogonTimestamp = lastLogonTimestamp;
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
		this.ssoguid = ssoguid.toUpperCase();
	}

	public String getEmployeeId()
	{
		return employeeId;
	}

	public void setEmployeeId(String employeeId)
	{
		this.employeeId = employeeId.toUpperCase();
	}

	public String getFirst()
	{
		return first;
	}

	public void setFirst(String first)
	{
		this.first = first;
	}

	public String getLast()
	{
		return last;
	}

	public void setLast(String last)
	{
		this.last = last;
	}

	/**
	 * Convenience method(s)
	 */
	public static Set<String> getSsoguids(Set<RelayUser> relayUsers)
	{
		Set<String> set = Sets.newHashSet();

		for(RelayUser relayUser : relayUsers)
		{
			set.add(relayUser.getSsoguid());
		}

		return set;
	}

	public static Set<RelayUser> getRelayUsersHavingEmployeeId(final Set<RelayUser> relayUsers)
	{
		Iterable<RelayUser> filtered = Iterables.filter(relayUsers, new Predicate<RelayUser>()
		{
			@Override
			public boolean apply(RelayUser relayUser)
			{
				return !Strings.isNullOrEmpty(relayUser.getEmployeeId());
			}
		});

		return Sets.newHashSet(filtered);
	}

	public static Set<RelayUser> getRelayUsersNotHavingEmployeeId(final Set<RelayUser> relayUsers)
	{
		Iterable<RelayUser> filtered = Iterables.filter(relayUsers, new Predicate<RelayUser>()
		{
			@Override
			public boolean apply(RelayUser relayUser)
			{
				return Strings.isNullOrEmpty(relayUser.getEmployeeId());
			}
		});

		return Sets.newHashSet(filtered);
	}

	public static RelayUser havingSsoguid(Set<RelayUser> relayUsers, final String ssoguid)
	{
		try
		{
			return Iterables.find(relayUsers, new Predicate<RelayUser>()
			{
				public boolean apply(RelayUser relayUser)
				{
					return relayUser.getSsoguid().equals(ssoguid);
				}
			});
		}
		catch(NoSuchElementException e)
		{
			return null;
		}
	}

	public static RelayUser getRelayUserHavingEmployeeId(Set<RelayUser> relayUsers, final String element)
	{
		try
		{
			return Iterables.find(relayUsers, new Predicate<RelayUser>()
			{
				public boolean apply(RelayUser relayUser)
				{
					return relayUser.getEmployeeId().equals(element);
				}
			});
		}
		catch(NoSuchElementException e)
		{
			return null;
		}
	}

	@Override
	public String toString()
	{
		return "RelayUser{" +
				"username='" + username + '\'' +
				", password='" + password + '\'' +
				", employeeId='" + employeeId + '\'' +
				", lastLogonTimestamp=" + lastLogonTimestamp +
				", ssoguid='" + ssoguid + '\'' +
				", first='" + first + '\'' +
				", last='" + last + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RelayUser relayUser = (RelayUser) o;

		if (ssoguid != null ? !ssoguid.equals(relayUser.ssoguid) : relayUser.ssoguid != null) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		int result = ssoguid != null ? ssoguid.hashCode() : 0;
		return result;
	}
}

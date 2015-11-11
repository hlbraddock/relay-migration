package org.cru.migration.domain;

import com.google.common.collect.Sets;
import org.ccci.idm.user.User;

import java.util.Set;

public class MatchingUsers
{
	private User userByGuid;
	private User userByEmail;
	private User userByLinked;
	private User userByProxy;

	public Set<User> toSet()
	{
		Set<User> users = Sets.newHashSet();

		if(userByGuid != null)
		{
			users.add(userByGuid);
		}

		if(userByEmail != null)
		{
			users.add(userByEmail);
		}

		if(userByLinked != null)
		{
			users.add(userByLinked);
		}

		if(userByProxy != null)
		{
			users.add(userByProxy);
		}

		return users;
	}

	public User getUserByGuid()
	{
		return userByGuid;
	}

	public void setUserByGuid(User userByGuid)
	{
		this.userByGuid = userByGuid;
	}

	public User getUserByEmail()
	{
		return userByEmail;
	}

	public void setUserByEmail(User userByEmail)
	{
		this.userByEmail = userByEmail;
	}

	public User getUserByLinked()
	{
		return userByLinked;
	}

	public void setUserByLinked(User userByLinked)
	{
		this.userByLinked = userByLinked;
	}

	public User getUserByProxy() {
		return userByProxy;
	}

	public void setUserByProxy(final User userByProxy) {
		this.userByProxy = userByProxy;
	}
}

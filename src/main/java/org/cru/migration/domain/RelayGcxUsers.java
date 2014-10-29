package org.cru.migration.domain;

import org.ccci.idm.user.User;
import org.cru.migration.thekey.GcxUserService;

import java.util.Set;

public class RelayGcxUsers
{
	private RelayUser relayUser;
	private User gcxUser;
	private Set<User> gcxUsers;
	private GcxUserService.MatchResult matchResult;
	private Exception exception;

	public RelayGcxUsers(RelayUser relayUser, Set<User> gcxUsers, Exception exception)
	{
		this.relayUser = relayUser;
		this.gcxUsers = gcxUsers;
		this.exception = exception;
	}

	public RelayGcxUsers(RelayUser relayUser, User gcxUser, Set<User> gcxUsers, GcxUserService.MatchResult
			matchResult)
	{
		this.relayUser = relayUser;
		this.gcxUser = gcxUser;
		this.gcxUsers = gcxUsers;
		this.matchResult = matchResult;
	}

	public RelayUser getRelayUser()
	{
		return relayUser;
	}

	public Set<User> getGcxUsers()
	{
		return gcxUsers;
	}

	public Exception getException()
	{
		return exception;
	}

	public User getGcxUser()
	{
		return gcxUser;
	}

	public GcxUserService.MatchResult getMatchResult()
	{
		return matchResult;
	}
}

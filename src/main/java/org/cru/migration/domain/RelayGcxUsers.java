package org.cru.migration.domain;

import org.ccci.gcx.idm.core.model.impl.GcxUser;
import org.cru.migration.thekey.GcxUserService;

import java.util.Set;

public class RelayGcxUsers
{
	private RelayUser relayUser;
	private GcxUser gcxUser;
	private Set<GcxUser> gcxUsers;
	private GcxUserService.MatchResult matchResult;
	private Exception exception;

	public RelayGcxUsers(RelayUser relayUser, Set<GcxUser> gcxUsers, Exception exception)
	{
		this.relayUser = relayUser;
		this.gcxUsers = gcxUsers;
		this.exception = exception;
	}

	public RelayGcxUsers(RelayUser relayUser, GcxUser gcxUser, Set<GcxUser> gcxUsers, GcxUserService.MatchResult
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

	public Set<GcxUser> getGcxUsers()
	{
		return gcxUsers;
	}

	public Exception getException()
	{
		return exception;
	}

	public GcxUser getGcxUser()
	{
		return gcxUser;
	}

	public GcxUserService.MatchResult getMatchResult()
	{
		return matchResult;
	}
}

package org.cru.migration.domain;

import org.ccci.gcx.idm.core.model.impl.GcxUser;

import java.util.Set;

public class RelayGcxUsers
{
	private RelayUser relayUser;
	private Set<GcxUser> gcxUsers;
	private Exception exception;

	public RelayGcxUsers(RelayUser relayUser, Set<GcxUser> gcxUsers, Exception exception)
	{
		this.relayUser = relayUser;
		this.gcxUsers = gcxUsers;
		this.exception = exception;
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
}

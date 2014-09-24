package org.cru.migration.domain;

import java.util.Set;

public class RelayUsersLoggedInStatus
{
	private Set<RelayUser> relayUsersLoggedIn;
	private Set<RelayUser> relayUsersNotLoggedIn;

	public RelayUsersLoggedInStatus(Set<RelayUser> relayUsersLoggedIn, Set<RelayUser> relayUsersNotLoggedIn)
	{
		this.relayUsersLoggedIn = relayUsersLoggedIn;
		this.relayUsersNotLoggedIn = relayUsersNotLoggedIn;
	}

	public Set<RelayUser> getRelayUsersLoggedIn()
	{
		return relayUsersLoggedIn;
	}

	public void setRelayUsersLoggedIn(Set<RelayUser> relayUsersLoggedIn)
	{
		this.relayUsersLoggedIn = relayUsersLoggedIn;
	}

	public Set<RelayUser> getRelayUsersNotLoggedIn()
	{
		return relayUsersNotLoggedIn;
	}

	public void setRelayUsersNotLoggedIn(Set<RelayUser> relayUsersNotLoggedIn)
	{
		this.relayUsersNotLoggedIn = relayUsersNotLoggedIn;
	}
}

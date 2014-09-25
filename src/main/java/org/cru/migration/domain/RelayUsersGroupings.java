package org.cru.migration.domain;

import com.google.common.collect.Sets;

import java.util.Set;

public class RelayUsersGroupings
{
	private Set<RelayUser> relayUsersStaff = Sets.newHashSet();
	private Set<RelayUser> relayUsersGoogle = Sets.newHashSet();

	private Set<RelayUser> relayUsersLoggedIn;
	private Set<RelayUser> relayUsersNotLoggedIn;

	private Set<RelayUser> relayUsersWithPassword;
	private Set<RelayUser> relayUsersWithoutPassword;

	private Set<RelayUser> relayUsersAuthoritative = Sets.newHashSet();

	public Set<RelayUser> getRelayUsersStaff()
	{
		return relayUsersStaff;
	}

	public void setRelayUsersStaff(Set<RelayUser> relayUsersStaff)
	{
		this.relayUsersStaff = relayUsersStaff;

		relayUsersAuthoritative.clear();
		relayUsersAuthoritative.addAll(relayUsersStaff);
		relayUsersAuthoritative.addAll(relayUsersGoogle);
	}

	public Set<RelayUser> getRelayUsersGoogle()
	{
		return relayUsersGoogle;
	}

	public void setRelayUsersGoogle(Set<RelayUser> relayUsersGoogle)
	{
		this.relayUsersGoogle = relayUsersGoogle;

		relayUsersAuthoritative.clear();
		relayUsersAuthoritative.addAll(relayUsersStaff);
		relayUsersAuthoritative.addAll(relayUsersGoogle);
	}

	public Set<RelayUser> getRelayUsersAuthoritative()
	{
		return relayUsersAuthoritative;
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

	public Set<RelayUser> getRelayUsersWithPassword()
	{
		return relayUsersWithPassword;
	}

	public void setRelayUsersWithPassword(Set<RelayUser> relayUsersWithPassword)
	{
		this.relayUsersWithPassword = relayUsersWithPassword;
	}

	public Set<RelayUser> getRelayUsersWithoutPassword()
	{
		return relayUsersWithoutPassword;
	}

	public void setRelayUsersWithoutPassword(Set<RelayUser> relayUsersWithoutPassword)
	{
		this.relayUsersWithoutPassword = relayUsersWithoutPassword;
	}
}

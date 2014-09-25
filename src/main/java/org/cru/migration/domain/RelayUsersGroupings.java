package org.cru.migration.domain;

import com.google.common.collect.Sets;
import org.joda.time.DateTime;

import java.util.Set;

public class RelayUsersGroupings
{
	private Set<RelayUser> relayUsersStaff = Sets.newHashSet();
	private Set<RelayUser> relayUsersGoogle = Sets.newHashSet();

	private Set<RelayUser> relayUsersLoggedIn;
	private Set<RelayUser> relayUsersNotLoggedIn;

	private Set<RelayUser> relayUsersWithPassword;
	private Set<RelayUser> relayUsersWithoutPassword;

	private Set<RelayUser> usStaffInGoogle = Sets.newHashSet();
	private Set<RelayUser> usStaffNotInGoogle = Sets.newHashSet();

	private Set<RelayUser> googleUserUSStaff = Sets.newHashSet();
	private Set<RelayUser> googleUserNotUSStaff = Sets.newHashSet();

	private Set<RelayUser> relayUsersAuthoritative = Sets.newHashSet();

	private Set<RelayUser> googleUserNotUSStaffHavingEmployeeId = Sets.newHashSet();
	private Set<RelayUser> googleUserNotUSStaffNotHavingEmployeeId = Sets.newHashSet();

	private DateTime loggedInSince;

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

	public Set<RelayUser> getUsStaffInGoogle()
	{
		return usStaffInGoogle;
	}

	public void setUsStaffInGoogle(Set<RelayUser> usStaffInGoogle)
	{
		this.usStaffInGoogle = usStaffInGoogle;
	}

	public Set<RelayUser> getUsStaffNotInGoogle()
	{
		return usStaffNotInGoogle;
	}

	public void setUsStaffNotInGoogle(Set<RelayUser> usStaffNotInGoogle)
	{
		this.usStaffNotInGoogle = usStaffNotInGoogle;
	}

	public Set<RelayUser> getGoogleUserUSStaff()
	{
		return googleUserUSStaff;
	}

	public void setGoogleUserUSStaff(Set<RelayUser> googleUserUSStaff)
	{
		this.googleUserUSStaff = googleUserUSStaff;
	}

	public Set<RelayUser> getGoogleUserNotUSStaff()
	{
		return googleUserNotUSStaff;
	}

	public void setGoogleUserNotUSStaff(Set<RelayUser> googleUserNotUSStaff)
	{
		this.googleUserNotUSStaff = googleUserNotUSStaff;
	}

	public Set<RelayUser> getGoogleUserNotUSStaffHavingEmployeeId()
	{
		return googleUserNotUSStaffHavingEmployeeId;
	}

	public void setGoogleUserNotUSStaffHavingEmployeeId(Set<RelayUser> googleUserNotUSStaffHavingEmployeeId)
	{
		this.googleUserNotUSStaffHavingEmployeeId = googleUserNotUSStaffHavingEmployeeId;
	}

	public Set<RelayUser> getGoogleUserNotUSStaffNotHavingEmployeeId()
	{
		return googleUserNotUSStaffNotHavingEmployeeId;
	}

	public void setGoogleUserNotUSStaffNotHavingEmployeeId(Set<RelayUser> googleUserNotUSStaffNotHavingEmployeeId)
	{
		this.googleUserNotUSStaffNotHavingEmployeeId = googleUserNotUSStaffNotHavingEmployeeId;
	}

	public DateTime getLoggedInSince()
	{
		return loggedInSince;
	}

	public void setLoggedInSince(DateTime loggedInSince)
	{
		this.loggedInSince = loggedInSince;
	}
}

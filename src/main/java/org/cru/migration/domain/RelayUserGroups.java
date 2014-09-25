package org.cru.migration.domain;

import com.google.common.collect.Sets;
import org.joda.time.DateTime;

import java.util.Set;

public class RelayUserGroups
{
	private Set<RelayUser> usStaff = Sets.newHashSet();
	private Set<RelayUser> googleUsers = Sets.newHashSet();

	private Set<RelayUser> loggedIn;
	private Set<RelayUser> notLoggedIn;

	private Set<RelayUser> withPassword;
	private Set<RelayUser> withoutPassword;

	private Set<RelayUser> usStaffInGoogle = Sets.newHashSet();
	private Set<RelayUser> usStaffNotInGoogle = Sets.newHashSet();

	private Set<RelayUser> googleUserUSStaff = Sets.newHashSet();
	private Set<RelayUser> googleUserNotUSStaff = Sets.newHashSet();

	private Set<RelayUser> authoritative = Sets.newHashSet();

	private Set<RelayUser> googleUsersNotUSStaffHavingEmployeeId = Sets.newHashSet();
	private Set<RelayUser> googleUsersNotUSStaffNotHavingEmployeeId = Sets.newHashSet();

	private Set<RelayUser> notFoundInCasAuditLog = Sets.newHashSet();

	private DateTime loggedInSince;

	public Set<RelayUser> getUsStaff()
	{
		return usStaff;
	}

	public void setUsStaff(Set<RelayUser> usStaff)
	{
		this.usStaff = usStaff;

		authoritative.clear();
		authoritative.addAll(this.usStaff);
		authoritative.addAll(googleUsers);
	}

	public Set<RelayUser> getGoogleUsers()
	{
		return googleUsers;
	}

	public void setGoogleUsers(Set<RelayUser> googleUsers)
	{
		this.googleUsers = googleUsers;

		authoritative.clear();
		authoritative.addAll(usStaff);
		authoritative.addAll(this.googleUsers);
	}

	public Set<RelayUser> getAuthoritative()
	{
		return authoritative;
	}

	public Set<RelayUser> getLoggedIn()
	{
		return loggedIn;
	}

	public void setLoggedIn(Set<RelayUser> loggedIn)
	{
		this.loggedIn = loggedIn;
	}

	public Set<RelayUser> getNotLoggedIn()
	{
		return notLoggedIn;
	}

	public void setNotLoggedIn(Set<RelayUser> notLoggedIn)
	{
		this.notLoggedIn = notLoggedIn;
	}

	public Set<RelayUser> getWithPassword()
	{
		return withPassword;
	}

	public void setWithPassword(Set<RelayUser> withPassword)
	{
		this.withPassword = withPassword;
	}

	public Set<RelayUser> getWithoutPassword()
	{
		return withoutPassword;
	}

	public void setWithoutPassword(Set<RelayUser> withoutPassword)
	{
		this.withoutPassword = withoutPassword;
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

	public Set<RelayUser> getGoogleUsersNotUSStaffHavingEmployeeId()
	{
		return googleUsersNotUSStaffHavingEmployeeId;
	}

	public void setGoogleUsersNotUSStaffHavingEmployeeId(Set<RelayUser> googleUsersNotUSStaffHavingEmployeeId)
	{
		this.googleUsersNotUSStaffHavingEmployeeId = googleUsersNotUSStaffHavingEmployeeId;
	}

	public Set<RelayUser> getGoogleUsersNotUSStaffNotHavingEmployeeId()
	{
		return googleUsersNotUSStaffNotHavingEmployeeId;
	}

	public void setGoogleUsersNotUSStaffNotHavingEmployeeId(Set<RelayUser> googleUsersNotUSStaffNotHavingEmployeeId)
	{
		this.googleUsersNotUSStaffNotHavingEmployeeId = googleUsersNotUSStaffNotHavingEmployeeId;
	}

	public DateTime getLoggedInSince()
	{
		return loggedInSince;
	}

	public void setLoggedInSince(DateTime loggedInSince)
	{
		this.loggedInSince = loggedInSince;
	}

	public Set<RelayUser> getNotFoundInCasAuditLog()
	{
		return notFoundInCasAuditLog;
	}

	public void setNotFoundInCasAuditLog(Set<RelayUser> notFoundInCasAuditLog)
	{
		this.notFoundInCasAuditLog = notFoundInCasAuditLog;
	}
}
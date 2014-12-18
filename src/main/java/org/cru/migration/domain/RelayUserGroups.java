package org.cru.migration.domain;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.ccci.idm.user.User;
import org.cru.migration.support.MigrationProperties;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RelayUserGroups
{
	MigrationProperties migrationProperties = new MigrationProperties();

	Boolean provisionUSStaff = Boolean.valueOf(migrationProperties.getNonNullProperty("provisionUSStaff"));
	Boolean provisionGoogleUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("provisionGoogleUsers"));
	Boolean provisionNonUSStaff = Boolean.valueOf(migrationProperties.getNonNullProperty("provisionNonUSStaff"));

    private Boolean updateAllUsers = true;

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

	private Set<RelayUser> staffAndGoogleUsers = Sets.newHashSet();

	private Set<RelayUser> allRelayUsers = Sets.newHashSet();

	private Set<RelayUser> allUsers = Sets.newConcurrentHashSet();

	private Set<RelayUser> nonStaffUsers = Sets.newHashSet();

	private Set<RelayUser> googleUsersNotUSStaffHavingEmployeeId = Sets.newHashSet();
	private Set<RelayUser> googleUsersNotUSStaffNotHavingEmployeeId = Sets.newHashSet();

	private Set<RelayUser> notFoundInCasAuditLog = Sets.newHashSet();

	private Set<RelayUser> serializedRelayUsers = Sets.newHashSet();

	private Map<User,Set<RelayUser>> multipleRelayUsersMatchingKeyUser = Maps.newHashMap();

	private DateTime loggedInSince;

	public Set<RelayUser> getUsStaff()
	{
		return usStaff;
	}

	public void setUsStaff(Set<RelayUser> usStaff)
	{
		this.usStaff = usStaff;

        updateAllUsers = true;
	}

	public Set<RelayUser> getGoogleUsers()
	{
		return googleUsers;
	}

	public void setGoogleUsers(Set<RelayUser> googleUsers)
	{
		this.googleUsers = googleUsers;

        updateAllUsers = true;
	}

	public Set<RelayUser> getStaffAndGoogleUsers()
	{
		return staffAndGoogleUsers;
	}

	public Set<RelayUser> getNonStaffUsers()
	{
		return nonStaffUsers;
	}

	public void setNonStaffUsers(Set<RelayUser> nonStaffUsers)
	{
		this.nonStaffUsers = nonStaffUsers;

        updateAllUsers = true;
	}

	public Set<RelayUser> getAllUsers()
	{
        if(updateAllUsers)
        {
            allUsers.clear();

            if(provisionUSStaff)
            {
                allUsers.addAll(usStaff);
            }

            if(provisionGoogleUsers)
            {
                allUsers.addAll(googleUsers);
            }

            if(provisionNonUSStaff)
            {
                allUsers.addAll(nonStaffUsers);
            }

            updateAllUsers = false;
        }

        return allUsers;
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

	public Set<RelayUser> getSerializedRelayUsers()
	{
		return serializedRelayUsers;
	}

	public void setSerializedRelayUsers(Set<RelayUser> serializedRelayUsers)
	{
		this.serializedRelayUsers = serializedRelayUsers;
	}

	public Set<RelayUser> getAllRelayUsers()
	{
		return allRelayUsers;
	}

	public void setAllRelayUsers(Set<RelayUser> allRelayUsers)
	{
		this.allRelayUsers = allRelayUsers;
	}

	public Map<User, Set<RelayUser>> getMultipleRelayUsersMatchingKeyUser() {
		return multipleRelayUsersMatchingKeyUser;
	}

	public void setMultipleRelayUsersMatchingKeyUser(Map<User, Set<RelayUser>> multipleRelayUsersMatchingKeyUser) {
		this.multipleRelayUsersMatchingKeyUser = multipleRelayUsersMatchingKeyUser;
	}
}

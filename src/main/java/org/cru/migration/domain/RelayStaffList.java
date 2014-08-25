package org.cru.migration.domain;

import java.util.List;

public class RelayStaffList
{
	List<StaffRelayUser> staffRelayUsers;
	List<PSHRStaff> notFoundInRelay;

	public RelayStaffList(List<StaffRelayUser> staffRelayUsers, List<PSHRStaff> notFoundInRelay)
	{
		this.staffRelayUsers = staffRelayUsers;
		this.notFoundInRelay = notFoundInRelay;
	}

	public List<StaffRelayUser> getStaffRelayUsers()
	{
		return staffRelayUsers;
	}

	public void setStaffRelayUsers(List<StaffRelayUser> staffRelayUsers)
	{
		this.staffRelayUsers = staffRelayUsers;
	}

	public List<PSHRStaff> getNotFoundInRelay()
	{
		return notFoundInRelay;
	}

	public void setNotFoundInRelay(List<PSHRStaff> notFoundInRelay)
	{
		this.notFoundInRelay = notFoundInRelay;
	}
}

package org.cru.migration.domain;

import java.util.List;

public class RelayStaffList
{
	List<RelayStaffUser> relayStaffUsers;
	List<PSHRStaff> notFoundInRelay;

	public RelayStaffList(List<RelayStaffUser> relayStaffUsers, List<PSHRStaff> notFoundInRelay)
	{
		this.relayStaffUsers = relayStaffUsers;
		this.notFoundInRelay = notFoundInRelay;
	}

	public List<RelayStaffUser> getRelayStaffUsers()
	{
		return relayStaffUsers;
	}

	public void setRelayStaffUsers(List<RelayStaffUser> relayStaffUsers)
	{
		this.relayStaffUsers = relayStaffUsers;
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

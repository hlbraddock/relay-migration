package org.cru.migration.domain;

public class RelayStaffUser extends RelayUser
{
	private String employeeId;

	public String getEmployeeId()
	{
		return employeeId;
	}

	public void setEmployeeId(String employeeId)
	{
		this.employeeId = employeeId;
	}

	@Override
	public String toString()
	{
		return super.toString() + "StaffRelayUser{" +
				"employeeId='" + employeeId + '\'' +
				'}';
	}
}

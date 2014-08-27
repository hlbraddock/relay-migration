package org.cru.migration.domain;

public class RelayStaff extends RelayUser
{
	private String employeeId;

	public RelayStaff()
	{
	}

	public RelayStaff(String employeeId)
	{
		this.employeeId = employeeId;
	}

	public RelayStaff(String username, String password, String ssoguid, String employeeId)
	{
		super(username, password, ssoguid);
		this.employeeId = employeeId;
	}

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

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof RelayStaff)) return false;

		RelayStaff that = (RelayStaff) o;

		if (employeeId != null ? !employeeId.equals(that.employeeId) : that.employeeId != null) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return employeeId != null ? employeeId.hashCode() : 0;
	}
}

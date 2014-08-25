package org.cru.migration.domain;

public class PSHRStaff
{
	private String employeeId;
	private String lastName;
	private String firstName;
	private String role;

	public PSHRStaff()
	{
	}

	public PSHRStaff(String employeeId, String role)
	{
		this.employeeId = employeeId;
		this.role = role;
	}

	public String getEmployeeId()
	{
		return employeeId;
	}

	public void setEmployeeId(String employeeId)
	{
		this.employeeId = employeeId;
	}

	public String getLastName()
	{
		return lastName;
	}

	public String getRole()
	{
		return role;
	}

	public void setRole(String role)
	{
		this.role = role;
	}

	public void setLastName(String lastName)
	{

		this.lastName = lastName;
	}

	public String getFirstName()
	{
		return firstName;
	}

	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PSHRStaff that = (PSHRStaff) o;

		if (employeeId != null ? !employeeId.equals(that.employeeId) : that.employeeId != null) return false;
		if (role != null ? !role.equals(that.role) : that.role != null) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		int result = employeeId != null ? employeeId.hashCode() : 0;
		result = 31 * result + (role != null ? role.hashCode() : 0);
		return result;
	}
}

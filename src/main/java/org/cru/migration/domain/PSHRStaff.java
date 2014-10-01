package org.cru.migration.domain;

import org.cru.migration.support.Misc;

public class PSHRStaff
{
	private String employeeId;
	private String lastName;
	private String firstName;

	public PSHRStaff()
	{
	}

	public PSHRStaff(String employeeId, String role)
	{
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

	public String getLastName()
	{
		return lastName;
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

    public String toCvsFormattedString()
    {
        return
                Misc.format(getLastName()) + "," +
                Misc.format(getFirstName()) + "," +
                Misc.format(getEmployeeId());
    }

    @Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PSHRStaff that = (PSHRStaff) o;

		if (employeeId != null ? !employeeId.equals(that.employeeId) : that.employeeId != null) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		int result = employeeId != null ? employeeId.hashCode() : 0;
		return result;
	}
}

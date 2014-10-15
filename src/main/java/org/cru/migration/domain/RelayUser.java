package org.cru.migration.domain;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.ccci.gcx.idm.core.model.impl.GcxUser;
import org.cru.migration.support.Misc;
import org.cru.migration.support.StringUtilities;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class RelayUser
{
	private String username;
	private String password;
	private DateTime lastLogonTimestamp;
	private String ssoguid;
	private String first;
	private String last;

	private String employeeId;
	private String departmentNumber;

    private String city;
    private String state;
    private String postal;
    private String country;

    private String ipPhone;
    private String mobile;
    private String telephone;

	private String cruDesignation;
	private String cruEmployeeStatus;
	private String cruGender;
	private String cruHrStatusCode;
	private String cruJobCode;
	private String cruManagerID;
	private String cruMinistryCode;
	private String cruPayGroup;
	private String cruPreferredName;
	private String cruSubMinistryCode;

	private List<String> proxyAddresses;

	private static Logger logger = LoggerFactory.getLogger(RelayUser.class);

	public RelayUser()
	{
	}

	public RelayUser(String username, String password, String first, String last, String employeeId, String ssoguid,
					 DateTime lastLogonTimestamp)
	{
		this.username = username.toLowerCase();
		this.password = password;
		this.first = first;
		this.last = last;
		this.employeeId = employeeId.toUpperCase();
		this.ssoguid = ssoguid.toUpperCase();
		this.lastLogonTimestamp = lastLogonTimestamp;
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username.toLowerCase();
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public DateTime getLastLogonTimestamp()
	{
		return lastLogonTimestamp;
	}

	public void setLastLogonTimestamp(DateTime lastLogonTimestamp)
	{
		this.lastLogonTimestamp = lastLogonTimestamp;
	}

	public String getSsoguid()
	{
		return ssoguid;
	}

	public void setSsoguid(String ssoguid)
	{
		this.ssoguid = ssoguid.toUpperCase();
	}

	public String getEmployeeId()
	{
		return employeeId;
	}

	public void setEmployeeId(String employeeId)
	{
		this.employeeId = employeeId.toUpperCase();
	}

	public String getFirst()
	{
		return first;
	}

	public void setFirst(String first)
	{
		this.first = first;
	}

	public String getLast()
	{
		return last;
	}

	public void setLast(String last)
	{
		this.last = last;
	}

	/**
	 * Convenience method(s)
	 */

	public GcxUser toGcxUser()
	{
		final GcxUser gcxUser = new GcxUser();

		gcxUser.setEmail(username);
		gcxUser.setPassword(password);
		gcxUser.setFirstName(first);
		gcxUser.setLastName(last);
		gcxUser.setGUID(ssoguid);
		gcxUser.setRelayGuid(ssoguid);

		return gcxUser;
	}

	public Boolean equals(RelayUser relayUser, boolean identical, StringBuffer difference)
	{
		DateTime lastLogon = lastLogonTimestamp != null ? lastLogonTimestamp : Misc.oldDateTime;
		DateTime relayUserLastLogon = relayUser.getLastLogonTimestamp() != null ? relayUser
				.getLastLogonTimestamp() : Misc.oldDateTime;

		Boolean result = identical ? Misc.equals(username, relayUser.getUsername()) &&
				Misc.equals(password, relayUser.getPassword()) &&
				Misc.equals(employeeId, relayUser.getEmployeeId()) &&
				Misc.equals(lastLogon, relayUserLastLogon) &&
				Misc.equals(ssoguid, relayUser.getSsoguid()) &&
				Misc.equals(first, relayUser.getFirst()) &&
				Misc.equals(last, relayUser.getLast())
				: equals(relayUser);

		if(!result)
		{
			if(!Misc.equals(username, relayUser.getUsername()))
				difference.append(toString()).append(" no match username ").append(username).append("," +
				"").append(relayUser.getUsername());

			if(!Misc.equals(password, relayUser.getPassword()))
				difference.append(toString()).append(" no match password ").append(password).append("," +
						"").append(relayUser.getPassword());

			if(!Misc.equals(employeeId, relayUser.getEmployeeId()))
				difference.append(toString()).append(" no match employee id ").append(employeeId).append("," +
						"").append(relayUser.getEmployeeId());

			if(!Misc.equals(lastLogonTimestamp, relayUserLastLogon))
				difference.append(toString()).append(" no match last logon ").append(lastLogon).append("," +
						"").append(relayUserLastLogon);

			if(!Misc.equals(ssoguid, relayUser.getSsoguid()))
				difference.append(toString()).append(" no match ssoguid ").append(ssoguid).append("," +
						"").append(relayUser.getSsoguid());

			if(!Misc.equals(first, relayUser.getFirst()))
				difference.append(toString()).append(" no match first ").append(first).append("," +
						"").append(relayUser.getFirst());

			if(!Misc.equals(last, relayUser.getLast()))
				difference.append(toString()).append(" no match last ").append(last).append("," +
						"").append(relayUser.getLast());

			logger.debug(difference.toString());
		}

		return result;
	}

	public static Set<String> getSsoguids(Set<RelayUser> relayUsers)
	{
		Set<String> set = Sets.newHashSet();

		for(RelayUser relayUser : relayUsers)
		{
			set.add(relayUser.getSsoguid());
		}

		return set;
	}

	public static Set<RelayUser> getRelayUsersHavingEmployeeId(final Set<RelayUser> relayUsers)
	{
		Iterable<RelayUser> filtered = Iterables.filter(relayUsers, new Predicate<RelayUser>()
		{
			@Override
			public boolean apply(RelayUser relayUser)
			{
				return !Strings.isNullOrEmpty(relayUser.getEmployeeId());
			}
		});

		return Sets.newHashSet(filtered);
	}

	public static Set<RelayUser> getRelayUsersNotHavingEmployeeId(final Set<RelayUser> relayUsers)
	{
		Iterable<RelayUser> filtered = Iterables.filter(relayUsers, new Predicate<RelayUser>()
		{
			@Override
			public boolean apply(RelayUser relayUser)
			{
				return Strings.isNullOrEmpty(relayUser.getEmployeeId());
			}
		});

		return Sets.newHashSet(filtered);
	}

	public static RelayUser havingSsoguid(Set<RelayUser> relayUsers, final String ssoguid)
	{
		try
		{
			return Iterables.find(relayUsers, new Predicate<RelayUser>()
			{
				public boolean apply(RelayUser relayUser)
				{
					return relayUser.getSsoguid().equals(ssoguid);
				}
			});
		}
		catch(NoSuchElementException e)
		{
			return null;
		}
	}

	public static RelayUser getRelayUserHavingEmployeeId(Set<RelayUser> relayUsers, final String element)
	{
		try
		{
			return Iterables.find(relayUsers, new Predicate<RelayUser>()
			{
				public boolean apply(RelayUser relayUser)
				{
					return relayUser.getEmployeeId().equals(element);
				}
			});
		}
		catch(NoSuchElementException e)
		{
			return null;
		}
	}

	@Override
	public String toString()
	{
		return "RelayUser{" +
				"username='" + username + '\'' +
				", password='" + password + '\'' +
				", employeeId='" + employeeId + '\'' +
				", lastLogonTimestamp=" + lastLogonTimestamp +
				", ssoguid='" + ssoguid + '\'' +
				", first='" + first + '\'' +
				", last='" + last + '\'' +
				'}';
	}

	public String toCsvFormattedString(Boolean secure)
	{
		return
				Misc.format(last) + "," +
						Misc.format(first) + "," +
						Misc.format(username) + "," +
						Misc.format(employeeId) + "," +
						Misc.format(ssoguid) + "," +
						Misc.format(lastLogonTimestamp) +
						(secure ? "," + Misc.format(password) : "");
	}

	static class FieldType
	{
		public static final int LAST = 0;
		public static final int FIRST = 1;
		public static final int USERNAME = 2;
		public static final int EMPLOYEE_ID = 3;
		public static final int SSOGUID = 4;
		public static final int LAST_LOGIN = 5;
		public static final int PASSWORD = 6;
	}

	public static RelayUser fromCsvFormattedString(String cvsFormattedString)
	{
		RelayUser relayUser = new RelayUser();

		String fields[] = StringUtilities.commaDelimitedListToStringArray(cvsFormattedString, "\\");

		for(Integer indices = 0; fields.length > indices; indices++)
		{
			String field = Misc.getNonNullField(indices, fields);

			field = Misc.unformat(field);

			if(indices == FieldType.LAST)
			{
				relayUser.setLast(field);
			}
			else if(indices == FieldType.FIRST)
			{
				relayUser.setFirst(field);
			}
			else if(indices == FieldType.USERNAME)
			{
				relayUser.setUsername(field);
			}
			else if(indices == FieldType.EMPLOYEE_ID)
			{
				relayUser.setEmployeeId(field);
			}
			else if(indices == FieldType.SSOGUID)
			{
				relayUser.setSsoguid(field);
			}
			else if(indices == FieldType.LAST_LOGIN)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setLastLogonTimestamp(Misc.dateTimeFormatter.parseDateTime(field));
				}
			}
			else if(indices == FieldType.PASSWORD)
			{
				relayUser.setPassword(field);
			}
		}


		return relayUser;
	}

	public String toCsvFormattedString()
	{
		return toCsvFormattedString(false);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RelayUser relayUser = (RelayUser) o;

		if (ssoguid != null ? !ssoguid.equals(relayUser.ssoguid) : relayUser.ssoguid != null) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		int result = ssoguid != null ? ssoguid.hashCode() : 0;
		return result;
	}

	public String getDepartmentNumber()
	{
		return departmentNumber;
	}

	public void setDepartmentNumber(String departmentNumber)
	{
		this.departmentNumber = departmentNumber;
	}

	public String getCruDesignation()
	{
		return cruDesignation;
	}

	public void setCruDesignation(String cruDesignation)
	{
		this.cruDesignation = cruDesignation;
	}

	public String getCruEmployeeStatus()
	{
		return cruEmployeeStatus;
	}

	public void setCruEmployeeStatus(String cruEmployeeStatus)
	{
		this.cruEmployeeStatus = cruEmployeeStatus;
	}

	public String getCruGender()
	{
		return cruGender;
	}

	public void setCruGender(String cruGender)
	{
		this.cruGender = cruGender;
	}

	public String getCruHrStatusCode()
	{
		return cruHrStatusCode;
	}

	public void setCruHrStatusCode(String cruHrStatusCode)
	{
		this.cruHrStatusCode = cruHrStatusCode;
	}

	public String getCruJobCode()
	{
		return cruJobCode;
	}

	public void setCruJobCode(String cruJobCode)
	{
		this.cruJobCode = cruJobCode;
	}

	public String getCruManagerID()
	{
		return cruManagerID;
	}

	public void setCruManagerID(String cruManagerID)
	{
		this.cruManagerID = cruManagerID;
	}

	public String getCruMinistryCode()
	{
		return cruMinistryCode;
	}

	public void setCruMinistryCode(String cruMinistryCode)
	{
		this.cruMinistryCode = cruMinistryCode;
	}

	public String getCruPayGroup()
	{
		return cruPayGroup;
	}

	public void setCruPayGroup(String cruPayGroup)
	{
		this.cruPayGroup = cruPayGroup;
	}

	public String getCruPreferredName()
	{
		return cruPreferredName;
	}

	public void setCruPreferredName(String cruPreferredName)
	{
		this.cruPreferredName = cruPreferredName;
	}

	public String getCruSubMinistryCode()
	{
		return cruSubMinistryCode;
	}

	public void setCruSubMinistryCode(String cruSubMinistryCode)
	{
		this.cruSubMinistryCode = cruSubMinistryCode;
	}

    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public String getPostal()
    {
        return postal;
    }

    public void setPostal(String postal)
    {
        this.postal = postal;
    }

    public String getCountry()
    {
        return country;
    }

    public void setCountry(String country)
    {
        this.country = country;
    }

    public String getIpPhone()
    {
        return ipPhone;
    }

    public void setIpPhone(String ipPhone)
    {
        this.ipPhone = ipPhone;
    }

    public String getMobile()
    {
        return mobile;
    }

    public void setMobile(String mobile)
    {
        this.mobile = mobile;
    }

    public String getTelephone()
    {
        return telephone;
    }

    public void setTelephone(String telephone)
    {
        this.telephone = telephone;
    }

	public List<String> getProxyAddresses()
	{
		return proxyAddresses;
	}

	public void setProxyAddresses(List<String> proxyAddresses)
	{
		this.proxyAddresses = proxyAddresses;
	}
}

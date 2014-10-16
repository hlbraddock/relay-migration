package org.cru.migration.domain;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.ccci.gcx.idm.core.model.impl.GcxUser;
import org.cru.migration.support.Container;
import org.cru.migration.support.Misc;
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
				Misc.equals(last, relayUser.getLast()) &&
				Misc.equals(departmentNumber, relayUser.getDepartmentNumber()) &&
				Misc.equals(city, relayUser.getCity()) &&
				Misc.equals(state, relayUser.getState()) &&
				Misc.equals(postal, relayUser.getPostal()) &&
				Misc.equals(country, relayUser.getCountry()) &&
				Misc.equals(ipPhone, relayUser.getIpPhone()) &&
				Misc.equals(mobile, relayUser.getMobile()) &&
				Misc.equals(cruDesignation, relayUser.getCruDesignation()) &&
				Misc.equals(cruEmployeeStatus, relayUser.getCruEmployeeStatus()) &&
				Misc.equals(cruGender, relayUser.getCruGender()) &&
				Misc.equals(cruHrStatusCode, relayUser.getCruHrStatusCode()) &&
				Misc.equals(cruJobCode, relayUser.getCruJobCode()) &&
				Misc.equals(cruManagerID, relayUser.getCruManagerID()) &&
				Misc.equals(cruMinistryCode, relayUser.getCruMinistryCode()) &&
				Misc.equals(cruPayGroup, relayUser.getCruPayGroup()) &&
				Misc.equals(cruPreferredName, relayUser.getCruPreferredName()) &&
				Misc.equals(cruSubMinistryCode, relayUser.getCruSubMinistryCode()) &&
				Misc.equals(proxyAddresses, relayUser.getProxyAddresses())
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

			if(!Misc.equals(departmentNumber, relayUser.getDepartmentNumber()))
				difference.append(toString()).append(" no match department number ").append(departmentNumber).append
						(",").append(relayUser.getDepartmentNumber());

			if(!Misc.equals(city, relayUser.getCity()))
				difference.append(toString()).append(" no match city ").append(city).append
						(",").append(relayUser.getCity());

			if(!Misc.equals(state, relayUser.getState()))
				difference.append(toString()).append(" no match state ").append(state).append
						(",").append(relayUser.getState());

			if(!Misc.equals(postal, relayUser.getPostal()))
				difference.append(toString()).append(" no match postal ").append(postal).append
						(",").append(relayUser.getPostal());

			if(!Misc.equals(country, relayUser.getCountry()))
				difference.append(toString()).append(" no match country ").append(country).append
						(",").append(relayUser.getCountry());

			if(!Misc.equals(ipPhone, relayUser.getIpPhone()))
				difference.append(toString()).append(" no match ip phone ").append(ipPhone).append
						(",").append(relayUser.getIpPhone());

			if(!Misc.equals(mobile, relayUser.getMobile()))
				difference.append(toString()).append(" no match mobile ").append(mobile).append
						(",").append(relayUser.getMobile());

			if(!Misc.equals(telephone, relayUser.getTelephone()))
				difference.append(toString()).append(" no match telephone ").append(telephone).append
						(",").append(relayUser.getTelephone());

			if(!Misc.equals(cruDesignation, relayUser.getCruDesignation()))
				difference.append(toString()).append(" no match cru designation ").append(cruDesignation).append
						(",").append(relayUser.getCruDesignation());

			if(!Misc.equals(cruEmployeeStatus, relayUser.getCruEmployeeStatus()))
				difference.append(toString()).append(" no match cru employee status ").append(cruEmployeeStatus).append
						(",").append(relayUser.getCruEmployeeStatus());

			if(!Misc.equals(cruGender, relayUser.getCruGender()))
				difference.append(toString()).append(" no match cru gender ").append(cruGender).append
						(",").append(relayUser.getCruGender());

			if(!Misc.equals(cruHrStatusCode, relayUser.getCruHrStatusCode()))
				difference.append(toString()).append(" no match cru hr status code ").append(cruHrStatusCode).append
						(",").append(relayUser.getCruHrStatusCode());

			if(!Misc.equals(cruJobCode, relayUser.getCruJobCode()))
				difference.append(toString()).append(" no match cru job code ").append(cruJobCode).append
						(",").append(relayUser.getCruJobCode());

			if(!Misc.equals(cruManagerID, relayUser.getCruManagerID()))
				difference.append(toString()).append(" no match cru manager id ").append(cruManagerID).append
						(",").append(relayUser.getCruManagerID());

			if(!Misc.equals(cruMinistryCode, relayUser.getCruMinistryCode()))
				difference.append(toString()).append(" no match cru ministry code ").append(cruMinistryCode).append
						(",").append(relayUser.getCruMinistryCode());

			if(!Misc.equals(cruPayGroup, relayUser.getCruPayGroup()))
				difference.append(toString()).append(" no match cru pay group ").append(cruPayGroup).append
						(",").append(relayUser.getCruPayGroup());

			if(!Misc.equals(cruPreferredName, relayUser.getCruPreferredName()))
				difference.append(toString()).append(" no match cru preferred name ").append(cruPreferredName).append
						(",").append(relayUser.getCruPreferredName());

			if(!Misc.equals(cruSubMinistryCode, relayUser.getCruSubMinistryCode()))
				difference.append(toString()).append(" no match cru sub ministry code ").append
						(cruSubMinistryCode).append(",").append(relayUser.getCruSubMinistryCode());

			if(!Container.equals(proxyAddresses, relayUser.getProxyAddresses()))
			{
				for(String string : proxyAddresses)
				{
					logger.debug("1st proxy addresses :" + string + ":");
				}
				for(String string : relayUser.getProxyAddresses())
				{
					logger.debug("2nd proxy addresses :" + string + ":");
				}

				difference.append(toString()).append(" no match proxy addresses ").append
						(proxyAddresses).append(",").append(relayUser.getProxyAddresses());
			}

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

	public List<String> toList()
	{
		List<String> list = Lists.newArrayList();

		list.add(Misc.escape(last));
		list.add(Misc.escape(first));
		list.add(Misc.escape(username));
		list.add(Misc.escape(employeeId));
		list.add(Misc.escape(ssoguid));
		list.add(Misc.format(lastLogonTimestamp));
		list.add(Misc.escape(password));
		list.add(Misc.escape(departmentNumber));
		list.add(Misc.escape(city));
		list.add(Misc.escape(state));
		list.add(Misc.escape(postal));
		list.add(Misc.escape(country));
		list.add(Misc.escape(ipPhone));
		list.add(Misc.escape(mobile));
		list.add(Misc.escape(telephone));
		list.add(Misc.escape(cruDesignation));
		list.add(Misc.escape(cruEmployeeStatus));
		list.add(Misc.escape(cruGender));
		list.add(Misc.escape(cruHrStatusCode));
		list.add(Misc.escape(cruJobCode));
		list.add(Misc.escape(cruManagerID));
		list.add(Misc.escape(cruMinistryCode));
		list.add(Misc.escape(cruPayGroup));
		list.add(Misc.escape(cruPreferredName));
		list.add(Misc.escape(cruSubMinistryCode));

		list.add(proxyAddresses != null ? Misc.escape(StringUtils.join(proxyAddresses.toArray(), ",")) : "");

		return list;
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
		public static final int DEPARTMENT_NUMBER = 7;
		public static final int CITY = 8;
		public static final int STATE = 9;
		public static final int POSTAL = 10;
		public static final int COUNTRY = 11;
		public static final int IP_PHONE = 12;
		public static final int MOBILE = 13;
		public static final int TELEPHONE = 14;
		public static final int CRU_DESIGNATION = 15;
		public static final int CRU_EMPLOYEE_STATUS = 16;
		public static final int CRU_GENDER = 17;
		public static final int CRU_HR_STATUS_CODE = 18;
		public static final int CRU_JOB_CODE = 19;
		public static final int CRU_MANAGER_ID = 20;
		public static final int CRU_MINISTRY_CODE = 21;
		public static final int CRU_PAY_GROUP = 22;
		public static final int CRU_PREFERRED_NAME = 23;
		public static final int CRU_SUB_MINISTRY_CODE = 24;
		public static final int PROXY_ADDRESSES = 25;
	}

	public static RelayUser fromList(List<String> list)
	{
		RelayUser relayUser = new RelayUser();

		for(Integer indices = 0; list.size() > indices; indices++)
		{
			String field = Misc.getNonNullField(indices, list.toArray(new String[0]));

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
			else if(indices == FieldType.DEPARTMENT_NUMBER)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setDepartmentNumber(field);
				}
			}
			else if(indices == FieldType.CITY)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setCity(field);
				}
			}
			else if(indices == FieldType.STATE)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setState(field);
				}
			}
			else if(indices == FieldType.POSTAL)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setPostal(field);
				}
			}
			else if(indices == FieldType.COUNTRY)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setCountry(field);
				}
			}
			else if(indices == FieldType.IP_PHONE)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setIpPhone(field);
				}
			}
			else if(indices == FieldType.IP_PHONE)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setIpPhone(field);
				}
			}
			else if(indices == FieldType.MOBILE)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setMobile(field);
				}
			}
			else if(indices == FieldType.TELEPHONE)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setTelephone(field);
				}
			}
			else if(indices == FieldType.CRU_DESIGNATION)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setCruDesignation(field);
				}
			}
			else if(indices == FieldType.CRU_EMPLOYEE_STATUS)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setCruEmployeeStatus(field);
				}
			}
			else if(indices == FieldType.CRU_GENDER)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setCruGender(field);
				}
			}
			else if(indices == FieldType.CRU_HR_STATUS_CODE)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setCruHrStatusCode(field);
				}
			}
			else if(indices == FieldType.CRU_JOB_CODE)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setCruJobCode(field);
				}
			}
			else if(indices == FieldType.CRU_MANAGER_ID)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setCruManagerID(field);
				}
			}
			else if(indices == FieldType.CRU_MINISTRY_CODE)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setCruMinistryCode(field);
				}
			}
			else if(indices == FieldType.CRU_PAY_GROUP)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setCruPayGroup(field);
				}
			}
			else if(indices == FieldType.CRU_PREFERRED_NAME)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setCruPreferredName(field);
				}
			}
			else if(indices == FieldType.CRU_SUB_MINISTRY_CODE)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setCruSubMinistryCode(field);
				}
			}
			else if(indices == FieldType.PROXY_ADDRESSES)
			{
				if(!Strings.isNullOrEmpty(field))
				{
					relayUser.setProxyAddresses(Lists.newArrayList(Splitter.on(",").split(field)));
				}
			}
		}

		return relayUser;
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

package org.cru.migration.domain;

import org.ccci.idm.ldap.attributes.LdapAttributes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StaffRelayUserMap implements Map<String,String>
{
	private Map<String, String> map;

	public StaffRelayUserMap(LdapAttributes ldapAttributes)
	{
		map = new HashMap<String, String>();

		map.put(ldapAttributes.employeeNumber, "employeeId");
		map.put(ldapAttributes.username, "username");
		map.put(ldapAttributes.password, "password");
		map.put(ldapAttributes.lastLogonTimeStamp, "lastLogonTimestamp");
		map.put(ldapAttributes.commonName, "ssoguid");
	}

	public void clear()
	{
		map.clear();
	}

	public boolean containsKey(Object key)
	{
		return map.containsKey(key);
	}

	public boolean containsValue(Object value)
	{
		return map.containsValue(value);
	}

	public Set<java.util.Map.Entry<String, String>> entrySet()
	{
		return map.entrySet();
	}

	public boolean equals(Object o)
	{
		return map.equals(o);
	}

	public String get(Object key)
	{
		return map.get(key);
	}

	public int hashCode()
	{
		return map.hashCode();
	}

	public boolean isEmpty()
	{
		return map.isEmpty();
	}

	public Set<String> keySet()
	{
		return map.keySet();
	}

	public String put(String key, String value)
	{
		return map.put(key, value);
	}

	public void putAll(Map<? extends String, ? extends String> m)
	{
		map.putAll(m);
	}

	public String remove(Object key)
	{
		return map.remove(key);
	}

	public int size()
	{
		return map.size();
	}

	public Collection<String> values()
	{
		return map.values();
	}
}


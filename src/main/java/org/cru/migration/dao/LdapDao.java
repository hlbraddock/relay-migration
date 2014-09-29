package org.cru.migration.dao;

import org.ccci.idm.ldap.Ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import java.util.List;

public class LdapDao
{
	private Ldap ldap;

	public LdapDao(Ldap ldap)
	{
		this.ldap = ldap;
	}

	public DirContext createStructuralObjectClass
			(String objectClassName, String description) throws NamingException
	{
		DirContext schema = ldap.getContext().getSchema("");

		// Specify attributes for the schema object
		Attributes attributes = new BasicAttributes(true); // Ignore case
		attributes.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.1.1");
		attributes.put("NAME", objectClassName);
		attributes.put("DESC", description);
		attributes.put("SUP", "top");
		attributes.put("STRUCTURAL", "true");
		Attribute must = new BasicAttribute("MUST", "cn");
		must.add("objectclass");
		attributes.put(must);

		// Add the new schema object for the object class
		return schema.createSubcontext(classDefinitionContextName(objectClassName), attributes);
	}

	public void addAttributesToObjectClass(String objectClassName, List<String> attributeNames,
										   String type) throws NamingException
	{
		DirContext schema = ldap.getContext().getSchema("");

		Attributes attributes = new BasicAttributes(false);

		for(String attributeName : attributeNames)
		{
			attributes.put(new BasicAttribute(type, attributeName));
		}

		// Modify schema object
		schema.modifyAttributes(classDefinitionContextName(objectClassName), DirContext.ADD_ATTRIBUTE, attributes);
	}

	public void createAttributes(List<String> attributeNames) throws NamingException
	{
		for(String attributeName : attributeNames)
		{
			DirContext attribute = addAttribute(attributeName);
		}
	}

	public DirContext addAttribute(String attributeName) throws NamingException
	{
		DirContext schema = ldap.getContext().getSchema("");

		// Specify attributes for the schema object
		Attributes attributes = new BasicAttributes(true); // Ignore case
		attributes.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.1.2");
		attributes.put("NAME", attributeName);
		attributes.put("DESC", "for JNDITutorial example only");
		attributes.put("SYNTAX", "1.3.6.1.4.1.1466.115.121.1.15");

		// Add the new schema object
		return schema.createSubcontext(attributeDefinitionContextName(attributeName), attributes);
	}

	private String attributeDefinitionContextName(String name)
	{
		return "AttributeDefinition/" + name;
	}

	private String classDefinitionContextName(String name)
	{
		return "ClassDefinition/" + name;
	}
}

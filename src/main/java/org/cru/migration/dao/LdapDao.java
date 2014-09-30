package org.cru.migration.dao;

import org.ccci.idm.ldap.Ldap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private Logger logger = LoggerFactory.getLogger(getClass());

	public LdapDao(Ldap ldap)
	{
		this.ldap = ldap;
	}

	public DirContext createStructuralObjectClass
			(String className, String description, List<String> requiredAttributes,
			 String numericOid) throws NamingException
	{
		DirContext schema = ldap.getContext().getSchema("");

		// Specify attributes for the schema object
		Attributes attributes = new BasicAttributes(true); // Ignore case
		// TODO generate dynamically
		attributes.put("NUMERICOID", numericOid);
		attributes.put("NAME", className);
		attributes.put("DESC", description);
		attributes.put("SUP", "top");
		attributes.put("STRUCTURAL", "true");

		for(String requiredAttribute : requiredAttributes)
		{
			Attribute attribute = new BasicAttribute("MUST", requiredAttribute);
			logger.info("adding required attribute " + requiredAttribute);
			attributes.put(attribute);
		}

		// Add the new schema object for the object class
		return schema.createSubcontext(classDefinitionContextName(className), attributes);
	}

	/**
	 * WARNING: The server does not seem to honor these requests
	 */
	public void addAttributeToClass(String className, String attributeName, String type) throws NamingException
	{
		DirContext schema = ldap.getContext().getSchema("");

		Attributes attributes = new BasicAttributes(false);

		Attribute attribute = new BasicAttribute(type, attributeName);
		attributes.put(attribute);

		// Modify schema object
		schema.modifyAttributes(classDefinitionContextName(className), DirContext.ADD_ATTRIBUTE, attributes);
	}

	public DirContext createAttribute(String attributeName, String description,
									  String numericOid) throws NamingException
	{
		logger.info("creating attribute " + attributeName);
		DirContext schema = ldap.getContext().getSchema("");

		// Specify attributes for the schema object
		Attributes attributes = new BasicAttributes(true); // Ignore case

		// TODO generate dynamically
		attributes.put("NUMERICOID", numericOid);
		attributes.put("NAME", attributeName);
		attributes.put("DESC", description);
		attributes.put("SYNTAX", "1.3.6.1.4.1.1466.115.121.1.15");

		// Add the new schema object
		return schema.createSubcontext(attributeDefinitionContextName(attributeName), attributes);
	}

	public void deleteClass(String className) throws NamingException
	{
		DirContext schema = ldap.getContext().getSchema("");

		schema.destroySubcontext(classDefinitionContextName(className));
	}

	public void deleteAttribute(String attributeName) throws NamingException
	{
		DirContext schema = ldap.getContext().getSchema("");

		schema.destroySubcontext(attributeDefinitionContextName(attributeName));
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

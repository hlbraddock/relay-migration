package org.cru.migration.dao;

import org.ccci.idm.ldap.Ldap;
import org.cru.migration.service.EntriesService;
import org.cru.migration.service.UserCountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import java.util.List;
import java.util.Map;

public class LdapDao
{
	private Ldap ldap;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public LdapDao(Ldap ldap)
	{
		this.ldap = ldap;
	}

	public enum ObjectClassType
	{
		Auxiliary, Structural
	}

    public void createGroup(String parentDn, String name, String owner) throws NamingException
    {
        Attributes attributes = new BasicAttributes();

        attributes.put(new BasicAttribute("objectClass", "top"));
        attributes.put(new BasicAttribute("objectClass", "groupOfNames"));
        attributes.put(new BasicAttribute("owner", owner));
        attributes.put(new BasicAttribute("ACL", "2#entry#[Root]#member"));

        String dn = "cn=" + name + "," + parentDn;

        ldap.create(dn, attributes);
    }

    public void createOrganizationUnit(String parentDn, String name) throws NamingException
    {
        Attributes attributes = new BasicAttributes();

        String dn = "ou=" + name + "," + parentDn;

        attributes.put(new BasicAttribute("objectClass", "top"));
        attributes.put(new BasicAttribute("objectClass", "organizationalUnit"));
//        attributes.put(new BasicAttribute("objectClass", "ndsLoginProperties"));
//        attributes.put(new BasicAttribute("objectClass", "ndsContainerLoginProperties"));
//        attributes.put(new BasicAttribute("ACL", "2#entry#" + dn + "#loginScript"));
//        attributes.put(new BasicAttribute("ACL", "2#entry#" + dn + "#printJobConfiguration"));

        logger.info(dn);
        ldap.create(dn, attributes);
    }

    public DirContext createObjectClass
			(String className, String description, List<String> requiredAttributes,
			 String numericOid, String superClass, ObjectClassType objectClassType) throws NamingException
	{
		logger.info("creating object " + className + " super class " + superClass);
		DirContext schema = ldap.getContext().getSchema("");

		// Specify attributes for the schema object
		Attributes attributes = new BasicAttributes(true); // Ignore case
		attributes.put("NUMERICOID", numericOid);
		attributes.put("NAME", className);
		attributes.put("DESC", description);
		attributes.put("SUP", superClass);
		attributes.put(objectClassType.toString().toUpperCase(), "true");

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

	public Integer getUserCount(String rootDn, String searchAttribute) throws NamingException
	{
		UserCountService userCountService = new UserCountService(ldap);

		return userCountService.getUserCount(rootDn, searchAttribute);
	}

	public Map<String, Attributes> getEntries(String rootDn, String searchAttribute,
											  String[] returnAttributes, int depth) throws NamingException
	{
		EntriesService entriesService = new EntriesService(ldap);

		return entriesService.getEntries(rootDn, searchAttribute, returnAttributes, depth);
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

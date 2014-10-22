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
import java.util.Arrays;
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

	public DirContext createStructuralObjectClass
			(String className, String description, List<String> requiredAttributes,
			 String numericOid, String superClass) throws NamingException
	{
		DirContext schema = ldap.getContext().getSchema("");

		// Specify attributes for the schema object
		Attributes attributes = new BasicAttributes(true); // Ignore case
		attributes.put("NUMERICOID", numericOid);
		attributes.put("NAME", className);
		attributes.put("DESC", description);
		attributes.put("SUP", superClass);
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

	public Integer getUserCount(String rootDn) throws NamingException
	{
		Integer count = 0;

		String[] returningAttributes = new String[]{};

		char[] alphabet = {'-','.',
				'a','b','c','d','e','f','g','h'
				,'i','j','k','l','m','n','o','p','q'
				,'r','s','t','u','v','w','x','y','z',
				'0','1','2','3','4','5','6','7','8','9'};

		char[] alphabet2 = {'_','@','-','.',
				'a','b','c','d','e','f','g','h'
				,'i','j','k','l','m','n','o','p','q'
				,'r','s','t','u','v','w','x','y','z',
				'0','1','2','3','4','5','6','7','8','9'};

		List<String> exclude = Arrays.asList("__");

		for(int index=0; index<alphabet.length-1; index++)
		{
			for(int index2=0; index2<alphabet2.length-1; index2++)
			{
				String searchValue = "" + alphabet[index] + alphabet2[index2];
				String searchFilter = "cn=" + searchValue + "*";

				if(exclude.contains(searchValue))
				{
					continue;
				}

				System.out.print("checking " + searchValue + "\r");

				Map<String, Attributes> results =
						ldap.searchAttributes(rootDn, searchFilter, returningAttributes);

				count += results.size();
			}
		}

		return count;
	}

}

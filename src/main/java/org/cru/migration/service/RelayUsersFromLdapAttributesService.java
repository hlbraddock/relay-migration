package org.cru.migration.service;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.ccci.idm.ldap.attributes.LdapAttributesActiveDirectory;
import org.ccci.idm.util.DataMngr;
import org.ccci.idm.util.MappedProperties;
import org.ccci.idm.util.Time;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.domain.StaffRelayUserMap;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;
import org.joda.time.DateTime;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class RelayUsersFromLdapAttributesService
{
    private LdapAttributesActiveDirectory ldapAttributes;

    private StaffRelayUserMap staffRelayUserMap;

    public RelayUsersFromLdapAttributesService()
    {
        ldapAttributes = new LdapAttributesActiveDirectory();

        staffRelayUserMap = new StaffRelayUserMap(ldapAttributes);
    }

    public class Results
    {
        Set<RelayUser> relayUsers = Sets.newConcurrentHashSet();
        Set<RelayUser> invalidRelayUsers = Sets.newConcurrentHashSet();

        public Set<RelayUser> getRelayUsers()
        {
            return relayUsers;
        }

        public Set<RelayUser> getInvalidRelayUsers()
        {
            return invalidRelayUsers;
        }
    }

	public Results getRelayUsers(Map<String, Attributes> entries, String[] returnAttributes) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

        Results results = new Results();

		ServiceData serviceData = new ServiceData(returnAttributes, entries, results);

		executionService.execute(new GetRelayUsers(), serviceData, 200);

		return serviceData.getResults();
	}

	private class ServiceData
	{
        private String[] returnAttributes;
        private Map<String, Attributes> entries;
        private Results results;

        private ServiceData(String[] returnAttributes, Map<String, Attributes> entries, Results results)
        {
            this.returnAttributes = returnAttributes;
            this.entries = entries;
            this.results = results;
        }

        public String[] getReturnAttributes()
        {
            return returnAttributes;
        }

        public Map<String, Attributes> getEntries()
        {
            return entries;
        }

        public Results getResults()
        {
            return results;
        }
    }

	public class GetRelayUsers implements ExecuteAction
	{
		@Override
		public void execute(ExecutorService executorService, Object object)
		{
			ServiceData serviceData = (ServiceData)object;

            for (Map.Entry<String, Attributes> entry : serviceData.getEntries().entrySet())
            {
                Attributes attributes = entry.getValue();

                executorService.execute(new WorkerThread(attributes, serviceData.getReturnAttributes(),
                        serviceData.getResults()));
            }
        }
	}

	private class WorkerThread implements Runnable
	{
        private Attributes attributes;
        private String[] returnAttributes;
        private Results results;

        private WorkerThread(Attributes attributes, String[] returnAttributes, Results results)
        {
            this.attributes = attributes;
            this.returnAttributes = returnAttributes;
            this.results = results;
        }

        @Override
		public void run()
		{
            try
			{
                RelayUser relayUser = getRelayUser(returnAttributes, attributes);

                if(relayUser != null)
                {
                    if(Strings.isNullOrEmpty(relayUser.getUsername()))
                    {
                        results.getInvalidRelayUsers().add(relayUser);
                    }
                    else
                    {
                        results.getRelayUsers().add(relayUser);
                    }
                }
            }
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

    private RelayUser getRelayUser(String[] returnAttributes, Attributes attributes)
    {
        RelayUser relayUser = new RelayUser();

        MappedProperties<RelayUser> mappedProperties = new MappedProperties<RelayUser>(staffRelayUserMap,
                relayUser);

        for (String attributeName : returnAttributes)
        {
            // handle multi valued attributes
            if (attributeName.equals(ldapAttributes.proxyAddresses))
            {
                relayUser.setProxyAddresses(DataMngr.getAttributes(attributes, attributeName));
                continue;
            }

            // handle single valued attributes
            String attributeValue = DataMngr.getAttribute(attributes, attributeName);

            if(Strings.isNullOrEmpty(attributeValue))
            {
                continue;
            }

            if (attributeName.equals(ldapAttributes.lastLogonTimeStamp))
            {
                if(!Strings.isNullOrEmpty(attributeValue))
                {
                    relayUser.setLastLogonTimestamp(new DateTime(Time.windowsToUnixTime(Long.parseLong
                            (attributeValue))));
                }
            }
            else
            {
                mappedProperties.setProperty(attributeName, attributeValue);
            }
        }

        return relayUser;
    }
}

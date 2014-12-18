package org.cru.migration.service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.ccci.idm.user.User;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;
import org.cru.migration.support.Misc;
import org.cru.migration.thekey.GcxUserService;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class FindKeyAccountsMatchingMultipleRelayAcccountsService
{
	private GcxUserService gcxUserService;

	private Map<User,Set<RelayUser>> multipleRelayUsersMatchingKeyUser = Maps.newConcurrentMap();

	public FindKeyAccountsMatchingMultipleRelayAcccountsService(GcxUserService gcxUserService)
	{
		this.gcxUserService = gcxUserService;
	}

	public Map<User,Set<RelayUser>> run(Map<String, Attributes> theKeyEntries, Set<RelayUser> relayUsers) throws NamingException
	{
		ExecutionService executionService = new ExecutionService();

		Data data = new Data(theKeyEntries, relayUsers);

		executionService.execute(new Run(), data, 200);

		return multipleRelayUsersMatchingKeyUser;
	}

	private class Data
	{
		Map<String, Attributes> theKeyEntries;
		Set<RelayUser> relayUsers;

		private Data(Map<String, Attributes> theKeyEntries, Set<RelayUser> relayUsers)
		{
			this.theKeyEntries = theKeyEntries;
			this.relayUsers = relayUsers;
		}
	}

	private class Run implements ExecuteAction
	{
		@Override
		public void execute(ExecutorService executorService, Object object)
		{
			Data data = (Data) object;
			Map<String, Attributes> theKeyEntries = data.theKeyEntries;
			Set<RelayUser> relayUsers = data.relayUsers;

			for (Map.Entry<String, Attributes> entry : theKeyEntries.entrySet())
			{
				Attributes attributes = entry.getValue();
				String theKeySsoguid = attributes.get("theKeyGuid").getID();
				String theKeyEmail = attributes.get("cn").getID();

				executorService.execute(new WorkerThread(relayUsers, theKeySsoguid, theKeyEmail));
			}
		}
	}

	private class WorkerThread implements Runnable
	{
		private Set<RelayUser> relayUsers;
		private String theKeySsoguid;
		private String theKeyEmail;

		private WorkerThread(Set<RelayUser> relayUsers, String theKeySsoguid, String theKeyEmail)
		{
			this.relayUsers = relayUsers;
			this.theKeySsoguid = theKeySsoguid;
			this.theKeyEmail = theKeyEmail;
		}

		@Override
		public void run()
		{
			try
			{
				RelayUser ssoguidMatchingRelayUser = null;
				RelayUser emailMatchingRelayUser = null;
				for(RelayUser relayUser : relayUsers)
				{
					if(relayUser.getSsoguid().equalsIgnoreCase(theKeySsoguid))
					{
						ssoguidMatchingRelayUser = relayUser;
						if(emailMatchingRelayUser != null)
						{
							break;
						}
					}

					if(relayUser.getUsername().equalsIgnoreCase(theKeyEmail))
					{
						emailMatchingRelayUser = relayUser;
						if(ssoguidMatchingRelayUser != null)
						{
							break;
						}
					}
				}

				RelayUser linkMatchingRelayUser = null;
				if(Misc.nonNullCount(ssoguidMatchingRelayUser, emailMatchingRelayUser) > 0)
				{
					String relaySsoguidLinkMatch = gcxUserService.findRelayUserGuidByLinked(theKeySsoguid);
					if(relaySsoguidLinkMatch != null)
					{
						linkMatchingRelayUser = RelayUser.havingSsoguid(relayUsers, relaySsoguidLinkMatch);
					}
				}

				// if more than one relay account matching key account
				if(Misc.nonNullCount(ssoguidMatchingRelayUser, emailMatchingRelayUser, linkMatchingRelayUser) > 1)
				{
					Set<RelayUser> matchingRelayUsers = Sets.newHashSet();

					if(ssoguidMatchingRelayUser != null)
					{
						matchingRelayUsers.add(ssoguidMatchingRelayUser);
					}
					if(emailMatchingRelayUser != null)
					{
						matchingRelayUsers.add(emailMatchingRelayUser);
					}
					if(linkMatchingRelayUser != null)
					{
						matchingRelayUsers.add(linkMatchingRelayUser);
					}

					User user = gcxUserService.findGcxUserByEmail(theKeyEmail);

					multipleRelayUsersMatchingKeyUser.put(user, matchingRelayUsers);
				}
			}
			catch(Exception e)
			{
			}
		}
	}
}

package org.cru.migration.service;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;
import org.cru.migration.support.Misc;
import org.cru.migration.thekey.GcxUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class FindKeyAccountsMatchingMultipleRelayAccountsService
{
	private Logger logger = LoggerFactory.getLogger(getClass());

	private GcxUserService gcxUserService;

	private Set<String> multipleRelayUsersMatchingKeyUser = Sets.newConcurrentHashSet();
	private Map<String, Set<RelayUser>> keyUserMatchingRelayUsers = Maps.newConcurrentMap();

	private Map<String,String> relayUserGuidUsernameMap = Maps.newHashMap();
	private Map<String,String> relayUserUsernameGuidMap = Maps.newHashMap();

	public class Result
	{
		private Set<String> multipleRelayUsersMatchingKeyUser;
		private Map<String, Set<RelayUser>> keyUserMatchingRelayUsers;

		public Set<String> getMultipleRelayUsersMatchingKeyUser()
		{
			return multipleRelayUsersMatchingKeyUser;
		}

		public void setMultipleRelayUsersMatchingKeyUser(Set<String> multipleRelayUsersMatchingKeyUser)
		{
			this.multipleRelayUsersMatchingKeyUser = multipleRelayUsersMatchingKeyUser;
		}

		public Map<String, Set<RelayUser>> getKeyUserMatchingRelayUsers()
		{
			return keyUserMatchingRelayUsers;
		}

		public void setKeyUserMatchingRelayUsers(Map<String, Set<RelayUser>> keyUserMatchingRelayUsers)
		{
			this.keyUserMatchingRelayUsers = keyUserMatchingRelayUsers;
		}
	}

	public FindKeyAccountsMatchingMultipleRelayAccountsService(GcxUserService gcxUserService)
	{
		this.gcxUserService = gcxUserService;
	}

	public Result run(Map<String, Attributes> theKeyEntries, Set<RelayUser> relayUsers) throws
			NamingException
	{
		ExecutionService executionService = new ExecutionService();

		for(RelayUser relayUser : relayUsers)
		{
			relayUserGuidUsernameMap.put(relayUser.getSsoguid(), relayUser.getUsername());
			relayUserUsernameGuidMap.put(relayUser.getUsername(), relayUser.getSsoguid());
		}

		Data data = new Data(theKeyEntries, relayUsers, relayUserGuidUsernameMap, relayUserUsernameGuidMap);

		executionService.execute(new Run(), data, 400);

		Result result = new Result();
		result.setKeyUserMatchingRelayUsers(keyUserMatchingRelayUsers);
		result.setMultipleRelayUsersMatchingKeyUser(multipleRelayUsersMatchingKeyUser);

		return result;
	}

	private class Data
	{
		private Map<String, Attributes> theKeyEntries;
		private Set<RelayUser> relayUsers;
		private Map<String,String> relayUserGuidUsernameMap;
		private Map<String,String> relayUserUsernameGuidMap;

		public Data(Map<String, Attributes> theKeyEntries, Set<RelayUser> relayUsers, Map<String, String> relayUserGuidUsernameMap, Map<String, String> relayUserUsernameGuidMap)
		{
			this.theKeyEntries = theKeyEntries;
			this.relayUsers = relayUsers;
			this.relayUserGuidUsernameMap = relayUserGuidUsernameMap;
			this.relayUserUsernameGuidMap = relayUserUsernameGuidMap;
		}
	}

	private class Run implements ExecuteAction
	{
		@Override
		public void execute(ExecutorService executorService, Object object)
		{
			Data data = (Data) object;

			for (Map.Entry<String, Attributes> entry : data.theKeyEntries.entrySet())
			{
				Attributes attributes = entry.getValue();

				String theKeyEmail = Misc.getAttribute(attributes, "cn");
				String theKeyGuid = Misc.getAttribute(attributes, "theKeyGuid");

				executorService.execute(
						new WorkerThread(data.relayUsers,
						data.relayUserGuidUsernameMap,
						data.relayUserUsernameGuidMap,
						theKeyGuid,
						theKeyEmail));
			}
		}
	}

	private class WorkerThread implements Runnable
	{
		private Set<RelayUser> relayUsers;
		private Map<String,String> relayUserGuidUsernameMap;
		private Map<String,String> relayUserUsernameGuidMap;
		private String theKeyGuid;
		private String theKeyEmail;

		public WorkerThread(Set<RelayUser> relayUsers, Map<String, String> relayUserGuidUsernameMap, Map<String, String> relayUserUsernameGuidMap, String theKeyGuid, String theKeyEmail)
		{
			this.relayUsers = relayUsers;
			this.relayUserGuidUsernameMap = relayUserGuidUsernameMap;
			this.relayUserUsernameGuidMap = relayUserUsernameGuidMap;
			this.theKeyGuid = theKeyGuid;
			this.theKeyEmail = theKeyEmail;
		}

		@Override
		public void run()
		{
			try
			{
				Boolean relayGuidMatches = false;
				Boolean relayUsernameMatches = false;
				Boolean relayLinkMatches = false;

				if(!Strings.isNullOrEmpty(theKeyGuid) && relayUserGuidUsernameMap.containsKey(theKeyGuid))
				{
					relayGuidMatches = true;
				}

				if((!Strings.isNullOrEmpty(theKeyEmail) && relayUserGuidUsernameMap.containsValue(theKeyEmail))
						&&
						// not the same relay user as already matched
						!(relayGuidMatches && relayUserGuidUsernameMap.get(theKeyGuid).equals(theKeyEmail)))
				{
					relayUsernameMatches = true;
				}

				/*
				 * only check for link if you already have at least one match,
				 * since we're just concerned with multiple matches
				 */
				String relayLinkedGuid = null;
				if(relayGuidMatches || relayUsernameMatches)
				{
					relayLinkedGuid = gcxUserService.findLinkedRelayUserGuidByTheKeyGuid(theKeyGuid);

					if(!Strings.isNullOrEmpty(relayLinkedGuid))
					{
						relayLinkMatches = true;

						// don't record match if the linked guid does not actually exist in relay
						if(relayLinkMatches)
							if(!relayUserGuidUsernameMap.containsKey(relayLinkedGuid))
							{
								relayLinkMatches = false;
							}

						// don't record match if it's the same relay user as already matched
						if(relayLinkMatches)
							if(relayGuidMatches && relayLinkedGuid.equals(theKeyGuid))
							{
								relayLinkMatches = false;
							}

						// don't record match if it's the same relay user as already matched
						if(relayLinkMatches)
							if(relayUsernameMatches && relayUserUsernameGuidMap.get(theKeyEmail).equals(relayLinkedGuid))
							{
								relayLinkMatches = false;
							}
					}
				}

				if(Misc.trueCount(relayGuidMatches, relayUsernameMatches, relayLinkMatches) > 1)
				{
					String result = "Match on the Key " + theKeyEmail + "," + theKeyGuid + " by ";

					Set<RelayUser> relayUsersMatching = Sets.newHashSet();

					if(relayGuidMatches)
					{
						RelayUser matchingRelayUser = RelayUser.havingSsoguid(relayUsers, theKeyGuid);
						if(matchingRelayUser != null)
						{
							relayUsersMatching.add(matchingRelayUser);
							result += "Relay GUID, ";
						}
						else
						{
							logger.error("Should never get null matching relay user on relay guid matching key guid "
									+ theKeyGuid);
						}
					}

					if(relayUsernameMatches)
					{
						RelayUser matchingRelayUser = RelayUser.havingUsername(relayUsers, theKeyEmail);
						if(matchingRelayUser != null)
						{
							relayUsersMatching.add(matchingRelayUser);
							result += "Relay USERNAME, ";
						}
						else
						{
							logger.error("Should never get null matching relay user on relay username matching key " +
									"username " + theKeyEmail);
						}
					}

					if(relayLinkMatches)
					{
						RelayUser matchingRelayUser = RelayUser.havingSsoguid(relayUsers, relayLinkedGuid);
						if(matchingRelayUser != null)
						{
							relayUsersMatching.add(matchingRelayUser);
							result += "Relay LINK (relay guid " + relayLinkedGuid + ")";
						}
						else
						{
							logger.error("Should never get null matching relay linked user on relay linked guid " +
									relayLinkedGuid);
						}
					}

					keyUserMatchingRelayUsers.put(theKeyGuid, relayUsersMatching);
					multipleRelayUsersMatchingKeyUser.add(result);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}

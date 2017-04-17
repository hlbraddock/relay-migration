package org.cru.migration.thekey;

import com.google.common.base.Strings;
import org.ccci.idm.user.User;
import org.ccci.idm.user.UserManager;
import org.cru.migration.domain.MatchingUsers;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.support.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GcxUserService
{
	private UserManager userManager;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public GcxUserService(UserManager userManager)
	{
		this.userManager = userManager;
	}

	public enum MatchType { GUID, EMAIL, LINKED, NONE, GUID_AND_LINKED, GUID_AND_EMAIL, EMAIL_AND_LINKED,
		GUID_AND_LINKED_AND_EMAIL, PROXY
	}

	public static class MatchResult
	{
		public MatchType matchType = MatchType.NONE;

		public Boolean multiples()
		{
			if(matchType.equals(MatchType.NONE))
			{
				return false;
			}

			if(matchType.equals(MatchType.GUID))
			{
				return false;
			}

			else if(matchType.equals(MatchType.EMAIL))
			{
				return false;
			}

			else if(matchType.equals(MatchType.LINKED))
			{
				return false;
			}

			else if(matchType.equals(MatchType.PROXY))
			{
				return false;
			}

			else if(matchType.equals(MatchType.GUID_AND_EMAIL))
			{
				return true;
			}

			else if(matchType.equals(MatchType.GUID_AND_LINKED))
			{
				return true;
			}

			else if(matchType.equals(MatchType.EMAIL_AND_LINKED))
			{
				return true;
			}

			else if(matchType.equals(MatchType.GUID_AND_LINKED_AND_EMAIL))
			{
				return true;
			}

			return false;
		}
	}

	public User resolveGcxUser(MatchResult matchResult, MatchingUsers matchingUsers)
	{
		User user = null;

		if(matchResult.matchType.equals(MatchType.GUID))
		{
			user = matchingUsers.getUserByGuid();
		}

		else if(matchResult.matchType.equals(MatchType.EMAIL))
		{
			user = matchingUsers.getUserByEmail();
		}

		else if(matchResult.matchType.equals(MatchType.LINKED))
		{
			user = matchingUsers.getUserByLinked();
		}

		else if(matchResult.matchType.equals(MatchType.PROXY))
		{
			user = matchingUsers.getUserByProxy();
		}

		else if(matchResult.matchType.equals(MatchType.GUID_AND_EMAIL))
		{
			user = matchingUsers.getUserByEmail();
		}

		else if(matchResult.matchType.equals(MatchType.GUID_AND_LINKED))
		{
			user = matchingUsers.getUserByLinked();
		}

		else if(matchResult.matchType.equals(MatchType.EMAIL_AND_LINKED))
		{
			user = matchingUsers.getUserByLinked();
		}

		else if(matchResult.matchType.equals(MatchType.GUID_AND_LINKED_AND_EMAIL))
		{
			user = matchingUsers.getUserByLinked();
		}

		else if(!matchResult.matchType.equals(MatchType.NONE))
		{
			throw new RuntimeException("Expected some kind of known match type.");
		}

		return user;
	}

	public MatchingUsers findGcxUsers(RelayUser relayUser, MatchResult matchResult)
	{
		MatchingUsers matchingUsers = new MatchingUsers();

		// search gcx user by various means
		User gcxUserByGuid = findGcxUserByTheKeyGuid(relayUser.getSsoguid());
		User gcxUserByLinked = findGcxUserByTheKeyGuid(findLinkedTheKeyUserGuidByRelayGuid(relayUser.getSsoguid()));
		User gcxUserByEmail = findGcxUserByEmail(relayUser.getUsername());

		int gcxUserMatchCount = Misc.nonNullCount(gcxUserByGuid, gcxUserByLinked, gcxUserByEmail);

		// if no users matched check for proxy match
		if(gcxUserMatchCount == 0)
		{
			User gcxUserByProxyAddress = findGcxUserByProxyAddressAsEmail(relayUser.getProxyAddresses());
			if(gcxUserByProxyAddress != null) {
				matchingUsers.setUserByProxy(gcxUserByProxyAddress);
				matchResult.matchType = MatchType.PROXY;
			}
		}

		// if one gcx user found
		else if(gcxUserMatchCount == 1)
		{
			if(gcxUserByGuid != null)
			{
				matchResult.matchType = MatchType.GUID;
				matchingUsers.setUserByGuid(gcxUserByGuid);
			}
			else if(gcxUserByEmail != null)
			{
				matchResult.matchType = MatchType.EMAIL;
				matchingUsers.setUserByEmail(gcxUserByEmail);
			}
			else if(gcxUserByLinked != null)
			{
				matchingUsers.setUserByLinked(gcxUserByLinked);
				matchResult.matchType = MatchType.LINKED;
			}
		}

		// if two gcx users found
		else if(gcxUserMatchCount == 2)
		{
			if(Misc.areNonNull(gcxUserByGuid, gcxUserByEmail))
			{
				matchingUsers.setUserByGuid(gcxUserByGuid);
				matchResult.matchType = MatchType.GUID;

				if(!equals(gcxUserByGuid, gcxUserByEmail))
				{
					matchingUsers.setUserByEmail(gcxUserByEmail);
					matchResult.matchType = MatchType.GUID_AND_EMAIL;
				}
			}

			else if(Misc.areNonNull(gcxUserByGuid, gcxUserByLinked))
			{
				matchingUsers.setUserByGuid(gcxUserByGuid);
				matchResult.matchType = MatchType.GUID;

				if(!equals(gcxUserByGuid, gcxUserByLinked))
				{
					matchingUsers.setUserByLinked(gcxUserByLinked);
					matchResult.matchType = MatchType.GUID_AND_LINKED;
				}
			}

			else if(Misc.areNonNull(gcxUserByLinked, gcxUserByEmail))
			{
				matchingUsers.setUserByEmail(gcxUserByEmail);
				matchResult.matchType = MatchType.EMAIL;

				if(!equals(gcxUserByLinked, gcxUserByEmail))
				{
					matchingUsers.setUserByLinked(gcxUserByLinked);
					matchResult.matchType = MatchType.EMAIL_AND_LINKED;
				}
			}

			else
			{
				throw new RuntimeException("Expected two matches. Should not have got here.");
			}
		}

		else if(gcxUserMatchCount == 3)
		{
			matchingUsers.setUserByGuid(gcxUserByGuid);
			matchResult.matchType = MatchType.GUID;

			if (equals(gcxUserByGuid, gcxUserByEmail))
			{
				if (!equals(gcxUserByGuid, gcxUserByLinked))
				{
					matchingUsers.setUserByLinked(gcxUserByLinked);
					matchResult.matchType = MatchType.GUID_AND_LINKED;
				}
			}

			else
			{
				matchingUsers.setUserByEmail(gcxUserByEmail);
				matchResult.matchType = MatchType.GUID_AND_EMAIL;

				if (!equals(gcxUserByGuid, gcxUserByLinked))
				{
					matchingUsers.setUserByLinked(gcxUserByLinked);
					matchResult.matchType = MatchType.GUID_AND_LINKED_AND_EMAIL;
				}
			}
		}

		return matchingUsers;
	}

	private Boolean equals(User gcxUser, User gcxUser2)
	{
		return Misc.areNonNull(gcxUser, gcxUser2) && gcxUser.getTheKeyGuid().equals(gcxUser2.getTheKeyGuid());
	}

	private User filter(User gcxUser)
	{
		return gcxUser == null || gcxUser.isDeactivated() ? null : gcxUser;
	}

	private User findGcxUserByTheKeyGuid(String id)
	{
		User gcxUser = null;

		if(Strings.isNullOrEmpty(id))
		{
			return null;
		}

		try
		{
			gcxUser = userManager.findUserByTheKeyGuid(id);
		}
		catch(Exception e)
		{
			logger.info("find by ssoguid " + id + " exception " + e.getMessage());
		}

		return filter(gcxUser);
	}

	private String findLinkedTheKeyUserGuidByRelayGuid(String id)
	{
		return null;
	}

	public String findLinkedRelayUserGuidByTheKeyGuid(String id)
	{
		return null;
	}

	private User findGcxUserByEmail(String id)
	{
		User gcxUser = null;

		if(Strings.isNullOrEmpty(id))
		{
			return null;
		}

		try
		{
			gcxUser = userManager.findUserByEmail(id, false);
		}
		catch(Exception e)
		{
			logger.info("find by email " + id + " exception " + e.getMessage());
		}

		return filter(gcxUser);
	}

	private User findGcxUserByProxyAddressAsEmail(List<String> proxyAddresses)
	{
		User gcxUser = null;

		if(proxyAddresses != null) {
			for (String proxyAddress : proxyAddresses) {
				try {
					gcxUser = userManager.findUserByEmail(proxyAddress.replaceAll("(?i)smtp:", ""), false);
					if (gcxUser != null) {
						break;
					}
				} catch (Exception e) {
					logger.info("find by email " + proxyAddress + " exception " + e.getMessage());
				}
			}
		}

		return filter(gcxUser);
	}

	public User getGcxUserFromRelayUser(RelayUser relayUser, String primaryGuid)
	{
		final User gcxUser = relayUser.toUser();

		gcxUser.setGuid(primaryGuid);

		gcxUser.setRelayGuid(primaryGuid);

		return gcxUser;
	}

	public static class MatchDifferentGcxUsersException extends Exception
	{
		public MatchDifferentGcxUsersException(String message)
		{
			super(message);
		}
	}

	public class MatchDifferentGcxUsersGuidLinkedException extends MatchDifferentGcxUsersException
	{
		public MatchDifferentGcxUsersGuidLinkedException(String message)
		{
			super(message);
		}
	}

	public class MatchDifferentGcxUsersEmailLinkedException extends MatchDifferentGcxUsersException
	{
		public MatchDifferentGcxUsersEmailLinkedException(String message)
		{
			super(message);
		}
	}

	public class MatchDifferentGcxUsersGuidEmailLinkedException extends MatchDifferentGcxUsersException
	{
		public MatchDifferentGcxUsersGuidEmailLinkedException(String message)
		{
			super(message);
		}
	}

	public UserManager getUserManager()
	{
		return userManager;
	}
}

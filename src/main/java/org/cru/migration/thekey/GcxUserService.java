package org.cru.migration.thekey;

import com.google.common.base.Strings;
import org.ccci.idm.user.User;
import org.ccci.idm.user.UserManager;
import org.cru.migration.domain.MatchingUsers;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.support.Base64RandomStringGenerator;
import org.cru.migration.support.Misc;
import org.cru.silc.domain.Identity;
import org.cru.silc.service.LinkingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcxUserService
{
	private UserManager userManager;

	private LinkingService linkingService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public GcxUserService(UserManager userManager, LinkingService linkingService)
	{
		this.userManager = userManager;
		this.linkingService = linkingService;
	}

	public enum MatchType { GUID, EMAIL, LINKED, NONE, GUID_AND_LINKED, GUID_AND_EMAIL, EMAIL_AND_LINKED,
		GUID_AND_LINKED_AND_EMAIL
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

	public User resolveGcxUser(RelayUser relayUser, MatchResult matchResult, MatchingUsers matchingUsers)
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
		User gcxUserByGuid = findGcxUserByGuid(relayUser.getSsoguid());
		User gcxUserByLinked = findGcxUserByGuid(findLinkedTheKeyUserGuidByRelayGuid(relayUser.getSsoguid()));
		User gcxUserByEmail = findGcxUserByEmail(relayUser.getUsername());

		int gcxUserMatchCount = Misc.nonNullCount(gcxUserByGuid, gcxUserByLinked, gcxUserByEmail);

		// if one gcx user found
		if(gcxUserMatchCount == 1)
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
		return Misc.areNonNull(gcxUser, gcxUser2) && gcxUser.getGuid().equals(gcxUser2.getGuid());
	}

	private User filter(User gcxUser)
	{
		return gcxUser == null ? null :
				(gcxUser.getEmail() == null ? gcxUser :
						(!gcxUser.getEmail().startsWith("$GUID$") ? gcxUser : null));
	}

	public User findGcxUserByGuid(String id)
	{
		User gcxUser = null;

		if(Strings.isNullOrEmpty(id))
		{
			return null;
		}

		try
		{
			gcxUser = userManager.findUserByGuid(id);
		}
		catch(Exception e)
		{
			logger.info("find by ssoguid " + id + " exception " + e.getMessage());
		}

		return filter(gcxUser);
	}

	public String findLinkedTheKeyUserGuidByRelayGuid(String id)
	{
		if(Strings.isNullOrEmpty(id))
		{
			return null;
		}

		Identity identity =  getLinkedIdentityByProviderType(new Identity(id, Identity.ProviderType.RELAY), Identity.ProviderType.THE_KEY);

		if(identity == null)
		{
			return null;
		}

		return identity.getId();
	}

	public String findLinkedRelayUserGuidByTheKeyGuid(String id)
	{
		if(Strings.isNullOrEmpty(id))
		{
			return null;
		}

		Identity identity = getLinkedIdentityByProviderType(new Identity(id, Identity.ProviderType.THE_KEY), Identity.ProviderType.RELAY);

		if(identity == null)
		{
			return null;
		}

		return identity.getId();
	}

	private Identity getLinkedIdentityByProviderType(Identity identity, Identity.ProviderType linkedProviderType)
	{
		try
		{
			return linkingService.getLinkedIdentityByProviderType(identity, linkedProviderType);

		}
		catch(Exception e)
		{
			logger.error("find by " + identity.getType() + " id " + identity.getId() + " exception " + e.getMessage());
		}

		return null;
	}

	public User findGcxUserByEmail(String id)
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

	public User getGcxUserFromRelayUser(RelayUser relayUser, String primaryGuid)
	{
		final User gcxUser = relayUser.toUser();

		gcxUser.setGuid(primaryGuid);

		gcxUser.setTheKeyGuid(primaryGuid);
		gcxUser.setRelayGuid(primaryGuid);
		gcxUser.setSignupKey(new Base64RandomStringGenerator().getNewString());
		gcxUser.setForcePasswordChange(false);
		gcxUser.setLoginDisabled(false);

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
}

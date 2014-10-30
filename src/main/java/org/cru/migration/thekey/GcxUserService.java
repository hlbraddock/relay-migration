package org.cru.migration.thekey;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.ccci.idm.user.User;
import org.ccci.idm.user.UserManager;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.support.Base64RandomStringGenerator;
import org.cru.migration.support.Misc;
import org.cru.silc.domain.Identity;
import org.cru.silc.service.LinkingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.Set;

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
	}

	public User resolveGcxUser(RelayUser relayUser, MatchResult matchResult, Set<User> gcxUsers)
			throws MatchDifferentGcxUsersException
	{
		if(gcxUsers.size() == 1)
		{
			return gcxUsers.iterator().next();
		}

		else if(gcxUsers.size() == 2)
		{
			/*
				Merge Relay with two Key accounts: one match on guid and one match on email

				Example Use Case:

				Relay:
				email / guid
				joe@cru.org / ABCDEFG

				The Key:
				email / guid
				joe@cru.org / 1234567
				sue@cru.org / ABCDEFG

				ReKey:
				email / guid / relay guid / key guid
				joe@cru.org / 1234567 / ABCDEFG / none
			 */

			if(matchResult.matchType.equals(MatchType.GUID_AND_EMAIL))
			{
				User gcxUser = getGcxUserHavingEmail(gcxUsers, relayUser.getUsername());

				if(gcxUser == null)
				{
					throw new RuntimeException("Should have found email match for " + relayUser.toString());
				}

				gcxUser.setRelayGuid(relayUser.getSsoguid());

				return gcxUser;
			}

			else if(matchResult.matchType.equals(MatchType.GUID_AND_LINKED))
			{
				throw new MatchDifferentGcxUsersGuidLinkedException("match on guid and linked");
			}

			else if(matchResult.matchType.equals(MatchType.EMAIL_AND_LINKED))
			{
				throw new MatchDifferentGcxUsersEmailLinkedException("match on email and linked");
			}

			else
			{
				throw new RuntimeException("Expected two set match type.");
			}
		}

		else if(gcxUsers.size() == 3)
		{
			throw new MatchDifferentGcxUsersGuidEmailLinkedException("match on guid, email, and linked");
		}

		return null;
	}

	public Set<User> findGcxUsers(RelayUser relayUser, MatchResult matchResult)
	{
		Set<User> gcxUsers = Sets.newHashSet();

		// search gcx user by various means
		User gcxUserByGuid = findGcxUserByGuid(relayUser.getSsoguid());
		User gcxUserByLinked = findGcxUserByLinked(relayUser.getSsoguid());
		User gcxUserByEmail = findGcxUserByEmail(relayUser.getUsername());

		int gcxUserMatchCount = Misc.nonNullCount(gcxUserByGuid, gcxUserByLinked, gcxUserByEmail);

		// if one gcx user found
		if(gcxUserMatchCount == 1)
		{
			if(gcxUserByGuid != null)
			{
				matchResult.matchType = MatchType.GUID;
				gcxUsers.add(gcxUserByGuid);
			}
			else if(gcxUserByEmail != null)
			{
				matchResult.matchType = MatchType.EMAIL;
				gcxUsers.add(gcxUserByEmail);
			}
			else if(gcxUserByLinked != null)
			{
				matchResult.matchType = MatchType.LINKED;
				gcxUsers.add(gcxUserByLinked);
			}
		}

		// if two gcx users found
		else if(gcxUserMatchCount == 2)
		{
			if(Misc.areNonNull(gcxUserByGuid, gcxUserByEmail))
			{
				gcxUsers.add(gcxUserByGuid);
				matchResult.matchType = MatchType.GUID;

				if(!equals(gcxUserByGuid, gcxUserByEmail))
				{
					gcxUsers.add(gcxUserByEmail);
					matchResult.matchType = MatchType.GUID_AND_EMAIL;
				}
			}

			else if(Misc.areNonNull(gcxUserByGuid, gcxUserByLinked))
			{
				gcxUsers.add(gcxUserByGuid);
				matchResult.matchType = MatchType.GUID;

				if(!equals(gcxUserByGuid, gcxUserByLinked))
				{
					gcxUsers.add(gcxUserByLinked);
					matchResult.matchType = MatchType.GUID_AND_LINKED;
				}
			}

			else if(Misc.areNonNull(gcxUserByLinked, gcxUserByEmail))
			{
				gcxUsers.add(gcxUserByLinked);
				matchResult.matchType = MatchType.LINKED;

				if(!equals(gcxUserByLinked, gcxUserByEmail))
				{
					gcxUsers.add(gcxUserByEmail);
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
			gcxUsers.add(gcxUserByGuid);
			matchResult.matchType = MatchType.GUID;

			if (equals(gcxUserByGuid, gcxUserByEmail))
			{
				if (!equals(gcxUserByGuid, gcxUserByLinked))
				{
					gcxUsers.add(gcxUserByLinked);
					matchResult.matchType = MatchType.GUID_AND_LINKED;
				}
			}

			else
			{
				gcxUsers.add(gcxUserByEmail);
				matchResult.matchType = MatchType.GUID_AND_EMAIL;

				if (!equals(gcxUserByGuid, gcxUserByLinked))
				{
					gcxUsers.add(gcxUserByLinked);
					matchResult.matchType = MatchType.GUID_AND_LINKED_AND_EMAIL;
				}
			}
		}

		return gcxUsers;
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

	public User findGcxUserByLinked(String id)
	{
		User gcxUser = null;

		if(Strings.isNullOrEmpty(id))
		{
			return null;
		}

		try
		{
			Identity identity = new Identity(id, Identity.ProviderType.RELAY);

			identity = linkingService.getLinkedIdentityByProviderType(identity, Identity.ProviderType.THE_KEY);

			if(identity == null)
			{
				return null;
			}

			gcxUser = findGcxUserByGuid(identity.getId());
		}
		catch(Exception e)
		{
			logger.info("find by relay ssoguid " + id + " exception " + e.getMessage());
		}

		return filter(gcxUser);
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

	public User getGcxUser(RelayUser relayUser, String primaryGuid)
	{
		final User gcxUser = relayUser.toUser();

		gcxUser.setGuid(primaryGuid);

		gcxUser.setTheKeyGuid(gcxUser.getGuid());
		gcxUser.setSignupKey(new Base64RandomStringGenerator().getNewString());
		gcxUser.setForcePasswordChange(false);
		gcxUser.setLoginDisabled(false);

		return gcxUser;
	}

	public abstract class MatchDifferentGcxUsersException extends Exception
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

	private User getGcxUserHavingEmail(Set<User> gcxUsers, final String element)
	{
		try
		{
			return Iterables.find(gcxUsers, new Predicate<User>()
			{
				public boolean apply(User gcxUser)
				{
					return gcxUser.getEmail().equalsIgnoreCase(element);
				}
			});
		}
		catch(NoSuchElementException e)
		{
			return null;
		}
	}
}

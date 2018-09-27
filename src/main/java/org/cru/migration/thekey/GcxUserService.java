package org.cru.migration.thekey;

import org.ccci.idm.user.User;
import org.ccci.idm.user.UserManager;
import org.cru.migration.domain.MatchingUsers;
import org.cru.migration.domain.RelayUser;


public class GcxUserService
{
	private UserManager userManager;

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

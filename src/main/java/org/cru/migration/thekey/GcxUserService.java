package org.cru.migration.thekey;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import me.thekey.cas.service.UserManager;
import org.ccci.gcx.idm.core.model.impl.GcxUser;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.support.Misc;
import org.cru.silc.domain.Identity;
import org.cru.silc.service.LinkingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public enum MatchType { GUID, EMAIL, RELAY_GUID, NONE, GUID_AND_RELAY_GUID, GUID_AND_EMAIL, RELAY_GUID_AND_EMAIL,
		GUID_AND_RELAY_GUID_AND_EMAIL }

	public static class MatchResult
	{
		MatchType matchType = MatchType.NONE;
	}

	public GcxUser resolveGcxUser(RelayUser relayUser, MatchResult matchResult, Set<GcxUser> gcxUsers)
			throws MatchDifferentGcxUsersException
	{
		if(gcxUsers.size() == 1)
		{
			return gcxUsers.iterator().next();
		}

		else if(gcxUsers.size() == 2)
		{
			if(matchResult.matchType.equals(MatchType.GUID_AND_EMAIL))
			{
				throw new MatchDifferentGcxUsersGuidEmailException("match on guid and email");
			}

			else if(matchResult.matchType.equals(MatchType.GUID_AND_RELAY_GUID))
			{
				throw new MatchDifferentGcxUsersGuidRelayGuidException("match on guid and relay guid");
			}

			else if(matchResult.matchType.equals(MatchType.RELAY_GUID_AND_EMAIL))
			{
				throw new MatchDifferentGcxUsersRelayGuidEmailException("match on relay guid and email");
			}

			else
			{
				throw new RuntimeException("Expected two set match type.");
			}
		}

		else if(gcxUsers.size() == 3)
		{
			throw new MatchDifferentGcxUsersRelayGuidEmailException("match on guid, relay guid, and email");
		}

		return null;
	}

	public Set<GcxUser> findGcxUsers(RelayUser relayUser, MatchResult matchResult)
	{
		Set<GcxUser> gcxUsers = Sets.newHashSet();

		// search gcx user by various means
		GcxUser gcxUserByGuid = findGcxUserByGuid(relayUser.getSsoguid());
		GcxUser gcxUserByRelayGuid = findGcxUserByRelayGuid(relayUser.getSsoguid());
		GcxUser gcxUserByEmail = findGcxUserByEmail(relayUser.getUsername());

		int gcxUserMatchCount = Misc.nonNullCount(gcxUserByGuid, gcxUserByRelayGuid, gcxUserByEmail);

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
			else if(gcxUserByRelayGuid != null)
			{
				matchResult.matchType = MatchType.RELAY_GUID;
				gcxUsers.add(gcxUserByRelayGuid);
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
					gcxUsers.add(gcxUserByGuid);
					matchResult.matchType = MatchType.GUID_AND_EMAIL;
				}
			}

			else if(Misc.areNonNull(gcxUserByGuid, gcxUserByRelayGuid))
			{
				gcxUsers.add(gcxUserByGuid);
				matchResult.matchType = MatchType.GUID;

				if(!equals(gcxUserByGuid, gcxUserByRelayGuid))
				{
					gcxUsers.add(gcxUserByGuid);
					matchResult.matchType = MatchType.GUID_AND_RELAY_GUID;
				}
			}

			else if(Misc.areNonNull(gcxUserByRelayGuid, gcxUserByEmail))
			{
				gcxUsers.add(gcxUserByRelayGuid);
				matchResult.matchType = MatchType.RELAY_GUID;

				if(!equals(gcxUserByRelayGuid, gcxUserByEmail))
				{
					gcxUsers.add(gcxUserByGuid);
					matchResult.matchType = MatchType.RELAY_GUID_AND_EMAIL;
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
				if (!equals(gcxUserByGuid, gcxUserByRelayGuid))
				{
					gcxUsers.add(gcxUserByRelayGuid);
					matchResult.matchType = MatchType.GUID_AND_RELAY_GUID;
				}
			}

			else
			{
				gcxUsers.add(gcxUserByEmail);
				matchResult.matchType = MatchType.GUID_AND_EMAIL;

				if (!equals(gcxUserByGuid, gcxUserByRelayGuid))
				{
					gcxUsers.add(gcxUserByRelayGuid);
					matchResult.matchType = MatchType.GUID_AND_RELAY_GUID_AND_EMAIL;

				}
			}
		}

		return gcxUsers;
	}

	private Boolean equals(GcxUser gcxUser, GcxUser gcxUser2)
	{
		return Misc.areNonNull(gcxUser, gcxUser2) && gcxUser.getGUID().equals(gcxUser2.getGUID());
	}

	public GcxUser findGcxUserByGuid(String id)
	{
		GcxUser gcxUser = null;

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

		return gcxUser;
	}

	public GcxUser findGcxUserByRelayGuid(String id)
	{
		GcxUser gcxUser = null;

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

		return gcxUser;
	}

	public GcxUser findGcxUserByEmail(String id)
	{
		GcxUser gcxUser = null;

		if(Strings.isNullOrEmpty(id))
		{
			return null;
		}

		try
		{
			gcxUser = userManager.findUserByEmail(id);
		}
		catch(Exception e)
		{
			logger.info("find by email " + id + " exception " + e.getMessage());
		}

		return gcxUser;
	}

	public GcxUser getGcxUser(RelayUser relayUser)
	{
		final GcxUser gcxUser = relayUser.toGcxUser();

		setGcxMetaData(gcxUser);

		return gcxUser;
	}

	public void setGcxMetaData(GcxUser gcxUser)
	{
		gcxUser.setSignupKey(TheKeyBeans.getRandomStringGenerator().getNewString());

		gcxUser.setPasswordAllowChange(true);
		gcxUser.setForcePasswordChange(false);
		gcxUser.setLoginDisabled(false);
		gcxUser.setVerified(false);
	}

	public abstract class MatchDifferentGcxUsersException extends Exception
	{
		public MatchDifferentGcxUsersException()
		{
		}

		public MatchDifferentGcxUsersException(String message)
		{
			super(message);
		}
	}

	public class MatchDifferentGcxUsersGuidEmailException extends MatchDifferentGcxUsersException
	{
		public MatchDifferentGcxUsersGuidEmailException()
		{
		}

		public MatchDifferentGcxUsersGuidEmailException(String message)
		{
			super(message);
		}
	}

	public class MatchDifferentGcxUsersGuidRelayGuidException extends MatchDifferentGcxUsersException
	{
		public MatchDifferentGcxUsersGuidRelayGuidException()
		{
		}

		public MatchDifferentGcxUsersGuidRelayGuidException(String message)
		{
			super(message);
		}
	}

	public class MatchDifferentGcxUsersRelayGuidEmailException extends MatchDifferentGcxUsersException
	{
		public MatchDifferentGcxUsersRelayGuidEmailException()
		{
		}

		public MatchDifferentGcxUsersRelayGuidEmailException(String message)
		{
			super(message);
		}
	}

	public class MatchDifferentGcxUsersGuidEmailRelayGuidException extends MatchDifferentGcxUsersException
	{
		public MatchDifferentGcxUsersGuidEmailRelayGuidException()
		{
		}

		public MatchDifferentGcxUsersGuidEmailRelayGuidException(String message)
		{
			super(message);
		}
	}
}

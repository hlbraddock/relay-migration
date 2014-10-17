package org.cru.migration.thekey;

import com.google.common.base.Strings;
import me.thekey.cas.service.UserManager;
import org.ccci.gcx.idm.core.model.impl.GcxUser;
import org.cru.migration.domain.RelayUser;
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

    public enum MatchType { GUID, EMAIL, RELAY_GUID, NONE }

    public static class MatchResult
    {
        MatchType matchType = MatchType.NONE;
    }

    public GcxUser findGcxUser(RelayUser relayUser, MatchResult matchResult) throws MatchDifferentGcxUsersGuidEmailException,
			MatchDifferentGcxUsersGuidRelayGuidException, MatchDifferentGcxUsersRelayGuidEmailException
	{
		// search gcx user by various means
		GcxUser gcxUserByGuid = findGcxUserByGuid(relayUser.getSsoguid());
		GcxUser gcxUserByRelayGuid = findGcxUserByRelayGuid(relayUser.getSsoguid());
		GcxUser gcxUserByEmail = findGcxUserByEmail(relayUser.getUsername());

        // if gcx user not found
		if(gcxUserByGuid == null && gcxUserByRelayGuid == null && gcxUserByEmail == null)
		{
			return null;
		}

        if(gcxUserByGuid != null)
        {
            matchResult.matchType = MatchType.GUID;
        }
        else if(gcxUserByEmail != null)
        {
            matchResult.matchType = MatchType.EMAIL;
        }
        else if(gcxUserByRelayGuid != null)
        {
            matchResult.matchType = MatchType.RELAY_GUID;
        }

        // if one gcx user found
		if(Misc.nonNullCount(gcxUserByGuid, gcxUserByRelayGuid, gcxUserByEmail) == 1)
		{
			return (GcxUser) Misc.firstNonNull(gcxUserByGuid, gcxUserByRelayGuid, gcxUserByEmail);
		}

		/*
		 * compare each gcx user found and throw exception if they are not the same user (should have matching ssoguid)
		 */
		if(gcxUserByGuid != null && gcxUserByRelayGuid != null)
		{
			if(!gcxUserByGuid.getGUID().equals(gcxUserByRelayGuid.getGUID()))
			{
				throw new MatchDifferentGcxUsersGuidRelayGuidException(
                        "relay guid matches different gcx users on guid and relay guid");
			}
		}

		if(gcxUserByGuid != null && gcxUserByEmail != null)
		{
			if(!gcxUserByGuid.getGUID().equals(gcxUserByEmail.getGUID()))
			{
				throw new MatchDifferentGcxUsersGuidEmailException(
                        "relay guid matches different gcx users on guid and email"
                );
			}
		}

		if(gcxUserByRelayGuid != null && gcxUserByEmail != null)
		{
			if(!gcxUserByRelayGuid.getGUID().equals(gcxUserByEmail.getGUID()))
			{
				throw new MatchDifferentGcxUsersRelayGuidEmailException(
                        "relay guid matches different gcx users on relay guid and email"
                );
			}
		}

        return (GcxUser) Misc.firstNonNull(gcxUserByGuid, gcxUserByRelayGuid, gcxUserByEmail);
	}

	public abstract class MatchDifferentGcxUsersException extends Exception
	{
        public MatchDifferentGcxUsersException(String message)
        {
            super(message);
        }
    }

	public class MatchDifferentGcxUsersGuidEmailException extends MatchDifferentGcxUsersException
	{
        public MatchDifferentGcxUsersGuidEmailException(String message)
        {
            super(message);
        }
    }

	public class MatchDifferentGcxUsersGuidRelayGuidException extends MatchDifferentGcxUsersException
	{
        public MatchDifferentGcxUsersGuidRelayGuidException(String message)
        {
            super(message);
        }
    }

	public class MatchDifferentGcxUsersRelayGuidEmailException extends MatchDifferentGcxUsersException
	{
        public MatchDifferentGcxUsersRelayGuidEmailException(String message)
        {
            super(message);
        }
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
}

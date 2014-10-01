package org.cru.migration.thekey;

import me.thekey.cas.service.UserManager;
import org.ccci.gcx.idm.core.model.impl.GcxUser;
import org.cru.migration.domain.RelayUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcxUserService
{
	private UserManager userManager;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public GcxUserService(UserManager userManager)
	{
		this.userManager = userManager;
	}

	public GcxUser findGcxUser(RelayUser relayUser) throws MatchDifferentGcxUsersGuidEmailException,
			MatchDifferentGcxUsersGuidRelayGuidException, MatchDifferentGcxUsersRelayGuidEmailException
	{
		GcxUser gcxUserByGuid = findGcxUserByGuid(relayUser.getSsoguid());

		GcxUser gcxUserByRelayGuid = findGcxUserByRelayGuid(relayUser.getSsoguid());

		GcxUser gcxUserByEmail = findGcxUserByEmail(relayUser.getUsername());

		if(gcxUserByGuid == null && gcxUserByRelayGuid == null)
			return gcxUserByEmail;

		if(gcxUserByGuid == null && gcxUserByEmail == null)
			return gcxUserByRelayGuid;

		if(gcxUserByRelayGuid == null && gcxUserByEmail == null)
			return gcxUserByGuid;

		if(gcxUserByGuid != null && gcxUserByRelayGuid != null)
		{
			if(!gcxUserByGuid.getGUID().equals(gcxUserByRelayGuid.getGUID()))
			{
				throw new MatchDifferentGcxUsersGuidRelayGuidException();
			}
		}
		else if(gcxUserByGuid != null && gcxUserByEmail != null)
		{
			if(!gcxUserByGuid.getGUID().equals(gcxUserByEmail.getGUID()))
			{
				throw new MatchDifferentGcxUsersGuidEmailException();
			}
		}
		else if(gcxUserByRelayGuid != null && gcxUserByEmail != null)
		{
			if(!gcxUserByRelayGuid.getGUID().equals(gcxUserByEmail.getGUID()))
			{
				throw new MatchDifferentGcxUsersRelayGuidEmailException();
			}
		}

		return null;
	}

	public class MatchDifferentGcxUsersException extends Exception
	{
	}

	public class MatchDifferentGcxUsersGuidEmailException extends MatchDifferentGcxUsersException
	{
	}

	public class MatchDifferentGcxUsersGuidRelayGuidException extends MatchDifferentGcxUsersException
	{
	}

	public class MatchDifferentGcxUsersRelayGuidEmailException extends MatchDifferentGcxUsersException
	{
	}

	public GcxUser findGcxUserByGuid(String ssoguid)
	{
		GcxUser gcxUser = null;

		try
		{
			gcxUser = userManager.findUserByGuid(ssoguid);
		}
		catch(Exception e)
		{
			logger.info("find by ssoguid " + ssoguid + " exception " + e.getMessage());
		}

		return gcxUser;
	}

	public GcxUser findGcxUserByRelayGuid(String ssoguid)
	{
		GcxUser gcxUser = null;

		try
		{
			gcxUser = userManager.findUserByRelayGuid(ssoguid);
		}
		catch(Exception e)
		{
			logger.info("find by relay ssoguid " + ssoguid + " exception " + e.getMessage());
		}

		return gcxUser;
	}

	public GcxUser findGcxUserByEmail(String email)
	{
		GcxUser gcxUser = null;

		try
		{
			gcxUser = userManager.findUserByEmail(email);
		}
		catch(Exception e)
		{
			logger.info("find by email " + email + " exception " + e.getMessage());
		}

		return gcxUser;
	}

	public GcxUser getGcxUser(RelayUser relayUser)
	{
		final GcxUser gcxUser = relayUser.toGcxUser();

		return setMetaData(gcxUser);
	}

	public GcxUser setMetaData(GcxUser gcxUser)
	{
		gcxUser.setSignupKey(TheKeyBeans.getRandomStringGenerator().getNewString());

		gcxUser.setPasswordAllowChange(true);
		gcxUser.setForcePasswordChange(false);
		gcxUser.setLoginDisabled(false);
		gcxUser.setVerified(false);

		return gcxUser;
	}
}

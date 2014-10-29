package org.cru.migration.service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import me.thekey.cas.service.UserAlreadyExistsException;
import me.thekey.cas.service.UserManager;
import org.apache.commons.lang.StringUtils;
import org.ccci.gcx.idm.core.model.impl.GcxUser;
import org.cru.migration.domain.RelayGcxUsers;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.support.FileHelper;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;
import org.cru.migration.support.StringUtilities;
import org.cru.migration.thekey.GcxUserService;
import org.cru.migration.thekey.TheKeyBeans;
import org.cru.silc.service.LinkingServiceImpl;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProvisionUsersService
{
	private MigrationProperties properties;

	private Logger logger = LoggerFactory.getLogger(getClass());

	private UserManager userManagerMerge;

	private GcxUserService gcxUserService;

	Set<RelayUser> relayUsersProvisioned = Sets.newSetFromMap(new ConcurrentHashMap<RelayUser, Boolean>());
	Set<GcxUser> gcxUsersProvisioned = Sets.newSetFromMap(new ConcurrentHashMap<GcxUser, Boolean>());
	Map<RelayUser, Exception> relayUsersFailedToProvision = Maps.newConcurrentMap();
	Map<GcxUser, Exception> gcxUsersFailedToProvision = Maps.newConcurrentMap();
	Map<RelayUser, GcxUser> matchingRelayGcxUsers = Maps.newConcurrentMap();
	Set<RelayUser> relayUsersMatchedMoreThanOneGcxUser = Sets.newSetFromMap(new ConcurrentHashMap<RelayUser,
			Boolean>());

	Set<RelayGcxUsers> relayUsersWithGcxUsersMatchedMoreThanOneGcxUser = Sets.newSetFromMap(new
			ConcurrentHashMap<RelayGcxUsers, Boolean>());

	Set<RelayGcxUsers> relayUsersWithGcxMatchAndGcxUsers = Sets.newSetFromMap(new
			ConcurrentHashMap<RelayGcxUsers, Boolean>());

	Set<RelayGcxUsers> userAlreadyExists = Sets.newSetFromMap(new
			ConcurrentHashMap<RelayGcxUsers, Boolean>());

	Boolean provisionUsers;
	Boolean provisioningFailureStackTrace;
	Boolean logProvisioningRealTime;
	Integer provisionUsersLimit;

	File provisioningRelayUsersFile;
	File failingProvisioningRelayUsersFile;

	public ProvisionUsersService(MigrationProperties properties) throws Exception
	{
		this.properties = properties;

		UserManager userManager = TheKeyBeans.getUserManager();
		userManagerMerge = TheKeyBeans.getUserManagerMerge();

		LinkingServiceImpl linkingServiceImpl = new LinkingServiceImpl();
		linkingServiceImpl.setResource(properties.getNonNullProperty("identityLinkingResource"));
		linkingServiceImpl.setIdentitiesAccessToken(properties.getNonNullProperty("identityLinkingAccessToken"));

		gcxUserService = new GcxUserService(userManager, linkingServiceImpl);

		provisionUsers = Boolean.valueOf(properties.getNonNullProperty("provisionUsers"));
		provisioningFailureStackTrace = Boolean.valueOf(properties.getNonNullProperty
				("provisioningFailureStackTrace"));
		logProvisioningRealTime = Boolean.valueOf(properties.getNonNullProperty
				("logProvisioningRealTime"));
		provisionUsersLimit = Integer.valueOf(properties.getNonNullProperty
				("provisionUsersLimit"));

		provisioningRelayUsersFile = FileHelper.getFileToWrite(properties.getNonNullProperty("relayUsersProvisioning"));
		failingProvisioningRelayUsersFile = FileHelper.getFileToWrite(properties.getNonNullProperty
				("relayUsersFailingProvisioning"));
	}

	public void provisionUsers(Set<RelayUser> relayUsers)
	{
		logger.info("provisioning relay users to the key of size " + relayUsers.size());

		int counter = 0;
		Long totalProvisioningTime = 0L;
		DateTime start = DateTime.now();

		ExecutorService executorService = Executors.newFixedThreadPool(400);

		for(RelayUser relayUser : relayUsers)
		{
			Runnable worker = new WorkerThread(relayUser);

			executorService.execute(worker);

			counter++;
			if (provisionUsersLimit > 0 && (counter >= provisionUsersLimit))
			{
				break;
			}
		}

		executorService.shutdown();

		logger.info("provisioning relay users done firing off worker threads");

		try
		{
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException e)
		{
			logger.error("provisioning relay users executor service exception on awaitTermination() " + e);
		}

		totalProvisioningTime += (new Duration(start, DateTime.now())).getMillis();
		logger.info("provisioned " + counter + " users at an average milliseconds of (" +
				totalProvisioningTime + "/" + counter + ")" + totalProvisioningTime / counter + " per user " +
				"and a total of " + StringUtilities.toString(new Duration(totalProvisioningTime)));

		logger.info("provisioning relay users to the key done ");

		try
		{
			Output.serializeRelayUsers(relayUsersProvisioned,
					properties.getNonNullProperty("relayUsersProvisioned"));
			Output.logGcxUsers(gcxUsersProvisioned,
					FileHelper.getFileToWrite(properties.getNonNullProperty("gcxUsersProvisioned")));
			Output.serializeRelayUsers(relayUsersFailedToProvision,
					properties.getNonNullProperty("relayUsersFailedToProvision"));
			Output.logGcxUsers(gcxUsersFailedToProvision,
					FileHelper.getFileToWrite(properties.getNonNullProperty("gcxUsersFailedToProvision")));
			Output.logRelayGcxUsersMap(matchingRelayGcxUsers,
					properties.getNonNullProperty("matchingRelayGcxUsers"));

			logger.info("Size of relayUsersMatchedMoreThanOneGcxUser " +
					relayUsersMatchedMoreThanOneGcxUser.size());
			Output.serializeRelayUsers(relayUsersMatchedMoreThanOneGcxUser,
					properties.getNonNullProperty("relayUsersMatchedMoreThanOneGcxUser"));

			logger.info("Size of relayUsersWithGcxUsersMatchedMoreThanOneGcxUser " +
					relayUsersWithGcxUsersMatchedMoreThanOneGcxUser.size());
			Output.logRelayUserGcxUsers(relayUsersWithGcxUsersMatchedMoreThanOneGcxUser,
					FileHelper.getFileToWrite(properties.getNonNullProperty
							("relayUsersWithGcxUsersMatchedMoreThanOneGcxUser")));

			logger.info("Size of userAlreadyExists " + userAlreadyExists.size());
			Output.logRelayUserGcxUsers(userAlreadyExists,
					FileHelper.getFileToWrite(properties.getNonNullProperty("userAlreadyExists")));

			logger.info("Size of relayUsersWithGcxMatchAndGcxUsers " +
					relayUsersWithGcxMatchAndGcxUsers.size());
			Output.logRelayUserGcxUsers(relayUsersWithGcxMatchAndGcxUsers,
					FileHelper.getFileToWrite(properties.getNonNullProperty("relayUsersWithGcxMatchAndGcxUsers")));
		}
		catch (Exception e)
		{
			logger.error("Caught final exception " + e);
			e.printStackTrace();
		}
	}

	private class WorkerThread implements Runnable
	{
		private RelayUser relayUser;

		private WorkerThread(RelayUser relayUser)
		{
			this.relayUser = relayUser;
		}

		@Override
		public void run()
		{
			if(logger.isTraceEnabled())
			{
				logger.trace(Thread.currentThread().getName() + " Start ");
			}

			processCommand();

			if(logger.isTraceEnabled())
			{
				logger.trace(Thread.currentThread().getName() + " End ");
			}
		}

		private void processCommand()
		{
			GcxUser gcxUser = null;
			Set<GcxUser> gcxUsers = null;

			DateTime startLookup = null;
			DateTime startProvisioning = null;
			GcxUserService.MatchResult matchResult = null;

			try
			{
				// "reset" gcx user
				gcxUser = null;

				if(logger.isTraceEnabled())
				{
					startLookup = DateTime.now();
				}

				// TODO capture match result somewhere
				matchResult = new GcxUserService.MatchResult();
				gcxUsers = gcxUserService.findGcxUsers(relayUser, matchResult);
				gcxUser = gcxUserService.resolveGcxUser(relayUser, matchResult, gcxUsers);

				if (logger.isTraceEnabled())
				{
					logDuration(startLookup, "gcx user lookup : ");
				}

				// if matching gcx user found
				if(gcxUser != null)
				{
					relayUsersWithGcxMatchAndGcxUsers.add(new RelayGcxUsers(relayUser, gcxUser, gcxUsers, matchResult));
					matchingRelayGcxUsers.put(relayUser, gcxUser);

					if(relayUser.isAuthoritative())
					{
						relayUser.setGcxUserFromRelayIdentity(gcxUser);
					}

					relayUser.setGcxUserFromRelayAttributes(gcxUser);
				}
				else
				{
					gcxUser = gcxUserService.getGcxUser(relayUser);
				}

				if(provisionUsers)
				{
					if(logger.isTraceEnabled())
					{
						logger.trace("user manager create user " + gcxUser.toString());
					}

					if(logger.isTraceEnabled())
					{
						startProvisioning = DateTime.now();
					}

					userManagerMerge.createUser(gcxUser);

					if (logger.isTraceEnabled())
					{
						logDuration(startProvisioning, "provisioned user : ");
					}

					gcxUsersProvisioned.add(gcxUser);
					relayUsersProvisioned.add(relayUser);

					if(logProvisioningRealTime)
					{
						Output.logMessage(StringUtils.join(relayUser.toList(), ","), provisioningRelayUsersFile);
					}
				}
			}
			catch(GcxUserService.MatchDifferentGcxUsersException matchDifferentGcxUsersException)
			{
				relayUsersMatchedMoreThanOneGcxUser.add(relayUser);
				relayUsersWithGcxUsersMatchedMoreThanOneGcxUser.add(new RelayGcxUsers(relayUser, gcxUsers,
						matchDifferentGcxUsersException));

				for(GcxUser fromGcxUsers : gcxUsers)
				{
					if(fromGcxUsers != null) // TODO find out why this could be null
					{
						gcxUsersFailedToProvision.put(fromGcxUsers, matchDifferentGcxUsersException);
					}
				}

				Output.logMessage(StringUtils.join(relayUser.toList(), "," +
						"") + " match different users exception " + matchDifferentGcxUsersException
						.getMessage(), failingProvisioningRelayUsersFile);

				if(provisioningFailureStackTrace)
				{
					matchDifferentGcxUsersException.printStackTrace();
				}
			}
			catch(UserAlreadyExistsException userAlreadyExistsException)
			{
				userAlreadyExists.add(new RelayGcxUsers(relayUser, gcxUser, gcxUsers, matchResult));
			}
			catch (Exception e)
			{
				relayUsersFailedToProvision.put(relayUser, e);
				if(gcxUser != null)
				{
					gcxUsersFailedToProvision.put(gcxUser, e);
				}
				Output.logMessage(StringUtils.join(relayUser.toList(), ",") + " " + e.getMessage() + "," +
						e.getCause() + "," + e.toString(), failingProvisioningRelayUsersFile);
				if(provisioningFailureStackTrace)
				{
					e.printStackTrace();
				}
			}
		}
	}

	private void logDuration(DateTime start, String message)
	{
		Duration duration = new Duration(start, DateTime.now());

		logger.trace(message + StringUtilities.toString(duration));
	}
}

package org.cru.migration.service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.ccci.idm.user.User;
import org.ccci.idm.user.UserAlreadyExistsException;
import org.ccci.idm.user.UserManager;
import org.cru.migration.domain.RelayGcxUsers;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.service.execution.ExecuteAction;
import org.cru.migration.service.execution.ExecutionService;
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

import javax.naming.NamingException;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ProvisionUsersService
{
	private MigrationProperties properties;

	private Logger logger = LoggerFactory.getLogger(getClass());

	private UserManager userManagerMerge;

	private GcxUserService gcxUserService;

	Set<RelayUser> relayUsersProvisioned = Sets.newSetFromMap(new ConcurrentHashMap<RelayUser, Boolean>());
	Set<User> gcxUsersProvisioned = Sets.newSetFromMap(new ConcurrentHashMap<User, Boolean>());
	Map<RelayUser, Exception> relayUsersFailedToProvision = Maps.newConcurrentMap();
	Map<User, Exception> gcxUsersFailedToProvision = Maps.newConcurrentMap();
	Map<RelayUser, User> matchingRelayGcxUsers = Maps.newConcurrentMap();
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

	private class ProvisionUsersData
	{
		private AtomicInteger counter;
		private Set<RelayUser> relayUsers;

		private ProvisionUsersData(AtomicInteger counter, Set<RelayUser> relayUsers)
		{
			this.counter = counter;
			this.relayUsers = relayUsers;
		}

		public AtomicInteger getCounter()
		{
			return counter;
		}

		public void incrementCounter()
		{
			counter.getAndAdd(1);
		}

		public Set<RelayUser> getRelayUsers()
		{
			return relayUsers;
		}
	}

	public void provisionUsers(Set<RelayUser> relayUsers) throws NamingException
	{
		logger.info("provisioning relay users to the key of size " + relayUsers.size());

		Long totalProvisioningTime = 0L;
		DateTime start = DateTime.now();

		ExecutionService executionService = new ExecutionService();

		ProvisionUsersData provisionUsersData = new ProvisionUsersData(new AtomicInteger(0), relayUsers);
		executionService.execute(new ProvisionUsers(), provisionUsersData, 400);

		Integer counter = provisionUsersData.getCounter().get();
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

	public class ProvisionUsers implements ExecuteAction
	{
		@Override
		public void execute(ExecutorService executorService, Object object)
		{
			ProvisionUsersData provisionUsersData = (ProvisionUsersData)object;

			for(RelayUser relayUser : provisionUsersData.getRelayUsers())
			{
				executorService.execute(new ProvisionUsersWorkerThread(relayUser));

				provisionUsersData.incrementCounter();
				if (provisionUsersLimit > 0 && (provisionUsersData.getCounter().get() >= provisionUsersLimit))
				{
					break;
				}
			}
		}
	}

	private class ProvisionUsersWorkerThread implements Runnable
	{
		private RelayUser relayUser;

		private ProvisionUsersWorkerThread(RelayUser relayUser)
		{
			this.relayUser = relayUser;
		}

		@Override
		public void run()
		{
			User gcxUser = null;
			Set<User> gcxUsers = null;

			DateTime startLookup = null;
			DateTime startProvisioning = null;
			GcxUserService.MatchResult matchResult = null;

			try
			{
				if(logger.isTraceEnabled())
				{
					startLookup = DateTime.now();
				}

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
						relayUser.setUserFromRelayIdentity(gcxUser);
					}

					relayUser.setUserFromRelayAttributes(gcxUser);
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

				for(User fromGcxUsers : gcxUsers)
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

package org.cru.migration.domain;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.cru.migration.support.CaseInsensitiveRelayUserMap;
import org.cru.migration.support.MigrationProperties;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class RelayUserGroups
{
	MigrationProperties migrationProperties = new MigrationProperties();

	Boolean provisionUSStaff = Boolean.valueOf(migrationProperties.getNonNullProperty("provisionUSStaff"));
	Boolean provisionGoogleUsers = Boolean.valueOf(migrationProperties.getNonNullProperty("provisionGoogleUsers"));
	Boolean provisionNonUSStaff = Boolean.valueOf(migrationProperties.getNonNullProperty("provisionNonUSStaff"));

    private Boolean updateAllUsers = true;

	private Set<RelayUser> usStaff = Sets.newHashSet();
	private Set<RelayUser> googleUsers = Sets.newHashSet();

	private Set<RelayUser> loggedIn;
	private Set<RelayUser> notLoggedIn;

	private Set<RelayUser> withPassword;
	private Set<RelayUser> withoutPassword;

	private Set<RelayUser> usStaffInGoogle = Sets.newHashSet();
	private Set<RelayUser> usStaffNotInGoogle = Sets.newHashSet();

	private Set<RelayUser> googleUserUSStaff = Sets.newHashSet();
	private Set<RelayUser> googleUserNotUSStaff = Sets.newHashSet();

	private Set<RelayUser> allRelayUsers = Sets.newHashSet();

	private Set<RelayUser> allUsers = Sets.newConcurrentHashSet();

	private Set<RelayUser> nonStaffUsers = Sets.newHashSet();

	private Set<RelayUser> googleUsersNotUSStaffHavingEmployeeId = Sets.newHashSet();
	private Set<RelayUser> googleUsersNotUSStaffNotHavingEmployeeId = Sets.newHashSet();

	private Set<RelayUser> notFoundInCasAuditLog = Sets.newHashSet();

	private Set<RelayUser> serializedRelayUsers = Sets.newHashSet();

	private Set<String> multipleRelayUsersMatchingKeyUser = Sets.newHashSet();

	private Map<String, Set<RelayUser>> keyUserMatchingRelayUsers = Maps.newHashMap();

	private DateTime loggedInSince;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public Set<RelayUser> getUsStaff()
	{
		return usStaff;
	}

	public void setUsStaff(Set<RelayUser> usStaff)
	{
		this.usStaff = usStaff;

        updateAllUsers = true;
	}

	public Set<RelayUser> getGoogleUsers()
	{
		return googleUsers;
	}

	public void setGoogleUsers(Set<RelayUser> googleUsers)
	{
		this.googleUsers = googleUsers;

        updateAllUsers = true;
	}

	public Set<RelayUser> getNonStaffUsers()
	{
		return nonStaffUsers;
	}

	public void setNonStaffUsers(Set<RelayUser> nonStaffUsers)
	{
		this.nonStaffUsers = nonStaffUsers;

        updateAllUsers = true;
	}

	private Map<String, RelayUser> allUsersSsoguidKey = new CaseInsensitiveRelayUserMap();
	public Map<String, RelayUser> getAllUsersSsoguidKey()
	{
		getAllUsers();
		return allUsersSsoguidKey;
	}

	private Map<String, RelayUser> allUsersUsernameKey = new CaseInsensitiveRelayUserMap();
	public Map<String, RelayUser> getAllUsersUsernameKey()
	{
		getAllUsers();
		return allUsersUsernameKey;
	}

	public Set<RelayUser> getAllUsers()
	{
		logger.info("getAllUsers() " + updateAllUsers + " all users size before is " + allUsers.size());

		if(updateAllUsers)
        {
            allUsers.clear();

			logger.info("getAllUsers() " + provisionUSStaff + " us staff size  is " + usStaff.size());

			if(provisionUSStaff)
            {
                allUsers.addAll(usStaff);
            }

			logger.info("getAllUsers() " + provisionGoogleUsers + " google users size  is " + googleUsers.size());

			if(provisionGoogleUsers)
            {
                allUsers.addAll(googleUsers);

				logger.info("Checking each all users (staff) current size " + allUsers.size() + " against google " +
						"users ...");

				int markedGoogle = 0;

				//
				// ensure google relay users marked
				// if they were already in the all users, then the google users wouldn't be added in which case those
				// users would not yet be marked google users
				//

				for(RelayUser relayUser : allUsers)
				{
					if(googleUsers.contains(relayUser))
					{
						markedGoogle++;
						relayUser.setGoogle(true);
					}
				}

				logger.info("Done checking all users (staff) current size " + allUsers.size() + " against google " +
						"users and marked google users total " + markedGoogle);
			}

			logger.info("getAllUsers() " + provisionNonUSStaff + " non us staff size  is " + nonStaffUsers.size());

			if(provisionNonUSStaff)
            {
                allUsers.addAll(nonStaffUsers);
            }

            updateAllUsers = false;

			for (RelayUser relayUser : allUsers) {
				allUsersSsoguidKey.put(relayUser.getSsoguid(), relayUser);
				allUsersUsernameKey.put(relayUser.getUsername(), relayUser);
			}
		}

		logger.info("getAllUsers() " + updateAllUsers + " all users size returning is " + allUsers.size());

		return allUsers;
	}

	public Set<RelayUser> getLoggedIn()
	{
		return loggedIn;
	}

	public void setLoggedIn(Set<RelayUser> loggedIn)
	{
		this.loggedIn = loggedIn;
	}

	public Set<RelayUser> getNotLoggedIn()
	{
		return notLoggedIn;
	}

	public void setNotLoggedIn(Set<RelayUser> notLoggedIn)
	{
		this.notLoggedIn = notLoggedIn;
	}

	public Set<RelayUser> getWithPassword()
	{
		return withPassword;
	}

	public void setWithPassword(Set<RelayUser> withPassword)
	{
		this.withPassword = withPassword;
	}

	public Set<RelayUser> getWithoutPassword()
	{
		return withoutPassword;
	}

	public void setWithoutPassword(Set<RelayUser> withoutPassword)
	{
		this.withoutPassword = withoutPassword;
	}

	public Set<RelayUser> getUsStaffInGoogle()
	{
		return usStaffInGoogle;
	}

	public void setUsStaffInGoogle(Set<RelayUser> usStaffInGoogle)
	{
		this.usStaffInGoogle = usStaffInGoogle;
	}

	public Set<RelayUser> getUsStaffNotInGoogle()
	{
		return usStaffNotInGoogle;
	}

	public void setUsStaffNotInGoogle(Set<RelayUser> usStaffNotInGoogle)
	{
		this.usStaffNotInGoogle = usStaffNotInGoogle;
	}

	public Set<RelayUser> getGoogleUserUSStaff()
	{
		return googleUserUSStaff;
	}

	public void setGoogleUserUSStaff(Set<RelayUser> googleUserUSStaff)
	{
		this.googleUserUSStaff = googleUserUSStaff;
	}

	public Set<RelayUser> getGoogleUserNotUSStaff()
	{
		return googleUserNotUSStaff;
	}

	public void setGoogleUserNotUSStaff(Set<RelayUser> googleUserNotUSStaff)
	{
		this.googleUserNotUSStaff = googleUserNotUSStaff;
	}

	public Set<RelayUser> getGoogleUsersNotUSStaffHavingEmployeeId()
	{
		return googleUsersNotUSStaffHavingEmployeeId;
	}

	public void setGoogleUsersNotUSStaffHavingEmployeeId(Set<RelayUser> googleUsersNotUSStaffHavingEmployeeId)
	{
		this.googleUsersNotUSStaffHavingEmployeeId = googleUsersNotUSStaffHavingEmployeeId;
	}

	public Set<RelayUser> getGoogleUsersNotUSStaffNotHavingEmployeeId()
	{
		return googleUsersNotUSStaffNotHavingEmployeeId;
	}

	public void setGoogleUsersNotUSStaffNotHavingEmployeeId(Set<RelayUser> googleUsersNotUSStaffNotHavingEmployeeId)
	{
		this.googleUsersNotUSStaffNotHavingEmployeeId = googleUsersNotUSStaffNotHavingEmployeeId;
	}

	public DateTime getLoggedInSince()
	{
		return loggedInSince;
	}

	public void setLoggedInSince(DateTime loggedInSince)
	{
		this.loggedInSince = loggedInSince;
	}

	public Set<RelayUser> getNotFoundInCasAuditLog()
	{
		return notFoundInCasAuditLog;
	}

	public void setNotFoundInCasAuditLog(Set<RelayUser> notFoundInCasAuditLog)
	{
		this.notFoundInCasAuditLog = notFoundInCasAuditLog;
	}

	public Set<RelayUser> getSerializedRelayUsers()
	{
		return serializedRelayUsers;
	}

	public void setSerializedRelayUsers(Set<RelayUser> serializedRelayUsers)
	{
		this.serializedRelayUsers = serializedRelayUsers;
	}

	public Set<RelayUser> getAllRelayUsers()
	{
		return allRelayUsers;
	}

	public void setAllRelayUsers(Set<RelayUser> allRelayUsers)
	{
		this.allRelayUsers = allRelayUsers;
	}

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

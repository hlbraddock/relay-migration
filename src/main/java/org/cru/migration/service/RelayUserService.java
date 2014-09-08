package org.cru.migration.service;

import com.google.common.collect.Sets;
import org.cru.migration.dao.CssDao;
import org.cru.migration.domain.CssRelayUser;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.support.FileHelper;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.support.Output;

import java.io.IOException;
import java.util.Set;

public class RelayUserService
{
	private CssDao cssDao;
	private MigrationProperties migrationProperties = new MigrationProperties();

	public void setPasswords(Set<RelayUser> relayUsers) throws IOException
	{
		Output.println("Set Relay user passwords");

		Output.println("Relay user size is " + relayUsers.size());

		Set<CssRelayUser> cssRelayUsers = cssDao.getCssRelayUsers(RelayUser.getSsoguids(relayUsers));

		Output.println("CSS relay users size is " + cssRelayUsers.size());

		Output.println("Setting relay users passwords ...");

		RelayUser relayUser;
		Set<RelayUser> relayUsersWithPasswordSet = Sets.newHashSet();
		for(CssRelayUser cssRelayUser : cssRelayUsers)
		{
			relayUser = RelayUser.havingSsoguid(relayUsers, cssRelayUser.getSsoguid());

			if(relayUser != null)
			{
				relayUser.setPassword(cssRelayUser.getPassword());
				relayUsersWithPasswordSet.add(relayUser);
			}
		}

		Output.println("Done setting relay users passwords.");

		Set<RelayUser> relayUsersWithoutPasswordSet = Sets.newHashSet();
		relayUsersWithoutPasswordSet.addAll(relayUsers);
		relayUsersWithoutPasswordSet.removeAll(relayUsersWithPasswordSet);

		Output.println("Relay users with password set size " + relayUsersWithPasswordSet.size());
		Output.println("Relay users without password set size " + relayUsersWithoutPasswordSet.size());
		Output.logRelayUser(relayUsersWithPasswordSet,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersWithPasswordSet")));
		Output.logRelayUser(relayUsersWithoutPasswordSet,
				FileHelper.getFile(migrationProperties.getNonNullProperty("relayUsersWithoutPasswordSet")));
	}

	public CssDao getCssDao()
	{
		return cssDao;
	}

	public void setCssDao(CssDao cssDao)
	{
		this.cssDao = cssDao;
	}
}

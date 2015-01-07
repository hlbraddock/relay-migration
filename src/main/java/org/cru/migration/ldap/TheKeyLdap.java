package org.cru.migration.ldap;

import com.google.common.collect.Maps;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.user.User;
import org.ccci.idm.user.exception.UserException;
import org.ccci.idm.user.UserManager;
import org.cru.migration.dao.LdapDao;
import org.cru.migration.domain.RelayUser;
import org.cru.migration.service.ProvisionUsersService;
import org.cru.migration.service.RemoveEntriesService;
import org.cru.migration.support.MigrationProperties;
import org.cru.migration.thekey.GcxUserService;
import org.cru.migration.thekey.TheKeyBeans;
import org.cru.silc.service.LinkingServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TheKeyLdap
{
	private MigrationProperties properties;

	private Ldap ldap;

	private LdapDao ldapDao;

	private UserManager userManagerMerge;

	private GcxUserService gcxUserService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public TheKeyLdap(MigrationProperties properties) throws Exception
	{
		this.properties = properties;

        Boolean eDirectoryAvailable = Boolean.valueOf(properties.getNonNullProperty("eDirectoryAvailable"));
        UserManager userManager = null;
        if(eDirectoryAvailable)
        {
            ldap = new Ldap(properties.getNonNullProperty("theKeyLdapHost"),
                    properties.getNonNullProperty("theKeyLdapUser"),
                    properties.getNonNullProperty("theKeyLdapPassword"));
            ldapDao = new LdapDao(ldap);

            userManager = TheKeyBeans.getUserManager();
            userManagerMerge = TheKeyBeans.getUserManagerMerge();
        }


		LinkingServiceImpl linkingServiceImpl = new LinkingServiceImpl();
		linkingServiceImpl.setResource(properties.getNonNullProperty("identityLinkingResource"));
		linkingServiceImpl.setIdentitiesAccessToken(properties.getNonNullProperty("identityLinkingAccessToken"));

		gcxUserService = new GcxUserService(userManager, linkingServiceImpl);
	}

	public void createUser(RelayUser relayUser) throws UserException
	{
		UserManager userManager = TheKeyBeans.getUserManager();

		User gcxUser = gcxUserService.getGcxUserFromRelayUser(relayUser, relayUser.getSsoguid());

		userManager.createUser(gcxUser);
	}

    public User getUser(String email) throws NamingException
    {
        return userManagerMerge.findUserByEmail(email,false);
    }

    public Integer getMergeUserCount() throws NamingException
    {
        String theKeyMergeUserRootDn = properties.getNonNullProperty("theKeyMergeUserRootDn");

        return ldapDao.getUserCount(theKeyMergeUserRootDn, "cn");
    }

	public void provisionUsers(Set<RelayUser> relayUsers, Map<String, Set<RelayUser>> keyUserMatchingRelayUsers) throws
            Exception
	{
		ProvisionUsersService provisionUsersService = new ProvisionUsersService(properties);

		provisionUsersService.provisionUsers(relayUsers, keyUserMatchingRelayUsers);
	}

    public Map<String, Attributes> getSourceEntries() throws NamingException
    {
        String theKeySourceUserRootDn = properties.getNonNullProperty("theKeySourceUserRootDn");

        List<String> returnAttributes = Arrays.asList("cn", "theKeyGuid");

        return ldapDao.getEntries(theKeySourceUserRootDn, "cn", returnAttributes.toArray(new String[returnAttributes.size()]), 3);
    }

    public Map<String, Attributes> getMergeEntries() throws NamingException
    {
        String theKeyMergeUserRootDn = properties.getNonNullProperty("theKeyMergeUserRootDn");

        return ldapDao.getEntries(theKeyMergeUserRootDn, "cn", new String[]{}, 3);
    }

    public void removeEntries(Map<String, Attributes> entries) throws NamingException
	{
		RemoveEntriesService removeEntriesService = new RemoveEntriesService(ldap);

		removeEntriesService.removeEntries(entries);
	}

	private static List<String> systems =
			Arrays.asList("AnswersBindUser", "ApacheBindUser", "CCCIBIPUSER", "CCCIBIPUSERCNCPRD", "relayCasBindUser",
					"CssUnhashPwd", "DssUcmUser", "EFTBIPUSER", "EasyVista", "GADS", "GUESTCNC", "GUESTCNCPRD",
					"GuidUsernameLookup", "IdentityLinking", "LDAPUSER", "LDAPUSERCNCPRD",
					"MigrationProcess", "Portfolio", "PshrReconUser", "RulesService", "SADMIN", "SADMINCNCPRD",
					"SHAREDCNC", "SHAREDCNCOUIPRD", "SHAREDCNCPRD", "SHAREDCNCSTG", "SHAREDCNCTST",
					"relaySelfService",
					"StellentSystem");

	private List<String> cruPersonAttributeNames = Arrays.asList("cruDesignation", "cruEmployeeStatus", "cruGender",
			"cruHrStatusCode", "cruJobCode", "cruManagerID", "cruMinistryCode", "cruPayGroup",
			"cruPreferredName", "cruSubMinistryCode", "proxyAddresses");

	private List<String> relayAttributeNames = Arrays.asList("relayGuid");

	private final static String cruPersonObjectId = "1.3.6.1.4.1.100.100.100.1.1.1";
	private final static String relayAttributesObjectId = "1.3.6.1.4.1.100.100.101.1.1.1";

    private static List<String> googleGroups =
            Arrays.asList(
                    "CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Cru,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Cru,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Policies,CN=Cru,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Forward,CN=Policies,CN=Cru,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Forward,CN=Policies,CN=Cru,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mobile,CN=Policies,CN=Cru,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Mobile,CN=Policies,CN=Cru,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=TwoFactor,CN=Policies,CN=Cru,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=TwoFactor,CN=Policies,CN=Cru,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=JesusFilm,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=JesusFilm,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Policies,CN=JesusFilm,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mobile,CN=Policies,CN=JesusFilm,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Mobile,CN=Policies,CN=JesusFilm,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Forward,CN=Policies,CN=JesusFilm,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Forward,CN=Policies,CN=JesusFilm,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=MobileUnencrypted,CN=Policies,CN=JesusFilm,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=AIA,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=AIA,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Policies,CN=AIA,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mobile,CN=Policies,CN=AIA,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Mobile,CN=Policies,CN=AIA,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Forward,CN=Policies,CN=AIA,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Forward,CN=Policies,CN=AIA,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=MobileUnencrypted,CN=Policies,CN=AIA,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Keynote,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Keynote,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Policies,CN=Keynote,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mobile,CN=Policies,CN=Keynote,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Mobile,CN=Policies,CN=Keynote,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Forward,CN=Policies,CN=Keynote,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Forward,CN=Policies,CN=Keynote,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=MobileUnencrypted,CN=Policies,CN=Keynote,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=AgapeItalia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=AgapeItalia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Policies,CN=AgapeItalia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mobile,CN=Policies,CN=AgapeItalia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Mobile,CN=Policies,CN=AgapeItalia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=TwoFactor,CN=Policies,CN=AgapeItalia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=TwoFactor,CN=Policies,CN=AgapeItalia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Forward,CN=Policies,CN=AgapeItalia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Forward,CN=Policies,CN=AgapeItalia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=MobileUnencrypted,CN=Policies,CN=AgapeItalia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=MilitaryMinistry,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=MilitaryMinistry,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Policies,CN=MilitaryMinistry,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mobile,CN=Policies,CN=MilitaryMinistry,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Mobile,CN=Policies,CN=MilitaryMinistry,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Forward,CN=Policies,CN=MilitaryMinistry,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Forward,CN=Policies,CN=MilitaryMinistry,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=MobileUnencrypted,CN=Policies,CN=MilitaryMinistry,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=NewLifeRussia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Policies,CN=NewLifeRussia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Forward,CN=Policies,CN=NewLifeRussia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Forward,CN=Policies,CN=NewLifeRussia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mobile,CN=Policies,CN=NewLifeRussia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Mobile,CN=Policies,CN=NewLifeRussia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=NewLifeRussia,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=FamilyLife,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=FamilyLife,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Policies,CN=FamilyLife,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=TwoFactor,CN=Policies,CN=FamilyLife,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=TwoFactor,CN=Policies,CN=FamilyLife,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Forward,CN=Policies,CN=FamilyLife,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Forward,CN=Policies,CN=FamilyLife,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mobile,CN=Policies,CN=FamilyLife,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Mail,CN=Mobile,CN=Policies,CN=FamilyLife,CN=Cru,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Google Admins,CN=GoogleApps,CN=Groups,CN=idm,DC=cru,DC=org"
                    );

    private static List<String> googleGroupNames =
            Arrays.asList("Mail", "Google Admins");

    private static List<String> stellentOrganizationalUnits =
            Arrays.asList(
                    "CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org"
            );

    private static List<String> stellentGroups =
            Arrays.asList(
                    "CN=Private_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=UCMAllAccounts,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-Comm_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-Comm_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-FSG_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-FSG_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-FundDev_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-FundDev_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-8_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-HRX_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-8_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-HRX_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-9_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-MPD_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-9_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-MPD_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-SS_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-SS_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-Cru-Leadership_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-Comm-Photo_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-Cru-Leadership_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-Comm-Photo_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-Cru_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-Comm-Web_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-Cru_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-Comm-Web_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-1-TH-13_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-HRX-Community_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-1-TH-13_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-HRX-Community_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-1-TH_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-HRX-USStaffOverseas_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-1-TH_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-LH-HRX-USStaffOverseas_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-1_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LH_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-1_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LH_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-8_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LH-Comm_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-8_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LH-Comm_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-15_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LH-MPD_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-15_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LH-MPD_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-19_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LH-Comm-Photo_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-19_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LH-Comm-Photo_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LH-Comm-Web_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LH-Comm-Web_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TI-1_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LH-MPD-Coaches_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TI-1_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LH-MPD-Coaches_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TI-2_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TI-2_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TI_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TI_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LHS_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-LHS_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-12_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-12_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-Destino_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-Destino_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-Revoked_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-Revoked_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-Military_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-Military_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-Military_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-Military_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-InnerCity_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-InnerCity_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-103-1_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-103-1_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-103_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-103_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-10_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-10_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-3_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-3_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-12_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-12_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-2_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-2_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-7_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-7_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-1_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TH-1_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-CSU_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-CSU_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-1_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-1_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-Campus_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-Campus-Local_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-Campus_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-Campus-Local_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-Campus-National_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-Campus-National_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-Campus_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-Campus_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-6_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-dss-1-TG-6_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-City_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Shared-City_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-SLI_R,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=Private-SLI_RWD,CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=PublicAdmin,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=PublicContributor,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=PublicLoggedInAdmin,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=PublicLoggedInConsumer,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=PublicLoggedInContributor,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=PublicLoggedInSupervisor,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=PublicSupervisor,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=StaffOnlyAdmin,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=StaffOnlyConsumer,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=StaffOnlyContributor,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=StaffOnlySupervisor,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=UCMAdmin,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=UCMSysManager,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org",
                    "CN=StudentContributor,CN=Roles,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org"
            );

    public void createGroups() throws NamingException
    {
        logger.info("creating cru groups ...");

        createGoogleGroups();

        createStellentGroups();
    }

    private void createStellentGroups() throws NamingException
    {
        String owner = properties.getNonNullProperty("theKeyLdapUser");

        for(String stellentOU : stellentOrganizationalUnits)
        {
            String groupName = stellentOU.split(",")[0].split("=")[1];
            String parentDn = stellentOU.substring(stellentOU.indexOf(",")+1, stellentOU.length());
            parentDn = parentDn.replaceAll("CN=", "ou=").replaceAll("cn=", "ou=");

            ldapDao.createOrganizationUnit(parentDn, groupName);
        }

        for(String stellentGroup : stellentGroups)
        {
            String groupName = stellentGroup.split(",")[0].split("=")[1];
            String parentDn = stellentGroup.substring(stellentGroup.indexOf(",")+1, stellentGroup.length());
            parentDn = parentDn.replaceAll("CN=", "ou=").replaceAll("cn=", "ou=");

            ldapDao.createGroup(parentDn, groupName, owner);
        }
    }

    private void createGoogleGroups() throws NamingException
    {
        String owner = properties.getNonNullProperty("theKeyLdapUser");
        String sourceGroupRootDn = properties.getNonNullProperty("relayGroupRootDn");
        String targetGroupRootDn = properties.getNonNullProperty("theKeyGroupRootDn").toLowerCase();

        for(String googleGroup : googleGroups)
        {
            googleGroup = googleGroup.replaceAll(sourceGroupRootDn, targetGroupRootDn);

            String parentDn = googleGroup.substring(googleGroup.indexOf(",")+1, googleGroup.length());
            parentDn = parentDn.replaceAll("CN=", "ou=");
            parentDn = parentDn.replaceAll("cn=", "ou=");
            parentDn = parentDn.replace(sourceGroupRootDn, targetGroupRootDn);

            String groupOrOrganizationalUnit = googleGroup.split(",")[0].split("=")[1];

            if(isGoogleGroupName(groupOrOrganizationalUnit))
            {
                ldapDao.createGroup(parentDn, groupOrOrganizationalUnit, owner);
            }
            else
            {
                ldapDao.createOrganizationUnit(parentDn, groupOrOrganizationalUnit);
            }
        }
    }

    private Boolean isGoogleGroupName(String possibleGroupName)
    {
        for(String googleGroupName : googleGroupNames)
        {
            if (possibleGroupName.toLowerCase().equals(googleGroupName.toLowerCase()))
            {
                return true;
            }
        }

        return false;
    }

    public void createCruPersonAttributes()
    {
        logger.info("creating cru person attributes ...");

        createAttributes(cruPersonAttributeNames, "cru person attribute", cruPersonObjectId);
    }

    public void createRelayAttributes()
	{
		logger.info("creating relay attributes ...");

		createAttributes(relayAttributeNames, "relay attribute", relayAttributesObjectId);
	}

	private void createAttributes(List<String> attributes, String description, String objectId)
	{
		logger.info("creating attributes ...");

		try
		{
			Integer iterator = 0;

			for(String attributeName : attributes)
			{
				ldapDao.createAttribute(attributeName, description, objectId + "." + iterator++);
			}

			// doesn't seem to work:
//			for(String attributeName : cruPersonAttributeNames)
//			{
//				ldapDao.addAttributeToClass(className, attributeName, "MAY");
//			}
		}
		catch (NamingException namingException)
		{
			namingException.printStackTrace();
		}
	}

	public void createCruPersonObject()
	{
		logger.info("creating cru person object class  ...");

		List<String> requiredAttributes = Arrays.asList("cn");

		createObjectClass("cruPerson", "Cru Person", requiredAttributes, cruPersonObjectId, "inetOrgPerson",
				LdapDao.ObjectClassType.Auxiliary);
	}

	public void createRelayAttributesObject()
	{
		logger.info("creating relay attributes object class  ...");

		List<String> requiredAttributes = Arrays.asList("cn");

		createObjectClass("relayAttributes", "Relay Attributes", requiredAttributes, relayAttributesObjectId,
				"inetOrgPerson", LdapDao.ObjectClassType.Auxiliary);
	}

	private void createObjectClass(String className, String description, List<String> requiredAttributes,
								   String objectId, String superClass, LdapDao.ObjectClassType objectClassType)
	{
		logger.info("creating object class  ...");
		try
		{
			ldapDao.createObjectClass(className, description, requiredAttributes, objectId,
					superClass, objectClassType);
		}
		catch (NamingException namingException)
		{
			namingException.printStackTrace();
		}
	}

	public void deleteCruPersonAttributes()
	{
		logger.info("deleting cru person attributes ...");

		deleteAttributes(cruPersonAttributeNames);
	}

	public void deleteRelayAttributes()
	{
		logger.info("deleting relay attributes ...");

		deleteAttributes(relayAttributeNames);
	}

	private void deleteAttributes(List<String> attributeNames)
	{
		logger.info("deleting attributes ...");
		try
		{
			for(String attributeName : attributeNames)
			{
				logger.info("deleting attribute " + attributeName);
				ldapDao.deleteAttribute(attributeName);
			}

		}
		catch (NamingException namingException)
		{
			namingException.printStackTrace();
		}
	}

	public void deleteCruPersonObject()
	{
		logger.info("deleting cru person object class ...");

		deleteObject("cruPerson");
	}

	public void deleteRelayObject()
	{
		logger.info("deleting cru person object class ...");

		deleteObject("relayAttributes");
	}

	public void deleteObject(String className)
	{
		logger.info("deleting object class ...");
		try
		{
			ldapDao.deleteClass(className);
		}
		catch (NamingException namingException)
		{
			namingException.printStackTrace();
		}
	}

	public void createSystemEntries() throws NamingException
	{
		for(String system : systems)
		{
			Map<String,String> attributeMap = Maps.newHashMap();

			attributeMap.put("cn", system);
			attributeMap.put("sn", system);
			attributeMap.put("userPassword", properties.getNonNullProperty(system));

			ldap.createEntity("cn=" + system + "," + properties.getNonNullProperty("theKeySystemUsersDn"), attributeMap, systemEntryClasses());
		}
	}

	private String[] systemEntryClasses()
	{
        List<String> classNames = Arrays.asList("Top", "Person", "inetOrgPerson", "organizationalPerson",
                "ndsLoginProperties", "homeInfo");

        return classNames.toArray(new String[classNames.size()]);
	}
}

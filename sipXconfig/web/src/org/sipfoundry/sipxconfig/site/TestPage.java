/*
 * 
 * 
 * Copyright (C) 2005 SIPfoundry Inc.
 * Licensed by SIPfoundry under the LGPL license.
 * 
 * Copyright (C) 2005 Pingtel Corp.
 * Licensed to SIPfoundry under a Contributor Agreement.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.site;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.tapestry.IPage;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.engine.IEngineService;
import org.apache.tapestry.html.BasePage;
import org.sipfoundry.sipxconfig.admin.callgroup.CallGroupContext;
import org.sipfoundry.sipxconfig.admin.commserver.SipxReplicationContext;
import org.sipfoundry.sipxconfig.admin.commserver.imdb.DataSet;
import org.sipfoundry.sipxconfig.admin.dialplan.DialPlanContext;
import org.sipfoundry.sipxconfig.admin.forwarding.ForwardingContext;
import org.sipfoundry.sipxconfig.admin.parkorbit.ParkOrbitContext;
import org.sipfoundry.sipxconfig.bulk.ldap.LdapImportManager;
import org.sipfoundry.sipxconfig.common.CoreContext;
import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.conference.ConferenceBridgeContext;
import org.sipfoundry.sipxconfig.gateway.GatewayContext;
import org.sipfoundry.sipxconfig.job.JobContext;
import org.sipfoundry.sipxconfig.phone.Phone;
import org.sipfoundry.sipxconfig.phone.PhoneContext;
import org.sipfoundry.sipxconfig.phone.PhoneModel;
import org.sipfoundry.sipxconfig.phone.polycom.PolycomModel;
import org.sipfoundry.sipxconfig.search.IndexManager;
import org.sipfoundry.sipxconfig.site.admin.commserver.ReplicationData;
import org.sipfoundry.sipxconfig.site.admin.commserver.RestartReminder;
import org.sipfoundry.sipxconfig.site.phone.NewPhone;
import org.sipfoundry.sipxconfig.site.search.EnumEditPageProvider;
import org.sipfoundry.sipxconfig.site.setting.EditGroup;
import org.sipfoundry.sipxconfig.site.upload.EditUpload;
import org.sipfoundry.sipxconfig.upload.UploadManager;
import org.sipfoundry.sipxconfig.upload.UploadSpecification;

/**
 * TestPage page
 */
public abstract class TestPage extends BasePage {

    public static final String PAGE = "TestPage";

    public static final int JOBS = 4;
    public static final String EMPTY_STRING = "";
    public static final int SERIAL_NUM_LEN = 12;

    // Data for the primary test user
    // Make sure the username matches SiteTestHelper.java
    public static final String TEST_USER_USERNAME = "testuser";
    public static final String TEST_USER_FIRSTNAME = "Test";
    public static final String TEST_USER_LASTNAME = "User";
    public static final String TEST_USER_ALIAS1 = "testy";
    public static final String TEST_USER_ALIAS2 = "200";
    public static final String TEST_USER_ALIASES = TEST_USER_ALIAS1 + " " + TEST_USER_ALIAS2;
    public static final String TEST_USER_PIN = "1234";
    public static final int MANY_USERS = 10000;
    
    public static final PhoneModel TEST_PHONE_MODEL = PolycomModel.MODEL_500;

    public abstract DialPlanContext getDialPlanContext();

    public abstract GatewayContext getGatewayContext();

    public abstract PhoneContext getPhoneContext();

    public abstract CallGroupContext getCallGroupContext();

    public abstract ParkOrbitContext getParkOrbitContext();

    public abstract UploadManager getUploadManager();

    public abstract CoreContext getCoreContext();

    public abstract ForwardingContext getForwardingContext();

    public abstract SipxReplicationContext getSipxReplicationContext();

    public abstract ConferenceBridgeContext getConferenceBridgeContext();

    public abstract JobContext getJobContext();

    public abstract IndexManager getIndexManager();
    
    public abstract LdapImportManager getLdapImportManager();

    public abstract ApplicationLifecycle getApplicationLifecycle();

    public abstract UserSession getUserSession();

    public abstract IEngineService getRestartService();

    public void resetCallForwarding() {
        getForwardingContext().clear();
    }

    public void resetDialPlans() {
        getDialPlanContext().clear();
        getGatewayContext().clear();
    }

    public void resetPhoneContext() {
        getPhoneContext().clear();
    }

    public void resetCallGroupContext() {
        getCallGroupContext().clear();
    }

    public void resetParkOrbitContext() {
        getParkOrbitContext().clear();
    }

    public void resetConferenceBridgeContext() {
        getConferenceBridgeContext().clear();
    }

    public String resetCoreContext() {
        // need to reset all data that could potentially have a reference
        // to users
        resetDialPlans();
        resetPhoneContext();
        resetCallGroupContext();
        getCoreContext().clear();
        getApplicationLifecycle().logout();
        // force rendering any new page after logout or infamous "invalid session" after
        // any links are clicked
        return PAGE;
    }

    public String logout() {
        getApplicationLifecycle().logout();
        return PAGE;
    }

    public IPage newGroup(IRequestCycle cycle, String resource) {
        EditGroup page = (EditGroup) cycle.getPage(EditGroup.PAGE);
        page.newGroup(resource, PAGE);
        return page;
    }

    public IPage goToRestartReminderPage(IRequestCycle cycle) {
        RestartReminder page = (RestartReminder) cycle.getPage(RestartReminder.PAGE);
        page.setNextPage(PAGE);
        return page;
    }

    public void toggleNavigation() {
        UserSession userSession = getUserSession();
        userSession.setNavigationVisible(!userSession.isNavigationVisible());
    }

    public void hideNavigation() {
        getUserSession().setNavigationVisible(false);
    }

    public void toggleAdmin() {
        UserSession userSession = getUserSession();
        boolean admin = !userSession.isAdmin();
        boolean supervisor = !userSession.isSupervisor();
        Integer userId = userSession.getUserId();
        if (userId == null) {
            login();
        } else {
            userSession.login(userId, admin, supervisor);
        }
    }

    public void seedTestUser() {
        createTestUserIfMissing();
    }

    private User createTestUserIfMissing() {
        String userName = TEST_USER_USERNAME;
        if (null != getCoreContext().loadUserByUserName(TEST_USER_USERNAME)) {
            // we already have test user - get a unique name for a new one
            userName = TEST_USER_USERNAME + System.currentTimeMillis();
        }
        String firstName = TEST_USER_FIRSTNAME;
        User user = new User();
        user.setUserName(userName);
        user.setFirstName(firstName);
        user.setLastName(TEST_USER_LASTNAME);
        user.setAliasesString(userName.equals(TEST_USER_USERNAME) ? TEST_USER_ALIASES
                : EMPTY_STRING);
        user.setPin(TEST_USER_PIN, getCoreContext().getAuthorizationRealm());
        getCoreContext().saveUser(user);
        return user;
    }

    public void populateUsers() {
        long l = System.currentTimeMillis();
        CoreContext coreContext = getCoreContext();
        String authorizationRealm = coreContext.getAuthorizationRealm();
        for (int i = 0; i < MANY_USERS; i++) {
            String firstName = TEST_USER_FIRSTNAME + i;
            User user = new User();
            user.setUserName("xuser" + (l + i));
            user.setFirstName(firstName);
            user.setLastName(TEST_USER_LASTNAME);
            user.setPin(TEST_USER_PIN, authorizationRealm);
            coreContext.saveUser(user);
        }
    }

    public void loginFirstTestUser() {
        // Find the first test user
        User user = getCoreContext().loadUserByUserName(TEST_USER_USERNAME);
        if (user == null) {
            throw new IllegalStateException("Test user with username = " + TEST_USER_USERNAME
                    + " is not in the database");
        }

        // Log it in
        UserSession userSession = getUserSession();
        userSession.login(user.getId(), false, true);
    }

    public void deleteAllUsers() {
        List users = getCoreContext().loadUsers();
        for (Iterator iter = users.iterator(); iter.hasNext();) {
            User user = (User) iter.next();
            getCoreContext().deleteUser(user);
        }
    }
    
    public IPage newPhone() {
        NewPhone newPhone = (NewPhone) getRequestCycle().getPage(NewPhone.PAGE);
        newPhone.setPhoneModel(TEST_PHONE_MODEL);
        return newPhone;
    }
    
    public void importLdap() {
        getLdapImportManager().insert();
    }

    public void indexAll() {
        getIndexManager().indexAll();
    }

    public void populateJobs() {
        JobContext jobContext = getJobContext();
        jobContext.clear();
        Serializable[] jobIds = new Serializable[JOBS];
        for (int i = 0; i < jobIds.length; i++) {
            jobIds[i] = jobContext.schedule("test" + i);
            if (i > 0) {
                jobContext.start(jobIds[i]);
            }
        }
        jobContext.success(jobIds[2]);
        jobContext.failure(jobIds[JOBS - 1], "something bad happened", null);
    }

    public void populatePhones() {
        Collection availablePhoneModels = getPhoneContext().getAvailablePhoneModels();
        for (Iterator i = availablePhoneModels.iterator(); i.hasNext();) {
            PhoneModel model = (PhoneModel) i.next();
            Phone phone = getPhoneContext().newPhone(model);
            phone.setSerialNumber(RandomStringUtils.randomNumeric(SERIAL_NUM_LEN));
            getPhoneContext().storePhone(phone);
        }
    }

    public void login() {
        User user = createTestUserIfMissing();
        getUserSession().login(user.getId(), true, true);
    }

    public void generateDataSet(String setName) {
        SipxReplicationContext sipxReplicationContext = getSipxReplicationContext();
        sipxReplicationContext.generate(DataSet.getEnum(setName));
    }

    public void throwException() {
        throw new IllegalArgumentException("Just testing");
    }

    public void validateEditPageProvider(IRequestCycle cycle) {
        EnumEditPageProvider provider = new EnumEditPageProvider();
        provider.validatePages(cycle);
    }

    public IPage newUpload(IRequestCycle cycle) {
        EditUpload page = (EditUpload) cycle.getPage(EditUpload.PAGE);
        page.setUploadId(null);
        page.setUploadSpecification(UploadSpecification.UNMANAGED);
        page.setReturnPage(PAGE);
        return page;
    }

    public void resetUploadManager() {
        getUploadManager().clear();
    }
    
    public IPage showDataSet(IRequestCycle cycle, String setName) {
        ReplicationData page = (ReplicationData) cycle.getPage(ReplicationData.PAGE);
        page.setDataSetName(setName);
        return page;
    }
}
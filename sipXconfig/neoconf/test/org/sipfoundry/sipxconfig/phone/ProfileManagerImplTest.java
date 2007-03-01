/*
 * 
 * 
 * Copyright (C) 2006 SIPfoundry Inc.
 * Licensed by SIPfoundry under the LGPL license.
 * 
 * Copyright (C) 2006 Pingtel Corp.
 * Licensed to SIPfoundry under a Contributor Agreement.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.phone;

import java.util.Arrays;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.sipfoundry.sipxconfig.device.RestartManager;
import org.sipfoundry.sipxconfig.job.JobContext;

public class ProfileManagerImplTest extends TestCase {
    
    public void testNewPhone() {
        new Phone();
    }

    public void testGenerateProfilesAndRestart() {
        Integer jobId = new Integer(4);

        Integer[] ids = {
            new Integer(1000), new Integer(2000)
        };

        IMocksControl jobContextCtrl = EasyMock.createStrictControl();
        JobContext jobContext = jobContextCtrl.createMock(JobContext.class);
        jobContext.schedule("Projection for phone 110000000000");
        jobContextCtrl.andReturn(jobId);
        jobContext.start(jobId);
        jobContext.success(jobId);
        jobContext.schedule("Projection for phone 120000000000");
        jobContextCtrl.andReturn(jobId);
        jobContext.start(jobId);
        jobContext.success(jobId);
        jobContextCtrl.replay();

        IMocksControl phoneControl = org.easymock.classextension.EasyMock.createStrictControl();
        Phone phone = phoneControl.createMock(Phone.class);
        phone.getSerialNumber();
        phoneControl.andReturn("110000000000");
        phone.generateProfiles();
        phone.getSerialNumber();
        phoneControl.andReturn("120000000000");
        phone.generateProfiles();
        phoneControl.replay();

        IMocksControl phoneContextCtrl = EasyMock.createControl();
        PhoneContext phoneContext = phoneContextCtrl.createMock(PhoneContext.class);
        phoneContext.loadPhone(ids[0]);
        phoneContextCtrl.andReturn(phone);
        phoneContext.loadPhone(ids[1]);
        phoneContextCtrl.andReturn(phone);
        phoneContextCtrl.replay();

        IMocksControl restartManagerCtrl = EasyMock.createControl();
        RestartManager restartManager = restartManagerCtrl.createMock(RestartManager.class);
        restartManager.restart(ids[0]);
        restartManager.restart(ids[1]);
        restartManagerCtrl.replay();

        ProfileManagerImpl pm = new ProfileManagerImpl();
        pm.setJobContext(jobContext);
        pm.setPhoneContext(phoneContext);
        pm.setRestartManager(restartManager);

        pm.generateProfilesAndRestart(Arrays.asList(ids));

        jobContextCtrl.verify();
        phoneControl.verify();
        phoneContextCtrl.verify();
        restartManagerCtrl.verify();
    }

    public void testGenerateProfileAndRestart() {
        Integer jobId = new Integer(4);
        Integer phoneId = new Integer(1000);

        IMocksControl jobContextCtrl = EasyMock.createStrictControl();
        JobContext jobContext = jobContextCtrl.createMock(JobContext.class);
        jobContext.schedule("Projection for phone 110000000000");
        jobContextCtrl.andReturn(jobId);
        jobContext.start(jobId);
        jobContext.success(jobId);
        jobContextCtrl.replay();

        IMocksControl phoneControl = org.easymock.classextension.EasyMock.createStrictControl();
        Phone phone = phoneControl.createMock(Phone.class);
        phone.getSerialNumber();
        phoneControl.andReturn("110000000000");
        phone.generateProfiles();
        phoneControl.replay();

        IMocksControl phoneContextCtrl = EasyMock.createControl();
        PhoneContext phoneContext = phoneContextCtrl.createMock(PhoneContext.class);
        phoneContext.loadPhone(phoneId);
        phoneContextCtrl.andReturn(phone);
        phoneContextCtrl.replay();

        IMocksControl restartManagerCtrl = EasyMock.createControl();
        RestartManager restartManager = restartManagerCtrl.createMock(RestartManager.class);
        restartManager.restart(phoneId);
        restartManagerCtrl.replay();

        ProfileManagerImpl pm = new ProfileManagerImpl();
        pm.setJobContext(jobContext);
        pm.setPhoneContext(phoneContext);
        pm.setRestartManager(restartManager);

        pm.generateProfileAndRestart(phoneId);

        jobContextCtrl.verify();
        phoneControl.verify();
        phoneContextCtrl.verify();
        restartManagerCtrl.verify();
    }
}
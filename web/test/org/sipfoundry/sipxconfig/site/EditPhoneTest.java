/*
 * 
 * 
 * Copyright (C) 2004 SIPfoundry Inc.
 * Licensed by SIPfoundry under the LGPL license.
 * 
 * Copyright (C) 2004 Pingtel Corp.
 * Licensed to SIPfoundry under a Contributor Agreement.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.site;

import java.util.Date;

import junit.framework.TestCase;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.test.AbstractInstantiator;
import org.easymock.MockControl;
import org.sipfoundry.sipxconfig.phone.Endpoint;
import org.sipfoundry.sipxconfig.phone.GenericPhone;
import org.sipfoundry.sipxconfig.phone.PhoneContext;
import org.sipfoundry.sipxconfig.site.phone.EditPhone;
import org.sipfoundry.sipxconfig.site.phone.ManagePhones;

public class EditPhoneTest extends TestCase {

    public void testSave() {
        AbstractInstantiator pageMaker = new AbstractInstantiator();
        EditPhone page = (EditPhone) pageMaker.getInstance(EditPhone.class);
        GenericPhone phone = new GenericPhone();
        Endpoint endpoint = new Endpoint();
        phone.setEndpoint(endpoint);
        endpoint.setPhoneId(phone.getModelId());
        page.setPhone(phone);
        endpoint.setSerialNumber(Long.toHexString(new Date().getTime()));
        
        MockControl daoControl = MockControl.createStrictControl(PhoneContext.class);
        PhoneContext dao = (PhoneContext) daoControl.getMock();
        dao.storeEndpoint(endpoint);
        dao.flush();
        daoControl.replay();

        MockControl cycleControl = MockControl.createStrictControl(IRequestCycle.class);
        IRequestCycle cycle = (IRequestCycle) cycleControl.getMock();
        cycleControl.expectAndReturn(cycle.getAttribute(PhoneContext.CONTEXT_BEAN_NAME), dao);
        cycle.activate(ManagePhones.PAGE);
        cycleControl.replay();

        page.ok(cycle);

        cycleControl.verify();
        daoControl.verify();
    }
}

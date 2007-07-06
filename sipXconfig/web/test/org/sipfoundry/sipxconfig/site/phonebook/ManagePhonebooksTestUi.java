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
package org.sipfoundry.sipxconfig.site.phonebook;

import junit.framework.Test;

import org.sipfoundry.sipxconfig.site.SiteTestHelper;

import net.sourceforge.jwebunit.WebTestCase;
import net.sourceforge.jwebunit.WebTester;

public class ManagePhonebooksTestUi extends WebTestCase {

    public static Test suite() throws Exception {
        return SiteTestHelper.webTestSuite(ManagePhonebooksTestUi.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        getTestContext().setBaseUrl(SiteTestHelper.getBaseUrl());
        SiteTestHelper.home(getTester());
        clickLink("link:phonebookReset");
    }

    public void testDisplay() {
        clickLink("link:managePhonebooks");
        SiteTestHelper.assertNoException(tester);
        assertElementPresent("phonebook:list");
    }
    
    public void testEditPhonebook() {
        seedPhonebook(tester, "manage-phonebooks");
        SiteTestHelper.home(getTester());
        clickLink("link:managePhonebooks");
        clickLinkWithText("manage-phonebooks");
        assertElementPresent("phonebookForm");
        // ok button tests that callback is present
        assertButtonPresent("form:ok");
    }
    
    public static void seedPhonebook(WebTester tester, String name) {        
        SiteTestHelper.home(tester);
        tester.clickLink("link:phonebook");        
        SiteTestHelper.initUploadFields(tester.getDialog().getForm(), "EditPhonebook");
        tester.setFormElement("name", name);
        tester.clickButton("form:apply");        
    }
}
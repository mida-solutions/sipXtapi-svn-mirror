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
package org.sipfoundry.sipxconfig.site.admin;

import java.io.File;

import junit.framework.Test;

import org.sipfoundry.sipxconfig.site.ListWebTestCase;
import org.sipfoundry.sipxconfig.site.SiteTestHelper;

public class ListParkOrbitsTestUi extends ListWebTestCase {
    private File m_tempFile;

    public static Test suite() throws Exception {
        return SiteTestHelper.webTestSuite(ListParkOrbitsTestUi.class);
    }

    public ListParkOrbitsTestUi() throws Exception {
        super("ListParkOrbits", "resetParkOrbitContext", "orbits");
        setHasDuplicate(false);
        m_tempFile = File.createTempFile("ListParkOrbitsTestUi", null);
    }

    public void setUp() {
        super.setUp();
        // sort by name
        clickLinkWithText("Name");
    }

    protected String[] getParamNames() {
        return new String[] {
            "name", "extension", "description",
        };
    }

    protected String[] getParamValues(int i) {
        return new String[] {
            "orbit" + i, Integer.toString(127 + i), "orbit description + i",
        };
    }

    protected Object[] getExpectedTableRow(String[] paramValues) {
        return new Object[] {
            paramValues[0], "false", paramValues[1], m_tempFile.getName()
        };
    }

    protected void setAddParams(String[] names, String[] values) {
        super.setAddParams(names, values);
        getDialog().getForm().setParameter("promptUpload", m_tempFile);
    }
    
    public void testParkOrbitDefaults() {
        clickLink("orbits:defaults");
        SiteTestHelper.assertNoException(tester);
        checkCheckbox("booleanField");
        clickButton("setting:apply");
        SiteTestHelper.assertNoException(tester);
        assertCheckboxSelected("booleanField");
        clickButton("setting:cancel");
        assertTablePresent("orbits:list");
    }
    
}
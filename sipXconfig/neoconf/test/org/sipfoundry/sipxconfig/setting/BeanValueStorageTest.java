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
package org.sipfoundry.sipxconfig.setting;

import junit.framework.TestCase;

public class BeanValueStorageTest extends TestCase {
    
    public void testGetSettingValue() {
        BeanValueStorageTestBean bean = new BeanValueStorageTestBean();
        BeanValueStorage vs = new BeanValueStorage(bean);
        SettingSet set = new SettingSet();
        Setting setting = new SettingImpl("wingspan");
        set.addSetting(setting);
        assertEquals("14 inches", vs.getSettingValue(setting).getValue());
    }

    static class BeanValueStorageTestBean {
        
        @SettingEntry(path = "wingspan")
        public String getWingSpan() {
            return "14 inches";
        }       
    }
}

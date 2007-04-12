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
package org.sipfoundry.sipxconfig.setting;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.sipfoundry.sipxconfig.common.BeanWithId;

/**
 * Basic layer of settings decoration that captures just setting values.
 */
public class ValueStorage extends BeanWithId implements Storage {
    private Map m_databaseValues = new HashMap();

    public int size() {
        return m_databaseValues.size();
    }    

    public Map getDatabaseValues() {
        return m_databaseValues;
    }

    public void setDatabaseValues(Map databaseValues) {
        m_databaseValues = databaseValues;
    }
    
    public int getSize() {
        return getDatabaseValues().size();
    }

    public SettingValue getSettingValue(Setting setting) {
        if (getDatabaseValues() == null) {
            return null;
        }
        
        SettingValue settingValue = null;
        // null is legal value so test for key existance
        if (getDatabaseValues().containsKey(setting.getPath())) {            
            String value = (String) getDatabaseValues().get(setting.getPath());
            settingValue = new SettingValueImpl(blankToNull(value));
        }
        return settingValue;
    }

    public void setSettingValue(String path, String value) {
        getDatabaseValues().put(path, nullToBlank(value));        
    }
    
    private static String nullToBlank(String value) {
        return value == null ? StringUtils.EMPTY : value;
    }
    
    private static String blankToNull(String value) {
        return StringUtils.isEmpty(value) ? null : value;
    }

    public void setSettingValue(Setting setting, SettingValue value, SettingValue defaultValue) {
        if (value.equals(defaultValue)) {
            revertSettingToDefault(setting);
        } else {
            setSettingValue(setting.getPath(), value.getValue());
        }
    }

    public void revertSettingToDefault(Setting setting) {
        getDatabaseValues().remove(setting.getPath());
    }
}
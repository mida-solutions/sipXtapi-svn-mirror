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
package org.sipfoundry.sipxconfig.phone.cisco;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.sipfoundry.sipxconfig.phone.AbstractLine;
import org.sipfoundry.sipxconfig.setting.Setting;
import org.sipfoundry.sipxconfig.setting.XmlModelBuilder;

/**
 * Cisco business functions for line meta setting
 */
public abstract class CiscoLine extends AbstractLine {

    public Setting getSettingModel() {
        File sysDir = new File(getPhoneContext().getSystemDirectory());
        File modelDefsFile = new File(sysDir, getModelFile());
        Setting model = new XmlModelBuilder(sysDir).buildModel(modelDefsFile);
        
        CiscoPhone ciscoPhone = (CiscoPhone) getPhone();
        return model.getSetting(ciscoPhone.getModel().getModelId());
    }
    
    public abstract String getRegistrationServerAddress();

    public abstract String getRegistrationServerPort();

    public abstract String getUserId();

    public abstract String getDisplayName();

    /**
     * borrowed from Polycom source as is
     * Doesn't include Display name or angle bracket, 
     * e.g. sip:user@blah.com, not "User Name"&lt;sip:user@blah.com&gt; 
     * NOTE: Unlike request URIs for REGISTER, this apparently requires the user
     * portion.  NOTE: I found this out thru trial and error.
     */
    public String getNotifyRequestUri() {
        StringBuffer sb = new StringBuffer();
        sb.append("sip:").append(getUserId());
        sb.append('@').append(getRegistrationServerAddress());
        String port = getRegistrationServerPort();
        if (StringUtils.isNotBlank(port)) {
            sb.append(':').append(port);
        }

        return sb.toString();
    }
}

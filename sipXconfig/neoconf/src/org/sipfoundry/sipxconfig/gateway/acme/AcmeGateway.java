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
package org.sipfoundry.sipxconfig.gateway.acme;

import org.sipfoundry.sipxconfig.device.DeviceDefaults;
import org.sipfoundry.sipxconfig.gateway.Gateway;
import org.sipfoundry.sipxconfig.setting.Setting;
import org.sipfoundry.sipxconfig.setting.SettingEntry;

public class AcmeGateway extends Gateway {

    @Override
    protected Setting loadSettings() {
        return getModelFilesContext().loadModelFile("acme-gateway.xml", "acme");
    }

    @Override
    public void initialize() {
        addDefaultBeanSettingHandler(new AcmeDefaults(getDefaults()));
    }

    public static class AcmeDefaults {
        private DeviceDefaults m_defaults;

        AcmeDefaults(DeviceDefaults defaults) {
            m_defaults = defaults;
        }

        @SettingEntry(path = "basic/proxyAddress")
        public String getProxyAddress() {
            return m_defaults.getProxyServerAddr();
        }

    }

    @Override
    protected String getProfileTemplate() {
        return "acme/acme-gateway.vm";
    }

    @Override
    protected String getProfileFilename() {
        return getSerialNumber() + ".ini";
    }
}
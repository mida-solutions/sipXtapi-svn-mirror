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
package org.sipfoundry.sipxconfig.gateway.audiocodes;

import java.util.Map;

import org.sipfoundry.sipxconfig.device.ProfileContext;
import org.sipfoundry.sipxconfig.gateway.Gateway;
import org.sipfoundry.sipxconfig.setting.BeanWithSettings;
import org.sipfoundry.sipxconfig.setting.Setting;

public abstract class AudioCodesGateway extends Gateway {

    @Override
    public void initialize() {
        AudioCodesGatewayDefaults defaults = new AudioCodesGatewayDefaults(this, getDefaults());
        // Added twice, Provides setting value directly by implementing SettingValueHandler
        // and also being wrapped by BeanValueStorage
        addDefaultSettingHandler(defaults);
        addDefaultBeanSettingHandler(defaults);
    }

    @Override
    protected String getProfileFilename() {
        return getSerialNumber() + ".ini";
    }

    @Override
    protected ProfileContext createContext() {
        return new AudioCodesContext(this, getProfileTemplate());
    }

    @Override
    protected String getProfileTemplate() {
        AudioCodesModel model = (AudioCodesModel) getModel();
        return model.getProfileTemplate();
    }

    @Override
    protected Setting loadSettings() {
        return getModelFilesContext().loadDynamicModelFile("mp-gateway.xml", "audiocodes",
                getSettingsEvaluator());
    }

    static class AudioCodesContext extends ProfileContext {
        public AudioCodesContext(BeanWithSettings device, String profileTemplate) {
            super(device, profileTemplate);
        }

        public Map<String, Object> getContext() {
            Map<String, Object> context = super.getContext();
            // $$ is used as ignore value
            context.put("ignore", "$$");
            return context;
        }
    }
}
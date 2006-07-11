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
package org.sipfoundry.sipxconfig.phone.kphone;

import org.sipfoundry.sipxconfig.common.SipUri;
import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.phone.Line;
import org.sipfoundry.sipxconfig.phone.LineInfo;
import org.sipfoundry.sipxconfig.phone.Phone;
import org.sipfoundry.sipxconfig.phone.PhoneModel;
import org.sipfoundry.sipxconfig.setting.SettingEntry;

public class KPhone extends Phone {    
    public static final PhoneModel MODEL_KPHONE = new PhoneModel("kphone", "KPhone");
    
    private static final String REG_URI = "Registration/SipUri";
    private static final String REG_USER = "Registration/UserName";
    private static final String REG_SERVER = "Registration/SipServer";
    
    public KPhone() {
        super(MODEL_KPHONE);
        setPhoneTemplate("kphone/kphonerc.vm");
    }
    
    public String getPhoneFilename() {
        return getWebDirectory() + "/" + getSerialNumber() + ".kphonerc";
    }    
    
    @Override
    public void initialize() {        
    }

    @Override
    public void initializeLine(Line line) {
        line.addDefaultBeanSettingHandler(new KPhoneLineDefaults(line));
    }
    
    @Override
    protected void setLineInfo(Line line, LineInfo lineInfo) {
        int port = SipUri.parsePort(lineInfo.getRegistrationServerPort(), SipUri.DEFAULT_SIP_PORT);
        String uri = SipUri.formatIgnoreDefaultPort(lineInfo.getDisplayName(), lineInfo.getUserId(), 
                lineInfo.getRegistrationServer(), port);
        
        line.setSettingValue(REG_URI, uri);
    }
    
    @Override
    protected LineInfo getLineInfo(Line line) {
        LineInfo lineInfo = new LineInfo();
        lineInfo.setUserId(line.getSettingValue(REG_USER));
        String uri = line.getSettingValue(REG_URI);
        lineInfo.setDisplayName(SipUri.extractFullUser(uri));
        // TODO Extract server and port
        // lineInfo.setRegistrationServer(SipUri.extractServer(uri));
        return lineInfo;
    }

    public static class KPhoneLineDefaults {
        private Line m_line;
        public KPhoneLineDefaults(Line line) {
            m_line = line;
        }
        
        @SettingEntry(path = REG_URI)
        public String getUri() {
            return m_line.getUri();
        }
        
        @SettingEntry(path = REG_USER)
        public String getUserName() {
            String userName = null;
            User user = m_line.getUser();
            if (user != null) {
                userName = user.getUserName();
            }
            
            return userName;            
        }
        
        @SettingEntry(path = REG_SERVER)
        public String getRegistrationServer() {
            return m_line.getPhoneContext().getPhoneDefaults().getDomainName();            
        }
    }
}

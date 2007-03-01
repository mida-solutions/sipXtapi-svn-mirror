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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.device.DeviceDefaults;
import org.sipfoundry.sipxconfig.device.DeviceTimeZone;
import org.sipfoundry.sipxconfig.phone.Line;
import org.sipfoundry.sipxconfig.phone.LineInfo;
import org.sipfoundry.sipxconfig.setting.SettingEntry;

/**
 * Support for Cisco 7940/7960
 */
public class CiscoIpPhone extends CiscoPhone {
    public static final String BEAN_ID = "ciscoIp";
    private static final String ZEROMIN = ":00";
    private static final String SHORTNAME_PATH = "line/shortname";
    private static final String AUTH_NAME_PATH = "line/authname";
    private static final String USER_NAME_PATH = "line/name";
    private static final String PASSWORD_PATH = "line/password";
    private static final String DISPLAY_NAME_PATH = "line/displayname";
    private static final String REGISTRATION_PATH = "proxy/address";
    private static final String REGISTRATION_PORT_PATH = "proxy/port";
    private static final String MESSAGES_URI_PATH = "phone/messages_uri";

    public CiscoIpPhone() {
        super(BEAN_ID);
        init();
    }

    public CiscoIpPhone(CiscoModel model) {
        super(model);
        init();
    }

    private void init() {
        setPhoneTemplate("ciscoIp/cisco-ip.vm");
    }

    public void initialize() {
        CiscoIpDefaults defaults = new CiscoIpDefaults(getPhoneContext().getPhoneDefaults());
        addDefaultBeanSettingHandler(defaults);
    }

    public String getPhoneFilename() {
        String phoneFilename = getSerialNumber();
        return getTftpRoot() + "/SIP" + phoneFilename.toUpperCase() + ".cnf";
    }

    @Override
    protected void setLineInfo(Line line, LineInfo lineInfo) {
        line.setSettingValue(DISPLAY_NAME_PATH, lineInfo.getDisplayName());
        line.setSettingValue(USER_NAME_PATH, lineInfo.getUserId());
        line.setSettingValue(PASSWORD_PATH, lineInfo.getPassword());
        line.setSettingValue(REGISTRATION_PATH, lineInfo.getRegistrationServer());
        line.setSettingValue(REGISTRATION_PORT_PATH, lineInfo.getRegistrationServerPort());
        line.setSettingValue(MESSAGES_URI_PATH, lineInfo.getVoiceMail());
    }

    @Override
    protected LineInfo getLineInfo(Line line) {
        LineInfo lineInfo = new LineInfo();
        lineInfo.setDisplayName(line.getSettingValue(DISPLAY_NAME_PATH));
        lineInfo.setUserId(line.getSettingValue(USER_NAME_PATH));
        lineInfo.setPassword(line.getSettingValue(PASSWORD_PATH));
        lineInfo.setRegistrationServer(line.getSettingValue(REGISTRATION_PATH));
        lineInfo.setRegistrationServerPort(line.getSettingValue(REGISTRATION_PORT_PATH));
        lineInfo.setVoiceMail(line.getSettingValue(MESSAGES_URI_PATH));
        return lineInfo;
    }

    public static class CiscoIpDefaults {
        private DeviceDefaults m_defaults;

        CiscoIpDefaults(DeviceDefaults defaults) {
            m_defaults = defaults;
        }

        @SettingEntry(path = MESSAGES_URI_PATH)
        public String getMessagesUri() {
            return m_defaults.getVoiceMail();
        }

        private DeviceTimeZone getZone() {
            return m_defaults.getTimeZone();
        }

        @SettingEntry(path = "datetime/time_zone")
        public String getTimeZoneId() {
            return getZone().getShortName();
        }

        @SettingEntry(path = "datetime/dst_auto_adjust")
        public boolean isDstAutoAdjust() {
            return getZone().getDstOffset() != 0;
        }

        @SettingEntry(path = "datetime/dst_offset")
        public long getDstOffset() {
            return isDstAutoAdjust() ? getZone().getDstOffset() / 3600 : 0;
        }

        @SettingEntry(path = "datetime/dst_start_day")
        public int getDstStartDay() {
            return isDstAutoAdjust() ? getZone().getStartDay() : 0;
        }

        @SettingEntry(path = "datetime/dst_start_day_of_week")
        public int getDstStartDayOfWeek() {
            return isDstAutoAdjust() ? getZone().getStartDayOfWeek() : 0;
        }

        @SettingEntry(path = "datetime/dst_start_month")
        public int getDstStartMonth() {
            return isDstAutoAdjust() ? getZone().getStartMonth() : 0;
        }

        @SettingEntry(path = "datetime/dst_start_time")
        public String getDstStartTime() {
            return isDstAutoAdjust() ? time(getZone().getStartTime()) : null;
        }

        @SettingEntry(path = "datetime/dst_start_week_of_month")
        public int getDstStartWeekOfMonth() {
            return isDstAutoAdjust() ? getZone().getStartWeek() : 0;
        }

        @SettingEntry(path = "datetime/dst_stop_day")
        public int getDstStopDay() {
            return isDstAutoAdjust() ? getZone().getStopDay() : 0;
        }

        @SettingEntry(path = "datetime/dst_stop_day_of_week")
        public int getDstStopDayOfWeek() {
            return isDstAutoAdjust() ? getZone().getStopDayOfWeek() : 0;
        }

        @SettingEntry(path = "datetime/dst_stop_month")
        public int getDstStopMonth() {
            return isDstAutoAdjust() ? getZone().getStopMonth() : 0;
        }

        @SettingEntry(path = "datetime/dst_stop_time")
        public String getStopTime() {
            return isDstAutoAdjust() ? time(getZone().getStopTime()) : null;
        }

        @SettingEntry(path = "datetime/dst_stop_week_of_month")
        public int getStopWeekOfMonth() {
            return isDstAutoAdjust() ? getZone().getStopWeek() : 0;
        }

        private String time(int time) {
            return String.valueOf(time / 3600) + ZEROMIN;
        }

    }

    public void initializeLine(Line line) {
        line.addDefaultBeanSettingHandler(new CiscoIpLineDefaults(line));
    }

    public class CiscoIpLineDefaults {
        private Line m_line;

        CiscoIpLineDefaults(Line line) {
            m_line = line;
        }

        @SettingEntry(path = PASSWORD_PATH)
        public String getPassword() {
            String password = null;
            User u = m_line.getUser();
            if (u != null) {
                password = u.getSipPassword();
            }
            return password;
        }

        @SettingEntry(
            paths = {
                SHORTNAME_PATH, AUTH_NAME_PATH, USER_NAME_PATH
                })
        public String getUserName() {
            String name = null;
            User u = m_line.getUser();
            if (u != null) {
                name = u.getUserName();
            }
            return name;
        }

        @SettingEntry(path = DISPLAY_NAME_PATH)
        public String getDisplayName() {
            String displayName = null;
            User u = m_line.getUser();
            if (u != null) {
                displayName = u.getDisplayName();
            }
            return displayName;
        }

        @SettingEntry(path = REGISTRATION_PATH)
        public String getProxyAddress() {
            return m_line.getPhoneContext().getPhoneDefaults().getDomainName();
        }

        @SettingEntry(path = MESSAGES_URI_PATH)
        public String getMessagesUri() {
            return m_line.getPhoneContext().getPhoneDefaults().getVoiceMail();
        }
    }

    public Collection getProfileLines() {
        ArrayList linesSettings = new ArrayList(getMaxLineCount());

        Collection lines = getLines();
        int i = 0;
        Iterator ilines = lines.iterator();
        for (; ilines.hasNext() && (i < getMaxLineCount()); i++) {
            linesSettings.add(((Line) ilines.next()).getSettings());
        }

        // copy in blank lines of all unused lines
        for (; i < getMaxLineCount(); i++) {
            Line line = createLine();
            line.setPosition(i);
            linesSettings.add(line.getSettings());
            line.addDefaultBeanSettingHandler(new CiscpIpStubbedLineDefaults());
        }

        return linesSettings;
    }

    public static class CiscpIpStubbedLineDefaults {
        @SettingEntry(
            paths = {
                REGISTRATION_PATH, REGISTRATION_PORT_PATH
                })
        public String getEmpty() {
            return StringUtils.EMPTY;
        }
    }
}
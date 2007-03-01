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
package org.sipfoundry.sipxconfig.admin.dialplan;

import org.sipfoundry.sipxconfig.common.BeanWithId;
import org.sipfoundry.sipxconfig.gateway.Gateway;


/**
 * RoutingException
 */
public class RoutingException extends BeanWithId {
    private Gateway m_gateway;
    private String m_callers;
    private String m_externalNumber;
    private EmergencyRouting m_emergencyRouting;

    /**
     * Default constructor used for bean creation
     */
    public RoutingException() {
    }

    /**
     * Ancillary constructor - mostly for testing
     * 
     * @param callers comma separated list of numbers
     * @param externalNumber number to which calls will be transferred
     * @param gateway PSTN or SIP gateway
     */
    public RoutingException(String callers, String externalNumber, Gateway gateway) {
        m_callers = callers;
        m_externalNumber = externalNumber;
        m_gateway = gateway;
    }

    public String getCallers() {
        return m_callers;
    }

    public void setCallers(String callers) {
        m_callers = callers;
    }

    public String getExternalNumber() {
        return m_externalNumber;
    }

    public void setExternalNumber(String externalNumber) {
        m_externalNumber = externalNumber;
    }

    public Gateway getGateway() {
        return m_gateway;
    }

    public void setGateway(Gateway gateway) {
        m_gateway = gateway;
    }

    public String[] getPatterns(String domainName) {
        String suffix = "@" + domainName;
        return DialPattern.getPatternsFromList(m_callers, suffix);
    }
    
    public void setEmergencyRouting(EmergencyRouting emergencyRouting) {
        m_emergencyRouting = emergencyRouting;
    }

    public EmergencyRouting getEmergencyRouting() {
        return m_emergencyRouting;
    }
}
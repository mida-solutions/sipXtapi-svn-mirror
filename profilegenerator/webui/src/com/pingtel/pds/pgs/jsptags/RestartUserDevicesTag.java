/*
 * 
 * 
 * Copyright (C) 2004 SIPfoundry Inc.
 * Licensed by SIPfoundry under the LGPL license.
 * 
 * Copyright (C) 2004 Pingtel Corp.
 * Licensed to SIPfoundry under a Contributor Agreement.
 * 
 * $$
 */


package com.pingtel.pds.pgs.jsptags;

import com.pingtel.pds.common.EJBHomeFactory;
import com.pingtel.pds.pgs.jsptags.util.ExTagSupport;
import com.pingtel.pds.pgs.user.UserAdvocate;
import com.pingtel.pds.pgs.user.UserAdvocateHome;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;


public class RestartUserDevicesTag extends ExTagSupport {

    private String m_userID;
    private UserAdvocateHome uaHome = null;


    public void setUserid( String userid ) {
        m_userID = userid;
    }


    public int doStartTag() throws JspException {
        try {
            if ( uaHome == null ) {
                uaHome = ( UserAdvocateHome )
                    EJBHomeFactory.getInstance().getHomeInterface(  UserAdvocateHome.class,
                                                                    "UserAdvocate" );
            }

            UserAdvocate userAdvocate = uaHome.create();

            userAdvocate.restartDevices ( m_userID );
        }
        catch (Exception ex ) {
            throw new JspTagException( ex.getMessage());
        }

        return SKIP_BODY;
    }


    protected void clearProperties()
    {
        m_userID = null;

        super.clearProperties();
    }
}

/*
 * $Id$
 *
 * Copyright (C) 2004 SIPfoundry Inc.
 * Licensed by SIPfoundry under the LGPL license.
 *
 * Copyright (C) 2004 Pingtel Corp.
 * Licensed to SIPfoundry under a Contributor Agreement.
 *
 * $$
 */

package org.sipfoundry.telephony.capabilities ;

import javax.telephony.capabilities.*  ;

public class PtTerminalConnectionCapabilities implements TerminalConnectionCapabilities
{
    public boolean canAnswer()
    {
        return true ;   
    }    
}

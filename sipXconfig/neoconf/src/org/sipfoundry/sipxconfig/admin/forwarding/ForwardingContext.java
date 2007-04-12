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
package org.sipfoundry.sipxconfig.admin.forwarding;

import java.util.List;

import org.sipfoundry.sipxconfig.admin.commserver.AliasProvider;
import org.sipfoundry.sipxconfig.common.User;

/**
 * ForwardingContext
 */
public interface ForwardingContext extends AliasProvider {
    public static final String CONTEXT_BEAN_NAME = "forwardingContext";

    public Ring getRing(Integer id);

    public CallSequence getCallSequenceForUser(User user);

    public void removeCallSequenceForUserId(Integer userId);

    public CallSequence getCallSequenceForUserId(Integer userId);

    public void saveCallSequence(CallSequence callSequence);

    public List getForwardingAuthExceptions();

    public void clear();

    public void flush();
}
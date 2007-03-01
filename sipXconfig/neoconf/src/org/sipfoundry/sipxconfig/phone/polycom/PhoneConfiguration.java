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
package org.sipfoundry.sipxconfig.phone.polycom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.velocity.VelocityContext;
import org.sipfoundry.sipxconfig.device.VelocityProfileGenerator;
import org.sipfoundry.sipxconfig.phone.Line;

/**
 * Responsible for generating MAC_ADDRESS.d/phone.cfg
 */
public class PhoneConfiguration extends VelocityProfileGenerator {

    private static final int TEMPLATE_DEFAULT_LINE_COUNT = 6;

    public PhoneConfiguration(PolycomPhone phone) {
        super(phone);
    }

    protected void addContext(VelocityContext context) {
        super.addContext(context);
        context.put("lines", getLines());
    }

    public Collection getLines() {
        PolycomPhone phone = (PolycomPhone) getDevice();
        int lineCount = Math.min(phone.getModel().getMaxLineCount(), TEMPLATE_DEFAULT_LINE_COUNT);
        ArrayList linesSettings = new ArrayList(lineCount);

        Collection lines = phone.getLines();
        int i = 0;
        Iterator ilines = lines.iterator();
        for (; ilines.hasNext(); i++) {
            linesSettings.add(((Line) ilines.next()).getSettings());
        }

        // copy in blank lines of all unused lines
        for (; i < lineCount; i++) {
            Line line = phone.createLine();
            line.setPhone(phone);
            line.setPosition(i);
            linesSettings.add(line.getSettings());
        }

        return linesSettings;
    }
}
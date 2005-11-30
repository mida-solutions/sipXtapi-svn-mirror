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
package org.sipfoundry.sipxconfig.admin;

import java.text.DateFormat;
import java.util.Date;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;
import org.sipfoundry.sipxconfig.admin.commserver.SipxProcessContext;
import org.sipfoundry.sipxconfig.common.ApplicationInitializedEvent;

public class WhackerTest extends TestCase {
    private Whacker m_whacker;
    private MockControl m_processControl;
    private boolean m_testWhacker;
    
    protected void setUp() throws Exception {
        m_whacker = new Whacker();

        // The Whacker is supposed to do a restart through the processContext.
        // Make a mock control that checks that.
        m_processControl = MockClassControl.createStrictControl(SipxProcessContext.class);
        SipxProcessContext processContext = (SipxProcessContext) m_processControl.getMock();
        processContext.manageServices(Whacker.SERVICE_NAMES, SipxProcessContext.Command.RESTART);
        m_processControl.replay();
        m_whacker.setProcessContext(processContext);
    }

    public void testWhacker() throws Exception {
        // By default, don't run this test because it takes too long.  In the sipfoundry main/3.1
        // branch, I have fixed that, but in this pingtel 3.0 branch I don't want to add any more
        // code than absolutely necessary.  Set m_testWhacker to true in the debugger to run the test.
	if (!m_testWhacker) {
            return;
        }
        
        // Schedule the WhackerTask.  The time is rounded down to the nearest minute, so add a
        // minute to ensure a time in the future.  Times in the past will get pushed way forward.
        // Add another 1/2-second to ensure that there is some delay to the start time.
        Date date = new Date();
        final long minute = 60000;
        final long halfSec = 500;
        final long startDelay = minute + halfSec;
        long taskTime = date.getTime() + startDelay;
        date.setTime(taskTime);
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        m_whacker.setTimeOfDay(df.format(date));
        
        // Run the Whacker, simulating app startup
        m_whacker.setEnabled(true);            // in case it is disabled via properties file
        m_whacker.setScheduledDay(ScheduledDay.EVERYDAY.getName());
        m_whacker.onApplicationEvent(new ApplicationInitializedEvent(this));
        
        // Wait for a 1/2-second extra to make sure the task has run, then verify
        Thread.sleep(startDelay + halfSec);
        try {
            m_processControl.verify();
        } catch (AssertionFailedError e) {
            fail("processContext.manageServices was not called by the TimerTask.  Be aware that this test is timing-dependent, you may need to tweak the TimerTask time or the sleep time for the main thread.");
        }
    }

    public void testWhackerLikesDateFormat() {
        // testWhacker above uses a special date format.  Make sure that the typical date format works.
        // Offset the time by one hour from now just to make sure the timer task won't fire, we're just
        // checking that the date works OK.
        Date date = new Date();
        date.setTime(date.getTime() + 1000 * 60 * 60);
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        m_whacker.setTimeOfDay(df.format(date));
        m_whacker.setEnabled(true);            // in case it is disabled via properties file
        m_whacker.onApplicationEvent(new ApplicationInitializedEvent(this));
    }
    
    public void testGetScheduledDay() {
        String dayNames[] = new String[] {"Every day", "Sunday", "Monday", "Tuesday", "Wednesday",
                "Thursday", "Friday", "Saturday"};
        ScheduledDay[] days = new ScheduledDay[] {ScheduledDay.EVERYDAY, ScheduledDay.SUNDAY,
                ScheduledDay.MONDAY, ScheduledDay.TUESDAY, ScheduledDay.WEDNESDAY,
                ScheduledDay.THURSDAY, ScheduledDay.FRIDAY, ScheduledDay.SATURDAY};
        for (int i = 0; i < days.length; i++) {
            m_whacker.setScheduledDay(dayNames[i]);
            assertTrue(m_whacker.getScheduledDayEnum() == days[i]);
        }
    }
    
}

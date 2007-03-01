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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sipfoundry.sipxconfig.admin.commserver.SipxProcessContext;
import org.sipfoundry.sipxconfig.admin.commserver.SipxProcessContext.Process;
import org.sipfoundry.sipxconfig.common.ApplicationInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/** Restart media server and status server periodically due to memory leaks */
public class Whacker implements ApplicationListener {
    class WhackerTask extends TimerTask {
        public void run() {
            LOG.info("Restarting the media server");
            m_processContext.manageServices(Arrays.asList(SERVICES),
                    SipxProcessContext.Command.RESTART);
        }
    }

    static final Process[] SERVICES = {
        Process.MEDIA_SERVER, Process.REGISTRAR
    };
    private static final Log LOG = LogFactory.getLog(Whacker.class);

    private SipxProcessContext m_processContext;
    private boolean m_enabled = true;
    private int m_hours = 3;
    private int m_minutes = 42;
    private String m_scheduledDay = ScheduledDay.SUNDAY.getName();
    private Timer m_timer;
    private boolean m_allowStaleDate; // for testing only

    public void setProcessContext(SipxProcessContext processContext) {
        m_processContext = processContext;
    }

    public boolean isEnabled() {
        return m_enabled;
    }

    public void setEnabled(boolean enabled) {
        m_enabled = enabled;
    }

    public String getScheduledDay() {
        return m_scheduledDay;
    }

    public void setScheduledDay(String scheduledDay) {
        m_scheduledDay = scheduledDay;
    }

    public void setHours(int hours) {
        m_hours = hours;
    }

    public void setMinutes(int minutes) {
        m_minutes = minutes;
    }

    // for testing only
    void setAllowStaleDate(boolean allowStaleDate) {
        m_allowStaleDate = allowStaleDate;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        // No need to register listener, all beans that implement listener interface are
        // automatically registered
        if (event instanceof ApplicationInitializedEvent) {
            resetTimer();
        }
    }

    private void resetTimer() {
        if (m_timer != null) {
            m_timer.cancel();
        }
        if (!isEnabled()) {
            LOG.info("Whacker is disabled");
            return;
        }
        m_timer = new Timer(false); // daemon, dies with main thread
        scheduleTask();
    }

    private void scheduleTask() {
        // Reuse the DailyBackupSchedule class to schedule this non-backup task.
        // Ideally we would do a little refactoring to make this clearer, but this code
        // is being written as a low-risk patch to 3.0, so that will have to wait.
        DailyBackupSchedule sched = new DailyBackupSchedule();
        sched.setEnabled(true);
        sched.setScheduledDay(getScheduledDayEnum());
        sched.setTimeOfDay(getTimeOfDayValue());
        sched.setAllowStaleDate(m_allowStaleDate); // for testing only
        sched.schedule(m_timer, new WhackerTask());
        LOG.info("Whacker is scheduled: " + sched.getScheduledDay().getName() + ", "
                + getTimeOfDayValue());
    }

    /** Convert the ScheduledDayName string to a ScheduledDay and return it */
    ScheduledDay getScheduledDayEnum() {
        ScheduledDay day = ScheduledDay.getScheduledDay(getScheduledDay());
        if (day == null) {
            throw new RuntimeException("Whacker: unrecognized scheduled day: "
                    + getScheduledDay());
        }
        return day;
    }

    /**
     * Convert the time-of-day string to a Date, expressed in GMT because that is what
     * DailyBackupSchedule is expecting.
     */
    private Date getTimeOfDayValue() {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, m_hours);
        date.set(Calendar.MINUTE, m_minutes);
        // switch to GMT timezone
        date.setTimeZone(TimeZone.getTimeZone("GMT"));
        return date.getTime();
    }
}
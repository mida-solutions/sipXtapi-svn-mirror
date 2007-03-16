/*
 * 
 * 
 * Copyright (C) 2006 SIPfoundry Inc.
 * Licensed by SIPfoundry under the LGPL license.
 * 
 * Copyright (C) 2006 Pingtel Corp.
 * Licensed to SIPfoundry under a Contributor Agreement.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.vm;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sipfoundry.sipxconfig.common.UserException;

public class MailboxManagerImpl implements MailboxManager {    
    private static final String MESSAGE_SUFFIX = "-00.xml";
    private static final FilenameFilter MESSAGE_FILES = new SuffixFileFilter(MESSAGE_SUFFIX);
    private static final Log LOG = LogFactory.getLog(MailboxManagerImpl.class);
    private File m_mailstoreDirectory;
    private String m_mediaServerCgiUrl;
    private MailboxPreferencesReader m_mailboxPreferencesReader;
    private MailboxPreferencesWriter m_mailboxPreferencesWriter;
    private DistributionListsReader m_distributionListsReader;
    private DistributionListsWriter m_distributionListsWriter;
    
    public boolean isEnabled() {
        return m_mailstoreDirectory != null && m_mailstoreDirectory.exists();
    }
    
    public DistributionList[] loadDistributionLists(Mailbox mailbox) {
        File file = mailbox.getDistributionListsFile();
        DistributionList[] lists = m_distributionListsReader.readObject(file);
        if (lists == null) {
            lists = DistributionList.createBlankList();
        }
        return lists;
    }

    public void saveDistributionLists(Mailbox mailbox, DistributionList[] lists) {
        File file = mailbox.getDistributionListsFile();
        m_distributionListsWriter.writeObject(lists, file);
    }
   
    public List<Voicemail> getVoicemail(Mailbox mbox, String folder) {
        checkMailstoreDirectory();
        File vmdir = new File(mbox.getUserDirectory(), folder);
        String[] wavs = vmdir.list(MESSAGE_FILES);
        if (wavs == null) {
            return Collections.emptyList();
        }
        Arrays.sort(wavs);
        List<Voicemail> vms = new ArrayList(wavs.length);
        for (String wav : wavs) {
            String basename = basename(wav);
            vms.add(new Voicemail(m_mailstoreDirectory, mbox.getUserId(), folder, basename));
        }
        return vms;
    }
    
    /** 
     * tell mediaserver cgi to mark voicemail as heard by using these
     * parameters
     *    action = updatestatus
     *    mailbox = userid
     *    category = inbox
     *    messageidlist = space delimited message ids
     */
    public void markRead(Mailbox mailbox, Voicemail voicemail) {
        String request = String.format("action=updatestatus&mailbox=%s&category=%s&messageidlist=%s",
                mailbox.getUserId(), voicemail.getFolderId(), voicemail.getMessageId());
        // triggers NOTIFY (iff folder is inbox, bug in mediaserver?)
        mediaserverCgiRequest(request);
    }
    
    public void move(Mailbox mailbox, Voicemail voicemail, String destinationFolderId) {
        File destination = new File(mailbox.getUserDirectory(), destinationFolderId);
        for (File f : voicemail.getAllFiles()) {
            f.renameTo(new File(destination, f.getName()));
        }
        
        triggerSipNotify(mailbox);
    }


    public void delete(Mailbox mailbox, Voicemail voicemail) {
        for (File f : voicemail.getAllFiles()) {
            f.delete();
        }

        triggerSipNotify(mailbox);
    }
    
    private void triggerSipNotify(Mailbox mailbox) {
        // reversed engineered this string from using sipx 3.6 system.
        String request = String.format("action=updatestatus&from=gateway&category=inbox&"
                + "mailbox=%s&messageidlist=-2", mailbox.getUserId());
        mediaserverCgiRequest(request);
    }
    
    public void mediaserverCgiRequest(String cgiRequest) {
        String errMsg = "Cannot contact media server to update voicemail status";
        if (StringUtils.isBlank(m_mediaServerCgiUrl)) {
            return;
        }
        String sUpdate = m_mediaServerCgiUrl + '?' + cgiRequest;
        InputStream updateResponse = null;
        try {
            LOG.info(sUpdate);
            updateResponse = new URL(sUpdate).openStream();
            IOUtils.readLines(updateResponse);
        } catch (MalformedURLException e) {
            throw new RuntimeException(errMsg, e);
        } catch (IOException e) {
            // not a fatal exception either. (unfort,. likely if mediaserver cert. isn't valid 
            // for multitude of reasons including reverse DNS not resolving)
            LOG.warn(errMsg, e);
        } finally {
            IOUtils.closeQuietly(updateResponse);
        }        
    }

    /**
     * Because in HA systems, admin may change mailstore directory, validate it
     */
    void checkMailstoreDirectory() {
        if (m_mailstoreDirectory == null) {
            throw new MailstoreMisconfigured(null);
        }
        if (!m_mailstoreDirectory.exists()) {
            throw new MailstoreMisconfigured(m_mailstoreDirectory.getAbsolutePath());
        }        
    }
    
    static class MailstoreMisconfigured extends UserException {
        MailstoreMisconfigured() {
            super("Mailstore directory configuration setting is missing.");
        }
        MailstoreMisconfigured(String dir) {
            super(String.format("Mailstore directory does not exist '%s'", dir));
        }
        MailstoreMisconfigured(String message, IOException cause) {
            super(message, cause);
        }
    }
    
    /** 
     * extract file name w/o ext.
     */
    static String basename(String filename) {
        int suffix = filename.lastIndexOf(MESSAGE_SUFFIX);
        return suffix >= 0 ? filename.substring(0, suffix) : filename; 
    }

    public String getMailstoreDirectory() {
        return m_mailstoreDirectory.getPath();
    }

    public void setMailstoreDirectory(String mailstoreDirectory) {
        m_mailstoreDirectory = new File(mailstoreDirectory);
    }
    
    public Mailbox getMailbox(String userId) {
        return new Mailbox(m_mailstoreDirectory, userId);
    }
    
    public void saveMailboxPreferences(Mailbox mailbox, MailboxPreferences preferences) {
        File file = mailbox.getVoicemailPreferencesFile();
        m_mailboxPreferencesWriter.writeObject(preferences, file);
    }
    
    public MailboxPreferences loadMailboxPreferences(Mailbox mailbox) {
        File prefsFile = mailbox.getVoicemailPreferencesFile();
        MailboxPreferences preferences = m_mailboxPreferencesReader.readObject(prefsFile);
        if (preferences == null) {
            preferences = new MailboxPreferences();
        }
        return preferences;
    }
    
    public static class YesNo {
        public String encode(Object o) {
            return Boolean.TRUE.equals(o) ? "yes" : "no";
        }        
    }

    public void setMailboxPreferencesReader(MailboxPreferencesReader mailboxReader) {
        m_mailboxPreferencesReader = mailboxReader;
    }

    public void setMailboxPreferencesWriter(MailboxPreferencesWriter mailboxWriter) {
        m_mailboxPreferencesWriter = mailboxWriter;
    }

    public void setDistributionListsReader(DistributionListsReader distributionListsReader) {
        m_distributionListsReader = distributionListsReader;
    }

    public void setDistributionListsWriter(DistributionListsWriter distributionListsWriter) {
        m_distributionListsWriter = distributionListsWriter;
    }

    public void setMediaServerCgiUrl(String mediaServerCgiUrl) {
        m_mediaServerCgiUrl = mediaServerCgiUrl;
    }
}
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sipfoundry.sipxconfig.phone.Line;
import org.sipfoundry.sipxconfig.phone.LineData;
import org.sipfoundry.sipxconfig.setting.Setting;

/**
 * Support for Cisco ATA186/188 and Cisco 7905/7912
 */
public class CiscoAtaPhone extends CiscoPhone {

    private static final String ZERO = "0";
    
    private static final String IMAGE_ID = "imageid";
    
    private static final String NONE = "none";
    
    private String m_ptagDat;
    
    private String m_cfgfmtUtility;

    private String m_binDir;
    
    public CiscoAtaPhone() {
        setLineFactoryId(CiscoAtaLine.FACTORY_ID);
        setPhoneTemplate("cisco/cisco-ata.vm");
        setModelFile("cisco/ata-phone.xml");
    }

    public String getCfgPrefix() {
        return getModel().getCfgPrefix();
    }

    public String getPhoneFilename() {
        String phoneFilename = getPhoneData().getSerialNumber();
        return getTftpRoot() + '/' + getCfgPrefix() + phoneFilename.toLowerCase();
    }
    
    public void setPtagDat(String ptagDat) {
        m_ptagDat = ptagDat;
    }
    
    public String getPtagDat() {
        if (m_ptagDat != null) {
            return m_ptagDat;
        }
        return getPhoneContext().getSystemDirectory() + "/cisco/" + getCfgPrefix() + "-ptag.dat";
    }

    public void setCfgfmtUtility(String cfgfmtUtility) {
        m_cfgfmtUtility = cfgfmtUtility;
    }

    /**
     * this points to the cfgfmt utility in etc/cisco directory
     */ 
    public String getCfgfmtUtility() {
        if (m_cfgfmtUtility != null) {
            return m_cfgfmtUtility;
        }
        return getPhoneContext().getSystemDirectory() + "/cisco/cfgfmt";
    }

    public String getBinDir() {
        return m_binDir;
    }

    public void setBinDir(String binDir) {
        m_binDir = binDir;
    }

    protected void save(String profile) throws IOException {
        String outputfile = getPhoneFilename();
        String outputTxtfile = outputfile + ".txt";
        FileWriter wtr = new FileWriter(outputTxtfile);
        try {
            CopyUtils.copy(profile, wtr);
        } finally {
            IOUtils.closeQuietly(wtr);
        }
        
        requireFile(getCfgfmtUtility());
        requireFile(getPtagDat());        
        try {
            String[] cmd = { 
                getCfgfmtUtility(), 
                "-t" + getPtagDat(), 
                outputTxtfile, 
                outputfile
            };
            Process p = Runtime.getRuntime().exec(cmd);
            int errCode = p.waitFor();
            if (errCode != 0) {
                String msg = "Cisco profile conversion utility failed status code:" + errCode; 
                StringWriter err = new StringWriter();                
                err.write(msg.toCharArray());
                CopyUtils.copy(p.getErrorStream(), err);
                throw new RuntimeException(err.toString());
            }
            
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }            
    }
    
    private void requireFile(String filename) {
        File f = new File(filename);
        if (!f.exists()) {
            StringBuffer msg = new StringBuffer();
            
            msg.append("Cannot complete configuration of Cisco device ");
            msg.append(getPhoneData().getSerialNumber());
            msg.append(".  Required file cannot be found ");
            msg.append(f.getAbsolutePath());
            msg.append(". This file is supplied by Cisco support.");
            throw new RuntimeException(msg.toString());
        }
    }

    // some settings should probably be added here
    protected void setDefaults(Setting settings) {
        String domainName = getPhoneContext().getDnsDomain();

        Setting pset = settings.getSetting(SIP);
        pset.getSetting("Proxy").setValue(domainName);
        pset.getSetting("SipOutBoundProxy").setValue(domainName);
    }

    public String getSoftwareUpgradeConfig() {
        Setting swupgrade = getSettings().getSetting("upgrade");

        if (swupgrade == null) {
            return StringUtils.EMPTY;
        }

        String swimage = swupgrade.getSetting("upgradecode").getValue();
        String imageid = swupgrade.getSetting(IMAGE_ID).getValue();
        String upghex = getModel().getUpgCode();

        if (StringUtils.isBlank(swimage) || swimage.equals(NONE)
                || imageid.equals(ZERO)) {
            return StringUtils.EMPTY;
        }

        return "upgradecode:3," + upghex + ",0.0.0.0,69," + imageid + "," + swimage;
    }

    public String getLogoUpgradeConfig() {
        if (getCfgPrefix().equals("ata")) {
            return StringUtils.EMPTY;
        }

        Setting logoupgrade = getSettings().getSetting("upgradelogo");

        if (logoupgrade == null) {
            return StringUtils.EMPTY;
        }

        String logofile = logoupgrade.getSetting("logofile").getValue();
        String imageid = logoupgrade.getSetting(IMAGE_ID).getValue();

        if (StringUtils.isBlank(logofile) || logofile.equals(NONE)
                || imageid.equals(ZERO)) {
            return StringUtils.EMPTY;
        }

        return "upgradelogo:" + imageid + ",0," + logofile;
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
            CiscoAtaLine line = new CiscoAtaLine();
            line.setPhone(this);
            line.setLineData(new LineData());
            line.getLineData().setPosition(i);
            line.setUserId(ZERO);
            line.setLoginId(ZERO);
            line.setDisplayName(ZERO);
            line.setPwd(ZERO);
            linesSettings.add(line.getSettings());
        }

        return linesSettings;
    }
}

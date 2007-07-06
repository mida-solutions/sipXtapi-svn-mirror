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
package org.sipfoundry.sipxconfig.site.cdr;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.BaseComponent;
import org.apache.tapestry.contrib.table.model.ITableColumn;
import org.apache.tapestry.services.ExpressionEvaluator;
import org.apache.tapestry.util.ContentType;
import org.apache.tapestry.web.WebResponse;
import org.sipfoundry.sipxconfig.cdr.CdrManager;
import org.sipfoundry.sipxconfig.cdr.CdrSearch;
import org.sipfoundry.sipxconfig.components.TapestryUtils;

public abstract class CdrStatsPanel extends BaseComponent {
    private static final Log LOG = LogFactory.getLog(CdrStatsPanel.class);

    public abstract ExpressionEvaluator getExpressionEvaluator();

    public abstract WebResponse getResponse();

    public abstract Date getStartTime();

    public abstract Date getEndTime();

    public abstract CdrSearch getCdrSearch();

    public abstract CdrManager getCdrManager();

    public ITableColumn getStartTimeColumn() {
        return TapestryUtils.createDateColumn("startTime", getMessages(),
                getExpressionEvaluator(), getPage().getLocale());
    }

    public void export() {
        WebResponse response = getResponse();

        response.setHeader("Expires", "0");
        response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
        response.setHeader("Pragma", "public");
        response.setHeader("Content-Disposition", "attachment; filename=\"cdrs.csv\"");

        ContentType csvType = new ContentType("text/comma-separated-values");

        try {
            PrintWriter writer = response.getPrintWriter(csvType);

            getCdrManager().dumpCdrs(writer, getStartTime(), getEndTime(), getCdrSearch());
            writer.close();
        } catch (IOException e) {
            LOG.error("Error during CDR export", e);
        }
    }
}
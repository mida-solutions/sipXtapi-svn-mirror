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
package org.sipfoundry.sipxconfig.site.acd;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.hivemind.Messages;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.Persist;
import org.apache.tapestry.contrib.table.model.ITableColumnModel;
import org.apache.tapestry.contrib.table.model.ITableRendererSource;
import org.apache.tapestry.contrib.table.model.simple.SimpleTableColumn;
import org.apache.tapestry.contrib.table.model.simple.SimpleTableColumnModel;
import org.apache.tapestry.event.PageBeginRenderListener;
import org.apache.tapestry.event.PageEvent;
import org.apache.tapestry.html.BasePage;
import org.postgresql.util.PGInterval;
import org.sipfoundry.sipxconfig.acd.stats.AcdHistoricalStats;
import org.sipfoundry.sipxconfig.common.SipUri;
import org.sipfoundry.sipxconfig.common.SqlInterval;
import org.sipfoundry.sipxconfig.site.cdr.CdrPage;
import org.sipfoundry.sipxconfig.site.common.DefaultTableValueRendererSource;


public abstract class AcdHistoryPage extends BasePage implements PageBeginRenderListener {

    public static final String PAGE = "acd/AcdHistoryPage";
    
    @InjectObject(value = "spring:acdHistoricalStats")
    public abstract AcdHistoricalStats getAcdHistoricalStats();

    public abstract Map<String, Object> getRow();
    
    @Persist
    public abstract String getReportName();
    
    public abstract void setReportName(String reportName);
    
    public abstract Object getAvailableReportsIndexItem();
    
    @Persist
    public abstract Date getStartTime();
    public abstract void setStartTime(Date startTime);
    
    @Persist
    public abstract Date getEndTime();
    public abstract void setEndTime(Date endTime);

    public void showReport(String reportName) {
        setReportName(reportName);
    }
    
    public void pageBeginRender(PageEvent event) {
        String report = getReportName();
        if (report == null) {
            report = getAcdHistoricalStats().getReports().get(0);
            setReportName(report);
        }
        
        if (getEndTime() == null) {
            setEndTime(CdrPage.getDefaultEndTime());
        }

        if (getStartTime() == null) {
            Date startTime = CdrPage.getDefaultStartTime(getEndTime());
            setStartTime(startTime);
        }
    }

    public List<Map<String, Object>>getRows() {
        return getAcdHistoricalStats().getReport(getReportName(), getStartTime(), getEndTime());
    }
    
    public ITableColumnModel getColumns() {
        ITableRendererSource valueRenderer = new DefaultTableValueRendererSource();
        List<String> names = getAcdHistoricalStats().getReportFields(getReportName());
        MapTableColumn[] columns = new MapTableColumn[names.size()];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = new MapTableColumn(getMessages(), getReportName(), names.get(i));
            columns[i].setValueRendererSource(valueRenderer);
        }        

        return new SimpleTableColumnModel(columns);
    }    
}

/**
 *  Get the column header from localized value from key built using what is effectively the 
 * report name and sql result's column name
 */
class MapTableColumn extends SimpleTableColumn {
    private Messages m_messages;
    private String m_report;
   
    public MapTableColumn(Messages messages, String report, String columnName) {
        super(columnName);
        m_messages = messages;
        m_report = report;
        setSortable(true);        
    }
    
    public Object getColumnValue(Object objRow) {
        String columnName = getColumnName();
        Object ovalue = ((Map<String, Object>) objRow).get(columnName);
        Object value = ovalue;
        if (ovalue instanceof PGInterval) {
            // at least this is sortable
            value = new SqlInterval((PGInterval) ovalue);
        } else if (columnName.equals("agent_uri") || columnName.equals("queue_uri")) {
            if (ovalue != null) {
                value = SipUri.extractUser((String) ovalue);
            }
        }            
            
        return value;
    }    
    
    public String getDisplayName() {
        return m_messages.getMessage(m_report + "." + getColumnName());
    }
}
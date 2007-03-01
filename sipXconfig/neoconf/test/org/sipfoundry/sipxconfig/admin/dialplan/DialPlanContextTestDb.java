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
package org.sipfoundry.sipxconfig.admin.dialplan;

import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.filter.IncludeTableFilter;
import org.sipfoundry.sipxconfig.SipxDatabaseTestCase;
import org.sipfoundry.sipxconfig.TestHelper;
import org.sipfoundry.sipxconfig.admin.dialplan.attendant.Holiday;
import org.sipfoundry.sipxconfig.admin.dialplan.attendant.ScheduledAttendant;
import org.sipfoundry.sipxconfig.admin.dialplan.attendant.WorkingTime;
import org.sipfoundry.sipxconfig.admin.dialplan.attendant.WorkingTime.WorkingHours;
import org.sipfoundry.sipxconfig.common.BeanWithId;
import org.sipfoundry.sipxconfig.common.UserException;
import org.sipfoundry.sipxconfig.gateway.Gateway;
import org.sipfoundry.sipxconfig.permission.Permission;
import org.springframework.context.ApplicationContext;

/**
 * DialPlanContextImplTest
 */
public class DialPlanContextTestDb extends SipxDatabaseTestCase {
    private DialPlanContext m_context;

    protected void setUp() throws Exception {
        ApplicationContext appContext = TestHelper.getApplicationContext();
        m_context = (DialPlanContext) appContext.getBean(DialPlanContext.CONTEXT_BEAN_NAME);
        TestHelper.cleanInsert("ClearDb.xml");
    }

    public void testAddDeleteRule() {
        // TODO - replace with IDialingRule mocks

        DialingRule r1 = new CustomDialingRule();
        r1.setName("a1");
        CustomDialingRule r2 = new CustomDialingRule();
        r2.setName("a2");
        r2.setPermissions(Collections.singletonList(Permission.VOICEMAIL));

        m_context.storeRule(r1);
        m_context.storeRule(r1);
        assertEquals(1, m_context.getRules().size());
        m_context.storeRule(r2);
        assertEquals(2, m_context.getRules().size());

        CustomDialingRule r = (CustomDialingRule) m_context.load(DialingRule.class, r2.getId());
        assertEquals(r2.getPermissionNames().get(0), r.getPermissionNames().get(0));

        Integer id1 = r1.getId();
        m_context.deleteRules(Collections.singletonList(id1));
        Collection rules = m_context.getRules();
        assertTrue(rules.contains(r2));
        assertFalse(rules.contains(r1));
        assertEquals(1, rules.size());
    }

    public void testAddRuleDuplicateName() {
        DialingRule r1 = new CustomDialingRule();
        r1.setName("a1");
        DialingRule r2 = new CustomDialingRule();
        r2.setName("a1");

        m_context.storeRule(r1);

        try {
            m_context.storeRule(r2);
            fail("Exception expected");
        } catch (UserException e) {
            assertTrue(e.getLocalizedMessage().indexOf("a1") > 0);
        }
    }

    public void testDefaultRuleTypes() throws Exception {
        m_context.resetToFactoryDefault();

        IncludeTableFilter filter = new IncludeTableFilter();
        filter.includeTable("*dialing_rule");

        IDataSet set = new FilteredDataSet(filter, TestHelper.getConnection().createDataSet());
        ITable table = set.getTable("dialing_rule");
        assertEquals(8, table.getRowCount());
        // FIXME: test agains the real data - need to remove ids...
        // IDataSet reference = new FilteredDataSet(filter, TestHelper
        // .loadDataSet("admin/dialplan/defaultFlexibleDialPlan.xml"));
        // Assertion.assertEquals(set, reference);

        ITable internal = set.getTable("attendant_dialing_rule");
        assertEquals(1, internal.getRowCount());
        assertEquals("operator 0", internal.getValue(0, "attendant_aliases"));
    }

    public void testDuplicateRules() throws Exception {
        DialingRule r1 = new CustomDialingRule();
        r1.setName("a1");
        m_context.storeRule(r1);
        assertFalse(r1.isNew());

        m_context.duplicateRules(Collections.singletonList(r1.getId()));

        assertEquals(2, m_context.getRules().size());

        IDataSet set = TestHelper.getConnection().createDataSet();
        ITable table = set.getTable("dialing_rule");
        assertEquals(2, table.getRowCount());
    }

    public void testDuplicateDefaultRules() throws Exception {
        m_context.resetToFactoryDefault();

        List rules = m_context.getRules();

        Transformer bean2id = new BeanWithId.BeanToId();

        Collection ruleIds = CollectionUtils.collect(rules, bean2id);

        m_context.duplicateRules(ruleIds);

        assertEquals(ruleIds.size() * 2, m_context.getRules().size());

        IDataSet set = TestHelper.getConnection().createDataSet();
        ITable table = set.getTable("dialing_rule");
        assertEquals(ruleIds.size() * 2, table.getRowCount());
    }

    public void testGetEmergencyRouting() throws Exception {
        TestHelper.insertFlat("admin/dialplan/emergency_routing.db.xml");
        EmergencyRouting emergencyRouting = m_context.getEmergencyRouting();

        assertEquals("9100", emergencyRouting.getExternalNumber());
        Gateway defaultGateway = emergencyRouting.getDefaultGateway();
        assertNotNull(defaultGateway);
        assertTrue(defaultGateway.getName().startsWith("x"));

        Collection exceptions = emergencyRouting.getExceptions();
        assertEquals(2, exceptions.size());

        for (Iterator i = exceptions.iterator(); i.hasNext();) {
            RoutingException e = (RoutingException) i.next();
            assertNotNull(e.getGateway());
            assertTrue(e.getExternalNumber().startsWith("9"));
            String[] callers = StringUtils.split(e.getCallers(), ", ");
            assertEquals(2, callers.length);
        }
    }

    public void testAddRoutingException() throws Exception {
        TestHelper.insertFlat("admin/dialplan/emergency_routing.db.xml");
        RoutingException re = new RoutingException();
        EmergencyRouting er = m_context.getEmergencyRouting();
        er.addException(re);
        m_context.storeEmergencyRouting(er);

        IDatabaseConnection db = TestHelper.getConnection();
        assertEquals(3, db.getRowCount("routing_exception"));
    }

    public void testRemoveRoutingException() throws Exception {
        TestHelper.insertFlat("admin/dialplan/emergency_routing.db.xml");

        EmergencyRouting er = m_context.getEmergencyRouting();
        RoutingException re = (RoutingException) er.getExceptions().iterator().next();
        m_context.removeRoutingException(re.getId());

        IDatabaseConnection db = TestHelper.getConnection();
        assertEquals(1, db.getRowCount("routing_exception"));
    }

    public void testIsAliasInUse() throws Exception {
        TestHelper.cleanInsert("admin/dialplan/seedDialPlanWithAttendant.xml");
        assertTrue(m_context.isAliasInUse("test attendant in use")); // auto attendant name
        assertTrue(m_context.isAliasInUse("100")); // voicemail extension
        assertFalse(m_context.isAliasInUse("200")); // a random extension that should not be in
        // use
    }

    public void testIsAliasInUseAttendant() throws Exception {
        TestHelper.cleanInsertFlat("admin/dialplan/attendant_rule.db.xml");
        assertTrue(m_context.isAliasInUse("333")); // auto attendant extension
        assertTrue(m_context.isAliasInUse("operator")); // auto attendant alias
        assertTrue(m_context.isAliasInUse("0")); // auto attendant alias
    }

    public void testGetBeanIdsOfObjectsWithAlias() throws Exception {
        TestHelper.cleanInsert("admin/dialplan/seedDialPlanWithAttendant.xml");
        assertTrue(m_context.getBeanIdsOfObjectsWithAlias("test attendant in use").size() == 1); // auto
        // attendant
        // name
        assertTrue(m_context.getBeanIdsOfObjectsWithAlias("100").size() == 1); // voicemail
        // extension
        assertTrue(m_context.getBeanIdsOfObjectsWithAlias("200").size() == 0); // a random
        // extension that
        // should not be
        // in use
    }

    public void testGetBeanIdsOfObjectsWithAliasAttendant() throws Exception {
        TestHelper.cleanInsertFlat("admin/dialplan/attendant_rule.db.xml");
        assertTrue(m_context.getBeanIdsOfObjectsWithAlias("333").size() == 1); // auto attendant
        // extension
        assertTrue(m_context.getBeanIdsOfObjectsWithAlias("operator").size() == 1); // auto
        // attendant
        // alias
        assertTrue(m_context.getBeanIdsOfObjectsWithAlias("0").size() == 1); // auto
    }

    public void testStoreAttendantRule() throws Exception {
        TestHelper.cleanInsert("admin/dialplan/seedDialPlanWithAttendant.xml");
        AutoAttendant autoAttendant = m_context.getAutoAttendant(new Integer(2000));

        ScheduledAttendant sa = new ScheduledAttendant();
        sa.setAttendant(autoAttendant);

        DateFormat format = new SimpleDateFormat("MM/dd/yyy");

        Holiday holiday = new Holiday();
        holiday.setAttendant(autoAttendant);
        holiday.addDay(format.parse("01/01/2005"));
        holiday.addDay(format.parse("06/06/2005"));
        holiday.addDay(format.parse("12/24/2005"));

        WorkingTime wt = new WorkingTime();
        wt.setAttendant(autoAttendant);
        WorkingHours[] workingHours = wt.getWorkingHours();
        Date stop = workingHours[4].getStop();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(stop);
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        workingHours[4].setStop(calendar.getTime());

        AttendantRule rule = new AttendantRule();
        rule.setName("myattendantschedule");
        rule.setAfterHoursAttendant(sa);
        rule.setHolidayAttendant(holiday);
        rule.setWorkingTimeAttendant(wt);

        m_context.storeRule(rule);

        assertEquals(1, getConnection().getRowCount("attendant_dialing_rule"));
        assertEquals(7, getConnection().getRowCount("attendant_working_hours"));
        assertEquals(3, getConnection().getRowCount("holiday_dates"));

        writeFlatXmlDataSet(new FileWriter("/tmp/kuku.xml"));
    }

    public void testLoadAttendantRule() throws Exception {
        TestHelper.cleanInsertFlat("admin/dialplan/attendant_rule.db.xml");

        DialingRule rule = m_context.getRule(new Integer(2002));
        assertTrue(rule instanceof AttendantRule);
        AttendantRule ar = (AttendantRule) rule;
        assertTrue(ar.getAfterHoursAttendant().isEnabled());
        assertFalse(ar.getWorkingTimeAttendant().isEnabled());
        assertTrue(ar.getHolidayAttendant().isEnabled());

        assertEquals(2, ar.getHolidayAttendant().getDates().size());

        assertEquals("19:25", ar.getWorkingTimeAttendant().getWorkingHours()[4].getStopTime());
    }

    public void testSaveExtensionThatIsDuplicateAlias() throws Exception {
        TestHelper.cleanInsertFlat("admin/dialplan/attendant_rule.db.xml");
        AttendantRule ar = new AttendantRule();
        ar.setName("autodafe");
        ar.setExtension("0");
        try {
            m_context.storeRule(ar);
            fail();
        } catch (UserException e) {
            // this is expected
            assertTrue(e.getMessage().indexOf("0") > 0);
        }
    }

    public void testSaveAliasThatIsDuplicateAlias() throws Exception {
        TestHelper.cleanInsertFlat("admin/dialplan/attendant_rule.db.xml");
        AttendantRule ar = new AttendantRule();
        ar.setName("autodafe");
        ar.setExtension("222");
        ar.setAttendantAliases("operator");
        try {
            m_context.storeRule(ar);
            fail();
        } catch (UserException e) {
            // this is expected
            assertTrue(e.getMessage().indexOf("operator") > 0);
        }
    }
}
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.InstanceofPredicate;
import org.sipfoundry.sipxconfig.admin.dialplan.attendant.ScheduledAttendant;
import org.sipfoundry.sipxconfig.common.BeanWithId;
import org.sipfoundry.sipxconfig.common.DataCollectionUtil;

public class DialPlan extends BeanWithId {
    private List<DialingRule> m_rules = new ArrayList<DialingRule>();

    public List<DialingRule> getRules() {
        return m_rules;
    }

    public void setRules(List<DialingRule> rules) {
        m_rules = rules;
    }

    public void removeRule(DialingRule rule) {
        Object[] keys = new Object[] {
            rule.getId()
        };
        DataCollectionUtil.removeByPrimaryKey(m_rules, keys);
    }

    public void removeRules(Collection ids) {
        DataCollectionUtil.removeByPrimaryKey(m_rules, ids.toArray());
    }

    public void addRule(DialingRule rule) {
        m_rules.add(rule);
        DataCollectionUtil.updatePositions(m_rules);
    }

    public void addRule(int position, DialingRule rule) {
        if (position < 0) {
            m_rules.add(rule);
        } else {
            m_rules.add(0, rule);
        }
        DataCollectionUtil.updatePositions(m_rules);
    }

    public void removeAllRules() {
        m_rules.clear();
    }

    public void moveRules(Collection<Integer> selectedRows, int step) {
        DataCollectionUtil.moveByPrimaryKey(m_rules, selectedRows.toArray(), step);
    }

    public List<DialingRule> getGenerationRules() {
        List<DialingRule> generationRules = new ArrayList<DialingRule>();
        for (DialingRule rule : getRules()) {
            rule.appendToGenerationRules(generationRules);
        }
        return generationRules;
    }

    /**
     * This function return all attendant rules contained in this plan.
     * 
     * @return list of attendant rules, empty list if no attendant rules in this plan
     */
    public List getAttendantRules() {
        List attendantRules = new ArrayList();
        Predicate isAttendantRule = InstanceofPredicate.getInstance(AttendantRule.class);
        CollectionUtils.select(m_rules, isAttendantRule, attendantRules);
        return attendantRules;
    }

    /**
     * Run thru dialing rules and set rellevant dial plans that take
     */
    public void setOperator(AutoAttendant operator) {
        DialingRule[] rules = getDialingRuleByType(m_rules, AttendantRule.class);
        for (int i = 0; i < rules.length; i++) {
            AttendantRule ar = (AttendantRule) rules[i];
            ScheduledAttendant sa = new ScheduledAttendant();
            sa.setAttendant(operator);
            ar.setAfterHoursAttendant(sa);
        }
    }

    /**
     * There can be multiple internal dialing rules and therefore multiple voicemail extensions,
     * but pick the most likely one.
     */
    public String getLikelyVoiceMailValue() {
        DialingRule[] rules = getDialingRuleByType(m_rules, InternalRule.class);
        if (rules.length == 0) {
            return InternalRule.DEFAULT_VOICEMAIL;
        }

        // return first, it's the most likely
        String voicemail = ((InternalRule) rules[0]).getVoiceMail();
        return voicemail;
    }

    static DialingRule[] getDialingRuleByType(List<DialingRule> rulesCandidates, Class c) {
        List<DialingRule> rules = new ArrayList<DialingRule>();
        for (DialingRule rule : rulesCandidates) {
            if (rule.getClass().isAssignableFrom(c)) {
                rules.add(rule);
            }
        }
        return rules.toArray(new DialingRule[rules.size()]);
    }
}
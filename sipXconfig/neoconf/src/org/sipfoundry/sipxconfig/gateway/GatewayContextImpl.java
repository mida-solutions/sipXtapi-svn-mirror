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
package org.sipfoundry.sipxconfig.gateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sipfoundry.sipxconfig.admin.commserver.SipxReplicationContext;
import org.sipfoundry.sipxconfig.admin.commserver.imdb.DataSet;
import org.sipfoundry.sipxconfig.admin.dialplan.DialPlanContext;
import org.sipfoundry.sipxconfig.admin.dialplan.DialingRule;
import org.sipfoundry.sipxconfig.common.DaoUtils;
import org.sipfoundry.sipxconfig.common.UserException;
import org.sipfoundry.sipxconfig.device.ModelSource;
import org.sipfoundry.sipxconfig.phone.PhoneModel;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

public class GatewayContextImpl extends HibernateDaoSupport implements GatewayContext,
        BeanFactoryAware {

    private static class DuplicateNameException extends UserException {
        private static final String ERROR = "A gateway with name \"{0}\" already exists.";

        public DuplicateNameException(String name) {
            super(ERROR, name);
        }
    }

    private static class DuplicateSerialNumberException extends UserException {
        private static final String ERROR = "A gateway with serial number \"{0}\" already exists.";

        public DuplicateSerialNumberException(String name) {
            super(ERROR, name);
        }
    }

    private DialPlanContext m_dialPlanContext;

    private BeanFactory m_beanFactory;

    private ModelSource m_modelSource;

    private SipxReplicationContext m_replicationContext;

    public GatewayContextImpl() {
        super();
    }

    public List<Gateway> getGateways() {
        return getHibernateTemplate().loadAll(Gateway.class);
    }

    public Collection<Integer> getAllGatewayIds() {
        return getHibernateTemplate().findByNamedQuery("gatewayIds");
    }

    public Gateway getGateway(Integer id) {
        return (Gateway) getHibernateTemplate().load(Gateway.class, id);
    }

    public void storeGateway(Gateway gateway) {
        // Before storing the gateway, make sure that it has a unique name.
        // Throw an exception if it doesn't.
        HibernateTemplate hibernate = getHibernateTemplate();
        DaoUtils.checkDuplicates(hibernate, Gateway.class, gateway, "name",
                new DuplicateNameException(gateway.getName()));
        DaoUtils.checkDuplicates(hibernate, Gateway.class, gateway, "serialNumber",
                new DuplicateSerialNumberException(gateway.getSerialNumber()));

        // Store the updated gateway
        hibernate.saveOrUpdate(gateway);

        m_replicationContext.generate(DataSet.CALLER_ALIAS);
    }

    public boolean deleteGateway(Integer id) {
        Gateway g = getGateway(id);
        g.removeProfiles();
        getHibernateTemplate().delete(g);
        return true;
    }

    public void deleteGateways(Collection<Integer> selectedRows) {
        // remove gateways from rules first
        m_dialPlanContext.removeGateways(selectedRows);
        // remove gateways from the database
        for (Integer id : selectedRows) {
            deleteGateway(id);
        }
    }

    public List<Gateway> getGatewayByIds(Collection<Integer> gatewayIds) {
        List<Gateway> gateways = new ArrayList<Gateway>(gatewayIds.size());
        for (Integer id : gatewayIds) {
            gateways.add(getGateway(id));
        }
        return gateways;
    }

    /**
     * Returns the list of gateways available for a specific rule
     * 
     * @param ruleId id of the rule for which gateways are checked
     * @return collection of available gateways
     */
    public Collection<Gateway> getAvailableGateways(Integer ruleId) {
        DialingRule rule = m_dialPlanContext.getRule(ruleId);
        if (null == rule) {
            return Collections.EMPTY_LIST;
        }
        List allGateways = getGateways();
        return rule.getAvailableGateways(allGateways);
    }

    public void addGatewaysToRule(Integer dialRuleId, Collection<Integer> gatewaysIds) {
        DialingRule rule = m_dialPlanContext.getRule(dialRuleId);
        for (Integer gatewayId : gatewaysIds) {
            Gateway gateway = getGateway(gatewayId);
            rule.addGateway(gateway);
        }
        m_dialPlanContext.storeRule(rule);
    }

    public void removeGatewaysFromRule(Integer dialRuleId, Collection<Integer> gatewaysIds) {
        DialingRule rule = m_dialPlanContext.getRule(dialRuleId);
        rule.removeGateways(gatewaysIds);
        m_dialPlanContext.storeRule(rule);
    }

    public void clear() {
        List gateways = getHibernateTemplate().loadAll(Gateway.class);
        getHibernateTemplate().deleteAll(gateways);
    }

    public Gateway newGateway(PhoneModel model) {
        Gateway gateway = (Gateway) m_beanFactory.getBean(model.getBeanId(), Gateway.class);
        gateway.setBeanId(model.getBeanId());
        gateway.setModelId(model.getModelId());
        return gateway;
    }

    public Collection<PhoneModel> getAvailableGatewayModels() {
        return m_modelSource.getModels();
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        m_beanFactory = beanFactory;
    }

    public void setDialPlanContext(DialPlanContext dialPlanContext) {
        m_dialPlanContext = dialPlanContext;
    }

    public void setModelSource(ModelSource modelSource) {
        m_modelSource = modelSource;
    }

    public void setReplicationContext(SipxReplicationContext replicationContext) {
        m_replicationContext = replicationContext;
    }
}
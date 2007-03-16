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
package org.sipfoundry.sipxconfig.service;

import org.sipfoundry.sipxconfig.common.NamedObject;
import org.sipfoundry.sipxconfig.device.ModelSource;
import org.sipfoundry.sipxconfig.setting.BeanWithSettings;

public abstract class ConfiguredService extends BeanWithSettings implements NamedObject {
    private String m_address;    
    private String m_name;
    private String m_description;
    private String m_beanId;
    private String m_descriptorId;
    private ServiceDescriptor m_descriptor;
    private ModelSource<ServiceDescriptor> m_descriptorSource;

    public ConfiguredService() {
    }

    protected ConfiguredService(String beanId) {
        m_beanId = beanId;
    }

    public ConfiguredService(ServiceDescriptor descriptor) {
        m_beanId = descriptor.getBeanId();
        m_descriptorId = descriptor.getModelId();
    }
    
    public String getBeanId() {
        return m_beanId;
    }

    public ServiceDescriptor getDescriptor() {
        if (m_descriptor != null) {
            return m_descriptor;
        }
        if (m_descriptorId == null) {
            throw new IllegalStateException("Descriptor ID not set");
        }
        if (m_descriptorSource == null) {
            throw new IllegalStateException("ModelSource not set");
        }
        m_descriptor = m_descriptorSource.getModel(m_descriptorId);
        return m_descriptor;
    }

    /**
     * Internal, do not call this method. Hibnerate property declared update=false, but still
     * required method be defined.
     */
    @SuppressWarnings("unused")    
    public void setBeanId(String illegal_) {
    }

    public String getDescription() {
        return m_description;
    }

    public void setDescription(String description) {
        m_description = description;
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    public String getAddress() {
        return m_address;
    }

    public void setAddress(String address) {
        m_address = address;
    }

    public void setDescriptor(ServiceDescriptor descriptor) {
        m_descriptor = descriptor;
    }

    public String getDescriptorId() {
        return m_descriptorId;
    }

    public void setDescriptorId(String descriptorId) {
        m_descriptorId = descriptorId;
        m_descriptor = null;
    }

    public ModelSource<ServiceDescriptor> getDescriptorSource() {
        return m_descriptorSource;
    }

    public void setDescriptorSource(ModelSource<ServiceDescriptor> descriptorSource) {
        m_descriptorSource = descriptorSource;
    }
}
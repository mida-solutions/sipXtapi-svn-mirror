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
package org.sipfoundry.sipxconfig.domain;

import junit.framework.TestCase;

public class DomainTest extends TestCase {
    private Domain m_domain;
    
    protected void setUp() {
        m_domain = new Domain("example.org");
    }

    public void testAddAlias() {
        assertNull(m_domain.getAliases());
        m_domain.addAlias("grackle");
        assertNotNull(m_domain.getAliases());
        assertSame("grackle", m_domain.getAliases().iterator().next());
        assertEquals(1, m_domain.getAliases().size());
    }

    public void testRemoveAlias() {
        try {
            m_domain.removeAlias("passenger pigeon");
        } catch (RuntimeException e) {
            fail(e.toString());
        }

        m_domain.addAlias("greebe");
        m_domain.removeAlias("greebe");
        assertEquals(0, m_domain.getAliases().size());
    }
}
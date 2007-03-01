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
package org.sipfoundry.sipxconfig.bulk.ldap;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

public class LdapConnectionParamsTest extends TestCase {

    public void testNoneAuthentication() {
        IMocksControl templateCtrl = EasyMock.createControl();
        JndiLdapTemplate template = templateCtrl.createMock(JndiLdapTemplate.class);
        template.setProviderUrl("ldap://example.sipfoundry.org:10");
        template.setSecurityAuthentication("none");
        template.setSecurityPrincipal("uid=bongo,dc=sipfoundry,dc=com");
        template.setSecurityCredentials(null);
        templateCtrl.replay();

        LdapConnectionParams params = new LdapConnectionParams();
        params.setPort(10);
        params.setHost("example.sipfoundry.org");
        params.setPrincipal("uid=bongo,dc=sipfoundry,dc=com");

        params.applyToTemplate(template);

        templateCtrl.verify();
    }

    public void testBasicAuthentication() {
        IMocksControl templateCtrl = EasyMock.createControl();
        JndiLdapTemplate template = templateCtrl.createMock(JndiLdapTemplate.class);
        template.setProviderUrl("ldap://example.sipfoundry.org:10");
        template.setSecurityAuthentication("basic");
        template.setSecurityPrincipal("uid=bongo,dc=sipfoundry,dc=com");
        template.setSecurityCredentials("abc");
        templateCtrl.replay();

        LdapConnectionParams params = new LdapConnectionParams();
        params.setPort(10);
        params.setHost("example.sipfoundry.org");
        params.setPrincipal("uid=bongo,dc=sipfoundry,dc=com");
        params.setSecret("abc");

        params.applyToTemplate(template);

        templateCtrl.verify();
    }
}
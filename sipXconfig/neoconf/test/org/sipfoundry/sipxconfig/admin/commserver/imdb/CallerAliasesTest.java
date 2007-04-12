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
package org.sipfoundry.sipxconfig.admin.commserver.imdb;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.dom4j.Document;
import org.easymock.EasyMock;
import org.sipfoundry.sipxconfig.TestHelper;
import org.sipfoundry.sipxconfig.XmlUnitHelper;
import org.sipfoundry.sipxconfig.common.CoreContext;
import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.common.UserCallerAliasInfo;
import org.sipfoundry.sipxconfig.gateway.Gateway;
import org.sipfoundry.sipxconfig.gateway.GatewayCallerAliasInfo;
import org.sipfoundry.sipxconfig.gateway.GatewayContext;
import org.sipfoundry.sipxconfig.permission.PermissionManagerImpl;

public class CallerAliasesTest extends XMLTestCase {
    public CallerAliasesTest() {
        XmlUnitHelper.setNamespaceAware(false);
        XMLUnit.setIgnoreWhitespace(true);
    }

    private String[][] USER_DATA = {
        {
            "first", "last", "userName", "1234", "ss"
        }, {
            null, null, "kuku", "4321", "87654321"
        }, {
            "user", "without", "number", null, "12345"
        },
    };

    private Object[][] GATEWAY_DATA = {
        {
            "example.org", "7832331111", true, false, "", -1, false
        }, {
            "bongo.com", null, false, false, "", -1, false
        }, {
            "kuku.net", "233", false, true, "+", 2, true
        }
    };

    private List<User> m_users;

    private List<Gateway> m_gateways;

    protected void setUp() throws Exception {
        PermissionManagerImpl impl = new PermissionManagerImpl();
        impl.setModelFilesContext(TestHelper.getModelFilesContext());

        m_users = new ArrayList<User>();
        for (String[] ud : USER_DATA) {
            User user = new User();
            user.setPermissionManager(impl);

            user.setFirstName(ud[0]);
            user.setLastName(ud[1]);
            user.setUserName(ud[2]);
            user.setSettingValue(UserCallerAliasInfo.EXTERNAL_NUMBER, ud[3]);
            user.addAlias(ud[4]);
            m_users.add(user);
        }

        m_gateways = new ArrayList<Gateway>();
        for (Object[] gd : GATEWAY_DATA) {
            Gateway gateway = new Gateway();
            gateway.setAddress((String) gd[0]);
            GatewayCallerAliasInfo info = new GatewayCallerAliasInfo();
            info.setDefaultCallerAlias((String) gd[1]);
            info.setIgnoreUserInfo((Boolean) gd[2]);
            info.setTransformUserExtension((Boolean) gd[3]);
            info.setAddPrefix((String) gd[4]);
            info.setKeepDigits((Integer) gd[5]);
            info.setAnonymous((Boolean) gd[6]);
            gateway.setCallerAliasInfo(info);
            m_gateways.add(gateway);
        }
    }

    public void testGenerateEmpty() throws Exception {
        CallerAliases cas = new CallerAliases();
        cas.setSipDomain("example.org");

        CoreContext coreContext = EasyMock.createMock(CoreContext.class);
        coreContext.loadUsers();
        EasyMock.expectLastCall().andReturn(Collections.emptyList());

        GatewayContext gatewayContext = EasyMock.createMock(GatewayContext.class);
        gatewayContext.getGateways();
        EasyMock.expectLastCall().andReturn(Collections.emptyList());

        cas.setCoreContext(coreContext);
        cas.setGatewayContext(gatewayContext);

        EasyMock.replay(coreContext, gatewayContext);

        Document document = cas.generate();
        org.w3c.dom.Document domDoc = XmlUnitHelper.getDomDoc(document);
        assertXpathEvaluatesTo("caller-alias", "/items/@type", domDoc);
        assertXpathNotExists("/items/item", domDoc);

        EasyMock.verify(coreContext, gatewayContext);
    }

    public void testGenerate() throws Exception {
        CallerAliases cas = new CallerAliases();
        cas.setSipDomain("example.org");
        cas.setAnonymousAlias("sip:anonymous@anonymous.invalid");

        CoreContext coreContext = EasyMock.createMock(CoreContext.class);
        coreContext.loadUsers();
        EasyMock.expectLastCall().andReturn(m_users);

        GatewayContext gatewayContext = EasyMock.createMock(GatewayContext.class);
        gatewayContext.getGateways();
        EasyMock.expectLastCall().andReturn(m_gateways);

        cas.setCoreContext(coreContext);
        cas.setGatewayContext(gatewayContext);

        EasyMock.replay(coreContext, gatewayContext);

        Document document = cas.generate();

        String casXml = XmlUnitHelper.asString(document);
        InputStream referenceXmlStream = AliasesTest.class
                .getResourceAsStream("caller-alias.test.xml");
        assertXMLEqual(new InputStreamReader(referenceXmlStream), new StringReader(casXml));

        EasyMock.verify(coreContext, gatewayContext);
    }

}
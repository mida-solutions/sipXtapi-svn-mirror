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
package org.sipfoundry.sipxconfig.admin.commserver.imdb;

import java.util.Collections;

import org.custommonkey.xmlunit.XMLTestCase;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.sipfoundry.sipxconfig.XmlUnitHelper;
import org.sipfoundry.sipxconfig.common.CoreContext;
import org.sipfoundry.sipxconfig.common.Md5Encoder;
import org.sipfoundry.sipxconfig.common.User;

public class CredentialsTest extends XMLTestCase {
    private static final String SIPFOUNDRY = "sipfoundry.org";
    private static final String REALM = SIPFOUNDRY;
    private static final String DOMAIN = "sipx." + SIPFOUNDRY;

    public void testGenerateEmpty() throws Exception {
        IMocksControl control = EasyMock.createControl();
        CoreContext coreContext = control.createMock(CoreContext.class);
        coreContext.getDomainName();
        control.andReturn("host.company.com");
        coreContext.getAuthorizationRealm();
        control.andReturn("company.com");
        coreContext.loadUsers();
        control.andReturn(Collections.EMPTY_LIST);
        control.replay();

        Credentials credentials = new Credentials();
        credentials.setCoreContext(coreContext);

        Document document = credentials.generate();
        org.w3c.dom.Document domDoc = XmlUnitHelper.getDomDoc(document);
        assertXpathEvaluatesTo("credential", "/items/@type", domDoc);
        assertXpathNotExists("/items/item", domDoc);
        control.verify();
    }

    public void testAddUser() throws Exception {
        Document document = DocumentFactory.getInstance().createDocument();
        Element item = document.addElement("items").addElement("item");
        User user = new User();
        user.setUserName("superadmin");
        final String PIN = "pin1234";
        user.setPin(PIN, REALM);
        user.setSipPassword("pass4321");

        Credentials credentials = new Credentials();
        credentials.addUser(item, user, DOMAIN, REALM);

        org.w3c.dom.Document domDoc = XmlUnitHelper.getDomDoc(document);
        assertXpathEvaluatesTo("sip:superadmin@" + DOMAIN, "/items/item/uri", domDoc);
        assertXpathEvaluatesTo(Md5Encoder.digestPassword("superadmin", REALM,
                PIN), "/items/item/pintoken", domDoc);
        assertXpathEvaluatesTo(Md5Encoder.digestPassword("superadmin", REALM,
                "pass4321"), "/items/item/passtoken", domDoc);
        assertXpathEvaluatesTo(REALM, "/items/item/realm", domDoc);
        assertXpathEvaluatesTo("DIGEST", "/items/item/authtype", domDoc);
    }

    public void testAddUserEmptyPasswords() throws Exception {
        Document document = DocumentFactory.getInstance().createDocument();
        Element item = document.addElement("items").addElement("item");

        User user = new User();
        user.setUserName("superadmin");
        user.setPin("", REALM);

        Credentials credentials = new Credentials();
        credentials.addUser(item, user, DOMAIN, REALM);

        org.w3c.dom.Document domDoc = XmlUnitHelper.getDomDoc(document);
        assertXpathEvaluatesTo("sip:superadmin@" + DOMAIN, "/items/item/uri", domDoc);
        String emptyHash = Md5Encoder.digestPassword("superadmin", REALM, "");
        assertXpathEvaluatesTo(emptyHash, "/items/item/pintoken", domDoc);
        assertXpathEvaluatesTo(emptyHash, "/items/item/passtoken", domDoc);
        assertXpathEvaluatesTo(REALM, "/items/item/realm", domDoc);
        assertXpathEvaluatesTo("DIGEST", "/items/item/authtype", domDoc);
    }
}
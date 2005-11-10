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
package org.sipfoundry.sipxconfig.site.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.tapestry.IExternalPage;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.contrib.table.model.IPrimaryKeyConvertor;
import org.apache.tapestry.event.PageEvent;
import org.apache.tapestry.event.PageRenderListener;
import org.apache.tapestry.html.BasePage;
import org.sipfoundry.sipxconfig.common.CoreContext;
import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.components.ObjectSourceDataSqueezer;
import org.sipfoundry.sipxconfig.components.TapestryUtils;
import org.sipfoundry.sipxconfig.search.SearchManager;

public abstract class SearchPage extends BasePage implements IExternalPage, PageRenderListener {

    public static final String PAGE = "SearchPage";

    public abstract SearchManager getSearchManager();

    public abstract String getQuery();

    public abstract void setQuery(String query);

    public abstract void setResults(Collection results);

    public abstract Collection getResults();

    public abstract void setUsers(Collection users);

    public abstract CoreContext getCoreContext();

    public void activateExternalPage(Object[] parameters, IRequestCycle cycle_) {
        String query = (String) TapestryUtils.assertParameter(String.class, parameters, 0);
        setQuery(query);
    }

    public void pageBeginRender(PageEvent event) {
        String query = getQuery();
        if (query == null) {
            return;
        }
        if (!event.getRequestCycle().isRewinding()) {
            // do not search when rewinding
            search(query);
        }
    }

    private void search(String query) {
        List results = getSearchManager().search(query);
        setResults(results);
        List users = new ArrayList();
        for (Iterator i = results.iterator(); i.hasNext();) {
            Object item = i.next();
            if (item instanceof User) {
                users.add(item);
            }
        }
        setUsers(users);
    }

    public IPrimaryKeyConvertor getUserIdConverter() {
        CoreContext context = getCoreContext();
        return new ObjectSourceDataSqueezer(context, User.class);
    }

    public String getFoundMsg() {
        Collection results = getResults();
        int foundCount = results != null ? results.size() : 0;
        return getMessages().format("msg.found", new Integer(foundCount));
    }

}

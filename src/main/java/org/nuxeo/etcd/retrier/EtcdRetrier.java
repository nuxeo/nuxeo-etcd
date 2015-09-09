/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.etcd.retrier;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.etcd.EtcdResult;
import org.nuxeo.etcd.EtcdService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 0.3
 */
public class EtcdRetrier extends DefaultComponent {

    protected boolean working = false;

    public static enum ACTION {SET, DELETE}

    public static final String DIRECTORY_NAME = "etcdAction";

    public static final String SCHEMA_NAME = "etcd_action";

    private static final Log log = LogFactory.getLog(EtcdRetrier.class);

    public static EtcdRetrier newInstance() {
        return new EtcdRetrier();
    }

    public void retry() {
        working = true;
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        Session session = directoryService.open(DIRECTORY_NAME);

        try {
            DocumentModelList entries = session.query(new HashMap<>());
            for (DocumentModel entry : entries) {
                handleEntry(session, entry);
            }
        } finally {
            working = false;
            session.close();
        }
    }

    public boolean isWorking() {
        return working;
    }

    protected void handleEntry(Session session, DocumentModel entry) {

        EtcdResult res;
        String action = (String) entry.getPropertyValue("action");
        if (action.equals(ACTION.SET.toString())) {
            res = set(entry);
        } else {
            log.error(String.format("Not supported action (%s), ignored", action));
            res = new EtcdResult(); //Used to fallback on deletion.
        }

        if (res != null) {
            session.deleteEntry(entry);
        }
    }

    protected EtcdResult set(DocumentModel entry) {
        EtcdService etcdService = Framework.getService(EtcdService.class);
        int ttl = ((Long) entry.getPropertyValue("ttl")).intValue();
        return etcdService.set((String) entry.getPropertyValue("key"), (String) entry.getPropertyValue("value"), ttl);
    }

    public void saveSet(String key, String value, int ttl) {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        Session session = directoryService.open(DIRECTORY_NAME);

        Map<String, Object> entry = new HashMap<>();
        entry.put("key", key);
        entry.put("action", ACTION.SET.toString());
        entry.put("value", value);
        entry.put("ttl", ttl);

        try {
            if (session.hasEntry(key)) {
                DocumentModel doc = session.getEntry(key);
                entry.remove("key");
                doc.setProperties(SCHEMA_NAME, entry);

                session.updateEntry(doc);
            } else {
                session.createEntry(entry);
            }
        } finally {
            session.close();
        }
    }
}

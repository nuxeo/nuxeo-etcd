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

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.etcd.EtcdResult;
import org.nuxeo.etcd.EtcdService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.1
 */
@RunWith(FeaturesRunner.class)
@Features({RuntimeFeature.class, EmbeddedAutomationServerFeature.class, MockitoFeature.class})
@Deploy({"org.nuxeo.ecm.platform.url.api", "org.nuxeo.ecm.platform.url.core", "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core", "org.nuxeo.etcd", "org.nuxeo.etcd.test"})
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestRetrier {

    @Inject
    EtcdRetrier retrier;

    @Inject
    DirectoryService directoryService;

    @Mock
    EtcdService etcdService;

    @Before
    public void setup() {
        EventServiceAdmin event = Framework.getService(EventServiceAdmin.class);
        event.setListenerEnabledFlag(EtcdRetrierListener.EVENT, false);

        Session session = directoryService.open(EtcdRetrier.DIRECTORY_NAME);
        try {
            session.query(new HashMap<>()).forEach(session::deleteEntry);
        } finally {
            session.close();
        }
    }

    @Test
    public void testDirectory() {
        EtcdRetrier service = Framework.getService(EtcdRetrier.class);
        assertNotNull(service);

        assertEquals(0, getDirectorySize());
    }

    @Test
    public void testSave() {
        assertEquals(0, getDirectorySize());

        retrier.saveSet("/bim/bam", "value", -1);
        assertEquals(1, getDirectorySize());

        retrier.saveSet("/bim/bam", "value2", -1);
        assertEquals(1, getDirectorySize());

        retrier.saveSet("/bim/bam/ds", "value2", -1);
        assertEquals(2, getDirectorySize());
    }

    @Test
    public void testRetry() {
        EtcdRetrier localRetrier = Mockito.mock(EtcdRetrier.class);
        doCallRealMethod().when(localRetrier).retry();
        doCallRealMethod().when(localRetrier).handleEntry(any(), any());

        assertEquals(0, getDirectorySize());
        retrier.saveSet("/bim/bam", "value", -1);
        retrier.saveSet("/bim/bam1", "value", -1);
        retrier.saveSet("/bim/bam2", "value", -1);
        retrier.saveSet("/bim/bam3", "value", -1);
        retrier.saveSet("/bim/bam4", "value", -1);
        retrier.saveSet("/bim/bam5", "value", -1);
        assertEquals(6, getDirectorySize());

        Mockito.when(localRetrier.set(Matchers.any())).thenReturn(
                new EtcdResult(), new EtcdResult(), null, new EtcdResult(), null, null);

        localRetrier.retry();
        assertEquals(3, getDirectorySize());
    }


    protected int getDirectorySize() {
        Session session = directoryService.open(EtcdRetrier.DIRECTORY_NAME);
        try {
            return session.query(new HashMap<>()).size();
        } finally {
            session.close();
        }
    }
}

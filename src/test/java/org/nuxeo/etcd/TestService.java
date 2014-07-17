/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     nuxeo.io Team
 */

package org.nuxeo.etcd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, EmbeddedAutomationServerFeature.class })
@Deploy({ "org.nuxeo.ecm.automation.test", "org.nuxeo.ecm.platform.url.api",
        "org.nuxeo.ecm.platform.url.core", "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core", "org.nuxeo.ecm.automation.io",
        "org.nuxeo.etcd", "org.nuxeo.etcd.test" })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestService {

    @Inject
    EtcdService etcdService;

    @Before
    public void doBefore() {
        MockEtcdRoot.keysToValues.clear();
    }

    @Test
    public void testServiceRegistration() {
        Assert.assertNotNull(etcdService);
    }

    @Test
    public void shouldSetValueAndReturnOldOne() {
        EtcdResult result = etcdService.set("mykey", "myvalue");
        Assert.assertNull(result.prevNode);
        Assert.assertEquals("myvalue", result.node.value);

        result = etcdService.set("mykey", "newvalue");
        Assert.assertEquals("newvalue", result.node.value);
        Assert.assertEquals("myvalue", result.prevNode.value);
    }

    @Test
    public void shouldGetValueFromKey() {
        etcdService.set("mykey", "myvalue");
        EtcdResult result = etcdService.get("mykey");
        Assert.assertEquals("myvalue", result.node.value);

        Assert.assertEquals("myvalue", etcdService.getValue("mykey"));
    }

    @Test
    public void shouldReturnNullOnMissingKey() {
        Assert.assertNull(etcdService.get("mykey"));
        Assert.assertNull(etcdService.getValue("mykey"));
    }

    @Test
    public void shouldDeleteKey() {
        etcdService.set("mykey", "myvalue");

        EtcdResult result = etcdService.delete("mykey");
        Assert.assertNull(result.node.value);
        Assert.assertEquals("myvalue", result.prevNode.value);

        result = etcdService.get("mykey");
        Assert.assertNull(result);
    }
}

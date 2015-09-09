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

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.representation.Form;

/**
 * @since 1.0
 */
public class EtcdServiceImpl extends DefaultComponent implements EtcdService {

    private static final Log log = LogFactory.getLog(EtcdServiceImpl.class);

    public static final String CONFIGURATION_EP = "configuration";

    public static final String ETCD_URL_FORMAT = "%s/v2/keys/";

    protected EtcdConfigurationDescriptor configuration = new EtcdConfigurationDescriptor();

    protected Client client;

    protected WebResource service;

    @Override
    public void activate(ComponentContext context) {
        ClientConfig config = new DefaultClientConfig();
        client = Client.create(config);
    }

    @Override
    public EtcdResult set(String key, String value) {
        return set(key, value, -1);
    }

    protected String computeURL() {
        String endpoint = configuration.getEndpoint();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return String.format(ETCD_URL_FORMAT, endpoint);
    }

    @Override
    public EtcdResult set(String key, String value, int ttl) {
        WebResource webResource = service.path(key);
        Form form = new Form();
        form.add("value", value);
        if (ttl > 0) {
            form.add("ttl", ttl);
        }

        ClientResponse response = webResource.put(ClientResponse.class, form);
        int status = response.getStatus();
        if (status != Response.Status.CREATED.getStatusCode()
                && status != Response.Status.OK.getStatusCode()) {
            String message = "Error while setting '%s' key on etcd. Status code: %d";
            log.error(String.format(message, key, response.getStatus()));
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(response.getEntityInputStream(),
                        EtcdResult.class);
            } catch (IOException e) {
                log.error(e, e);
            }
        }
        return null;
    }

    @Override
    public EtcdResult get(String key) {
        WebResource webResource = service.path(key);
        ClientResponse response = webResource.get(ClientResponse.class);
        if (response.getStatus() != Response.Status.OK.getStatusCode()
                && response.getStatus() != Response.Status.NOT_FOUND.getStatusCode()) {
            String message = "Error while getting '%s' key on etcd. Status code: %d";
            log.error(String.format(message, key, response.getStatus()));
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            EtcdResult result = mapper.readValue(
                    response.getEntityInputStream(), EtcdResult.class);
            return result.errorCode == 100 ? null : result;
        } catch (IOException e) {
            log.error(e, e);
            return null;
        }
    }

    @Override
    public String getValue(String key) {
        EtcdResult result = get(key);
        return result != null && result.node != null ? result.node.value : null;
    }

    @Override
    public EtcdResult delete(String key) {
        return delete(key, false);
    }

    @Override
    public EtcdResult delete(String key, boolean recursive) {
        WebResource webResource = service.path(key);
        if (recursive) {
            webResource = webResource.queryParam("recursive", "true");
        }

        ClientResponse response = webResource.delete(ClientResponse.class);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            String message = "Error while getting '%s' key on etcd. Status code: %d";
            log.error(String.format(message, key, response.getStatus()));
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getEntityInputStream(),
                    EtcdResult.class);
        } catch (IOException e) {
            log.error(e, e);
        }
        return null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (CONFIGURATION_EP.equalsIgnoreCase(extensionPoint)) {
            configuration = (EtcdConfigurationDescriptor) contribution;
            service = client.resource(computeURL());
        } else {
            log.warn("Trying to register a contribution for an unknown extension point: "
                    + extensionPoint);
        }
    }

}

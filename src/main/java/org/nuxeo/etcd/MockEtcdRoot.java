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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@Path("/v2/keys")
@Produces("application/json;charset=UTF-8")
@WebObject(type = "EtcdRoot")
public class MockEtcdRoot extends ModuleRoot {

    protected static Map<String, String> keysToValues = new HashMap<>();

    @PUT
    @Path("{key}")
    public Object doPutKey(@PathParam("key")
    String key) throws IOException {
        FormData form = ctx.getForm();
        String value = form.getFormProperty("value");
        boolean created = false;
        String oldValue = keysToValues.put(key, value);
        if (oldValue == null) {
            oldValue = value;
            created = true;
        }

        if (created) {
            String createdJson = "{\n" + "  \"action\": \"set\",\n"
                    + "  \"node\": {\n" + "    \"key\": \"/%s\",\n"
                    + "    \"value\": \"%s\",\n"
                    + "    \"modifiedIndex\": 19827,\n"
                    + "    \"createdIndex\": 19827\n" + "  }\n" + "}";
            return Response.ok(
                    new InputStreamBlob(new ByteArrayInputStream(String.format(
                            createdJson, key, value).getBytes("UTF-8")),
                            "application/json")).status(Response.Status.CREATED).build();
        } else {
            String modifiedJson = "{\n" + "  \"action\": \"set\",\n"
                    + "  \"node\": {\n" + "    \"key\": \"/%s\",\n"
                    + "    \"value\": \"%s\",\n"
                    + "    \"modifiedIndex\": 19827,\n"
                    + "    \"createdIndex\": 19827\n" + "  },\n"
                    + "  \"prevNode\": {\n" + "    \"key\": \"/%s\",\n"
                    + "    \"value\": \"%s\",\n"
                    + "    \"modifiedIndex\": 19824,\n"
                    + "    \"createdIndex\": 19824\n" + "  }\n" + "}";
            return Response.ok(
                    new InputStreamBlob(new ByteArrayInputStream(String.format(
                            modifiedJson, key, value, key, oldValue).getBytes(
                            "UTF-8")), "application/json")).status(
                    Response.Status.OK).build();
        }
    }

    @GET
    @Path("{key}")
    public Object doGetKey(@PathParam("key")
    String key) throws UnsupportedEncodingException {
        if (!keysToValues.containsKey(key)) {
            String s = "{\n" + "    \"cause\": \"%s\",\n"
                    + "    \"errorCode\": 100,\n" + "    \"index\": 1,\n"
                    + "    \"message\": \"Key Not Found\"\n" + "}";
            return Response.status(Response.Status.NOT_FOUND).entity(
                    new InputStreamBlob(new ByteArrayInputStream(String.format(
                            s, key).getBytes("UTF-8")), "application/json")).build();
        }

        String s = "{\n" + "    \"action\": \"get\",\n" + "    \"node\": {\n"
                + "        \"createdIndex\": 2,\n"
                + "        \"key\": \"/%s\",\n"
                + "        \"modifiedIndex\": 2,\n"
                + "        \"value\": \"%s\"\n" + "    }\n" + "}";

        return new InputStreamBlob(new ByteArrayInputStream(String.format(s,
                key, keysToValues.get(key)).getBytes("UTF-8")),
                "application/json");
    }

    @DELETE
    @Path("{key}")
    public Object doDeleteKey(@PathParam("key")
    String key) throws UnsupportedEncodingException {
        if (!keysToValues.containsKey(key)) {
            return Response.status(Response.Status.NOT_FOUND);
        }

        String s = "{\n" + "    \"action\": \"delete\",\n"
                + "    \"node\": {\n" + "        \"createdIndex\": 3,\n"
                + "        \"key\": \"/%s\",\n"
                + "        \"modifiedIndex\": 4\n" + "    },\n"
                + "    \"prevNode\": {\n" + "        \"key\": \"/%s\",\n"
                + "        \"value\": \"%s\",\n"
                + "        \"modifiedIndex\": 3,\n"
                + "        \"createdIndex\": 3\n" + "    }\n" + "}";

        return new InputStreamBlob(new ByteArrayInputStream(String.format(s,
                key, key, keysToValues.remove(key)).getBytes("UTF-8")),
                "application/json");
    }
}

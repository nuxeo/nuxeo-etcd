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

/**
 * Service used to manage environments on the cloud.
 *
 * @since 1.0
 */
public interface EtcdService {

    /**
     * Sets the {@code value} for the given {@code key}.
     */
    EtcdResult set(String key, String value);

    /**
     * Sets the {@code value} for the given {@code key} with an optional ttl.
     *
     * @param ttl the ttl value, not used if < 0.
     */
    EtcdResult set(String key, String value, int ttl);

    /**
     * Returns the given {@code key}, null if not found.
     */
    EtcdResult get(String key);

    /**
     * Returns the value of given {@code key}, null if not found.
     */
    String getValue(String key);

    /**
     * Deletes the given {@code key}.
     */
    EtcdResult delete(String key);

    /**
     * Deletes the given {@code key}.
     */
    EtcdResult delete(String key, boolean recursive);

}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.api.resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * The <code>StreamProvider</code> interface defines the API to be implemented
 * by class providing access to some data in the form of an input stream. For
 * example this interface may be implemented by an implementation of the
 * {@link Resource} interface if the resource abstract access to a JCR Node. In
 * this case the {@link #getInputStream()} method would try to access a property
 * whose value would be returned as an input stream.
 */
public interface StreamProvider {

    /**
     * Returns an <code>InputStream</code> to read the data from this
     * provider. if the provider can stream data. Otherwise <code>null</code>
     * is returned.
     * <p>
     * For a JCR Repository based implementation this method may return the
     * stream of the <code>jcr:content/jcr:data</code> property of an
     * <code>nt:file</code> node.
     *
     * @throws IOException May be thrown if an error occurrs trying to create
     *             the input stream.
     */
    InputStream getInputStream() throws IOException;

}

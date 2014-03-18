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
package org.apache.sling.replication.serialization;

import java.io.InputStream;

/**
 * A {@link org.apache.sling.replication.serialization.ReplicationPackage} importer
 */
public interface ReplicationPackageImporter {

    /**
     * Synchronously import the stream of a {@link org.apache.sling.replication.serialization.ReplicationPackage}
     *
     * @param stream the <code>InputStream</code> of the given <code>ReplicationPackage</code>
     * @param type   the <code>String</code> representing the ({@link ReplicationPackage#getType() type} of the given package
     * @return <code>true</code> if successfully imported, <code>false</code> otherwise
     */
    boolean importStream(InputStream stream, String type);

}

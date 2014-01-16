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
package org.apache.sling.replication.serialization.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component(metatype = false)
@Service(value = ReplicationPackageBuilder.class)
@Property(name = "name", value = VoidReplicationPackageBuilder.NAME)
public class VoidReplicationPackageBuilder implements ReplicationPackageBuilder {
    public static final String NAME = "void";

    public ReplicationPackage createPackage(ReplicationRequest request) throws ReplicationPackageBuildingException {
        return new VoidReplicationPackage(request, "VOID");
    }

    public ReplicationPackage readPackage(InputStream stream, boolean install) throws ReplicationPackageReadingException {
        try {
            return VoidReplicationPackage.fromStream(stream);
        } catch (Exception e) {
            throw new ReplicationPackageReadingException(e);
        }
    }

    public ReplicationPackage getPackage(String id) {
        try {
            return VoidReplicationPackage.fromStream(new ByteArrayInputStream(id.getBytes()));
        }
        catch (IOException ex){
            return null;
        }
    }
}

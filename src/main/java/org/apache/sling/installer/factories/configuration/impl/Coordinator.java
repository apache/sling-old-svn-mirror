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
package org.apache.sling.installer.factories.configuration.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Coordinator service.
 *
 * All operations should be synced on the {@link #SHARED} instance.
 */
public class Coordinator {

    /**
     * Shared instance for syncing and keeping track of operations.
     */
    public static final Coordinator SHARED = new Coordinator();

    /**
     * Entries expire after this number of milliseconds (defaults to 5 secs)
     */
    private static final long EXPIRY = 5000;

    /**
     * An operation
     */
    public static final class Operation {
        public final String pid;
        public final String factoryPid;
        public final boolean isDelete;
        public final long created;

        public Operation(final String pid, final String factoryPid, final boolean isDelete) {
            created = System.currentTimeMillis();
            this.pid = pid;
            this.factoryPid = factoryPid;
            this.isDelete = isDelete;
        }

        @Override
        public String toString() {
            return "Operation [pid=" + pid + ", factoryPid=" + factoryPid
                    + ", isDelete=" + isDelete + ", created=" + created + "]";
        }
    }

    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * The list of operations.
     */
    private final List<Operation> operations = new ArrayList<Coordinator.Operation>();

    /**
     * Private constructor
     */
    private Coordinator() {
        // private constructor
    }

    public void add(final Operation op) {
        this.cleanup();
        this.operations.add(op);
        logger.debug("Adding {}", op);
    }

    public Operation get(final String pid, final String factoryPid, final boolean isDelete) {
        this.cleanup();
        logger.debug("Searching {} : {} - {}", new Object[] {pid, factoryPid, isDelete});
        Operation result = null;
        final Iterator<Operation> i = this.operations.iterator();
        while ( i.hasNext() ) {
            final Operation op = i.next();
            if ( op.isDelete == isDelete ) {
                if ( op.pid.equals(pid) ) {
                    if ( (op.factoryPid == null && factoryPid == null)
                      || (op.factoryPid != null && op.factoryPid.equals(factoryPid)) ) {
                        result = op;
                        i.remove();
                        break;
                    }
                }
            }
        }
        logger.debug("Result ({} : {} - {}) : {}", new Object[] {pid, factoryPid, isDelete, result});
        return result;
    }

    /**
     * Clean up the list of operations.
     * Remove all entries which are older then the {@link #EXPIRY}
     */
    private void cleanup() {
        final long time = System.currentTimeMillis() - EXPIRY;
        final Iterator<Operation> i = this.operations.iterator();
        while ( i.hasNext() ) {
            final Operation op = i.next();
            if ( op.created <= time ) {
                logger.debug("Deleting expired {}", op);
                i.remove();
            } else {
                break;
            }
        }
    }
}

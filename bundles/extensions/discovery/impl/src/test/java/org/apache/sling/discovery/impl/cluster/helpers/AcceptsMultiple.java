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
package org.apache.sling.discovery.impl.cluster.helpers;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;

public class AcceptsMultiple implements TopologyEventAsserter {

    private final Type[] acceptedTypes;

    private final Map<Type, Integer> counts = new HashMap<Type, Integer>();

    public AcceptsMultiple(Type... acceptedTypes) {
        this.acceptedTypes = acceptedTypes;
    }

    public synchronized void assertOk(TopologyEvent event) {
        for (int i = 0; i < acceptedTypes.length; i++) {
            Type aType = acceptedTypes[i];
            if (aType == event.getType()) {
                // perfect
                Integer c = counts.remove(aType);
                if (c == null) {
                    counts.put(aType, new Integer(1));
                } else {
                    counts.put(aType, new Integer(c + 1));
                }
                return;
            }
        }

        throw new IllegalStateException("Got an Event which I did not expect: "
                + event.getType());
    }

    public synchronized int getEventCnt(Type type) {
        Integer i = counts.get(type);
        if (i!=null) {
            return i;
        } else {
            return 0;
        }
    }

}

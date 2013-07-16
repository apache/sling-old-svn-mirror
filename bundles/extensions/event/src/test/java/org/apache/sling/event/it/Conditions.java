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
package org.apache.sling.event.it;

import java.util.Collection;

import org.apache.sling.testing.tools.retry.RetryLoop.Condition;

/**
 * The <tt>Conditions</tt> class references commonly-used conditions
 * 
 */
public abstract class Conditions {

    /**
     * Creates a condition which expects the collection to not be empty
     * 
     * @param collection the collection to verify
     * @param description the description to use for the condition
     * @return the condition
     */
    public static Condition collectionIsNotEmptyCondition(final Collection<?> collection, String description) {
        return new DescribedCondition(description) {
            @Override
            public boolean isTrue() throws Exception {
                return collection.size() > 0;
            }
        };
    }

    private Conditions() {
        // prevent instantiation
    }

    static abstract class DescribedCondition implements Condition {

        private final String description;

        public DescribedCondition(String description) {
            this.description = description;
        }

        @Override
        public String getDescription() {
            return description;
        }

    }
}

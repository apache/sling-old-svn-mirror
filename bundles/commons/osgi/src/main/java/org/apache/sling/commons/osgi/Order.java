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
package org.apache.sling.commons.osgi;

/**
 * Order for services being sorted by {@link ServiceUtil#getComparableForServiceRanking(java.util.Map, Order)}
 * or {@link RankedServices}. Either {@link #ASCENDING} meaning services with lowest service ranking first or 
 * {@link #DESCENDING} meaning services with highest service ranking first.
 */
public enum Order {
    ASCENDING(-1, 1),
    DESCENDING(1, -1);

    public final int lessThan;
    public final int greaterThan;

    private Order(int lessThan, int greaterThan) {
        this.lessThan = lessThan;
        this.greaterThan = greaterThan;
    }
}
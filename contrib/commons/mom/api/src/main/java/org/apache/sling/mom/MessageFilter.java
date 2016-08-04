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

package org.apache.sling.mom;

import java.util.Map;

/**
 * Created by ieb on 30/03/2016.
 * Filter inbound messages, optionally implemented by QueueReaders.
 */
public interface MessageFilter {
    /**
     * Provides message filtering, the implementation should return true if it wants to get the message, false if not.
     * It should make these checks as fast as possible with minimal overhead to avoid consuming resources. Do not implement
     * this method to process the message. Implementation code calling this method will be very latency sensitive
     * and subscriptions using slow implementations may get unsubscribed.
     * @param name the name of the queue or topic the message was sent on.
     * @param mapMessage the message content.
     * @return true if the message is to be allowed through the filter.
     */
    boolean accept(Types.Name name, Map<String, Object> mapMessage);





}

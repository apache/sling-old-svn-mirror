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
 *
 * To implement a topic subscriber implement this interface, register it as an OSGi service and the TopicManager
 * which will implement a OSGi Whiteboard pattern will register it based on the values in the OSGi property "topics".
 * The component may optionally implement MessageFilter if it wants to separate filtering messages sooner.
 */
public interface Subscriber {

    /**
     * This is a String[] OSGi property containing the topic names this subscriber should subscribe to.
     */
    String TOPIC_NAMES_PROP = "topics";

    /**
     * Will be called with each message matching the filters the TopicListener is registered with.
     *
     * @param topic
     * @param message
     */
    void onMessage(Types.TopicName topic, Map<String, Object> message);


}

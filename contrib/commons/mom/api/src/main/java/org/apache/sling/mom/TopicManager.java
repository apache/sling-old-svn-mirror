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


import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by ieb on 30/03/2016.
 * Manages Topics allowing callers to publish messages onto a Topic and Subscribe to a topic.
 *
 * To create a subscriber implement the Subscriber interface and the implementation of TopicManager should
 * implement the OSGi whiteboard pattern.
 */
public interface TopicManager {



    /**
     * Publish a message to a topic with a command name.
     * @param name the name
     * @param commandName the command name
     * @param message the message
     */
    void publish(Types.TopicName name, Types.CommandName commandName, Map<String, Object> message);



}

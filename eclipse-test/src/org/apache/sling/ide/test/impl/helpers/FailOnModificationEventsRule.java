/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.test.impl.helpers;

import org.apache.sling.ide.transport.CommandExecutionProperties;
import org.apache.sling.ide.transport.Repository.CommandExecutionFlag;
import org.osgi.service.event.Event;

/**
 * The <tt>FailOnModificationEventsRule</tt> registers an event listener and fails the test under execution if
 * modification events are fired, whether they are successful or not
 *
 */
public class FailOnModificationEventsRule extends AbstractFailOnUnexpectedEventsRule {

    @Override
    public void handleEvent(Event event) {

        String type = (String) event.getProperty(CommandExecutionProperties.ACTION_TYPE);

        if ("AddOrUpdateNodeCommand".equals(type) || "ReorderChildNodesCommand".equals(type)
                || "DeleteNodeCommand".equals(type)) {
            String flags = (String) event.getProperty(CommandExecutionProperties.ACTION_FLAGS);

            // it's OK to create prerequisites if needed
            if (flags == null || !CommandExecutionFlag.CREATE_ONLY_WHEN_MISSING.toString().equals(flags)) {
                addUnexpectedEvent(event);
            }
        }
    }

}
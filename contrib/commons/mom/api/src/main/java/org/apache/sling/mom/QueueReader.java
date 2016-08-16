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
 * A queue reader receives messages from the queue in the onMessage method. It should avoid performing processing in
 * the onMessage method aiming to return as fast as possible.
 *
 * This interface should be implemented as an OSGi Service. The implementation of the MoM API should register any services
 * implementing QueueReader using the OSGi Whiteboard pattern.
 */
public interface QueueReader  {


    /**
     * Configuration property name for QueueReaders implemented using a whiteboard pattern.
     */
    String QUEUE_NAME_PROP = "queue-name";

    /**
     * Receive messages from the queue. If the message cant be processed at this time a RequeueMessageException must be thrown to cause the message
     * to be requeued. Any other exception will cause the message to fail without a retry. Messages that fail are dropped and not notified except the logs.
     * Implementors should avoid failing any message without a retry.
     * @param queueName the name of the queue
     * @param message the message
     * @throws RequeueMessageException when the message must be re-queued.
     */
    void onMessage(Types.QueueName queueName, Map<String, Object> message) throws RequeueMessageException;


}

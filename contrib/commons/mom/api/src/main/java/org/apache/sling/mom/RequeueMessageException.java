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

/**
 * Created by ieb on 02/04/2016.
 * Thown when a queue reader cant process a message and needs to indicate that the message sould be re-queued and retried some time later.
 */
public class RequeueMessageException extends Exception {

    /**
     *
     * @param message
     */
    public RequeueMessageException(String message) {
        super(message);
    }

    /**
     *
     * @param message
     * @param cause
     */
    public RequeueMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}

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
package org.apache.sling.jcr.api;

import javax.jcr.LoginException;

/**
 * The <code>TooManySessionsException</code> is a special
 * <code>LoginException</code> thrown when the number of active sessions in
 * a given session pool has reached the configured maximum number of sessions.
 */
public class TooManySessionsException extends LoginException {

    public TooManySessionsException(String message) {
        super(message);
    }

    public TooManySessionsException(Throwable t) {
        super(t);
    }

    public TooManySessionsException(String message, Throwable t) {
        super(message, t);
    }

}

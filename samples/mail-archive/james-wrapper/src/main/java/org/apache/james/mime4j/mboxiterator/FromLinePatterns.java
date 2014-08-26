/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.mime4j.mboxiterator;

/**
 * Collection of From_ line patterns. Messages inside an mbox are separated by these lines.
 * The pattern is usually constant in a file but depends on the mail agents that wrote it.
 * It's possible that more mailer agents wrote in the same file using different From_ lines.
 */
public interface FromLinePatterns {

    /**
     * Match a line like: From ieugen@apache.org Fri Sep 09 14:04:52 2011
     */
    static final String DEFAULT = "^From \\S+@\\S.*\\d{4}$";

    /**
     * Other type of From_ line: From MAILER-DAEMON Wed Oct 05 21:54:09 2011
     */


}

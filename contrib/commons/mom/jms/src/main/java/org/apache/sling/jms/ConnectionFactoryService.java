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
package org.apache.sling.jms;

import javax.jms.ConnectionFactory;

/**
 * Created by ieb on 30/03/2016.
 * Implementations of this service provide JMS Connection factories. In general implementations should work OOTB with no
 * further configuration and provide an efficient JMS Connection Factory that allows sessions to be created with minimal
 * overhead.
 */
public interface ConnectionFactoryService {
    /**
     *
     * Get a connection factory.
     * @return the connection factory.
     */
    ConnectionFactory getConnectionFactory();
}

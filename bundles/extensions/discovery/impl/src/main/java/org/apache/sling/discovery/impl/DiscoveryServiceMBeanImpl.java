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
package org.apache.sling.discovery.impl;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.sling.discovery.impl.common.heartbeat.HeartbeatHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MBean implementation for the DiscoveryServiceImpl
 */
public class DiscoveryServiceMBeanImpl extends StandardMBean implements DiscoveryServiceMBean {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HeartbeatHandler heartbeatHandler;

    DiscoveryServiceMBeanImpl(HeartbeatHandler heartbeatHandler) throws NotCompliantMBeanException {
        super(DiscoveryServiceMBean.class);
        this.heartbeatHandler = heartbeatHandler;
    }

    public void startNewVoting() {
        logger.info("startNewVoting: JMX-triggered starting a new voting with the HeartbeatHandler.");
        heartbeatHandler.startNewVoting();
        logger.info("startNewVoting: new voting was started.");
    }

}

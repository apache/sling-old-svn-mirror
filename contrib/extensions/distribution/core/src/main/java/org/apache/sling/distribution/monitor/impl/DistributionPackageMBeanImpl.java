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
package org.apache.sling.distribution.monitor.impl;

/**
 * Implementation of {@link DistributionPackageMBean}
 */
public final class DistributionPackageMBeanImpl implements DistributionPackageMBean {

    private final String id;

    private final String type;

    private final String[] paths;

    private final String requestType;

    private final long size;

    private final long processingTime;

    public DistributionPackageMBeanImpl(String id,
                                        String type,
                                        String[] paths,
                                        String requestType,
                                        long size,
                                        long processingTime) {
        this.id = id;
        this.type = type;
        this.paths = paths;
        this.requestType = requestType;
        this.size = size;
        this.processingTime = processingTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String[] getPaths() {
        return paths;
    }

    @Override
    public String getRequestType() {
        return requestType;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public long getProcessingTime() {
        return processingTime;
    }

}

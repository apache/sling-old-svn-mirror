/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.engine.impl.filter;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.sling.engine.jmx.FilterProcessorMBean;

/**
 * This is the implementation of the management interface for the
 * FilterProcessorMBean.
 */
public class FilterProcessorMBeanImpl extends StandardMBean implements FilterProcessorMBean{
    
    // number of filter invocations
    private volatile long n;
    
    private volatile double meanDuration;
    
    public FilterProcessorMBeanImpl() throws NotCompliantMBeanException{
        super(FilterProcessorMBean.class);
        resetStatistics();
    }

    void addFilterHandle(FilterHandle filterHandle) {        
        this.n = filterHandle.getCalls();        
        this.meanDuration = (double)filterHandle.getTimePerCall()/1000;
    }

    @Override
    public long getInvocationsCount() {
        return this.n;
    }

    @Override
    public double getMeanFilterDurationMsec() {
        return this.meanDuration;
    }
    
    @Override
    public void resetStatistics() {
        this.n = 0;
        this.meanDuration = 0;
    }
}

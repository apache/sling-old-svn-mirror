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
package org.apache.sling.commons.threads.impl;

import java.util.concurrent.ThreadPoolExecutor;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.sling.commons.threads.impl.DefaultThreadPoolManager.Entry;
import org.apache.sling.commons.threads.jmx.ThreadPoolMBean;

class ThreadPoolMBeanImpl extends StandardMBean implements ThreadPoolMBean {
    
    private final Entry entry;

    ThreadPoolMBeanImpl(Entry entry) throws NotCompliantMBeanException {
        super(ThreadPoolMBean.class);
        this.entry = entry;
    }

    public String getBlockPolicy() {
        return this.entry.getConfig().getBlockPolicy().name();
    }

    public int getExecutorActiveCount() {
        final ThreadPoolExecutor tpe = this.entry.getExecutor();
        if ( tpe != null ) {
            return tpe.getActiveCount();
        } else {
            return -1;
        }
    }

    public long getExecutorCompletedTaskCount() {
        final ThreadPoolExecutor tpe = this.entry.getExecutor();
        if ( tpe != null ) {
            return tpe.getCompletedTaskCount();
        } else {
            return -1;
        }
    }

    public int getExecutorCorePoolSize() {
        final ThreadPoolExecutor tpe = this.entry.getExecutor();
        if ( tpe != null ) {
            return tpe.getCorePoolSize();
        } else {
            return -1;
        }
    }

    public int getExecutorLargestPoolSize() {
        final ThreadPoolExecutor tpe = this.entry.getExecutor();
        if ( tpe != null ) {
            return tpe.getLargestPoolSize();
        } else {
            return -1;
        }
    }

    public int getExecutorMaximumPoolSize() {
        final ThreadPoolExecutor tpe = this.entry.getExecutor();
        if ( tpe != null ) {
            return tpe.getMaximumPoolSize();
        } else {
            return -1;
        }
    }

    public int getExecutorPoolSize() {
        final ThreadPoolExecutor tpe = this.entry.getExecutor();
        if ( tpe != null ) {
            return tpe.getPoolSize();
        } else {
            return -1;
        }
    }

    public long getExecutorTaskCount() {
        final ThreadPoolExecutor tpe = this.entry.getExecutor();
        if ( tpe != null ) {
            return tpe.getTaskCount();
        } else {
            return -1;
        }
    }

    public long getKeepAliveTime() {
        return this.entry.getConfig().getKeepAliveTime();
    }

    public int getMaxPoolSize() {
        return this.entry.getConfig().getMaxPoolSize();
    }

    public int getMinPoolSize() {
        return this.entry.getConfig().getMinPoolSize();
    }

    public String getName() {
        return this.entry.getName();
    }

    public String getPid() {
        return this.entry.getPid();
    }

    public String getPriority() {
        return this.entry.getConfig().getPriority().name();
    }

    public int getQueueSize() {
        return this.entry.getConfig().getQueueSize();
    }

    public int getShutdownWaitTimeMs() {
        return this.entry.getConfig().getShutdownWaitTimeMs();
    }

    public boolean isDaemon() {
        return this.entry.getConfig().isDaemon();
    }

    public boolean isShutdownGraceful() {
        return this.entry.getConfig().isShutdownGraceful();
    }

    public boolean isUsed() {
        return this.entry.isUsed();
    }

}

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
package org.apache.sling.nosql.generic.adapter;

import java.util.Iterator;

import org.apache.sling.api.resource.LoginException;
import org.slf4j.Logger;

/**
 * Wrapper for {@link NoSqlAdapter} that enables logging and time counting for each call.
 */
public final class MetricsNoSqlAdapterWrapper implements NoSqlAdapter {

    private final NoSqlAdapter delegate;
    private final Logger logger;

    public MetricsNoSqlAdapterWrapper(NoSqlAdapter delegate, Logger logger) {
        this.delegate = delegate;
        this.logger = logger;
    }

    public boolean validPath(String path) {
        return delegate.validPath(path);
    }

    public NoSqlData get(String path) {
        Metrics metrics = new Metrics();
        try {
            return delegate.get(path);
        }
        finally {
            metrics.finish("get({})", path);
        }
    }

    public Iterator<NoSqlData> getChildren(String parentPath) {
        Metrics metrics = new Metrics();
        try {
            return delegate.getChildren(parentPath);
        }
        finally {
            metrics.finish("getChildren({})", parentPath);
        }
    }

    public boolean store(NoSqlData data) {
        Metrics metrics = new Metrics();
        try {
            return delegate.store(data);
        }
        finally {
            metrics.finish("store({})", data.getPath());
        }
    }

    public boolean deleteRecursive(String path) {
        Metrics metrics = new Metrics();
        try {
            return delegate.deleteRecursive(path);
        }
        finally {
            metrics.finish("deleteRecursive({})", path);
        }
    }

    public Iterator<NoSqlData> query(String query, String language) {
        Metrics metrics = new Metrics();
        try {
            return delegate.query(query, language);
        }
        finally {
            metrics.finish("query({})", query);
        }
    }
    
    @Override
    public void checkConnection() throws LoginException {
        delegate.checkConnection();
    }

    private class Metrics {
        
        private long startTime;
        
        public Metrics() {
            if (logger.isDebugEnabled()) {
                startTime = System.currentTimeMillis();
            }
        }
        
        public void finish(String message, Object... data) {
            if (logger.isDebugEnabled()) {
                long duration = System.currentTimeMillis() - startTime;
                logger.debug(message + " - " + duration + "ms", data);
            }
        }
        
    }
    
}

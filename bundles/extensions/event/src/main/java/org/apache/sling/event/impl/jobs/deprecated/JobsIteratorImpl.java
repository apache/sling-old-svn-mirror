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
package org.apache.sling.event.impl.jobs.deprecated;

import java.util.Iterator;

import org.apache.sling.event.JobsIterator;
import org.osgi.service.event.Event;

/**
 * Implementation of the jobs iterator
 * @deprecated
 */
@Deprecated
public class JobsIteratorImpl implements JobsIterator {

    private final org.apache.sling.event.jobs.JobsIterator delegatee;

    public JobsIteratorImpl(final org.apache.sling.event.jobs.JobsIterator i) {
        this.delegatee = i;
    }

    /**
     * @see org.apache.sling.event.JobsIterator#close()
     * @deprecated
     */
    @Override
    @Deprecated
    public void close() {
        // nothing to do
    }

    /**
     * @see org.apache.sling.event.jobs.JobsIterator#getPosition()
     */
    @Override
    public long getPosition() {
        return this.delegatee.getPosition();
    }

    /**
     * @see org.apache.sling.event.jobs.JobsIterator#getSize()
     */
    @Override
    public long getSize() {
        return this.delegatee.getSize();
    }

    /**
     * @see org.apache.sling.event.jobs.JobsIterator#skip(long)
     */
    @Override
    public void skip(long skipNum) {
        this.delegatee.skip(skipNum);
    }

    /**
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        return this.delegatee.hasNext();
    }

    /**
     * @see java.util.Iterator#next()
     */
    @Override
    public Event next() {
        return this.delegatee.next();
    }

    /**
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
        this.delegatee.remove();
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Event> iterator() {
        return this.delegatee.iterator();
    }
}

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
package org.apache.sling.event.impl.jobs;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.sling.event.jobs.JobsIterator;
import org.osgi.service.event.Event;

/**
 * Jobs iterator.
 */
public class JobsIteratorImpl implements JobsIterator {

    /** The events list size. */
    private final long size;

    /** The current position. */
    private int index = 0;

    /** The iterator. */
    private final Iterator<Event> iter;

    public JobsIteratorImpl(final List<Event> events) {
        this.size = events.size();
        this.iter = events.iterator();
    }

    /**
     * @see org.apache.sling.event.jobs.JobsIterator#getPosition()
     */
    public long getPosition() {
        return this.index;
    }

    /**
     * @see org.apache.sling.event.jobs.JobsIterator#getSize()
     */
    public long getSize() {
        return this.size;
    }

    /**
     * @see org.apache.sling.event.jobs.JobsIterator#skip(long)
     */
    public void skip(long skipNum) {
        if ( skipNum < 0 ) {
            throw new IllegalArgumentException();
        }
        if ( this.index + skipNum >= this.size ) {
            throw new NoSuchElementException();
        }
        for(long i=0; i<skipNum; i++) {
            this.iter.next();
        }
        this.index += skipNum;
    }

    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return this.iter.hasNext();
    }

    /**
     * @see java.util.Iterator#next()
     */
    public Event next() {
        return this.iter.next();
    }

    /**
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Event> iterator() {
        return this;
    }
}

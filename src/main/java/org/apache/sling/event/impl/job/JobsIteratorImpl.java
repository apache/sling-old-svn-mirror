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
package org.apache.sling.event.impl.job;

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.event.JobsIterator;
import org.apache.sling.event.impl.JobEventHandler;
import org.osgi.service.event.Event;

/**
 * JCR Based Implementation of the jobs iterator
 */
public class JobsIteratorImpl implements JobsIterator {

    private NodeIterator delegatee;

    private Session session;

    private JobEventHandler handler;

    public JobsIteratorImpl(final NodeIterator ni,
                            final Session session,
                            final JobEventHandler handler) {
        this.delegatee = ni;
        this.session = session;
        this.handler = handler;
    }

    /**
     * @see org.apache.sling.event.JobsIterator#close()
     */
    public void close() {
        if ( this.session != null ) {
            this.session.logout();
            this.session = null;
            this.delegatee = null;
            this.handler = null;
        }
    }

    /**
     * @see org.apache.sling.event.JobsIterator#getPosition()
     */
    public long getPosition() {
        return this.delegatee.getPosition();
    }

    /**
     * @see org.apache.sling.event.JobsIterator#getSize()
     */
    public long getSize() {
        return this.delegatee.getSize();
    }

    /**
     * @see org.apache.sling.event.JobsIterator#skip(long)
     */
    public void skip(long skipNum) {
        this.delegatee.skip(skipNum);
    }

    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        final boolean result = (this.delegatee == null ? false : this.delegatee.hasNext());
        if ( !result ) {
            this.close();
        }
        return result;
    }

    /**
     * @see java.util.Iterator#next()
     */
    public Event next() {
        final Node n = this.delegatee.nextNode();
        try {
            return this.handler.forceReadEvent(n);
        } catch (RepositoryException e) {
            // if something goes wrong, we shutdown the iterator
            this.close();
            throw (NoSuchElementException)new NoSuchElementException("Repository exception during job reading").initCause(e);
        }
    }

    /**
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException("Remove not supported.");
    }
}

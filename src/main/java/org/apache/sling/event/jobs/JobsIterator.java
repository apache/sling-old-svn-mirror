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
package org.apache.sling.event.jobs;

import java.util.Iterator;

import org.osgi.service.event.Event;

import aQute.bnd.annotation.ProviderType;

/**
 * This <code>Iterator</code> allows to iterate over {@link Event}s.
 * In addition to an iterator it might return the number of elements
 * in the collection and allows to skip several elements.
 * @since 3.0
 * @deprecated - Use the new {@link JobManager#findJobs} methods instead.
 */
@Deprecated
@ProviderType
public interface JobsIterator extends Iterator<Event>, Iterable<Event> {

    /**
     * Skip a number of jobs.
     * @param skipNum the non-negative number of elements to skip
     * @throws java.util.NoSuchElementException
     *          if skipped past the last job in the iterator.
     */
    void skip(long skipNum);

    /**
     * Returns the total number of jobs. In some cases a precise information
     * is not available. In these cases -1 is returned.
     */
    long getSize();

    /**
     * Returns the current position within the iterator. The number returned is
     * the 0-based index of the next job.
     */
    long getPosition();
}

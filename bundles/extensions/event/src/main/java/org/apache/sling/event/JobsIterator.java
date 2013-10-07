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
package org.apache.sling.event;

import org.osgi.service.event.Event;

import aQute.bnd.annotation.ProviderType;

/**
 * This <code>Iterator</code> allows to iterate over {@link Event}s.
 * In addition to an iterator it might return the number of elements
 * in the collection and allows to skip several elements.
 * If the iterator is not used to iterate through the whole collection
 * of jobs, the {@link #close()} method must be called in order to
 * free resources!
 * @deprecated
 */
@Deprecated
@ProviderType
public interface JobsIterator extends org.apache.sling.event.jobs.JobsIterator {

    /**
     * Releases this iterators resources immediately instead of waiting for this
     * to happen when it is automatically closed. After a call to close, this
     * iterator should not be used anymore.
     * The iterator is closed automatically when it reaches it's end.
     */
    void close();
}

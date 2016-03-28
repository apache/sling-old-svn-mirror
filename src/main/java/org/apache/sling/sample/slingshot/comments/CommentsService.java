/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.sample.slingshot.comments;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;

/**
 * Service for handling the comments
 */
public interface CommentsService {

    /**
     * Return the path to the comments resource for a resource.
     * @param resource The content resource, this is usually an entry.
     * @return The path to the comments resource or {@code null} if
     *         the passed in content resource is not part of
     *         Slingshot.
     */
    String getCommentsResourcePath(final Resource resource);

    void addComment(final Resource resource, final Comment c)
    throws PersistenceException;
}

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
package org.apache.sling.resourceresolver.impl.helper;

import java.util.Iterator;
import java.util.Set;

import org.apache.sling.api.resource.Resource;

/**
 * This iterator removes duplicated Resource entries. Regular resources
 * overrides the synthetic ones.
 */
public class UniqueResourceIterator extends AbstractIterator<Resource> {

    private final Iterator<Resource> input;

    private final Set<String> visited;

    public UniqueResourceIterator(final Set<String> visited, final Iterator<Resource> input) {
        this.input = input;
        this.visited = visited;
    }

    @Override
    protected Resource seek() {
        while (input.hasNext()) {
            final Resource next = input.next();
            final String name = next.getName();

            if (visited.contains(name)) {
                continue;
            } else {
                visited.add(name);
                next.getResourceMetadata().setResolutionPath(next.getPath());
                return next;
            }
        }

        return null;
    }
}


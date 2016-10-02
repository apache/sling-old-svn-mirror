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
package org.apache.sling.pipes.internal;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.Plumber;
import org.apache.sling.pipes.ReferencePipe;

import java.util.Collections;
import java.util.Iterator;

/**
 * executes a pipe referred in the configuration, but invert output:
 * nothing if the pipe has something, input if the pipe has nothing
 */
public class NotPipe extends ReferencePipe {

    public static final String RESOURCE_TYPE = "slingPipes/not";

    public NotPipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
    }

    @Override
    public Iterator<Resource> getOutput() {
        if (reference.getOutput().hasNext()){
            return EMPTY_ITERATOR;
        }
        return Collections.singleton(getInput()).iterator();
    }
}

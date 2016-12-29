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
package org.apache.sling.ide.filter;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.annotation.NonNull;

public interface FilterLocator {

    /**
     * Loads a filter for the given project (which determines which parts of the repository should be overwritten)
     * 
     * @param IProject the Eclipse project from which to retrieve the filter
     * @return the filter
     * @throws IOException, IllegalStateException in case the filter could not be retrieved from the project
     */
    @NonNull Filter loadFilter(@NonNull IProject project) throws IOException, IllegalStateException;
}

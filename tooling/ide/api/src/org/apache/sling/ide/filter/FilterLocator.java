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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface FilterLocator {

    // TODO - should be File[] to allow multiple lookups, see {filter-vlt.xml, filter.xml}
    File findFilterLocation(File syncDirectory);

    /**
     * Loads a filter based on the raw <tt>filterFileContents</tt>
     * 
     * <p>
     * If the <tt>filterFileContents</tt> is null it returns a default filter
     * </p>
     * 
     * @param filterFileContents the raw contents of the filter file, possibly null
     * @return
     * @throws IOException
     */
    Filter loadFilter(InputStream filterFileContents) throws IOException;
}

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
package org.apache.sling.ide.impl.vlt.filter;

import java.io.IOException;
import java.io.InputStream;

import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.config.ConfigurationException;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.impl.AggregateManagerImpl;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterResult;

public class VltFilter implements Filter {

    private DefaultWorkspaceFilter filter;

    public VltFilter(InputStream in) throws IOException, ConfigurationException {

        if (in != null) {
            filter = new DefaultWorkspaceFilter();
            filter.load(in);
        } else {
            filter = AggregateManagerImpl.getDefaultWorkspaceFilter();
        }

    }

    @Override
    public FilterResult filter(String relativeFilePath) {

        if (relativeFilePath.length() > 0 && relativeFilePath.charAt(0) != '/') {
            relativeFilePath = '/' + relativeFilePath;
        }

        if (filter.contains(relativeFilePath)) {
            return FilterResult.ALLOW;
        }

        for (PathFilterSet pathFilterSet : filter.getFilterSets()) {
            if (pathFilterSet.getRoot().startsWith(relativeFilePath)) {
                return FilterResult.PREREQUISITE;
            }
        }

        return FilterResult.DENY;
    }

}

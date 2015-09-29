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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;

import org.apache.jackrabbit.vault.fs.config.ConfigurationException;
import org.apache.sling.ide.filter.FilterResult;
import org.junit.Test;

public class VltFilterTest {

    @Test
    public void defaultFilterExcludedVarClasses() throws IOException, ConfigurationException {

        assertThat(newFilter("filter-default.xml").filter("/var/classes"), is(FilterResult.DENY));
        
    }

    private VltFilter newFilter(String filterFile) throws IOException, ConfigurationException {
        InputStream input = getClass().getResourceAsStream(filterFile);
        if (input == null) {
            throw new IllegalArgumentException("Unable to load filter from classpath location " + filterFile);
        }
        return new VltFilter(input);
    }

    @Test
    public void fallbackFilterExcludedVarClasses() throws IOException, ConfigurationException {

        VltFilter filter = new VltFilter(null);

        assertThat(filter.filter("/var/classes"), is(FilterResult.DENY));

    }

    @Test
    public void defaultFilterIncludesLibs() throws IOException, ConfigurationException {

        assertThat(newFilter("filter-default.xml").filter("/libs"), is(FilterResult.ALLOW));
    }

    @Test
    public void pathMissingLeadingSlashIsCorrected() throws IOException, ConfigurationException {

        assertThat(newFilter("filter-default.xml").filter("libs"), is(FilterResult.ALLOW));
    }

    @Test
    public void deepFilterHasParentDirectoriesAsPrerequisites() throws IOException, ConfigurationException {

        String[] parents = new String[] { "/libs", "/libs/sling", "/libs/sling/servlet" };
        for (String parent : parents) {
            assertThat("Parent '" + parent + "'", newFilter("filter-deep.xml").filter(parent),
                    is(FilterResult.PREREQUISITE));
        }
    }

}

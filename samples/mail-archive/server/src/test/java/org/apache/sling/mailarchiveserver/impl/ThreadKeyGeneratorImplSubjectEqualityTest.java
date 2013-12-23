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
package org.apache.sling.mailarchiveserver.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ThreadKeyGeneratorImplSubjectEqualityTest {
    private ThreadKeyGeneratorImpl generator = new ThreadKeyGeneratorImpl();
    private final String orig;
    private final String re;

    public ThreadKeyGeneratorImplSubjectEqualityTest(String input, String expected) {
        this.orig = input;
        this.re = expected;
    }

    @Parameters(name="{0}")
    public static List<Object[]> data() {
        final List<Object[]> result = new ArrayList<Object[]>();

        result.add(new Object[] {"Chef cookbooks for Installing FAQ & packages", "Re: Chef cookbooks for Installing FAQ & packages"} ); 
        result.add(new Object[] {"Dropbox to throw random files in and be accessible through http/ ftp?", "Re: Dropbox to throw random files in and be accessible through http/ ftp?"} ); 
        result.add(new Object[] {"Dropbox to throw random files in and be accessible through http/ ftp?", "RE: Dropbox to throw random files in and be accessible through http/ ftp?"} ); 
        result.add(new Object[] {"FAQ integration guidelines for ES3", " \u7B54\u590D: FAQ integration guidelines for ES3"} ); 

        return result;
    }

    @Test
    public void testGetThreadKey() {
        assertEquals(generator.getThreadKey(orig), generator.getThreadKey(re));
    }
}

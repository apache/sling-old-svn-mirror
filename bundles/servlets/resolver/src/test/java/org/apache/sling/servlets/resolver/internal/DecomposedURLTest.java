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
package org.apache.sling.servlets.resolver.internal;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.request.RequestPathInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DecomposedURLTest {
    private final RequestPathInfo rpi;
    private final String path;
    private final String extension;
    private final String selectors;
    private String suffix;
    
    @Parameters(name="{0}")
    public static List<Object[]> data() {
            final List<Object[]> result = new ArrayList<Object[]>();
            result.add(new Object[] { 
                    "http://localhost:8080/libs/foo/content/something/formitems.truc.who.json/image/vnd/xnd/knd.xml",
                    "/libs/foo/content/something/formitems",
                    "json",
                    "truc.who",
                    "/image/vnd/xnd/knd.xml"
            }); 
            result.add(new Object[] { 
                    "/libs/foo.a4.print.html",
                    "/libs/foo",
                    "html",
                    "a4.print",
                    null
            }); 
            result.add(new Object[] { 
                    "/libs/bar.html",
                    "/libs/bar",
                    "html",
                    null,
                    null
            });
            
            // dots are always considered separators between path and selectors/extension,
            // as mentioned on the webconsole plugin path warning.
            result.add(new Object[] { 
                    "/home/users/geometrixx-outdoors/emily.andrews@mailinator.com/profile.form.html/content/geometrixx-outdoors/en/user/profile",
                    "/home/users/geometrixx-outdoors/emily",
                    "com",
                    "andrews@mailinator",
                    "/profile.form.html/content/geometrixx-outdoors/en/user/profile"
            });
            
            // the trick is to replace dots with _, which gives the same results as
            // if a resource exists at ...@mailinator.com/profile
            result.add(new Object[] { 
                    "/home/users/geometrixx-outdoors/emily_andrews@mailinator_com/profile.form.html/content/geometrixx-outdoors/en/user/profile",
                    "/home/users/geometrixx-outdoors/emily_andrews@mailinator_com/profile",
                    "html",
                    "form",
                    "/content/geometrixx-outdoors/en/user/profile"
            }); 
            return result;
    }
    
    public DecomposedURLTest(String input, String path, String extension, String selectors, String suffix) {
        rpi = new DecomposedURL(input).getRequestPathInfo();
        this.path = path;
        this.extension = extension;
        this.selectors = selectors;
        this.suffix = suffix;
    }
    
    @Test
    public void checkPath() {
        assertEquals(path, rpi.getResourcePath());
    }
    
    @Test
    public void checkExtension() {
        assertEquals(extension, rpi.getExtension());
    }
    
    @Test
    public void checkSelectors() {
        assertEquals(selectors, rpi.getSelectorString());
    }
    
    @Test
    public void checkSuffix() {
        assertEquals(suffix, rpi.getSuffix());
    }
}

/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine.extension;

import java.util.LinkedHashMap;
import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.sling.scripting.sightly.render.AbstractRuntimeObjectModel;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.render.RuntimeObjectModel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class URIManipulationFilterExtensionTest {

    private static RenderContext renderContext;
    private URIManipulationFilterExtension underTest;

    @BeforeClass
    public static void prepareTests() {
        // wire only the AbstractRuntimeObjectModel
        renderContext = new RenderContext() {
            @Override
            public RuntimeObjectModel getObjectModel() {
                return new AbstractRuntimeObjectModel() {
                };
            }

            @Override
            public Bindings getBindings() {
                return new SimpleBindings();
            }

            @Override
            public Object call(String functionName, Object... arguments) {
                return null;
            }
        };
    }

    @Before
    public void setUp() {
        underTest = new URIManipulationFilterExtension();
    }

    @Test
    public void testPercentEncodedURLs_SLING_6761() {
        assertEquals(
                "/example/search.html?q=6%25-10%25",
                underTest.call(renderContext, "/example/search?q=6%25-10%25", new LinkedHashMap<String, Object>() {{
                    put(URIManipulationFilterExtension.EXTENSION, "html");
                }})
        );
        assertEquals(
                "/example/search.a.html?q=6%25-10%25&s=%40sling&t=%25sling",
                underTest.call(renderContext, "/example/search?q=6%25-10%25", new LinkedHashMap<String, Object>() {{
                    put(URIManipulationFilterExtension.EXTENSION, "html");
                    put(URIManipulationFilterExtension.ADD_SELECTORS, "a");
                    put(URIManipulationFilterExtension.ADD_QUERY, new LinkedHashMap<String, Object>() {{
                        put("s", "@sling");
                        put("t", "%sling");
                    }});
                }})
        );
    }


}

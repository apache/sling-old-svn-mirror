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

import java.net.URI;
import java.net.URISyntaxException;
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
    public void testUnescapePercentInQuery() throws URISyntaxException {
        URI testUri = new URI("http", null, "example.com", -1, "/example/search.html", "q=6%25-10%25", "fragment=%");
        assertEquals("http://example.com/example/search.html?q=6%25-10%25#fragment=%25",
                URIManipulationFilterExtension.unescapePercentInQuery(testUri.toString()));
        testUri = new URI("http", null, "example.com", -1, "/example/search.html", "q=6%25-10%25", null);
        assertEquals("http://example.com/example/search.html?q=6%25-10%25",
                URIManipulationFilterExtension.unescapePercentInQuery(testUri.toString()));
    }

    @Test
    public void testConcatenateWithSlashes() {
        assertEquals("a/b/c", URIManipulationFilterExtension.concatenateWithSlashes("a", "b", "c"));
        assertEquals("a/b/c", URIManipulationFilterExtension.concatenateWithSlashes("a", "/b", "c"));
        assertEquals("a/b/c", URIManipulationFilterExtension.concatenateWithSlashes("a", "/b", "/c"));
        assertEquals("/b/", URIManipulationFilterExtension.concatenateWithSlashes("/", "b", "/"));
        assertEquals("/one/two/", URIManipulationFilterExtension.concatenateWithSlashes("/one/", "/two/"));
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

    @Test
    public void testOpaqueUris() {
        // see SLING_7000
        assertEquals(
                "mailto:test@apache.org",
                underTest.call(renderContext, "mailto:test@apache.org", new LinkedHashMap<String, Object>() {{
                    put(URIManipulationFilterExtension.EXTENSION, "html");
                }})
        );
        // check other opaque links

        // data url according to https://tools.ietf.org/html/rfc2397
        String dataUrl =
                "data:image/gif;base64,R0lGODdhMAAwAPAAAAAAAP///ywAAAAAMAAw" +
                        "AAAC8IyPqcvt3wCcDkiLc7C0qwyGHhSWpjQu5yqmCYsapyuvUUlvONmOZtfzgFz" +
                        "ByTB10QgxOR0TqBQejhRNzOfkVJ+5YiUqrXF5Y5lKh/DeuNcP5yLWGsEbtLiOSp" +
                        "a/TPg7JpJHxyendzWTBfX0cxOnKPjgBzi4diinWGdkF8kjdfnycQZXZeYGejmJl" +
                        "ZeGl9i2icVqaNVailT6F5iJ90m6mvuTS4OK05M0vDk0Q4XUtwvKOzrcd3iq9uis" +
                        "F81M1OIcR7lEewwcLp7tuNNkM3uNna3F2JQFo97Vriy/Xl4/f1cf5VWzXyym7PH" +
                        "hhx4dbgYKAAA7";

        assertEquals(
                dataUrl,
                underTest.call(renderContext, dataUrl, new LinkedHashMap<String, Object>() {{
                    put(URIManipulationFilterExtension.EXTENSION, "html");
                }})
        );
    }
}

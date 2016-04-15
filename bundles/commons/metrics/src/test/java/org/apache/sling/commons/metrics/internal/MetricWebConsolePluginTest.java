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

package org.apache.sling.commons.metrics.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.codahale.metrics.JvmAttributeGaugeSet;
import com.codahale.metrics.MetricRegistry;
import com.gargoylesoftware.htmlunit.StringWebResponse;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HTMLParser;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import org.apache.felix.inventory.Format;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetricWebConsolePluginTest {
    @Rule
    public final OsgiContext context = new OsgiContext();

    private MetricWebConsolePlugin plugin = new MetricWebConsolePlugin();

    private static Map<String, Object> regProps(String name) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("name", name);
        return props;
    }

    @Test
    public void consolidatedRegistry() throws Exception {
        MetricRegistry reg1 = new MetricRegistry();
        reg1.meter("test1");
        context.registerService(MetricRegistry.class, reg1, regProps("foo"));

        activatePlugin();

        MetricRegistry consolidated = plugin.getConsolidatedRegistry();

        //Check name decoration
        assertEquals(1, consolidated.getMetrics().size());
        assertTrue(consolidated.getMeters().containsKey("foo:test1"));

        MetricRegistry reg2 = new MetricRegistry();
        reg2.meter("test2");
        context.registerService(MetricRegistry.class, reg2);

        //Metric Registry without name would not be decorated
        consolidated = plugin.getConsolidatedRegistry();
        assertEquals(2, consolidated.getMetrics().size());
        assertTrue(consolidated.getMeters().containsKey("test2"));

        //Duplicate metric in other registry should not fail. Warning log
        //should be generated
        MetricRegistry reg3 = new MetricRegistry();
        reg3.meter("test2");
        context.registerService(MetricRegistry.class, reg3);
        consolidated = plugin.getConsolidatedRegistry();
        assertEquals(2, consolidated.getMetrics().size());

        MetricRegistry reg4 = new MetricRegistry();
        reg4.meter("test1");
        context.registerService(MetricRegistry.class, reg4, regProps("bar"));
        consolidated = plugin.getConsolidatedRegistry();
        assertTrue(consolidated.getMeters().containsKey("foo:test1"));
        assertTrue(consolidated.getMeters().containsKey("bar:test1"));
    }

    @Test
    public void inventory_text() throws Exception {
        MetricRegistry reg1 = new MetricRegistry();
        reg1.meter("test1").mark(5);
        context.registerService(MetricRegistry.class, reg1, regProps("foo"));

        activatePlugin();

        StringWriter sw = new StringWriter();
        plugin.print(new PrintWriter(sw), Format.TEXT, false);

        String out = sw.toString();
        assertThat(out, containsString("foo:test1"));
        assertThat(out, containsString("Meters"));
    }

    @Test
    public void webConsolePlugin() throws Exception {
        MetricRegistry reg1 = new MetricRegistry();
        reg1.meter("test1").mark(5);
        reg1.timer("test2").time().close();
        reg1.histogram("test3").update(743);
        reg1.counter("test4").inc(9);
        reg1.registerAll(new JvmAttributeGaugeSet());
        context.registerService(MetricRegistry.class, reg1, regProps("foo"));

        activatePlugin();

        StringWriter sw = new StringWriter();

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        plugin.doGet(mock(HttpServletRequest.class), response);

        WebClient client = new WebClient();
        WebResponse resp = new StringWebResponse(sw.toString(), WebClient.URL_ABOUT_BLANK);
        HtmlPage page = HTMLParser.parseHtml(resp, client.getCurrentWindow());

        assertTable("data-meters", page);
        assertTable("data-counters", page);
        assertTable("data-timers", page);
        assertTable("data-histograms", page);
        assertTable("data-guages", page);
    }

    private void assertTable(String name, HtmlPage page) {
        HtmlTable table = page.getHtmlElementById(name);
        assertNotNull(table);

        //1 for header and 1 for actual metric row
        assertThat(table.getRowCount(), greaterThanOrEqualTo(2));

    }

    private void activatePlugin() {
        MockOsgi.activate(plugin, context.bundleContext(), Collections.<String, Object>emptyMap());
    }
}

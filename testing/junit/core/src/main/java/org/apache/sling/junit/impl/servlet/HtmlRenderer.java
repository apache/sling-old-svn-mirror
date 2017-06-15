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
package org.apache.sling.junit.impl.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.junit.Renderer;
import org.apache.sling.junit.RendererFactory;
import org.apache.sling.junit.TestSelector;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/** HTML renderer for JUnit servlet */
@Component(immediate=false)
@Service(serviceFactory=true)
public class HtmlRenderer extends RunListener implements Renderer,RendererFactory {

    public static final String EXTENSION = "html";
    private PrintWriter output;

    public Renderer createRenderer() {
        return new HtmlRenderer();
    }

    public boolean appliesTo(TestSelector s) {
        // This is our default renderer, applies to the empty
        // extension as well
        return EXTENSION.equals(s.getExtension()) || "".equals(s.getExtension());
    }

    public String getExtension() {
        return EXTENSION;
    }

    public void info(String cssClass, String str) {
        output.println("<p class='" + cssClass + "'>");
        HtmlFilter.escape(output, str);
        output.println("</p>");
    }

    public void list(String cssClass, Collection<String> data) {
        output.println("<ul class='testNames'>");
        for(String str : data) {
            output.println("<li>");
            link(str, str + ".html", "GET");
            output.println("</li>");
        }
        output.println("</ul>");
    }

    public void title(int level, String title) {
        output.print("<h" + level + ">");
        HtmlFilter.escape(output, title);
        output.print("</h" + level + ">");
    }

    public void link(String info, String url, String method) {
        output.println("<div class='link'>");

        if("POST".equalsIgnoreCase(method)) {
            output.print("<form method='POST' action='");
            output.print(url);
            output.print("'>");
            output.print("<input type='submit' value='");
            HtmlFilter.escape(output, info);
            output.print("'/>");
            output.println("</form>");
        } else {
            output.print("<a href='");
            output.print(url);
            output.print("'>");
            HtmlFilter.escape(output, info);
            output.println("</a>");
        }

        output.println("</div>");
    }

    public void setup(HttpServletResponse response, String pageTitle) throws IOException, UnsupportedEncodingException {
        if(output != null) {
            throw new IllegalStateException("Output Writer already set");
        }
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        output = response.getWriter();
        output.println("<html><head>");
        output.println("<link rel='stylesheet' type='text/css' href='" + ServletProcessor.CSS + "'/>");
        output.print("<title>");
        HtmlFilter.escape(output, pageTitle);
        output.println("</title>");
        output.println("</head><body><h1>");
        HtmlFilter.escape(output, pageTitle);
        output.println("</h1>");
    }

    public void cleanup() {
        output.println("</body>");
        output.println("</html>");
        output = null;
    }

    public RunListener getRunListener() {
        return this;
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        super.testFailure(failure);
        output.print("<div class='failure'><h3>");
        output.print("TEST FAILED: ");
        HtmlFilter.escape(output, failure.getTestHeader());
        output.print("</h3><div class='failureDetails'>");
        output.print("<div>");
        HtmlFilter.escape(output, failure.toString());
        output.print("</div><div>Stack Trace: ");
        HtmlFilter.escape(output, failure.getTrace());
        output.println("</div></div></div>");
    }

    @Override
    public void testFinished(Description description) throws Exception {
        super.testFinished(description);
        output.print("<p class='finished'>Test finished: ");
        HtmlFilter.escape(output, description.toString());
        output.println("</p></div>");
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        super.testIgnored(description);
        output.print("<p class='ignored'><h3>TEST IGNORED</h3><b>");
        HtmlFilter.escape(output, description.toString());
        output.println("</b></p>");
    }

    private void counter(String name, String cssName, int value) {
        final String cssClass = cssName + (value > 0 ? "NonZero" : "Zero");
        output.print("<span class='");
        output.print(cssClass);
        output.print("'>");
        HtmlFilter.escape(output, name);
        output.print(":");
        HtmlFilter.escape(output, String.valueOf(value));
        output.println("</span>");
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        super.testRunFinished(result);
        String cssClass = "testRun ";
        if(result.getFailureCount() > 0) {
            cssClass += "failure";
        } else if(result.getIgnoreCount() > 0) {
            cssClass += "ignored";
        } else {
            cssClass += "success";
        }

        output.println("<p class='testRun'>");
        output.print("TEST RUN FINISHED: ");
        counter("tests", "testCount", result.getRunCount());
        output.print(", ");
        counter("failures", "failureCount", result.getFailureCount());
        output.print(", ");
        counter("ignored", "ignoredCount", result.getIgnoreCount());
        output.println("</p>");
    }

    @Override
    public void testRunStarted(Description description)
            throws Exception {
        super.testRunStarted(description);
    }

    @Override
    public void testStarted(Description description) throws Exception {
        super.testStarted(description);
        output.println("<div class='test'>");
    }
}

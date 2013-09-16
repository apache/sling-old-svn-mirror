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
package org.apache.sling.commons.log.logback.integration.remote;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.MatchingFilter;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.apache.sling.commons.log.logback.ConfigProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Marker;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 * Test bundle activator which registers all type of extension point supported by bundle
 * Used by ITWebConsoleRemote to assert output of the WebConsole Plugin
 */
public class WebConsoleTestActivator implements BundleActivator {
    public static Class[] BUNDLE_CLASS_NAMES = {
        WebConsoleTestActivator.class,
        WebConsoleTestTurboFilter.class,
        WebConsoleTestConfigProvider.class,
        WebConsoleTestAppender.class,
        WebConsoleTestFilter.class,
    };

    @Override
    public void start(BundleContext context) throws Exception {

        context.registerService(TurboFilter.class.getName(),new WebConsoleTestTurboFilter(),null);
        context.registerService(ConfigProvider.class.getName(),new WebConsoleTestConfigProvider(),null);

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        String prefix = "WebConsoleTest";
        String[] loggers = {
                prefix + ".foo.bar:DEBUG",
                prefix + ".foo.baz:INFO",
        };

        props.put("loggers", loggers);
        context.registerService(Appender.class.getName(), new WebConsoleTestAppender(), props);

        Dictionary<String, Object> props2 = new Hashtable<String, Object>();
        props2.put("appenders", "WebConsoleTestAppender");
        context.registerService(Filter.class.getName(), new WebConsoleTestFilter(), props2);

        String configAsString = "<included> <!-- WebConsoleTestComment --></included>";
        context.registerService(String.class.getName(), configAsString, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }

    private static class WebConsoleTestTurboFilter extends MatchingFilter {
        @Override
        public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
            if(logger.getName().equals("turbofilter.foo.bar")){
                return FilterReply.DENY;
            }
            return FilterReply.NEUTRAL;
        }
    }

    private static class WebConsoleTestFilter extends Filter<ILoggingEvent> {

        @Override
        public FilterReply decide(ILoggingEvent event) {
            if(event.getLoggerName().equals("filter.foo.bar")){
                return FilterReply.DENY;
            }
            return FilterReply.NEUTRAL;
        }
    }

    private static class WebConsoleTestAppender extends AppenderBase<ILoggingEvent> {
        final List<ILoggingEvent> events = new ArrayList<ILoggingEvent>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            events.add(eventObject);
        }

        @Override
        public String getName() {
            return "WebConsoleTestAppender";
        }
    }

    private static class WebConsoleTestConfigProvider implements ConfigProvider {

        public InputSource getConfigSource() {
            String config = "<included>  <appender name=\"FILE\" class=\"ch.qos.logback.core.FileAppender\">\n" +
                    "    <file>${sling.home}/logs/webconsoletest1.log</file>\n" +
                    "    <encoder>\n" +
                    "      <pattern>%d %-5level %logger{35} - %msg %n</pattern>\n" +
                    "    </encoder>\n" +
                    "  </appender></included>";
            return new InputSource(new StringReader(config));
        }
    }
}

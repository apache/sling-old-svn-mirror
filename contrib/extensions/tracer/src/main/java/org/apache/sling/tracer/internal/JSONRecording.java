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

package org.apache.sling.tracer.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import ch.qos.logback.classic.Level;
import com.google.common.primitives.Longs;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import static com.google.common.base.Preconditions.checkArgument;

class JSONRecording implements Recording, Comparable<JSONRecording> {
    private static final String[] QUERY_API_PKGS = {
            "org.apache.sling.resourceresolver", //Sling package would come first in stack so listed first
            "org.apache.jackrabbit.oak"
    };
    private static final Object[] EMPTY = new Object[0];
    private static final Logger log = LoggerFactory.getLogger(JSONRecording.class);
    public static final String OAK_QUERY_PKG = "org.apache.jackrabbit.oak.query";
    private final String method;
    private final String requestId;
    private final String uri;
    private final boolean compress;
    private final List<QueryEntry> queries = new ArrayList<QueryEntry>();
    private final List<LogEntry> logs = new ArrayList<LogEntry>();
    private RequestProgressTracker tracker;
    private byte[] json;
    private final long start = System.currentTimeMillis();
    private long timeTaken;
    private final QueryLogCollector queryCollector = new QueryLogCollector();
    private final CallerFinder queryCallerFinder = new CallerFinder(QUERY_API_PKGS);

    public JSONRecording(String requestId, HttpServletRequest r, boolean compress) {
        this.requestId = requestId;
        this.compress = compress;
        this.method = r.getMethod();
        this.uri = r.getRequestURI();
    }

    public boolean render(Writer w) throws IOException {
        if (json != null) {
            Reader r = new InputStreamReader(getInputStream(false), "UTF-8");
            IOUtils.copy(r, w);
            return true;
        }
        return false;
    }

    public boolean render(OutputStream os, boolean compressed) throws IOException {
        if (json != null) {
            IOUtils.copyLarge(getInputStream(compressed), os);
            return true;
        }
        return false;
    }

    public int size() {
        if (json != null){
            return json.length;
        }
        return 0;
    }

    public String getUri() {
        return uri;
    }

    public String getRequestId() {
        return requestId;
    }

    public long getTimeTaken() {
        return timeTaken;
    }

    public long getStart() {
        return start;
    }

    //~---------------------------------------< Recording >

    @Override
    public void log(TracerConfig tc, Level level, String logger, FormattingTuple tuple) {
        if (logger.startsWith(OAK_QUERY_PKG)) {
            queryCollector.record(level, logger, tuple);
        }
        logs.add(new LogEntry(tc, level, logger, tuple));
    }

    @Override
    public void registerTracker(RequestProgressTracker tracker) {
        this.tracker = tracker;
    }

    public void done() {
        try {
            if (json == null) {
                json = toJSON();

                //Let the tracker and other references go to
                //not occupy memory
                tracker = null;
                queries.clear();
                logs.clear();
            }
        } catch (JSONException e) {
            log.warn("Error occurred while converting the log data for request {} to JSON", requestId, e);
        } catch (UnsupportedEncodingException e) {
            log.warn("Error occurred while converting the log data for request {} to JSON", requestId, e);
        } catch (IOException e) {
            log.warn("Error occurred while converting the log data for request {} to JSON", requestId, e);
        }
    }

    private byte[] toJSON() throws JSONException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream os = baos;
        if (compress) {
            os = new GZIPOutputStream(os);
        }
        OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
        JSONWriter jw = new JSONWriter(osw);
        jw.setTidy(true);
        jw.object();
        jw.key("method").value(method);

        timeTaken = System.currentTimeMillis() - start;
        jw.key("time").value(timeTaken);
        jw.key("timestamp").value(start);

        if (tracker != null) {
            jw.key("requestProgressLogs");
            jw.array();
            Iterator<String> it = tracker.getMessages();
            //Per docs iterator can be null
            while (it != null && it.hasNext()) {
                jw.value(it.next());
            }
            jw.endArray();
        }

        queryCollector.done();
        addJson(jw, "queries", queries);

        addJson(jw, "logs", logs);
        jw.endObject();
        osw.flush();
        os.close();
        return baos.toByteArray();
    }

    private void addJson(JSONWriter jw, String name, List<? extends JsonEntry> entries) throws JSONException {
        jw.key(name);
        jw.array();
        for (JsonEntry je : entries) {
            jw.object();
            je.toJson(jw);
            jw.endObject();
        }
        jw.endArray();
    }

    private InputStream getInputStream(boolean compressed) throws IOException {
        InputStream is = new ByteArrayInputStream(json);

        if (compressed) {
            checkArgument(compress, "Cannot provide compressed response with compression disabled");
            return is;
        }

        if (compress) {
            is = new GZIPInputStream(is);
        }
        return is;
    }

    @Override
    public int compareTo(@Nonnull JSONRecording o) {
        return Longs.compare(start, o.start);
    }

    private interface JsonEntry {
        void toJson(JSONWriter jw) throws JSONException;
    }

    private static class LogEntry implements JsonEntry {
        final Level level;
        final String logger;
        final FormattingTuple tuple;
        final long timestamp = System.currentTimeMillis();
        final List<StackTraceElement> caller;

        private LogEntry(TracerConfig tc, Level level, String logger, FormattingTuple tuple) {
            this.level = level != null ? level : Level.INFO;
            this.logger = logger;
            this.tuple = tuple;
            this.caller = getCallerData(tc);
        }

        private static List<StackTraceElement> getCallerData(TracerConfig tc) {
            if (tc.isReportCallerStack()){
                return tc.getCallerReporter().report();
            }
            return Collections.emptyList();
        }

        private static String toString(Object o) {
            //Make use of Slf4j null safe toString support!
            return MessageFormatter.format("{}", o).getMessage();
        }

        private static String getStackTraceAsString(Throwable throwable) {
            StringWriter stringWriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stringWriter));
            return stringWriter.toString();
        }

        @Override
        public void toJson(JSONWriter jw) throws JSONException {
            jw.key("timestamp").value(timestamp);
            jw.key("level").value(level.levelStr);
            jw.key("logger").value(logger);
            jw.key("message").value(tuple.getMessage());

            Object[] params = tuple.getArgArray();
            if (params != null) {
                jw.key("params");
                jw.array();
                for (Object o : params) {
                    jw.value(toString(o));
                }
                jw.endArray();
            }

            Throwable t = tuple.getThrowable();
            if (t != null) {
                //Later we can look into using Logback Throwable handling
                jw.key("exception").value(getStackTraceAsString(t));

            }

            if (!caller.isEmpty()){
                jw.key("caller");
                jw.array();
                for (StackTraceElement o : caller) {
                    jw.value(o.toString());
                }
                jw.endArray();
            }
        }
    }

    private static class QueryEntry implements JsonEntry {
        final String query;
        final String plan;
        final String caller;

        private QueryEntry(String query, String plan, String caller) {
            this.query = query;
            this.plan = plan;
            this.caller = caller;
        }

        @Override
        public void toJson(JSONWriter jw) throws JSONException {
            jw.key("query").value(query);
            jw.key("plan").value(plan);
            jw.key("caller").value(caller);
        }
    }

    private class QueryLogCollector {
        /**
         * The MDC key is used in org.apache.jackrabbit.oak.query.QueryEngineImpl
         */
        static final String MDC_QUERY_ID = "oak.query.id";
        String query;
        String plan;
        String id;
        String caller;

        public void record(Level level, String logger, FormattingTuple tuple) {
            String idFromMDC = MDC.get(MDC_QUERY_ID);

            //Use the query id to detect change of query execution
            //i.e. once query gets executed for current thread and next
            //query start it would cause the id to change. That would
            //be a trigger to finish up on current query collection and
            //switch to new one
            if (idFromMDC != null && !idFromMDC.equals(id)) {
                addQueryEntry();
                id = idFromMDC;
            }

            //TODO Query time. Change Oak to provide this information via some
            //dedicated Audit logging such that below reliance on impl details
            //can be avoided
            String msg = tuple.getMessage();
            if (Level.DEBUG == level && msg != null) {
                Object[] args = tuple.getArgArray() == null ? EMPTY : tuple.getArgArray();
                if (query == null){
                    if ("org.apache.jackrabbit.oak.query.QueryEngineImpl".equals(logger)
                            && msg.contains("Parsing") && args.length == 2){
                        //LOG.debug("Parsing {} statement: {}", language, statement);
                        query = nullSafeString(args[1]);
                        caller = determineCaller();
                    }
                }

                //Plan for union query are logged separately
                if (plan == null){
                    if ("org.apache.jackrabbit.oak.query.QueryImpl".equals(logger)
                            && msg.startsWith("query plan ")){
                        //logDebug("query execute " + statement);
                        plan = msg.substring("query plan ".length());
                    } else if ("org.apache.jackrabbit.oak.query.UnionQueryImpl".equals(logger)
                            && msg.contains("query union plan") && args.length > 0){
                        // LOG.debug("query union plan {}", getPlan());
                        plan = nullSafeString(args[0]);
                    }
                }
            }
        }

        private String determineCaller() {
            StackTraceElement caller = queryCallerFinder.determineCaller(Thread.currentThread().getStackTrace());
            if (caller != null) {
                return caller.toString();
            }
            return null;
        }

        public void addQueryEntry(){
            if (query != null && plan != null){
                queries.add(new QueryEntry(nullSafeTrim(query), nullSafeTrim(plan), caller));
                plan = query = null;
            }
        }

        public void done(){
            //Push any last pending entry i.e. last query
            addQueryEntry();
        }
    }

    private static String nullSafeTrim(String s){
        if(s == null){
            return "";
        }
        return s.trim();
    }

    private static String nullSafeString(Object o){
        if (o != null){
            return o.toString();
        }
        return null;
    }

}

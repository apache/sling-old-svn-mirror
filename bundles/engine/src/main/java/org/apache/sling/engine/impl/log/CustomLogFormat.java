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
package org.apache.sling.engine.impl.log;

import java.text.CharacterIterator;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.Cookie;

import org.apache.sling.engine.impl.request.RequestData;

/**
 * The <code>CustomLogFormat</code> class implements the support for log format
 * strings similar to the Apache httpd CustomLog configuration.
 *
 * @see <a
 *      href="http://sling.apache.org/site/client-request-logging.html">Client
 *      Request Logging</a> for documentation of supported formats.
 */
class CustomLogFormat {

    /*
     * NOTE: Documentation at
     * https://cwiki.apache.org/confluence/display/SLINGxSITE
     * /Client+Request+Logging should be kept in sync with this class !
     */

    /**
     * The parsed list of log format parts whose <code>print</code> method is
     * called when building the log message line.
     */
    Parameter[] logParameters;

    /**
     * Creates a new instance from of this class parsing the log format pattern.
     *
     * @param pattern The pattern to be parsed.
     */
    CustomLogFormat(String pattern) {
        this.logParameters = this.parse(pattern);
        if (this.logParameters.length == 0) {
            this.logParameters = null;
        }
    }

    /**
     * Creates a log message from the given <code>request</code> and
     * <code>response</code> objects according to the log format from which this
     * instance has been created.
     *
     * @param request The {@link RequestLoggerRequest} used to extract values
     *            for the log message.
     * @param response The {@link RequestLoggerResponse} used to extract values
     *            for the log message.
     * @return The formatted log message or <code>null</code> if this log
     *         formatter has not been initialized with a valid log format
     *         pattern.
     */
    String format(RequestLoggerRequest request, RequestLoggerResponse response) {
        if (this.logParameters != null) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < this.logParameters.length; i++) {
                this.logParameters[i].print(buf, request, response);
            }
            return buf.toString();
        }

        return null;
    }

    /**
     * Returns a string representation of this log format instance. The returned
     * String is actually rebuilt from the parsed format string and may be used
     * to create another instance of this class with the same format string.
     *
     * @return String representation of this instance.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; this.logParameters != null && i < this.logParameters.length; i++) {
            buf.append(this.logParameters[i]);
        }
        return buf.toString();
    }

    // ---------- Parsing the format pattern -----------------------------------

    private Parameter[] parse(String pattern) {

        List<Parameter> parameterList = new ArrayList<Parameter>();
        StringBuilder buf = new StringBuilder();

        CharacterIterator sr = new StringCharacterIterator(pattern);

        for (int c = sr.first(); c != CharacterIterator.DONE; c = sr.next()) {
            if (c == '%') {
                int c1 = sr.next();
                if (c1 != '%') {
                    if (buf.length() > 0) {
                        Parameter text = new PlainTextParameter(buf.toString());
                        parameterList.add(text);
                        buf.setLength(0);
                    }

                    Parameter param = this.parseFormatString(sr, c1);
                    if (param != null) {
                        parameterList.add(param);
                    }
                    continue;
                }
            }

            buf.append((char) c);
        }

        // append any remaining plain text
        if (buf.length() > 0) {
            Parameter text = new PlainTextParameter(buf.toString());
            parameterList.add(text);
            buf.setLength(0);
        }

        return parameterList.toArray(new Parameter[parameterList.size()]);
    }

    private Parameter parseFormatString(CharacterIterator sr, int c) {

        // read all modifiers
        boolean required = true;
        int[] statCodes = null;
        while (c != CharacterIterator.DONE) {
            if (c == '!') {
                required = false;
            } else if (c >= '0' && c <= '9') {
                statCodes = this.parseStatusCodes(sr, c);
            } else if (c == '>' || c == '<') {
                // ignore first/last modifiers
            } else {
                break;
            }

            c = sr.next();
        }

        // read name
        String name;
        if (c == '{') {
            StringBuilder nameBuf = new StringBuilder();
            for (c = sr.next(); c != CharacterIterator.DONE && c != '}'; c = sr.next()) {
                nameBuf.append((char) c);
            }
            name = (nameBuf.length() > 0) ? nameBuf.toString() : null;

            // get the format indicator
            c = sr.next();
        } else {
            name = null;
        }

        Parameter param;
        switch (c) {
            case 'a':
                param = new RemoteIPParameter();
                break;

            case 'A':
                param = new LocalIPParameter();
                break;

            case 'b':
            case 'B':
                param = new ByteCountParameter();
                break;

            case 'C':
                param = (name == null) ? null : new CookieParameter(name, true);
                break;

            case 'D':
                param = new DurationParameter(false);
                break;

            case 'f':
                // we assume the path to the content the request resolved to
                param = new ContentPathParameter();
                break;

            case 'h':
                param = new RemoteHostParameter();
                break;

            case 'H':
                param = new ProtocolParameter();
                break;

            case 'i':
                param = (name == null) ? null : new HeaderParameter(name, true);
                break;

            case 'm':
                param = new MethodParameter();
                break;

            case 'M':
                param = new ParamParameter(name);
                break;

            case 'o':
                param = (name == null) ? null : new HeaderParameter(name, false);
                break;

            case 'p':
                param = new LocalPortParameter();
                break;

            case 'P':
                // %{format}P form is not currently supported
                param = new ThreadParameter(name);
                break;

            case 'q':
                param = new QueryParameter();
                break;

            case 'r':
                param = new FirstRequestLineParameter();
                break;

            case 'R':
                param = new IdParameter();
                break;

            case 's':
                param = new StatusParameter();
                break;

            case 't':
                // %{format}t form is not currently supported
                param = new TimeParameter(name);
                break;

            case 'T':
                param = new DurationParameter(true);
                break;

            case 'u':
                param = new UserParameter();
                break;

            case 'U':
                param = new RequestParameter();
                break;

            case 'v':
            case 'V':
                param = new ServerNameParameter();
                break;

            case 'y':
                param = new AuthTypeParameter();
                break;

            case 'X': // no supported fall through to default
            case 'I': // no supported fall through to default
            case 'O': // no supported fall through to default
            case 'n': // no supported fall through to default
            case 'l': // no supported fall through to default
            case 'e': // no supported fall through to default
            default:
                param = new NonImplementedParameter(name);
                break;
        }

        if (param instanceof BaseParameter) {
            BaseParameter baseParam = (BaseParameter) param;
            baseParam.setParName((char) c);
            baseParam.setRequired(required);
            baseParam.setStatusLimits(statCodes);
        }

        return param;
    }

    private int[] parseStatusCodes(CharacterIterator sr, int c) {
        StringBuilder buf = new StringBuilder();
        buf.append((char) c);

        List<Integer> numbers = new ArrayList<Integer>();
        for (c = sr.next(); c != CharacterIterator.DONE; c = sr.next()) {
            if (c == ',') {
                int num = 0;
                try {
                    num = Integer.parseInt(buf.toString());
                } catch (NumberFormatException nfe) {
                    // don't care
                }
                if (num >= 100 && num <= 999) {
                    numbers.add(num);
                }
                buf.setLength(0);
            } else if (c >= '0' && c <= '9') {
                buf.append((char) c);
            } else {
                // end of number list
                break;
            }
        }

        // reset to the last mark
        sr.previous();

        // get the last number
        int num = 0;
        try {
            num = Integer.parseInt(buf.toString());
        } catch (NumberFormatException nfe) {
            // don't care
        }
        if (num >= 100 && num <= 999) {
            numbers.add(new Integer(num));
        }

        if (numbers.isEmpty()) {
            return null;
        }

        int[] statusCodes = new int[numbers.size()];
        for (int i = 0; i < numbers.size(); i++) {
            statusCodes[i] = (numbers.get(i)).intValue();
        }
        return statusCodes;
    }

    // ---------- Parameter support --------------------------------------------

    static interface Parameter {
        void print(StringBuilder dest, RequestLoggerRequest request, RequestLoggerResponse response);
    }

    static class PlainTextParameter implements Parameter {
        private String value;

        PlainTextParameter(String value) {
            this.value = value;
        }

        public void print(StringBuilder dest, RequestLoggerRequest request, RequestLoggerResponse response) {
            dest.append(this.value);
        }

        public String toString() {
            return this.value;
        }
    }

    abstract static class BaseParameter implements Parameter {
        private int[] statusLimits;

        private boolean required;

        private char parName;

        private final String parParam;

        private final boolean isRequest;

        protected BaseParameter(String parParam, boolean isRequest) {
            this.parParam = parParam;
            this.isRequest = isRequest;
        }

        public void setParName(char parName) {
            this.parName = parName;
        }

        public void setStatusLimits(int[] statusLimits) {
            this.statusLimits = statusLimits;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        protected abstract String getValue(RequestLoggerRequest request);

        protected abstract String getValue(RequestLoggerResponse response);

        public final void print(StringBuilder dest, RequestLoggerRequest request, RequestLoggerResponse response) {
            if (this.printOk(response.getStatus())) {
                String value = this.isRequest ? this.getValue(request) : this.getValue(response);
                dest.append((value == null) ? "-" : value);
            }
        }

        protected boolean printOk(int status) {
            if (this.statusLimits == null) {
                return true;
            }

            for (int i = 0; i < this.statusLimits.length; i++) {
                if (status == this.statusLimits[i]) {
                    return this.required;
                }
            }

            return !this.required;
        }

        protected char getParName() {
            return this.parName;
        }

        protected String getParParam() {
            return this.parParam;
        }

        public String toString() {
            StringBuilder result = new StringBuilder("%");

            if (this.statusLimits != null) {
                if (!this.required) {
                    result.append('!');
                }

                for (int i = 0; i < this.statusLimits.length; i++) {
                    if (i > 0) {
                        result.append(',');
                    }
                    result.append(this.statusLimits[i]);
                }
            }

            if (this.parParam != null) {
                result.append('{').append(this.parParam).append('}');
            }
            result.append(this.parName);

            return result.toString();
        }

        // --------- helper ----------------------------------------------------

        private static boolean isPrint(char c) {
            return c >= 0x20 && c < 0x7f && c != '\\' && c != '"';
        }

        static String escape(String value) {
            // nothing to do for empty values
            if (value == null || value.length() == 0) {
                return value;
            }

            // find the first non-printable
            int i = 0;
            while (i < value.length() && isPrint(value.charAt(i))) {
                i++;
            }

            // if none has been found, just return the value
            if (i >= value.length()) {
                return value;
            }

            // otherwise copy the printable first part in a string buffer
            // and start encoding
            StringBuilder buf = new StringBuilder(value.substring(0, i));
            while (i < value.length()) {
                char c = value.charAt(i);
                if (isPrint(c)) {
                    buf.append(c);
                } else if (c == '\n') { // LF
                    buf.append("\\n");
                } else if (c == '\r') { // CR
                    buf.append("\\r");
                } else if (c == '\t') { // HTAB
                    buf.append("\\t");
                } else if (c == '\f') { // VTAB
                    buf.append("\\f");
                } else if (c == '\b') { // BSP
                    buf.append("\\b");
                } else if (c == '"') { // "
                    buf.append("\\\"");
                } else if (c == '\\') { // \
                    buf.append("\\\\");
                } else { // encode
                    buf.append("\\u");
                    if (c < 0x10) {
                        buf.append('0'); // leading zero
                    }
                    if (c < 0x100) {
                        buf.append('0'); // leading zero
                    }
                    if (c < 0x1000) {
                        buf.append('0'); // leading zero
                    }
                    buf.append(Integer.toHexString(c));
                }
                i++;
            }

            // return the encoded string value
            return buf.toString();
        }

    }

    static class NonImplementedParameter extends BaseParameter {

        NonImplementedParameter(String parParam) {
            super(parParam, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            return null;
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class ThreadParameter extends BaseParameter {
        public ThreadParameter(String parParam) {
            super(parParam, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            return Thread.currentThread().getName();
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class ParamParameter extends BaseParameter {
        public ParamParameter(String parParam) {
            super(parParam, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            return request.getParameter(this.getParParam());
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class IdParameter extends BaseParameter {
        public IdParameter() {
            super(null, false);
        }

        protected String getValue(RequestLoggerRequest request) {
            return null;
        }

        protected String getValue(RequestLoggerResponse response) {
            return String.valueOf(response.getRequestId());
        }
    }

    static class ByteCountParameter extends BaseParameter {
        public ByteCountParameter() {
            super(null, false);
        }

        protected String getValue(RequestLoggerRequest request) {
            return null;
        }

        protected String getValue(RequestLoggerResponse response) {
            int count = response.getCount();
            if (count == 0) {
                return (this.getParName() == 'b') ? "-" : "0";
            }

            return String.valueOf(count);
        }
    }

    static class TimeParameter extends BaseParameter {

        /** date format - see access logging in service() */
        private static final SimpleDateFormat accessLogFmt = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss ", Locale.US);

        /** time format for GMT offset - see access logging in service() */
        private static final DecimalFormat dfmt = new DecimalFormat("+0000;-0000");

        /** the timezone for the timezone offset calculation */
        private static final Calendar calendar = Calendar.getInstance();

        /** last zone offset (cached by hours) */
        private static String lastZoneOffset = "";

        private static long lastZoneOffsetHour = -1;

        /** last formatted time (cached in seconds) */
        private static String lastTimeFormatted = "";

        private static long lastTimeFormattedSeconds = -1;

        private final boolean requestStart;

        public TimeParameter(String parParam) {
            super(parParam, false);

            this.requestStart = parParam == null || !parParam.equals("end");
        }

        protected String getValue(RequestLoggerRequest request) {
            return null;
        }

        protected String getValue(RequestLoggerResponse response) {
            long time = this.requestStart ? response.getRequestStart() : response.getRequestEnd();
            return timeFormatted(time);
        }

        // ---------- internal
        // -----------------------------------------------------

        static String timeFormatted(long time) {
            if (time / 1000 != lastTimeFormattedSeconds) {
                lastTimeFormattedSeconds = time / 1000;
                Date date = new Date(time);
                StringBuilder buf = new StringBuilder(accessLogFmt.format(date));
                if (time / 3600000 != lastZoneOffsetHour) {
                    lastZoneOffsetHour = time / 3600000;
                    calendar.setTime(date);
                    int tzOffset = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
                    tzOffset /= (60 * 1000);
                    tzOffset = ((tzOffset / 60) * 100) + (tzOffset % 60);

                    lastZoneOffset = dfmt.format(tzOffset);
                }
                buf.append(lastZoneOffset);
                lastTimeFormatted = buf.toString();
            }
            return lastTimeFormatted;
        }
    }

    static class DurationParameter extends BaseParameter {
        private final boolean seconds;

        public DurationParameter(boolean seconds) {
            super(null, false);
            this.seconds = seconds;
        }

        protected String getValue(RequestLoggerRequest request) {
            return null;
        }

        protected String getValue(RequestLoggerResponse response) {
            long time = response.getRequestDuration();
            if (this.seconds) {
                time /= 1000;
            }
            return String.valueOf(time);
        }
    }

    static class RemoteIPParameter extends BaseParameter {
        public RemoteIPParameter() {
            super(null, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            return request.getRemoteAddr();
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class RemoteHostParameter extends BaseParameter {
        public RemoteHostParameter() {
            super(null, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            return request.getRemoteHost();
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class LocalIPParameter extends BaseParameter {
        public LocalIPParameter() {
            super(null, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            return request.getLocalAddr();
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class LocalPortParameter extends BaseParameter {
        public LocalPortParameter() {
            super(null, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            return String.valueOf(request.getServerPort());
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class ServerNameParameter extends BaseParameter {
        public ServerNameParameter() {
            super(null, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            return request.getServerName();
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class ContentPathParameter extends BaseParameter {
        public ContentPathParameter() {
            super(null, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            final Object resourcePath = request.getAttribute(RequestData.REQUEST_RESOURCE_PATH_ATTR);
            if (resourcePath instanceof String) {
                return (String) resourcePath;
            }
            return null;
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class FirstRequestLineParameter extends BaseParameter {
        public FirstRequestLineParameter() {
            super(null, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            String query = request.getQueryString();
            query = (query == null || query.length() == 0) ? "" : "?" + query;

            return request.getMethod() + " " + request.getRequestURI() + query + " " + request.getProtocol();
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class ProtocolParameter extends BaseParameter {
        public ProtocolParameter() {
            super(null, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            return request.getProtocol();
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class MethodParameter extends BaseParameter {
        public MethodParameter() {
            super(null, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            return request.getMethod();
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class RequestParameter extends BaseParameter {
        public RequestParameter() {
            super(null, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            return request.getRequestURI();
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class QueryParameter extends BaseParameter {
        public QueryParameter() {
            super(null, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            String query = request.getQueryString();
            return (query == null || query.length() == 0) ? "" : "?" + query;
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class UserParameter extends BaseParameter {
        public UserParameter() {
            super(null, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            final String user = request.getRemoteUser();
            return (user == null) ? null : escape(user);
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class AuthTypeParameter extends BaseParameter {
        public AuthTypeParameter() {
            super(null, true);
        }

        protected String getValue(RequestLoggerRequest request) {
            final String authType = request.getAuthType();
            return (authType == null) ? null : escape(authType);
        }

        protected String getValue(RequestLoggerResponse response) {
            return null;
        }
    }

    static class StatusParameter extends BaseParameter {
        public StatusParameter() {
            super(null, false);
        }

        protected String getValue(RequestLoggerRequest request) {
            return null;
        }

        protected String getValue(RequestLoggerResponse response) {
            return String.valueOf(response.getStatus());
        }
    }

    static class CookieParameter extends BaseParameter {
        private String cookieName;

        CookieParameter(String cookieName, boolean isRequest) {
            super(cookieName, isRequest);
            this.cookieName = cookieName;
        }

        protected String getValue(RequestLoggerRequest request) {
            return getValue(request.getCookie(this.cookieName));
        }

        protected String getValue(RequestLoggerResponse response) {
            return getValue(response.getCookie(this.cookieName));

        }

        private String getValue(final Cookie cookie) {
            return (cookie == null) ? null : escape(cookie.getValue());
        }
    }

    static class HeaderParameter extends BaseParameter {
        private String headerName;

        HeaderParameter(String headerName, boolean isRequest) {
            super(headerName, isRequest);
            this.headerName = headerName;
        }

        protected String getValue(RequestLoggerRequest request) {
            Enumeration<?> values = request.getHeaders(this.headerName);
            if (values == null || !values.hasMoreElements()) {
                return null;
            }

            String value = (String) values.nextElement();
            while (values.hasMoreElements()) {
                value += "," + values.nextElement();
            }
            return escape(value);
        }

        protected String getValue(RequestLoggerResponse response) {
            return escape(response.getHeadersString(this.headerName));
        }
    }
}

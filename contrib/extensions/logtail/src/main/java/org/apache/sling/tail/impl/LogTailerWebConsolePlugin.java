package org.apache.sling.tail.impl;

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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.tail.LogFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.*;

/**
 *
 */
@Component
@Service(value = { Servlet.class })
@Properties({
        @Property(name=org.osgi.framework.Constants.SERVICE_DESCRIPTION,
                value="Apache Sling Web Console Plugin to tail log(s) of this Sling instance"),
        @Property(name= WebConsoleConstants.PLUGIN_LABEL, value=LogTailerWebConsolePlugin.LABEL),
        @Property(name=WebConsoleConstants.PLUGIN_TITLE, value=LogTailerWebConsolePlugin.TITLE),
        @Property(name="felix.webconsole.configprinter.modes", value={"always"})
})
public class LogTailerWebConsolePlugin extends AbstractWebConsolePlugin {
    public static final String LABEL = "tail";
    public static final String TITLE = "Tail Logs";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final int LINES_TO_TAIL = 100;
    private static final String POSITION_COOKIE = "log.tail.position";
    private static final String FILTER_COOKIE = "log.tail.filter";
    private static final String MODIFIED_COOKIE = "log.modified";
    private static final String CREATED_COOKIE = "log.created";

    private String fileName = "";
    private File errLog;

    @Override
    protected void renderContent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if(isAjaxRequest(request)) {

            parseCommand(request, response);

            RandomAccessFile randomAccessFile = null;

            try {

                try {
                    randomAccessFile = new RandomAccessFile(errLog, "r");
                    log.debug("Tailing file " + fileName + " of length " + randomAccessFile.length());
                } catch (Exception e) {
                    log.error("Error reading " + fileName, e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }

                JSONWriter json = new JSONWriter(response.getWriter());
                json.setTidy(true);
                json.object();

                boolean reverse = false;
                //long created = getCreatedTimestampFromCookie(request);
            /*long modified = getModifiedTimestampFromCookie(request);
            if(errLog.lastModified() == modified) {
                json.endObject();
                return;
            }
            else {
                 persistCookie(response, MODIFIED_COOKIE, String.valueOf(errLog.lastModified()));
            }*/
                long pos = getPositionFromCookie(request);
                if(pos < 0) {
                    pos = randomAccessFile.length()-1;
                    reverse = true;
                }
                else if(pos > randomAccessFile.length()) {//file rotated
                    pos = 0;
                }
                LogFilter[] query = getQueryFromCookie(request);

                if(reverse) {
                    randomAccessFile.seek(pos);
                    if(randomAccessFile.read() == '\n') {
                        pos--;
                        randomAccessFile.seek(pos);
                        if(randomAccessFile.read() == '\r') {
                            pos--;
                        }
                    }

                    json.key("content").array();
                    int found = 0;
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    List<String> lines = new ArrayList<String>();
                    while(found != LINES_TO_TAIL && pos > 0) {
                        boolean eol = false;
                        randomAccessFile.seek(pos);
                        int c = randomAccessFile.read();
                        if(c == '\n') {
                            found++;
                            sb = sb.reverse();
                            line = sb.toString();
                            sb = new StringBuilder();
                            eol = true;
                            pos--;
                            if(pos > 0) {
                                randomAccessFile.seek(pos);
                                if(randomAccessFile.read() == '\r') {
                                    pos--;
                                }
                            }
                        }
                        else {
                            sb.append((char)c);
                            pos--;
                        }

                        if(eol) {
                            if(filter(line, query)){
                                lines.add(line);
                            }
                        }
                    }

                    if(pos < 0) {
                        if(filter(line, query)){
                            lines.add(line);
                        }
                    }
                    for(int i=lines.size()-1; i > -1; i--) {
                        json.object().key("line").value(lines.get(i)).endObject();
                    }
                    json.endArray();
                    json.endObject();
                }
                else {
                    randomAccessFile.seek(pos);
                    String line = null;
                    int lineCount = 0;
                    json.key("content").array();
                    boolean read = true;
                    while(read) {
                        StringBuilder input = new StringBuilder();
                        int c = -1;
                        boolean eol = false;

                        while (!eol) {
                            switch (c = randomAccessFile.read()) {
                                case -1:
                                case '\n':
                                    eol = true;
                                    break;
                                case '\r':
                                    eol = true;
                                    long cur = randomAccessFile.getFilePointer();
                                    if ((randomAccessFile.read()) != '\n') {
                                        randomAccessFile.seek(cur);
                                    }
                                    break;
                                default:
                                    input.append((char)c);
                                    break;
                            }
                        }

                        if ((c == -1) && (input.length() == 0)) {
                            read = false;
                            continue;
                        }
                        line = input.toString();
                        lineCount++;
                        if(lineCount == LINES_TO_TAIL) {
                            read = false;
                        }
                        pos = randomAccessFile.getFilePointer();

                        if(filter(line, query)){
                            json.object().key("line").value(line).endObject();
                        }
                    }
                    json.endArray();
                    json.endObject();
                }

                persistCookie(response, POSITION_COOKIE, String.valueOf(randomAccessFile.getFilePointer()));

            } catch (Exception e) {
                log.error("Error tailing " + fileName, e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            finally {
                try {
                    if(randomAccessFile != null) {
                        randomAccessFile.close();
                    }
                }
                catch (Exception e) {
                    log.error("Error closing " + fileName, e);
                }
            }
        }
        else {
            PrintWriter printWriter = response.getWriter();
            printWriter.println("<script type=\"text/javascript\" src=\"/libs/sling/logtail-plugin/js/tail.js\"></script>");
            printWriter.println("<link href=\"/libs/sling/logtail-plugin/css/tail.css\" rel=\"stylesheet\" type=\"text/css\"></link>");
            printWriter.println("<div class=\"header-cont\">");
            printWriter.println("   <div class=\"header\" style=\"display:none;\">");
            printWriter.println("       <table>");
            printWriter.println("           <tr>");
            printWriter.println("               <td><button class=\"numbering\" title=\"Show Line Numbers\" data-numbers=\"false\">Show Line No.</button></td>");
            printWriter.println("               <td><button class=\"pause\" title=\"Pause\">Pause</button></td>");
            printWriter.println("               <td class=\"longer\"><label>Sync frequency(msec)</label>");
            printWriter.println("                   <button class=\"faster\" title=\"Sync Faster\">-</button>");
            printWriter.println("                   <input id=\"speed\" type=\"text\" value=\"3000\"/>");
            printWriter.println("                   <button class=\"slower\" title=\"Sync Slower\">+</button></td>");
            printWriter.println("               <td><button class=\"tail\" title=\"Unfollow Tail\" data-following=\"true\">Unfollow</button></td>");
            printWriter.println("               <td><button class=\"highlighting\" title=\"Highlight\">Highlight</button></td>");
            printWriter.println("               <td><button class=\"clear\" title=\"Clear Display\">Clear</button></td>");
            printWriter.println("               <td class=\"longer\"><input id=\"filter\" type=\"text\"/><span class=\"filterClear ui-icon ui-icon-close\" title=\"Clear Filter\">&nbsp;</span><button class=\"filter\" title=\"Filter Logs\">Filter</button></td>");
            printWriter.println("               <td><button class=\"refresh\" title=\"Reload Logs\">Reload</button></td>");
            printWriter.println("               <td><button class=\"sizeplus\" title=\"Bigger\">a->A</button></td>");
            printWriter.println("               <td><button class=\"sizeminus\" title=\"Smaller\">A->a</button></td>");
            printWriter.println("               <td><button class=\"top\" title=\"Scroll to Top\">Top</button></td>");
            printWriter.println("               <td><button class=\"bottom\" title=\"Scroll to Bottom\">Bottom</button></td>");
            printWriter.println("           </tr>");
            printWriter.println("           <tr>");
            printWriter.println("               <td class=\"loadingstatus\" colspan=\"2\" data-status=\"inactive\"><ul><li></li></ul></td>");
            printWriter.println("               <td>Tailing &nbsp; <select id=\"logfiles\">" + getOptions() + "</select></td>");
            printWriter.println("           </tr>");
            printWriter.println("       </table>");
            printWriter.println("   </div>");
            printWriter.println("   <div class=\"pulldown\" title=\"Click to show options\">&nbsp;==&nbsp;</div>");
            printWriter.println("</div>");
            printWriter.println("");
            printWriter.println("   <div class=\"content\">");
            printWriter.println("");
            printWriter.println("       <div id=\"logarea\"></div>");
            printWriter.println("");
            printWriter.println("   </div>");
        }
    }

    protected void parseCommand(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String cmd = request.getParameter("command");
        if(cmd == null) {
            return;
        }

        if(cmd.equals("reset")) {
            deleteCookie(response, FILTER_COOKIE);
        }
        else if(cmd.startsWith("filter:")) {
            String queryStr = cmd.substring(7);
            if(queryStr.length()==0) {
                deleteCookie(response, FILTER_COOKIE);
            }
            else {
                persistCookie(response, FILTER_COOKIE, queryStr);
                log.info("Filtering on : " + queryStr);
            }
        }
        else if(cmd.startsWith("file:")) {
            if(!fileName.equals(cmd.substring(5))) {
                deleteCookie(response, FILTER_COOKIE);
                deleteCookie(response, POSITION_COOKIE);
                fileName = cmd.substring(5);
                errLog = new File(filePathMap.get(fileName));
                if(!errLog.exists()) {
                    throw new ServletException("File " + fileName + " doesn't exist");
                }
                if(!errLog.canRead()) {
                    throw new ServletException("Cannot read file " + fileName);
                }
            }
        }
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    private HashMap<String, String> filePathMap = new HashMap<String, String>();

    private String getKey(File file) {
        if(!filePathMap.containsKey(file.getName())) {
            filePathMap.put(file.getName(), file.getAbsolutePath());
        }
        return file.getName();
    }

    private String getOptions() {
        Set<String> logFiles = new HashSet<String>();
        LoggerContext context = (LoggerContext)LoggerFactory.getILoggerFactory();
        for (ch.qos.logback.classic.Logger logger : context.getLoggerList()) {
            for (Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders(); index.hasNext();) {
                Appender<ILoggingEvent> appender = index.next();
                if(appender instanceof FileAppender) {
                    FileAppender fileAppender = (FileAppender) appender;
                    String logfilePath = fileAppender.getFile();
                    logFiles.add(logfilePath);
                }
            }
        }

        String logFilesHtml = "<option value=\"\"> - Select file - </option>";
        for(String logFile : logFiles) {
            File file = new File(logFile);
            logFilesHtml += "<option value=\"" + getKey(file) + "\">" + file.getName() + "</option>";
        }
        return logFilesHtml;
    }

    @Override
    protected boolean isHtmlRequest( final HttpServletRequest request ) {
        return !isAjaxRequest(request);
    }

    private boolean isAjaxRequest( final HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    private boolean filter(String str, LogFilter[] query) {
        if(query != null) {
            for(LogFilter q : query) {
                if(!q.eval(str)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        log.debug("Deleting cookie :: " + cookie.getName());
    }

    private void persistCookie(HttpServletResponse response, String name, String value) {
        Cookie cookie = new Cookie(name , value);
        //cookie.setPath("/system/console/" + LABEL);
        response.addCookie(cookie);
        log.debug("Adding cookie :: " + cookie.getName() + " " + cookie.getValue());
    }

    private LogFilter[] getQueryFromCookie(HttpServletRequest request) {
        try {
            for(Cookie cookie : request.getCookies()) {
                if(cookie.getName().equals(FILTER_COOKIE)) {
                    String[] parts = cookie.getValue().split("&&");
                    LogFilter[] conditions = new LogFilter[parts.length];
                    for(int i=0; i<parts.length; i++) {
                        final String part = parts[i];
                        conditions[i] = new LogFilter() {
                            public boolean eval(String input) {
                                return input.contains(part);
                            }

                            public String toString() {
                                return part;
                            }
                        };
                    }
                    return conditions;
                }
            }
        }
        catch (Exception e) {

        }
        return null;
    }

    private long getCreatedTimestampFromCookie(HttpServletRequest request) {
        try {
            for(Cookie cookie : request.getCookies()) {
                if(cookie.getName().equals(CREATED_COOKIE)) {
                    return Long.parseLong(cookie.getValue());
                }
            }
        }
        catch (Exception e) {

        }
        return -1;
    }

    private long getModifiedTimestampFromCookie(HttpServletRequest request) {
        try {
            for(Cookie cookie : request.getCookies()) {
                if(cookie.getName().equals(MODIFIED_COOKIE)) {
                    return Long.parseLong(cookie.getValue());
                }
            }
        }
        catch (Exception e) {

        }
        return -1;
    }

    private long getPositionFromCookie(HttpServletRequest request) {
        try {
            for(Cookie cookie : request.getCookies()) {
                if(cookie.getName().equals(POSITION_COOKIE)) {
                    return Long.parseLong(cookie.getValue());
                }
            }
        }
        catch (Exception e) {
            log.debug("Position specified is invalid, Tailing from beginning of the file.", e);
        }
        return -1;
    }
}

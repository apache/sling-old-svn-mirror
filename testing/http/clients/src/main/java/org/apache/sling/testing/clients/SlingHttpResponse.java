/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.clients;

import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class SlingHttpResponse implements CloseableHttpResponse {

    public static final String STATUS = "Status";
    public static final String MESSAGE = "Message";
    public static final String LOCATION = "Location";
    public static final String PARENT_LOCATION = "ParentLocation";
    public static final String PATH = "Path";
    public static final String REFERER = "Referer";
    public static final String CHANGE_LOG = "ChangeLog";

    private final CloseableHttpResponse httpResponse;
    private String content;

    public SlingHttpResponse(CloseableHttpResponse response) {
        this.httpResponse = response;
    }

    /**
     * <p>Get the {@code String} content of the response.</p>
     * <p>The content is cached so it is safe to call this method several times.</p>
     * <p><b>Attention!</b> Calling this method consumes the entity, so it cannot be used as an InputStream later</p>
     *
     * @return the content as String
     */
    public String getContent() {
        if (!this.isConsumed()) {
            try {
                this.content = EntityUtils.toString(this.getEntity());
                this.close();
            } catch (IOException e) {
                throw new RuntimeException("Could not read content from response", e);
            }
        }

        return content;
    }

    public boolean isConsumed() {
        return this.content != null || this.getEntity() == null;
    }

    /**
     * <p>Assert that response matches supplied status</p>
     *
     * @param expected the expected http status
     * @throws AssertionError if the response does not match the expected
     */
    public void checkStatus(int expected) throws ClientException {
        if (this.getStatusLine().getStatusCode() != expected) {
            throw new ClientException(this + " has wrong response status ("
                    + this.getStatusLine().getStatusCode() + "). Expected " + expected);
        }
    }

    /**
     * <p>Assert that response matches supplied content type (from Content-Type header)</p>
     *
     * @param expected the expected content type
     * @throws AssertionError if the response content type does not match the expected
     */
    public void checkContentType(String expected) throws ClientException {
        // Remove whatever follows semicolon in content-type
        String contentType = this.getEntity().getContentType().getValue();
        if (contentType != null) {
            contentType = contentType.split(";")[0].trim();
        }

        // check for match
        if (!contentType.equals(expected)) {
            throw new ClientException(this + " has wrong content type (" + contentType + "). Expected " + expected);
        }
    }

    /**
     * <p>For each regular expression, assert that at least one line of the response matches the expression</p>
     * <p>The regular expressions are automatically prefixed and suffixed with .* it order to partial-match the lines</p>
     *
     * @param regexp list of regular expressions
     * @throws AssertionError if the response content does not match one of the regexp
     */
    public void checkContentRegexp(String... regexp) throws ClientException {
        for(String expr : regexp) {
            final Pattern p = Pattern.compile(".*" + expr + ".*");
            final Scanner scanner = new Scanner(this.getContent());
            boolean matched = false;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (p.matcher(line).matches()) {
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                throw new ClientException("Pattern " + p + " didn't match any line in content");
            }
        }
    }

    /**
     * <p>Assert that all the provided {@code Strings} are contained in the response</p>
     *
     * @param expected list of expected strings
     */
    public void checkContentContains(String... expected) throws ClientException {
        for (String s : expected) {
            if (!this.getContent().contains(s)) {
                throw new ClientException("Content does not contain string " + s + ". Content is: \n\n" + getContent());
            }
        }
    }

    /**
     * Get status from Sling Response
     *
     * @return Sling Status
     */
    public String getSlingStatus() {
        String searchPattern = "id=\"" + STATUS + "\">";
        return extractFromHTMLResponse(searchPattern);
    }

    /**
     * Get status from Sling Response as integer
     *
     * @return Sling Status
     */
    public int getSlingStatusAsInt() throws NumberFormatException {
        String strStatus = getSlingStatus();
        return Integer.parseInt(strStatus);
    }

    /**
     * Get message from Sling Response
     *
     * @return Sling Message
     */
    public String getSlingMessage() {
        String searchPattern = "id=\"" + MESSAGE + "\">";
        return extractFromHTMLResponse(searchPattern);
    }

    /**
     * Get copy paths from message
     *
     * @return copy paths as String Array
     */
    public String[] getSlingCopyPaths() {
        String copyPaths = getSlingMessage();
        StringTokenizer tokenizer = new StringTokenizer(copyPaths);
        List<String> copies = new ArrayList<String>();
        while (tokenizer.hasMoreElements()) {
            copies.add(tokenizer.nextToken());
        }
        return copies.toArray(new String[copies.size()]);
    }

    /**
     * Get location from Sling Response
     *
     * @return Sling Location
     */
    public String getSlingLocation() {
        String searchPattern = "id=\"" + LOCATION + "\">";
        return extractFromHTMLResponse(searchPattern);
    }

    /**
     * Get parent location from Sling Response
     *
     * @return Sling Parent Location
     */
    public String getSlingParentLocation() {
        String searchPattern = "id=\"" + PARENT_LOCATION + "\">";
        return extractFromHTMLResponse(searchPattern);
    }

    /**
     * Get path from Sling Response
     *
     * @return Sling Path
     */
    public String getSlingPath() {
        String searchPattern = "id=\"" + PATH + "\">";
        return extractFromHTMLResponse(searchPattern);
    }

    /**
     * Get referer from Sling Response
     *
     * @return Sling Referer
     */
    public String getSlingReferer() {
        String searchPattern = "id=\"" + REFERER + "\">";
        return extractFromHTMLResponse(searchPattern);
    }

    /**
     * Get change log from Sling Response
     *
     * @return Sling Change Log
     */
    public String getSlingChangeLog() {
        String searchPattern = "id=\"" + CHANGE_LOG + "\">";
        return extractFromHTMLResponse(searchPattern);
    }

    /**
     * Extract information from response
     *
     * @param searchPattern search pattern to look for
     * @return Sling information
     */
    protected String extractFromHTMLResponse(String searchPattern) {
        String tmpResponse = null;
        int start = getContent().indexOf(searchPattern);
        if (start > 0) {
            start += searchPattern.length();
            tmpResponse = getContent().substring(start);
            int end = tmpResponse.indexOf("<");
            tmpResponse = tmpResponse.substring(0, end);
        }
        return tmpResponse;
    }

    // HttpResponse delegated methods

    @Override
    public StatusLine getStatusLine() {
        return httpResponse.getStatusLine();
    }

    @Override
    public void setStatusLine(StatusLine statusline) {
        httpResponse.setStatusLine(statusline);
    }

    @Override
    public void setStatusLine(ProtocolVersion ver, int code) {
        httpResponse.setStatusLine(ver, code);
    }

    @Override
    public void setStatusLine(ProtocolVersion ver, int code, String reason) {
        httpResponse.setStatusLine(ver, code, reason);
    }

    @Override
    public void setStatusCode(int code) throws IllegalStateException {
        httpResponse.setStatusCode(code);
    }

    @Override
    public void setReasonPhrase(String reason) throws IllegalStateException {
        httpResponse.setReasonPhrase(reason);
    }

    @Override
    public HttpEntity getEntity() {
        return httpResponse.getEntity();
    }

    @Override
    public void setEntity(HttpEntity entity) {
        httpResponse.setEntity(entity);
    }

    @Override
    public Locale getLocale() {
        return httpResponse.getLocale();
    }

    @Override
    public void setLocale(Locale loc) {
        httpResponse.setLocale(loc);
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return httpResponse.getProtocolVersion();
    }

    @Override
    public boolean containsHeader(String name) {
        return httpResponse.containsHeader(name);
    }

    @Override
    public Header[] getHeaders(String name) {
        return httpResponse.getHeaders(name);
    }

    @Override
    public Header getFirstHeader(String name) {
        return httpResponse.getFirstHeader(name);
    }

    @Override
    public Header getLastHeader(String name) {
        return httpResponse.getLastHeader(name);
    }

    @Override
    public Header[] getAllHeaders() {
        return httpResponse.getAllHeaders();
    }

    @Override
    public void addHeader(Header header) {
        httpResponse.addHeader(header);
    }

    @Override
    public void addHeader(String name, String value) {
        httpResponse.addHeader(name, value);
    }

    @Override
    public void setHeader(Header header) {
        httpResponse.setHeader(header);
    }

    @Override
    public void setHeader(String name, String value) {
        httpResponse.setHeader(name, value);
    }

    @Override
    public void setHeaders(Header[] headers) {
        httpResponse.setHeaders(headers);
    }

    @Override
    public void removeHeader(Header header) {
        httpResponse.removeHeader(header);
    }

    @Override
    public void removeHeaders(String name) {
        httpResponse.removeHeaders(name);
    }

    @Override
    public HeaderIterator headerIterator() {
        return httpResponse.headerIterator();
    }

    @Override
    public HeaderIterator headerIterator(String name) {
        return httpResponse.headerIterator(name);
    }

    @SuppressWarnings("deprecation")
    @Override
    public HttpParams getParams() {
        return httpResponse.getParams();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setParams(HttpParams params) {
        httpResponse.setParams(params);
    }

    @Override
    public void close() throws IOException {
        httpResponse.close();
    }
}

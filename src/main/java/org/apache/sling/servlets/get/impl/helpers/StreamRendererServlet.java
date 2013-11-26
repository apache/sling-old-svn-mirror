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
package org.apache.sling.servlets.get.impl.helpers;

import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_IF_MODIFIED_SINCE;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_LAST_MODIFIED;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>StreamRendererServlet</code> streams the current resource to the
 * client on behalf of the
 * {@link org.apache.sling.servlets.get.impl.DefaultGetServlet}. If the current
 * resource cannot be streamed it is rendered using the
 * {@link PlainTextRendererServlet}.
 */
public class StreamRendererServlet extends SlingSafeMethodsServlet {

    public static final String EXT_RES = "res";

    private static final long serialVersionUID = -1L;

    /**
     * MIME multipart separation string
     */
    private static final String mimeSeparation = "SLING_MIME_BOUNDARY";

    /**
     * Full range marker.
     */
    private static ArrayList<Range> FULL = new ArrayList<Range>(0);

    static final int IO_BUFFER_SIZE = 2048;

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private boolean index;

    private String[] indexFiles;

    public StreamRendererServlet(boolean index, String[] indexFiles) {
        this.index = index;
        this.indexFiles = indexFiles;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {

        // whether this servlet is called as of a request include
        final boolean included = request.getAttribute(SlingConstants.ATTR_REQUEST_SERVLET) != null;

        // ensure no extension or "res"
        String ext = request.getRequestPathInfo().getExtension();
        if (ext != null && !ext.equals(EXT_RES)) {
            request.getRequestProgressTracker().log(
                "StreamRendererServlet does not support for extension " + ext);
            if (included || response.isCommitted()) {
                log.error(
                    "StreamRendererServlet does not support extension {}",
                    ext);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }

        final Resource resource = request.getResource();
        if (ResourceUtil.isNonExistingResource(resource)) {
            throw new ResourceNotFoundException("No data to render.");
        }

        // trailing slash on url means directory listing
        if ("/".equals(request.getRequestPathInfo().getSuffix())) {
            renderDirectory(request, response, included);
            return;
        }

        // check the last modification time and If-Modified-Since header
        if (!included) {
            ResourceMetadata meta = resource.getResourceMetadata();
            long modifTime = meta.getModificationTime();
            if (unmodified(request, modifTime)) {
                response.setStatus(SC_NOT_MODIFIED);
                return;
            }
        }

        // fall back to plain text rendering if the resource has no stream
        InputStream stream = resource.adaptTo(InputStream.class);
        if (stream != null) {

            streamResource(resource, stream, included, request, response);

        } else {

            // the resource is the root, do not redirect, immediately index
            if (isRootResourceRequest(resource)) {

                renderDirectory(request, response, included);

            } else if (included || response.isCommitted() ) {

                // request is included or committed, not redirecting
                request.getRequestProgressTracker().log(
                    "StreamRendererServlet: Not redirecting with trailing slash, response is committed or request included");
                log.warn("StreamRendererServlet: Not redirecting with trailing slash, response is committed or request included");

            } else {

                // redirect to this with trailing slash to render the index
                String url = request.getResourceResolver().map(request,
                    resource.getPath())
                    + "/";
                response.sendRedirect(url);

            }
        }
    }

    private boolean isRootResourceRequest(Resource resource) {
        return ("/".equals(resource.getPath())) ||
            ("/".equals(resource.getResourceResolver().map(resource.getPath())));
    }

    /**
     * Returns <code>true</code> if the request has a
     * <code>If-Modified-Since</code> header whose date value is later than the
     * last modification time given as <code>modifTime</code>.
     *
     * @param request The <code>ComponentRequest</code> checked for the
     *            <code>If-Modified-Since</code> header.
     * @param modifTime The last modification time to compare the header to.
     * @return <code>true</code> if the <code>modifTime</code> is less than or
     *         equal to the time of the <code>If-Modified-Since</code> header.
     */
    private boolean unmodified(HttpServletRequest request, long modifTime) {
        if (modifTime > 0) {
            long modTime = modifTime / 1000; // seconds
            long ims = request.getDateHeader(HEADER_IF_MODIFIED_SINCE) / 1000;
            return modTime <= ims;
        }

        // we have no modification time value, assume modified
        return false;
    }

    private void streamResource(final Resource resource,
            final InputStream stream, final boolean included,
            final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) throws IOException {
        // finally stream the resource
        try {

            final ArrayList<Range> ranges;
            if (included) {

                // no range support on included requests
                ranges = FULL;

            } else {

                // parse optional ranges
                ranges = parseRange(request, response,
                    resource.getResourceMetadata());
                if (ranges == null) {
                    // there was something wrong, the parseRange has sent a
                    // response and we are done
                    return;
                }

                // set various response headers, unless the request is included
                setHeaders(resource, response);
            }

            ServletOutputStream out = response.getOutputStream();

            if (ranges == FULL) {

                // return full resource
                setContentLength(response,
                    resource.getResourceMetadata().getContentLength());
                byte[] buf = new byte[IO_BUFFER_SIZE];
                int rd;
                while ((rd = stream.read(buf)) >= 0) {
                    out.write(buf, 0, rd);
                }

            } else {

                // return ranges of the resource
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

                if (ranges.size() == 1) {

                    Range range = ranges.get(0);
                    response.addHeader("Content-Range", "bytes " + range.start
                        + "-" + range.end + "/" + range.length);
                    setContentLength(response, range.end - range.start + 1);

                    copy(stream, out, range);

                } else {

                    response.setContentType("multipart/byteranges; boundary="
                        + mimeSeparation);

                    copy(resource, out, ranges.iterator());
                }

            }

        } finally {
            closeSilently(stream);
        }
    }

    private void renderDirectory(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response, final boolean included)
            throws ServletException, IOException {

        // request is included or committed, not rendering index
        if (included || response.isCommitted()) {
            request.getRequestProgressTracker().log(
                "StreamRendererServlet: Not rendering index, response is committed or request included");
            log.warn("StreamRendererServlet: Not rendering index, response is committed or request included");
            return;
        }

        Resource resource = request.getResource();
        ResourceResolver resolver = request.getResourceResolver();

        // check for an index file
        for (String index : indexFiles) {
            Resource fileRes = resolver.getResource(resource, index);
            if (fileRes != null && !ResourceUtil.isSyntheticResource(fileRes)) {

                // include the index resource with no suffix and selectors !
                RequestDispatcherOptions rdo = new RequestDispatcherOptions();
                rdo.setReplaceSuffix("");
                rdo.setReplaceSelectors("");

                RequestDispatcher dispatcher;
                if (index.indexOf('.') < 0) {
                    String filePath = fileRes.getPath() + ".html";
                    dispatcher = request.getRequestDispatcher(filePath, rdo);
                } else {
                    dispatcher = request.getRequestDispatcher(fileRes, rdo);
                }

                setHeaders(fileRes, response);

                dispatcher.include(request, response);
                return;
            }
        }

        if (index) {
            renderIndex(resource, response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }

    }

    /**
     * @param resource
     * @param request
     * @param response
     */
    private void setHeaders(Resource resource,
            SlingHttpServletResponse response) {

        final ResourceMetadata meta = resource.getResourceMetadata();
        final long modifTime = meta.getModificationTime();
        if (modifTime > 0) {
            response.setDateHeader(HEADER_LAST_MODIFIED, modifTime);
        }

        final String defaultContentType = "application/octet-stream";
        String contentType = meta.getContentType();
        if (contentType == null || defaultContentType.equals(contentType)) {
            // if repository doesn't provide a content-type, or
            // provides the
            // default one,
            // try to do better using our servlet context
            final String ct = getServletContext().getMimeType(
                resource.getPath());
            if (ct != null) {
                contentType = ct;
            }
        }
        if (contentType != null) {
            response.setContentType(contentType);
        }

        String encoding = meta.getCharacterEncoding();
        if (encoding != null) {
            response.setCharacterEncoding(encoding);
        }
    }

    /**
     * Set the <code>Content-Length</code> header to the give value. If the
     * length is larger than <code>Integer.MAX_VALUE</code> it is converted to a
     * string and the <code>setHeader(String, String)</code> method is called
     * instead of the <code>setContentLength(int)</code> method.
     *
     * @param response The response on which to set the
     *            <code>Content-Length</code> header.
     * @param length The content length to be set. If this value is equal to or
     *            less than zero, the header is not set.
     */
    private void setContentLength(final HttpServletResponse response, final long length) {
        if (length > 0) {
            if (length < Integer.MAX_VALUE) {
                response.setContentLength((int) length);
            } else {
                response.setHeader("Content-Length", String.valueOf(length));
            }
        }
    }

    private void renderIndex(Resource resource,
            SlingHttpServletResponse response) throws IOException {

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        String path = resource.getPath();

        PrintWriter pw = response.getWriter();
        pw.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">");
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>Index of " + path + "</title>");
        pw.println("</head>");

        pw.println("<body>");
        pw.println("<h1>Index of " + path + "</h1>");

        pw.println("<pre>");
        pw.println("Name                               Last modified                   Size  Description");
        pw.println("<hr>");

        if (!"/".equals(path)) {
            pw.println("<a href='../'>../</a>                                                                 -     Parent");
        }

        // render the children
        Iterator<Resource> children = ResourceUtil.listChildren(resource);
        while (children.hasNext()) {
            renderChild(pw, children.next());
        }

        pw.println("</pre>");
        pw.println("</body>");
        pw.println("</html>");

    }

    private void renderChild(PrintWriter pw, Resource resource) {

        String name = ResourceUtil.getName(resource.getPath());

        InputStream ins = resource.adaptTo(InputStream.class);
        if (ins == null) {
            name += "/";
        } else {
            closeSilently(ins);
        }

        String displayName = name;
        String suffix;
        if (displayName.length() >= 32) {
            displayName = displayName.substring(0, 29) + "...";
            suffix = "";
        } else {
            suffix = "                                               ".substring(
                0, 32 - displayName.length());
        }
        pw.printf("<a href='%s'>%s</a>%s", name, displayName, suffix);

        ResourceMetadata meta = resource.getResourceMetadata();
        long lastModified = meta.getModificationTime();
        pw.print("    " + new Date(lastModified) + "    ");

        long length = meta.getContentLength();
        if (length > 0) {
            pw.print(length);
        } else {
            pw.print('-');
        }

        pw.println();
    }

    //---------- Range header support
    // The following code is copy-derived from the Tomcate DefaultServlet
    // http://svn.apache.org/viewvc/tomcat/trunk/java/org/apache/catalina/servlets/DefaultServlet.java?view=markup

    /**
     * Copies a number of ranges from the given resource to the output stream.
     * Copy the contents of the specified input stream to the specified output
     * stream, and ensure that both streams are closed before returning (even in
     * the face of an exception).
     *
     * @param resource The resource from which to send ranges
     * @param ostream The output stream to write to
     * @param ranges Iterator of the ranges the client wanted to retrieve
     * @param contentType Content type of the resource
     * @exception IOException if an input/output error occurs
     */
    private void copy(Resource resource, ServletOutputStream ostream,
            Iterator<Range> ranges) throws IOException {

        String contentType = resource.getResourceMetadata().getContentType();
        IOException exception = null;

        while ((exception == null) && (ranges.hasNext())) {

            InputStream resourceInputStream = resource.adaptTo(InputStream.class);
            InputStream istream = new BufferedInputStream(resourceInputStream,
                IO_BUFFER_SIZE);

            try {
                Range currentRange = ranges.next();

                // Writing MIME header.
                ostream.println();
                ostream.println("--" + mimeSeparation);
                if (contentType != null) {
                    ostream.println("Content-Type: " + contentType);
                }
                ostream.println("Content-Range: bytes " + currentRange.start + "-"
                    + currentRange.end + "/" + currentRange.length);
                ostream.println();

                // Copy content
                try {
                    copy(istream, ostream, currentRange);
                } catch(IOException e) {
                    exception = e;
                }
            } finally {
                closeSilently(istream);
            }

        }

        ostream.println();
        ostream.print("--" + mimeSeparation + "--");
        
        if(exception != null) {
            throw exception;
        }
    }

    /**
    * Copy the contents of the specified input stream to the specified
    * output stream.
    *
    * @param istream The input stream to read from
    * @param ostream The output stream to write to
    * @param range Range the client wanted to retrieve
    * @exception IOException if an input/output error occurs
    */
    private void copy(InputStream istream, OutputStream ostream,
            Range range) throws IOException {
        // HTTP Range 0-9 means "byte 9 included"
        final long endIndex = range.end + 1;
        log.debug("copy: Serving bytes: {}-{}", range.start, endIndex);
        staticCopyRange(istream, ostream, range.start, endIndex);
    }

    // static, package-private method to make unit testing easier
    static void staticCopyRange(InputStream istream,
            OutputStream ostream, long start, long end) throws IOException {
        long position = 0;
        byte buffer[] = new byte[IO_BUFFER_SIZE];

        while (position < start) {
            long skipped = istream.skip(start - position);
            if (skipped == 0) {
                // skip() may return zero if for whatever reason it wasn't
                // able to advance the stream. In such cases we need to
                // fall back to read() to force the skipping of bytes.
                int len = (int) Math.min(start - position, buffer.length);
                skipped = istream.read(buffer, 0, len);
                if (skipped == -1) {
                    throw new IOException("Failed to skip " + start
                            + " bytes; only skipped " + position + " bytes");
                }
            }
            position += skipped;
        }

        while (position < end) {
            int len = (int) Math.min(end - position, buffer.length);
            int read = istream.read(buffer, 0, len);
            if (read != -1) {
                position += read;
                ostream.write(buffer, 0, read);
            } else {
                break;
            }
        }
    }

    /**
     * Parse the range header.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @return ArrayList of ranges parsed from the Range header or {@link #FULL}
     *         if the full resource should be returned or <code>null</code> if
     *         an error occurred parsing the header and the request has been
     *         finished sending an error status.
     */
    private ArrayList<Range> parseRange(HttpServletRequest request,
            HttpServletResponse response, ResourceMetadata metadata)
            throws IOException {

        // Checking If-Range
        String headerValue = request.getHeader("If-Range");
        if (headerValue != null) {

            long headerValueTime = (-1L);
            try {
                headerValueTime = request.getDateHeader("If-Range");
            } catch (IllegalArgumentException e) {
                // Ignore
            }

            if (headerValueTime == (-1L)) {

                // If the ETag the client gave does not match the entity
                // etag, then the entire entity is returned.
                // Sling: no etag support yet, return full range
                return FULL;

            } else if (metadata.getModificationTime() > (headerValueTime + 1000)) {

                // If the timestamp of the entity the client got is older than
                // the last modification date of the entity, the entire entity
                // is returned.
                return FULL;

            }

        }

        long fileLength = metadata.getContentLength();
        if (fileLength == 0) {
            return FULL;
        }

        // Retrieving the range header (if any is specified)
        String rangeHeader = request.getHeader("Range");
        if (rangeHeader == null) {
            return FULL;
        }

        // bytes is the only range unit supported (and I don't see the point
        // of adding new ones).
        if (!rangeHeader.startsWith("bytes")) {
            failParseRange(response, fileLength, rangeHeader);
            return null;
        }

        rangeHeader = rangeHeader.substring(6);

        // Vector which will contain all the ranges which are successfully
        // parsed.
        ArrayList<Range> result = new ArrayList<Range>();
        StringTokenizer commaTokenizer = new StringTokenizer(rangeHeader, ",");

        // Parsing the range list
        while (commaTokenizer.hasMoreTokens()) {
            String rangeDefinition = commaTokenizer.nextToken().trim();

            Range currentRange = new Range();
            currentRange.length = fileLength;

            int dashPos = rangeDefinition.indexOf('-');

            if (dashPos == -1) {
                failParseRange(response, fileLength, rangeHeader);
                return null;
            }

            if (dashPos == 0) {

                try {
                    long offset = Long.parseLong(rangeDefinition);
                    currentRange.start = fileLength + offset;
                    currentRange.end = fileLength - 1;
                } catch (NumberFormatException e) {
                    failParseRange(response, fileLength, rangeHeader);
                    return null;
                }

            } else {

                try {
                    currentRange.start = Long.parseLong(rangeDefinition.substring(
                        0, dashPos));
                    if (dashPos < rangeDefinition.length() - 1)
                        currentRange.end = Long.parseLong(rangeDefinition.substring(
                            dashPos + 1, rangeDefinition.length()));
                    else
                        currentRange.end = fileLength - 1;
                } catch (NumberFormatException e) {
                    failParseRange(response, fileLength, rangeHeader);
                    return null;
                }

            }

            if (!currentRange.validate()) {
                failParseRange(response, fileLength, rangeHeader);
                return null;
            }

            result.add(currentRange);
        }

        return result;
    }

    /**
     * Sends a 416 error response to the client if the Range header is
     * not acceptable
     */
    private void failParseRange(final HttpServletResponse response,
            final long fileLength, final String rangeHeader) throws IOException {
        log.error("parseRange: Cannot support range {}; sending 416",
            rangeHeader);
        response.addHeader("Content-Range", "bytes */" + fileLength);
        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    private void closeSilently(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignore) {
            }
        }
    }

    // --------- Range Inner Class

    protected class Range {

        public long start;

        public long end;

        public long length;

        /**
         * Validate range.
         */
        public boolean validate() {
            if (end >= length) end = length - 1;
            return ((start >= 0) && (end >= 0) && (start <= end) && (length > 0));
        }

    }
}
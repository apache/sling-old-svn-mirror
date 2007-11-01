/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.microsling.slingservlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.HttpStatusCodeException;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.microsling.helpers.constants.HttpConstants;

/**
 * The <code>StreamServlet</code> handles requests for nodes which may just be
 * streamed out to the response. If the requested JCR Item is an
 * <em>nt:file</em> whose <em>jcr:content</em> child node is of type
 * <em>nt:resource</em>, the response content type, last modification time and
 * charcter encoding are set according to the resource node. In addition if
 * the <em>If-Modified-Since</em> header is set, the resource will only be
 * spooled if the last modification time is later than the header. Otherwise
 * a 304 (Not Modified) status code is sent.
 * <p>
 * If the requested item is not an <em>nt:file</em>/<em>nt:resource</em> tuple,
 * the item is just resolved by following the primary item trail according to
 * the algorithm
 * <pre>
 *     while (item.isNode) {
 *         item = ((Node) item).getPrimaryItem();
 *     }
 * </pre>
 * Until a property is found or the primary item is either not defined or not
 * existing in which case an exception is thrown and the request fails with
 * a 404 (Not Found) status.
 */
public class StreamServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException, IOException {

        if (!(request.getResource().getRawData() instanceof Node)) {
            throw new HttpStatusCodeException(HttpServletResponse.SC_NOT_FOUND,
                "Resource " + request.getResource().getURI()
                    + " must be a Node");
        }

        try {
            // otherwise handle nt:file/nt:resource specially
            Node node = (Node) request.getResource().getRawData();
            if (node.isNodeType("nt:file")) {
                Node content = node.getNode("jcr:content");
                if (content.isNodeType("nt:resource")) {

                    // check for if last modified
                    long ifModified = request.getDateHeader(HttpConstants.HEADER_IF_MODIFIED_SINCE);
                    long lastModified = getLastModified(content);
                    if (ifModified < 0 || lastModified > ifModified) {

                        String contentType = getMimeType(content);
                        if (contentType == null) {
                            contentType = request.getResponseContentType();
                        }

                        spool(response,
                            content.getProperty(JcrConstants.JCR_DATA),
                            contentType, getEncoding(content),
                            getLastModified(content));
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    }

                    return;
                }
            }

            // just spool, the property to which the item resolves through
            // the primary item trail
            // the item is a property, spool and forget
            spool(response, findDefaultProperty(node), null, null, -1);

        } catch (ItemNotFoundException infe) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);

        } catch (RepositoryException re) {
            throw new SlingException(re.getMessage(), re);
        }
    }

    /**
     * Spool the property value to the response setting the content type,
     * character set, last modification data and content length header
     */
    private void spool(HttpServletResponse response, Property prop,
            String mimeType, String encoding, long lastModified)
            throws RepositoryException, IOException {

        if (mimeType != null) {
            response.setContentType(mimeType);
        }

        if (encoding != null) {
            response.setCharacterEncoding(encoding);
        }

        if (lastModified > 0) {
            response.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, lastModified);
        }

        // only set the content length if the property is a binary
        if (prop.getType() == PropertyType.BINARY) {
            response.setContentLength((int) prop.getLength());
        }

        InputStream ins = prop.getStream();
        OutputStream out = null;
        try {
            ins = prop.getStream();
            out = response.getOutputStream();

            byte[] buf = new byte[2048];
            int num;
            while ((num = ins.read(buf)) >= 0) {
                out.write(buf, 0, num);
            }
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    /** Follow the primary item trail of item until a property is found */
    Property findDefaultProperty(Item item) throws ItemNotFoundException,
            RepositoryException {
        while (item.isNode()) {
            // will throw if there is none defined or existsing
            item = ((Node) item).getPrimaryItem();
        }
        return (Property) item;
    }

    /** return the jcr:lastModified property value or null if property is missing */
    private long getLastModified(Node resourceNode) throws RepositoryException {
        Property lastModifiedProp = getProperty(resourceNode,
            JcrConstants.JCR_LASTMODIFIED);
        return (lastModifiedProp != null) ? lastModifiedProp.getLong() : -1;
    }

    /** return the jcr:mimeType property value or null if property is missing */
    private String getMimeType(Node resourceNode) throws RepositoryException {
        Property mimeTypeProp = getProperty(resourceNode,
            JcrConstants.JCR_MIMETYPE);
        return (mimeTypeProp != null) ? mimeTypeProp.getString() : null;
    }

    /** return the jcr:encoding property value or null if property is missing */
    private String getEncoding(Node resourceNode) throws RepositoryException {
        Property encodingProp = getProperty(resourceNode,
            JcrConstants.JCR_ENCODING);
        return (encodingProp != null) ? encodingProp.getString() : null;
    }

    /** Return the named property or null if not existing or node is null */
    private Property getProperty(Node node, String relPath)
            throws RepositoryException {
        if (node != null && node.hasProperty(relPath)) {
            return node.getProperty(relPath);
        }

        return null;
    }
}

package org.apache.sling.apt.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.apt.parser.SlingAptParser;

/**
* Parses APT structured text files and renders them in HTML
*
* @scr.service
*  interface="javax.servlet.Servlet"
*
* @scr.component
*  immediate="true"
*  metatype="no"
*
* @scr.property
*  name="service.description"
*  value="Sling APT Servlet"
*
* @scr.property
*  name="service.vendor"
*  value="The Apache Software Foundation"
*
* Use this as the default GET servlet for apt requests
* @scr.property
*  name="sling.servlet.resourceTypes"
*  value="sling/servlet/default"
*
* TODO for now we have to use this weird extension, added after the
* full filename. We should add a sling.servlet.contentExtension parameter
* to the servlet selection mechanism, and use that to tell sling to map
* an html request to an apt file using this servlet.
* 
* @scr.property
*  name="sling.servlet.extensions"
*  value="aptml"
*/

public class SlingAptServlet extends SlingSafeMethodsServlet {
    
    /** @scr.reference */
    protected SlingAptParser parser;
    
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        final InputStream stream = request.getResource().adaptTo(InputStream.class);
        if(stream == null) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST, 
                "Resource does not adapt to an InputStream: " + request.getResource()
            );
        }
        
        // TODO which encoding to use for input??
        // Should find out from the JCR resource node
        final String encoding = "UTF-8";
        final Reader r = new InputStreamReader(stream, encoding);
        final Writer w = new StringWriter();
        try {
            parser.parse(r, w);
        } catch(Exception e) {
            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Exception while parsing APT content: " + e
            );
        }

        final byte [] bytes = w.toString().getBytes(encoding);
        response.setContentType("text/html");
        response.setCharacterEncoding(encoding);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }
}
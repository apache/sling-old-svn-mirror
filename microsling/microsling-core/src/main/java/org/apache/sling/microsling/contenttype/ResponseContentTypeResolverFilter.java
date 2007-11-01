package org.apache.sling.microsling.contenttype;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.microsling.request.helpers.AbstractFilter;

public class ResponseContentTypeResolverFilter extends AbstractFilter {

    // TODO: Is text/plain ok or should this rather be text/html ??
    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "text/plain";

    @Override
    protected void init() {
        // no further initialization
    }

    public void doFilter(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        request = new ContentTypedRequest((SlingHttpServletRequest) request);
        filterChain.doFilter(request, response);

    }

    private class ContentTypedRequest extends SlingHttpServletRequestWrapper {

        private String responseContentType;

        ContentTypedRequest(SlingHttpServletRequest request) {
            super(request);
        }

        @Override
        public String getResponseContentType() {
            if (responseContentType == null) {
                String file = "dummy."
                    + getSlingRequest().getRequestPathInfo().getExtension();
                final String contentType = getFilterConfig().getServletContext().getMimeType(
                    file);
                if (contentType != null) {
                    responseContentType = contentType;
                } else {
                    responseContentType = DEFAULT_RESPONSE_CONTENT_TYPE;
                }
            }

            return responseContentType;
        }

        @Override
        public Enumeration<String> getResponseContentTypes() {
            Collection<String> c = Collections.singleton(getResponseContentType());
            return Collections.enumeration(c);
        }
    }
}

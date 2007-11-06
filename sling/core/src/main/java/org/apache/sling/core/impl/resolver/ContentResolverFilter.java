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
package org.apache.sling.core.impl.resolver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.component.ComponentFilterChain;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.ComponentResponseWrapper;
import org.apache.sling.component.Content;
import org.apache.sling.content.ContentManager;
import org.apache.sling.content.jcr.JcrContentManagerFactory;
import org.apache.sling.core.content.SelectableContent;
import org.apache.sling.core.content.Selector;
import org.apache.sling.core.impl.RequestData;
import org.apache.sling.core.resolver.ContentResolver;
import org.apache.sling.core.resolver.ResolvedURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @scr.component immediate="true" label="%resolver.name"
 *      description="%resolver.description"
 * @scr.property name="service.description"
 *      value="Default ContentResolver implementation"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="filter.scope" value="request" private="true"
 * @scr.property name="filter.order" value="-500" type="Integer" private="true"
 * @scr.service
 */
public class ContentResolverFilter implements ComponentFilter, ContentResolver {

    /**
    * @scr.property value="true" type="Boolean"
    */
    public static final String MAPPER_ALLOW_DIRECT = "resolver.allowDirect";

    /**
     * The resolver.fake property has no default configuration. But the sling
     * maven plugin and the sling management console cannot handle empty
     * multivalue properties at the moment. So we just add a dummy direct
     * mapping.
     * @scr.property values.1="/-/"
     */
    public static final String MAPPER_FAKE = "resolver.fake";

    /**
     * @scr.property values.1="/-/" values.2="/content/-/"
     *               Cvalues.3="/apps/&times;/docroot/-/"
     *               Cvalues.4="/libs/&times;/docroot/-/"
     *               values.5="/system/docroot/-/"
     */
    public static final String MAPPER_MAPPING = "resolver.mapping";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(ContentResolverFilter.class);

    /**
     * Allow startup without the factory
     * @scr.reference cardinality="0..1" policy="dynamic"
     */
    private JcrContentManagerFactory contentManagerFactory;

    /** all mappings */
    private Mapping[] mappings;

    /** The fake urls */
    private BidiMap fakeURLs;

    /** <code>true</code>, if direct mappings from URI to handle are allowed */
    private boolean allowDirect = false;

    // ---------- AbstractCoreFilter

    public void init(ComponentContext context) {
    }

    public void doFilter(ComponentRequest request, ComponentResponse response,
            ComponentFilterChain filterChain) throws IOException,
            ComponentException {

        // fail early, if we have no ContentManagerFactory
        JcrContentManagerFactory cmf = contentManagerFactory;
        if (cmf == null) {
            log.error("Missing ContentManageFactory, cannot access data");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        RequestData requestData = RequestData.getRequestData(request);

        ContentManager cm = cmf.getContentManager(requestData.getSession());
        requestData.setContentManager(cm);

        // 1.6 URL Mapping / Content Resolution --> URL Mapper
        ResolvedURL resolvedURL = resolveURL(cm, requestData.getRequestURI());
        if (resolvedURL == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 1.7 Check content selection
        Content content = resolvedURL.getContent();
        if (content instanceof SelectableContent) {
            Selector sel = ((SelectableContent) content).getSelector();
            if (sel != null) {
                content = sel.select(request, content);

                if (content == null) {
                    log.error("Content slection yielded no content");
                    response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "No content selected");
                    return;
                }

                // replace content in the resolved URL with the selected content
                ResolvedURLImpl ru = new ResolvedURLImpl(resolvedURL);
                ru.setContent(content);
                resolvedURL = ru;
            }
        }

        // 2. Handle the request
        requestData.pushContent(resolvedURL);
        try {
            filterChain.doFilter(request, new MapperComponentResponse(response, request.getContextPath()));
        } finally {
            requestData.popContent();
        }

    }

    public void destroy() {
    }

    // ---------- SCR Integration ---------------------------------------

    protected void activate(org.osgi.service.component.ComponentContext context) {

        Dictionary<?, ?> properties = context.getProperties();

        BidiMap fakes = new TreeBidiMap();
        String[] fakeList = (String[]) properties.get(MAPPER_FAKE);
        for (int i=0; fakeList != null && i < fakeList.length; i++) {
            String[] parts = split(fakeList[i]);
            fakes.put(parts[0], parts[2]);
        }
        fakeURLs = fakes;

        List<Mapping> maps = new ArrayList<Mapping>();
        String[] mappingList = (String[]) properties.get(MAPPER_MAPPING);
        for (int i=0; mappingList != null && i < mappingList.length; i++) {
            maps.add(new Mapping(split(mappingList[i])));
        }
        Mapping[] tmp = maps.toArray(new Mapping[maps.size()]);

        // check whether direct mappings are allowed
        Boolean directProp = (Boolean) properties.get(MAPPER_ALLOW_DIRECT);
        allowDirect = (directProp != null) ? directProp.booleanValue() : true;
        if (allowDirect) {
            Mapping[] tmp2 = new Mapping[tmp.length + 1];
            tmp2[0] = new Mapping("", "", null);
            System.arraycopy(tmp, 0, tmp2, 1, tmp.length);
            mappings = tmp2;
        } else {
            mappings = tmp;
        }
    }

    // ---------- ContentResolver interface -----------------------------------

    /**
     * Creates a MappedURL object from the url.
     *
     * @param ticket The ticket to access the contentbus
     * @param url the url to map
     * @return The MappedURL object after the correct mapping of the URI or
     *         <code>null</code> if the request uri cannot be mapped to a page
     *         handle.
     *
     * @throws UnauthorizedException if access to the repository node is not
     *      allowed for the request.
     */
    public ResolvedURL resolveURL(ContentManager cm, String requestURI) {

        // decode the request URI (required as the servlet container does not
        try {
            requestURI = URLDecoder.decode(requestURI, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            log.error("Cannot decode request URI using UTF-8", uee);
        } catch (Exception e) {
            log.error("Failed to decode request URI " + requestURI, e);
        }

        // convert fake urls
        requestURI = checkFakeURL(requestURI);
        try {

            // translate url to a mapped url structure
            return transformURL(cm, requestURI);

        } catch (AccessControlException ace) {
            // rethrow AccessControlExceptions to be handled
            throw ace;

        } catch (JcrMappingException jme) {

            log.warn("Mapping Failure for " + requestURI, jme);

        } catch (Throwable t) {
            log.warn("Problem resolving " + requestURI, t);
        }

        return null;
    }

    /**
     * @see URLMapperService#handleToURL(String)
     */
    public String pathToURL(String handle) {
        return pathToURL(null, handle, null);
    }

    /**
     * @see URLMapperService#handleToURL(String, String, String)
     */
    public String pathToURL(String prefix, String handle, String suffix) {
        String href = null;

        // get first map
        for (int i = allowDirect ? 1 : 0; i < mappings.length; i++) {
            href = mappings[i].mapHandle(handle);
            if (href != null) {
                break;
            }
        }

        // if no mapping's to prefix matches the handle, use the handle itself
        if (href == null) {
            href = handle;
        }

        // check fake mappings
        String fake = (String) fakeURLs.getKey(href);
        if (fake != null) {
            href = fake;
        }

        // handle prefix and suffix
        if (prefix != null && !prefix.equals("") && !prefix.equals("/")) {
            href = prefix + href;
        }
        if (suffix != null) {
            href += suffix;
        }

        log.debug("MapHandle: {} + {} + {} -> {}", new Object[] { prefix,
            handle, suffix, href });
        return href;
    }

    // ---------- internal

    private String[] split(String map) {
        String[] res = new String[3]; // src, op, dst
        StringTokenizer st = new StringTokenizer(map, "-<>", true);

        for (int i=0; i < res.length; i++) {
            res[i] = st.hasMoreTokens() ?  st.nextToken() : "";
        }

        return res;
    }

    /**
     * Translates a URI from the request to a MappedURL with decomposed
     * ContentBus page handle, selectors, extension and suffix.
     *
     * @param url The URI path to resolve and decompose
     * @return The <code>Content</code> object to which the URL maps or
     *      <code>null</code> if no such mapping can be found
     */
    private ResolvedURL transformURL(ContentManager cm, String url) {

        for (int i = 0; i < mappings.length; i++) {
            // exchange the 'to'-portion with the 'from' portion and check
            String href = mappings[i].mapUri(url);
            if (href == null) {
                log.debug("Mapping {} cannot map {}", mappings[i], url);
                continue;
            }
            Content content = findItem(cm, href);
            if (content == null) {
                log.debug("Cannot resolve {} to an item", href);
                continue;
            }

            // the URL which lead to the content loaded
            ResolvedURLImpl ru = new ResolvedURLImpl(url, content);

            // selectors come after handle - where's the period ?
            int selectorStart = content.getPath().length() + 1;
            if (selectorStart > href.length()) {
                // neither selectors nor extension
                return ru;
            }

            // suffix is path elements (with slashes) after selectors
            int suffixPos = href.indexOf('/', selectorStart);
            if (suffixPos < 0) {
                ru.setSelectorsExtension(href.substring(selectorStart));
            } else {
                ru.setSelectorsExtension(href.substring(selectorStart,
                    suffixPos));
                ru.setSuffix(href.substring(suffixPos));
            }

            return ru;
        }

        log.error("Could not resolve URL {} to a Content object", url);
        return null;
    }

    // ---------- internal -----------------------------------------------------

    // TODO
    private Content findItem(ContentManager cm, String href) {

        // common case, test as is
        Content jcrObject = cm.load(href);
        if (jcrObject != null) {
            return jcrObject;
        }

        // next scan from back to beginning for dots to cut off
        for (int i = href.lastIndexOf('.'); i > 0; i = href.lastIndexOf('.',
            i - 1)) {
            String sub = href.substring(0, i);
            jcrObject = cm.load(sub);
            if (jcrObject != null) {
                return jcrObject;
            }
        }

        // got here, if there is nothing available
        log.debug("No Content found for {}", href);
        return null;
    }

    /**
     * checks for a fakeurl
     *
     * @return the new url, if matches, otherwise the old one
     */
    private String checkFakeURL(String url) {
        String fake = (String) fakeURLs.get(url);
        return (fake != null) ? fake : url;
    }

    private class MapperComponentResponse extends ComponentResponseWrapper {

        private String contextPath;

        public MapperComponentResponse(ComponentResponse response, String contextPath) {
            super(response);
            this.contextPath = contextPath;
        }

        public String encodeURL(String path) {
            return super.encodeURL(pathToURL(contextPath, path, null));
        }

        public String encodeRedirectURL(String path) {
            return super.encodeRedirectURL(pathToURL(contextPath, path, null));
        }
    }
}

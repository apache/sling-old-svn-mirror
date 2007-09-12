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
package org.apache.sling.core.impl.mapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.Session;
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
import org.apache.sling.core.mapper.URLMapper;
import org.apache.sling.core.mapper.URLTranslationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.krb5.internal.Ticket;


/**
 * @scr.component immediate="true" label="%mapper.name"
 *      description="%mapper.description"
 * @scr.property name="service.description"
 *      value="Default URLMapper implementation"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="filter.scope" value="request" private="true"
 * @scr.property name="filter.order" value="-500" type="Integer" private="true"
 * @scr.service interface="org.apache.sling.component.ComponentFilter"
 */
public class URLMapperFilter implements ComponentFilter, URLMapper {

    /**
    * @scr.property value="true" type="Boolean"
    */
    public static final String MAPPER_ALLOW_DIRECT = "mapper.allowDirect";

    /**
     * The mapper.fake property has no default configuration. But the sling
     * maven plugin and the sling management console cannot handle empty
     * multivalue properties at the moment. So we just add a dummy direct
     * mapping.
     * @scr.property values.1="/-/"
     */
    public static final String MAPPER_FAKE = "mapper.fake";

    /**
     * @scr.property values.1="/-/" values.2="/content/-/"
     *               Cvalues.3="/apps/&times;/docroot/-/"
     *               Cvalues.4="/libs/&times;/docroot/-/"
     *               values.5="/system/docroot/-/"
     */
    public static final String MAPPER_MAPPING = "mapper.mapping";

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(URLMapperFilter.class);

    /**
     * @scr.reference target=""
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

        RequestData requestData = RequestData.getRequestData(request);

        ContentManager cm = this.contentManagerFactory.getContentManager(requestData.getSession());
        requestData.setContentManager(cm);

        // 1.6 URL Mapping / Content Resolution --> URL Mapper
        Content content = this.getMappedURL(requestData);
        if (content == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 1.7 Check content selection
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
            }
        }

        // 2. Handle the request
        requestData.pushContent(content);
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

        Dictionary properties = context.getProperties();

        BidiMap fakes = new TreeBidiMap();
        String[] fakeList = (String[]) properties.get(MAPPER_FAKE);
        for (int i=0; fakeList != null && i < fakeList.length; i++) {
            String[] parts = this.split(fakeList[i]);
            fakes.put(parts[0], parts[2]);
        }
        this.fakeURLs = fakes;

        List maps = new ArrayList();
        String[] mappingList = (String[]) properties.get(MAPPER_MAPPING);
        for (int i=0; mappingList != null && i < mappingList.length; i++) {
            maps.add(new Mapping(this.split(mappingList[i])));
        }
        Mapping[] tmp = (Mapping[]) maps.toArray(new Mapping[maps.size()]);

        // check whether direct mappings are allowed
        Boolean directProp = (Boolean) properties.get(MAPPER_ALLOW_DIRECT);
        this.allowDirect = (directProp != null) ? directProp.booleanValue() : true;
        if (this.allowDirect) {
            Mapping[] tmp2 = new Mapping[tmp.length + 1];
            tmp2[0] = new Mapping("", "", null);
            System.arraycopy(tmp, 0, tmp2, 1, tmp.length);
            this.mappings = tmp2;
        } else {
            this.mappings = tmp;
        }
    }

    protected void bindContentManagerFactory(
            JcrContentManagerFactory contentManagerFactory) {
        this.contentManagerFactory = contentManagerFactory;
    }

    protected void unbindContentManagerFactory(
            JcrContentManagerFactory contentManagerFactory) {
        this.contentManagerFactory = null;
    }

    // ---------- URLMapperService interface -----------------------------------

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
    public Content getMappedURL(RequestData requestData) {

        // decode the request URI (required as the servlet container does not
        String requestURI = requestData.getRequestURI();
        try {
            requestURI = URLDecoder.decode(requestURI, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            log.error("Cannot decode request URI using UTF-8", uee);
        } catch (Exception e) {
            log.error("Failed to decode request URI " + requestURI, e);
        }

        // convert fake urls
        requestURI = this.checkFakeURL(requestURI);
        try {

            // translate url to a mapped url structure
            return this.transformURL(requestData, requestURI);

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
    public String handleToURL(String handle) {
        return this.handleToURL(null, handle, null);
    }

    /**
     * @see URLMapperService#handleToURL(String, String, String)
     */
    public String handleToURL(String prefix, String handle, String suffix) {
        String href = null;

        // get first map
        for (int i = this.allowDirect ? 1 : 0; i < this.mappings.length; i++) {
            href = this.mappings[i].mapHandle(handle);
            if (href != null) {
                break;
            }
        }

        // if no mapping's to prefix matches the handle, use the handle itself
        if (href == null) {
            href = handle;
        }

        // check fake mappings
        String fake = (String) this.fakeURLs.getKey(href);
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
    private Content transformURL(RequestData requestData, String url) {

        ContentManager pm = requestData.getContentManager();
        for (int i = 0; i < this.mappings.length; i++) {
            // exchange the 'to'-portion with the 'from' portion and check
            String href = this.mappings[i].mapUri(url);
            if (href == null) {
                log.debug("Mapping {} cannot map {}", this.mappings[i], url);
                continue;
            }
            Content content = this.findItem(pm, href);
            if (content == null) {
                log.debug("Cannot resolve {} to an item", href);
                continue;
            }

            // the URL which lead to the content loaded
            requestData.setOriginalURL(url);

            // selectors come after handle - where's the period ?
            int selectorStart = content.getPath().length() + 1;
            if (selectorStart > href.length()) {
                // neither selectors nor extension
                return content;
            }

            // suffix is path elements (with slashes) after selectors
            int suffixPos = href.indexOf('/', selectorStart);
            if (suffixPos < 0) {
                requestData.setSelectorsExtension(href.substring(selectorStart));
            } else {
                requestData.setSelectorsExtension(href.substring(selectorStart,
                    suffixPos));
                requestData.setSuffix(href.substring(suffixPos));
            }

            return content;
        }

        log.error("Could not resolve URL {} to a Content object", url);
        return null;
    }

    // ---------- internal -----------------------------------------------------

    // TODO
    private Content findItem(ContentManager pm, String href) {

        // common case, test as is
        Content jcrObject = pm.load(href);
        if (jcrObject != null) {
            return jcrObject;
        }

        // next scan from back to beginning for dots to cut off
        for (int i = href.lastIndexOf('.'); i > 0; i = href.lastIndexOf('.',
            i - 1)) {
            String sub = href.substring(0, i);
            jcrObject = pm.load(sub);
            if (jcrObject != null) {
                return jcrObject;
            }
        }

        // got here, if there is nothing available
        log.debug("No Content found for {}", href);
        return null;
    }

    // private HandlePatternList getHandlePatterns(URLMapperConfig[]
    // mapperConfig) {
    // // extract the from strings to build the handle pattern list
    // String[] fromStrings = new String[mapperConfig.length];
    // for (int i=0; i < mapperConfig.length; i++) {
    // fromStrings[i] = mapperConfig[i].from;
    // }
    //
    // // create the pattern list and register to get notified
    // return new HandlePatternList(fromStrings);
    // }

    // private Mapping[] buildMappingList(URLMapperConfig[] mapperConfig /*,
    // HandleExpander handleExpander */) {
    //
    // // prepare the mappings in this list
    // List mappings = new ArrayList(mapperConfig.length);
    //
    // // convert the mapper config to the list of mappers
    // for (int i=0; i < mapperConfig.length; i++) {
    // mapperConfig[i].addMappings(mappings, null /* handleExpander */);
    // }
    //
    // // return converted to array
    // return (Mapping[]) mappings.toArray(new Mapping[mappings.size()]);
    // }

    /**
     * checks for a fakeurl
     *
     * @return the new url, if matches, otherwise the old one
     */
    private String checkFakeURL(String url) {
        String fake = (String) this.fakeURLs.get(url);
        return (fake != null) ? fake : url;
    }

    /**
     * @see URLMapperService#externalizeHref(Ticket, String, String, String,
     *      String)
     */
    public URLTranslationInfo externalizeHref(Session session, String prefix,
            String href, String base, String suffix) {

        return null;

        // if (href == null || href.length() == 0) {
        // throw new IllegalArgumentException("href must not be empty");
        // }
        // if (prefix.equals("/")) {
        // prefix = "";
        // }
        //
        // String original = href;
        //
        // // cut off query from href
        // int pos = href.indexOf('?');
        // String query = "";
        // if (pos > 0) {
        // query = href.substring(pos);
        // href = href.substring(0, pos);
        // }
        //
        // // check for missing dot, assume ".html" (if not "/")
        // if (!href.equals("/") && href.lastIndexOf('.') <=
        // href.lastIndexOf('/')) {
        // log.warn("Extension missing in link: {}. Extending it.", original);
        // href = href += ".html";
        // }
        //
        // // prepare the href
        // UUID uuid = null;
        // pos = 0;
        //
        // if (href.charAt(0) == '[') {
        // // extract the UUID from the href
        // pos = href.indexOf(']');
        // if (pos < 0) {
        // log.warn("HREF scrambled (not found): {}", href);
        // pos = 0;
        // } else {
        // try {
        // uuid = new UUID(href.substring(1, pos));
        // } catch (Exception e) {
        // log.warn("UUID in href not valid: {}", e.getMessage());
        // }
        // }
        //
        // } else if (href.charAt(0) != '/') {
        // if (base == null) {
        // log.warn(
        // "Relative translation required but no base handle specified.
        // href={}",
        // href);
        // return new URLTranslationInfoImpl(original, href + query, null);
        // }
        // href = Text.fullPath(base, href);
        // }
        //
        // /*
        // * Remove the prefix (Servlet Context Path) from the href before
        // further
        // * checking just in case someone already added it before. Here we have
        // * to remove the "prefix" from the href, if the href starts with the
        // * prefix and a slash immediately follows the prefix in href or if the
        // * href equals the prefix. Before actually removing the prefix, we
        // have
        // * to make sure, it does not actually address a valid ContentBus
        // * hierarchy node (directly or via URL mapping). This second check
        // * ensures, that e.g. /author/author/something remains addressable.
        // Note
        // * that I assume that the secondary check may be somewhat expensive
        // * because I consider it an exception that the href actually starts
        // with
        // * the prefix.
        // */
        // if (prefix.length() > 0 && href.startsWith(prefix)) {
        // if (href.length() <= prefix.length()
        // || href.charAt(prefix.length()) == '/') {
        // if (!session.hasPage(prefix)
        // && getMappedURL(session, href) == null) {
        // // remove the prefix for the checking
        // href = href.substring(prefix.length());
        // }
        // }
        // }
        //
        // // now search the href
        // int endPos = 0;
        // try {
        // // resolve the handle to a hierarchy node
        // endPos = session.matchHandle(href, pos);
        // String matchedHandle = href.substring(pos, endPos);
        //
        // log.debug("handle matched: {}-{} = {}.", String.valueOf(pos),
        // String.valueOf(endPos), matchedHandle);
        //
        // HierarchyNode node = session.getNode(matchedHandle);
        //
        // if (node.hasContentNode()) {
        // // if the CSD is invalid, we cannot map
        // if (!session.getHierarchyMgr().hasCSDInfo(
        // node.getContentNode().getCSD())) {
        // return null;
        // }
        //
        // // here we know, that the handle exists
        // if (uuid != null) {
        // // check contentid
        // log.debug("Handle ''{}'' ok (no contentid validation).",
        // href);
        // if (uuid.equals(node.getContentNode().getUUID())) {
        // return new URLTranslationInfoImpl(original,
        // handleToURL(prefix, matchedHandle,
        // href.substring(endPos))
        // + query, node);
        // }
        //
        // } else {
        // // since we cannot check it. the handle should be ok
        // if (original.charAt(0) == '/' || original.charAt(0) == '[') {
        // return new URLTranslationInfoImpl(original,
        // handleToURL(prefix, matchedHandle,
        // href.substring(endPos))
        // + query, node);
        // } else {
        // // we respect the original href, if it was a relative
        // // link
        // return new URLTranslationInfoImpl(original, original,
        // node);
        // }
        // }
        // }
        // } catch (AccessControlException e) {
        // // matchHandle()
        // log.warn("trouble while externalizing href {}: {}", original,
        // e.getMessage());
        // return null;
        // } catch (NoSuchNodeException e) {
        // // getNode()
        // // drop out
        // } catch (ContentBusException e) {
        // // all contentbus calls
        // log.info("trouble while externalizing href {}: {}", original,
        // e.getMessage());
        // // drop out
        // } catch (MalformedURLException e) {
        // // matchHandle()
        // log.info("trouble while externalizing href {}: {}", original,
        // e.getMessage());
        // // drop out
        // }
        //
        // // what we know here :
        // // -- either the handle does not denote a node with content, valid
        // CSD
        // // and - if the UUID is contained in href - the content's UUID
        // // does not match the desired UUID
        // // -- or the handle is invalid
        //
        // // here we know, that provided handle is incorrect
        // if (uuid == null) {
        // // ok, href is not really a handle but might be an URL of
        // // some sort. try to get a MappedURL from it, if so take the
        // // handle as is without a node.
        // MappedURL url = getMappedURL(session, href);
        // if (url == null) {
        // log.info("Handle ''{}'' broken (no contentid).", href);
        // return null;
        // }
        // log.info("Non-ContentBus Link Specified: {} (mapped to {})",
        // original, url.getHandle());
        // try {
        // return new URLTranslationInfoImpl(original, prefix + href
        // + query, session.getNode(url.getHandle()));
        // } catch (NoSuchNodeException e) {
        // log.info("Handle ''{}'' broken (content gone).", href);
        // return null;
        // } catch (ContentBusException e) {
        // log.error("Error while accessing valid data?", e);
        // return null;
        // }
        // }
        //
        // // ok, href handle is invalid, but a UUID exists, get on of those
        // try {
        // // so get the (first) node for the UUID with a content node
        // HierarchyNodeIterator iter = session.getNodes(uuid);
        // HierarchyNode node = null;
        // while (iter.hasNext()) {
        // node = iter.nextNode();
        // // actually, we should respect current handle and respect best
        // // matching
        // if (node.hasContentNode()) break;
        // node = null;
        // }
        //
        // // not even the uuid gives content
        // if (node == null) {
        // // content if gone aswell
        // log.info("Handle ''{}'' broken (content gone).", href);
        // return null;
        // }
        //
        // // this is not very cool, but we have to guess the invalid, old
        // handle
        // if (endPos == 0) endPos = href.indexOf('.', pos);
        // if (endPos < pos) {
        // // kind of default
        // return new URLTranslationInfoImpl(original, handleToURL(prefix,
        // node.getHandle(), ".html")
        // + query, node);
        // } else {
        // return new URLTranslationInfoImpl(original, handleToURL(prefix,
        // node.getHandle(), href.substring(endPos))
        // + query, node);
        // }
        //
        // } catch (ContentBusException e) {
        // log.info("Handle ''{}'' broken (error: {}).", original, e);
        // return null;
        // }
    }

    private class MapperComponentResponse extends ComponentResponseWrapper {

        private String contextPath;

        public MapperComponentResponse(ComponentResponse response, String contextPath) {
            super(response);
            this.contextPath = contextPath;
        }

        public String encodeURL(String path) {
            return super.encodeURL(URLMapperFilter.this.handleToURL(this.contextPath, path, null));
        }

// No implemented yet, will be done when ComponentResponse extends HttpServletResponse
//        public String encodeRedirectURL(String path) {
//            return super.encodeRedirectURL(handleToURL(path));
//        }
    }

    /**
     * Implementation of <code>URLTranslationInfo</code>
     */
    private final static class URLTranslationInfoImpl implements
            URLTranslationInfo {

        /** the mapped href (or null if not valid) */
        public final String href;

        /** the original href */
        public final String original;

        /** the underlaying hierarchy node */
        private final Node node;

        public URLTranslationInfoImpl(String ori, String href, Node node) {
            this.original = ori;
            this.href = href;
            this.node = node;
        }

        public Node getNode() {
            return this.node;
        }

        public String getExternalHref() {
            return this.href;
        }

        public String getInternalHref() {
            return this.original;
        }
    }

    /**
     * The <code>URLMapperConfig</code> class is a simple data container to
     * internally store the original configuration found in the mapping
     * configuration element.
     */
    private static class URLMapperConfig {
        final String from;

        final String to;

        final String dir;

        boolean addSlash;

        String stringValue;

        URLMapperConfig(String from, String to, String dir) {
            this.from = from;
            this.to = to;
            this.dir = dir;
            this.addSlash = from != null && from.endsWith("/");
        }

        boolean isValid() {
            return this.from != null && this.to != null;
        }

        void addMappings(Collection mappings,
                Object /* HandleExpander */handleExpander) {

            // check validity of this entry
            if (!this.isValid()) {
                log.debug("addMappings: mapperConfig={} invalid", this.toString());
                return;
            }

            if (handleExpander == null || this.from.length() == 0) {

                log.debug("addMappings: Add unexpanded mapping from {} to"
                    + " {}, dir={}", new Object[] { this.from, this.to, this.dir });
                mappings.add(new Mapping(this.from, this.to, this.dir));

            } else {
                // check for wildcard, otherwise don't expand (bug #9465)
                String[] fromList;
                // TODO: Have to decide how to handle patterns -> maybe XPath ??
                // if (GlobPattern.containsWildcards(from)) {
                // // handle expand the from property
                // fromList = new String[0]; // FIXME:
                // handleExpander.expand(from);
                // } else {
                fromList = new String[] { this.from };
                this.addSlash = false;
                // }
                for (int i = 0; i < fromList.length; i++) {
                    // add slash lost during expansion
                    if (this.addSlash) fromList[i] += "/";
                    log.debug(
                        "addMappings: Add mapping from {} to {}, dir={}",
                        new Object[] { fromList[i], this.to, this.dir });
                    mappings.add(new Mapping(fromList[i], this.to, this.dir));
                }

            }
        }

        public String toString() {
            if (this.stringValue != null) {
                StringBuffer buf = new StringBuffer(super.toString());
                buf.append(" [from=").append(this.from);
                buf.append(", to=").append(this.to);
                buf.append(", dir=").append(this.dir).append("]");
                this.stringValue = buf.toString();
            }
            return this.stringValue;
        }
    }
}

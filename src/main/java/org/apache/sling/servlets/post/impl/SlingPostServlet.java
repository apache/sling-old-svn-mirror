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
package org.apache.sling.servlets.post.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.servlets.post.HtmlResponse;
import org.apache.sling.servlets.post.JSONResponse;
import org.apache.sling.servlets.post.NodeNameGenerator;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.PostResponseCreator;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.SlingPostOperation;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.apache.sling.servlets.post.impl.helper.DateParser;
import org.apache.sling.servlets.post.impl.helper.DefaultNodeNameGenerator;
import org.apache.sling.servlets.post.impl.helper.MediaRangeList;
import org.apache.sling.servlets.post.impl.operations.CheckinOperation;
import org.apache.sling.servlets.post.impl.operations.CheckoutOperation;
import org.apache.sling.servlets.post.impl.operations.CopyOperation;
import org.apache.sling.servlets.post.impl.operations.DeleteOperation;
import org.apache.sling.servlets.post.impl.operations.ImportOperation;
import org.apache.sling.servlets.post.impl.operations.ModifyOperation;
import org.apache.sling.servlets.post.impl.operations.MoveOperation;
import org.apache.sling.servlets.post.impl.operations.NopOperation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * POST servlet that implements the sling client library "protocol"
 */
@Component(immediate = true, specVersion = "1.1", metatype = true, label = "%servlet.post.name", description = "%servlet.post.description")
@Service(value = Servlet.class)
@org.apache.felix.scr.annotations.Properties({
    @Property(name = "service.description", value = "Sling Post Servlet"),
    @Property(name = "service.vendor", value = "The Apache Software Foundation"),
    @Property(name = "sling.servlet.prefix", intValue = -1, propertyPrivate = true),
    @Property(name = "sling.servlet.paths", value = "sling/servlet/default/POST", propertyPrivate = true) })
@References({
    @Reference(name = "postProcessor", referenceInterface = SlingPostProcessor.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "postOperation", referenceInterface = PostOperation.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "nodeNameGenerator", referenceInterface = NodeNameGenerator.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "postResponseCreator", referenceInterface = PostResponseCreator.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "contentImporter", referenceInterface = ContentImporter.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC) })
public class SlingPostServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1837674988291697074L;

    /**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property({ "EEE MMM dd yyyy HH:mm:ss 'GMT'Z", "ISO8601",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd",
        "dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy" })
    private static final String PROP_DATE_FORMAT = "servlet.post.dateFormats";

    @Property({ "title", "jcr:title", "name", "description",
        "jcr:description", "abstract", "text", "jcr:text" })
    private static final String PROP_NODE_NAME_HINT_PROPERTIES = "servlet.post.nodeNameHints";

    @Property(intValue = 20)
    private static final String PROP_NODE_NAME_MAX_LENGTH = "servlet.post.nodeNameMaxLength";

    private static final boolean DEFAULT_CHECKIN_ON_CREATE = false;

    @Property(boolValue = DEFAULT_CHECKIN_ON_CREATE)
    private static final String PROP_CHECKIN_ON_CREATE = "servlet.post.checkinNewVersionableNodes";

    private static final boolean DEFAULT_AUTO_CHECKOUT = false;

    @Property(boolValue = DEFAULT_AUTO_CHECKOUT)
    private static final String PROP_AUTO_CHECKOUT = "servlet.post.autoCheckout";

    private static final boolean DEFAULT_AUTO_CHECKIN = true;

    @Property(boolValue = DEFAULT_AUTO_CHECKIN)
    private static final String PROP_AUTO_CHECKIN = "servlet.post.autoCheckin";


    private static final String PARAM_CHECKIN_ON_CREATE = ":checkinNewVersionableNodes";

    private static final String PARAM_AUTO_CHECKOUT = ":autoCheckout";

    private static final String PARAM_AUTO_CHECKIN = ":autoCheckin";

    private static final String DEFAULT_IGNORED_PARAMETER_NAME_PATTERN = "j_.*";

    @Property(value = DEFAULT_IGNORED_PARAMETER_NAME_PATTERN)
    private static final String PROP_IGNORED_PARAMETER_NAME_PATTERN = "servlet.post.ignorePattern";

    private final ModifyOperation modifyOperation = new ModifyOperation();

    private ServiceRegistration[] internalOperations;

    /** Map of post operations. */
    private final Map<String, PostOperation> postOperations = new HashMap<String, PostOperation>();

    /** Sorted list of post processor holders. */
    private final List<PostProcessorHolder> postProcessors = new ArrayList<PostProcessorHolder>();

    /** Cached list of post processors, used during request processing. */
    private SlingPostProcessor[] cachedPostProcessors = new SlingPostProcessor[0];

    /** Sorted list of node name generator holders. */
    private final List<NodeNameGeneratorHolder> nodeNameGenerators = new ArrayList<NodeNameGeneratorHolder>();

    /** Cached list of node name generators used during request processing. */
    private NodeNameGenerator[] cachedNodeNameGenerators = new NodeNameGenerator[0];

    /** Sorted list of post response creator holders. */
    private final List<PostResponseCreatorHolder> postResponseCreators = new ArrayList<PostResponseCreatorHolder>();

    /** Cached array of post response creators used during request processing. */
    private PostResponseCreator[] cachedPostResponseCreators = new PostResponseCreator[0];

    private final ImportOperation importOperation = new ImportOperation();

    /**
     * The content importer reference.
     */
	private ContentImporter contentImporter;

    private VersioningConfiguration baseVersioningConfiguration;

    @Override
    protected void doPost(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) throws IOException {
        final VersioningConfiguration localVersioningConfig = createRequestVersioningConfiguration(request);

        request.setAttribute(VersioningConfiguration.class.getName(), localVersioningConfig);

        // prepare the response
        final PostResponse htmlResponse = createPostResponse(request);
        htmlResponse.setReferer(request.getHeader("referer"));

        final PostOperation operation = getSlingPostOperation(request);
        if (operation == null) {

            htmlResponse.setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Invalid operation specified for POST request");

        } else {
            request.getRequestProgressTracker().log(
                    "Calling PostOperation: {0}", operation.getClass().getName());
            final SlingPostProcessor[] processors = this.cachedPostProcessors;
            try {
                operation.run(request, htmlResponse, processors);
            } catch (ResourceNotFoundException rnfe) {
                htmlResponse.setStatus(HttpServletResponse.SC_NOT_FOUND,
                    rnfe.getMessage());
            } catch (final Exception exception) {
                log.warn("Exception while handling POST "
                    + request.getResource().getPath() + " with "
                    + operation.getClass().getName(), exception);
                htmlResponse.setError(exception);
            }

        }

        // check for redirect URL if processing succeeded
        if (htmlResponse.isSuccessful()) {
            if (redirectIfNeeded(request, htmlResponse, response)) {
                return;
            }
        }

        // create a html response and send if unsuccessful or no redirect
        htmlResponse.send(response, isSetStatus(request));
    }

    /**
     * Redirects the HttpServletResponse, if redirectURL is not empty
     * @param htmlResponse
     * @param request
     * @param redirectURL The computed redirect URL
     * @param response The HttpServletResponse to use for redirection
     * @return Whether a redirect was requested
     * @throws IOException
     */
    boolean redirectIfNeeded(final SlingHttpServletRequest request, final PostResponse htmlResponse, final SlingHttpServletResponse response)
            throws IOException {
        final String redirectURL = getRedirectUrl(request, htmlResponse);
        if (redirectURL != null) {
            final Matcher m = REDIRECT_WITH_SCHEME_PATTERN.matcher(redirectURL);
            final boolean hasScheme = m.matches();
            final String encodedURL;
            if (hasScheme && m.group(2).length() > 0) {
                encodedURL = m.group(1) + response.encodeRedirectURL(m.group(2));
            } else if (hasScheme) {
                encodedURL = redirectURL;
            } else {
                log.debug("Request path is [{}]", request.getPathInfo());
                encodedURL = response.encodeRedirectURL(redirectURL);
            }
            log.debug("redirecting to URL [{}] - encoded as [{}]", redirectURL, encodedURL);
            response.sendRedirect(encodedURL);
            return true;
        }
        return false;
    }
    private static final Pattern REDIRECT_WITH_SCHEME_PATTERN = Pattern.compile("^(https?://[^/]+)(.*)$");

    /**
     * Creates an instance of a PostResponse.
     * @param req The request being serviced
     * @return a {@link org.apache.sling.servlets.post.impl.helper.JSONResponse} if any of these conditions are true:
     * <ul>
     *   <li> the request has an <code>Accept</code> header of <code>application/json</code></li>
     *   <li>the request is a JSON POST request (see SLING-1172)</li>
     *   <li>the request has a request parameter <code>:accept=application/json</code></li>
     * </ul>
     * or a {@link org.apache.sling.api.servlets.PostResponse} otherwise
     */
    PostResponse createPostResponse(final SlingHttpServletRequest req) {
        for (final PostResponseCreator creator : cachedPostResponseCreators) {
            final PostResponse response = creator.createPostResponse(req);
            if (response != null) {
                return response;
            }
        }

        // Fall through to default behavior
        @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
        final MediaRangeList mediaRangeList = new MediaRangeList(req);
        if (JSONResponse.RESPONSE_CONTENT_TYPE.equals(mediaRangeList.prefer("text/html", JSONResponse.RESPONSE_CONTENT_TYPE))) {
            return new JSONResponse();
        } else {
            return new HtmlResponse();
        }
    }

    private PostOperation getSlingPostOperation(
            final SlingHttpServletRequest request) {
        final String operation = request.getParameter(SlingPostConstants.RP_OPERATION);
        if (operation == null || operation.length() == 0) {
            // standard create/modify operation;
            return modifyOperation;
        }

        // named operation, retrieve from map
        synchronized ( this.postOperations ) {
            return postOperations.get(operation);
        }
    }

    /**
     * compute redirect URL (SLING-126)
     *
     * @param ctx the post processor
     * @return the redirect location or <code>null</code>
     */
    protected String getRedirectUrl(final SlingHttpServletRequest request, final PostResponse ctx) {
        // redirect param has priority (but see below, magic star)
        String result = request.getParameter(SlingPostConstants.RP_REDIRECT_TO);
        if (result != null && ctx.getPath() != null) {
            log.debug("redirect requested as [{}] for path [{}]", result, ctx.getPath());

            // redirect to created/modified Resource
            final int star = result.indexOf('*');
            if (star >= 0) {
                final StringBuilder buf = new StringBuilder();

                // anything before the star
                if (star > 0) {
                    buf.append(result.substring(0, star));
                }

                // append the name of the manipulated node
                buf.append(ResourceUtil.getName(ctx.getPath()));

                // anything after the star
                if (star < result.length() - 1) {
                    buf.append(result.substring(star + 1));
                }

                // Prepend request path if it ends with create suffix and result isn't absolute
                final String requestPath = request.getPathInfo();
                if (requestPath.endsWith(SlingPostConstants.DEFAULT_CREATE_SUFFIX) && buf.charAt(0) != '/' &&
                        !REDIRECT_WITH_SCHEME_PATTERN.matcher(buf).matches()) {
                    buf.insert(0, requestPath);
                }

                // use the created path as the redirect result
                result = buf.toString();

            } else if (result.endsWith(SlingPostConstants.DEFAULT_CREATE_SUFFIX)) {
                // if the redirect has a trailing slash, append modified node
                // name
                result = result.concat(ResourceUtil.getName(ctx.getPath()));
            }

            log.debug("Will redirect to {}", result);
        }
        return result;
    }

    protected boolean isSetStatus(final SlingHttpServletRequest request) {
        final String statusParam = request.getParameter(SlingPostConstants.RP_STATUS);
        if (statusParam == null) {
            log.debug(
                "getStatusMode: Parameter {} not set, assuming standard status code",
                SlingPostConstants.RP_STATUS);
            return true;
        }

        if (SlingPostConstants.STATUS_VALUE_BROWSER.equals(statusParam)) {
            log.debug(
                "getStatusMode: Parameter {} asks for user-friendly status code",
                SlingPostConstants.RP_STATUS);
            return false;
        }

        if (SlingPostConstants.STATUS_VALUE_STANDARD.equals(statusParam)) {
            log.debug(
                "getStatusMode: Parameter {} asks for standard status code",
                SlingPostConstants.RP_STATUS);
            return true;
        }

        log.debug(
            "getStatusMode: Parameter {} set to unknown value {}, assuming standard status code",
            SlingPostConstants.RP_STATUS);
        return true;
    }

    // ---------- SCR Integration ----------------------------------------------

    @Activate
    protected void activate(final ComponentContext context,
            final Map<String, Object> configuration) {
        // configure now
        this.configure(configuration);

        // other predefined operations
        final ArrayList<ServiceRegistration> providedServices = new ArrayList<ServiceRegistration>();
        final BundleContext bundleContext = context.getBundleContext();
        providedServices.add(registerOperation(bundleContext,
            SlingPostConstants.OPERATION_MODIFY, modifyOperation));
        providedServices.add(registerOperation(bundleContext,
            SlingPostConstants.OPERATION_COPY, new CopyOperation()));
        providedServices.add(registerOperation(bundleContext,
            SlingPostConstants.OPERATION_MOVE, new MoveOperation()));
        providedServices.add(registerOperation(bundleContext,
            SlingPostConstants.OPERATION_DELETE, new DeleteOperation()));
        providedServices.add(registerOperation(bundleContext,
            SlingPostConstants.OPERATION_NOP, new NopOperation()));
        providedServices.add(registerOperation(bundleContext,
            SlingPostConstants.OPERATION_CHECKIN, new CheckinOperation()));
        providedServices.add(registerOperation(bundleContext,
            SlingPostConstants.OPERATION_CHECKOUT, new CheckoutOperation()));
        providedServices.add(registerOperation(bundleContext,
            SlingPostConstants.OPERATION_IMPORT, importOperation));

        internalOperations = providedServices.toArray(new ServiceRegistration[providedServices.size()]);
    }

    private ServiceRegistration registerOperation(final BundleContext context,
            final String opCode, final PostOperation operation) {
        final Properties properties = new Properties();
        properties.put(PostOperation.PROP_OPERATION_NAME, opCode);
        properties.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling POST Servlet Operation " + opCode);
        properties.put(Constants.SERVICE_VENDOR,
            context.getBundle().getHeaders().get(Constants.BUNDLE_VENDOR));
        return context.registerService(PostOperation.SERVICE_NAME, operation,
            properties);
    }

    @Override
    public void init() throws ServletException {
        modifyOperation.setServletContext(getServletContext());
    }

    @Modified
    private void configure(final Map<String, Object> configuration) {
        this.baseVersioningConfiguration = createBaseVersioningConfiguration(configuration);

        final DateParser dateParser = new DateParser();
        final String[] dateFormats = OsgiUtil.toStringArray(configuration.get(PROP_DATE_FORMAT));
        for (String dateFormat : dateFormats) {
            try {
                dateParser.register(dateFormat);
            } catch (Throwable t) {
                log.warn(
                    "configure: Ignoring DateParser format {} because it is invalid: {}",
                    dateFormat, t);
            }
        }

        final String[] nameHints = OsgiUtil.toStringArray(configuration.get(PROP_NODE_NAME_HINT_PROPERTIES));
        final int nameMax = (int) OsgiUtil.toLong(
            configuration.get(PROP_NODE_NAME_MAX_LENGTH), -1);
        final NodeNameGenerator nodeNameGenerator = new DefaultNodeNameGenerator(
            nameHints, nameMax);

        final String paramMatch = OsgiUtil.toString(
            configuration.get(PROP_IGNORED_PARAMETER_NAME_PATTERN),
            DEFAULT_IGNORED_PARAMETER_NAME_PATTERN);
        final Pattern paramMatchPattern = Pattern.compile(paramMatch);

        this.modifyOperation.setDateParser(dateParser);
        this.modifyOperation.setDefaultNodeNameGenerator(nodeNameGenerator);
        this.importOperation.setDefaultNodeNameGenerator(nodeNameGenerator);
        this.modifyOperation.setIgnoredParameterNamePattern(paramMatchPattern);
        this.importOperation.setIgnoredParameterNamePattern(paramMatchPattern);
    }

    @Override
    public void destroy() {
        modifyOperation.setServletContext(null);
    }

    @Deactivate
    protected void deactivate() {
        if (internalOperations != null) {
            for (final ServiceRegistration registration : internalOperations) {
                registration.unregister();
            }
            internalOperations = null;
        }
        modifyOperation.setExtraNodeNameGenerators(null);
        importOperation.setExtraNodeNameGenerators(null);
        importOperation.setContentImporter(null);
    }

    /**
     * Bind a new post operation
     */
    protected void bindPostOperation(final PostOperation operation, final Map<String, Object> properties) {
        final String operationName = (String) properties.get(SlingPostOperation.PROP_OPERATION_NAME);
        if ( operationName != null && operation != null ) {
            synchronized (this.postOperations) {
                this.postOperations.put(operationName, operation);
            }
        }
    }

    /**
     * Unbind a post operation
     */
    protected void unbindPostOperation(final PostOperation operation, final Map<String, Object> properties) {
        final String operationName = (String) properties.get(SlingPostOperation.PROP_OPERATION_NAME);
        if ( operationName != null ) {
            synchronized (this.postOperations) {
                this.postOperations.remove(operationName);
            }
        }
    }

    /**
     * Bind a new post processor
     */
    protected void bindPostProcessor(final SlingPostProcessor processor, final Map<String, Object> properties) {
        final PostProcessorHolder pph = new PostProcessorHolder();
        pph.processor = processor;
        pph.ranking = OsgiUtil.toInteger(properties.get(Constants.SERVICE_RANKING), 0);

        synchronized ( this.postProcessors ) {
            int index = 0;
            while ( index < this.postProcessors.size() &&
                    pph.ranking < this.postProcessors.get(index).ranking ) {
                index++;
            }
            if ( index == this.postProcessors.size() ) {
                this.postProcessors.add(pph);
            } else {
                this.postProcessors.add(index, pph);
            }
            this.updatePostProcessorCache();
        }
    }

    /**
     * Unbind a post processor
     */
    protected void unbindPostProcessor(final SlingPostProcessor processor, final Map<String, Object> properties) {
        synchronized ( this.postProcessors ) {
            final Iterator<PostProcessorHolder> i = this.postProcessors.iterator();
            while ( i.hasNext() ) {
                final PostProcessorHolder current = i.next();
                if ( current.processor == processor ) {
                    i.remove();
                }
            }
            this.updatePostProcessorCache();
        }
    }

    /**
     * Update the post processor cache
     * This method is called by sync'ed methods, no need to add additional syncing.
     */
    private void updatePostProcessorCache() {
        final SlingPostProcessor[] localCache = new SlingPostProcessor[this.postProcessors.size()];
        int index = 0;
        for(final PostProcessorHolder current : this.postProcessors) {
            localCache[index] = current.processor;
            index++;
        }
        this.cachedPostProcessors = localCache;
    }

    /**
     * Bind a new node name generator
     */
    protected void bindNodeNameGenerator(final NodeNameGenerator generator, final Map<String, Object> properties) {
        final NodeNameGeneratorHolder nngh = new NodeNameGeneratorHolder();
        nngh.generator = generator;
        nngh.ranking = OsgiUtil.toInteger(properties.get(Constants.SERVICE_RANKING), 0);

        synchronized ( this.nodeNameGenerators ) {
            int index = 0;
            while ( index < this.nodeNameGenerators.size() &&
                    nngh.ranking < this.nodeNameGenerators.get(index).ranking ) {
                index++;
            }
            if ( index == this.nodeNameGenerators.size() ) {
                this.nodeNameGenerators.add(nngh);
            } else {
                this.nodeNameGenerators.add(index, nngh);
            }
            this.updateNodeNameGeneratorCache();
        }
    }

    /**
     * Unbind a node name generator
     */
    protected void unbindNodeNameGenerator(final NodeNameGenerator generator, final Map<String, Object> properties) {
        synchronized ( this.nodeNameGenerators ) {
            final Iterator<NodeNameGeneratorHolder> i = this.nodeNameGenerators.iterator();
            while ( i.hasNext() ) {
                final NodeNameGeneratorHolder current = i.next();
                if ( current.generator == generator ) {
                    i.remove();
                }
            }
            this.updateNodeNameGeneratorCache();
        }
    }

    /**
     * Update the node name generator cache
     * This method is called by sync'ed methods, no need to add additional syncing.
     */
    private void updateNodeNameGeneratorCache() {
        final NodeNameGenerator[] localCache = new NodeNameGenerator[this.nodeNameGenerators.size()];
        int index = 0;
        for(final NodeNameGeneratorHolder current : this.nodeNameGenerators) {
            localCache[index] = current.generator;
            index++;
        }
        this.cachedNodeNameGenerators = localCache;
        this.modifyOperation.setExtraNodeNameGenerators(this.cachedNodeNameGenerators);
        this.importOperation.setExtraNodeNameGenerators(this.cachedNodeNameGenerators);
    }

    /**
     * Bind a new post response creator
     */
    protected void bindPostResponseCreator(final PostResponseCreator creator, final Map<String, Object> properties) {
        final PostResponseCreatorHolder nngh = new PostResponseCreatorHolder();
        nngh.creator = creator;
        nngh.ranking = OsgiUtil.toInteger(properties.get(Constants.SERVICE_RANKING), 0);

        synchronized ( this.postResponseCreators ) {
            int index = 0;
            while ( index < this.postResponseCreators.size() &&
                    nngh.ranking < this.postResponseCreators.get(index).ranking ) {
                index++;
            }
            if ( index == this.postResponseCreators.size() ) {
                this.postResponseCreators.add(nngh);
            } else {
                this.postResponseCreators.add(index, nngh);
            }
            this.updatePostResponseCreatorCache();
        }
    }

    /**
     * Unbind a post response creator
     */
    protected void unbindPostResponseCreator(final PostResponseCreator creator, final Map<String, Object> properties) {
        synchronized ( this.postResponseCreators ) {
            final Iterator<PostResponseCreatorHolder> i = this.postResponseCreators.iterator();
            while ( i.hasNext() ) {
                final PostResponseCreatorHolder current = i.next();
                if ( current.creator == creator ) {
                    i.remove();
                }
            }
            this.updatePostResponseCreatorCache();
        }
    }

    /**
     * Update the post response creator cache
     * This method is called by sync'ed methods, no need to add additional syncing.
     */
    private void updatePostResponseCreatorCache() {
        final PostResponseCreator[] localCache = new PostResponseCreator[this.postResponseCreators.size()];
        int index = 0;
        for(final PostResponseCreatorHolder current : this.postResponseCreators) {
            localCache[index] = current.creator;
            index++;
        }
        this.cachedPostResponseCreators = localCache;
    }

    protected void bindContentImporter(final ContentImporter importer) {
        this.contentImporter = importer;
        importOperation.setContentImporter(importer);
    }

    protected void unbindContentImporter(final ContentImporter importer) {
        if ( this.contentImporter == importer ) {
            this.contentImporter = null;
            importOperation.setContentImporter(null);
        }
    }

    private VersioningConfiguration createBaseVersioningConfiguration(Map<?, ?> props) {
        VersioningConfiguration cfg = new VersioningConfiguration();
        cfg.setCheckinOnNewVersionableNode(OsgiUtil.toBoolean(
                props.get(PROP_CHECKIN_ON_CREATE), DEFAULT_CHECKIN_ON_CREATE));
        cfg.setAutoCheckout(OsgiUtil.toBoolean(
                props.get(PROP_AUTO_CHECKOUT), DEFAULT_AUTO_CHECKOUT));
        cfg.setAutoCheckin(OsgiUtil.toBoolean(
                props.get(PROP_AUTO_CHECKIN), DEFAULT_AUTO_CHECKIN));
        return cfg;
    }

    private VersioningConfiguration createRequestVersioningConfiguration(SlingHttpServletRequest request) {
        VersioningConfiguration cfg = baseVersioningConfiguration.clone();

        String paramValue = request.getParameter(PARAM_CHECKIN_ON_CREATE);
        if (paramValue != null) {
            cfg.setCheckinOnNewVersionableNode(Boolean.parseBoolean(paramValue));
        }
        paramValue = request.getParameter(PARAM_AUTO_CHECKOUT);
        if (paramValue != null) {
            cfg.setAutoCheckout(Boolean.parseBoolean(paramValue));
        }
        paramValue = request.getParameter(PARAM_AUTO_CHECKIN);
        if (paramValue != null) {
            cfg.setAutoCheckin(Boolean.parseBoolean(paramValue));
        }
        return cfg;
    }

    private static final class PostProcessorHolder {
        public SlingPostProcessor processor;
        public int ranking;
    }

    private static final class NodeNameGeneratorHolder {
        public NodeNameGenerator generator;
        public int ranking;
    }

    private static final class PostResponseCreatorHolder {
        public PostResponseCreator creator;
        public int ranking;
    }
}

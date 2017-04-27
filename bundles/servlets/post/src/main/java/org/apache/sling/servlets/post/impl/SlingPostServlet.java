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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.servlets.post.HtmlResponse;
import org.apache.sling.servlets.post.JSONResponse;
import org.apache.sling.servlets.post.NodeNameGenerator;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.PostResponseCreator;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.apache.sling.servlets.post.impl.helper.DateParser;
import org.apache.sling.servlets.post.impl.helper.DefaultNodeNameGenerator;
import org.apache.sling.servlets.post.impl.helper.JCRSupport;
import org.apache.sling.servlets.post.impl.helper.MediaRangeList;
import org.apache.sling.servlets.post.impl.operations.CheckinOperation;
import org.apache.sling.servlets.post.impl.operations.CheckoutOperation;
import org.apache.sling.servlets.post.impl.operations.CopyOperation;
import org.apache.sling.servlets.post.impl.operations.DeleteOperation;
import org.apache.sling.servlets.post.impl.operations.ImportOperation;
import org.apache.sling.servlets.post.impl.operations.ModifyOperation;
import org.apache.sling.servlets.post.impl.operations.MoveOperation;
import org.apache.sling.servlets.post.impl.operations.NopOperation;
import org.apache.sling.servlets.post.impl.operations.RestoreOperation;
import org.apache.sling.servlets.post.impl.operations.StreamedUploadOperation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * POST servlet that implements the sling client library "protocol"
 */
@Component(service = Servlet.class,
    property = {
            "service.description=Sling Post Servlet",
            "service.vendor=The Apache Software Foundation",
            "sling.servlet.prefix:Integer=-1",
            "sling.servlet.paths=sling/servlet/default/POST"
    })
@Designate(ocd = SlingPostServlet.Config.class)
public class SlingPostServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1837674988291697074L;

    @ObjectClassDefinition(name = "Apache Sling POST Servlet",
            description="The Sling POST Servlet is registered as the default " +
                        "servlet to handle POST requests in Sling.")
    public @interface Config {

        @AttributeDefinition(name = "Date Format",
                description = "List SimpleDateFormat strings for date "+
                     "formats supported for parsing from request input to data fields. The special "+
                     "format \"ISO8601\" (without the quotes) can be used to designate strict ISO-8601 "+
                     "parser which is able to parse strings generated by the Property.getString() "+
                     "method for Date properties. The default "+
                     "value is [ \"EEE MMM dd yyyy HH:mm:ss 'GMT'Z\", \"ISO8601\", "+
                     "\"yyyy-MM-dd'T'HH:mm:ss.SSSZ\", "+
                     "\"yyyy-MM-dd'T'HH:mm:ss\", \"yyyy-MM-dd\", \"dd.MM.yyyy HH:mm:ss\", \"dd.MM.yyyy\" ].")
        String[] servlet_post_dateFormats() default { "EEE MMM dd yyyy HH:mm:ss 'GMT'Z", "ISO8601",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd",
            "dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy" };

        @AttributeDefinition(name = "Node Name Hint Properties",
                    description = "The list of properties whose values "+
                     "may be used to derive a name for newly created nodes. When handling a request "+
                     "to create a new node, the name of the node is automatically generated if the "+
                     "request URL ends with a star (\"*\") or a slash (\"/\"). In this case the request "+
                     "parameters listed in this configuration value may be used to create the name. "+
                     "Default value is [ \"title\", \"jcr:title\", \"name\", \"description\", "+
                     "\"jcr:description\", \"abstract\", \"text\", \"jcr:text\" ].")
        String[] servlet_post_nodeNameHints() default { "title", "jcr:title", "name", "description",
            "jcr:description", "abstract", "text", "jcr:text" };

        @AttributeDefinition(name = "Maximum Node Name Length",
                    description = "Maximum number of characters to "+
                     "use for automatically generated node names. The default value is 20. Note, "+
                     "that actual node names may be generated with at most 4 more characters if the "+
                     "numeric suffixes must be appended to make the name unique.")
        int servlet_post_nodeNameMaxLength() default 20;

        @AttributeDefinition(name = "Checkin New Versionable Nodes",
                    description = "If true, newly created "+
                     "versionable nodes or non-versionable nodes which are made versionable by the "+
                     "addition of the mix:versionable mixin are checked in. By default, false.")
        boolean servlet_post_checkinNewVersionableNodes() default false;

        @AttributeDefinition(name = "Auto Checkout Nodes",
                    description = "If true, checked in nodes are "+
                    "checked out when necessary. By default, false.")
        boolean servlet_post_autoCheckout() default false;

        @AttributeDefinition(name = "Auto Checkin Nodes",
                    description = "If true, nodes which are checked out "+
                    "by the post servlet are checked in. By default, true.")
        boolean servlet_post_autoCheckin() default true;

        @AttributeDefinition(name = "Ignored Parameters",
                    description = "Configures a regular expression "+
                            "pattern to select request parameters which should be ignored when writing "+
                            "content to the repository. By default this is \"j_.*\" thus ignoring all "+
                            "request parameters starting with j_ such as j_username.")
        String servlet_post_ignorePattern() default "j_.*";
    }

    /**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String PARAM_CHECKIN_ON_CREATE = ":checkinNewVersionableNodes";

    private static final String PARAM_AUTO_CHECKOUT = ":autoCheckout";

    private static final String PARAM_AUTO_CHECKIN = ":autoCheckin";

    private final ModifyOperation modifyOperation = new ModifyOperation();

    private final StreamedUploadOperation streamedUploadOperation = new StreamedUploadOperation();

    private ServiceRegistration<PostOperation>[] internalOperations;

    /** Map of post operations. */
    private final Map<String, PostOperation> postOperations = new HashMap<>();

    /** Sorted list of post processor holders. */
    private final List<PostProcessorHolder> postProcessors = new ArrayList<>();

    /** Cached list of post processors, used during request processing. */
    private SlingPostProcessor[] cachedPostProcessors = new SlingPostProcessor[0];

    /** Sorted list of node name generator holders. */
    private final List<NodeNameGeneratorHolder> nodeNameGenerators = new ArrayList<>();

    /** Cached list of node name generators used during request processing. */
    private NodeNameGenerator[] cachedNodeNameGenerators = new NodeNameGenerator[0];

    /** Sorted list of post response creator holders. */
    private final List<PostResponseCreatorHolder> postResponseCreators = new ArrayList<>();

    /** Cached array of post response creators used during request processing. */
    private PostResponseCreator[] cachedPostResponseCreators = new PostResponseCreator[0];

    private final ImportOperation importOperation = new ImportOperation();

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
        final MediaRangeList mediaRangeList = new MediaRangeList(req);
        if (JSONResponse.RESPONSE_CONTENT_TYPE.equals(mediaRangeList.prefer("text/html", JSONResponse.RESPONSE_CONTENT_TYPE))) {
            return new JSONResponse();
        } else {
            return new HtmlResponse();
        }
    }

    private PostOperation getSlingPostOperation(
            final SlingHttpServletRequest request) {
        if (streamedUploadOperation.isRequestStreamed(request)) {
            return streamedUploadOperation;
        }
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
        if (result != null) {
            try {
                URI redirectUri = new URI(result);
                if (redirectUri.getAuthority() != null) {
                    // if it has a host information
                    log.warn("redirect target ({}) does include host information ({}). This is not allowed for security reasons!", result, redirectUri.getAuthority());
                    return null;
                }
            } catch (URISyntaxException e) {
                log.warn("given redirect target ({}) is not a valid uri: {}", result, e);
                return null;
            }

            log.debug("redirect requested as [{}] for path [{}]", result, ctx.getPath());

            // redirect to created/modified Resource
            final int star = result.indexOf('*');
            if (star >= 0 && ctx.getPath() != null) {
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

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(final BundleContext bundleContext,
            final Config configuration) {
        // configure now
        this.configure(configuration);

        // other predefined operations
        final ArrayList<ServiceRegistration<PostOperation>> providedServices = new ArrayList<>();
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

        // the following operations require JCR:
        if ( JCRSupport.INSTANCE.jcrEnabled()) {
            providedServices.add(registerOperation(bundleContext,
                SlingPostConstants.OPERATION_IMPORT, importOperation));
            providedServices.add(registerOperation(bundleContext,
                    SlingPostConstants.OPERATION_CHECKIN, new CheckinOperation()));
            providedServices.add(registerOperation(bundleContext,
                    SlingPostConstants.OPERATION_CHECKOUT, new CheckoutOperation()));
            providedServices.add(registerOperation(bundleContext,
                    SlingPostConstants.OPERATION_RESTORE, new RestoreOperation()));
        }
        internalOperations = providedServices.toArray(new ServiceRegistration[providedServices.size()]);
    }

    private ServiceRegistration<PostOperation> registerOperation(final BundleContext context,
            final String opCode, final PostOperation operation) {
        final Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(PostOperation.PROP_OPERATION_NAME, opCode);
        properties.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling POST Servlet Operation " + opCode);
        properties.put(Constants.SERVICE_VENDOR,
            context.getBundle().getHeaders().get(Constants.BUNDLE_VENDOR));
        return context.registerService(PostOperation.class, operation,
            properties);
    }

    @Override
    public void init() throws ServletException {
        modifyOperation.setServletContext(getServletContext());
        streamedUploadOperation.setServletContext(getServletContext());
    }

    @Modified
    private void configure(final Config configuration) {
        this.baseVersioningConfiguration = createBaseVersioningConfiguration(configuration);

        final DateParser dateParser = new DateParser();
        final String[] dateFormats = configuration.servlet_post_dateFormats();
        for (String dateFormat : dateFormats) {
            try {
                dateParser.register(dateFormat);
            } catch (Throwable t) {
                log.warn(
                    "configure: Ignoring DateParser format {} because it is invalid: {}",
                    dateFormat, t);
            }
        }

        final String[] nameHints = configuration.servlet_post_nodeNameHints();
        final int nameMax = configuration.servlet_post_nodeNameMaxLength();
        final NodeNameGenerator nodeNameGenerator = new DefaultNodeNameGenerator(
            nameHints, nameMax);

        final String paramMatch = configuration.servlet_post_ignorePattern();
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
        streamedUploadOperation.setServletContext(null);
    }

    @Deactivate
    protected void deactivate() {
        if (internalOperations != null) {
            for (final ServiceRegistration<PostOperation> registration : internalOperations) {
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
    @Reference(service = PostOperation.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected void bindPostOperation(final PostOperation operation, final Map<String, Object> properties) {
        final String operationName = (String) properties.get(PostOperation.PROP_OPERATION_NAME);
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
        final String operationName = (String) properties.get(PostOperation.PROP_OPERATION_NAME);
        if ( operationName != null ) {
            synchronized (this.postOperations) {
                this.postOperations.remove(operationName);
            }
        }
    }

    private int getRanking(final Map<String, Object> properties) {
        final Object val = properties.get(Constants.SERVICE_RANKING);
        return val instanceof Integer ? (Integer)val : 0;
    }

    /**
     * Bind a new post processor
     */
    @Reference(service = SlingPostProcessor.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected void bindPostProcessor(final SlingPostProcessor processor, final Map<String, Object> properties) {
        final PostProcessorHolder pph = new PostProcessorHolder();
        pph.processor = processor;
        pph.ranking = getRanking(properties);

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
    @Reference(service = NodeNameGenerator.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected void bindNodeNameGenerator(final NodeNameGenerator generator, final Map<String, Object> properties) {
        final NodeNameGeneratorHolder nngh = new NodeNameGeneratorHolder();
        nngh.generator = generator;
        nngh.ranking = getRanking(properties);

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
    @Reference(service = PostResponseCreator.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected void bindPostResponseCreator(final PostResponseCreator creator, final Map<String, Object> properties) {
        final PostResponseCreatorHolder nngh = new PostResponseCreatorHolder();
        nngh.creator = creator;
        nngh.ranking = getRanking(properties);

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

    @Reference(service = ContentImporter.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    protected void bindContentImporter(final Object importer) {
        importOperation.setContentImporter(importer);
    }

    protected void unbindContentImporter(final Object importer) {
        importOperation.unsetContentImporter(importer);
    }

    private VersioningConfiguration createBaseVersioningConfiguration(Config config) {
        VersioningConfiguration cfg = new VersioningConfiguration();
        cfg.setCheckinOnNewVersionableNode(config.servlet_post_checkinNewVersionableNodes());
        cfg.setAutoCheckout(config.servlet_post_autoCheckout());
        cfg.setAutoCheckin(config.servlet_post_autoCheckin());
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

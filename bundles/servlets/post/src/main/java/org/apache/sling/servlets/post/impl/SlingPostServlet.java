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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
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
import org.osgi.framework.ServiceReference;
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

    private ModifyOperation modifyOperation;

    private ServiceRegistration[] internalOperations;

    private final List<ServiceReference> delayedPostOperations = new ArrayList<ServiceReference>();

    private final Map<String, PostOperation> postOperations = new HashMap<String, PostOperation>();

    private final List<ServiceReference> delayedPostProcessors = new ArrayList<ServiceReference>();

    private final List<ServiceReference> postProcessors = new ArrayList<ServiceReference>();

    private SlingPostProcessor[] cachedPostProcessors = new SlingPostProcessor[0];

    private final List<ServiceReference> delayedNodeNameGenerators = new ArrayList<ServiceReference>();

    private final List<ServiceReference> nodeNameGenerators = new ArrayList<ServiceReference>();

    private NodeNameGenerator[] cachedNodeNameGenerators = new NodeNameGenerator[0];

    private final List<ServiceReference> delayedPostResponseCreators = new ArrayList<ServiceReference>();

    private final List<ServiceReference> postResponseCreators = new ArrayList<ServiceReference>();

    private PostResponseCreator[] cachedPostResponseCreators = new PostResponseCreator[0];

    private ComponentContext componentContext;

    private ImportOperation importOperation;

    /**
     * The content importer reference.
     */
	private ContentImporter contentImporter;

    private VersioningConfiguration baseVersioningConfiguration;

    @Override
    protected void doPost(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {
        VersioningConfiguration localVersioningConfig = createRequestVersioningConfiguration(request);

        request.setAttribute(VersioningConfiguration.class.getName(), localVersioningConfig);

        // prepare the response
        PostResponse htmlResponse = createPostResponse(request);
        htmlResponse.setReferer(request.getHeader("referer"));

        PostOperation operation = getSlingPostOperation(request);
        if (operation == null) {

            htmlResponse.setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Invalid operation specified for POST request");

        } else {

            final SlingPostProcessor[] processors;
            synchronized ( this.delayedPostProcessors ) {
                processors = this.cachedPostProcessors;
            }
            try {
                operation.run(request, htmlResponse, processors);
            } catch (ResourceNotFoundException rnfe) {
                htmlResponse.setStatus(HttpServletResponse.SC_NOT_FOUND,
                    rnfe.getMessage());
            } catch (Throwable throwable) {
                log.debug("Exception while handling POST "
                    + request.getResource().getPath() + " with "
                    + operation.getClass().getName(), throwable);
                htmlResponse.setError(throwable);
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
    boolean redirectIfNeeded(SlingHttpServletRequest request, PostResponse htmlResponse, SlingHttpServletResponse response)
            throws IOException {
        String redirectURL = getRedirectUrl(request, htmlResponse);
        if (redirectURL != null) {
            Matcher m = REDIRECT_WITH_SCHEME_PATTERN.matcher(redirectURL);
            boolean hasScheme = m.matches();
            String encodedURL;
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
    PostResponse createPostResponse(SlingHttpServletRequest req) {
        for (final PostResponseCreator creator : cachedPostResponseCreators) {
            PostResponse response = creator.createPostResponse(req);
            if (response != null) {
                return response;
            }
        }

        // Fall through to default behavior
        @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
        MediaRangeList mediaRangeList = new MediaRangeList(req);
        if (JSONResponse.RESPONSE_CONTENT_TYPE.equals(mediaRangeList.prefer("text/html", JSONResponse.RESPONSE_CONTENT_TYPE))) {
            return new JSONResponse();
        } else {
            return new HtmlResponse();
        }
    }

    private PostOperation getSlingPostOperation(
            SlingHttpServletRequest request) {
        String operation = request.getParameter(SlingPostConstants.RP_OPERATION);
        if (operation == null || operation.length() == 0) {
            // standard create/modify operation;
            return modifyOperation;
        }

        // named operation, retrieve from map
        return postOperations.get(operation);
    }

    /**
     * compute redirect URL (SLING-126)
     *
     * @param ctx the post processor
     * @return the redirect location or <code>null</code>
     */
    protected String getRedirectUrl(SlingHttpServletRequest request, PostResponse ctx) {
        // redirect param has priority (but see below, magic star)
        String result = request.getParameter(SlingPostConstants.RP_REDIRECT_TO);
        if (result != null && ctx.getPath() != null) {
            log.debug("redirect requested as [{}] for path [{}]", result, ctx.getPath());

            // redirect to created/modified Resource
            int star = result.indexOf('*');
            if (star >= 0) {
                StringBuffer buf = new StringBuffer();

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
                String requestPath = request.getPathInfo();
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

    protected boolean isSetStatus(SlingHttpServletRequest request) {
        String statusParam = request.getParameter(SlingPostConstants.RP_STATUS);
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

    protected void activate(final ComponentContext context,
            final Map<String, Object> configuration) {
        synchronized ( this.delayedPostProcessors ) {
            this.componentContext = context;
            for(final ServiceReference ref : this.delayedPostProcessors) {
                this.registerPostProcessor(ref);
            }
            this.delayedPostProcessors.clear();
        }
        synchronized ( this.delayedPostOperations ) {
            for(final ServiceReference ref : this.delayedPostOperations) {
                this.registerPostOperation(ref);
            }
            this.delayedPostOperations.clear();
        }

        // Dictionary<?, ?> props = context.getProperties();

        synchronized ( this.delayedNodeNameGenerators ) {
            for(final ServiceReference ref : this.delayedNodeNameGenerators) {
                this.registerNodeNameGenerator(ref);
            }
            this.delayedNodeNameGenerators.clear();
        }

        synchronized ( this.delayedPostResponseCreators ) {
            for(final ServiceReference ref : this.delayedPostResponseCreators) {
                this.registerPostResponseCreator(ref);
            }
            this.delayedPostResponseCreators.clear();
        }

        // default operation: create/modify
        modifyOperation = new ModifyOperation();
        modifyOperation.setExtraNodeNameGenerators(cachedNodeNameGenerators);

        importOperation = new ImportOperation(contentImporter);
        importOperation.setExtraNodeNameGenerators(cachedNodeNameGenerators);

        // configure now
        configure(configuration);

        // other predefined operations
        final ArrayList<ServiceRegistration> providedServices = new ArrayList<ServiceRegistration>();
        final BundleContext bundleContext = componentContext.getBundleContext();
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

    @Override
    public void init() throws ServletException {
        modifyOperation.setServletContext(getServletContext());
    }

    @Modified
    private void configure(Map<String, Object> configuration) {
        this.baseVersioningConfiguration = createBaseVersioningConfiguration(configuration);

        final DateParser dateParser = new DateParser();
        String[] dateFormats = OsgiUtil.toStringArray(configuration.get(PROP_DATE_FORMAT));
        for (String dateFormat : dateFormats) {
            try {
                dateParser.register(dateFormat);
            } catch (Throwable t) {
                log.warn(
                    "configure: Ignoring DateParser format {} because it is invalid: {}",
                    dateFormat, t);
            }
        }

        String[] nameHints = OsgiUtil.toStringArray(configuration.get(PROP_NODE_NAME_HINT_PROPERTIES));
        int nameMax = (int) OsgiUtil.toLong(
            configuration.get(PROP_NODE_NAME_MAX_LENGTH), -1);
        NodeNameGenerator nodeNameGenerator = new DefaultNodeNameGenerator(
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

    protected void deactivate(ComponentContext context) {
        if (internalOperations != null) {
            for (ServiceRegistration registration : internalOperations) {
                registration.unregister();
            }
            internalOperations = null;
        }
        modifyOperation = null;
        this.componentContext = null;
    }

    protected void bindPostOperation(ServiceReference ref) {
    	synchronized ( this.delayedPostOperations ) {
			if (this.componentContext == null) {
				this.delayedPostOperations.add(ref);
			} else {
				this.registerPostOperation(ref);
			}
		}
    }

    protected void registerPostOperation(ServiceReference ref) {
    	String operationName = (String) ref.getProperty(SlingPostOperation.PROP_OPERATION_NAME);
		PostOperation operation = (PostOperation) this.componentContext.locateService("postOperation", ref);
		if ( operation != null ) {
	        synchronized (this.postOperations) {
	            this.postOperations.put(operationName, operation);
	        }
		}
    }

    protected void unbindPostOperation(ServiceReference ref) {
    	synchronized ( this.delayedPostOperations ) {
        	String operationName = (String) ref.getProperty(SlingPostOperation.PROP_OPERATION_NAME);
        	synchronized (this.postOperations) {
        		this.postOperations.remove(operationName);
        	}
    	}
    }

    protected void bindPostProcessor(ServiceReference ref) {
        synchronized ( this.delayedPostProcessors ) {
            if ( this.componentContext == null ) {
                this.delayedPostProcessors.add(ref);
            } else {
                this.registerPostProcessor(ref);
            }
        }
    }

    protected void unbindPostProcessor(ServiceReference ref) {
        synchronized ( this.delayedPostProcessors ) {
            this.delayedPostProcessors.remove(ref);
            this.postProcessors.remove(ref);
        }
    }

    protected void registerPostProcessor(ServiceReference ref) {
        final int ranking = OsgiUtil.toInteger(ref.getProperty(Constants.SERVICE_RANKING), 0);
        int index = 0;
        while ( index < this.postProcessors.size() &&
                ranking < OsgiUtil.toInteger(this.postProcessors.get(index).getProperty(Constants.SERVICE_RANKING), 0)) {
            index++;
        }
        if ( index == this.postProcessors.size() ) {
            this.postProcessors.add(ref);
        } else {
            this.postProcessors.add(index, ref);
        }
        this.cachedPostProcessors = new SlingPostProcessor[this.postProcessors.size()];
        index = 0;
        for(final ServiceReference current : this.postProcessors) {
            final SlingPostProcessor processor = (SlingPostProcessor) this.componentContext.locateService("postProcessor", current);
            if ( processor != null ) {
                this.cachedPostProcessors[index] = processor;
                index++;
            }
        }
        if ( index < this.cachedPostProcessors.length ) {
            SlingPostProcessor[] oldArray = this.cachedPostProcessors;
            this.cachedPostProcessors = new SlingPostProcessor[index];
            for(int i=0;i<index;i++) {
                this.cachedPostProcessors[i] = oldArray[i];
            }
        }
    }

    private ServiceRegistration registerOperation(final BundleContext context,
            final String opCode, final PostOperation operation) {
        Properties properties = new Properties();
        properties.put(PostOperation.PROP_OPERATION_NAME, opCode);
        properties.put(Constants.SERVICE_DESCRIPTION,
            "Sling POST Servlet Operation " + opCode);
        properties.put(Constants.SERVICE_VENDOR,
            context.getBundle().getHeaders().get(Constants.BUNDLE_VENDOR));
        return context.registerService(PostOperation.SERVICE_NAME, operation,
            properties);
    }

    protected void bindNodeNameGenerator(ServiceReference ref) {
        synchronized ( this.delayedNodeNameGenerators ) {
            if ( this.componentContext == null ) {
                this.delayedNodeNameGenerators.add(ref);
            } else {
                this.registerNodeNameGenerator(ref);
            }
        }
    }

    protected void unbindNodeNameGenerator(ServiceReference ref) {
        synchronized ( this.delayedNodeNameGenerators ) {
            this.delayedNodeNameGenerators.remove(ref);
            this.nodeNameGenerators.remove(ref);
        }
    }

    protected void registerNodeNameGenerator(ServiceReference ref) {
        final int ranking = OsgiUtil.toInteger(ref.getProperty(Constants.SERVICE_RANKING), 0);
        int index = 0;
        while ( index < this.nodeNameGenerators.size() &&
                ranking < OsgiUtil.toInteger(this.nodeNameGenerators.get(index).getProperty(Constants.SERVICE_RANKING), 0)) {
            index++;
        }
        if ( index == this.nodeNameGenerators.size() ) {
            this.nodeNameGenerators.add(ref);
        } else {
            this.nodeNameGenerators.add(index, ref);
        }
        this.cachedNodeNameGenerators = new NodeNameGenerator[this.nodeNameGenerators.size()];
        index = 0;
        for(final ServiceReference current : this.nodeNameGenerators) {
            final NodeNameGenerator generator = (NodeNameGenerator) this.componentContext.locateService("nodeNameGenerator", current);
            if ( generator != null ) {
                this.cachedNodeNameGenerators[index] = generator;
                index++;
            }
        }
        if ( index < this.cachedNodeNameGenerators.length ) {
            NodeNameGenerator[] oldArray = this.cachedNodeNameGenerators;
            this.cachedNodeNameGenerators = new NodeNameGenerator[index];
            for(int i=0;i<index;i++) {
                this.cachedNodeNameGenerators[i] = oldArray[i];
            }
        }
        if(this.modifyOperation != null) {
            this.modifyOperation.setExtraNodeNameGenerators(this.cachedNodeNameGenerators);
        }
        if (this.importOperation != null) {
        	this.importOperation.setExtraNodeNameGenerators(this.cachedNodeNameGenerators);
        }
    }

    protected void bindPostResponseCreator(ServiceReference ref) {
        synchronized ( this.delayedPostResponseCreators ) {
            if ( this.componentContext == null ) {
                this.delayedPostResponseCreators.add(ref);
            } else {
                this.registerPostResponseCreator(ref);
            }
        }
    }

    protected void unbindPostResponseCreator(ServiceReference ref) {
        synchronized ( this.delayedPostResponseCreators ) {
            this.delayedPostResponseCreators.remove(ref);
            this.postResponseCreators.remove(ref);
        }
    }

    protected void registerPostResponseCreator(ServiceReference ref) {
        final int ranking = OsgiUtil.toInteger(ref.getProperty(Constants.SERVICE_RANKING), 0);
        int index = 0;
        while ( index < this.postResponseCreators.size() &&
            ranking < OsgiUtil.toInteger(this.postResponseCreators.get(index).getProperty(Constants.SERVICE_RANKING), 0)) {
            index++;
        }
        if ( index == this.postResponseCreators.size() ) {
            this.postResponseCreators.add(ref);
        } else {
            this.postResponseCreators.add(index, ref);
        }
        this.cachedPostResponseCreators = new PostResponseCreator[this.postResponseCreators.size()];
        index = 0;
        for(final ServiceReference current : this.postResponseCreators) {
            final PostResponseCreator creator = (PostResponseCreator) this.componentContext.locateService("postResponseCreator", current);
            if ( creator != null ) {
                this.cachedPostResponseCreators[index] = creator;
                index++;
            }
        }
        if ( index < this.cachedPostResponseCreators.length ) {
            PostResponseCreator[] oldArray = this.cachedPostResponseCreators;
            this.cachedPostResponseCreators = new PostResponseCreator[index];
            for(int i=0;i<index;i++) {
                this.cachedPostResponseCreators[i] = oldArray[i];
            }
        }
    }

    protected void bindContentImporter(ContentImporter importer) {
        this.contentImporter = importer;
        if (importOperation != null) {
            importOperation.setContentImporter(importer);
        }
    }

    protected void unbindContentImporter(ContentImporter importer) {
        this.contentImporter = null;
        if (importOperation != null) {
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
}

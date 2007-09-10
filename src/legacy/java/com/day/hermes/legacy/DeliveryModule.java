/**
 * $Id: DeliveryModule.java 22189 2006-09-07 11:47:26Z fmeschbe $
 *
 * Copyright 1997-2004 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package com.day.hermes.legacy;

import com.day.engine.*;
import com.day.hermes.DefaultHandlerData;
import com.day.hermes.auth.AuthenticationService;
import com.day.hermes.config.Config;
import com.day.hermes.contentbus.*;
import com.day.hermes.event.RequestEvent;
import com.day.hermes.event.RequestEventListener2;
import com.day.hermes.event.RequestEventListenerWrapper;
import com.day.hermes.logging.FmtLogger;
import com.day.hermes.mgt.Support;
import com.day.hermes.script.ScriptHandlerService;
import com.day.hermes.template.TemplateInfo;
import com.day.hermes.util.ACL;
import com.day.util.ListenerList;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @version $Revision: 1.39 $
 * @author fmeschbe
 * @since coati
 * @audience core
 */
public final class DeliveryModule extends AbstractModule implements StatusProviderSpi {

    /** default logger */
    FmtLogger log =
	(FmtLogger) FmtLogger.getLogger(DeliveryModule.class);

    /** request logger - used to write the req.log in cq2 times */
    FmtLogger requestLog =
	(FmtLogger) FmtLogger.getUnmappedLogger("delivery.req");

    /** access logger - used to write a stripped down access.log */
    FmtLogger accessLog =
	(FmtLogger) FmtLogger.getUnmappedLogger("delivery.access");

    /** date format  - see access logging in service() */
    private static final SimpleDateFormat fmt =
	    new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss ", Locale.US);

    /** time format for GMT offset - see access logging in service() */
    private static final DecimalFormat dfmt = new DecimalFormat("+0000;-0000");

    /** the timezone for the timezone offset calculation */
    private static final Calendar calendar = Calendar.getInstance();

    /** The element XPath for the requesthelper configuration */
    private static final String HELPER_ELEMENT = "/requesthelper";

    /** The element XPath for additional services configuration */
    private static final String SERVICES_ELEMENT = "/services";

    /** The element XPath for template path configuration */
    private static final String TEMPLATEPATH_ELEMENT = "/templatepath";

    /**
     * The name of the attribute defining the shutdown grace time
     * @see #shutdownGraceTime
     */
    private static final String ATTR_SHUTDOWN_GRACE_TIME = "shutdownGraceTime";

    /**
     * The default shut down grace time in seconds if configuration is missing.
     * @see #shutdownGraceTime
     */
    private static final long DEFAULT_SHUTDOWN_GRACE_TIME = 20;

    /** The counter for the number of requests handled by the service method */
    private static int requestCounter = 0;

    /** The singleton instance - assigned by the constructor */
    private static DeliveryModule instance;

    /**
     * The time in secnds the {@link #unload(com.day.engine.EngineContext)}
     * method waits for active requests to end before aborting them to continue
     * module unload.
     *
     * @see #unload(com.day.engine.EngineContext)
     * @see #DEFAULT_SHUTDOWN_GRACE_TIME
     * @see #ATTR_SHUTDOWN_GRACE_TIME
     */
    private long shutdownGraceTime;

    /** The cache handler */
    private CacheHandlerService cacheHandler;

    /** The authentication handler */
    private AuthenticationService authHandler;

    /** The URL mapper handler */
    private URLMapperService urlMapper;

    /** The link checker */
    private LinkCheckerService linkChecker;

    /** The CMS service */
    private CmsService cms;

    /** The script handler */
    private ScriptHandlerService scriptHandler;

    /** The servlet specification */
    private ServletSpec servletSpec;

    /** Context attachment/detachment listener registered to this debugger */
    private ListenerList listeners = new ListenerList();

    /** The webapp context - initialized after the first request */
    private String webappContext = null;

    /** Table of pending requests, indexed by their thread's name */
    private final Map pendingRequests = new HashMap();

    /**
     * Creates a new instance of the delivery module. This constructor can only
     * be called. Any subsequent construction throws an IllegalStateException.
     * <p>
     * Due to the fact, that the DeliveryModule is instantiated by the Engine
     * through the parameterless default constructor, it is not possible for
     * the DeliveyModule to implement the usual Singleton pattern using the
     * traditional static getInstance() method.
     *
     * @throws IllegalStateException if called more than once.
     */
    public DeliveryModule() {
	// Check singleton instance
	synchronized(DeliveryModule.class) {
	    if (instance != null) {
		throw new IllegalStateException("DeliveryModule is singleton");
	    }

	    DeliveryModule.instance = this;
	}
    }

    /**
     * Returns the instance of the delivery module. Package protected because
     * it should only be used by this package.
     *
     * @throws IllegalStateException if either no instance has yet been created
     * 		or if the instance is not loaded or currently un-loaded.
     */
    public static synchronized DeliveryModule getInstance() {

	// check instance and whether loaded
	if (instance == null) {
	    throw new IllegalStateException("No instance or module not loaded");
	}

	return instance;
    }

    //---------- Module interface ----------------------------------------------

    /**
     * Load a module. The module itself will initialize its own services.
     *
     * @param context the engine context
     * @exception ModuleException if an error occurs
     */
    public void doLoad(Ticket ticket, ClassLoader classloader,
                       EngineContext context)
            throws ModuleException {

	Config deliveryConfig = context.getConfig().getChild("/delivery");

	try {
            // configuration in seconds
            shutdownGraceTime =
                    deliveryConfig.getProperty(ATTR_SHUTDOWN_GRACE_TIME,
                            DEFAULT_SHUTDOWN_GRACE_TIME);

            // register request helper services
	    Config helperConfig = deliveryConfig.getChild(HELPER_ELEMENT);
            registerServices(ticket, classloader, helperConfig);

            // check request helper services
            checkRequestHelper();

            // setup request and response classes
            DeliveryHttpServletRequestImpl.setup(cms);
            DeliveryHttpServletResponseImpl.setup(urlMapper, cacheHandler,
                linkChecker, scriptHandler);

	    // register general services
	    Config servicesConfig = deliveryConfig.getChild(SERVICES_ELEMENT);
	    registerServices(ticket, classloader, servicesConfig);

	    // configure the TemplateInfo
	    TemplateInfo.init(deliveryConfig.getChild(TEMPLATEPATH_ELEMENT));

            // register as a status provider
            StatusProviderRegistry.registerStatusProviderSpi(getStatusProviderSpi());
	} catch (ServiceException e) {

	    throw new ModuleException(e);

	}
    }

    /**
     * Called to unload the delivery module. This first marks the module
     * "not-loaded". This way new requests will not be serviced anymore. The
     * next step is check whether there are any requests active. If so, the
     * unload procedure is delayed for a certain amount of time, after which
     * all request still not finished will be terminated. Next the base class
     * implementation is called, which continues the standard unload procedure.
     *
     * @param context the engine context
     */
    public void unload(EngineContext context) throws ModuleException {
        // mark unloaded
        loaded = false;

        // check to see whether to wait for requests
        int numPending = pendingRequests.size();
        if (numPending > 0) {

            log.warn("unload: Waiting {0} secs for {1} requests to terminate",
                    String.valueOf(shutdownGraceTime),
                    String.valueOf(numPending));

            // wait for the requests to terminate
            try {
                Thread.sleep(shutdownGraceTime * 1000L);
            } catch (InterruptedException ie) {
                log.debug("unload: Interrupted when waiting for requests" +
                        " to terminate, continuing");
            }

            // will terminate any remaining pending request now
            PendingRequest[] req = getPendingRequests();
            for (int i=0; i < req.length; i++) {
                log.warn("unload: Aborting request ''{0}''", req[i].getRequestId());
                req[i].abortRequest();
            }
        }

        // continue standard unload procedure
        super.unload(context);
    }

    /**
     * Unloads a module. The module itself can cleanup whatever it needs.
     * please note, that the services of the module are already stopped
     * and destroyed this time.
     * @param context the engine context
     */
    public void doUnload(Ticket ticket, EngineContext context) {

        // unregister as a status provider
        StatusProviderRegistry.unregisterStatusProviderSpi(getStatusProviderSpi());

        // clear fields
        cacheHandler = null;
        authHandler = null;
        urlMapper = null;
        linkChecker = null;
        cms = null;
        scriptHandler = null;

        // reset the request and response classes
        DeliveryHttpServletRequestImpl.setup(null);
        DeliveryHttpServletResponseImpl.setup(null, null, null, null);
    }

    //---------- more methods --------------------------------------------------

    /**
     * Handles the servlet request on behalf of the DeliveryServlet.
     * <p>
     * For the eyes of the {@link DeliveryServlet} only, therefor package
     * protected.
     *
     * @param servlet The servlet object which was called by the servlet
     * 		container to handle the request and which called this method.
     * @param req The original HttpServletRequest set by the servlet container
     * @param res The original HttpServletResponse set by the servlet container
     *
     * @throws ServletException if the module is not loaded or if it is thrown
     * 		during request processing.
     * @throws IOException if thrown during processing
     */
    void service(DeliveryServlet servlet, HttpServletRequest req,
		 HttpServletResponse res)
	    throws ServletException, IOException {

	// check whether we are loaded at all
	if (!loaded) {
	    throw new ServletException("DeliveryModule not loaded");
	}

	// request attachement has not been sent
	boolean attachementEventSent = false;

	// log the request start
        int requestId = requestCounter++;
        long requestTime = System.currentTimeMillis();
	if (requestLog.isInfoEnabled()) {
            // we build the message ourselves, which is somewhat faster than
            // MessageFormat here
            StringBuffer msg = new StringBuffer();
            msg.append('[').append(requestId).append("] -> ");
            msg.append(req.getMethod()).append(' ').append(req.getRequestURI());
            if (req.getQueryString() != null) {
                msg.append('?').append(req.getQueryString());
            }
            msg.append(' ').append(req.getProtocol());
            requestLog.info(msg);
	}

	// will be recycled in the finally block
	DeliveryHttpServletRequestImpl dreq = null;
	DeliveryHttpServletResponseImpl dres = null;

	// determine current servlet spec version
	ServletSpec spec = getServletSpec(servlet.getServletContext());
	if (spec == null) {
	    throw new UnavailableException("unknown servlet specification");
	}

	try {
	    threadEntered(req);

	    // wrap request and response
	    dreq = DeliveryHttpServletRequestImpl.getInstance(spec, req);
	    dres = DeliveryHttpServletResponseImpl.getInstance(spec, res, dreq, servlet);

	    // check webapp context
	    synchronized (this) {
		if (webappContext==null) {
		    webappContext = req.getContextPath();
		    /**
		     * Security precaution for erroneous servlet containers. According to
		     * the spec : `The path starts with a "/" character but does not end
		     * with a "/" character. For servlets in the default (root) context,
		     * this method returns "". The container does not decode this string.´
		     *
		     * From this follows, that prefix MUST NOT be "/", but then there
		     * might be containers not really following the spec ;-)
		     */
		    if (webappContext.equals("/")) {
			webappContext="";
		    }
		}
	    }

	    // Create ticket - end the request if the ticket is null
	    Ticket ticket = authHandler.authenticate(dreq, dres);
            if (ticket == null) {
                // request for valid ticket has been sent
                return;
            }

	    // assign the ticket to the delivery request
	    dreq.setTicket(ticket);

	    // Check burstmode cache - only if ticket not null
	    if (cacheHandler.burstModeCache(dreq, dres)) {
		return;
	    }

	    // notify listeners of the request and mark event sent
	    fireRequestAttached(dreq, dres);
	    attachementEventSent = true;

	    // create mapped url
	    MappedURL mappedURL = urlMapper.getMappedURL(ticket, dreq.getRealRequestURI());
	    if (mappedURL == null) {
		dres.sendError(HttpServletResponse.SC_NOT_FOUND);
		return;
	    } else {
		dreq.setMappedURL(mappedURL);
	    }

	    // authorize the page access
	    if (!ticket.isGranted(dreq.getHandle(), ACL.RIGHT_READ)) {
                // now call the authentication request
		if (!authHandler.requestAuthentication(dreq, dres)) {
		    dres.sendError(HttpServletResponse.SC_FORBIDDEN);
		}
		return;
	    }

	    // set the page and content element in the request
	    // ticket and handle are already known by now
	    try {
		dreq.loadContent();
	    } catch (ContentBusException cbe) {
		log.warn("service: cannot load page content for {0}: {1}",
		    dreq.getHandle(), cbe.toString());
		dres.sendError(HttpServletResponse.SC_NOT_FOUND);
		return;
	    }

	    // check page validity
	    if (!cms.showScheduledPages()) {
		try {
		    int validity = cms.getValidity(dreq.getPage());
		    if (validity != 0) {
			log.warn("service: Page ''{0}'' is {1}",
			    dreq.getHandle(),
			    validity < 0 ? "predated" : "outdated");
			dres.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		    }
		} catch (ContentBusException cbe) {
		    log.warn("service: Cannot check validity on page {0}: {1}",
			dreq.getHandle(), cbe.toString());
		    dres.sendError(HttpServletResponse.SC_NOT_FOUND);
		    return;
		}
	    }

	    // Check caching
	    DefaultHandlerData cacheHandlerData = cacheHandler.checkCache(dreq, dres);
	    if (cacheHandlerData == CacheHandlerService.FROM_CACHE) {
		return;
	    } else {
		// DeliveryHttpServletResponseImpl needs this, when an
                // OutputStream or a PrintWriter is requested.
		dres.setCacheHandlerData(cacheHandlerData);
	    }

            // Check link checking
            dres.setLinkCheckerData(linkChecker.checkLinkChecker(dreq, dres));

	    // analyze input data and create the ContentBusAction
	    try {
		dreq.handleInput();
	    } catch (DeliveryException de) {
		log.error("service: {0}", de.toString());
		dres.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		return;
	    }

            // wrap original ticket with DeliveryTicketWrapper
            dreq.setTicket(new DeliveryTicketWrapper(ticket, dres));

            // refetch page with new ticket
            dreq.loadContent();

            // Call the scripthandler
            scriptHandler.call(servlet, dreq, dres);

        } catch (ServletException se) {

            if (!dres.sendError(se)) {

                Throwable root = se.getRootCause();

                if (root == null
                    || !dres.sendError(root)) {

                    throw se;

                }

            }

        } catch (IOException ioe) {

            if (!dres.sendError(ioe)) {
                throw ioe;
            }

        } catch (RuntimeException re) {

            if (!dres.sendError(re)) {
                throw re;
            }

        } catch (ThreadDeath td) {
            // thread has been killed, just finish the request
            log.info("service: Request {0} has been aborted !",
                    dreq.getRequestURI());
            try {
                dres.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Request aborted");
            } catch (IllegalStateException ise) {
                throw new ServletException("Request aborted");
            }

        } catch (Error re) {

            if (!dres.sendError(re)) {
                throw re;
            }

 	} catch (Throwable t) {

            // handle as an internal server error
 	    log.warn("service: Unexpected Exception/Error for {0}: {1}",
                req.getRequestURI(), t.toString());
 	    if (log.isDebugEnabled()) log.debug("dump:", t);

            if (!dres.sendError(t)) {
                throw new ServletException("Unexpected Exception", t);
            }

	} finally {

	    // write the access log
	    if (accessLog.isInfoEnabled() && dreq != null && dres != null) {
		accessLog.info("{0} - {1} [{2}] \"{3} {4} {5}\" {6} {7} \"{8}\" \"{9}\"",
		    new Object[]{
			dreq.getRemoteAddr(),                              // 0
			checkAccessLogValue(dreq.getRemoteUser()),         // 1
			getCurrentTimeFormatted(),                         // 2
			dreq.getMethod(),                                  // 3
			req.getRequestURI(),                               // 4
			dreq.getProtocol(),                                // 5
			String.valueOf(dres.getStatus()),                  // 6
			/* bytes */ "-",                                   // 7
			checkAccessLogValue(dreq.getHeader("Referer")),    // 8
			checkAccessLogValue(dreq.getHeader("User-Agent"))  // 9
		    } );
	    }

	    // recylce request and response wrappers, if set
            String contentType = "unknown";  // unknown if dres not set
            int status = -1;                 // unknown if dres not set

            // notify end of request if attachement event sent
            if (attachementEventSent) {
                fireRequestDetached(dreq, dres);
            }

	    if (dres != null) {
                // get the original status and content type before recycling
                contentType = dres.getContentType();
                status = dres.getStatus();

		DeliveryHttpServletResponseImpl.recycleInstance(dres);
	    }
            if (dreq != null) {
                DeliveryHttpServletRequestImpl.recycleInstance(dreq);
            }

            // write the terminating request log
            // not sent to CQDE, see bug #9199
            if (requestLog.isInfoEnabled()) {
                // calculate the request time
                requestTime = System.currentTimeMillis() - requestTime;

                // we build the message ourselves, which is somewhat faster than
                // MessageFormat here
                requestLog.info("[" + requestId + "] <- " + status + " " +
                        contentType + " " + requestTime + "ms");
            }

	    threadExited(null);
	}

    }

    //---------- getting the services ------------------------------------------

    /**
     * Returns the URLMapper of the instance of this module.
     * @return the URLMapper of the instance of this module.
     */
    public static URLMapperService getURLMapperService() {
	return getInstance().urlMapper;
    }

    /**
     * Returns the {@link AuthenticationService} instance used.
     * @return the {@link AuthenticationService} instance used.
     */
    public AuthenticationService getAuthenticationService() {
        return authHandler;
    }

    /**
     * Returns the {@link CacheHandlerService} instance used.
     * @return the {@link CacheHandlerService} instance used.
     */
    public CacheHandlerService getCacheHandlerService() {
        return cacheHandler;
    }

    /**
     * Returns the {@link LinkCheckerService} instance used.
     * @return the {@link LinkCheckerService} instance used.
     */
    public LinkCheckerService getliLinkCheckerService() {
        return linkChecker;
    }

    /**
     * Return the {@link CmsService} instance used.
     * @return the {@link CmsService} instance used.
     */
    public CmsService getCmsService() {
        return cms;
    }

    /**
     * Returns the {@link ScriptHandlerService} instance used.
     * #return the {@link ScriptHandlerService} instance used.
     */
    public ScriptHandlerService getScriptHandlerService() {
        return scriptHandler;
    }

    /**
     * Returns the {@link ScriptHandlerService} instance used.
     * #return the {@link ScriptHandlerService} instance used.
     * 
     * @deprecated as of iguana. Use {@link #getScriptHandlerService()} instead.
     *      This is obviously the wrong name for the method.
     */
    public ScriptHandlerService getscScriptHandlerService() {
        return scriptHandler;
    }

    /**
     * Returns the singleton status provider for the <code>DevedServer</code>.
     */
    public static StatusProviderSpi getStatusProviderSpi() {
	return instance;
    }

    /**
     * Register a single service.
     * @param service service instance
     * @throws ServiceException
     */
    public void registerService(Service service) throws ServiceException {
        super.registerService(service);

        // nasty stuff
        if (service instanceof CacheHandlerService) {
            cacheHandler = (CacheHandlerService)service;
        } else if (service instanceof AuthenticationService) {
            authHandler = (AuthenticationService)service;
        } else if (service instanceof URLMapperService) {
            urlMapper = (URLMapperService)service;
        } else if (service instanceof LinkCheckerService) {
            linkChecker = (LinkCheckerService)service;
        } else if (service instanceof CmsService) {
            cms = (CmsService)service;
        } else if (service instanceof ScriptHandlerService) {
            scriptHandler = (ScriptHandlerService)service;
        }
    }

    /**
     * Return the servlet specification for the servlet engine, the delivery
     * module is currently running inside.
     */
    public ServletSpec getServletSpec(ServletContext context) {
	if (servletSpec == null) {
	    servletSpec = ServletSpec.getVersion(context);
	}
	return servletSpec;
    }

    //---------------------------------< Supporting RequestEventListener >-----

    /**
     * Add a listener to the list of listeners if it is not yet registered in
     * the list.
     *
     * @param listener The listener to add to the list. If <code>null</code>
     *      nothing is done.
     */
    public synchronized void addRequestEventListener(RequestEventListener listener) {
        // ignore if null
        if (listener != null) {
            addRequestEventListener(new RequestEventListenerWrapper(listener));
        }
    }

    /**
     * Remove a listener from the list of listeners if it is contained.
     *
     * @param listener The listener to remove from the list. If
     *      <code>null</code> nothing is done.
     */
    public synchronized void removeRequestEventListener(RequestEventListener listener) {
        if (listener != null) {
            removeRequestEventListener(new RequestEventListenerWrapper(listener));
        }
    }

    /**
     * Add a listener to the list of listeners if it is not yet registered in
     * the list.
     *
     * @param listener The listener to add to the list. If <code>null</code>
     *      nothing is done.
     */
    public synchronized void addRequestEventListener(RequestEventListener2 listener) {
        listeners.addListener(listener);
    }

    /**
     * Remove a listener from the list of listeners if it is contained.
     *
     * @param listener The listener to remove from the list. If
     *      <code>null</code> nothing is done.
     */
    public void removeRequestEventListener(RequestEventListener2 listener) {
        listeners.removeListener(listener);
    }

    /**
     * Notifies the registered listeners on the attachment and handling of a
     * new request.
     *
     * @param request The {@link DeliveryHttpServletRequest} whose processing is
     * 		about to begin.
     * @param response The {@link DeliveryHttpServletResponse} object for this
     *      request.
     */
    protected void fireRequestAttached(DeliveryHttpServletRequest request,
            DeliveryHttpServletResponse response) {
        RequestEvent event = new RequestEvent(request, response);
        Object[] listenerList = listeners.getListeners();
        for (int i=0; i < listenerList.length; i++) {
            ((RequestEventListener2) listenerList[i]).requestAttached(event);
        }
    }

    /**
     * Notifies the registered listeners on the detachment of a request. This
     * should only be called for any given request, if and only iff the
     * attachement event has been sent for the same request.
     *
     * @param request The {@link DeliveryHttpServletRequest} whose processing is
     * 		about to be terminated.
     * @param response The {@link DeliveryHttpServletResponse} object for this
     *      request.
     */
    protected void fireRequestDetached(DeliveryHttpServletRequest request,
            DeliveryHttpServletResponse response) {
        RequestEvent event = new RequestEvent(request, response);
        Object[] listenerList = listeners.getListeners();
        for (int i=0; i < listenerList.length; i++) {
            ((RequestEventListener2) listenerList[i]).requestDetached(event);
        }
    }

    /**
     * Flush a page in the output cache.
     *
     * @param handle handle to page in the contentbus whose output cache
     *               should be flushed
     */
    public static void flushOutputCache(String handle) {
	CacheHandlerService chs = getInstance().cacheHandler;
        if (chs != null) {
            chs.flushPageEntries(handle);
        }
    }

    //---------- StatusProvideSpi ----------------------------------------------

    /** Initialized status providers */
    private StatusProvider[] statusProviders = null;

    /**
     * Returns all toplevel status providers of this service provider.
     * @return an array of toplevel status providers.
     */
    public StatusProvider[] getStatusProviders() {
	if (statusProviders == null) {
	    DefaultStatusProvider dsp = new DefaultStatusProvider(
		    "delivery", "Delivery");
	    dsp.addSubProvider(
		    new DefaultStatusProvider("pending", "Pending Requests"));
	    statusProviders = new StatusProvider[] { dsp };
	}
	return statusProviders;
    }

    /**
     * Auxiliary class containing description of a pending request
     * <p>
     * Note: this class has a natural ordering that is inconsistent with equals.
     */
    public static class PendingRequest implements Comparable {

        /**
         * Thread name
         * @deprecated use accessor {@link #getThreadName()} instead
         */
        public final String threadName;

	/**
         * Request line
         * @deprecated use accessor {@link #getRequestLine()} instead
         */
	public final String requestLine;

	/**
         * Start time
         * @deprecated use accessor {@link #getStartTimeMs()} instead
         */
	public final long startTimeMs;

        /**
         * Client IP
         * @deprecated use accessor {@link #getClientIP()} instead
         */
        public final String clientIP;

        //---------- new fields ------------------------------------------------

        /** The thread associated with the request */
        private final Thread requestThread;

	/**
	 * Create a new <code>PendingRequest</code>
	 */
	public PendingRequest(HttpServletRequest request) {
            requestThread = Thread.currentThread();

            // old properties
            this.threadName = requestThread.getName();
            this.requestLine = getRequestLine(request);
            this.startTimeMs = System.currentTimeMillis();
            this.clientIP = request.getRemoteAddr();
	}

	/**
	 * Compute the request line given a request
	 */
	private String getRequestLine(HttpServletRequest request) {
            StringBuffer b = new StringBuffer(256);
            b.append(request.getMethod());
            b.append(' ');
            b.append(request.getRequestURI());

            String qs = request.getQueryString();
            if (qs != null) {
                b.append('?');
                b.append(qs);
            }

            b.append(' ');
            b.append(request.getProtocol());
            return b.toString();
	}

        //---------- Official Accessors ----------------------------------------

        public String getClientIP() {
            return clientIP;
        }

        public String getRequestLine() {
            return requestLine;
        }

        public long getStartTimeMs() {
            return startTimeMs;
        }

        public String getThreadName() {
            return threadName;
        }

        public String getRequestId() {
            return threadName;
        }

        //----------- Methods --------------------------------------------------

        /**
         * Aborts this pending request by sending stop to the request's thread.
         */
        public void abortRequest() {
            requestThread.stop();
        }
        
        //---------- Comparable interface --------------------------------------
        
        /**
         * Compares this instance to another <code>PendingRequet</code>
         * instance. <b>NOTE: The natural ordering implemented by this method
         * is inconsistent with <code>equals(Object)</code></b>.
         * 
         * @param other The other <code>PendingRequest</code> instance to
         *      compare this instance to.
         * 
         * @return <code>-1</code> if this pending request has been started
         *      before the other, <code>0</code> if both have been started at
         *      the same time, <code>+1</code> if this pending request has been
         *      started after the other.
         * 
         * @throws ClassCastException if <code>other</code> is not a
         *      <code>PendingRequest</code>.
         * @throws NullPointerException if <code>other</code> is
         *      <code>null</code>.
         * 
         */
        public int compareTo(Object other) {
            PendingRequest prs  = (PendingRequest) other;
            long diff = getStartTimeMs() - prs.getStartTimeMs();
            if (diff < 0) {
                return -1;
            } else if (diff > 0) {
                return 1;
            } else {
                return 0;
            }
        }

        //---------- Object overwrite ------------------------------------------
        
        /**
         * Returns a human readable representation of this instance.
         */
        public String toString() {
            return "PendingRequest id=\"" + getRequestId() + 
                "\", Client: " + getClientIP() +
                ", Start: " + new Date(getStartTimeMs()) +
                ", Thread: " + getThreadName() +
                "\r\n" + getRequestLine();
        }
    }

    /**
     * Add a thread to the internal list of pending requests
     */
    protected PendingRequest threadEntered(HttpServletRequest request) {
        PendingRequest pr = new PendingRequest(request);
	synchronized (pendingRequests) {
	    pendingRequests.put(pr.getThreadName(), pr);
	}
        return pr;
    }

    /**
     * Remove a thread from the internal list of pending requests
     */
    protected void threadExited(PendingRequest req) {
        String threadName = (req == null)
                ? Thread.currentThread().getName()
                : req.getThreadName();
	synchronized (pendingRequests) {
	    pendingRequests.remove(threadName);
	}
    }

    /**
     * Return a snapshot of the pending requests, sorted by ascending
     * start time (i.e. descending elapsed time).
     * @return snapshot, zero-sized if no requests are pending
     */
    public PendingRequest[] getPendingRequests() {
	PendingRequest[] prs;

	synchronized (pendingRequests) {
	    Collection c = pendingRequests.values();
	    c.toArray(prs = new PendingRequest[c.size()]);
	}

	Arrays.sort(prs);
    
	return prs;
    }
    
    /**
     * Return a snapshot of the pending requests as a string array, sorted by
     * ascending start time (i.e. descending elapsed time).
     * @return snapshot, zero-sized if no requests are pending
     */
    public String[] getStringPendingRequests() {
        return Support.toStringArray(getPendingRequests());
    }

    /**
     * Aborts the request with the given name.
     *
     * @param requestId The name of the request to abort
     *
     * @return A success or failure message is returned.
     */
    public String abortPendingRequest(String requestId) {
        synchronized (pendingRequests) {
            PendingRequest req = (PendingRequest) pendingRequests.get(requestId);
            if (req != null) {
                req.abortRequest();
                return "OK: Sent abort command to the request";
            } else {
                return "ERR: No such request exists";
            }
        }
    }

    //---------- internal ------------------------------------------------------

    /**
     * Checks that all needed request helper services have been assigned. If any
     * one service is missing a {@link ModuleException} is thrown.
     *
     * @throws ModuleException if one of the needed request helper services is
     *      not assigned.
     */
    private void checkRequestHelper() throws ModuleException {
        if (cacheHandler == null) {
            throw new ModuleException("Missing CacheHandlerService");

        } else if (authHandler == null) {
            throw new ModuleException("Missing AuthenticationService");

        } else if (urlMapper == null) {
            throw new ModuleException("Missing URLMapper");

        } else if (linkChecker == null) {
            throw new ModuleException("Missing LinkCheckerService");

        } else if (cms == null) {
            throw new ModuleException("Missing CmsService");

        } else if (scriptHandler == null) {
            throw new ModuleException("Missing ScriptHandlerService");
        }
    }

    /**
     * Returns the value if not <code>null</code> and not empty or a String
     * consisiting of a single dash if the value is <code>null</code> or empty.
     */
    private static String checkAccessLogValue(String value) {
	return (value == null || value.length() == 0) ? "-" : value;
    }

    /** last zone offset (cached by hours) */
    private static String lastZoneOffset = "";
    private static long lastZoneOffsetHour = -1;

    /** last formatted time (cached in seconds) */
    private static String lastTimeFormatted = "";
    private static long lastTimeFormattedSeconds = -1;

    /**
     * Calculates and formats the current time according to default format
     * for access.log files. please note that this method is synchronized,
     * because the formatting routines are not thread safe.
     */
    private synchronized static String getCurrentTimeFormatted() {
	long now = System.currentTimeMillis();
        if (now / 1000 != lastTimeFormattedSeconds) {
            lastTimeFormattedSeconds = now / 1000;
            Date date = new Date(now);
	    StringBuffer buf = new StringBuffer(fmt.format(date));
            if (now / 3600000 != lastZoneOffsetHour) {
                lastZoneOffsetHour = now / 3600000;
		calendar.setTime(date);
		int tzOffset = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
		tzOffset /= (60 * 1000);
		tzOffset = ((tzOffset / 60) * 100) + (tzOffset % 60);

                lastZoneOffset = dfmt.format(tzOffset);
            }
            buf.append(lastZoneOffset);
            lastTimeFormatted = buf.toString();
        }
	return lastTimeFormatted;
    }
}

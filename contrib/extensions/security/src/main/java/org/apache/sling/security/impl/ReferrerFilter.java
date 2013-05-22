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
package org.apache.sling.security.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype=true, description="%referrer.description",
        label="%referrer.name")
@Property(name="pattern", value="/.*", propertyPrivate=true)
@Service(value=Filter.class)
public class ReferrerFilter implements Filter {

    /**
     * Request header providing the clients user agent information used
     * by {@link #isBrowserRequest(HttpServletRequest)} to decide whether
     * a request is probably sent by a browser or not.
     */
    private static final String USER_AGENT = "User-Agent";

    /**
     * String contained in a {@link #USER_AGENT} header indicating a Mozilla
     * class browser. Examples of such browsers are Firefox (generally Gecko
     * based browsers), Safari, Chrome (probably generally WebKit based
     * browsers), and Microsoft IE.
     */
    private static final String BROWSER_CLASS_MOZILLA = "Mozilla";

    /**
     * String contained in a {@link #USER_AGENT} header indicating a Opera class
     * browser. The only known browser in this class is the Opera browser.
     */
    private static final String BROWSER_CLASS_OPERA = "Opera";

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Default value for allow empty. */
    private static final boolean DEFAULT_ALLOW_EMPTY = true;

    /** Allow empty property. */
    @Property(boolValue=DEFAULT_ALLOW_EMPTY)
    private static final String PROP_ALLOW_EMPTY = "allow.empty";

    private static final String[] DEFAULT_PROP_HOSTS = {};

    /** Allow referrer uri hosts property. */
    @Property(unbounded=PropertyUnbounded.ARRAY)
    private static final String PROP_HOSTS = "allow.hosts";

    /** Allow referrer regex hosts property */
    @Property(unbounded=PropertyUnbounded.ARRAY)
    private static final String PROP_HOSTS_REGEX = "allow.hosts.regexp";

    /** Filtered methods property */
    @Property(unbounded=PropertyUnbounded.ARRAY, value={"POST", "PUT", "DELETE"})
    private static final String PROP_METHODS = "filter.methods";



    /** Do we allow empty referrer? */
    private boolean allowEmpty;

    /** Allowed uri referrers */
    private URL[] allowedUriReferrers;

    /** Allowed regexp referrers */
    private Pattern[] allowedRegexReferrers;

    /** Methods to be filtered. */
    private String[] filterMethods;

    private ServiceRegistration configPrinterRegistration;

    /**
     * Create a default list of referrers
     */
    private Set<String> getDefaultAllowedReferrers() {
        final Set<String> referrers = new HashSet<String>();
        try {
            final Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();

            while(ifaces.hasMoreElements()){
                final NetworkInterface iface = ifaces.nextElement();
                logger.info("Adding Allowed referers for Interface:" + iface.getDisplayName());
                final Enumeration<InetAddress> ias = iface.getInetAddresses();
                while(ias.hasMoreElements()){
                    final InetAddress ia = ias.nextElement();
                    final String address = ia.getHostAddress().trim().toLowerCase();
                    if ( ia instanceof Inet4Address ) {
                        referrers.add("http://" + address + ":0");
                        referrers.add("https://" + address + ":0");
                    }
                    if ( ia instanceof Inet6Address ) {
                        referrers.add("http://[" + address + "]" + ":0");
                        referrers.add("https://[" + address + "]" + ":0");
                    }
                }
            }
        } catch ( final SocketException se) {
            logger.error("Unable to detect network interfaces", se);
        }
        referrers.add("http://localhost" + ":0");
        referrers.add("http://127.0.0.1" + ":0");
        referrers.add("http://[::1]" + ":0");
        referrers.add("https://localhost" + ":0");
        referrers.add("https://127.0.0.1" + ":0");
        referrers.add("https://[::1]" + ":0");

        return referrers;
    }

    private void add(final List<URL> urls, final String ref) {
        try {
            final URL u  = new URL(ref);
            urls.add(u);
        } catch (final MalformedURLException mue) {
            logger.warn("Unable to create URL from " + ref + " : " + mue.getMessage());
        }
    }

    /**
     * Create URLs out of the uri referrer set
     */
    private URL[] createReferrerUrls(final Set<String> referrers) {
        final List<URL> urls = new ArrayList<URL>();

        for(final String ref : referrers) {
            final int pos = ref.indexOf("://");
            // valid url?
            if ( pos != -1 ) {
                this.add(urls, ref);
            } else {
                this.add(urls, "http://" + ref + ":0");
                this.add(urls, "https://" + ref + ":0");
            }
        }
        return urls.toArray(new URL[urls.size()]);
    }

    /**
     * Create Patterns out of the regexp referrer list
     */
    private Pattern[] createReferrerPatterns(final String[] regexps) {
        final List<Pattern> patterns = new ArrayList<Pattern>();
        for(final String regexp : regexps) {
            try {
                final Pattern pattern  = Pattern.compile(regexp);
                patterns.add(pattern);
            } catch (final Exception e) {
                logger.warn("Unable to create Pattern from {} : {}", new String[]{regexp, e.getMessage()});
            }
        }
        return patterns.toArray(new Pattern[patterns.size()]);
    }

    /**
     * Activate
     */
    @Activate
    protected void activate(final ComponentContext ctx) {
        final Dictionary props = ctx.getProperties();

        this.allowEmpty = PropertiesUtil.toBoolean(props.get(PROP_ALLOW_EMPTY), DEFAULT_ALLOW_EMPTY);

        final String[] allowRegexHosts = defaultIfEmpty(PropertiesUtil.toStringArray(props.get(PROP_HOSTS_REGEX),
                DEFAULT_PROP_HOSTS), DEFAULT_PROP_HOSTS);
        this.allowedRegexReferrers = createReferrerPatterns(allowRegexHosts);

        final Set<String> allowUriReferrers = getDefaultAllowedReferrers();
        final String[] allowHosts = defaultIfEmpty(PropertiesUtil.toStringArray(props.get(PROP_HOSTS),
                DEFAULT_PROP_HOSTS), DEFAULT_PROP_HOSTS);
        allowUriReferrers.addAll(Arrays.asList(allowHosts));
        this.allowedUriReferrers = createReferrerUrls(allowUriReferrers);

        this.filterMethods = PropertiesUtil.toStringArray(props.get(PROP_METHODS));
        if ( this.filterMethods != null && this.filterMethods.length == 1 && (this.filterMethods[0] == null || this.filterMethods[0].trim().length() == 0) ) {
            this.filterMethods = null;
        }
        if ( this.filterMethods != null ) {
            for(int i=0; i<filterMethods.length; i++) {
                filterMethods[i] = filterMethods[i].toUpperCase();
            }
        }
        this.configPrinterRegistration = registerConfigPrinter(ctx.getBundleContext());
    }

    @Deactivate
    protected void deactivate() {
        this.configPrinterRegistration.unregister();
    }

    private ServiceRegistration registerConfigPrinter(BundleContext bundleContext) {
        final ConfigurationPrinter cfgPrinter = new ConfigurationPrinter();
        final Dictionary<String, String> serviceProps = new Hashtable<String, String>();
        serviceProps.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling Referrer Filter Configuration Printer");
        serviceProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        serviceProps.put("felix.webconsole.label", "slingreferrerfilter");
        serviceProps.put("felix.webconsole.title", "Sling Referrer Filter");
        serviceProps.put("felix.webconsole.configprinter.modes", "always");

       return bundleContext.registerService(Object.class.getName(),
                cfgPrinter, serviceProps);
    }


    private boolean isModification(final HttpServletRequest req) {
        final String method = req.getMethod();
        if ( filterMethods != null ) {
            for(final String m : filterMethods) {
                if ( m.equals(method) ) {
                    return true;
                }
            }
        }
        return false;
    }

    public void doFilter(final ServletRequest req,
                         final ServletResponse res,
                         final FilterChain chain)
    throws IOException, ServletException {
        if ( req instanceof HttpServletRequest && res instanceof HttpServletResponse ) {
            final HttpServletRequest request = (HttpServletRequest)req;

            // is this a modification request from a browser
            if ( this.isBrowserRequest(request) && this.isModification(request) ) {
                if ( !this.isValidRequest(request) ) {
                    final HttpServletResponse response = (HttpServletResponse)res;
                    // we use 403
                    response.sendError(403);
                    return;
                }
            }
        }
        chain.doFilter(req, res);
    }

    final static class HostInfo {
        public String host;
        public String scheme;
        public int port;
        public String toURI() {
            return scheme + "://" + host + ":" + port;
        }
    }

    HostInfo getHost(final String referrer) {
        final int startPos = referrer.indexOf("://") + 3;
        if ( startPos == 2 ) {
            // we consider this illegal
            return null;
        }
        final HostInfo info = new HostInfo();
        info.scheme = referrer.substring(0, startPos - 3);

        final int paramStart = referrer.indexOf('?');
        final String hostAndPath = (paramStart == -1 ? referrer : referrer.substring(0, paramStart));
        final int endPos = hostAndPath.indexOf('/', startPos);
        final String hostPart = (endPos == -1 ? hostAndPath.substring(startPos) : hostAndPath.substring(startPos, endPos));
        final int hostNameStart = hostPart.indexOf('@') + 1;
        final int hostNameEnd = hostPart.lastIndexOf(':');
        if (hostNameEnd < hostNameStart ) {
            info.host = hostPart.substring(hostNameStart);
            if ( info.scheme.equals("http") ) {
                info.port = 80;
            } else if ( info.scheme.equals("https") ) {
                info.port = 443;
            }
        } else {
            info.host = hostPart.substring(hostNameStart, hostNameEnd);
            info.port = Integer.valueOf(hostPart.substring(hostNameEnd + 1));
        }
        return info;
    }

    boolean isValidRequest(final HttpServletRequest request) {
        final String referrer = request.getHeader("referer");
        // check for missing/empty referrer
        if ( referrer == null || referrer.trim().length() == 0 ) {
            if ( !this.allowEmpty ) {
                this.logger.info("Rejected empty referrer header for {} request to {}", request.getMethod(), request.getRequestURI());
            }
            return this.allowEmpty;
        }
        // check for relative referrer - which is always allowed
        if ( referrer.indexOf(":/") == - 1 ) {
            return true;
        }
        // check for air referrer - which is always allowed
        if ( referrer.startsWith("app:/") ) {
            return true;
        }

        final HostInfo info = getHost(referrer);
        if ( info == null ) {
            // if this is invalid we just return invalid
            this.logger.info("Rejected illegal referrer header for {} request to {} : {}",
                    new Object[] {request.getMethod(), request.getRequestURI(), referrer});
            return false;
        }

        // allow the request if the host name of the referrer is
        // the same as the request's host name
        if ( info.host.equals(request.getServerName()) ) {
            return true;
        }

        // allow the request if the referrer matches any of the allowed referrers
        boolean valid = isValidUriReferrer(info) || isValidRegexReferrer(info);

        if ( !valid) {
            this.logger.info("Rejected referrer header for {} request to {} : {}",
                    new Object[] {request.getMethod(), request.getRequestURI(), referrer});
        }
        return valid;
    }

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(final FilterConfig config) throws ServletException {
        // nothing to do
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        // nothing to do
    }

    /**
     * @param hostInfo The hostInfo to check for validity
     * @return <code>true</code> if the hostInfo matches any of the allowed URI referrer.
     */
    private boolean isValidUriReferrer(HostInfo hostInfo) {
        for(final URL ref : this.allowedUriReferrers) {
            if ( hostInfo.host.equals(ref.getHost()) && hostInfo.scheme.equals(ref.getProtocol()) ) {
                if ( ref.getPort() == 0 || hostInfo.port == ref.getPort() ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param hostInfo The hostInfo to check for validity
     * @return <code>true</code> if the hostInfo matches any of the allowed regexp referrer.
     */
    private boolean isValidRegexReferrer(HostInfo hostInfo) {
        for(final Pattern ref : this.allowedRegexReferrers) {
            String url = hostInfo.toURI();
            if (ref.matcher(url).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return The <code>defaultProperties</code> if <code>properties</code> contains a single empty string,
     *         <code>properties</code> otherwise.
     */
    private String[] defaultIfEmpty(String[] properties, String[] defaultProperties) {
        return properties.length == 1 && properties[0].trim().length() == 0
                ? defaultProperties
                : properties;
    }

    /**
     * Returns <code>true</code> if the given request can be assumed to be sent
     * by a client browser such as Firefix, Internet Explorer, etc.
     * <p>
     * This method inspects the <code>User-Agent</code> header and returns
     * <code>true</code> if the header contains the string <i>Mozilla</i> (known
     * to be contained in Firefox, Internet Explorer, WebKit-based browsers
     * User-Agent) or <i>Opera</i> (known to be contained in the Opera
     * User-Agent).
     *
     * @param request The request to inspect
     * @return <code>true</code> if the request is assumed to be sent by a
     *         browser.
     */
    private boolean isBrowserRequest(final HttpServletRequest request) {
        final String userAgent = request.getHeader(USER_AGENT);
        if (userAgent != null && (userAgent.contains(BROWSER_CLASS_MOZILLA) || userAgent.contains(BROWSER_CLASS_OPERA))) {
            return true;
        }
        return false;
    }

    public class ConfigurationPrinter {

        /**
         * Print out the allowedReferrers
         * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
         */
        public void printConfiguration(final PrintWriter pw) {
            pw.println("Current Apache Sling Referrer Filter Allowed Referrers:");
            pw.println();
            for (final URL url : allowedUriReferrers) {
                pw.println(url.toString());
            }
            for (final Pattern pattern : allowedRegexReferrers) {
                pw.println(pattern.toString());
            }
        }

    }
}

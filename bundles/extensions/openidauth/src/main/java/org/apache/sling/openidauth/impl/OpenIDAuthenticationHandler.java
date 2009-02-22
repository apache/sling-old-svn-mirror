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
package org.apache.sling.openidauth.impl;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.engine.auth.AuthenticationHandler;
import org.apache.sling.engine.auth.AuthenticationInfo;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.apache.sling.openidauth.OpenIDConstants;
import org.apache.sling.openidauth.OpenIDUserUtil;
import org.apache.sling.openidauth.OpenIDConstants.OpenIDFailure;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyuproject.openid.OpenIdUser;
import com.dyuproject.openid.RelyingParty;
import com.dyuproject.openid.manager.CookieBasedUserManager;

/**
 * The <code>AuthorizationHeaderAuthenticationHandler</code> class implements
 * the authorization steps based on the Authorization header of the HTTP
 * request. This authenticator should eventually support both BASIC and DIGEST
 * authentication methods.
 *
 * @scr.component immediate="false" label="%auth.openid.name"
 *                description="%auth.openid.description"
 * @scr.property name="service.description" value="Apache Sling OpenID Authentication Handler"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property nameRef="AuthenticationHandler.PATH_PROPERTY" values.0="/"
 * @scr.service
 */
public class OpenIDAuthenticationHandler implements
        AuthenticationHandler, LoginModulePlugin {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * @scr.property valueRef="DEFAULT_LOGIN_FORM"
     */
    public static final String PROP_LOGIN_FORM = "openid.login.form";
    
    public static final String DEFAULT_LOGIN_FORM = "/system/sling/openid/loginform.html";

    
    /**
     * @scr.property valueRef="DEFAULT_LOGIN_IDENTIFIER_FORM_FIELD"
     */
    public static final String PROP_LOGIN_IDENTIFIER_FORM_FIELD = "openid.login.identifier";
    
    public static final String DEFAULT_LOGIN_IDENTIFIER_FORM_FIELD = RelyingParty.DEFAULT_IDENTIFIER_PARAMETER;

    
    /**
     * @scr.property valueRef="DEFAULT_ORIGINAL_URL_ON_SUCCESS" type="Boolean"
     */
    public static final String PROP_ORIGINAL_URL_ON_SUCCESS = "openid.original.url.onsuccess";
    
    public static final boolean DEFAULT_ORIGINAL_URL_ON_SUCCESS = true;

    
    /**
     * @scr.property valueRef="DEFAULT_AUTH_SUCCESS_URL"
     */
    public static final String PROP_AUTH_SUCCESS_URL = "openid.login.success";
    
    public static final String DEFAULT_AUTH_SUCCESS_URL = "/system/sling/openid/authsuccess.html";
    
    
    /**
     * @scr.property valueRef="DEFAULT_AUTH_FAIL_URL"
     */
    public static final String PROP_AUTH_FAIL_URL = "openid.login.fail";
    
    public static final String DEFAULT_AUTH_FAIL_URL = "/system/sling/openid/authfail.html";
    
    
    /**
     * @scr.property valueRef="DEFAULT_LOGOUT_URL"
     */
    public static final String PROP_LOGOUT_URL = "openid.logout";
    
    public static final String DEFAULT_LOGOUT_URL = "/system/sling/openid/logout.html";
    
    
    /**
     * @scr.property valueRef="DEFAULT_EXTERNAL_URL_PREFIX"
     */
    public static final String PROP_EXTERNAL_URL_PREFIX = "openid.external.url.prefix";
    
    public static final String DEFAULT_EXTERNAL_URL_PREFIX = "http://my.external.sling.com";
    
    
    /**
     * @scr.property valueRef="DEFAULT_OPENID_USERS_PASSWORD"
     */
    public static final String PROP_OPENID_USERS_PASSWORD = "openid.users.password";
    
    public static final String DEFAULT_OPENID_USERS_PASSWORD = "changeme";
    

    /**
     * @scr.property valueRef="DEFAULT_ANONYMOUS_AUTH_RESOURCES" type="Boolean"
     */
    public static final String PROP_ANONYMOUS_AUTH_RESOURCES = "openid.anon.auth.resources";
    
    public static final boolean DEFAULT_ANONYMOUS_AUTH_RESOURCES = true;

    
    /**
     * @scr.property valueRef="DEFAULT_USE_COOKIE" type="Boolean"
     */
    public static final String PROP_USE_COOKIE = "openid.use.cookie";
    
    public static final boolean DEFAULT_USE_COOKIE = false;

    
    /**
     * @scr.property valueRef="DEFAULT_COOKIE_DOMAIN"
     */
    public static final String PROP_COOKIE_DOMAIN = "openid.cookie.domain";
    
    public static final String DEFAULT_COOKIE_DOMAIN = ".sling.com";
    
    
    /**
     * @scr.property valueRef="DEFAULT_COOKIE_NAME"
     */
    public static final String PROP_COOKIE_NAME = "openid.cookie.name";
    
    public static final String DEFAULT_COOKIE_NAME = "sling.openid";
    
    
    /**
     * @scr.property valueRef="DEFAULT_COOKIE_PATH"
     */
    public static final String PROP_COOKIE_PATH = "openid.cookie.path";
    
    public static final String DEFAULT_COOKIE_PATH = "/";
    
    
    /**
     * @scr.property valueRef="DEFAULT_COOKIE_SECRET_KEY"
     */
    public static final String PROP_COOKIE_SECRET_KEY = "openid.cookie.secret.key";
    
    public static final String DEFAULT_COOKIE_SECRET_KEY = "secret";
    
        
    static final String SLASH = "/";
    
    private ComponentContext context;
    
    private String loginForm;
    private String authSuccessUrl;
    private String authFailUrl;
    private String logoutUrl;
    private boolean accessAuthPageAnon;
    
    private boolean redirectToOriginalUrl;
    private String externalUrlPrefix;
    private boolean useCookie;
    private String cookieDomain;
    private String cookieName;
    private String cookiePath;
    private String identifierParam;
	
	private RelyingParty relyingParty;
	
	
    public OpenIDAuthenticationHandler() {
        log.info("OpenIDAuthenticationHandler created");
    }

    // ----------- AuthenticationHandler interface ----------------------------

    /**
     * Extracts credential data from the request if at all contained. This check
     * is only based on the original request object, no URI translation has
     * taken place yet.
     * <p>
     * The method returns any of the following values : <table>
     * <tr>
     * <th>value
     * <th>description</tr>
     * <tr>
     * <td><code>null</code>
     * <td>no user details were contained in the request </tr>
     * <tr>
     * <td>{@link AuthenticationInfo#DOING_AUTH}
     * <td>the handler is in an ongoing authentication exchange with the
     * client. The request handling is terminated.
     * <tr>
     * <tr>
     * <td>valid credentials
     * <td>The user sent credentials.</tr>
     * </table>
     * <p>
     * The method must not request credential information from the client, if
     * they are not found in the request.
     * <p>
     * Note : The implementation should pay special attention to the fact, that
     * the request may be for an included servlet, in which case the values for
     * some URI specific values are contained in javax.servlet.include.* request
     * attributes.
     *
     * @param request The request object containing the information for the
     *            authentication.
     * @param response The response object which may be used to send the
     *            information on the request failure to the user.
     * @return A valid Credentials instance identifying the request user,
     *         DOING_AUTH if the handler is in an authentication transaction with
     *         the client or null if the request does not contain authentication
     *         information. In case of DOING_AUTH, the method must have sent a
     *         response indicating that fact to the client.
     */
    public AuthenticationInfo authenticate(HttpServletRequest request,
            HttpServletResponse response) {

        // extract credentials and return
        AuthenticationInfo info = this.extractAuthentication(request, response);
        if (info != null) {
            return info;
        }

        return null;
    }

    /**
     * Sends status <code>401</code> (Unauthorized) with a
     * <code>WWW-Authenticate</code> requesting standard HTTP header
     * authentication with the <code>Basic</code> scheme and the configured
     * realm name. If the response is already committed, an error message is
     * logged but the 401 status is not sent.
     *
     * @param request The request object
     * @param response The response object to which to send the request
     * @return <code>true</code> is always returned by this handler
     * @throws IOException if an error occurrs sending back the response.
     */
    public boolean requestAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // if the response is already committed, we have a problem !!
        if (!response.isCommitted()) {
        	
        	// If we're here & we have a valid authenticated user
        	// probably we failed the repository login (no repo user
        	// configured for the authenticated principal)
        	OpenIdUser user = (OpenIdUser)request.getAttribute(OpenIDConstants.OPEN_ID_USER_ATTRIBUTE);
        	if(user != null && user.isAuthenticated()) {
        		request.getSession().setAttribute(
        				OpenIDConstants.OPENID_FAILURE_REASON_ATTRIBUTE, 
        				OpenIDConstants.OpenIDFailure.REPOSITORY);
        	}

        	// requestAuthentication is only called after a failed authentication
        	// so it makes sense to remove any existing login
        	relyingParty.invalidate(request, response);

        	// original URL is set only if it doesn't already exist        	
        	if(request.getSession().getAttribute(OpenIDConstants.ORIGINAL_URL_ATTRIBUTE) == null) {
        		String originalUrl = request.getRequestURI() +
        			(request.getQueryString() != null ? "?" + request.getQueryString() : "");
        		
        		// handle corner case where login form requested directly
        		if(!originalUrl.equals(loginForm)) {
        			request.getSession().setAttribute(OpenIDConstants.ORIGINAL_URL_ATTRIBUTE, originalUrl);
        		}
        	}
        	response.sendRedirect(loginForm);
        } else {
            log.error("requestAuthentication: Response is committed, cannot request authentication");
        }

        return true;
    }

    protected AuthenticationInfo handleAuthFailure(OpenIDFailure failure, HttpServletRequest request, HttpServletResponse response) 
    	throws IOException {

    	request.getSession().setAttribute(OpenIDConstants.OPENID_FAILURE_REASON_ATTRIBUTE, failure);
		
		if(authFailUrl != null && !"".equals(authFailUrl)) {
			response.sendRedirect(authFailUrl);
			return AuthenticationInfo.DOING_AUTH;
		} else {
			return null;
		}
    }
    
    protected AuthenticationInfo handleLogout(HttpServletRequest request, HttpServletResponse response) 
    	throws IOException {
		String redirectUrl = null;
		
		if(request.getParameter(OpenIDConstants.REDIRECT_URL_PARAMETER) != null) {
			redirectUrl = request.getParameter(OpenIDConstants.REDIRECT_URL_PARAMETER);
		} else {
			redirectUrl = logoutUrl;
		}
		
		// fallback
		if(redirectUrl == null) {
			redirectUrl = "/";
		}
		
		response.sendRedirect(redirectUrl);
		return AuthenticationInfo.DOING_AUTH;
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext componentContext) {
    	context = componentContext;
    	
    	loginForm = OsgiUtil.toString(
         		context.getProperties().get(PROP_LOGIN_FORM), 
         		DEFAULT_LOGIN_FORM);
    	
    	authSuccessUrl = OsgiUtil.toString(
         		context.getProperties().get(PROP_AUTH_SUCCESS_URL), 
         		DEFAULT_AUTH_SUCCESS_URL);
    	
    	authFailUrl = OsgiUtil.toString(
         		context.getProperties().get(PROP_AUTH_FAIL_URL), 
         		DEFAULT_AUTH_FAIL_URL);
    	
    	logoutUrl = OsgiUtil.toString(
         		context.getProperties().get(PROP_LOGOUT_URL), 
         		DEFAULT_LOGOUT_URL);
    	
    	redirectToOriginalUrl = OsgiUtil.toBoolean(
         		context.getProperties().get(PROP_ORIGINAL_URL_ON_SUCCESS), 
         		DEFAULT_ORIGINAL_URL_ON_SUCCESS);
    	
    	accessAuthPageAnon = OsgiUtil.toBoolean(
         		context.getProperties().get(PROP_ANONYMOUS_AUTH_RESOURCES), 
         		DEFAULT_ANONYMOUS_AUTH_RESOURCES);
    	
    	externalUrlPrefix = OsgiUtil.toString(
    			context.getProperties().get(PROP_EXTERNAL_URL_PREFIX),
    			DEFAULT_EXTERNAL_URL_PREFIX);
    	
    	// DYU OpenID properties
    	useCookie = OsgiUtil.toBoolean(
         		context.getProperties().get(PROP_USE_COOKIE), 
         		DEFAULT_USE_COOKIE);
    	
    	cookieDomain = OsgiUtil.toString(
    			context.getProperties().get(PROP_COOKIE_DOMAIN),
    			DEFAULT_COOKIE_DOMAIN);
    	
    	cookieName = OsgiUtil.toString(
    			context.getProperties().get(PROP_COOKIE_NAME),
    			DEFAULT_COOKIE_NAME);
    	
    	cookiePath = OsgiUtil.toString(
    			context.getProperties().get(PROP_COOKIE_PATH),
    			DEFAULT_COOKIE_PATH);
    	
    	identifierParam = OsgiUtil.toString(
        		context.getProperties().get(PROP_LOGIN_IDENTIFIER_FORM_FIELD), 
        		DEFAULT_LOGIN_IDENTIFIER_FORM_FIELD);
        
    	String cookieSecret = OsgiUtil.toString(
    			context.getProperties().get(PROP_COOKIE_SECRET_KEY),
    			DEFAULT_COOKIE_SECRET_KEY);
    	
        Properties openIdProps = new Properties();
        
        openIdProps.setProperty("openid.identifier.parameter", identifierParam);
        
        if(useCookie) {
        	openIdProps.setProperty("openid.user.manager", CookieBasedUserManager.class.getName());
        	openIdProps.setProperty("openid.user.manager.cookie.name", cookieName);
        	openIdProps.setProperty("openid.user.manager.cookie.path", cookiePath);
        	openIdProps.setProperty("openid.user.manager.cookie.domain", cookieDomain);
        	openIdProps.setProperty("openid.user.manager.cookie.security.secret_key", cookieSecret);
        }
        
		relyingParty = RelyingParty.newInstance(openIdProps);
    }

    // ---------- internal -----------------------------------------------------

    protected AuthenticationInfo extractAuthentication(
            HttpServletRequest request, HttpServletResponse response) {

    	
    	OpenIdUser user = null;
    	
        try
        {
            user = relyingParty.discover(request);
            
            // Authentication timeout
            if(user == null && RelyingParty.isAuthResponse(request))
            {
            	log.debug("OpenID authentication timeout");
                response.sendRedirect(request.getRequestURI());
                return AuthenticationInfo.DOING_AUTH;
            }
            
	    	if(request.getPathInfo() != null) {
	    		String requestPath = request.getPathInfo();
	    		if(requestPath != null) {
	    			if(OpenIDConstants.LOGOUT_REQUEST_PATH.equals(requestPath)) {
	    				relyingParty.invalidate(request, response);
    					user = null;
    					return handleLogout(request, response);
	    			} 
	    			// handle (possibly)anon auth resources
	    			else if (loginForm.equals(requestPath) || 
	    					authFailUrl.equals(requestPath) ||
	    					logoutUrl.equals(requestPath)) {
	    				
	    				if (loginForm.equals(requestPath)) {
		    				// can force a login with Allow Anonymous enabled, by requesting
		    				// login form directly.  Checking this parameter allows us
		    				// to redirect user somewhere useful if login is successful
		    				if(request.getParameter(OpenIDConstants.REDIRECT_URL_PARAMETER) != null) {
		    					request.getSession().setAttribute(OpenIDConstants.ORIGINAL_URL_ATTRIBUTE, 
		    							request.getParameter(OpenIDConstants.REDIRECT_URL_PARAMETER));
		    				}
		    				
		    				moveAttributeFromSessionToRequest(
		    						OpenIDConstants.OPENID_FAILURE_REASON_ATTRIBUTE, 
		    						OpenIDConstants.OpenIDFailure.class,
		    						request);
		    				
		    				moveAttributeFromSessionToRequest(
		    						OpenIDConstants.ORIGINAL_URL_ATTRIBUTE, 
		    						String.class,
		    						request);
		    				
	    				} else if (authFailUrl.equals(requestPath)) {
	    					// move the failure reason attribute from session to request
	    					moveAttributeFromSessionToRequest(
		    						OpenIDConstants.OPENID_FAILURE_REASON_ATTRIBUTE, 
		    						OpenIDConstants.OpenIDFailure.class,
		    						request);
	    					
	    					moveAttributeFromSessionToRequest(
		    						OpenIDConstants.ORIGINAL_URL_ATTRIBUTE, 
		    						String.class,
		    						request);
	    				}
	    				
	    				if(accessAuthPageAnon) {
	    					// Causes anonymous login
	    					// but does not respect SlingAuthenticator allowAnonymous
	    					return new AuthenticationInfo(OpenIDConstants.OPEN_ID_AUTH_TYPE, null);
	    				}
	    			}
	    		}
	    	}
        	
            if(user != null) {
	            if(user.isAuthenticated()) {
	                // user already authenticated
	                request.setAttribute(OpenIdUser.ATTR_NAME, user);
	                return getAuthInfoFromUser(user);
	            } else if(user.isAssociated()) {
	            	if(RelyingParty.isAuthResponse(request)) {
		            	if(relyingParty.verifyAuth(user, request, response)) {
		                    // authenticated                    
		                    response.sendRedirect(request.getRequestURI());
		                    return AuthenticationInfo.DOING_AUTH;
		                } else {
		                    // failed verification
		                	AuthenticationInfo authInfo = handleAuthFailure(OpenIDFailure.VERIFICATION, request, response);
		    				if(authInfo != null) {
		    					return authInfo;
		    				}
		                }
		            } else {
		            	// Assume a cancel or some other non-successful response from provider
		            	// failed verification
		            	relyingParty.invalidate(request, response);
		            	user = null;
		            	
	                	AuthenticationInfo authInfo = handleAuthFailure(OpenIDFailure.AUTHENTICATION, request, response);
	    				if(authInfo != null) {
	    					return authInfo;
	    				}
		            }
	            } else {
		            // associate and authenticate user
		            StringBuffer url = null; 
		            String trustRoot = null;
		            String returnTo = null;
		            
		            if(externalUrlPrefix != null && !"".equals(externalUrlPrefix.trim())) {
		            	url = new StringBuffer(externalUrlPrefix).append(request.getRequestURI());
		            	trustRoot = externalUrlPrefix;
		            } else {
		            	url = request.getRequestURL();
		            	trustRoot = url.substring(0, url.indexOf(SLASH, 9));
		            }
		            
	            	String realm = url.substring(0, url.lastIndexOf(SLASH));
	            	
		            if(redirectToOriginalUrl) {
		            	returnTo = url.toString();        
		            } else {
		            	request.setAttribute(OpenIDConstants.ORIGINAL_URL_ATTRIBUTE, request.getRequestURI());
		            	returnTo =  authSuccessUrl;
		    		}
		            
		            if(relyingParty.associateAndAuthenticate(user, request, response, trustRoot, realm, 
		                    returnTo)) {
		                // user is associated and then redirected to his openid provider for authentication                
		                return AuthenticationInfo.DOING_AUTH;
		            } else {
		            	// failed association or auth request generation
	                	AuthenticationInfo authInfo = handleAuthFailure(OpenIDFailure.ASSOCIATION, request, response);
	    				if(authInfo != null) {
	    					return authInfo;
	    				}
		            }
	            }
            }
        } catch(Exception e) {
        	log.error("Error processing OpenID request", e);
        }
    	
    	return null;
    }
    
    private <T> T removeAttributeFromSession(String attrName, Class<T> type, HttpServletRequest request) {
    	T attr = (T)request.getSession().getAttribute(attrName);
		request.getSession().removeAttribute(attrName);
		return attr;
    }
    
    private <T> T moveAttributeFromSessionToRequest(String attrName, Class<T> type, HttpServletRequest request) {
		T attr = removeAttributeFromSession(attrName, type, request);
		request.setAttribute(attrName, attr);
		return attr;
    }
    
    private AuthenticationInfo getAuthInfoFromUser(OpenIdUser user) {
    	String jcrId = OpenIDUserUtil.getPrincipalName(user.getIdentity());

    	SimpleCredentials creds = new SimpleCredentials(jcrId,new char[0]);
    	creds.setAttribute(getClass().getName(), user);
        return new AuthenticationInfo(OpenIDConstants.OPEN_ID_AUTH_TYPE, creds);
    }

	public boolean canHandle(Credentials credentials) {
		if(credentials != null && credentials instanceof SimpleCredentials) {
			SimpleCredentials sc = (SimpleCredentials)credentials;
			OpenIdUser user = (OpenIdUser)sc.getAttribute(getClass().getName());
			if(user != null) {
				return user.isAssociated();
			}
		}
		return false;
	}

	public void doInit(CallbackHandler callbackHandler, Session session,
			Map options) throws LoginException {
		return;
	}

	public AuthenticationPlugin getAuthentication(Principal principal,
			Credentials creds) throws RepositoryException {
		return new OpenIDAuthenticationPlugin(principal);
	}

	public Principal getPrincipal(Credentials credentials) {
		if(credentials != null && credentials instanceof SimpleCredentials) {
			SimpleCredentials sc = (SimpleCredentials)credentials;
			OpenIdUser user = (OpenIdUser)sc.getAttribute(getClass().getName());
			if(user != null) {
				return new OpenIDPrincipal(user);
			}
		}
		return null;
	}

	public int impersonate(Principal principal, Credentials credentials)
			throws RepositoryException, FailedLoginException {
		return LoginModulePlugin.IMPERSONATION_DEFAULT;
	}

}
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
package org.apache.sling.auth.xing.login.impl;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.auth.core.AuthConstants;
import org.apache.sling.auth.core.AuthUtil;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.apache.sling.auth.xing.login.XingLogin;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    label = "Apache Sling Authentication XING Login “Authentication Handler”",
    description = "Authentication Handler for Sling Authentication XING Login",
    immediate = true,
    metatype = true
)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = XingLogin.SERVICE_VENDOR),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Authentication Handler for Sling Authentication XING Login"),
    @Property(name = Constants.SERVICE_RANKING, intValue = 0, propertyPrivate = false),
    @Property(name = AuthenticationHandler.PATH_PROPERTY, value = "/", unbounded = PropertyUnbounded.ARRAY),
    @Property(name = AuthenticationHandler.TYPE_PROPERTY, value = XingLogin.AUTH_TYPE, propertyPrivate = true)
})
public class XingLoginAuthenticationHandler extends DefaultAuthenticationFeedbackHandler implements AuthenticationHandler {

    private String consumerKey;

    private String xingCookie;

    private String userCookie;

    private String userIdCookie;

    private int maxCookieSize;

    private String loginPath;

    private String logoutPath;

    private ServiceRegistration loginPageRegistration;

    private static final String DEFAULT_USER_COOKIE = "sling_auth_xing_user";

    private static final String DEFAULT_USERID_COOKIE = "sling_auth_xing_userid";

    private static final int DEFAULT_MAX_COOKIE_SIZE = 4096;

    @Property(value = "")
    private static final String CONSUMER_KEY_PARAMETER = "org.apache.sling.auth.xing.login.impl.XingLoginAuthenticationHandler.consumerKey";

    @Property(value = DEFAULT_USER_COOKIE)
    private static final String USER_COOKIE_PARAMETER = "org.apache.sling.auth.xing.login.impl.XingLoginAuthenticationHandler.cookie.user";

    @Property(value = DEFAULT_USERID_COOKIE)
    private static final String USERID_COOKIE_PARAMETER = "org.apache.sling.auth.xing.login.impl.XingLoginAuthenticationHandler.cookie.userid";

    @Property(intValue = DEFAULT_MAX_COOKIE_SIZE)
    private static final String MAX_COOKIE_SIZE_PARAMETER = "org.apache.sling.auth.xing.login.impl.XingLoginAuthenticationHandler.cookie.maxSize";

    @Property
    private static final String LOGIN_PATH_PARAMETER = "org.apache.sling.auth.xing.login.impl.XingLoginAuthenticationHandler.login.path";

    @Property
    private static final String LOGOUT_PATH_PARAMETER = "org.apache.sling.auth.xing.login.impl.XingLoginAuthenticationHandler.logout.path";

    private final Logger logger = LoggerFactory.getLogger(XingLoginAuthenticationHandler.class);

    public XingLoginAuthenticationHandler() {
    }

    @Activate
    protected void activate(final ComponentContext ComponentContext) {
        logger.debug("activate");
        configure(ComponentContext);
    }

    @Modified
    protected void modified(final ComponentContext ComponentContext) {
        logger.debug("modified");
        configure(ComponentContext);
    }

    @Deactivate
    protected void deactivate(final ComponentContext ComponentContext) {
        logger.debug("deactivate");
        if (loginPageRegistration != null) {
            loginPageRegistration.unregister();
            loginPageRegistration = null;
        }
    }

    protected void configure(final ComponentContext context) {
        final Dictionary properties = context.getProperties();
        consumerKey = PropertiesUtil.toString(properties.get(CONSUMER_KEY_PARAMETER), "").trim();
        userCookie = PropertiesUtil.toString(properties.get(USER_COOKIE_PARAMETER), DEFAULT_USER_COOKIE).trim();
        userIdCookie = PropertiesUtil.toString(properties.get(USERID_COOKIE_PARAMETER), DEFAULT_USERID_COOKIE).trim();
        maxCookieSize = PropertiesUtil.toInteger(properties.get(MAX_COOKIE_SIZE_PARAMETER), DEFAULT_MAX_COOKIE_SIZE);
        loginPath = PropertiesUtil.toString(properties.get(LOGIN_PATH_PARAMETER), "").trim();
        logoutPath = PropertiesUtil.toString(properties.get(LOGOUT_PATH_PARAMETER), "").trim();

        if (StringUtils.isEmpty(consumerKey)) {
            logger.warn("configured consumer key is empty");
            xingCookie = "";
        } else {
            xingCookie = XingLogin.XING_COOKIE_PREFIX.concat(consumerKey);
        }

        if (loginPageRegistration != null) {
            loginPageRegistration.unregister();
        }

        if (StringUtils.isEmpty(loginPath)) {
            logger.warn("configured login path is empty");
        } else {
            final Dictionary<String, Object> loginPathProperties = new Hashtable<String, Object>();
            final String[] authRequirements = new String[]{"-".concat(loginPath)};
            loginPathProperties.put(AuthConstants.AUTH_REQUIREMENTS, authRequirements);
            loginPageRegistration = context.getBundleContext().registerService(Object.class.getName(), new Object(), loginPathProperties);
        }

        if (StringUtils.isEmpty(logoutPath)) {
            logger.warn("configured logout path is empty");
        }

        logger.info("configured with consumer key '{}', cookie name '{}', login path '{}' and logout path '{}'", consumerKey, xingCookie, loginPath, logoutPath);
    }

    /**
     * we need the <i>hash</i> from the XING cookie (<code>xing_p_lw_s_[...]</code>) and
     * the <i>user data</i> and <i>id</i> from our own cookies (<code>sling_auth_xing_[...]</code>)
     *
     * @param request
     * @param response
     * @return
     */
    @Override
    public AuthenticationInfo extractCredentials(final HttpServletRequest request, final HttpServletResponse response) {
        logger.debug("extract credentials");

        String hash = null;
        String user = null;
        String userId = null;

        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                final String cookieName = cookie.getName();
                if (cookieName.equals(xingCookie)) {
                    hash = readCookieValue(cookie);
                    logger.debug("“Login with XING” cookie found: {}", hash);
                } else if (cookieName.equals(userCookie)) {
                    user = readCookieValue(cookie);
                } else if (cookieName.equals(userIdCookie)) {
                    userId = readCookieValue(cookie);
                }
            }
        }

        if (!StringUtils.isEmpty(hash) && !StringUtils.isEmpty(userId) && !StringUtils.isEmpty(user)) {
            logger.debug("valid cookies with hash and user data and id found");
            final AuthenticationInfo authenticationInfo = new AuthenticationInfo(XingLogin.AUTH_TYPE, userId);
            authenticationInfo.put(XingLogin.AUTHENTICATION_CREDENTIALS_HASH_KEY, hash);
            authenticationInfo.put(XingLogin.AUTHENTICATION_CREDENTIALS_USERDATA_KEY, user);
            return authenticationInfo;
        } else {
            logger.debug("unable to extract credentials from request");
            return null;
        }
    }

    @Override
    public boolean requestCredentials(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        logger.debug("request credentials");
        if (!loginPath.equals(request.getServletPath())) {
            final String target = request.getContextPath().concat(loginPath);
            logger.debug("redirecting to '{}'", target);
            AuthUtil.sendRedirect(request, response, target, null);
        }
        return true;
    }

    @Override
    public void dropCredentials(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        logger.debug("drop credentials");
        // delete cookies
        deleteCookies(request, response);
        // redirect and call JavaScript xing.logout()
        final HashMap<String, String> params = new HashMap<String, String>();
        logger.debug("redirecting to '{}'", logoutPath);
        AuthUtil.sendRedirect(request, response, logoutPath, params);
    }

    protected String readCookieValue(final Cookie cookie) {
        if (cookie.getValue() != null) {
            if (cookie.getValue().length() > maxCookieSize) {
                logger.warn("size of cookie value greater than configured max. cookie size of {}", maxCookieSize);
            } else {
                return cookie.getValue();
            }
        }
        return null;
    }

    protected void deleteCookies(final HttpServletRequest request, final HttpServletResponse response) {
        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                final String name = cookie.getName();
                logger.debug("cookie found: '{}'", name);
                if (name.equals(xingCookie) || name.equals(userCookie) || name.equals(userIdCookie)) {
                    logger.debug("deleting cookie '{}' with value '{}'", cookie.getName(), cookie.getValue());
                    cookie.setValue(null);
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

}

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
package org.apache.sling.auth.xing.oauth.impl;

import java.io.IOException;
import java.util.Dictionary;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.apache.sling.auth.xing.api.XingUser;
import org.apache.sling.auth.xing.api.users.Users;
import org.apache.sling.auth.xing.oauth.XingOauth;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.XingApi;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    label = "Apache Sling Authentication XING OAuth “Authentication Handler”",
    description = "Authentication Handler for Sling Authentication XING OAuth",
    immediate = true,
    metatype = true
)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = XingOauth.SERVICE_VENDOR),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Authentication Handler for Sling Authentication XING OAuth"),
    @Property(name = Constants.SERVICE_RANKING, intValue = 0, propertyPrivate = false),
    @Property(name = AuthenticationHandler.PATH_PROPERTY, value = "/", unbounded = PropertyUnbounded.ARRAY),
    @Property(name = AuthenticationHandler.TYPE_PROPERTY, value = XingOauth.AUTH_TYPE, propertyPrivate = true)
})
public class XingOauthAuthenticationHandler extends DefaultAuthenticationFeedbackHandler implements AuthenticationHandler {

    private OAuthService oAuthService;

    private String consumerKey;

    private String consumerSecret;

    private String callbackUrl;

    private String usersMeUrl;

    private static final String DEFAULT_USERS_ME_URL = "https://api.xing.com/v1/users/me.json";

    @Property(value = "")
    private static final String CONSUMER_KEY_PARAMETER = "org.apache.sling.auth.xing.oauth.impl.XingOauthAuthenticationHandler.consumerKey";

    @Property(value = "")
    private static final String CONSUMER_SECRET_PARAMETER = "org.apache.sling.auth.xing.oauth.impl.XingOauthAuthenticationHandler.consumerSecret";

    @Property(value = "")
    private static final String CALLBACK_URL_PARAMETER = "org.apache.sling.auth.xing.oauth.impl.XingOauthAuthenticationHandler.callbackUrl";

    @Property(value = DEFAULT_USERS_ME_URL)
    private static final String USERS_ME_URL_PARAMETER = "org.apache.sling.auth.xing.oauth.impl.XingOauthAuthenticationHandler.usersMeUrl";

    public static final String USER_SESSION_ATTRIBUTE_NAME = "xing-user";

    private final Logger logger = LoggerFactory.getLogger(XingOauthAuthenticationHandler.class);

    public XingOauthAuthenticationHandler() {
    }

    @Activate
    protected void activate(final ComponentContext componentContext) {
        logger.debug("activate");
        configure(componentContext);
    }

    @Modified
    protected void modified(final ComponentContext componentContext) {
        logger.debug("modified");
        configure(componentContext);
    }

    @Deactivate
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug("deactivate");
    }

    protected synchronized void configure(final ComponentContext componentContext) {
        final Dictionary properties = componentContext.getProperties();
        consumerKey = PropertiesUtil.toString(properties.get(CONSUMER_KEY_PARAMETER), "").trim();
        consumerSecret = PropertiesUtil.toString(properties.get(CONSUMER_SECRET_PARAMETER), "").trim();
        callbackUrl = PropertiesUtil.toString(properties.get(CALLBACK_URL_PARAMETER), "").trim();
        usersMeUrl = PropertiesUtil.toString(properties.get(USERS_ME_URL_PARAMETER), DEFAULT_USERS_ME_URL).trim();

        if (StringUtils.isEmpty(consumerKey)) {
            logger.warn("configured consumer key is empty");
        }

        if (StringUtils.isEmpty(consumerSecret)) {
            logger.warn("configured consumer secret is empty");
        }

        if (StringUtils.isEmpty(callbackUrl)) {
            logger.warn("configured callback URL is empty");
        }

        if (StringUtils.isEmpty(usersMeUrl)) {
            logger.warn("configured users me URL is empty");
        }

        if (!StringUtils.isEmpty(consumerKey) && !StringUtils.isEmpty(consumerSecret) && !StringUtils.isEmpty(callbackUrl)) {
            oAuthService = new ServiceBuilder().provider(XingApi.class).apiKey(consumerKey).apiSecret(consumerSecret).callback(callbackUrl).build();
        }

        logger.info("configured with consumer key '{}', callback url '{}' and users me url '{}'", consumerKey, callbackUrl, usersMeUrl);
    }

    // we need the OAuth access token and the user from XING (/v1/users/me)
    @Override
    public AuthenticationInfo extractCredentials(final HttpServletRequest request, final HttpServletResponse response) {
        logger.debug("extract credentials");

        try {
            final HttpSession httpSession = request.getSession(true);

            Token accessToken = (Token) httpSession.getAttribute(OAuthConstants.ACCESS_TOKEN);
            XingUser xingUser = (XingUser) httpSession.getAttribute(USER_SESSION_ATTRIBUTE_NAME);

            if (accessToken == null) {
                // we need the request token and verifier to get an access token
                final Token requestToken = (Token) httpSession.getAttribute(OAuthConstants.TOKEN);
                final String verifier = request.getParameter(OAuthConstants.VERIFIER);
                if (requestToken == null || verifier == null) {
                    return null;
                }
                accessToken = oAuthService.getAccessToken(requestToken, new Verifier(verifier));
                logger.debug("access token: {}", accessToken);
                httpSession.setAttribute(OAuthConstants.ACCESS_TOKEN, accessToken);
            }

            if (xingUser == null) {
                xingUser = fetchUser(accessToken);
                logger.debug("xing user: {}", xingUser);
                httpSession.setAttribute(USER_SESSION_ATTRIBUTE_NAME, xingUser);
            }

            final AuthenticationInfo authenticationInfo = new AuthenticationInfo(XingOauth.AUTH_TYPE, xingUser.getId());
            authenticationInfo.put(XingOauth.AUTHENTICATION_CREDENTIALS_ACCESS_TOKEN_KEY, accessToken);
            authenticationInfo.put(XingOauth.AUTHENTICATION_CREDENTIALS_USER_KEY, xingUser);
            return authenticationInfo;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            removeAuthFromSession(request);
            return null;
        }
    }

    @Override
    public boolean requestCredentials(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        logger.debug("request credentials");
        try {
            final Token requestToken = oAuthService.getRequestToken();
            logger.debug("received request token: '{}'", requestToken);
            final HttpSession httpSession = request.getSession(true);
            httpSession.setAttribute(OAuthConstants.TOKEN, requestToken);
            final String authUrl = oAuthService.getAuthorizationUrl(requestToken);
            logger.debug("redirecting to auth url: '{}'", authUrl);
            response.sendRedirect(authUrl);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void dropCredentials(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        logger.debug("drop credentials");
        removeAuthFromSession(request);
    }

    protected XingUser fetchUser(final Token accessToken) throws Exception {
        final OAuthRequest request = new OAuthRequest(Verb.GET, usersMeUrl);
        oAuthService.signRequest(accessToken, request);
        final Response response = request.send();
        final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        final Users users = gson.fromJson(response.getBody(), Users.class);
        return users.getUsers().get(0);
    }

    protected void removeAuthFromSession(final HttpServletRequest request) {
        try {
            final HttpSession httpSession = request.getSession();
            httpSession.removeAttribute(OAuthConstants.TOKEN);
            httpSession.removeAttribute(OAuthConstants.ACCESS_TOKEN);
            httpSession.removeAttribute(USER_SESSION_ATTRIBUTE_NAME);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}

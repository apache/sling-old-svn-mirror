/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.clients;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class SlingClientConfig {

    /**
     * Base URI of the server under test.
     */
    protected final URI url;

    /**
     * Name of the user that will be used to authenticate the requests.
     */
    protected final String user;

    /**
     * Password of the user that will be used to authenticate the requests.
     */
    protected final String password;

    /**
     * The cookie store
     */
    protected final CookieStore cookieStore;

    /**
     * The credentials provider
     */
    protected final CredentialsProvider credsProvider;

    /**
     * AuthCache for preemptive auth
     */
    protected final AuthCache authCache;

    /**
     * Extra values to be used in interceptors, custom auth mechanisms, etc.
     */
    protected final Map<String, String> values;

    protected SlingClientConfig(URI url, String user, String password,
                                CookieStore cookieStore,
                                CredentialsProvider credentialsProvider, AuthCache authCache) {
        this.url = url;
        this.user = user;
        this.password = password;

        this.cookieStore = cookieStore;
        this.credsProvider = credentialsProvider;
        this.authCache = authCache;

        this.values = new ConcurrentHashMap<String, String>();
    }

    /**
     * @return the base URL that the sling client is pointing to. It should always end with a "/"
     */
    public URI getUrl() {
        return url;
    }

    /**
     * @return the user that the client is using.
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the user that the client is using.
     */
    public String getPassword() {
        return password;
    }

    /**
     * <p>Get the map of extra custom values configured on the client</p>
     * <p>These may be used by interceptors, for example</p>
     *
     * @return the reference to the map
     */
    public Map<String, String> getValues() {
        return values;
    }

    /**
     * @return a reference to the cookie store used by the client
     */
    public CookieStore getCookieStore() {
        return cookieStore;
    }

    /**
     * @return the reference to the CredentialsProvider used by the client
     */
    public CredentialsProvider getCredsProvider() {
        return credsProvider;
    }

    /**
     * @return the reference the AuthCache used by the client
     */
    public AuthCache getAuthCache() {
        return authCache;
    }

    public static class Builder {
        protected URI url;

        protected String user;

        protected String password;

        protected CookieStore cookieStore;

        protected CredentialsProvider credsProvider;

        protected AuthCache authCache;

        protected Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder setUrl(String url) throws URISyntaxException {
            return setUrl(new URI(url));
        }

        public Builder setUrl(URI url) {
            this.url = url;
            // Add / as path if none is present
            if (Strings.isNullOrEmpty(this.url.getPath()) || !this.url.getPath().endsWith("/")) {
                this.url = this.url.resolve(Strings.nullToEmpty(this.url.getPath()) + "/");
            }
            return this;
        }

        public Builder setUser(String user) {
            this.user = user;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setCredentialsProvider(CredentialsProvider credsProvider) {
            this.credsProvider = credsProvider;
            return this;
        }

        public Builder setAuthCache(AuthCache authCache) {
            this.authCache = authCache;
            return this;
        }

        public Builder setCookieStore(CookieStore cookieStore) {
            this.cookieStore = cookieStore;
            return this;
        }

        public SlingClientConfig build() {
            // Create default CredentialsProvider if not set
            if (credsProvider == null) {
                credsProvider = new BasicCredentialsProvider();
                if (StringUtils.isNotEmpty(this.user)) {
                    HttpHost targetHost = URIUtils.extractHost(this.url);
                    credsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                            new UsernamePasswordCredentials(this.user, this.password));
                }
            }

            // Create default AuthCache if not set
            if (authCache == null) {
                BasicScheme basicScheme = new BasicScheme();
                authCache = new BasicAuthCache();
                authCache.put(URIUtils.extractHost(url), basicScheme);
            }

            // Create default CookieStore if not set
            if (cookieStore == null) {
                cookieStore = new BasicCookieStore();
            }

            return new SlingClientConfig(url, user, password, cookieStore, credsProvider, authCache);
        }
    }
}

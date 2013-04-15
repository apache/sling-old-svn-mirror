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
package org.apache.sling.ftpserver.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

/**
 * The <code>SlingUserManager</code> is the FTP server's user manager based on
 * the Jackrabbit Repository user manager.
 * <p>
 * This implementation is read-only in that the {@link #save(User)} and
 * {@link #delete(String)} methods throw an
 * {@code UnsupportedOperationException}.
 * <p>
 * Limitations of the users can be configured globally applying to all users
 * or on a per-user basis using the following properties:
 * <table>
 * <tr><th>
 */
public class SlingUserManager implements UserManager {

    private static final String FTP_PROPERTY_FOLDER = "ftp";

    static final String FTP_ENABLED = FTP_PROPERTY_FOLDER + "/enabled";

    static final String FTP_MAX_IDLE_TIME_SEC = FTP_PROPERTY_FOLDER + "/maxIdleTimeSec";

    static final String FTP_MAX_CONCURRENT = FTP_PROPERTY_FOLDER + "/maxConcurrent";

    static final String FTP_MAX_CONCURRENT_PER_IP = FTP_PROPERTY_FOLDER + "/maxConcurrentPerIp";

    static final String FTP_MAX_DOWNLOAD_RATE = FTP_PROPERTY_FOLDER + "/maxDownloadRate";

    static final String FTP_MAX_UPLOAD_RATE = FTP_PROPERTY_FOLDER + "/maxUploadRate";

    private final ResourceResolverFactory rrFactory;

    private final SlingConfiguration config;

    SlingUserManager(final ResourceResolverFactory rrFactory, final SlingConfiguration config) {
        this.rrFactory = rrFactory;
        this.config = config;
    }

    public User authenticate(Authentication auth) throws AuthenticationFailedException {
        if (auth == null) {
            throw new AuthenticationFailedException("Missing Authentication");
        }

        final ResourceResolver resolver;
        try {
            if (auth instanceof AnonymousAuthentication) {
                resolver = this.rrFactory.getResourceResolver(null);
            } else if (auth instanceof UsernamePasswordAuthentication) {
                final Map<String, Object> creds = new HashMap<String, Object>();
                creds.put(ResourceResolverFactory.USER, ((UsernamePasswordAuthentication) auth).getUsername());
                creds.put(ResourceResolverFactory.PASSWORD,
                    ((UsernamePasswordAuthentication) auth).getPassword().toCharArray());
                resolver = this.rrFactory.getResourceResolver(creds);
            } else {
                resolver = null;
            }

            if (resolver != null) {
                return createUser(resolver.getUserID(),
                    resolver.adaptTo(org.apache.jackrabbit.api.security.user.User.class), resolver);
            }
        } catch (LoginException e) {
            throw new AuthenticationFailedException("Cannot login user " + auth, e);
        }

        // unsupported authentication
        throw new AuthenticationFailedException("Authentication of type " + auth.getClass() + " not supported");
    }

    public String getAdminName() throws FtpException {
        final ResourceResolver admin = this.getAdminResolver();
        try {
            return admin.getUserID();
        } finally {
            admin.close();
        }
    }

    public boolean isAdmin(String id) throws FtpException {
        return id != null && id.equals(getAdminName());
    }

    public boolean doesExist(String name) throws FtpException {
        return getRepoUser(name) != null;
    }

    public User getUserByName(String name) throws FtpException {
        org.apache.jackrabbit.api.security.user.User repoUser = getRepoUser(name);
        return (repoUser != null) ? createUser(name, repoUser, null) : null;
    }

    public String[] getAllUserNames() throws FtpException {
        final ResourceResolver admin = this.getAdminResolver();
        try {
            org.apache.jackrabbit.api.security.user.UserManager um = admin.adaptTo(org.apache.jackrabbit.api.security.user.UserManager.class);
            if (um != null) {
                try {
                    Iterator<Authorizable> ai = um.findAuthorizables(null, null,
                        org.apache.jackrabbit.api.security.user.UserManager.SEARCH_TYPE_USER);
                    ArrayList<String> list = new ArrayList<String>();
                    while (ai.hasNext()) {
                        list.add(ai.next().getID());
                    }
                    return list.toArray(new String[list.size()]);
                } catch (RepositoryException e) {
                    throw new FtpException(e);
                }
            }
        } finally {
            admin.close();
        }

        // no user managemnent or problem accessing them
        throw new FtpException("Cannot get users");
    }

    public void delete(String name) {
        throw new UnsupportedOperationException("delete");
    }

    public void save(User user) {
        throw new UnsupportedOperationException("save");
    }

    /**
     * Returns the repository user of the given name or {@code null} if no such
     * user exists or if a group of that name exists.
     *
     * @param name The name of the user to retrieve
     * @return The repository user or {@code null} if no such user exists or if
     *         a group of that name exists.
     * @throws FtpException If the repository user manager cannot be retrieved
     *             or an error occurrs looking for the user
     */
    private org.apache.jackrabbit.api.security.user.User getRepoUser(final String name) throws FtpException {
        final ResourceResolver admin = this.getAdminResolver();
        try {
            org.apache.jackrabbit.api.security.user.UserManager um = admin.adaptTo(org.apache.jackrabbit.api.security.user.UserManager.class);
            if (um != null) {
                Authorizable a;
                try {
                    a = um.getAuthorizable(name);
                    return (a != null && !a.isGroup()) ? (org.apache.jackrabbit.api.security.user.User) a : null;
                } catch (RepositoryException e) {
                    throw new FtpException(e);
                }
            }
        } finally {
            admin.close();
        }

        throw new FtpException("Missing internal user manager; cannot find user");
    }

    /**
     * Returns an administative {@code ResourceResolver}.
     * <p>
     * This resource resolver must be closed when not used any more to prevent
     * memory and performance leaks.
     *
     * @return an administrative resource resolver
     * @throws FtpException If the resource resolver cannot be created
     */
    private ResourceResolver getAdminResolver() throws FtpException {
        try {
            return this.rrFactory.getAdministrativeResourceResolver(null);
        } catch (LoginException e) {
            throw new FtpException("Cannot create administrative ResourceResolver", e);
        }
    }

    private User createUser(final String name, final org.apache.jackrabbit.api.security.user.User repoUser,
            final ResourceResolver resolver) {
        SlingUser user = new SlingUser(name, resolver);

        user.setEnabled(getProperty(repoUser, FTP_ENABLED, config.isEnabled()));
        user.setMaxIdleTimeSec(getProperty(repoUser, FTP_MAX_IDLE_TIME_SEC, config.getMaxIdelTimeSec()));

        List<Authority> list = new ArrayList<Authority>();
        list.add(new ConcurrentLoginPermission(//
            getProperty(repoUser, FTP_MAX_CONCURRENT, config.getMaxConcurrent()),//
            getProperty(repoUser, FTP_MAX_CONCURRENT_PER_IP, config.getMaxConcurrentPerIp())));
        list.add(new TransferRatePermission(//
            getProperty(repoUser, FTP_MAX_DOWNLOAD_RATE, config.getMaxDownloadRate()),//
            getProperty(repoUser, FTP_MAX_UPLOAD_RATE, config.getMaxUploadRate())));
        list.add(new SlingWritePermission(resolver));

        user.setAuthorities(list);

        return user;
    }

    private int getProperty(final Authorizable a, final String prop, final int defaultValue) {
        try {
            Value[] vals = a.getProperty(prop);
            if (vals != null && vals.length > 0) {
                return (int) vals[0].getLong();
            }
        } catch (RepositoryException re) {
            // ignore
        }

        return defaultValue;
    }

    private boolean getProperty(final Authorizable a, final String prop, final boolean defaultValue) {
        try {
            Value[] vals = a.getProperty(prop);
            if (vals != null && vals.length > 0) {
                return vals[0].getBoolean();
            }
        } catch (RepositoryException re) {
            // ignore
        }

        return defaultValue;
    }
}

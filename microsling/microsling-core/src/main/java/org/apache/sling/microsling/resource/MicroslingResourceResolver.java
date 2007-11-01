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
package org.apache.sling.microsling.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The microsling ResourceResolver locates a Resource using a
 * ResourcePathIterator: it first tries to find an exact match for the Request
 * URI, and if not goes up the path, breaking it a "." and "/", stopping if it
 * finds a Resource.
 */
public class MicroslingResourceResolver implements ResourceResolver {

    private final Logger log = LoggerFactory.getLogger(MicroslingResourceResolver.class);

    private Session session;

    public MicroslingResourceResolver(Session session) {
        this.session = session;
    }

    public void dispose() {
        if (session != null) {
            try {
                session.logout();
            } catch (Throwable t) {
                log.warn("dispose: Unexpected problem logging out", t);
            }

            session = null;
        }
    }

    public Resource resolve(ServletRequest request) throws SlingException,
            ResourceNotFoundException {
        Resource result = null;
        String path = null;
        String pathInfo = ((HttpServletRequest) request).getPathInfo();
        try {
            Session session = getSession();
            final ResourcePathIterator it = new ResourcePathIterator(pathInfo);
            while (it.hasNext() && result == null) {
                path = it.next();
                if (log.isDebugEnabled()) {
                    log.debug("Trying to locate Resource at path '" + path
                        + "'");
                }
                result = getResource(session, path);
            }
        } catch (RepositoryException re) {
            throw new SlingException("RepositoryException for path=" + path, re);
        }

        if (result == null) {
            result = new NonExistingResource(pathInfo);
        }

        return result;
    }

    public Resource getResource(Resource base, String path)
            throws SlingException {
        // TODO Auto-generated method stub
        return null;
    }

    public Resource getResource(String path) throws SlingException {
        try {
            Session session = getSession();
            if (session.itemExists(path)) {
                return getResource(session, path);
            }

            log.info("Path '{}' does not resolve to an Item", path);
            return null;
        } catch (RepositoryException re) {
            throw new SlingException("Cannot get resource " + path, re);
        }
    }

    public Iterator<Resource> listChildren(Resource parent)
            throws SlingException {
        // TODO Auto-generated method stub
        return null;
    }

    public Iterator<Map<String, Object>> queryResources(String query,
            String language) throws SlingException {
        try {
            QueryResult result = queryInternal(query, language);
            final String[] colNames = result.getColumnNames();
            final RowIterator rows = result.getRows();
            return new Iterator<Map<String, Object>>() {
                public boolean hasNext() {
                    return rows.hasNext();
                };

                public Map<String, Object> next() {
                    Map<String, Object> row = new HashMap<String, Object>();
                    try {
                        Value[] values = rows.nextRow().getValues();
                        for (int i = 0; i < values.length; i++) {
                            row.put(colNames[i], toJavaObject(values[i]));
                        }
                    } catch (RepositoryException re) {
                        // TODO:log
                    }
                    return row;
                }

                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
        } catch (RepositoryException re) {
            throw new SlingException(re);
        }
    }

    public Iterator<Resource> findResources(String query, String language)
            throws SlingException {
        try {
            final NodeIterator ni = queryInternal(query, language).getNodes();
            return new Iterator<Resource>() {
                public boolean hasNext() {
                    return ni.hasNext();
                }

                public Resource next() {
                    try {
                        return new JcrNodeResource(ni.nextNode());
                    } catch (RepositoryException re) {
                        // log and return next, but not null
                        return null;
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
        } catch (RepositoryException re) {
            throw new SlingException(re);
        }
    }

    protected Session getSession() throws SlingException {
        if (session != null && session.isLive()) {
            return session;
        }

        throw new SlingException("Session has already been closed");
    }

    protected Resource getResource(Session session, String path)
            throws RepositoryException {
        if (session.itemExists(path)) {
            Resource result = new JcrNodeResource(session, path);
            result.getResourceMetadata().put(ResourceMetadata.RESOLUTION_PATH,
                path);
            log.info("Found Resource at path '{}'", path);
            return result;
        }

        return null;
    }

    private QueryResult queryInternal(String query, String language)
            throws RepositoryException, SlingException {
        Session s = getSession();
        Query q = s.getWorkspace().getQueryManager().createQuery(query,
            language);
        return q.execute();
    }

    private Object toJavaObject(Value value) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.BINARY:
                return new LazyInputStream(value);
            case PropertyType.BOOLEAN:
                return value.getBoolean();
            case PropertyType.DATE:
                return value.getDate();
            case PropertyType.DOUBLE:
                return value.getDouble();
            case PropertyType.LONG:
                return value.getLong();
            case PropertyType.NAME: // fall through
            case PropertyType.PATH: // fall through
            case PropertyType.REFERENCE: // fall through
            case PropertyType.STRING: // fall through
            case PropertyType.UNDEFINED: // not actually expected
            default: // not actually expected
                return value.getString();
        }
    }

    private static class LazyInputStream extends InputStream {

        private final Value value;

        private InputStream delegatee;

        LazyInputStream(Value value) {
            this.value = value;
        }

        private InputStream getStream() throws IOException {
            if (delegatee == null) {
                try {
                    delegatee = value.getStream();
                } catch (RepositoryException re) {
                    throw (IOException) new IOException(re.getMessage()).initCause(re);
                }
            }
            return delegatee;
        }

        @Override
        public void close() throws IOException {
            if (delegatee != null) {
                delegatee.close();
            }
        }

        @Override
        public int available() throws IOException {
            return getStream().available();
        }

        @Override
        public int read() throws IOException {
            return getStream().read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return getStream().read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return getStream().read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return getStream().skip(n);
        }

        @Override
        public boolean markSupported() {
            try {
                return getStream().markSupported();
            } catch (IOException ioe) {
                // TODO: log
            }
            return false;
        }

        @Override
        public synchronized void mark(int readlimit) {
            try {
                getStream().mark(readlimit);
            } catch (IOException ioe) {
                // TODO: log
            }
        }

        @Override
        public synchronized void reset() throws IOException {
            getStream().reset();
        }
    }

}

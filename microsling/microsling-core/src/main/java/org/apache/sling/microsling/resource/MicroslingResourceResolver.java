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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
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

    /**
     * Resolves the Resource from the request
     */
    public Resource resolve(ServletRequest request) throws SlingException {
        Resource result = null;
        String path = null;
        String pathInfo = ((HttpServletRequest) request).getPathInfo();
        try {
            Session session = getSession();
            final ResourcePathIterator it = new ResourcePathIterator(pathInfo);
            while (it.hasNext() && result == null) {
                result = getResource(session, it.next());
            }
        } catch (RepositoryException re) {
            throw new SlingException("RepositoryException for path=" + path, re);
        }

        if (result == null) {
            result = new NonExistingResource(pathInfo);
        }

        return result;
    }

    /**
     * Resolves a resource relative to the given base resource
     */
    public Resource getResource(Resource base, String path)
            throws SlingException {

        // special case of absolute paths
        if (path.startsWith("/")) {
            return getResource(path);
        }

        // resolve relative path segments now
        path = resolveRelativeSegments(path);
        if (path != null) {
            if (path.length() == 0) {
                // return the base resource
                return base;
            } else if (base.getRawData() instanceof Node) {
                try {
                    Node baseNode = (Node) base.getRawData();
                    if (baseNode.hasNode(path)) {
                        return new JcrNodeResource(baseNode.getNode(path));
                    }

                    log.error("getResource: There is no node at {} below {}",
                        path, base.getURI());
                    return null;
                } catch (RepositoryException re) {
                    log.error(
                        "getResource: Problem accessing relative resource at "
                            + path, re);
                    return null;
                }
            }
        }

        // try (again) with absolute resource path
        path = base.getURI() + "/" + path;
        return getResource(path);
    }

    /**
     * Resolves a resource with an absolute path
     */
    public Resource getResource(String path) throws SlingException {

        path = resolveRelativeSegments(path);
        if (path != null) {
            try {
                return getResource(getSession(), path);
            } catch (RepositoryException re) {
                throw new SlingException("Cannot get resource " + path, re);
            }
        }

        // relative path segments cannot be resolved
        return null;
    }

    /**
     * Find all child resources of the given parent resource
     */
    public Iterator<Resource> listChildren(final Resource parent)
            throws SlingException {
        if (parent.getRawData() instanceof Node) {

            try {
                final NodeIterator children = ((Node) parent.getRawData()).getNodes();
                return new Iterator<Resource>() {

                    public boolean hasNext() {
                        return children.hasNext();
                    }

                    public Resource next() {
                        try {
                            return new JcrNodeResource(children.nextNode());
                        } catch (RepositoryException re) {
                            log.warn(
                                "Problem while trying to create a resource", re);
                            return new NonExistingResource(parent.getURI()
                                + "/?");
                        }
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("remove");
                    }

                };
            } catch (RepositoryException re) {
                throw new SlingException("Cannot get children of Resource "
                    + parent, re);
            }
        }

        // return an empty iterator if parent has no node
        List<Resource> empty = Collections.emptyList();
        return empty.iterator();
    }

    /**
     * Query the JCR repository and return an iterator of query results
     */
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

    /**
     * Find all resources matching the given query
     */
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

    /** Returns the session used by this resolver */
    protected Session getSession() throws SlingException {
        if (session != null && session.isLive()) {
            return session;
        }

        throw new SlingException("Session has already been closed");
    }

    /** Creates a JcrNodeResource with the given path if existing */
    protected Resource getResource(Session session, String path)
            throws RepositoryException {
        if (session.itemExists(path)) {
            Resource result = new JcrNodeResource(session, path);
            result.getResourceMetadata().put(ResourceMetadata.RESOLUTION_PATH,
                path);
            log.info("Found Resource at path '{}'", path);
            return result;
        }

        log.info("Path '{}' does not resolve to an Item", path);
        return null;
    }

    /**
     * Resolves relative path segments '.' and '..' in the absolute path.
     * Returns null if not possible (.. points above root) or if path is not
     * absolute.
     */
    protected String resolveRelativeSegments(String path) {

        // don't care for empty paths
        if (path.length() == 0) {
            log.error("resolveRelativeSegments: Not modifying empty path");
            return path;
        }

        // prepare the path buffer with trailing slash (simplifies impl)
        int absOffset = (path.charAt(0) == '/') ? 0 : 1;
        char[] buf= new char[path.length() + 1 + absOffset];
        if (absOffset == 1) {
            buf[0] = '/';
        }
        path.getChars(0, path.length(), buf, absOffset);
        buf[buf.length - 1] = '/';

        int lastSlash = 0; // last slash in path
        int numDots = 0; // number of consecutive dots after last slash

        int bufPos = 0;
        for (int bufIdx = lastSlash; bufIdx < buf.length; bufIdx++) {
            char c = buf[bufIdx];
            if (c == '/') {
                if (numDots == 2) {
                    if (bufPos == 0) {
                        log.error("resolveRelativeSegments: Path '{}' cannot be resolved", path);
                        return null;
                    }

                    do {
                        bufPos--;
                    } while (bufPos > 0 && buf[bufPos] != '/');
                }

                lastSlash = bufIdx;
                numDots = 0;
            } else if (c == '.' && numDots < 2) {
                numDots++;
            } else {
                // find the next slash
                int nextSlash = bufIdx + 1;
                while (nextSlash < buf.length && buf[nextSlash] != '/') {
                    nextSlash++;
                }

                // append up to the next slash (or end of path)
                if (bufPos < lastSlash) {
                    int segLen = nextSlash - bufIdx + 1;
                    System.arraycopy(buf, lastSlash, buf, bufPos, segLen);
                    bufPos += segLen;
                } else {
                    bufPos = nextSlash;
                }

                numDots = 0;
                lastSlash = nextSlash;
                bufIdx = nextSlash;
            }
        }

        String resolved;
        if (bufPos == 0 && numDots == 0) {
            resolved = (absOffset == 0) ? "/" : "";
        } else if ((bufPos - absOffset) == path.length()) {
            resolved = path;
        } else {
            resolved = new String(buf, absOffset, bufPos-absOffset);
        }

        log.debug("resolveRelativeSegments: Resolving '{}' to '{}'", path, resolved);
        return resolved;
    }

    /** Helper method to execute a JCR query */
    private QueryResult queryInternal(String query, String language)
            throws RepositoryException, SlingException {
        Session s = getSession();
        Query q = s.getWorkspace().getQueryManager().createQuery(query,
            language);
        return q.execute();
    }

    /** Converts a JCR Value to a corresponding Java Object */
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

    /**
     * Lazily acquired InputStream which only accesses the JCR Value InputStream
     * if data is to be read from the stream.
     */
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
                // ignore
            }
            return false;
        }

        @Override
        public synchronized void mark(int readlimit) {
            try {
                getStream().mark(readlimit);
            } catch (IOException ioe) {
                // ignore
            }
        }

        @Override
        public synchronized void reset() throws IOException {
            getStream().reset();
        }
    }

}

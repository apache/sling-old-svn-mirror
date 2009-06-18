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
package org.apache.sling.jcr.base.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * The <code>PooledSession</code> class implements the
 * <code>javax.jcr.Session</code> interface as a wrapper to a delegatee session.
 * The only method overwritten by this implementation is the {@link #logout()}
 * which does not actually logout the delegatee but releases this session to the
 * session pool to which this session is attached and
 * {@link #impersonate(Credentials)} which also tries to return a pooled session
 * for the impersonated user.
 */
public class PooledSession implements Session {

    /**
     * The {@link SessionPool session pool} to which this session belongs.
     */
    private final SessionPool sessionPool;

    /**
     * The delegatee session to which all methods except {@link #logout()} are
     * delegated.
     */
    private final Session delegatee;

    /**
     * Creates an instance of this class.
     *
     * @param sessionPool The {@link SessionPool session pool} to which
     *          this instance belongs.
     * @param delegatee The <code>Session</code> to which all calls are
     *          delegated.
     */
    public PooledSession(SessionPool sessionPool, Session delegatee) {
        this.sessionPool = sessionPool;
        this.delegatee = delegatee;
    }

    /**
     * Returns the {@link SessionPool} to which this session belongs.
     */
    protected SessionPool getSessionPool() {
        return this.sessionPool;
    }

    /**
     * Returns the delegatee session to which all calls except {@link #logout()}
     * and {@link #impersonate(Credentials)} are delegated.
     */
    public Session getSession() {
        return this.delegatee;
    }

    //---------- Session interface --------------------------------------------

    /**
     * Releases this session to the session pool, to which this instance is
     * attached.
     */
    public void logout() {
        this.getSessionPool().release(this);
    }

    /** @inheritDoc */
    public void addLockToken(String lt) {
        this.getSession().addLockToken(lt);
    }

    /** @inheritDoc */
    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        this.getSession().checkPermission(absPath, actions);
    }

    /** @inheritDoc */
    public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        this.getSession().exportDocumentView(absPath, contentHandler, skipBinary, noRecurse);
    }

    /** @inheritDoc */
    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        this.getSession().exportDocumentView(absPath, out, skipBinary, noRecurse);
    }

    /** @inheritDoc */
    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        this.getSession().exportSystemView(absPath, contentHandler, skipBinary, noRecurse);
    }

    /** @inheritDoc */
    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        this.getSession().exportSystemView(absPath, out, skipBinary, noRecurse);
    }

    /** @inheritDoc */
    public Object getAttribute(String name) {
        return this.getSession().getAttribute(name);
    }

    /** @inheritDoc */
    public String[] getAttributeNames() {
        return this.getSession().getAttributeNames();
    }

    /** @inheritDoc */
    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException {
        return this.getSession().getImportContentHandler(parentAbsPath, uuidBehavior);
    }

    /** @inheritDoc */
    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
        return this.getSession().getItem(absPath);
    }

    /** @inheritDoc */
    public String[] getLockTokens() {
        return this.getSession().getLockTokens();
    }

    /** @inheritDoc */
    public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
        return this.getSession().getNamespacePrefix(uri);
    }

    /** @inheritDoc */
    public String[] getNamespacePrefixes() throws RepositoryException {
        return this.getSession().getNamespacePrefixes();
    }

    /** @inheritDoc */
    public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
        return this.getSession().getNamespaceURI(prefix);
    }

    /** @inheritDoc */
    public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
        return this.getSession().getNodeByUUID(uuid);
    }

    /** @inheritDoc */
    public Repository getRepository() {
        return this.getSession().getRepository();
    }

    /** @inheritDoc */
    public Node getRootNode() throws RepositoryException {
        return this.getSession().getRootNode();
    }

    /** @inheritDoc */
    public String getUserID() {
        return this.getSession().getUserID();
    }

    /** @inheritDoc */
    public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return this.getSession().getValueFactory();
    }

    /** @inheritDoc */
    public Workspace getWorkspace() {
        return this.getSession().getWorkspace();
    }

    /** @inheritDoc */
    public boolean hasPendingChanges() throws RepositoryException {
        return this.getSession().hasPendingChanges();
    }

    /**
     * Impersonates this session calling the Session pool manager for an
     * impersonated session thus returning a pooled session.
     */
    public Session impersonate(Credentials credentials) throws LoginException, RepositoryException {
        return this.getSessionPool().getPoolManager().impersonate(this.getSession(), credentials);
    }

    /** @inheritDoc */
    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException {
        this.getSession().importXML(parentAbsPath, in, uuidBehavior);
    }

    /** @inheritDoc */
    public boolean isLive() {
        return this.getSession().isLive();
    }

    /** @inheritDoc */
    public boolean itemExists(String absPath) throws RepositoryException {
        return this.getSession().itemExists(absPath);
    }

    /** @inheritDoc */
    public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        this.getSession().move(srcAbsPath, destAbsPath);
    }

    /** @inheritDoc */
    public void refresh(boolean keepChanges) throws RepositoryException {
        this.getSession().refresh(keepChanges);
    }

    /** @inheritDoc */
    public void removeLockToken(String lt) {
        this.getSession().removeLockToken(lt);
    }

    /** @inheritDoc */
    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        this.getSession().save();
    }

    /** @inheritDoc */
    public void setNamespacePrefix(String prefix, String uri) throws NamespaceException, RepositoryException {
        this.getSession().setNamespacePrefix(prefix, uri);
    }
}

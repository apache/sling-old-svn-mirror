/*
 * $Id: DeliveryPageWrapper.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import com.day.hermes.contentbus.Atom;
import com.day.hermes.contentbus.CSDInfo;
import com.day.hermes.contentbus.Container;
import com.day.hermes.contentbus.ContentBusException;
import com.day.hermes.contentbus.ContentElement;
import com.day.hermes.contentbus.ContentNode;
import com.day.hermes.contentbus.HierarchyNode;
import com.day.hermes.contentbus.NoSuchContentElementException;
import com.day.hermes.contentbus.ObjectClass;
import com.day.hermes.contentbus.Page;
import com.day.hermes.contentbus.PageIterator;
import com.day.hermes.contentbus.Ticket;
import com.day.hermes.contentbus.UUID;
import com.day.hermes.contentbus.Version;
import com.day.hermes.contentbus.VersionId;
import com.day.net.URI;

/**
 * The <code>DeliveryPageWrapper</code> wraps a Contentbus {@link Page} and
 * adds a dependency to the {@link DeliveryHttpServletResponse} whenever another
 * page is accessed from this <code>Page</code>.
 *
 * @version $Revision: 1.8 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author mreutegg
 * @since echidna
 * @audience core
 */
class DeliveryPageWrapper implements Page {

    /** The ContentBus Page */
    private final Page delegatee;

    /** The response object for the dependency registration */
    private final DeliveryHttpServletResponse response;

    /**
     * Constructs a new DeliveryPageWrapper.
     *
     * @param page The ContentBus <code>Page</code>.
     * @param res The response object for the dependency registration
     */
    public DeliveryPageWrapper(Page page, DeliveryHttpServletResponse res) {
        delegatee = page;
        response = res;
    }

    /**
     * Returns the delegatee <code>Page</code>.
     * <p>
     * <em><strong>NOTE: Do not use this page to access content because you
     * will loose the automatic dependency handling of this instance.</strong>
     * </em>.
     */
    public Page getPage() {
        return delegatee;
    }
    
    public void commit() throws ContentBusException {
        delegatee.commit();
    }

    public void copy(String dsthandle, String above, boolean shallow)
            throws ContentBusException {
        delegatee.copy(dsthandle, above, shallow);
    }

    public Page create(String csd, String labelhint, String above)
            throws ContentBusException {
        Page p = delegatee.create(csd, labelhint, above);
        response.registerDependency(p.getPath());
        return new DeliveryPageWrapper(p, response);
    }

    public void delete(boolean shallow) throws ContentBusException {
        delegatee.delete(shallow);
    }

    public boolean exists() {
        return delegatee.exists();
    }

    public String getAbsoluteParent(int level) {
        return delegatee.getAbsoluteParent(level);
    }

    public Atom getAtom(String qualident)
            throws NoSuchContentElementException, ContentBusException {
        return delegatee.getAtom(qualident);
    }

    public String getAtomString(String qualident)
            throws NoSuchContentElementException, ContentBusException {
        return delegatee.getAtomString(qualident);
    }

    public PageIterator getChildren() throws ContentBusException {
        PageIterator it = delegatee.getChildren();
        response.registerDependency(delegatee.getPath() + "/");
        return new DeliveryPageIteratorWrapper(it, response);
    }

    public PageIterator getChildren(int rights) throws ContentBusException {
        PageIterator it = delegatee.getChildren(rights);
        response.registerDependency(delegatee.getPath() + "/");
        return new DeliveryPageIteratorWrapper(it, response);
    }

    public Container getContent() throws ContentBusException {
        return delegatee.getContent();
    }

    public ContentNode getContentNode() {
        return delegatee.getContentNode();
    }

    public String getCSD() throws ContentBusException {
        return delegatee.getCSD();
    }

    public CSDInfo getCSDInfo() throws ContentBusException {
        return delegatee.getCSDInfo();
    }

    public String getEffectiveHandle() {
        return delegatee.getEffectiveHandle();
    }

    public String getEffectiveLabel() {
        return delegatee.getEffectiveLabel();
    }

    public Page getEffectivePage() {
        Page p = delegatee.getEffectivePage();
        response.registerDependency(p.getPath());
        return new DeliveryPageWrapper(p, response);
    }

    public ContentElement getElement(String qualident)
            throws NoSuchContentElementException, ContentBusException {
        return delegatee.getElement(qualident);
    }

    public String getHandle() {
        return delegatee.getPath();
    }

    public HierarchyNode getHierarchyNode() {
        return delegatee.getHierarchyNode();
    }

    public String getLabel() {
        return delegatee.getLabel();
    }

    public ContentElement getNearestElement(String qualident)
            throws ContentBusException {
        return delegatee.getNearestElement(qualident);
    }

    public String getRelativeParent(int level) {
        return delegatee.getRelativeParent(level);
    }

    public String getSymLinkTarget() {
        return delegatee.getSymLinkTarget();
    }

    public Ticket getTicket() {
        return new DeliveryTicketWrapper(delegatee.getTicket(), response);
    }

    public URI getURI(ContentElement elem) {
        return delegatee.getURI(elem);
    }

    public UUID getUUID() throws ContentBusException {
        return delegatee.getUUID();
    }

    public String md5() throws ContentBusException {
        return delegatee.md5();
    }

    public Version getVersion() throws ContentBusException {
        return delegatee.getVersion();
    }

    public VersionId getVersionId() {
        return delegatee.getVersionId();
    }

    public boolean hasAnyChildren() throws ContentBusException {
        boolean b = delegatee.hasAnyChildren();
        response.registerDependency(delegatee.getPath() + "/");
        return b;
    }

    public int numChildren() throws ContentBusException {
        response.registerDependency(delegatee.getPath() + "/");
        return delegatee.numChildren();
    }

    public int numAnyChildren() throws ContentBusException {
        response.registerDependency(delegatee.getPath() + "/");
        return delegatee.numAnyChildren();
    }

    public boolean hasChildren() throws ContentBusException {
        boolean b = delegatee.hasChildren();
        response.registerDependency(delegatee.getPath());
        return b;
    }

    public boolean hasObjectClass(String objectclass) {
        return delegatee.hasObjectClass(objectclass);
    }

    public boolean isEffectivePage() {
        return delegatee.isEffectivePage();
    }

    public boolean isInTransaction() throws ContentBusException {
        return delegatee.isInTransaction();
    }

    public boolean isSymLink() {
        return delegatee.isSymLink();
    }

    public void move(String dsthandle, String above, boolean shallow)
            throws ContentBusException {
        delegatee.move(dsthandle, above, shallow);
    }

    public ObjectClass[] objectClasses() throws ContentBusException {
        return delegatee.objectClasses();
    }

    public void rollback() throws ContentBusException {
        delegatee.rollback();
    }

    public void setSortOrder(String labels[]) throws ContentBusException {
        delegatee.setSortOrder(labels);
    }

    public Container startTransaction() throws ContentBusException {
        return delegatee.startTransaction();
    }

    public Container startTransaction(Object userObject) throws ContentBusException {
        return delegatee.startTransaction(userObject);
    }
}

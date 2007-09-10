/*
 * $Id: DeliveryTicketWrapper.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import com.day.hermes.contentbus.*;
import com.day.hermes.util.ACL;
import com.day.hermes.util.Finalizer;
import com.day.net.URI;

import java.io.File;
import java.io.InputStream;
import java.security.AccessControlException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.Date;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.EntityResolver;
import org.w3c.dom.Document;

/**
 * The <code>DeliveryTicketWrapper</code> wraps the common ContentBus
 * {@link Ticket} class and adds a dependency to the
 * {@link DeliveryHttpServletResponse} object whenever a ContentBus page is
 * accessed with this ticket.
 *
 * @version $Revision: 1.14 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author mreutegg
 * @since echidna
 * @audience core
 */
class DeliveryTicketWrapper implements Ticket {

    /** The ContentBus Ticket */
    private final Ticket delegatee;

    /** The response object for the dependency registration */
    private final DeliveryHttpServletResponse response;

    /**
     * Constructs a new <code>DeliveryTicketWrapper</code>.
     *
     * @param ticket The ContentBus <code>Ticket</code>.
     * @param resp The response object for the dependency registration
     */
    public DeliveryTicketWrapper(Ticket ticket, DeliveryHttpServletResponse resp) {
        delegatee = ticket;
        response = resp;
    }

    /**
     * Returns the delegatee <code>Ticket</code>.
     * <p>
     * <em><strong>NOTE: Do not use this ticket to access content because you
     * will loose the automatic dependency handling of this instance.</strong>
     * </em>.
     */
    public Ticket getTicket() {
        return delegatee;
    }

    public void addTempFile(File file) {
        delegatee.addTempFile(file);
    }

    public void close() throws ContentBusException {
        delegatee.close();
    }

    public void commit() throws ContentBusException {
        delegatee.commit();
    }

    public void copyPage(String handle, String dstHandle, String aboveLabel,
                         boolean shallow) throws ContentBusException {
        delegatee.copyPage(handle, dstHandle, aboveLabel, shallow);
    }

    public ContentPackage createContentPackage(String handle) throws ContentBusException {
        response.registerDependency(handle);
        return delegatee.createContentPackage(handle);
    }

    public InputSource createInputSource(String handle)
            throws ContentBusException {
        response.registerDependency(handle);
        return delegatee.createInputSource(handle);
    }

    public InputStream createInputStream(String handle)
            throws ContentBusException {
        response.registerDependency(handle);
        return delegatee.createInputStream(handle);
    }

    public Page createPage(String handle, String csd) throws ContentBusException {
        Page p = delegatee.createPage(handle,  csd);
        response.registerDependency(p.getPath());
        return new DeliveryPageWrapper(p, response);
    }

    public Page createPage(String handle, String csd, UUID uuid)
            throws ContentBusException {
        Page p = delegatee.createPage(handle, csd, uuid);
        response.registerDependency(p.getPath());
        return new DeliveryPageWrapper(p, response);
    }

    public Page createPage(String parent, String csd, String labelhint,
                           String aboveLabel) throws ContentBusException {
        Page p = delegatee.createPage(parent, csd, labelhint, aboveLabel);
        response.registerDependency(p.getPath());
        return new DeliveryPageWrapper(p, response);
    }

    public Page createPage(String parent, String csd, String labelhint,
                           String aboveLabel, boolean exact) throws ContentBusException {
        Page p = delegatee.createPage(parent, csd, labelhint, aboveLabel, exact);
        response.registerDependency(p.getPath());
        return new DeliveryPageWrapper(p, response);
    }

    public Version createVersion(String handle)
            throws ContentBusException, NoSuchPageException {
        return delegatee.createVersion(handle);
    }

    public Document createXMLDocument(String handle) throws ContentBusException {
        response.registerDependency(handle);
        return delegatee.createXMLDocument(handle);
    }

    public InputSource createXmlIncludingInputSource(String handle)
            throws ContentBusException {
        response.registerDependency(handle);
        return delegatee.createXmlIncludingInputSource(handle);
    }

    public XMLReader createXMLReader() throws ContentBusException {
        return delegatee.createXMLReader();
    }

    public void deletePage(String handle, boolean shallow)
            throws ContentBusException {
        delegatee.deletePage(handle, shallow);
    }

    public Ticket duplicate() {
        // return a new DeliveryTicketWrapper instance
	// this is to ensure that a duplicate of a
	// DeliveryTicketWrapper instance is equal to
	// the original.
	return new DeliveryTicketWrapper(delegatee.duplicate(), response);
    }

    public PageIterator getChildren(String handle) throws ContentBusException {
        PageIterator it = delegatee.getChildren(handle);
        response.registerDependency(handle + "/");
        return new DeliveryPageIteratorWrapper(it, response);
    }

    public PageIterator getChildren(String handle, int rights)
            throws ContentBusException {
        PageIterator it = delegatee.getChildren(handle, rights);
        response.registerDependency(handle + "/");
        return new DeliveryPageIteratorWrapper(it, response);
    }

    public String getCSD(String handle)
            throws ContentBusException, NoSuchPageException {
        return delegatee.getCSD(handle);
    }

    public PageIterator getDeletedChildren(String handle) throws ContentBusException {
        return delegatee.getDeletedChildren(handle);
    }

    public String getEffectiveCSD(String handle)
            throws ContentBusException, NoSuchPageException {
        return delegatee.getEffectiveCSD(handle);
    }

    public Page getEffectivePage(String handle)
            throws ContentBusException, NoSuchPageException {
        Page p = delegatee.getEffectivePage(handle);
        response.registerDependency(p.getPath());
        return new DeliveryPageWrapper(p, response);
    }

    public Page getEffectivePage(String handle, VersionId version)
            throws ContentBusException, NoSuchPageException {
        Page p = delegatee.getEffectivePage(handle,  version);
        response.registerDependency(p.getPath());
        return new DeliveryPageWrapper(p, response);
    }

    public ContentElement getElement(URI uri)
            throws NoSuchContentElementException, NoSuchPageException,
            AccessControlException, MalformedURLException, ContentBusException {
        ContentElement e = delegatee.getElement(uri);
        response.registerDependency(uri.getPath());
        return e;
    }

    public EntityResolver getEntityResolver() {
        return delegatee.getEntityResolver();
    }

    public HandleExpander getHandleExpander() {
        return delegatee.getHandleExpander();
    }

    public Set getHandleSet(ContentPackage pack) {
        return delegatee.getHandleSet(pack);
    }

    public HierarchyMgr getHierarchyMgr() {
        return delegatee.getHierarchyMgr();
    }

    public HierarchyNode getNode(String handle)
            throws ContentBusException, NoSuchNodeException {
        return delegatee.getNode(handle);
    }

    public HierarchyNodeIterator getNodes(UUID uuid)
            throws ContentBusException {
        return delegatee.getNodes(uuid);
    }

    public Page getPage(String handle)
            throws ContentBusException, NoSuchPageException {
        Page p = delegatee.getPage(handle);
        response.registerDependency(p.getPath());
        return new DeliveryPageWrapper(p, response);
    }

    public Page getPage(String handle, PathContext path)
            throws ContentBusException, NoSuchPageException {
        Page p = delegatee.getPage(handle,  path);
        response.registerDependency(p.getPath());
        return new DeliveryPageWrapper(p, response);
    }

    public Page getPage(String handle, VersionId version)
            throws ContentBusException, NoSuchPageException {
        Page p = delegatee.getPage(handle, version);
        response.registerDependency(p.getPath());
        return new DeliveryPageWrapper(p, response);
    }

    public Page getPage(String handle, String versionKey)
            throws ContentBusException, NoSuchPageException {
        Page p = delegatee.getPage(handle, versionKey);
        response.registerDependency(p.getPath());
        return new DeliveryPageWrapper(p, response);
    }

    public long getPageModificationTime(String handle)
            throws ContentBusException {
        return delegatee.getPageModificationTime(handle);
    }

    public URL getResource(String handle) throws ContentBusException {
        response.registerDependency(handle);
	return delegatee.getResource(handle);
    }

    public long getResourceModificationTime(String handle)
            throws ContentBusException {
        return delegatee.getResourceModificationTime(handle);
    }

    public String getUserHandle() {
        return delegatee.getUserHandle();
    }

    public String getUserId() {
        return delegatee.getUserId();
    }

    public Page getUserPage() {
        response.registerDependency(delegatee.getUserPage().getPath());
        return new DeliveryPageWrapper(delegatee.getUserPage(), response);
    }

    public Version getVersion(String handle)
            throws ContentBusException, NoSuchPageException {
        return delegatee.getVersion(handle);
    }

    public Version getVersion(String handle, Date date)
            throws ContentBusException, NoSuchPageException {
        return delegatee.getVersion(handle, date);
    }

    public Version getVersion(String handle, VersionId id)
            throws ContentBusException, NoSuchPageException {
        return delegatee.getVersion(handle,  id);
    }

    public Version getVersion(String handle, String tagname)
            throws ContentBusException, NoSuchPageException {
        return delegatee.getVersion(handle, tagname);
    }

    public Version[] getVersions(String handle)
            throws ContentBusException, NoSuchPageException {
        return delegatee.getVersions(handle);
    }

    public Version[] getVersions(UUID uuid) throws ContentBusException {
        return delegatee.getVersions(uuid);
    }

    public VersionSelector getVersionSelector() {
        return delegatee.getVersionSelector();
    }

    public EntityResolver getXmlIncludingEntityResolver() {
        return delegatee.getXmlIncludingEntityResolver();
    }

    public void grant(String handle, int rights) throws AccessControlException {
        delegatee.grant(handle, rights);
    }

    public void grant(Page page, int rights) throws AccessControlException {
        delegatee.grant(page, rights);
    }

    public HandleIterator handleIterator(ContentPackage pkg) {
        return delegatee.handleIterator(pkg);
    }

    public HandleIterator handleIterator(ContentPackage pkg, int rights) {
        return delegatee.handleIterator(pkg, rights);
    }

    public boolean hasAnyChildren(String handle) {
        response.registerDependency(handle + "/");
        return delegatee.hasAnyChildren(handle);
    }

    public boolean hasChildren(String handle) {
        response.registerDependency(handle + "/");
        return delegatee.hasChildren(handle);
    }

    public int numChildren(String handle) throws ContentBusException {
        response.registerDependency(handle + "/");
        return delegatee.numChildren(handle);
    }

    public int numAnyChildren(String handle) throws ContentBusException {
        response.registerDependency(handle + "/");
        return delegatee.numAnyChildren(handle);
    }

    public boolean hasContentPage(String handle) {
        response.registerDependency(handle);
        return delegatee.hasContentPage(handle);
    }

    public boolean hasPage(String handle) {
        response.registerDependency(handle);
        return delegatee.hasPage(handle);
    }

    public boolean isAnonymous() {
        return delegatee.isAnonymous();
    }

    public boolean isGranted(String handle, int rights) {
        return delegatee.isGranted(handle,  rights);
    }

    public boolean isGranted(Page page, int rights) {
        return delegatee.isGranted(page, rights);
    }

    public boolean isInTransaction() {
        return delegatee.isInTransaction();
    }

    public boolean isSuperuser() {
        return delegatee.isSuperuser();
    }

    public int matchHandle(String urlpath, int from)
            throws NoSuchContentElementException, MalformedURLException,
            AccessControlException, ContentBusException {
        return delegatee.matchHandle(urlpath, from);
    }

    public void movePage(String handle, String dstHandle, String aboveLabel,
                         boolean shallow) throws ContentBusException {
        delegatee.movePage(handle, dstHandle, aboveLabel, shallow);
    }

    public void orderPage(String handle, String aboveLabel)
            throws ContentBusException {
        delegatee.orderPage(handle, aboveLabel);
    }

    public void registerObject(Finalizer object) {
        delegatee.registerObject(object);
    }

    public void restorePackage(String packhandle) throws ContentBusException {
        delegatee.restorePackage(packhandle);
    }

    public void restoreVersion(String handle, VersionId vid)
            throws ContentBusException, NoSuchVersionException, NoSuchPageException {
        delegatee.restoreVersion(handle, vid);
    }

    public void deleteVersion(String handle, VersionId vid)
            throws ContentBusException, NoSuchVersionException, NoSuchPageException {
        delegatee.deleteVersion(handle, vid);
    }

    public void rollback() throws ContentBusException {
        delegatee.rollback();
    }

    public void setSortOrder(String handle, String[] labels)
            throws ContentBusException {
        delegatee.setSortOrder(handle, labels);
    }

    public VersionSelector setVersionSelector(VersionSelector selector) {
        return delegatee.setVersionSelector(selector);
    }

    public void tagVersion(String handle, VersionId vid, String name, String value)
            throws ContentBusException {
        delegatee.tagVersion(handle,  vid, name, value);
    }

    public void touchPage(String handle) throws ContentBusException {
        delegatee.touchPage(handle);
    }

    public void updatePackageData(String handle) throws ContentBusException {
        delegatee.updatePackageData(handle);
    }

    public Ticket getRealTicket() {
        // bug 10772: have to return wrapped ticket to ensure proper dependency
        //              handling
        // bug 11733: only if the real ticket is not the same as the delegatee
        Ticket realTicket = delegatee.getRealTicket();
        if (realTicket != delegatee) {
            return new DeliveryTicketWrapper(realTicket, response);
        }

        return this;
    }

    public Ticket impersonate(String userHandle)
            throws LoginException, ContentBusException {
        return new DeliveryTicketWrapper(delegatee.impersonate(userHandle), response);
    }

    public boolean isMemberOf(String groupHandle) {
        return delegatee.isMemberOf(groupHandle);
    }

    public ACL getUserAcl() {
        return delegatee.getUserAcl();
    }

    public ACL getUserAcl(String handle) throws ContentBusException {
        return delegatee.getUserAcl(handle);
    }

    public String toString() {
	return delegatee.toString();
    }

    public boolean equals(Object obj) {
	if (obj instanceof DeliveryTicketWrapper) {
	    DeliveryTicketWrapper other = (DeliveryTicketWrapper)obj;
	    return delegatee.equals(other.delegatee);
	} else {
	    return false;
	}
    }

    public int hashCode() {
	return delegatee.hashCode();
    }

    public boolean isValid() {
	return delegatee.isValid();
    }
}

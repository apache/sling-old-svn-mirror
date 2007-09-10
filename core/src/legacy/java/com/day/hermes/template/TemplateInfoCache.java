/*
 * $Url: $
 * $Id: TemplateInfoCache.java 22189 2006-09-07 11:47:26Z fmeschbe $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package com.day.hermes.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationKey;

import sun.security.krb5.internal.Ticket;

import com.day.logging.FmtLogger;
import com.day.logging.Log;

public class TemplateInfoCache implements EventListener {

    /** Default logging */
    private static final FmtLogger log =
        (FmtLogger) FmtLogger.getLogger(TemplateInfoCache.class);

    /** The default path names to look in for templates */
//    private static final String[] DEFAULT_TEMPLATE_PATH =
//        new String[] { "/", "/apps/", "/libs/" };

    /** The attribute name of a template path entry */
    private static final String PATH_ATTR = "path";

    /** The path names to look in for templates */
    private TemplatePathContext templatePath;

    /** internal references to the templateinfos by (possibly relative) path */
    private final HashMap templateInfosByPath = new HashMap();

    /** internal references to the templateinfos by (absolute) handle */
    private static final HashMap templateInfosByHandle = new HashMap();

    /**
     * internal referenced to pending template infos. this is to avoid
     * stackoverflow if circular references occur while loading.
     */
    private static final HashSet pendingHandles = new HashSet();

    /**
     * Creates a instance of the <code>TemplateInfoCache</code> and registers
     * with the <code>ObservationService</code> to get informed on changes in
     * the ContentBus. This observation is mainly used to get informed on
     * changed templates. If the <code>ObservationService</code> cannot be
     * obtained, the cache will still be able to work, but the dynamic update of
     * <code>TemplateInfo</code> entries will not work.
     *
     * Configures the template info cache to use the given handles as the
     * template page paths.
     * <p>
     * This method is for the use of the {@link DeliveryModule} class only.
     */
    TemplateInfoCache(Session session, Configuration config) {

        List cfgPaths =
            config.getList(ConfigurationKey.constructAttributeKey(PATH_ATTR));
        List paths = new ArrayList();
        for (Iterator pi=cfgPaths.iterator(); pi.hasNext(); ) {
            Object pathObj = pi.next();
            if (pathObj != null) {
                String path = String.valueOf(pathObj);
                if (path.length() == 0) {
                    paths.add(path);
                } else {
                    log.debug("init: Ignoring empty template path entry");
                }
            } else {
                log.debug("init: Ignoring null template path entry");
            }
        }

        String[] path = (String[]) paths.toArray(new String[paths.size()]);
        templatePath = new TemplatePathContext(session, path);

        try {
            session.getWorkspace().getObservationManager().addEventListener(this,
                Event.NODE_ADDED|Event.NODE_REMOVED, "/", true, null,
                new String[]{ TemplatePathContext.NODE_TYPE_TEMPLATE} , false);
        } catch (RepositoryException re) {
            log.warn("Cannot register for dynamic template cache update", re);
        }
    }

    /**
     * Configures the template info cache to use the given handles as the
     * template page paths.
     * <p>
     * This method is for the use of the {@link TemplateInfo} class only.
     *
     * @param path The path prefixes to use
     */
//    void setTemplatePath(String[] path) {
//        templatePath.setPath(path);
//    }

    /**
     * Removes a <code>TemplateInfo</code> and all its dependencies from the
     * cache.
     */
    private synchronized void remove(String handle) {
        // check whether we know about that handle
        TemplateInfo info = (TemplateInfo) templateInfosByHandle.remove(handle);
        if (info != null) {

            // yes, so remove
            log.debug("Remove templateinfo from cache. handle={0}", handle);

            // first find out about all paths, the <code>TemplateInfo</code>
            // is known about and remove from the path cache.
            Iterator iter = info.paths();
            while (iter.hasNext()) {
                templateInfosByPath.remove(iter.next());
            }

            // then remove all templates, which are based on this template
            iter = info.references();
            while (iter.hasNext()) {
                remove(((TemplateInfo) iter.next()).getHandle());
            }
        }
    }

    /**
     * Returns the <code>TemplateInfo</code> instance loaded from the given
     * path. This path may be an absolute path or a relative path, which is
     * checked in turn with the template path configured with the
     * {@link #setTemplatePath} method.
     *
     * @param ticket The <code>Ticket</code> to access the page in the
     *            ContentBus if not already loaded in the cache.
     * @param path The path under which to look for the template page.
     * @return The <code>TemplateInfo</code> loaded from the given handle or
     *         <code>null</code> if no template could be found for that path.
     */
    synchronized TemplateInfo getInstance2(Session session, String path) {
        log.trace("TemplateInfoCache.getInstance(path={0})", path);

        // First try to get a cached instance
        TemplateInfoImpl info = (TemplateInfoImpl) templateInfosByPath.get(path);
        if (info != null) {
            log.trace("taking info from path cache. handle={0} id={1}",
                info.getHandle(), info);
            return info;
        }

        // else try to create a new template info
        String templateHandle;
        try {
            templateHandle = templatePath.resolveHandle(session, path);
        } catch (RepositoryException re) {
            log.info("Cannot expand template path {0}", path, re);
            return null;
        }

        // abort, if no valid template page could be found
        if (templateHandle == null) {
            log.debug("No template found for path={0}", path);
            return null;
        }

        // Check if templateinfo is already in handle cache
        info = (TemplateInfoImpl) templateInfosByHandle.get(templateHandle);
        if (info != null) {

            // add this path to the path cache
            templateInfosByPath.put(path, info);
            info.addPath(path);

            log.trace("taking info from handle cache. handle={0} id={1}",
                info.getHandle(), info);

            return info;
        }

        // sync with pending-set
        if (pendingHandles.contains(templateHandle)) {
            log.error("Unable to load template info from {0}. There are"
                + " circular references.", templateHandle);
            return null;
        }
        pendingHandles.add(templateHandle);

        // try to create new template info
        try {
            // load the template from the page
            Item page = session.getItem(templateHandle);

            // if the template item is a reference property dereference
            // otherwise return null
            if (!page.isNode()) {
                Property prop = (Property) page;
                if (prop.getType() == PropertyType.REFERENCE) {
                    page = prop.getNode();
                } else {
                    log.error(
                        "Path {0} addresses a property, cannot load template",
                        templateHandle);
                    return null;
                }
            }

            info = new TemplateInfoImpl(this, (Node) page);

            // update caches
            info.addPath(path);
            templateInfosByPath.put(path, info);
            templateInfosByHandle.put(templateHandle, info);

            log.debug("Add templateinfo to cache. handle={0} id={1}",
                info.getHandle(), info);
            return info;
        } catch (RepositoryException e) {
            log.error("Unable to load template info from {0}: {1}",
                templateHandle, e.getMessage());
            return null;
        } finally {
            // remove from pending-set
            pendingHandles.remove(templateHandle);
        }
    }

    /**
     * Returns the template named with the handle. This handle may either be
     * absolute or relative. The handle is looked up in the handle cache and in
     * the path cache. If no cache has an entry for the handle <code>null</code>
     * is returned.
     * <p>
     * Since this method accesses thread-unsafe members that are changed inside
     * <code>synchronized</code> methods, it has been itself marked
     * <code>synchronized</code>.
     *
     * @param handle The handle for the template to return.
     * @return The template addressed with the handle or <code>null</code> if
     *         the template has not yet been loaded.
     */
    synchronized TemplateInfo getTemplateInfo(String handle) {
        if (templateInfosByHandle.containsKey(handle)) {
            return (TemplateInfo) templateInfosByHandle.get(handle);
        }

        return (TemplateInfo) templateInfosByPath.get(handle);
    }

    /**
     * Returns the handle of the template applicable for the page. This is
     * how we get at the applicable template for the page :
     *
     * <ol>
     * <li>If the page has the object class <em>template</em>, the handle of
     *      the page is taken as the name of the template.
     * <li>Else, if the page has a <em>Template</em> atom. The value of this
     *      atom is taken if not empty.
     * <li>Else the name of the CSD of the page is used to address the template.
     * </ol>
     *
     * @param page The page for which the applicable template handle has to
     *      be retrieved.
     *
     * @return The handle of the template applicable for the page.
     *
     * @throws ContentBusException if the CSD name of the page cannot be
     *      retrieved.
     */
    public static String getTemplate(Node page) throws RepositoryException {

        String templateName = null;

        // (1) check whether the page is a template page
        if (page.isNodeType(TemplatePathContext.NODE_TYPE_TEMPLATE)) {
            log.debug("getTemplate: Page {0} is a template, unsing it",
                page.getPath());
            templateName = page.getPath();
        }

        // (2) try Template atom (ignore ContentBusException)
        if ((templateName == null) || (templateName.length() == 0)) {
            try {
                templateName = page.getProperty("Template").getString();
                log.debug("getTemplate: Page {0} has Template atom: {1}",
                    page.getPath(), templateName);
            } catch (RepositoryException cbe) {
                log.debug("getTemplate: Problem accessing Template atom:",
                    cbe.toString());
            }
        }

        // (3) use CSD, throw ContentBusException
        if ((templateName == null) || (templateName.length() == 0)) {
            log.debug("getTemplate: Trying page CSD as template");
            templateName = page.getPrimaryNodeType().getName();
        }

        // return the template name
        log.debug("getTemplate({0}) -> {1}", page.getPath(), templateName);
        return templateName;
    }

    /**
     * Returns the <code>TemplateInfo</code> object for the page. The name of
     * the applicable template for that page is retrieved using the
     * {@link #getTemplate} method. If the template info must be loaded from
     * the ContentBus, the ticket used to load the page is used to load that
     * template info.
     *
     * @param page The <code>Page</code> for which to get the applicable
     *          <code>TemplateInfo</code>.
     *
     * @return The <code>TemplateInfo</code> object of the template applicable
     *      for the page if existing or <code>null</code> if the template
     *      cannot be retrieved from the page or the template is not existing.
     */
    public TemplateInfo getInstance(Node page) {
        try {
            String templatePath = getTemplate(page);
            return getInstance(page.getSession(), templatePath);
        } catch (RepositoryException cbe) {
            log.warn("getInstance: cannot get the template: {0}",
                cbe.toString());
        }

        // had an exception, if i get here
        return null;
    }

    /**
     * Returns the <code>TemplateInfo</code> object for the page addressed
     * with the path. The name of the applicable template for that page is
     * retrieved using the {@link #getTemplate} method. The ticket is used to
     * load both the named page and - if the template info is not contained in
     * the page itself - to load the template from the ContentBus if needed.
     * <p>
     * If the path is not absolute, it is assumed to denote a template and it
     * is tried to find the template using the configured template path. In this
     * case the algorithm described for the {@link #getTemplate} method is not
     * used as this method is not called at all.
     *
     * @param ticket The <code>Ticket</code> used to read the template content
     *      page from the ContentBus.
     * @param path The ContentBus handle of either a content or a template page.
     *
     * @return The <code>TemplateInfo</code> object created for the template
     *      applicable for the named page or <code>null</code> if no applicable
     *      template can be found or if the template cannot be loaded.
     */
    public TemplateInfo getInstance(Session ticket, String path) {
        try {
            if (path.startsWith("/")) {
                log.debug("getInstance: {0} might be web page or template page");
                return getInstance((Node) ticket.getItem(path));
            }
        } catch (PathNotFoundException nspe) {
            log.debug("getInstance: {0} is not a page handle", path);
        } catch (RepositoryException cbe) {
            log.debug("getInstance: {0} has some problem: {1}", path,
                    cbe.toString());
        }

        // had an exception, if i get here
        log.debug("getInstance: Try to find template with given path");
        return getInstance2(ticket, path);
    }

    /**
     * Returns an <code>Iterator</code> over the handles of the loaded
     * templates.
     * <p>
     * This method should only be called by some method holding the lock on this
     * object, since the <code>templateInfosByHandle</code> member internals
     * are returned and in some synchronized method this
     * <code>templateInfosByHandle</code> member might be changed while
     * iterating over the keys. Therefore the method has been made private.
     *
     * @return An <code>Iterator</code> over the handles of the loaded
     *         templates.
     */
//    private Iterator loadedTemplateInfos() {
//        return templateInfosByHandle.keySet().iterator();
//    }

    //---------- ModificationListener interface ----------------------------

    /**
     * Handles a page modification notification. We don't really update any
     * cached <code>TemplateInfo</code> but rather remove the cache entry
     * and reload upon request. This is why we don't care about the type
     * of modification.
     *
     * @param modif The <code>PageModification</code> describing the
     *      modification and the page modified.
     */
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            Event event = events.nextEvent();
            try {
                remove(event.getPath());
            } catch (RepositoryException re) {
                log.info("Cannot handle event {0}", event, re);
            }
        }
    }
}
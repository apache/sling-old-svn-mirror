/*
 * $Url: $
 * $Id: TemplatePathContext.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import com.day.jcr.AbstractPathContext;
import com.day.logging.FmtLogger;

class TemplatePathContext extends AbstractPathContext
        implements EventListener {

    /** default logger */
    private static final FmtLogger log =
        (FmtLogger) FmtLogger.getLogger(TemplatePathContext.class);

    public static final String NODE_TYPE_TEMPLATE = "cq:template";

    /** The list of handles given at construction time */
    private final ArrayList templatePath;

    /**
     * Creates the path context from the given list of path handles. These
     * handles may contain globs and a listener is setup to listen for
     * changes in the set of matched handles.
     *
     * @param path The list of path handles.
     */
    /* package */ TemplatePathContext(Session session, String[] path) {
        templatePath = new ArrayList();
        setPath(path);

        // register with the observation service to adapt to path changes
        try {
            session.getWorkspace().getObservationManager().addEventListener(
                this, Event.NODE_ADDED|Event.NODE_REMOVED, "/", true, null,
                new String[]{ NODE_TYPE_TEMPLATE }, false);
        } catch (RepositoryException re) {
            log.warn("Cannot register with observation", re);
        }
    }

    /* package */ void setPath(String[] path) {
        // clear the path first
        templatePath.clear();

        // copy the path into the list
        for (int i=0; i < path.length; i++) {
            String handle = path[i];

            // ignore empty/null handles
            if (handle == null || handle.length() == 0) {
                log.debug("TemplatePathContext: Ignoring empty/null path entry");
                continue;
            }

            // cut off trailing slashes, but leave "/" intact
            while (handle.endsWith("/") && handle.length() > 1) {
                handle = handle.substring(0, handle.length()-1);
            }

            // add entry to the path list
            templatePath.add(path[i]);
        }
    }

    //---------- ModificationListener interface ----------------------------

    /**
     * Handles the situation where a pages is created/deleted/moved which
     * has an influence on the internal path list. We are generous and
     * force path rebuild for each such situation.
     *
     * @param modification page modification that took place
     */
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            try {
                String path = events.nextEvent().getPath();
                for (int i=0; i < templatePath.size(); i++) {
                    String tp = (String) templatePath.get(i);
                    // TODO: should actually check pattern matching of path
                    if (path.startsWith(tp)) {
                        setNeedCacheBuild(true);
                        return;
                    }
                }
            } catch (RepositoryException re) {
                log.warn("Cannot get path of event", re);
            }
        }
    }

    //---------- AbstractPathContext API -----------------------------------

    /**
     * Returns <code>true</code> if the handle addresses a page which (1)
     * has a content node and (2) has the object class "template" (see
     * {@link ObjectClass#TEMPLATE}.
     *
     * @param ticket The <code>Ticket</code> to access the ContentBus to further
     *      check the handle.
     * @param handle The handle to check for acceptance.
     *
     * @return <code>true</code> is returned if the handle is acceptable to be
     *      returned.
     */
    protected boolean acceptHandle(Session session, String handle) {
        try {
            if (!session.itemExists(handle)) {
                return false;
            }

            Item item = session.getItem(handle);
            if (!item.isNode()) {
                return false;
            }

            Node node = (Node) item;
            return node.isNodeType(NODE_TYPE_TEMPLATE);
        } catch (RepositoryException re) {
            log.info("acceptHandle: Problem checking {0}: {1}", handle, re);
        }

        // fallback in case of problems
        return false;
    }

    /**
     * Returns the list of page handles configured at instance creation.
     * This implementation does not support addition/removal of path elements
     * after having been setup, thus this method always returns the same
     * data.
     *
     * @param ticket The {@link Ticket} might be used to access the ContentBus.
     *
     * @return The list of path handles configured at instance creation
     *      time.
     */
    protected ArrayList buildPathCache(Session session) {
        return templatePath;
    }
}
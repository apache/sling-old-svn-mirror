/*
 * $Id: TemplateInfoImpl.java 22189 2006-09-07 11:47:26Z fmeschbe $
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
package com.day.hermes.template;

import java.awt.Container;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.day.hermes.legacy.DeliveryHttpServletRequest;
import com.day.hermes.script.ScriptInfo;
import com.day.jcr.ContentPackage;
import com.day.jcr.FilterContentPackageBuilder;
import com.day.logging.FmtLogger;

/**
 * The TemplateInfoImpl implements the TemplateInfo
 *
 * @version $Revision: 1.14 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author tripod
 * @since antbear
 */
class TemplateInfoImpl extends TemplateInfo {

    /** Default logging */
    private static final FmtLogger log =
		(FmtLogger) FmtLogger.getLogger(TemplateInfoImpl.class);

    /** TODO */
    private final TemplateInfoCache cache;

    /** The handle of the page from which this template info has been loaded */
    private final String handle;

    /** The list of the contained {@link ScriptInfo}  */
    private ScriptInfo[] scriptInfos;

    /**
     * The list of script infos access by {@link #getScriptInfos()}
     * <p>
     * This map is sorted by the <code>ScriptInfo</code> objects contained
     * within. The reson for not using a <code>SortedSet</code> is, that for
     * two <code>ScriptInfo</code> objects <em>a</em> and <em>b</em>, with the
     * same globbing patterns and the same method list but different script to
     * call, the entry <em>a</em> would not be overwritten by a subsequent
     * addition of <em>b</em> as required by the contract of the
     * {@link TemplateInfo}. Thus the map is used where the <code>ScriptInfo</code>
     * to add is used as both the kay and the value. In the above case of two
     * objects <em>a</em> and <em>b</em>, the subsequent addition of <em>b</em>
     * would not overwrite the key of entry <em>a</em> but its value, which is
     * what we later retrieve.
     */
    private SortedMap scriptInfoMap;

    /** the allowed locations */
    private ContentPackage allowLocation;

    /** the allowed parent templates */
    private ContentPackage allowParentTemplates;

    /** the allowed children templates */
    private ContentPackage allowChildTemplates;

    /** the resolved base template */
    private TemplateInfoImpl baseTemplate;

    /** the map of eventual references (if this is a base template) */
    private HashMap references;

    //---------- construction --------------------------------------------------

    /**
     * Create a new template info master structure
     *
     * @param page The {@link Page} containing the template
     */
    public TemplateInfoImpl(TemplateInfoCache cache, Node page) throws RepositoryException {
        this.cache = cache;
        this.handle = page.getPath();

    	log.trace("Loading TemplateInfo handle={0}", handle);
    	load(page);
    }

    /**
     * Returns the {@link ScriptInfo} matching the query. The matching
     * strategy is first-match. That is the first {@link ScriptInfo} found
     * in the Vector matching the request details is returned.
     * <p>
     * A match is first searched for in the template itself. If no match can
     * be found and a base template has been configured, the base template is
     * asked for a script info. If the base template does not have any either,
     * <code>null</code> is returned.
     *
     * @param request The {@link DeliveryHttpServletRequest} for which to find
     *      the match {@link ScriptInfo}.
     *
     * @return the first {@link ScriptInfo} matching the request or
     *         <code>null</code> if no script info could be found.
     */
    public ScriptInfo getScriptInfo(DeliveryHttpServletRequest request) {

        // check whether we can handle the script
        for (int i = 0; i < scriptInfos.length && scriptInfos[i] != null; i++) {
            if (scriptInfos[i].matches(request)) {
                return scriptInfos[i];
            }
        }

        // outsch - cannot handle
        log.debug("No scriptinfo for query: {0}", request);
        return null;
    }

    /**
     * Return the {@link ScriptInfo} enumeration for the template
     *
     * @return {@link ScriptInfo} enumeration
     */
    public Iterator getScriptInfos() {
        return scriptInfoMap.values().iterator();
    }

    /**
     * adds a reference templateinfo
     */
    protected void addReference(TemplateInfo ref) {
        if (references == null) {
            references = new HashMap();
        }
        references.put(ref.getHandle(), ref);
    }

    /**
     * returns an iterator over the templateinfo references
     */
    public Iterator references() {
        if (references == null) {
            return Collections.EMPTY_SET.iterator();
        }
        return references.values().iterator();
    }

    /**
     * Get the handle of the template
     * @return the handle of the template
     */
    public String getHandle() {
        return handle;
    }

    /**
     * checks if the handle is allowed
     */
    public boolean isAllowedLocation(String handle) {
        return allowLocation.contains(handle);
    }

    /**
     * checks if the parent template is allowed
     */
    public boolean isAllowedParentTemplate(String templatename) {
        return allowParentTemplates == null
            || allowParentTemplates.contains(templatename);
    }

    /**
     * checks if the child template is alloed
     */
    public boolean isAllowedChildTemplate(String templatename) {
        return allowChildTemplates == null
            || allowChildTemplates.contains(templatename);
    }

    /**
     * Load the script information from the container. The container must
     * contain a ContainerList named "Scripts" for this method to
     * work correctly.
     * <p>
     * Also initialises the list of suggested MIME type serialisations for
     * pages based on this template.  This is used to provide hints to
     * serialisation mechanisms such as WebDAV.
     * <p>
     * Note that this method currently silently (besides the logging) fails,
     * if the information cannot be loaded.
     *
     * @param page The {@link Page} to load the script information from
     *
     * @throws ContentBusException if an error during loading of content occurrs
     */
    private void load(Node page) throws RepositoryException {

        // get basetemplate name
        if (page.hasProperty("cq:baseTemplate")) {
            // get the base template
            Node baseTemplateNode = page.getProperty("cq:baseTemplate").getNode();
            baseTemplate = (TemplateInfoImpl) cache.getInstance(baseTemplateNode);

            if (baseTemplate == null) {

                // invalid base template reference or circular
                log.error(
                    "Template ''{0}'' has invalid base template reference: {1}",
                    handle, baseTemplateNode.getPath());
                throw new RepositoryException(
                    "Unable to load base template reference:"
                        + baseTemplateNode.getPath());

            }

            // register with base template - used for modification
            // notifications
            baseTemplate.addReference(this);

            // prefill the map with the baseTemplates map
            scriptInfoMap = new TreeMap(baseTemplate.scriptInfoMap);

        } else {

            // no base template configured
            baseTemplate = null;

            // create an empty map
            scriptInfoMap = new TreeMap();
        }

        // get my own script definitions
        NodeIterator ni = page.getNodes("cq:scripts");
        while (ni.hasNext()) {
            ScriptInfo si = ScriptInfo.getInstance(ni.nextNode());
            if (si != null) {
                log.debug("load: ScriptInfo={0}", si);
                scriptInfoMap.put(si, si);
            }
        }

        // copy the list to the array for faster search access
        scriptInfos = (ScriptInfo[]) scriptInfoMap.values().toArray(
            new ScriptInfo[scriptInfoMap.size()]);

        // get allow location
        FilterContentPackageBuilder builder = new FilterContentPackageBuilder();
        if (page.hasNode("cq:allowLocation")) {
            builder.addFilters(page.getNode("cq:allowLocation"));
            allowLocation = builder.createContentPackage();
        }

        // get allow parent
        if (page.hasNode("cq:allowParent")) {
            builder.addFilters(page.getNode("cq:allowParent"));
            allowParentTemplates = builder.createContentPackage();
        }

        // get allow child
        if (page.hasNode("cq:allowChild")) {
            builder.addFilters(page.getNode("cq:allowChild"));
            allowChildTemplates = builder.createContentPackage();
        }
    }
}

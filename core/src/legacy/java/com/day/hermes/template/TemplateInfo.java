/*
 * $Url: $
 * $Id: TemplateInfo.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import java.util.Iterator;
import java.util.LinkedList;

import com.day.hermes.legacy.DeliveryHttpServletRequest;
import com.day.hermes.script.ScriptInfo;

/**
 * The <code>TemplateInfo</code> abstract class defines an API to be implemented
 * by concrete template information objects.
 *
 * @author fmeschbe
 * @author tripod
 * @version $Rev$, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 *
 * @see com.day.hermes.template.TemplateInfoCache
 */
public abstract class TemplateInfo {

    /** The paths of the template (that were used so far) */
    private final LinkedList paths;

    //---------- Construction and base methods ---------------------------------

    /**
     * Constructs the base class. Only to be used by implementations of this
     * abstract class.
     */
    protected TemplateInfo() {
        paths = new LinkedList();
    }

    /**
     * Registers a new path to this template info
     *
     * @param path the path to add
     */
    protected void addPath(String path) {
        paths.add(path);
    }

    /**
     * Returns an <code>Iterator</code> over the known paths of this
     * <code>TemplateInfo</code>.
     *
     * @return <code>Iterator</code> over the path strings
     */
    protected Iterator paths() {
        return paths.iterator();
    }

    //---------- API -----------------------------------------------------------

    /**
     * Returns the {@link ScriptInfo} matching the query. The matching strategy
     * is dpendent on the implementation. The default implementation might
     * implement a first match strategy where first the base template may be
     * decide on a {@link ScriptInfo} to use and only then this
     * <code>TemplateInfo</code> checks its {@link ScriptInfo} instances in
     * configuration sequence.
     *
     * @param request The {@link DeliveryHttpServletRequest} for which to find
     *      the matching {@link ScriptInfo}.
     *
     * @return A {@link ScriptInfo} matching the request or <code>null</code>
     *      if no script info could be found.
     */
    public abstract ScriptInfo getScriptInfo(DeliveryHttpServletRequest request);

    /**
     * Returns an <code>Iterator</code> of the {@link ScriptInfo} instances of
     * this template. This <code>Iterator</code> should not include the
     * {@link ScriptInfo} instances of the base template.
     *
     * @return An <code>Iterator</code> of the {@link ScriptInfo} instances.
     */
    public abstract Iterator getScriptInfos();

    /**
     * Returns an <code>Iterator</code> of all the <code>TemplateInfo</code>
     * instances, for which this <code>TemplateInfo</code> is the (direct)
     * base template. When a <code>TemplateInfo</code> is instantiated from the
     * content page, it registers itself as a referrer to the base template.
     *
     * @return An <code>Iterator</code> of the <code>TemplateInfo</code>
     *      instances for which this <code>TemplateInfo</code> is the base
     *      template.
     */
    protected abstract Iterator references();

    /**
     * Returns the ContentBus handle of the content page, from which this
     * <code>TemplateInfo</code> instance was loaded.
     *
     * @return The ContentBus handle of the template page of this
     *      <code>TemplateInfo</code> instance.
     */
    public abstract String getHandle();

    /**
     * Returns <code>true</code> if it is allowed to create a page with this
     * template.
     *
     * @return <code>true</code> if it is allowed to create a page with this
     * template.
     */
    public abstract boolean isAllowedLocation(String handle);

    /**
     * Returns <code>true</code> if it is allowed to create a template with this
     * template below a page with the given template.
     *
     * @param templatePath The template handle to check, whether it is allowed
     *      for the parent page.
     *
     * @return <code>true</code> if the parent page is allowed to have the
     *      <code>templatePath</code>.
     */
    public abstract boolean isAllowedParentTemplate(String templatePath);

    /**
     * Returns <code>true</code> if it is allowed to create a child page with
     * the given template handle.
     *
     * @param templatePath The template handle to check, whether it is allowed
     *      for the child page.
     *
     * @return <code>true</code> if the <code>templatePath</code> template is
     *      allowed for a child page.
     */
    public abstract boolean isAllowedChildTemplate(String templatePath);
}

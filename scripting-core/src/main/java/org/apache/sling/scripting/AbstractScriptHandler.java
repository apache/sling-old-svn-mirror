/*
 * $Url: $
 * $Id: $
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
package org.apache.sling.scripting;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.component.Component;
import org.apache.sling.core.components.AbstractRepositoryComponent;

/**
 * The <code>AbstractScriptHandler</code> provides a helper method, which may
 * be called by implementations to resolve relative path names to scripts if the
 * scripts denote paths.
 */
public abstract class AbstractScriptHandler implements ScriptHandler {

    /**
     * Tries to resolve the script name relative to the component. If the script
     * name is not absolute, the path of the component (if it is an
     * AbstractRepositoryComponent) or the component ID is prepended. After that
     * the script name is assumed to be absolute and relative path elements (.
     * and ..) are resolved.
     *
     * @param component The Component relative to which the script name is made
     *            absolute if the name is relative.
     * @param scriptName The path to the script to resolve.
     * @return The resolved absolute script name.
     * @throws IllegalArgumentException if the scriptName is <code>null</code>
     *             or the empty string or if resolving relative path elements
     *             (..) would point higher than root.
     */
    protected String resolve(Component component, String scriptName) {
        // ensure script
        if (scriptName == null || scriptName.length() == 0) {
            throw new IllegalArgumentException("Missing Script Name");
        }

        // ensure absolute script path
        if (scriptName.charAt(0) != '/') {
            String root;
            if (component instanceof AbstractRepositoryComponent) {
                root = ((AbstractRepositoryComponent) component).getPath();
            } else {
                root = component.getId();
            }
            scriptName = root + "/" + scriptName;
        }

        // return now if there are no relative path elements
        if (scriptName.indexOf("/.") < 0) {
            return scriptName;
        }

        // resolve relative path elements
        String[] elements = scriptName.split("/");
        List<String> elementList = new ArrayList<String>();
        for (int i = 0; i < elements.length; i++) {
            if ("..".equals(elements[i])) {
                // remove trailing path element
                if (elementList.size() == 0) {
                    throw new IllegalArgumentException("Cannot resolve path "
                        + scriptName);
                }

                // remove last element from the list
                elementList.remove(elementList.size() - 1);
            } else if (!".".equals(elements[i])) {
                // just append if not "current path"
                elementList.add(elements[i]);
            }
        }

        // assemble the path elements to a path again
        StringBuffer pathBuf = new StringBuffer(scriptName.length());
        for (String element : elementList) {
            pathBuf.append('/').append(element);
        }

        // and return it
        return pathBuf.toString();
    }

}

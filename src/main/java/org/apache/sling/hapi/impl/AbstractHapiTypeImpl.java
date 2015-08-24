/*************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * __________________
 *
 *  Copyright 2014 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/
package org.apache.sling.hapi.impl;

import org.apache.sling.hapi.HApiProperty;
import org.apache.sling.hapi.HApiType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractHapiTypeImpl implements HApiType {

    public static final String ABSTRACT = "Abstract";
    private final String name;

    public AbstractHapiTypeImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return ABSTRACT;
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public String getFqdn() {
        return null;
    }

    @Override
    public List<String> getParameters() {
        return null;
    }

    @Override
    public Map<String, HApiProperty> getProperties() {
        return new HashMap<String, HApiProperty>();
    }

    @Override
    public Map<String, HApiProperty> getAllProperties() {
        return getProperties();
    }

    @Override
    public HApiType getParent() {
        return null;
    }

    @Override
    public boolean isAbstract() {
        return true;
    }
}

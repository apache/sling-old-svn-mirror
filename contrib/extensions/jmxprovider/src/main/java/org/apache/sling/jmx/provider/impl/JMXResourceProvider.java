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
package org.apache.sling.jmx.provider.impl;

import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.osgi.PropertiesUtil;

@Component(metatype=true,
    label="Apache Sling JMX Resource Provider",
    description="This provider mounts JMX mbeans into the resource tree.")
@Service(value = ResourceProvider.class)
@Properties({
    @Property(name = ResourceProvider.ROOTS, value="/system/sling/monitoring/mbeans",
            label="Root",
            description="The mount point of the JMX beans")
})
/**
 * Brief summary of a "good" object name:
 *
 * Object names:
 * - have a domain
 * - should have a type property
 * - could have a name property
 * - additional props are not recommended
 *
 * Path to an MBean:
 * {Domain}/{type property}/{name property}{all other props}
 * where
 * {Domain} : is a path consisting of the domain (dots replaced with slashes)
 * {type property} : is the value of the type property or "{notype}" if no type property is set
 * {name property} : is the value of the name property or "{noname}" if no name property is set
 * {all other props} : name/value pairs containing all additional props
 */
public class JMXResourceProvider implements ResourceProvider {

    /** Configured root paths, ending with a slash */
    private String[] rootsWithSlash;

    /** Configured root paths, not ending with a slash */
    private String[] roots;

    /** The mbean server. */
    private MBeanServer mbeanServer;

    @Activate
    protected void activate(final Map<String, Object> props) {
        final String paths[] = PropertiesUtil.toStringArray(props.get(ResourceProvider.ROOTS));
        final List<String> rootsList = new ArrayList<String>();
        final List<String> rootsWithSlashList = new ArrayList<String>();
        if ( paths != null ) {
            for(final String p : paths) {
                if ( p.length() > 0 ) {
                    if ( p.endsWith("/") ) {
                        rootsList.add(p.substring(0,  p.length() - 1));
                        rootsWithSlashList.add(p);
                    } else {
                        rootsList.add(p);
                        rootsWithSlashList.add(p + "/");
                    }
                }
            }
        }
        this.rootsWithSlash = rootsWithSlashList.toArray(new String[rootsWithSlashList.size()]);
        this.roots = rootsList.toArray(new String[rootsList.size()]);

        this.mbeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    @Deactivate
    protected void deactivate() {
        this.mbeanServer = null;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver, javax.servlet.http.HttpServletRequest, java.lang.String)
     */
    public Resource getResource(final ResourceResolver resourceResolver,
                                final HttpServletRequest request,
                                final String path) {
        return getResource(resourceResolver, path);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
     */
    public Resource getResource(final ResourceResolver resourceResolver,
                                final String path) {
        final PathInfo info = this.parse(path);
        if ( info != null ) {
            if ( info.isRoot ) {
                return new RootResource(resourceResolver, path);
            }
            if ( info.mbeanInfo == null ) {
                final Set<ObjectName> names = this.queryObjectNames(info.pathInfo);
                if ( names.size() != 0 ) {
                    return new RootResource(resourceResolver, path);
                }
            } else {
                if (info.pathInfo == null ) {
                    return new MBeanResource(this.mbeanServer, resourceResolver, this.convertObjectNameToResourcePath(info.objectName), path, info.mbeanInfo, info.objectName);
                }
                if ( info.pathInfo.equals("mbean:attributes") ) {
                    final MBeanResource parent = (MBeanResource)resourceResolver.getResource(ResourceUtil.getParent(path));
                    return new AttributesResource(resourceResolver, path, parent);
                }
                if ( info.pathInfo.startsWith("mbean:attributes/") ) {
                    final Resource parentRsrc = resourceResolver.getResource(ResourceUtil.getParent(path));
                    final AttributesResource parentAttributesResource;
                    final MBeanResource parentMBeanResource;
                    if ( parentRsrc instanceof AttributesResource ) {
                        parentAttributesResource = (AttributesResource) parentRsrc;
                        parentMBeanResource = (MBeanResource)parentRsrc.getParent();
                    } else {
                        final AttributeResource parent;
                        if ( parentRsrc instanceof AttributeResource) {
                            parent = (AttributeResource)parentRsrc;
                        } else {
                            parent = ((MapResource)parentRsrc).getAttributeResource();
                        }
                        parentAttributesResource = (AttributesResource) parent.getParent();
                        parentMBeanResource = (MBeanResource) parentAttributesResource.getParent();
                    }
                    final AttributeList result = parentMBeanResource.getAttributes();

                    final String attrPath = info.pathInfo.substring("mbean:attributes/".length());
                    final int pos = attrPath.indexOf('/');
                    final String attrName;
                    final String subPath;
                    if ( pos == -1 ) {
                        attrName = attrPath;
                        subPath = null;
                    } else {
                        attrName = attrPath.substring(0, pos);
                        subPath = attrPath.substring(pos + 1);
                    }
                    for(final MBeanAttributeInfo mai : info.mbeanInfo.getAttributes()) {
                        if ( mai.getName().equals(attrName) ) {
                            final Iterator iter = result.iterator();
                            Object value = null;
                            while ( iter.hasNext() && value == null ) {
                                final Attribute a = (Attribute) iter.next();
                                if ( a.getName().equals(attrName) ) {
                                    value = a.getValue();
                                }
                            }
                            final AttributeResource rsrc = new AttributeResource(resourceResolver, path, mai, value, parentAttributesResource);
                            if ( subPath != null ) {
                                return rsrc.getChildResource(subPath);
                            }
                            return rsrc;
                        }
                    }
                }
            }
        }
        return null;
    }

    private Set<ObjectName> queryObjectNames(final String prefix) {
        final Set<ObjectName> allNames = this.mbeanServer.queryNames(null, null);
        Set<ObjectName> names = allNames;
        if ( prefix != null ) {
            final String pathPrefix = prefix + '/';
            names = new HashSet<ObjectName>();
            for(final ObjectName name : allNames) {
                final String path = this.convertObjectNameToResourcePath(name);
                if ( path.startsWith(pathPrefix) ) {
                    names.add(name);
                }
            }
        }
        return names;
    }
    /**
     * @see org.apache.sling.api.resource.ResourceProvider#listChildren(org.apache.sling.api.resource.Resource)
     */
    public Iterator<Resource> listChildren(final Resource parent) {
        final PathInfo info = this.parse(parent.getPath());
        if ( info != null ) {
            if ( info.isRoot || info.mbeanInfo == null ) {
                // list all MBeans
                final Set<ObjectName> names = this.queryObjectNames(info.isRoot ? null : info.pathInfo);
                final Set<String> filteredNames = new HashSet<String>();
                final String prefix = (info.isRoot ? null : info.pathInfo + "/");
                for(final ObjectName name : names) {
                    final String path = this.convertObjectNameToResourcePath(name);
                    final String testName = (info.isRoot ? path : path.substring(prefix.length()));
                    final int sep = testName.indexOf('/');
                    if ( sep == -1 ) {
                        filteredNames.add(":" + name.getCanonicalName());
                    } else {
                        filteredNames.add(testName.substring(0, sep));
                    }
                }
                final List<String> sortedNames = new ArrayList<String>(filteredNames);

                Collections.sort(sortedNames);
                final Iterator<String> iter = sortedNames.iterator();
                return new Iterator<Resource>() {

                    private Resource next;

                    {
                        seek();
                    }

                    private void seek() {
                        while ( iter.hasNext() && this.next == null ) {
                            final String name = iter.next();
                            if ( name.startsWith(":") ) {
                                try {
                                    final ObjectName on = new ObjectName(name.substring(1));
                                    final MBeanInfo info = mbeanServer.getMBeanInfo(on);
                                    final String path = convertObjectNameToResourcePath(on);
                                    final int sep = path.lastIndexOf('/');
                                    this.next = new MBeanResource(mbeanServer, parent.getResourceResolver(), path, parent.getPath() + "/" + path.substring(sep + 1), info, on);
                                } catch (final IntrospectionException e) {
                                    // ignore
                                } catch (final InstanceNotFoundException e) {
                                    // ignore
                                } catch (final ReflectionException e) {
                                    // ignore
                                } catch (final MalformedObjectNameException e) {
                                    // ignore
                                }
                            } else {
                                this.next = new RootResource(parent.getResourceResolver(), parent.getPath() + '/' + name);
                            }
                        }
                    }

                    public boolean hasNext() {
                        return next != null;
                    }

                    public Resource next() {
                        if ( next != null ) {
                            final Resource rsrc = next;
                            this.next = null;
                            seek();
                            return rsrc;
                        }
                        throw new NoSuchElementException();
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("remove");
                    }
                };
            } else {
                if ( info.pathInfo == null ) {
                    final MBeanResource parentResource = (MBeanResource)parent;
                    final List<Resource> list = new ArrayList<Resource>();
                    list.add(new AttributesResource(parent.getResourceResolver(), parent.getPath() + "/mbean:attributes", parentResource));
                    return list.iterator();
                } else if ( info.pathInfo.equals("mbean:attributes") ) {
                    final AttributesResource parentResource = (AttributesResource)parent;
                    final MBeanResource parentMBeanResource = (MBeanResource)parentResource.getParent();
                    final AttributeList result = parentMBeanResource.getAttributes();

                    final MBeanAttributeInfo[] infos = info.mbeanInfo.getAttributes();
                    final Map<String, MBeanAttributeInfo> infoMap = new HashMap<String, MBeanAttributeInfo>();
                    for(final MBeanAttributeInfo i : infos) {
                        infoMap.put(i.getName(), i);
                    }
                    final Iterator iter = result.iterator();
                    return new Iterator<Resource>() {

                        public void remove() {
                            throw new UnsupportedOperationException("remove");
                        }

                        public Resource next() {
                            final Attribute attr = (Attribute)iter.next();
                            return new AttributeResource(parent.getResourceResolver(),
                                    parent.getPath() + "/" + attr.getName(),
                                    infoMap.get(attr.getName()),
                                    attr.getValue(),
                                    parentResource);
                        }

                        public boolean hasNext() {
                            return iter.hasNext();
                        }
                    };
                } else if ( info.pathInfo.startsWith("mbean:attributes/") ) {
                    final AttributeResource parentResource;
                    if ( parent instanceof AttributeResource ) {
                        parentResource = (AttributeResource)parent;
                    } else {
                        parentResource = ((MapResource)parent).getAttributeResource();
                    }

                    final String attrPath = info.pathInfo.substring("mbean:attributes/".length());
                    final int pos = attrPath.indexOf('/');
                    final String subPath;
                    if ( pos == -1 ) {
                        subPath = null;
                    } else {
                        subPath = attrPath.substring(pos + 1);
                    }

                    return parentResource.getChildren(parent.getPath(), subPath);

                }
            }
        }
        return null;
    }

    private static final String MARKER_NOTYPE = "{notype}";
    private static final String MARKER_NONAME = "{noname}";

    private String encode(final String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (final UnsupportedEncodingException uee) {
            // this should never happen, UTF-8 is always supported
            return value;
        }
    }

    private String decode(final String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (final UnsupportedEncodingException uee) {
            // this should never happen, UTF-8 is always supported
            return value;
        }
    }

    private String convertObjectNameToResourcePath(final ObjectName name) {
        final StringBuilder sb = new StringBuilder(name.getDomain().replace('.', '/'));
        sb.append('/');
        if ( name.getKeyProperty("type") != null ) {
            sb.append(encode(name.getKeyProperty("type")));
        } else {
            sb.append(MARKER_NOTYPE);
        }
        sb.append('/');
        if ( name.getKeyProperty("name") != null ) {
            sb.append(encode(name.getKeyProperty("name")));
        } else {
            sb.append(MARKER_NONAME);
        }
        final TreeMap<String, String> props = new TreeMap<String, String>(name.getKeyPropertyList());
        props.remove("name");
        props.remove("type");
        boolean first = true;
        for(final Map.Entry<String, String> entry : props.entrySet()) {
            if ( first ) {
                first = false;
                sb.append(':');
            } else {
                sb.append(',');
            }
            sb.append(encode(entry.getKey()));
            sb.append('=');
            sb.append(encode(entry.getValue()));
        }
        return sb.toString();
    }

    private ObjectName convertResourcePathToObjectName(final String path) {
        final int nameSlash = path.lastIndexOf('/');
        if ( nameSlash != -1 ) {
            final int typeSlash = path.lastIndexOf('/', nameSlash - 1);
            if ( typeSlash != -1 ) {
                final String domain = path.substring(0, typeSlash).replace('/', '.');
                final String type = decode(path.substring(typeSlash + 1, nameSlash));
                final String nameAndProps = path.substring(nameSlash + 1);
                final int colonPos = nameAndProps.indexOf(':');
                final String name;
                final String props;
                if ( colonPos == -1 ) {
                    name = decode(nameAndProps);
                    props = null;
                } else {
                    name = decode(nameAndProps.substring(0,  colonPos));
                    props = nameAndProps.substring(colonPos + 1);
                }
                final StringBuilder sb = new StringBuilder();
                sb.append(domain);
                sb.append(':');
                boolean hasProps = false;
                if ( !MARKER_NOTYPE.equals(type)) {
                    sb.append("type=");
                    sb.append(type);
                    hasProps = true;
                }
                if ( !MARKER_NONAME.equals(name) ) {
                    if ( hasProps ) {
                        sb.append(",");
                    }
                    sb.append("name=");
                    sb.append(name);
                    hasProps = true;
                }
                if ( props != null ) {
                    final String[] propArray = props.split(",");
                    for(final String keyValue : propArray) {
                        if ( hasProps ) {
                            sb.append(",");
                        }
                        final int pos = keyValue.indexOf('=');
                        if ( pos == -1 ) {
                            return null;
                        }
                        sb.append(decode(keyValue.substring(0, pos)));
                        sb.append('=');
                        sb.append(decode(keyValue.substring(pos+1)));
                    }
                }
                try {
                    return new ObjectName(sb.toString());
                } catch (final MalformedObjectNameException e) {
                    // ignore
                }
            }
        }
        return null;
    }

    public final static class PathInfo {

        public final boolean isRoot;
        public final String pathInfo;

        public ObjectName objectName;
        public MBeanInfo mbeanInfo;

        public PathInfo(final boolean isRoot) {
            this.isRoot = isRoot;
            this.pathInfo = null;
        }

        public PathInfo(final String info) {
            this.isRoot = false;
            this.pathInfo = info;
        }
    }

    /**
     * Parse the path
     */
    private PathInfo parse(final String path) {
        for(final String root : this.rootsWithSlash) {
            if ( path.startsWith(root) ) {
                final String subPath = path.substring(root.length());
                if ( subPath.length() == 0 ) {
                    return new PathInfo(true);
                }
                // MBean name / path
                String checkPath = subPath;
                String pathInfo = null;

                ObjectName objectName = null;
                MBeanInfo mbi = null;

                while ( checkPath.length() > 0 && mbi == null ) {
                    try {
                        objectName = this.convertResourcePathToObjectName(checkPath);
                        if ( objectName != null ) {
                            mbi = this.mbeanServer.getMBeanInfo(objectName);
                        }
                    } catch (final IntrospectionException e) {
                        // ignore
                    } catch (final InstanceNotFoundException e) {
                        // ignore
                    } catch (final ReflectionException e) {
                        // ignore
                    }
                    if ( mbi == null ) {
                        final int sep = checkPath.lastIndexOf('/');
                        if ( sep == -1 ) {
                            checkPath = "";
                            pathInfo = subPath;
                        } else {
                            checkPath = checkPath.substring(0, sep);
                            pathInfo = subPath.substring(sep + 1);
                        }
                    }
                }
                final PathInfo info = new PathInfo(pathInfo);
                if ( mbi != null ) {
                    info.objectName = objectName;
                    info.mbeanInfo = mbi;
                }
                return info;
            }
        }
        for(final String root : this.roots) {
            if ( path.equals(root) ) {
                return new PathInfo(true);
            }
        }
        return null;

    }
}

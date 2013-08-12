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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

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
import org.apache.sling.commons.osgi.PropertiesUtil;

@Component
@Service(value = ResourceProvider.class)
@Properties({
    @Property(name = ResourceProvider.ROOTS, value="/system/sling/monitoring/mbeans")
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
                    return new MBeanResource(resourceResolver, this.convertObjectNameToResourcePath(info.objectName), path, info.mbeanInfo, info.objectName);
                }
                if ( info.pathInfo.equals("attributes") ) {
                    return new AttributesResource(resourceResolver, path);
                }
                if ( info.pathInfo.startsWith("attributes/") ) {
                    final String attrName = info.pathInfo.substring(11);
                    if ( attrName.indexOf('/') == - 1) {
                        for(final MBeanAttributeInfo mai : info.mbeanInfo.getAttributes()) {
                            if ( mai.getName().equals(attrName) ) {
                                return new AttributeResource(mbeanServer, info.objectName, resourceResolver, path, mai);
                            }
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
                                    this.next = new MBeanResource(parent.getResourceResolver(), path, parent.getPath() + "/" + path.substring(sep + 1), info, on);
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
                    final List<Resource> list = new ArrayList<Resource>();
                    list.add(new AttributesResource(parent.getResourceResolver(), parent.getPath() + "/attributes"));
                    return list.iterator();
                } else if ( info.pathInfo.equals("attributes") ) {
                    final MBeanAttributeInfo[] infos = info.mbeanInfo.getAttributes();
                    final List<MBeanAttributeInfo> list = Arrays.asList(infos);
                    final Iterator<MBeanAttributeInfo> iter = list.iterator();
                    return new Iterator<Resource>() {

                        public void remove() {
                            throw new UnsupportedOperationException("remove");
                        }

                        public Resource next() {
                            final MBeanAttributeInfo mai = iter.next();
                            return new AttributeResource(mbeanServer, info.objectName, parent.getResourceResolver(), parent.getPath() + "/" + mai.getName(), mai);
                        }

                        public boolean hasNext() {
                            return iter.hasNext();
                        }
                    };
                }
            }
        }
        return null;
    }

    private static final String MARKER_NOTYPE = "{notype}";
    private static final String MARKER_NONAME = "{noname}";

    private String convertObjectNameToResourcePath(final ObjectName name) {
        final StringBuilder sb = new StringBuilder(name.getDomain().replace('.', '/'));
        sb.append('/');
        if ( name.getKeyProperty("type") != null ) {
            sb.append(name.getKeyProperty("type"));
        } else {
            sb.append(MARKER_NOTYPE);
        }
        sb.append('/');
        if ( name.getKeyProperty("name") != null ) {
            sb.append(name.getKeyProperty("name"));
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
            sb.append(entry.getKey());
            sb.append('=');
            sb.append(entry.getValue());
        }
        return sb.toString();
    }

    private ObjectName convertResourcePathToObjectName(final String path) {
        final int nameSlash = path.lastIndexOf('/');
        if ( nameSlash != -1 ) {
            final int typeSlash = path.lastIndexOf('/', nameSlash - 1);
            if ( typeSlash != -1 ) {
                final String domain = path.substring(0, typeSlash).replace('/', '.');
                final String type = path.substring(typeSlash + 1, nameSlash);
                final String nameAndProps = path.substring(nameSlash + 1);
                final int colonPos = nameAndProps.indexOf(':');
                final String name;
                final String props;
                if ( colonPos == -1 ) {
                    name = nameAndProps;
                    props = null;
                } else {
                    name = nameAndProps.substring(0,  colonPos);
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
                    if ( hasProps ) {
                        sb.append(",");
                    }
                    sb.append(props);
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

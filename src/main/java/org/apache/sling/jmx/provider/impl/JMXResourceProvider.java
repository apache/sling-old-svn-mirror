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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

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
public class JMXResourceProvider implements ResourceProvider {

    private String[] rootsWithSlash;

    private String[] roots;

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
            try {
                final ObjectName on = new ObjectName(info.mbeanName);
                final MBeanInfo mbi = this.mbeanServer.getMBeanInfo(on);
                if (info.pathInfo == null ) {
                    return new MBeanResource(resourceResolver, path, mbi);
                }
                if ( info.pathInfo.equals("attributes") ) {
                    return new AttributesResource(resourceResolver, path);
                }
                if ( info.pathInfo.startsWith("attributes/") ) {
                    final String attrName = info.pathInfo.substring(11);
                    if ( attrName.indexOf('/') == - 1) {
                        for(final MBeanAttributeInfo mai : mbi.getAttributes()) {
                            if ( mai.getName().equals(attrName) ) {
                                return new AttributeResource(mbeanServer, on, resourceResolver, path, mai);
                            }
                        }
                    }
                }
            } catch (final MalformedObjectNameException e) {
                // ignore
            } catch (final IntrospectionException e) {
                // ignore
            } catch (final InstanceNotFoundException e) {
                // ignore
            } catch (final ReflectionException e) {
                // ignore
            }
        }
        return null;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceProvider#listChildren(org.apache.sling.api.resource.Resource)
     */
    public Iterator<Resource> listChildren(final Resource parent) {
        final PathInfo info = this.parse(parent.getPath());
        if ( info != null ) {
            if ( info.isRoot ) {
                // list all mbeans
                final Set<ObjectName> beans = this.mbeanServer.queryNames(null, null);
                final List<ObjectName> sortedBeans = new ArrayList<ObjectName>(beans);
                Collections.sort(sortedBeans);
                final Iterator<ObjectName> iter = sortedBeans.iterator();
                return new Iterator<Resource>() {

                    private Resource next;

                    {
                        seek();
                    }

                    private void seek() {
                        while ( iter.hasNext() ) {
                            final ObjectName on = iter.next();
                            try {
                                final MBeanInfo info = mbeanServer.getMBeanInfo(on);
                                this.next = new MBeanResource(parent, on.getCanonicalName(), info);
                                break;
                            } catch (final IntrospectionException e) {
                                // ignore
                            } catch (final InstanceNotFoundException e) {
                                // ignore
                            } catch (final ReflectionException e) {
                                // ignore
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
                try {
                    final ObjectName on = new ObjectName(info.mbeanName);
                    final MBeanInfo mbi = this.mbeanServer.getMBeanInfo(on);
                    if ( info.pathInfo == null ) {
                        final List<Resource> list = new ArrayList<Resource>();
                        list.add(new AttributesResource(parent.getResourceResolver(), parent.getPath() + "/attributes"));
                        return list.iterator();
                    } else if ( info.pathInfo.equals("attributes") ) {
                        final MBeanAttributeInfo[] infos = mbi.getAttributes();
                        final List<MBeanAttributeInfo> list = Arrays.asList(infos);
                        final Iterator<MBeanAttributeInfo> iter = list.iterator();
                        return new Iterator<Resource>() {

                            public void remove() {
                                throw new UnsupportedOperationException("remove");
                            }

                            public Resource next() {
                                final MBeanAttributeInfo mai = iter.next();
                                return new AttributeResource(mbeanServer, on, parent.getResourceResolver(), parent.getPath() + "/" + mai.getName(), mai);
                            }

                            public boolean hasNext() {
                                return iter.hasNext();
                            }
                        };
                    }
                } catch (final MalformedObjectNameException e) {
                    // ignore
                } catch (final IntrospectionException e) {
                    // ignore
                } catch (final InstanceNotFoundException e) {
                    // ignore
                } catch (final ReflectionException e) {
                    // ignore
                }
            }
        }
        return null;
    }

    public final static class PathInfo {
        public final boolean isRoot;
        public final String mbeanName;
        public final String pathInfo;

        public PathInfo(final boolean isRoot) {
            this.isRoot = isRoot;
            this.mbeanName = null;
            this.pathInfo = null;
        }

        public PathInfo(final String name, final String info) {
            this.isRoot = false;
            this.mbeanName = name;
            this.pathInfo = info;
        }
    }

    private PathInfo parse(final String path) {
        for(final String root : this.rootsWithSlash) {
            if ( path.startsWith(root) ) {
                final String subPath = path.substring(root.length());
                if ( subPath.length() == 0 ) {
                    return new PathInfo(true);
                }
                // mbean name
                final int sep = subPath.indexOf('/');
                final String mbeanName;
                final String pathInfo;
                if ( sep == -1 ) {
                    mbeanName = subPath;
                    pathInfo = null;
                } else {
                    mbeanName = subPath.substring(0, sep);
                    pathInfo = subPath.substring(sep + 1);
                }
                return new PathInfo(mbeanName, pathInfo);
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

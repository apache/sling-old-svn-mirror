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
package org.apache.sling.distribution.packaging.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Package related utility methods
 */
public class DistributionPackageUtils {

    private static final Logger log = LoggerFactory.getLogger(DistributionPackageUtils.class);

    private final static String META_START = "DSTRPACKMETA";

    private static final Object repolock = new Object();
    private static final Object filelock = new Object();

    public final static String PROPERTY_REMOTE_PACKAGE_ID = "remote.package.id";

    /**
     * distribution package origin queue
     */
    private static final String PACKAGE_INFO_PROPERTY_ORIGIN_QUEUE = "internal.origin.queue";

    /**
     * distribution request user
     */
    public static final String PACKAGE_INFO_PROPERTY_REQUEST_USER = "internal.request.user";

    public static final String PACKAGE_INFO_PROPERTY_REQUEST_ID = "internal.request.id";

    public static final String PACKAGE_INFO_PROPERTY_REQUEST_START_TIME = "internal.request.startTime";

    /**
     * Acquires the package if it's a {@link SharedDistributionPackage}, via {@link SharedDistributionPackage#acquire(String[])}
     * @param distributionPackage a distribution package
     * @param queueNames the name of the queue in which the package should be acquired
     */
    public static void acquire(DistributionPackage distributionPackage, String... queueNames) {
        if (distributionPackage instanceof SharedDistributionPackage) {
            ((SharedDistributionPackage) distributionPackage).acquire(queueNames);
        }
    }

    /**
     * Releases the package if it's a {@link SharedDistributionPackage}, via {@link SharedDistributionPackage#release(String[])}
     * @param distributionPackage a distribution package
     * @param queueNames the name of the queue in which the package should be released
     */
    public static void release(DistributionPackage distributionPackage, String... queueNames) {
        if (distributionPackage instanceof SharedDistributionPackage) {
            ((SharedDistributionPackage) distributionPackage).release(queueNames);
        }
    }

    /**
     * Releases a distribution package if it's a {@link SharedDistributionPackage}, otherwise deletes it.
     * @param distributionPackage a distribution package
     * @param queueNames the name of the queue from which it should be eventually released
     */
    public static void releaseOrDelete(DistributionPackage distributionPackage, String... queueNames) {
        if (distributionPackage == null) {
            return;
        }
        try {
            if (distributionPackage instanceof SharedDistributionPackage) {
                if (queueNames != null) {
                    ((SharedDistributionPackage) distributionPackage).release(queueNames);
                    log.debug("package {} released from queue {}", distributionPackage.getId(), queueNames);
                } else {
                    log.error("package {} cannot be released from null queue", distributionPackage.getId());
                }
            } else {
                deleteSafely(distributionPackage);
                log.debug("package {} deleted", distributionPackage.getId());
            }
        } catch (Throwable t) {
            log.error("cannot release package {}", t);
        }
    }

    /**
     * Delete a distribution package, if deletion fails, ignore it
     * @param distributionPackage the package to delete
     */
    public static void deleteSafely(DistributionPackage distributionPackage) {
        if (distributionPackage != null) {
            try {
                distributionPackage.delete();
            } catch (Throwable t) {
                log.error("error deleting package", t);
            }
        }
    }

    public static void closeSafely(DistributionPackage distributionPackage) {
        if (distributionPackage != null) {
            try {
                distributionPackage.close();
            } catch (Throwable t) {
                log.error("error closing package", t);
            }
        }
    }

    /**
     * Create a queue item out of a package
     * @param distributionPackage a distribution package
     * @return a distribution queue item
     */
    public static DistributionQueueItem toQueueItem(DistributionPackage distributionPackage) {
        return new DistributionQueueItem(distributionPackage.getId(), distributionPackage.getSize(), distributionPackage.getInfo());
    }

    /**
     * Create a {@link DistributionPackageInfo} from a queue item
     * @param queueItem a distribution queue item
     * @return a {@link DistributionPackageInfo}
     */
    public static DistributionPackageInfo fromQueueItem(DistributionQueueItem queueItem) {
        String type = queueItem.get(DistributionPackageInfo.PROPERTY_PACKAGE_TYPE, String.class);
        return new DistributionPackageInfo(type, queueItem);
    }

    public static String getQueueName(DistributionPackageInfo packageInfo) {
        return packageInfo.get(PACKAGE_INFO_PROPERTY_ORIGIN_QUEUE, String.class);
    }

    public static void mergeQueueEntry(DistributionPackageInfo packageInfo, DistributionQueueEntry entry) {
        packageInfo.putAll(entry.getItem());
        packageInfo.put(PACKAGE_INFO_PROPERTY_ORIGIN_QUEUE, entry.getStatus().getQueueName());
    }


    public static void fillInfo(DistributionPackageInfo info, DistributionRequest request) {
        info.put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, request.getRequestType());
        info.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, request.getPaths());
        info.put(DistributionPackageInfo.PROPERTY_REQUEST_DEEP_PATHS, getDeepPaths(request));
    }

    private static String[] getDeepPaths(DistributionRequest request) {
        List<String> deepPaths = new ArrayList<String>();
        for (String path : request.getPaths()) {
            if (request.isDeep(path)) {
                deepPaths.add(path);
            }
        }

        return deepPaths.toArray(new String[deepPaths.size()]);
    }

    public static InputStream createStreamWithHeader(DistributionPackage distributionPackage) throws IOException {

        DistributionPackageInfo packageInfo = distributionPackage.getInfo();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Map<String, Object> headerInfo = new HashMap<String, Object>();
        headerInfo.put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, packageInfo.getRequestType());
        headerInfo.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, packageInfo.getPaths());
        headerInfo.put(PROPERTY_REMOTE_PACKAGE_ID, distributionPackage.getId());
        if (packageInfo.containsKey("reference-required")) {
            headerInfo.put("reference-required", packageInfo.get("reference-required"));
            log.info("setting reference-required to {}", packageInfo.get("reference-required"));
        }
        writeInfo(outputStream, headerInfo);

        InputStream headerStream = new ByteArrayInputStream(outputStream.toByteArray());
        InputStream bodyStream = distributionPackage.createInputStream();
        return new SequenceInputStream(headerStream, bodyStream);
    }

    public static void readInfo(InputStream inputStream, Map<String, Object> info) {

        try {
            int size = META_START.getBytes("UTF-8").length;
            inputStream.mark(size);
            byte[] buffer = new byte[size];
            int bytesRead = inputStream.read(buffer, 0, size);
            String s = new String(buffer, "UTF-8");

            if (bytesRead > 0 && buffer[0] > 0 && META_START.equals(s)) {
                ObjectInputStream stream = getSafeObjectInputStream(inputStream);

                HashMap<String, Object> map = (HashMap<String, Object>) stream.readObject();
                info.putAll(map);
            } else {
                inputStream.reset();
            }
        } catch (IOException e) {
            log.error("Cannot read stream info", e);
        } catch (ClassNotFoundException e) {
            log.error("Cannot read stream info", e);
        }

    }

    public static void writeInfo(OutputStream outputStream, Map<String, Object> info) {

        HashMap<String, Object> map = new HashMap<String, Object>(info);


        try {
            outputStream.write(META_START.getBytes("UTF-8"));

            ObjectOutputStream stream = new ObjectOutputStream(outputStream);

            stream.writeObject(map);

        } catch (IOException e) {
            log.error("Cannot read stream info", e);
        }
    }

    public static Resource getPackagesRoot(ResourceResolver resourceResolver, String packagesRootPath) throws PersistenceException {
        Resource packagesRoot = resourceResolver.getResource(packagesRootPath);

        if (packagesRoot != null) {
            return packagesRoot;
        }

        synchronized (repolock) {
            if (resourceResolver.hasChanges()) {
                resourceResolver.refresh();
            }
            packagesRoot = ResourceUtil.getOrCreateResource(resourceResolver, packagesRootPath, "sling:Folder", "sling:Folder", true);
        }

        return packagesRoot;
    }

    public static InputStream getStream(Resource resource) throws RepositoryException {
        Node parent = resource.adaptTo(Node.class);
        return parent.getProperty("bin/jcr:content/jcr:data").getBinary().getStream();
    }

    public static void uploadStream(Resource resource, InputStream stream) throws RepositoryException {
        Node parent = resource.adaptTo(Node.class);
        Node file = JcrUtils.getOrAddNode(parent, "bin", NodeType.NT_FILE);
        Node content = JcrUtils.getOrAddNode(file, Node.JCR_CONTENT, NodeType.NT_RESOURCE);
        Binary binary = parent.getSession().getValueFactory().createBinary(stream);
        content.setProperty(Property.JCR_DATA, binary);
        Node refs = JcrUtils.getOrAddNode(parent, "refs", NodeType.NT_UNSTRUCTURED);
    }


    public static void acquire(Resource resource, @Nonnull String[] holderNames) throws RepositoryException {
        if (holderNames.length == 0) {
            throw new IllegalArgumentException("holder name cannot be null or empty");
        }

        Node parent = resource.adaptTo(Node.class);

        Node refs = parent.getNode("refs");

        for (String holderName : holderNames) {
            if (!refs.hasNode(holderName)) {
                refs.addNode(holderName, NodeType.NT_UNSTRUCTURED);
            }
        }
    }

    public static boolean release(Resource resource, @Nonnull String[] holderNames) throws RepositoryException {
        if (holderNames.length == 0) {
            throw new IllegalArgumentException("holder name cannot be null or empty");
        }

        Node parent = resource.adaptTo(Node.class);

        Node refs = parent.getNode("refs");

        for (String holderName : holderNames) {
            Node refNode = refs.getNode(holderName);
            if (refNode != null) {
                refNode.remove();
            }
        }

        if (!refs.hasNodes()) {
            refs.remove();
            return true;
        }

        return false;
    }

    public static void acquire(File file, @Nonnull String[] holderNames) throws IOException {

        if (holderNames.length == 0) {
            throw new IllegalArgumentException("holder name cannot be null or empty");
        }

        synchronized (filelock) {
            try {
                HashSet<String> set = new HashSet<String>();

                if (file.exists()) {
                    ObjectInputStream inputStream = getSafeObjectInputStream(new FileInputStream(file));
                    set = (HashSet<String>) inputStream.readObject();
                    IOUtils.closeQuietly(inputStream);
                }

                set.addAll(Arrays.asList(holderNames));

                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
                outputStream.writeObject(set);
                IOUtils.closeQuietly(outputStream);

            } catch (ClassNotFoundException e) {
                log.error("Cannot release file", e);
            }
        }


    }

    public static boolean release(File file, @Nonnull String[] holderNames) throws IOException {
        if (holderNames.length == 0) {
            throw new IllegalArgumentException("holder name cannot be null or empty");
        }

        synchronized (filelock) {
            try {

                HashSet<String> set = new HashSet<String>();

                if (file.exists()) {
                    ObjectInputStream inputStream = getSafeObjectInputStream(new FileInputStream(file));
                    set = (HashSet<String>) inputStream.readObject();
                    IOUtils.closeQuietly(inputStream);
                }

                set.removeAll(Arrays.asList(holderNames));

                if (set.isEmpty()) {
                    FileUtils.deleteQuietly(file);
                    return true;
                }

                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
                outputStream.writeObject(set);
                IOUtils.closeQuietly(outputStream);
            }
            catch (ClassNotFoundException e) {
                log.error("Cannot release file", e);
            }
        }
        return false;
    }

    private static ObjectInputStream getSafeObjectInputStream(InputStream inputStream) throws IOException {

        final Class[] acceptedClasses = new Class[] {
                HashMap.class, HashSet.class,
                String.class, String[].class,
                Long.class,
                Number.class,
                Boolean.class,
                Enum.class,
                DistributionRequestType.class
        };

        return new ObjectInputStream(inputStream) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass osc) throws IOException, ClassNotFoundException {
                String className = osc.getName();
                for (Class clazz : acceptedClasses) {
                    if (clazz.getName().equals(className)) {
                        return super.resolveClass(osc);
                    }
                }

                throw new InvalidClassException("Class name not accepted: " + className);
            }
        };

        // TODO: replace with the following lines when switching to commons-io-2.5
        //        return new ValidatingObjectInputStream(inputStream)
        //                .accept(acceptedClasses);
    }
}

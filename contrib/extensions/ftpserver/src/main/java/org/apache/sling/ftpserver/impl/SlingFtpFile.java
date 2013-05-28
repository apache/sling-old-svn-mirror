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
package org.apache.sling.ftpserver.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.LoggerFactory;

public class SlingFtpFile implements FtpFile {

    private final ResourceResolver resolver;

    private Resource resource;

    private final String absPath;

    private final String name;

    public SlingFtpFile(final Resource resource) {
        this.resolver = resource.getResourceResolver();
        this.resource = resource;
        this.absPath = this.resource.getPath();
        this.name = this.resource.getName();
    }

    public SlingFtpFile(final String absPath, final ResourceResolver resolver) {
        this.resolver = resolver;
        this.resource = null;
        this.absPath = absPath;
        this.name = ResourceUtil.getName(absPath);
    }

    public InputStream createInputStream(long offset) throws IOException {
        if (offset != 0) {
            throw new IOException("random access not supported");
        }
        return this.resource.adaptTo(InputStream.class);
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        if (offset != 0) {
            throw new IOException("random access not supported");
        }

        Resource content = getContent();
        if (content != null) {
            return new SlingFtpOutputStream(content);
        }

        throw new IOException("Cannot create OutputStream to " + this.getAbsolutePath());
    }

    public boolean delete() {
        if (this.resource != null) {
            try {
                this.resolver.delete(this.resource);
                this.resolver.commit();
                return true;
            } catch (PersistenceException pe) {
                LoggerFactory.getLogger(getClass()).error("delete: Failed removing", pe);
            }
        }

        return false;
    }

    public boolean doesExist() {
        return this.resource != null;
    }

    public String getAbsolutePath() {
        return this.absPath;
    }

    public String getGroupName() {
        // no owner groups
        return "nobody";
    }

    public long getLastModified() {
        long time = this.resource.getResourceMetadata().getModificationTime();
        if (time < 0) {
            time = this.resource.getResourceMetadata().getCreationTime();
            if (time < 0) {
                time = System.currentTimeMillis();
            }
        }
        return time;
    }

    public int getLinkCount() {
        // number of children from listFiles()
        return listFiles().size();
    }

    public String getName() {
        return this.name;
    }

    public String getOwnerName() {
        // no owner name
        return "nobody";
    }

    public long getSize() {
        return this.resource.getResourceMetadata().getContentLength();
    }

    public boolean isDirectory() {
        try {
            InputStream ins = this.createInputStream(0);
            if (ins == null) {
                return true;
            }

            ins.close();
        } catch (IOException ignore) {
        }

        return false;
    }

    public boolean isFile() {
        return !isDirectory();
    }

    public boolean isHidden() {
        // we don't hide what is not hidden by ACL
        return false;
    }

    public boolean isReadable() {
        return true;
    }

    public boolean isRemovable() {
        // TODO Consider access control !!
        return true;
    }

    public boolean isWritable() {
        // TODO Consider access control !!
        return true;
    }

    public List<FtpFile> listFiles() {
        Iterator<Resource> children = this.resource.listChildren();
        ArrayList<FtpFile> list = new ArrayList<FtpFile>();
        while (children.hasNext()) {
            Resource child = children.next();
            if (!"jcr:content".equals(child.getName())) {
                list.add(new SlingFtpFile(child));
            }
        }
        return list;
    }

    public boolean mkdir() {
        if (!doesExist()) {
            try {
                this.resource = create(this.absPath, "sling:Folder");
                this.resolver.commit();
                return this.resource != null;
            } catch (PersistenceException e) {
                // TODO Auto-generated catch block
            }
        }

        // already exists or error creating
        return false;
    }

    public boolean move(FtpFile arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean setLastModified(long arg0) {
        Resource content = getContent();
        if (content != null) {
            ModifiableValueMap map = content.adaptTo(ModifiableValueMap.class);
            if (map != null) {
                map.put("jcr:lastModified", arg0);
                try {
                    this.resolver.commit();
                    return true;
                } catch (PersistenceException e) {
                    // TODO: handle
                }
            }
        }

        // error fallback
        return false;
    }

    @Override
    public String toString() {
        return this.getAbsolutePath();
    }

    @SuppressWarnings("serial")
    private Resource create(final String path, final String type) throws PersistenceException {
        Resource parent = this.resolver.getResource(ResourceUtil.getParent(path));
        if (parent != null) {
            return this.resolver.create(parent, this.getName(), new HashMap<String, Object>() {
                {
                    put("jcr:primaryType", type);
                }
            });
        }

        return null;
    }

    @SuppressWarnings("serial")
    private Resource createFile(final String path) throws PersistenceException {
        Resource file = this.create(path, "nt:file");
        if (file != null) {
            return this.resolver.create(file, "jcr:content", new HashMap<String, Object>() {
                {
                    put("jcr:primaryType", "nt:unstructured");
                }
            });
        }

        return null;
    }

    private Resource getContent() {
        if (this.resource == null) {
            // create the resource ??
            try {
                return createFile(this.absPath);
            } catch (PersistenceException e) {
                // TODO Auto-generated catch block
            }
        } else {
            Resource content = this.resource.getChild("jcr:content");
            if (content == null) {
                content = this.resource;
            }
            return content;
        }

        return null;
    }
}

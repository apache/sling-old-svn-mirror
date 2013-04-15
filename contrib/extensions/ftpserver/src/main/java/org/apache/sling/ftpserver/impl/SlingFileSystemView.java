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

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

public class SlingFileSystemView implements FileSystemView {

    private final ResourceResolver resolver;

    private FtpFile cwd;

    public SlingFileSystemView(final ResourceResolver resolver) {
        this.resolver = resolver;
        this.cwd = getHomeDirectory();
    }

    public boolean changeWorkingDirectory(String wd) {
        final String cwd;
        if (wd.startsWith("/")) {
            cwd = wd;
        } else {
            cwd = this.cwd.getAbsolutePath() + "/" + wd;
        }

        FtpFile cwdFile = getFile(cwd);
        if (cwdFile.doesExist()) {
            this.cwd = cwdFile;
            return true;
        }

        return false;
    }

    public void dispose() {
        this.resolver.close();
    }

    public FtpFile getFile(String path) {
        if (!path.startsWith("/")) {
            path = this.cwd.getAbsolutePath() + "/" + path;
        }

        path = ResourceUtil.normalize(path);
        Resource res = this.resolver.getResource(path);
        if (res != null) {
            return new SlingFtpFile(res);
        }

        return new SlingFtpFile(path, this.resolver);
    }

    public FtpFile getHomeDirectory() {
        return getFile("/");
    }

    public FtpFile getWorkingDirectory() {
        return this.cwd;
    }

    public boolean isRandomAccessible() {
        // only stream access to data
        return false;
    }

}

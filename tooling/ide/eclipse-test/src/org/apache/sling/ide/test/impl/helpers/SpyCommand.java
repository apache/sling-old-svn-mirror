/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.test.impl.helpers;

import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;

/**
 * The <tt>SpyCommand</tt> records the arguments passed to it and can be used to verify that the invocation is made with
 * the right parameters
 *
 * @param <T>
 */
public class SpyCommand<T> implements Command<T> {

    public enum Kind {
        ADD_OR_UPDATE;
    }

    private final ResourceProxy resourceProxy;
    private final FileInfo fileInfo;
    private final String path;
    private final SpyCommand.Kind kind;

    public SpyCommand(ResourceProxy resourceProxy, FileInfo fileInfo, String path, SpyCommand.Kind kind) {
        this.resourceProxy = resourceProxy;
        this.fileInfo = fileInfo;
        this.path = path;
        this.kind = kind;
    }

    @Override
    public Result<T> execute() {
        throw new UnsupportedOperationException("Unable to execute a " + getClass().getSimpleName());
    }

    @Override
    public String getPath() {
        return path;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public ResourceProxy getResourceProxy() {
        return resourceProxy;
    }

    public SpyCommand.Kind getKind() {
        return kind;
    }
}
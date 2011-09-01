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
package org.apache.sling.launchpad.testservices.handlers;

import org.apache.jackrabbit.server.io.ExportContext;
import org.apache.jackrabbit.server.io.IOManager;
import org.apache.jackrabbit.server.io.ImportContext;
import org.apache.jackrabbit.server.io.PropertyExportContext;
import org.apache.jackrabbit.server.io.PropertyImportContext;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.property.PropEntry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Map;

/**
 * This handler can export only if the path given in the context contains the handler name.
 */
public abstract class AbstractHandler implements
        org.apache.jackrabbit.server.io.IOHandler,
        org.apache.jackrabbit.server.io.PropertyHandler {

    private static final String HANDLER_IDENTIFIER = "handler-identifier";

    private IOManager ioManager ;

    // IOHandler

    public IOManager getIOManager() {
        return ioManager;
    }

    public void setIOManager(IOManager ioManager) {
        this.ioManager = ioManager;
    }

    public String getName() {
        return getClass().getName();
    }

    public boolean canImport(ImportContext context, DavResource davResource) {
        return canImport(context, davResource.isCollection());
    }

    public boolean canImport(ImportContext context, boolean b) {
        return false;
    }

    public boolean importContent(ImportContext context, DavResource davResource) throws IOException {
        return importContent(context, davResource.isCollection());
    }

    public boolean importContent(ImportContext context, boolean b) throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean canExport(ExportContext context, boolean b) {
        try {
            final Node node = (Node) context.getExportRoot();
            return matches(node.getPath());
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean canExport(ExportContext context, DavResource davResource) {
        return canExport(context, davResource.isCollection());
    }

    public boolean exportContent(ExportContext context, DavResource davResource) throws IOException {
        return exportContent(context, davResource.isCollection());
    }

    public boolean exportContent(ExportContext context, boolean b) throws IOException {
        context.setProperty(HANDLER_IDENTIFIER, getIdentifier());
        return true;
    }

    // PropertyHandler

    public boolean canExport(PropertyExportContext context, boolean b) {
        try {
            final Node node = (Node) context.getExportRoot();
            return matches(node.getPath());
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean exportProperties(PropertyExportContext context, boolean b) throws RepositoryException {
        context.setProperty(HANDLER_IDENTIFIER, getIdentifier());
        return true;
    }

    public boolean canImport(PropertyImportContext context, boolean b) {
        return false;
    }

    public Map<? extends PropEntry, ?> importProperties(PropertyImportContext context, boolean b) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    //

    public String getIdentifier() {
        return getHandlerName() + "-";
    }

    public boolean matches(String path) {
        return path != null && path.contains(getHandlerName());
    }

    //

    public abstract String getHandlerName() ;

}
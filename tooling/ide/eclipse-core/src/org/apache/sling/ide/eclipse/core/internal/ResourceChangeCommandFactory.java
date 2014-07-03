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
package org.apache.sling.ide.eclipse.core.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.apache.sling.ide.eclipse.core.ResourceUtil;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterResult;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.ResourceProxy;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

/**
 * The <tt>ResourceChangeCommandFactory</tt> creates new {@link #Command commands} correspoding to resource addition,
 * change, or removal
 *
 */
public class ResourceChangeCommandFactory {

    private final Set<String> ignoredFileNames = new HashSet<String>();
    {
        ignoredFileNames.add(".vlt");
        ignoredFileNames.add(".vltignore");
    }

    private final SerializationManager serializationManager;

    public ResourceChangeCommandFactory(SerializationManager serializationManager) {
        this.serializationManager = serializationManager;
    }

    public Command<?> newCommandForAddedOrUpdated(Repository repository, IResource addedOrUpdated) throws CoreException {
        try {
            return addFileCommand(repository, addedOrUpdated);
        } catch (SerializationException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed updating " + addedOrUpdated,
                    e));
        } catch (IOException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed updating " + addedOrUpdated,
                    e));
        }
    }

    private Command<?> addFileCommand(Repository repository, IResource resource) throws SerializationException,
            CoreException, IOException {

        if (ignoredFileNames.contains(resource.getName())) {
            return null;
        }

        Object ignoreNextUpdate = resource.getSessionProperty(ResourceUtil.QN_IGNORE_NEXT_CHANGE);
        if (ignoreNextUpdate != null) {
            resource.setSessionProperty(ResourceUtil.QN_IGNORE_NEXT_CHANGE, null);
            return null;
        }

        if (resource.isTeamPrivateMember(IResource.CHECK_ANCESTORS)) {
            Activator.getDefault().getPluginLogger().trace("Skipping team-private resource {0}", resource);
            return null;
        }

        FileInfo info = createFileInfo(resource, repository);
        Activator.getDefault().getPluginLogger().trace("For {0} built fileInfo {1}", resource, info);
        if (info == null) {
            return null;
        }

        File syncDirectoryAsFile = ProjectUtil.getSyncDirectoryFullPath(resource.getProject()).toFile();
        IFolder syncDirectory = ProjectUtil.getSyncDirectory(resource.getProject());

        if (serializationManager.isSerializationFile(info.getLocation())) {
            InputStream contents = null;
            try {
                IFile file = (IFile) resource;
                contents = file.getContents();
                String resourceLocation = file.getFullPath().makeRelativeTo(syncDirectory.getFullPath())
                        .toPortableString();
                ResourceProxy resourceProxy = serializationManager.readSerializationData(resourceLocation, contents);
                // TODO - not sure if this 100% correct, but we definitely should not refer to the FileInfo as the
                // .serialization file, since for nt:file/nt:resource nodes this will overwrite the file contents
                String primaryType = (String) resourceProxy.getProperties().get(Repository.JCR_PRIMARY_TYPE);
                if (Repository.NT_FILE.equals(primaryType) || Repository.NT_RESOURCE.equals(primaryType)) {
                    // TODO move logic to serializationManager
                    File locationFile = new File(info.getLocation());
                    String locationFileParent = locationFile.getParent();
                    int endIndex = locationFileParent.length() - ".dir".length();
                    File actualFile = new File(locationFileParent.substring(0, endIndex));
                    String newLocation = actualFile.getAbsolutePath();
                    String newName = actualFile.getName();
                    String newRelativeLocation = actualFile.getAbsolutePath().substring(
                            syncDirectoryAsFile.getAbsolutePath().length());
                    info = new FileInfo(newLocation, newRelativeLocation, newName);

                    Activator.getDefault().getPluginLogger()
                            .trace("Adjusted original location from {0} to {1}", resourceLocation, newLocation);
                }

                return repository.newAddOrUpdateNodeCommand(info, resourceProxy);
            } catch (IOException e) {
                Activator.getDefault().getPluginLogger().warn(e.getMessage(), e);
                return null;
            } finally {
                IOUtils.closeQuietly(contents);
            }
        } else {

            // TODO - move logic to serializationManager
            // possible .dir serialization holder
            if (resource.getType() == IResource.FOLDER && resource.getName().endsWith(".dir")) {
                IFolder folder = (IFolder) resource;
                IResource contentXml = folder.findMember(".content.xml");
                // .dir serialization holder ; nothing to process here, the .content.xml will trigger the actual work
                if (contentXml != null && contentXml.exists()
                        && serializationManager.isSerializationFile(contentXml.getLocation().toOSString())) {
                    return null;
                }
            }

            ResourceProxy resourceProxy = buildResourceProxyForPlainFileOrFolder(resource, syncDirectory);

            return repository.newAddOrUpdateNodeCommand(info, resourceProxy);
        }
    }

    private FileInfo createFileInfo(IResource resource, Repository repository) throws SerializationException,
            CoreException {

        IProject project = resource.getProject();

        IFolder syncFolder = project.getFolder(ProjectUtil.getSyncDirectoryValue(project));
        final IFolder syncFolder1 = syncFolder;
        Filter filter = ProjectUtil.loadFilter(syncFolder1.getProject());

        if (filter != null) {
            FilterResult filterResult = getFilterResult(resource, filter, ProjectUtil.getSyncDirectoryFile(project),
                    syncFolder, repository);

            Activator.getDefault().getPluginLogger().trace("FilterResult for {0} is {1}", resource, filterResult);

            if (filterResult == FilterResult.DENY || filterResult == FilterResult.PREREQUISITE) {
                return null;
            }
        }

        IPath relativePath = resource.getFullPath().makeRelativeTo(syncFolder.getFullPath());

        FileInfo info = new FileInfo(resource.getLocation().toOSString(), relativePath.toOSString(), resource.getName());

        Activator.getDefault().getPluginLogger().trace("For {0} built fileInfo {1}", resource, info);

        return info;
    }

    private FilterResult getFilterResult(IResource resource, Filter filter, File contentSyncRoot, IFolder syncFolder,
            Repository repository) throws SerializationException {

        IPath relativePath = resource.getFullPath().makeRelativeTo(syncFolder.getFullPath());

        String absFilePath = new File(contentSyncRoot, relativePath.toOSString()).getAbsolutePath();
        String filePath = serializationManager.getBaseResourcePath(absFilePath);

        IPath osPath = Path.fromOSString(filePath);
        String repositoryPath = serializationManager.getRepositoryPath(osPath.makeRelativeTo(syncFolder.getLocation())
                .toPortableString());

        Activator.getDefault().getPluginLogger().trace("Filtering by {0} for {1}", repositoryPath, resource);

        return filter.filter(contentSyncRoot, repositoryPath, repository.getRepositoryInfo());
    }

    private ResourceProxy buildResourceProxyForPlainFileOrFolder(IResource changedResource, IFolder syncDirectory)
            throws CoreException, IOException {

        SerializationKind serializationKind;
        String fallbackNodeType;
        if (changedResource.getType() == IResource.FILE) {
            serializationKind = SerializationKind.FILE;
            fallbackNodeType = Repository.NT_FILE;
        } else { // i.e. IResource.FOLDER
            serializationKind = SerializationKind.FOLDER;
            fallbackNodeType = Repository.NT_FOLDER;
        }

        String resourceLocation = '/' + changedResource.getFullPath().makeRelativeTo(syncDirectory.getFullPath())
                .toPortableString();
        IPath serializationFilePath = Path.fromPortableString(serializationManager.getSerializationFilePath(
                resourceLocation,
                serializationKind));
        IResource serializationResource = syncDirectory.findMember(serializationFilePath);

        // if the serialization resource is null, it's valid to look for a serialization resource
        // higher in the filesystem, given that the found serialization resource covers this resource
        // TODO - this too should be abstracted in the service layer, rather than in the Eclipse-specific code
        if (serializationResource == null && changedResource.getType() == IResource.FOLDER) {
            while (!serializationFilePath.isRoot()) {
                serializationFilePath = serializationFilePath.removeLastSegments(1);
                IFolder folderWithPossibleSerializationFile = (IFolder) syncDirectory.findMember(serializationFilePath);
                if (folderWithPossibleSerializationFile == null) {
                    continue;
                }

                // it's safe to use a specific SerializationKind since this scenario is only valid for METADATA_PARTIAL
                // coverage
                String possibleSerializationFilePath = serializationManager.getSerializationFilePath(
                        ((IFolder) folderWithPossibleSerializationFile).getLocation().toOSString(),
                        SerializationKind.METADATA_PARTIAL);

                if (serializationManager.isSerializationFile(possibleSerializationFilePath)) {

                    IPath parentSerializationFilePath = Path.fromOSString(possibleSerializationFilePath).makeRelativeTo(
                            syncDirectory.getLocation());
                    IFile possibleSerializationFile = syncDirectory.getFile(parentSerializationFilePath);
                    if (!possibleSerializationFile.exists()) {
                        continue;
                    }

                    InputStream contents = possibleSerializationFile.getContents();
                    ResourceProxy serializationData;
                    try {
                        serializationData = serializationManager.readSerializationData(
                                parentSerializationFilePath.toPortableString(), contents);
                    } finally {
                        IOUtils.closeQuietly(contents);
                    }

                    boolean covered = serializationData
                            .covers(serializationManager.getRepositoryPath(resourceLocation));
                    if (covered) {
                        return serializationData.getChild(serializationManager.getRepositoryPath(resourceLocation));
                    }

                    break;
                }
            }
        }
        return buildResourceProxy(resourceLocation, serializationResource, syncDirectory, fallbackNodeType);
    }

    private ResourceProxy buildResourceProxy(String resourceLocation, IResource serializationResource,
            IFolder syncDirectory, String fallbackPrimaryType) throws CoreException, IOException {
        if (serializationResource instanceof IFile) {
            IFile serializationFile = (IFile) serializationResource;
            InputStream contents = null;
            try {
                contents = serializationFile.getContents();
                String serializationFilePath = serializationResource.getFullPath()
                        .makeRelativeTo(syncDirectory.getFullPath()).toPortableString();
                ResourceProxy resourceProxy = serializationManager.readSerializationData(serializationFilePath, contents);
                return resourceProxy;
            } finally {
                IOUtils.closeQuietly(contents);
            }
        }

        return new ResourceProxy(serializationManager.getRepositoryPath(resourceLocation), Collections.singletonMap(
                Repository.JCR_PRIMARY_TYPE, (Object) fallbackPrimaryType));
    }

    public Command<?> newCommandForRemovedResources(Repository repository, IResource removed) throws CoreException {
        
        try {
            return removeFileCommand(repository, removed);
        } catch (SerializationException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed removing" + removed, e));
        } catch (IOException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed removing" + removed, e));
        }
    }

    private Command<?> removeFileCommand(Repository repository, IResource resource) throws CoreException,
            SerializationException, IOException {

        if (resource.isTeamPrivateMember(IResource.CHECK_ANCESTORS)) {
            Activator.getDefault().getPluginLogger().trace("Skipping team-private resource {0}", resource);
            return null;
        }

        if (ignoredFileNames.contains(resource.getName())) {
            return null;
        }

        IFolder syncDirectory = ProjectUtil.getSyncDirectory(resource.getProject());
        File syncDirectoryAsFile = ProjectUtil.getSyncDirectoryFile(resource.getProject());
        final IFolder syncFolder = syncDirectory;

        Filter filter = ProjectUtil.loadFilter(syncFolder.getProject());

        if (filter != null) {
            FilterResult filterResult = getFilterResult(resource, filter, syncDirectoryAsFile, syncDirectory,
                    repository);
            if (filterResult == FilterResult.DENY || filterResult == FilterResult.PREREQUISITE) {
                return null;
            }
        }

        ResourceProxy resourceProxy = buildResourceProxyForPlainFileOrFolder(resource, syncDirectory);
        
        return repository.newDeleteNodeCommand(resourceProxy);
    }
}

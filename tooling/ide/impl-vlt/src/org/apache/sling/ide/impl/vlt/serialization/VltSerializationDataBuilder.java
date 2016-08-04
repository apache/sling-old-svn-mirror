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
package org.apache.sling.ide.impl.vlt.serialization;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.vault.fs.api.Aggregate;
import org.apache.jackrabbit.vault.fs.api.Aggregator;
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress;
import org.apache.jackrabbit.vault.fs.api.VaultFile;
import org.apache.jackrabbit.vault.fs.api.VaultFileSystem;
import org.apache.jackrabbit.vault.fs.config.ConfigurationException;
import org.apache.jackrabbit.vault.fs.impl.aggregator.FileAggregator;
import org.apache.jackrabbit.vault.fs.impl.aggregator.GenericAggregator;
import org.apache.jackrabbit.vault.fs.impl.io.DocViewSerializer;
import org.apache.jackrabbit.vault.util.Constants;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.jackrabbit.vault.util.MimeTypes;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.jackrabbit.vault.util.Text;
import org.apache.sling.ide.impl.vlt.Activator;
import org.apache.sling.ide.impl.vlt.VaultFsLocator;
import org.apache.sling.ide.jcr.RepositoryUtils;
import org.apache.sling.ide.serialization.SerializationData;
import org.apache.sling.ide.serialization.SerializationDataBuilder;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.serialization.SerializationKindManager;
import org.apache.sling.ide.transport.ResourceProxy;

public class VltSerializationDataBuilder implements SerializationDataBuilder {

    private VaultFsLocator fsLocator;
    private SerializationKindManager skm;
	private org.apache.sling.ide.transport.Repository repo;
	private Session session;
	private VaultFileSystem fs;

    public void init(org.apache.sling.ide.transport.Repository repository, File contentSyncRoot)
            throws SerializationException {

    	this.repo = repository;
    	
        try {
            this.skm = new SerializationKindManager();
            this.skm.init(repository);
            if (!contentSyncRoot.exists()) {
            	throw new IllegalArgumentException("contentSyncRoot does not exist: "+contentSyncRoot);
            }

            Repository jcrRepo = RepositoryUtils.getRepository(repo.getRepositoryInfo());
            Credentials credentials = RepositoryUtils.getCredentials(repo.getRepositoryInfo());
            
            session = jcrRepo.login(credentials);

            RepositoryAddress address = RepositoryUtils.getRepositoryAddress(repo.getRepositoryInfo());

            fs = fsLocator.getFileSystem(address, contentSyncRoot, session);
        
        } catch (org.apache.sling.ide.transport.RepositoryException e) {
            throw new SerializationException(e);
        } catch (RepositoryException e) {
            throw new SerializationException(e);
		} catch (IOException e) {
            throw new SerializationException(e);
		} catch (ConfigurationException e) {
            throw new SerializationException(e);
		}
    }
    
    @Override
    public void destroy() {
        if (session != null) {
            session.logout();
        }
    }

    @Override
    public SerializationData buildSerializationData(File contentSyncRoot, ResourceProxy resource) throws SerializationException {

        try {

            List<Aggregate> chain = findAggregateChain(resource);

            if (chain == null) {
            	return null;
            }

            Aggregate aggregate = chain.get(chain.size() - 1);

            String fileOrFolderPathHint = calculateFileOrFolderPathHint(chain);

            String nameHint = PlatformNameFormat.getPlatformName(aggregate.getName());

            SerializationKind serializationKind = getSerializationKind(aggregate);

            if (resource.getPath().equals("/") || serializationKind == SerializationKind.METADATA_PARTIAL
                    || serializationKind == SerializationKind.FILE || serializationKind == SerializationKind.FOLDER) {
                nameHint = Constants.DOT_CONTENT_XML;
            } else if (serializationKind == SerializationKind.METADATA_FULL) {
                nameHint += ".xml";
            }

            Activator.getDefault().getPluginLogger()
                    .trace("Got location {0} for path {1}", fileOrFolderPathHint, resource.getPath());

            if (!needsDir(aggregate)) {
                return SerializationData.empty(fileOrFolderPathHint, serializationKind);
            }

            DocViewSerializer s = new DocViewSerializer(aggregate);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            s.writeContent(out);
            
            byte[] result = out.toByteArray();

            return new SerializationData(fileOrFolderPathHint, nameHint, result, serializationKind);

        } catch (RepositoryException e) {
            throw new SerializationException(e);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    private SerializationKind getSerializationKind(Aggregate aggregate) throws RepositoryException {

        NodeType[] mixinNodeTypes = aggregate.getNode().getMixinNodeTypes();
        List<String> mixinNodeTypeNames = new ArrayList<>(mixinNodeTypes.length);
        for (NodeType nodeType : mixinNodeTypes)
            mixinNodeTypeNames.add(nodeType.getName());

        return skm.getSerializationKind(aggregate.getNode()
                .getPrimaryNodeType()
                .getName(), mixinNodeTypeNames);
    }

    private boolean needsDir(Aggregate aggregate) throws RepositoryException, PathNotFoundException,
            ValueFormatException {

        Aggregator aggregator = fs.getAggregateManager().getAggregator(aggregate.getNode(), null);
        boolean needsDir = true;
        if (aggregator instanceof FileAggregator) {
            needsDir = false;
            // TODO - copy-pasted from FileAggregator, and really does not belong here...
            Node content = aggregate.getNode();
            if (content.isNodeType(JcrConstants.NT_FILE)) {
                content = content.getNode(JcrConstants.JCR_CONTENT);
            }
            String mimeType = null;
            if (content.hasProperty(JcrConstants.JCR_MIMETYPE)) {
                try {
                    mimeType = content.getProperty(JcrConstants.JCR_MIMETYPE).getString();
                } catch (RepositoryException e) {
                    // ignore
                }
            }
            if (mimeType == null) {
                // guess mime type from name
                mimeType = MimeTypes.getMimeType(aggregate.getNode().getName(),
                        MimeTypes.APPLICATION_OCTET_STREAM);
            }

            needsDir = !MimeTypes.matches(aggregate.getNode().getName(), mimeType,
                    MimeTypes.APPLICATION_OCTET_STREAM);

            if (!needsDir) {
                if (content.hasProperty(JcrConstants.JCR_MIXINTYPES)) {
                    for (Value v : content.getProperty(JcrConstants.JCR_MIXINTYPES).getValues()) {
                        if (!v.getString().equals(JcrConstants.MIX_LOCKABLE)) {
                            needsDir = true;
                            break;
                        }
                    }
                }
            }

         // TODO - copy-pasted from GenericAggregator
        } else if (aggregator instanceof GenericAggregator) {
            if (isPlainNtFolder(aggregate)) {
                needsDir = false;
            }
        }
        return needsDir;
    }

    private String calculateFileOrFolderPathHint(List<Aggregate> chain) throws RepositoryException {

        ListIterator<Aggregate> aggs = chain.listIterator();
        StringBuilder out = new StringBuilder();
        while (aggs.hasNext()) {
            Aggregate cur = aggs.next();
            if (aggs.previousIndex() == 0) {
                out.append(PlatformNameFormat.getPlatformPath(cur.getPath()));
            } else {
                out.append("/");
                out.append(PlatformNameFormat.getPlatformPath(cur.getRelPath()));
            }

            if (needsDir(cur)) {
                SerializationKind serializationKind = getSerializationKind(cur);

                if (serializationKind == SerializationKind.FILE) {
                    out.append(".dir");
                }

                if (!aggs.hasNext() && serializationKind == SerializationKind.METADATA_FULL) {
                    out.delete(out.lastIndexOf("/"), out.length());
                }
            }
        }

        return out.toString();
    }

    private boolean isPlainNtFolder(Aggregate agg) throws RepositoryException {

        return agg.getNode().getPrimaryNodeType().getName().equals("nt:folder")
                && agg.getNode().getMixinNodeTypes().length == 0;
    }

    /**
     * Returns the aggregates for a specific resource
     * 
     * <p>
     * In the simplest case, a single element is returned in the chain, signalling that the aggregate is a top-level
     * one.
     * </p>
     * 
     * <p>
     * For leaf aggregates, the list contains the top-most aggregates first and ends up with the leaf-most ones.
     * </p>
     * 
     * @param resource the resource to find the aggregate chain for
     * @return a list of aggregates
     * @throws IOException
     * @throws RepositoryException
     */
    private List<Aggregate> findAggregateChain(ResourceProxy resource) throws IOException, RepositoryException {

        VaultFile vaultFile = fs.getFile(PlatformNameFormat.getPlatformPath(resource.getPath()));

        if (vaultFile == null || vaultFile.getAggregate() == null) {
                // this file might be a leaf aggregate of a vaultfile higher in the resource path ; so look for a
                // parent higher

            String parentPath = Text.getRelativeParent(resource.getPath(), 1);
            while (!parentPath.equals("/")) {
                VaultFile parentFile = fs.getFile(PlatformNameFormat.getPlatformPath(parentPath));

                if (parentFile != null) {
                    Aggregate parentAggregate = parentFile.getAggregate();
                    ArrayList<Aggregate> parents = new ArrayList<>();
                    parents.add(parentAggregate);
                    List<Aggregate> chain = lookForAggregateInLeaves(resource, parentAggregate, parents);
                    if (chain != null) {
                        return chain;
                    }
                }

                parentPath = Text.getRelativeParent(parentPath, 1);
            }

            return null;
        }


        return Collections.singletonList(vaultFile.getAggregate());
    }

    /**
     * Recursively looks for an aggregate matching the <tt>resource</tt>'s path starting at the <tt>parentAggregate</tt>
     * 
     * <p>
     * The returned chain will contain at least one aggregate, in case the resource is contained in a stand-alone (?)
     * aggregate, or multiple aggregates in case the matching aggregate is a leaf one.
     * </p>
     * 
     * @param resource the resource
     * @param parentAggregate the known parent aggregate which potentially matches this resource
     * @param chain the chain used to record all intermediate aggregates
     * @return the final aggregate chain
     * 
     * @throws RepositoryException
     */
    private List<Aggregate> lookForAggregateInLeaves(ResourceProxy resource, Aggregate parentAggregate,
            List<Aggregate> chain) throws RepositoryException {

        if (parentAggregate == null) {
            return null;
        }

        List<? extends Aggregate> leaves = parentAggregate.getLeaves();
        if (leaves == null) {
            return null;
        }

        for (Aggregate leaf : leaves) {
            if (leaf.getPath().equals(resource.getPath())) {
                chain.add(leaf);
                return chain;
            } else if (Text.isDescendant(leaf.getPath(), resource.getPath())) {
                chain.add(leaf);
                return lookForAggregateInLeaves(resource, leaf, chain);
            }
        }

        return null;
    }

    public void setLocator(VaultFsLocator locator) {

        this.fsLocator = locator;
    }

}

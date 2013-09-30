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

import static org.apache.sling.ide.util.PathUtil.getName;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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
import org.apache.sling.ide.impl.vlt.RepositoryUtils;
import org.apache.sling.ide.impl.vlt.VaultFsLocator;
import org.apache.sling.ide.serialization.SerializationData;
import org.apache.sling.ide.serialization.SerializationDataBuilder;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.serialization.SerializationKindManager;
import org.apache.sling.ide.transport.ResourceProxy;

public class VltSerializationDataBuilder implements SerializationDataBuilder {

    VaultFsLocator fsLocator;
    private SerializationKindManager skm;
	private File contentSyncRoot;
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
            this.contentSyncRoot = contentSyncRoot;

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

        // TODO - there is a small mismatch here since we're doing remote calls to the repository
        // but taking a resourceProxy - not sure if we'll run into problems down the road or not

        // TODO - there might be a performance problem with getting the session on-demand each time
        // the resolution might be to have a SerializationManager instance kept per 'transaction'
        // which is stateful, with init(RepositoryInfo) and destroy() methods
        try {
            

            VaultFile vaultFile = fs.getFile(resource.getPath());
            String platformPath = resource.getPath();
            if (vaultFile == null) {

                // TODO - not sure why we need to try both ... not a performance impact but ugly nonetheless
                platformPath = PlatformNameFormat.getPlatformPath(resource.getPath()) + VltSerializationManager.EXTENSION_XML;
                vaultFile = fs.getFile(platformPath);

                if (vaultFile == null) {
                    platformPath = PlatformNameFormat.getPlatformPath(resource.getPath());
                    vaultFile = fs.getFile(platformPath);
                }

                if (vaultFile == null) {
                    // TODO proper logging ; discover if this is expected or not and fail hard if it's not
                    System.err.println("No vaultFile at path " + resource.getPath());
                    return null;
                }
            }

            String nameHint = getName(platformPath);
            String fileOrFolderPathHint = vaultFile.getPath();

            Aggregate aggregate = vaultFile.getAggregate();

            if (aggregate == null) {
            	//TODO: there are valid cases apparently when aggregate is null and yet there
            	// are children which must be honored.. so we can't throw an exception here
            	// but we should review why this aggregate is null here and if that's valid.
            	System.err.println("No aggregate found for path " + resource.getPath());
            	return null;
            }

            NodeType[] mixinNodeTypes = aggregate.getNode().getMixinNodeTypes();
            List<String> mixinNodeTypeNames = new ArrayList<String>(mixinNodeTypes.length);
            for (NodeType nodeType : mixinNodeTypes)
                mixinNodeTypeNames.add(nodeType.getName());

            SerializationKind serializationKind = skm.getSerializationKind(aggregate.getNode().getPrimaryNodeType()
                    .getName(), mixinNodeTypeNames);

            if (resource.getPath().equals("/") || serializationKind == SerializationKind.METADATA_PARTIAL
                    || serializationKind == SerializationKind.FILE || serializationKind == SerializationKind.FOLDER) {
                nameHint = Constants.DOT_CONTENT_XML;
            }

            Aggregator aggregator = fs.getAggregateManager().getAggregator(aggregate.getNode(), null);
            if (aggregator instanceof FileAggregator) {
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
                    mimeType = MimeTypes.getMimeType(aggregate.getNode().getName(), MimeTypes.APPLICATION_OCTET_STREAM);
                }

                boolean needsDir = !MimeTypes.matches(aggregate.getNode().getName(), mimeType,
                        MimeTypes.APPLICATION_OCTET_STREAM);
                if (!needsDir) {
                    return SerializationData.empty(fileOrFolderPathHint, serializationKind);
                }
            } else if (aggregator instanceof GenericAggregator) {
                // TODO - copy-pasted from GenericAggregator
                if (aggregate.getNode().getPrimaryNodeType().getName().equals("nt:folder")
                        && aggregate.getNode().getMixinNodeTypes().length == 0) {
                    return SerializationData.empty(fileOrFolderPathHint, serializationKind);
                }
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


}

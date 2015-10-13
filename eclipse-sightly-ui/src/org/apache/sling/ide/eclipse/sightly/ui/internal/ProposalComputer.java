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
package org.apache.sling.ide.eclipse.sightly.ui.internal;

import org.apache.sling.ide.eclipse.sightly.SightlyFacetHelper;
import org.apache.sling.ide.eclipse.sightly.model.Attribute;
import org.apache.sling.ide.eclipse.sightly.model.ModelElements;
import org.apache.sling.ide.eclipse.sightly.model.ProposalDescription;
import org.apache.sling.ide.eclipse.sightly.model.Tag;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.DefaultXMLCompletionProposalComputer;
import org.eclipse.wst.xml.ui.internal.contentassist.MarkupCompletionProposal;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLRelevanceConstants;

@SuppressWarnings("restriction")
public class ProposalComputer extends DefaultXMLCompletionProposalComputer {

    @Override
    protected void addAttributeNameProposals(ContentAssistRequest contentAssistRequest,
            CompletionProposalInvocationContext context) {

        if ( addSightlyProposals(contentAssistRequest ) ) {
            for ( String attributeName: ModelElements.ATTRIBUTE_NAMES ) {
                addAttributeProposal(contentAssistRequest, new Attribute(attributeName));
            }
        }
        
        super.addAttributeNameProposals(contentAssistRequest, context);
    }
    

    @Override
    protected void addTagInsertionProposals(ContentAssistRequest contentAssistRequest, int childPosition,
            CompletionProposalInvocationContext context) {

        if ( addSightlyProposals(contentAssistRequest)) {
            addAttributeProposal(contentAssistRequest, new Tag("sly"));
        }
    }

    private void addAttributeProposal(final ContentAssistRequest contentAssistRequest,
            ProposalDescription proposalDescription) {

        String replacementString = proposalDescription.getInsertionText();

        int replacementOffset = contentAssistRequest.getReplacementBeginPosition();
        int replacementLength = contentAssistRequest.getReplacementLength();
        int cursorPosition = getCursorPositionForProposedText(replacementString);

        ICompletionProposal proposal = new MarkupCompletionProposal(replacementString.toString(), replacementOffset,
                replacementLength, cursorPosition, SharedImages.SIGHTLY_ICON.createImage(),
                proposalDescription.getLabel(), null, proposalDescription.getAdditionalInfo(),
                XMLRelevanceConstants.R_TAG_NAME);

        contentAssistRequest.addProposal(proposal);
    }
    
    private static boolean addSightlyProposals(ContentAssistRequest request) {

        final IDOMNode element = (IDOMNode) request.getNode();
        
        IFile file = getFile(element.getModel());

        return SightlyFacetHelper.hasSightlyFacet(file.getProject());
    }
    
    public static final IFile getFile(IStructuredModel model) {
        
        String baselocation = model.getBaseLocation();
        
        if (baselocation != null) {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IPath filePath = new Path(baselocation);
            if (filePath.segmentCount() > 1) {
                return root.getFile(filePath);
            }
        }
        return null;
    }    
}

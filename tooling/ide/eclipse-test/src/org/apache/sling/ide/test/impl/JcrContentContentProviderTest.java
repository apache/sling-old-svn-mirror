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
package org.apache.sling.ide.test.impl;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.sling.ide.eclipse.ui.nav.JcrContentContentProvider;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.apache.sling.ide.eclipse.ui.nav.model.SyncDir;
import org.apache.sling.ide.test.impl.helpers.DisableDebugStatusHandlers;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.apache.sling.ide.util.PathUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class JcrContentContentProviderTest {

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Rule
    public DisableDebugStatusHandlers disableDebugHandlers = new DisableDebugStatusHandlers();

    private IProject contentProject;
    private ProjectAdapter project;
    
    @Before
    public void prepareProject() throws Exception {
        
        contentProject = projectRule.getProject();

        project = new ProjectAdapter(contentProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install content facet
        project.installFacet("sling.content", "1.0");
    }

    @Test
    public void listChildrenInNestedStructure() throws Exception {

        // create .content.xml structure
        InputStream contentXml = getClass().getResourceAsStream("content-nested-structure.xml");
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/test-root/en.xml"), contentXml);

        // directly create the root node
        SyncDir syncDirNode = new SyncDir((IFolder) contentProject.findMember("jcr_root"));

        // assertions
        assertIsNavigableAndHasNoChildren(syncDirNode, "/content/test-root/en/message");
        assertIsNavigableAndHasNoChildren(syncDirNode, "/content/test-root/en/error");
        assertIsNavigableAndHasNoChildren(syncDirNode, "/content/test-root/en/warning");
    }

    @Test
    public void listChildrenWithNestedContentXmlInEscapedDir() throws Exception {

        // create .content.xml structure
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/.content.xml"), getClass()
                .getResourceAsStream("sling-folder-nodetype.xml"));

        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/_sling_stuff/.content.xml"), getClass()
                .getResourceAsStream("nt-unstructured-nodetype.xml"));

        // directly create the root node
        SyncDir syncDirNode = new SyncDir((IFolder) contentProject.findMember("jcr_root"));

        // assertion
        assertIsNavigableAndHasNoChildren(syncDirNode, "/content/sling:stuff");
    }

    @Test
    public void listChildrenWhenContentXmlIsBroken() throws Exception {

        // create .content.xml structure
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/.content.xml"), new ByteArrayInputStream(
                "invalid".getBytes()));

        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/child1.txt"), new ByteArrayInputStream(
                "hello, world".getBytes()));
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/child2.txt"), new ByteArrayInputStream(
                "hello, world".getBytes()));

        // directly create the root node
        SyncDir syncDirNode = new SyncDir((IFolder) contentProject.findMember("jcr_root"));
        
        // assertions
        assertIsNavigableAndHasNoChildren(syncDirNode, "/content/child1.txt");
        assertIsNavigableAndHasNoChildren(syncDirNode, "/content/child2.txt");
    }
    
    @Test
    public void listChildrenOnNtFolderIncludedUnderJcrContentNode() throws Exception  {

        // create .content.xml structure
        project.createOrUpdateFile(Path.fromPortableString("jcr_root/content/.content.xml"), 
                getClass().getResourceAsStream("nt-unstructured-with-folder-child.xml")); // TODO - rename xml file
        
        project.ensureDirectoryExists(Path.fromPortableString("jcr_root/content/_jcr_content/first-folder/second-folder"));

        // directly create the root node
        SyncDir syncDirNode = new SyncDir((IFolder) contentProject.findMember("jcr_root"));
        
        // assertion
        assertIsNavigableAndHasNoChildren(syncDirNode, "/content/jcr:content/first-folder/second-folder");
    }
    
    /**
     * Asserts that the specified <tt>nodePath</tt> is reachable from the <tt>startNode</tt>
     * 
     * <p>It further asserts that there are no children beyond the <tt>nodePath</tt>, i.e. it
     * is and endpoint</p>
     * 
     * @param startNode the node to start from
     * @param nodePath the path that is reachable and an endpoint
     */
    private void assertIsNavigableAndHasNoChildren(SyncDir startNode, String nodePath) {
        
        JcrContentContentProvider contentProvider = new JcrContentContentProvider();
        
        if ( nodePath.charAt(0) == '/') {
            nodePath = nodePath.substring(1);
        }
        
        String[] pathElements = nodePath.split("/");
        JcrNode current = startNode;
        
        segments: for ( int i = 0 ; i < pathElements.length ; i++ ) {
            
            String expectedChildName = pathElements[i];
            Object[] children = contentProvider.getChildren(current);
            
            for ( Object child : children ) {
                JcrNode childNode = (JcrNode) child;
                // childNode.getName() does not seem to be usable here, so relying on the path
                String childName = PathUtil.getName(childNode.getJcrPath());
                if ( childName.equals(expectedChildName)) {
                    current = childNode;
                    continue segments;
                }
            }
            
            fail("Unable to navigate to '" + nodePath + "'. "
                    + " No child named '"+ expectedChildName +"'found for node at " + current.getJcrPath() + ", children: " + Arrays.toString(children));
        }
        
        Object[] children = contentProvider.getChildren(current);
        if ( children.length != 0 ) {
            fail("Unexpected children for node at '" + current.getJcrPath() + "' : " + Arrays.toString(children));
        }
    }
}

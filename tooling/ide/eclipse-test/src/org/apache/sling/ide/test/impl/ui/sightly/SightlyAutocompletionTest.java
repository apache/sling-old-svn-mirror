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
package org.apache.sling.ide.test.impl.ui.sightly;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class SightlyAutocompletionTest {
    
    private static SWTWorkbenchBot bot;
    
    @Rule
    public TemporaryProject projectRule = new TemporaryProject();
    
    @BeforeClass
    public static void closeWelcomeView() throws Exception {
        bot = new SWTWorkbenchBot();
        if ( bot.activeView().getTitle().equals("Welcome") ) {
            bot.activeView().close();
        }
    }    

    @Test
    public void tagNameAutocompletion() throws Exception {
        
        List<String> proposals = new AutocompletionCallable() {
            @Override
            protected void prepareEditor(SWTBotEclipseEditor editor) {
                editor.insertText("<html>\n\n</html>");
                editor.navigateTo(1, 0);
            }
        }.call();
        

        // validate auto-completion proposals
        assertThat("proposal list does not contain 'sly'", proposals, hasItem("sly"));
    }
    
    @Test
    public void attributeAutocompletion() throws Exception {
        
        List<String> proposals = new AutocompletionCallable() {
            @Override
            protected void prepareEditor(SWTBotEclipseEditor editor) {
                editor.insertText("<html>\n<div  ></div>\n</html>");
                editor.navigateTo(1, 5);
            }
        }.call();

        // validate auto-completion proposals
        assertThat("proposal list does not contain 'data-sly-test'", proposals, hasItem("data-sly-test"));
    }
    
    abstract class AutocompletionCallable implements Callable<List<String>> {
        
        @Override
        public List<String> call() throws Exception {
            
            // create faceted project
            IProject contentProject = projectRule.getProject();

            ProjectAdapter project = new ProjectAdapter(contentProject);
            project.addNatures(JavaCore.NATURE_ID, "org.eclipse.wst.common.project.facet.core.nature");

            // install facets
            project.installFacet("sling.content", "1.0");
            project.installFacet("sightly", "1.1");
            
            // create basic html file
            project.createOrUpdateFile(Path.fromOSString("jcr_root/index.html"), new ByteArrayInputStream("".getBytes()));
            Thread.sleep(1000); // TODO - wait for project to be registered in the UI
            
            // ensure that we get the tree from the project explorer
            bot.viewByTitle("Project Explorer").setFocus();
            
            // open editor
            SWTBotTreeItem projectItem = bot.tree().expandNode(contentProject.getName());
            // it seems that two 'jcr_root' nodes confuse SWTBot so we expand and navigate manually
            SWTBotTreeItem folderNode = projectItem.getItems()[0].expand();
            folderNode.getItems()[0].select().doubleClick();
            
            // generate auto-completion proposals
            SWTBotEclipseEditor editor = bot.editorByTitle("index.html").toTextEditor();
            prepareEditor(editor);

            // gather proposals
            List<String> proposals = editor.getAutoCompleteProposals("");
            
            // close editor, otherwise cleanup can hang due to the dialog
            editor.saveAndClose();

            return proposals;
        }

        /**
         * Prepare the editor by adding text and positioning the cursor
         * 
         * <p>After this method is executed the editor must be ready to generate
         * the autocompletions expected by the test</p>
         * 
         * @param editor the editor instance, never <code>null</code>
         */
        protected abstract void prepareEditor(SWTBotEclipseEditor editor);
        
    }

}

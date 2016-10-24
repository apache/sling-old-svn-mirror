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
package org.apache.sling.ide.eclipse.m2e.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Resolutions when current pom.xml configuration does not support manifest generation during incremental builds.
 *
 * @see <a href="https://wiki.eclipse.org/FAQ_How_do_I_implement_Quick_Fixes_for_my_own_language%3F">Implement Quick
 *      Fixes</a>
 */
public class BundleProjectNotSupportingM2EIncrementalBuildQuickFix implements IMarkerResolutionGenerator {

    @Override
    public IMarkerResolution[] getResolutions(IMarker marker) {
        // either suggest to use maven-bundle-project 3.2.0 with correct configuration
        IMarkerResolution[] resolutions = new IMarkerResolution[3];
        resolutions[0] = new InstallM2ETychoExtension();
        resolutions[1] = new FixUsingBndMavenPlugin();
        resolutions[2] = new FixUsingMBP320WithCorrectConfiguration();
        return resolutions;
    }

    public static final class FixUsingMBP320WithCorrectConfiguration implements IMarkerResolution {

        FixUsingMBP320WithCorrectConfiguration() {
        }

        @Override
        public String getLabel() {
            return "Use maven-bundle-plugin 3.2.0 or newer with correct configuration";
        }

        @Override
        public void run(IMarker marker) {
            MessageDialogWithLinkSection.openInformationWithLink(null,
                    "Use maven-bundle-plugin 3.2.0 or newer with correct configuration",
                    "Please upgrade to at least maven-bundle-plugin 3.2.0 and explicitly configure goal 'manifest' with configuration 'supportIncrementalBuild' and 'exportScr' set to 'true'.",
                    "Further information is available at <a href=\"http://felix.apache.org/documentation/faqs/apache-felix-bundle-plugin-faq.html#use-scr-metadata-generated-by-bnd-in-unit-tests\">Maven Bundle Plugin FAQ</a>, <a href=\"https://issues.apache.org/jira/browse/FELIX-4009\">FELIX-4009</a> and <a href=\"https://issues.apache.org/jira/browse/https://issues.apache.org/jira/browse/FELIX-3324\">https://issues.apache.org/jira/browse/FELIX-3324</a>");
        }
    }
    
    public static final class FixUsingBndMavenPlugin implements IMarkerResolution {

        FixUsingBndMavenPlugin() {
        }

        @Override
        public String getLabel() {
            return "Use bnd-maven-plugin in version 2.4.1 or newer";
        }

        @Override
        public void run(IMarker marker) {
            MessageDialogWithLinkSection.openInformationWithLink(null,
                    "Use bnd-maven-plugin 2.4.1 or newer",
                    "Please use bnd-maven-plugin as this supports m2e incremental builds correctly.",
                    "Further information is available at <a href=\"http://njbartlett.name/2015/03/27/announcing-bnd-maven-plugin.html\">Blog Post with Announcement</a>, <a href=\"https://github.com/bndtools/bnd/issues/1180\">issues-1180</a> and <a href=\"https://github.com/bndtools/bnd/pull/1333\">PR-1333</a>.");
        }
    }

    /**
     * Similar to {@link MessageDialog} but with an additional area above the buttons which contain links.
     * 
     * Unfortunately such a functionality is not yet part of Eclipse, but requested in
     * <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=484347">bug 484347</a>.
     * 
     * @see <a href="http://stackoverflow.com/questions/29374160/add-link-to-messagedialog-message">http://stackoverflow
     *      .com/questions/29374160/add-link-to-messagedialog-message<a>
     * @see <a href="http://stackoverflow.com/questions/3968620/how-can-i-add-a-hyperlink-to-a-jface-dialog">http://stackoverflow.com/questions/3968620/how-can-i-add-a-hyperlink-to-a-jface-dialog</a>
     * 
     * 
     */
    public static final class MessageDialogWithLinkSection extends MessageDialog {

        private final String linkText;

        public MessageDialogWithLinkSection(Shell parentShell, String dialogTitle, Image dialogTitleImage,
                String dialogMessage, int dialogImageType, String[] dialogButtonLabels, int defaultIndex,
                String linkText) {
            super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels,
                    defaultIndex);
            this.linkText = linkText;
        }

        public static boolean openInformationWithLink(Shell parent, String title, String message, String link) {
            MessageDialogWithLinkSection dialog = new MessageDialogWithLinkSection(parent, title, null, message,
                    INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0, link);
            return dialog.open() == 0;
        }

        @Override
        protected Control createCustomArea(Composite parent) {
            Link link = new Link(parent, SWT.WRAP);
            link.setText(linkText);

            link.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    try {
                        // Open default external browser
                        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
                    } catch (PartInitException ex) {
                        Activator.getDefault().getPluginLogger()
                                .error("Could not open external browser for link '" + e.text + "'", ex);
                    } catch (MalformedURLException ex) {
                        Activator.getDefault().getPluginLogger().error("Invalid link detected '" + e.text + "'", ex);
                    }
                }
            });
            return link;
        }
    }

    public static final class InstallM2ETychoExtension implements IMarkerResolution {

        InstallM2ETychoExtension() {
        }

        @Override
        public String getLabel() {
            return "Install m2e-tycho extension (incompatible with maven-bundle-plugin 3.2.0 and later)";
        }

        @SuppressWarnings("restriction")
        @Override
        public void run(IMarker marker) {
            org.eclipse.m2e.internal.discovery.MavenDiscovery.launchWizard(Collections.singleton("bundle"),
                    Collections.<MojoExecutionKey> emptyList(), Collections.<String> emptyList(), Collections.<String> emptyList());
        }
    }

}

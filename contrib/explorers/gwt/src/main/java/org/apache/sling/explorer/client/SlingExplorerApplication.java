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
package org.apache.sling.explorer.client;

import org.apache.sling.explorer.client.widgets.grid.resource.ResourceGrids;
import org.apache.sling.explorer.client.widgets.tree.resource.ResourceTree;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes for the Sling Explorer application
 */
public class SlingExplorerApplication implements EntryPoint {

    public static final ExplorerImages images = (ExplorerImages) GWT.create(ExplorerImages.class);

    ExplorerConstants constants = (ExplorerConstants) GWT.create(ExplorerConstants.class);

    private ExplorerPageTemplate explorerPage = new ExplorerPageTemplate();

    private ResourceTree resourceTree;
    private ResourceGrids resourceGrid;

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        // Create the Sling explorer page
        setupTitlePanel(constants);
        setupMainLinks(constants);

        //init explorer widgets
        resourceGrid = new ResourceGrids();
        resourceTree = new ResourceTree(resourceGrid);

        RootPanel.get().add(explorerPage);
        explorerPage.setMainWidget(createExplorerPanel());

        // Populate the root level of the explorer tree
        resourceTree.populate();
    }

    private Widget createExplorerPanel() {
		HorizontalSplitPanel explorerPanel = new HorizontalSplitPanel();
		explorerPanel.setSize("100%", "100%");
		explorerPanel.setSplitPosition("20%");


		DisclosurePanel propertiesPanel = new DisclosurePanel(constants.propertiesDescripton(),true);
		propertiesPanel.add(resourceGrid.getPropertyGrid());
		propertiesPanel.setStyleName("application-DisclosurePanel");

		DisclosurePanel nodeChildrenPanel = new DisclosurePanel(constants.subResourcesDescription(),true);
		nodeChildrenPanel.add(resourceGrid.getResourceChildrenGrid());
		nodeChildrenPanel.setStyleName("application-DisclosurePanel");

		propertiesPanel.setAnimationEnabled(true);
		nodeChildrenPanel.setAnimationEnabled(true);


	    FlexTable layout = new FlexTable();
	    layout.setCellSpacing(6);
	    layout.setWidget(0, 0, propertiesPanel);
	    layout.setWidget(1, 0, nodeChildrenPanel);


		explorerPanel.setRightWidget(layout);
		explorerPanel.setLeftWidget(resourceTree);

		return explorerPanel;
	}

    /**
     * Create the main links at the top of the application.
     *
     * @param constants the constants with text
     */
    private void setupMainLinks(ExplorerConstants constants) {
        // Link to Sling Homepage
        explorerPage.addLink(new HTML("<a href=\"" + ExplorerConstants.SLING_HOMEPAGE + "\">"
            + constants.slingHomePage() + "</a>"));
    }

    /**
     * Create the title bar at the top of the application.
     *
     * @param constants the constant values to use
     */
    private void setupTitlePanel(ExplorerConstants constants) {
    	// Get the title from the internationalized constants
        String pageTitle = "<h1>" + constants.mainTitle() + "</h1><h2>"
            + constants.mainSubTitle() + "</h2>";

        // Add the title and some images to the title bar
        HorizontalPanel titlePanel = new HorizontalPanel();
        titlePanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        titlePanel.add(images.explorerLogo().createImage());
        titlePanel.add(new HTML(pageTitle));
        explorerPage.setTitleWidget(titlePanel);
    }
}


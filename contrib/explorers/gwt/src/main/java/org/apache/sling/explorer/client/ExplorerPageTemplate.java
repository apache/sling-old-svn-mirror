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


import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.HTMLTable.RowFormatter;

/**
 * Page template composed of a Top Panel & a main panel
 * The Top panel can contain logo, title & links
 *
 * The main panel can contain any kind of panels or widgets
 *
 */
public class ExplorerPageTemplate extends Composite {

    /**
     * The base style name.
     */
    public static final String DEFAULT_STYLE_NAME = "Application";

    /**
     * The panel that contains the menu and content.
     */
    private HorizontalPanel mainPanel;

    /**
     * The panel that holds the main links.
     */
    private HorizontalPanel linksPanel;

    /**
     * The panel that contains the title widget and links.
     */
    private FlexTable topPanel;

   /**
     * Constructor.
     */
    public ExplorerPageTemplate() {
	    // Setup the main layout widget
        FlowPanel layout = new FlowPanel();
        initWidget(layout);

        // Setup the top panel with the title and links
        createTopPanel();
        layout.add(topPanel);

        // Add the main menu
        mainPanel = new HorizontalPanel();
        mainPanel.setSize("100%","100%");

        mainPanel.setSpacing(0);
        mainPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
        layout.add(mainPanel);
    }

    /**
     * Add a link to the top of the page.
     *
     * @param link the widget to add to the mainLinks
     */
    public void addLink(Widget link) {
        if (linksPanel.getWidgetCount() > 0) {
            linksPanel.add(new HTML("&nbsp;|&nbsp;"));
        }
        linksPanel.add(link);
    }

    /**
     * @return the {@link Widget} used as the title
     */
    public Widget getTitleWidget() {
        return topPanel.getWidget(0, 0);
    }

    /**
     * Set the {@link Widget} to display in the content area.
     *
     * @param content the content widget
     */
    public void setMainWidget(Widget content) {
	    mainPanel.add(content);
    }

    /**
     * Set the {@link Widget} to use as the title bar.
     *
     * @param title the title widget
     */
    public void setTitleWidget(Widget title) {
        topPanel.setWidget(1, 0, title);
    }

    /**
     * Create the panel at the top of the page that contains the title and links.
     */
    private void createTopPanel() {
        boolean isRTL = LocaleInfo.getCurrentLocale().isRTL();
        topPanel = new FlexTable();
        topPanel.setCellPadding(0);
        topPanel.setCellSpacing(0);
        topPanel.setStyleName(DEFAULT_STYLE_NAME + "-top");
        FlexCellFormatter formatter = topPanel.getFlexCellFormatter();

        // Setup the links cell
        linksPanel = new HorizontalPanel();
        topPanel.setWidget(0, 0, linksPanel);
        formatter.setStyleName(0, 0, DEFAULT_STYLE_NAME + "-links");
        if (isRTL) {
            formatter.setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_LEFT);
        } else {
            formatter.setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        }
        formatter.setColSpan(0, 0, 2);

        // Setup the title cell
        setTitleWidget(null);
        formatter.setStyleName(1, 0, DEFAULT_STYLE_NAME + "-title");

        formatter.setStyleName(1, 1, DEFAULT_STYLE_NAME + "-options");
        if (isRTL) {
            formatter.setHorizontalAlignment(1, 1, HasHorizontalAlignment.ALIGN_LEFT);
        } else {
            formatter.setHorizontalAlignment(1, 1, HasHorizontalAlignment.ALIGN_RIGHT);
        }

        RowFormatter rowFormatter = topPanel.getRowFormatter();

        // Align the content to the top
        rowFormatter.setVerticalAlign(0,
            HasVerticalAlignment.ALIGN_TOP);
        rowFormatter.setVerticalAlign(1,
            HasVerticalAlignment.ALIGN_TOP);
    }
}

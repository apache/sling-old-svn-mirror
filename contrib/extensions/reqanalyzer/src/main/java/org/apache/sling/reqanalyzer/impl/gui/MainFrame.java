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
package org.apache.sling.reqanalyzer.impl.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;

public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String MAIN_Y = "main.y";
    private static final String MAIN_X = "main.x";
    private static final String MAIN_HEIGHT = "main.height";
    private static final String MAIN_WIDTH = "main.width";
    private static final String MAIN_COLS = "main.cols";

    public MainFrame(final File file, final int limit, final Dimension screenSize) throws FileNotFoundException,
            IOException {

        JTextPane text = Util.showStartupDialog("Reading from " + file, screenSize);

        final RequestTrackerFile dm = new RequestTrackerFile(file, limit, text);

        final JTable table = new JTable(dm);
        table.setAutoCreateRowSorter(true);
        table.setGridColor(Color.GRAY);
        table.setShowGrid(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setTableHeader(new JTableHeader(table.getColumnModel()));
        Util.setupColumnWidths(table.getColumnModel(), MAIN_COLS);
//        table.setFont(new Font("Monospaced", table.getFont().getStyle(), table.getFont().getSize()));
        table.getSelectionModel().addListSelectionListener(new RequestListSelectionListener(this, table, screenSize));

        add(new JScrollPane(table));

        setTitle(file.getPath());

        // setup location and size and ensure updating preferences
        Util.setupComponentLocationSize(this, MAIN_X, MAIN_Y, MAIN_WIDTH, MAIN_HEIGHT, (int) screenSize.getWidth() / 4,
                0, (int) screenSize.getWidth() / 2, (int) screenSize.getHeight() / 4);

        Util.disposeStartupDialog(text);
    }
}

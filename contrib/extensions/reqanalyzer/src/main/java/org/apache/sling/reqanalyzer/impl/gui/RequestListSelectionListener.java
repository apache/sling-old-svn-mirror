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
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

public class RequestListSelectionListener implements ListSelectionListener {

    private static final String REQUEST_Y = "request.y";
    private static final String REQUEST_X = "request.x";
    private static final String REQUEST_HEIGHT = "request.height";
    private static final String REQUEST_WIDTH = "request.width";
    private static final String REQUEST_COLS = "request.cols";

    private final Window parent;
    private final JTable table;
    private final Dimension screenSize;

    private JTable dataField = null;

    public RequestListSelectionListener(final Window parent, final JTable table, final Dimension screenSize) {
        this.parent = parent;
        this.table = table;
        this.screenSize = screenSize;
    }

    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            int idx = lsm.getMinSelectionIndex();
            if (idx >= 0) {
                try {
                    idx = table.getRowSorter().convertRowIndexToModel(idx);
                    TableModel tm = ((RequestTrackerFile) table.getModel()).getData(idx);
                    if (dataField == null) {
                        dataField = new JTable();
                        dataField.setAutoCreateRowSorter(true);
                        dataField.setGridColor(Color.GRAY);
                        dataField.setShowGrid(true);
                        dataField.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        dataField.setRowSelectionAllowed(true);
                        dataField.setTableHeader(new JTableHeader(dataField.getColumnModel()));
                        dataField.setFont(new Font("Monospaced", dataField.getFont().getStyle(), dataField.getFont().getSize()));
                        dataField.setShowHorizontalLines(false);
//                        dataField.setIntercellSpacing(new Dimension(3, 5));

                        JDialog d = new JDialog(this.parent);
                        d.add(new JScrollPane(dataField));

                        d.addWindowListener(new WindowAdapter() {
                            @Override
                            public void windowClosing(WindowEvent e) {
                                dataField = null;
                            }
                        });

                        // setup location and size and ensure updating preferences
                        Util.setupComponentLocationSize(d, REQUEST_X, REQUEST_Y, REQUEST_WIDTH, REQUEST_HEIGHT,
                                (int) screenSize.getWidth() / 4, (int) screenSize.getHeight() / 4,
                                (int) screenSize.getWidth() / 2, (int) screenSize.getHeight() / 2);

                        d.setVisible(true);
                    }

                    dataField.setModel(tm);

                    Util.setupColumnWidths(dataField.getColumnModel(), REQUEST_COLS);

                } catch (IOException e1) {
                    // ignore
                }
            }
        }
    }

}

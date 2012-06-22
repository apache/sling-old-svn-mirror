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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextPane;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class RequestTrackerFile implements TableModel {

    private final RandomAccessFile raFile;
    private final List<RequestTrackerFileEntry> entries;

    RequestTrackerFile(File file, int limit, JTextPane text) throws FileNotFoundException, IOException {
        this.raFile = new RandomAccessFile(file, "r");
        this.entries = new ArrayList<RequestTrackerFileEntry>();
        String line;
        for (int i = 0; i < limit && (line = this.raFile.readLine()) != null; ) {
            if (line.charAt(0) == ':') {
                try {
                    text.setText(line);
                    RequestTrackerFileEntry entry = new RequestTrackerFileEntry(line, this.raFile.getFilePointer());
                    this.entries.add(entry);
                    i++;
                } catch (Exception e) {
                    System.err.println(e);
                    e.printStackTrace(System.err);
                    break;
                }
            }
        }
    }

    TableModel getData(final int rowIndex) throws IOException {
        final RequestTrackerFileEntry entry = this.entries.get(rowIndex);
        final long offset = entry.getOffset();
        RequestTableModel rtm = new RequestTableModel();
        this.raFile.seek(offset);
        String line;
        while ((line = raFile.readLine()) != null && line.charAt(0) == '!') {
            rtm.addRow(line.substring(1));
        }
        return rtm;
    }
    
    public int getRowCount() {
        return this.entries.size();
    }

    public int getColumnCount() {
        return RequestTrackerFileEntry.getColumnCount();
    }

    public String getColumnName(int columnIndex) {
        return RequestTrackerFileEntry.getColumnName(columnIndex);
    }

    public Class<?> getColumnClass(int columnIndex) {
        return RequestTrackerFileEntry.getColumnClass(columnIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        RequestTrackerFileEntry entry = this.entries.get(rowIndex);
        return entry.getField(columnIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new UnsupportedOperationException("setValue");
    }

    public void addTableModelListener(TableModelListener l) {
        // not really ...
    }

    public void removeTableModelListener(TableModelListener l) {
        // not really ...
    }
}

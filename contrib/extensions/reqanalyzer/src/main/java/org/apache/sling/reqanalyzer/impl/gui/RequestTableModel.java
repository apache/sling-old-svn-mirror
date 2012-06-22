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

import java.util.ArrayList;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class RequestTableModel implements TableModel {

    private final ArrayList<Object[]> rows = new ArrayList<Object[]>();

    private long previousStamp;
    
    void addRow(String row) {
        // split row: "%1$7d (%2$tF %2$tT) %3$s%n
        
        final String stampS = row.substring(0, 7);
        final int endTimeStamp = row.indexOf(')');
        final String message = row.substring(endTimeStamp+2); 
        
        long stamp = Long.parseLong(stampS.trim());
        long delta = stamp - this.previousStamp;
        this.previousStamp = stamp;
        
        final Object[] rowValue = new Object[]{ stamp, delta, message };
        this.rows.add(rowValue);
    }
    
    public int getRowCount() {
        return rows.size();
    }

    public int getColumnCount() {
        return 3;
    }

    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Timestamp";
            case 1:
                return "Delta";
            case 2:
                return "Message";
            default:
                throw new IllegalArgumentException("columnIndex=" + columnIndex);
        }
    }

    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Long.class;
            case 1:
                return Long.class;
            case 2:
                return String.class;
            default:
                throw new IllegalArgumentException("columnIndex=" + columnIndex);
        }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Object[] row = rows.get(rowIndex);
        return row[columnIndex];
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    public void addTableModelListener(TableModelListener l) {
    }

    public void removeTableModelListener(TableModelListener l) {
    }
}

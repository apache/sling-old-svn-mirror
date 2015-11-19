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
package org.apache.sling.discovery.commons.providers.spi.base;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.discovery.commons.providers.BaseTopologyView;

public class ClusterSyncHistory {

    class HistoryEntry {
        BaseTopologyView view;
        String msg;
        String fullLine;
    }
    
    /** the date format used in the truncated log of topology events **/
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    protected List<HistoryEntry> history = new LinkedList<HistoryEntry>();
    
    public List<String> getSyncHistory() {
        List<HistoryEntry> snapshot;
        synchronized(history) {
            snapshot = Collections.unmodifiableList(history);
        }
        List<String> result = new ArrayList<String>(snapshot.size());
        for (HistoryEntry historyEntry : snapshot) {
            result.add(historyEntry.fullLine);
        }
        return result;
    }

    protected void addHistoryEntry(BaseTopologyView view, String msg) {
        synchronized(history) {
            for(int i = history.size() - 1; i>=0; i--) {
                HistoryEntry entry = history.get(i);
                if (!entry.view.equals(view)) {
                    // don't filter if the view starts differing,
                    // only filter for the last few entries where
                    // the view is equal
                    break;
                }
                if (entry.msg.equals(msg)) {
                    // if the view is equal and the msg matches
                    // then this is a duplicate entry, so ignore
                    return;
                }
            }
            String fullLine = sdf.format(Calendar.getInstance().getTime()) + ": " + msg;
            HistoryEntry newEntry = new HistoryEntry();
            newEntry.view = view;
            newEntry.fullLine = fullLine;
            newEntry.msg = msg;
            history.add(newEntry);
            while (history.size() > 12) {
                history.remove(0);
            }
        }
    }
    
}

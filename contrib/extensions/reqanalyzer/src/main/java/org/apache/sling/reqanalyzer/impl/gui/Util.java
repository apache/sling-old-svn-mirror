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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JTextPane;
import javax.swing.table.TableColumnModel;

public class Util {

    private Util() {
    }

    static JTextPane showStartupDialog(final String title, final Dimension screenSize) {
        JTextPane text = new JTextPane();
        text.setText("...");

        JDialog d = new JDialog((Window) null);
        d.setTitle(title);
        d.add(text);
        d.setSize((int) screenSize.getWidth() / 2, 30);
        d.setLocation((int) screenSize.getWidth() / 4, (int) screenSize.getHeight() / 2 - 15);
        d.setVisible(true);

        return text;
    }

    static void disposeStartupDialog(final Component comp) {
        Container parent = comp.getParent();
        while (parent != null && !(parent instanceof Window)) {
            parent = parent.getParent();
        }
        if (parent instanceof Window) {
            ((Window) parent).dispose();
        }
    }

    static void setupComponentLocationSize(final Component comp, final String propX, final String propY,
            final String propWidth, final String propHeight, final int defaultX, final int defaultY,
            final int defaultWidth, final int defaultHeight) {

        comp.setLocation(getPreference(propX, defaultY), getPreference(propY, defaultX));
        comp.setSize(getPreference(propWidth, defaultWidth), getPreference(propHeight, defaultHeight));

        comp.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                setPreference(propX, e.getComponent().getX(), false);
                setPreference(propY, e.getComponent().getY(), true);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                setPreference(propWidth, e.getComponent().getWidth(), false);
                setPreference(propHeight, e.getComponent().getHeight(), true);
            }
        });
    }

    static void setupColumnWidths(final TableColumnModel tcm, final String propertyName) {
        PropertyChangeListener pcl = new PropertyChangeListener() {
            private final String pclPropName = propertyName;
            private final TableColumnModel pclTcm = tcm;

            public void propertyChange(PropertyChangeEvent evt) {
                if ("width".equals(evt.getPropertyName())) {
                    int[] colWidths = new int[pclTcm.getColumnCount()];
                    for (int i = 0; i < colWidths.length; i++) {
                        colWidths[i] = pclTcm.getColumn(i).getWidth();
                    }
                    setPreference(pclPropName, colWidths, true);
                }
            }
        };

        int[] colWidths = getPreference(propertyName, new int[0]);
        for (int i = 0; i < colWidths.length && i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setPreferredWidth(colWidths[i]);
        }
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).addPropertyChangeListener(pcl);
        }
    }

    static void setPreference(final String name, final Object value, final boolean flush) {
        Preferences prefs = getPreferences();
        try {
            prefs.sync();
            if (value instanceof Long) {
                prefs.putLong(name, (Long) value);
            } else if (value instanceof Integer) {
                prefs.putInt(name, (Integer) value);
            } else if (value instanceof int[]) {
                String string = null;
                for (int val : (int[]) value) {
                    if (string == null) {
                        string = String.valueOf(val);
                    } else {
                        string += "," + val;
                    }
                }
                prefs.put(name, string);
            } else if (value != null) {
                prefs.put(name, value.toString());
            }

            if (flush) {
                prefs.flush();
            }
        } catch (BackingStoreException ioe) {
            // ignore
        }
    }

    static int getPreference(final String name, final int defaultValue) {
        Preferences prefs = getPreferences();
        try {
            prefs.sync();
            return prefs.getInt(name, defaultValue);
        } catch (BackingStoreException ioe) {
            // ignore
        }
        return defaultValue;
    }

    static int[] getPreference(final String name, final int[] defaultValues) {
        Preferences prefs = getPreferences();
        try {
            prefs.sync();
            String value = prefs.get(name, null);
            if (value != null) {
                String[] values = value.split(",");
                int[] result = new int[values.length];
                for (int i = 0; i < values.length; i++) {
                    result[i] = Integer.parseInt(values[i]);
                }
                return result;
            }
        } catch (BackingStoreException ioe) {
            // ignore
        } catch (NumberFormatException nfe) {
            // ignore
        }
        return defaultValues;
    }

    static Preferences getPreferences() {
        return Preferences.userNodeForPackage(Util.class);
    }
}

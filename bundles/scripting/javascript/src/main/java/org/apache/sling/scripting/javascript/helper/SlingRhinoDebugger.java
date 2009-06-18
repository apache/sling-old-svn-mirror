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
package org.apache.sling.scripting.javascript.helper;

import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.tools.debugger.Dim;
import org.mozilla.javascript.tools.debugger.SwingGui;

class SlingRhinoDebugger extends Dim {
    private SlingContextFactory slingContextFactory;

    private final SwingGui gui;

    SlingRhinoDebugger(String windowTitle) {
        gui = new SwingGui(this, windowTitle);
        gui.pack();
        gui.setVisible(true);
        gui.setExitAction(new Runnable() {
            public void run() {
                if (slingContextFactory != null) {
                    slingContextFactory.debuggerStopped();
                }
            }
        });
    }

    @Override
    public void attachTo(ContextFactory factory) {
        super.attachTo(factory);

        if (factory instanceof SlingContextFactory) {
            this.slingContextFactory = (SlingContextFactory) factory;
        }
    }

    @Override
    public void detach() {
        this.slingContextFactory = null;
        super.detach();
    }

    @Override
    public void dispose() {
        clearAllBreakpoints();
        go();
        gui.dispose();
        super.dispose();
    }
}

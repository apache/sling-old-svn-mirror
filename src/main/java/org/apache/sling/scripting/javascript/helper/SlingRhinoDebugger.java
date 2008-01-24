package org.apache.sling.scripting.javascript.helper;

import org.mozilla.javascript.tools.debugger.Dim;
import org.mozilla.javascript.tools.debugger.SwingGui;

class SlingRhinoDebugger extends Dim {
    SlingRhinoDebugger(String windowTitle) {
        final SwingGui gui = new SwingGui(this, windowTitle);
        gui.pack();
        gui.setVisible(true);
    }
}

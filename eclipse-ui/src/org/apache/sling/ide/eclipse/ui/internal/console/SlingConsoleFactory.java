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
package org.apache.sling.ide.eclipse.ui.internal.console;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

public class SlingConsoleFactory implements IConsoleFactory {

    public static final String CONSOLE_NAME = "Sling console";

    private MessageConsole console;

    public static MessageConsole getConsole() {
        // TODO not the right place, should converge with initConsole

        for (IConsole console : ConsolePlugin.getDefault().getConsoleManager().getConsoles()) {
            if (console.getName().equals(CONSOLE_NAME)) {
                return (MessageConsole) console;
            }
        }

        return null;
    }

    @Override
    public void openConsole() {

        IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();

        initConsole(consoleManager);

        consoleManager.showConsoleView(console);
    }

    private void initConsole(IConsoleManager consoleManager) {

        if (console == null) {
            console = new MessageConsole(CONSOLE_NAME, null);
            consoleManager.addConsoles(new IConsole[] { console });
        }
    }

}

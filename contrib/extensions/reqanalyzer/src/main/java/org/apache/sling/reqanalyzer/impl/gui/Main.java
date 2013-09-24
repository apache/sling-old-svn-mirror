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

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Cannot run in headless mode");
            System.exit(1);
        }

        if (args.length == 0) {
            System.err.println("Missing argument <file>");
            System.exit(1);
        }

        File file = new File(args[0]);
        if (!file.canRead()) {
            System.err.println("Cannot read from file " + file);
            System.exit(1);
        }

        final int limit;
        if (args.length >= 2) {
            limit = Integer.parseInt(args[1]);
        } else {
            limit = Integer.MAX_VALUE;
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        MainFrame frame = new MainFrame(file, limit, screenSize);
        frame.setVisible(true);

        // exit the application if the main frame is closed
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

}

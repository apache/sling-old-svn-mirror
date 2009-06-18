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
package org.apache.sling.scripting.scala.engine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import scala.tools.nsc.Settings;
import scala.tools.nsc.reporters.AbstractReporter;
import scala.tools.nsc.util.Position;

public class BacklogReporter extends AbstractReporter {
    public static final int DEFAULT_SIZE = 50;

    private final List<Info> backLog = new LinkedList<Info>();
    private final Settings settings;
    private final int size;

    public BacklogReporter(Settings settings) {
        this(settings, DEFAULT_SIZE);
    }

    public BacklogReporter(Settings settings, int size) {
        super();
        this.settings = settings;
        this.size = size;
    }

    @Override
    public void reset() {
        super.reset();
        backLog.clear();
    }

    @Override
    public void display(Position pos, String msg, Severity severity) {
        severity.count_$eq(severity.count() + 1);
        if (size > 0) {
            backLog.add(new Info(pos, msg, severity));
            if (backLog.size() > size) {
                backLog.remove(0);
            }
        }
    }

    @Override
    public void displayPrompt() {
        // empty
    }

    @Override
    public Settings settings() {
        return settings;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for (Info info : backLog) {
            pw.println(info.toString());
        }
        return sw.toString();
    }

    private class Info {
        private final Position pos;
        private final String msg;
        private final Severity severity;

        public Info(Position pos, String msg, Severity severity) {
            this.pos = pos;
            this.msg = msg;
            this.severity = severity;
        }

        @Override
        public String toString() {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            print(pw, severity);
            pw.print(" ");
            print(pw, pos);
            pw.print(": ");
            pw.print(msg);
            return sw.toString();
        }

        private void print(PrintWriter pw, Position pos) {
            if (pos.source().isDefined()) {
                pw.print(pos.source().get());
                pw.print(" ");
            }
            if (pos.line().isDefined()) {
                pw.print("line ");
                pw.print(pos.line().get());
            }
        }

        private void print(PrintWriter pw, Severity severity) {
            if (INFO().equals(severity)) {
                pw.print("INFO");
            }
            else if (WARNING().equals(severity)) {
                pw.print("WARNING");
            }
            else if (ERROR().equals(severity)) {
                pw.print("ERROR");
            }
            else {
                throw new IllegalArgumentException("Severtiy out of range");
            }
        }

    }

}

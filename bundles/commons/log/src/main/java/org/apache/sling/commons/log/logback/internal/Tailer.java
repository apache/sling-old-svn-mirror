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

package org.apache.sling.commons.log.logback.internal;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

class Tailer{
    static final int BUFFER_SIZE = 1024;
    private final int numOfLines;
    private final TailerListener listener;
    private final byte[] buffer = new byte[BUFFER_SIZE];

    public Tailer(PrintWriter printWriter, int numOfLines) {
        this(new PrinterListener(printWriter), numOfLines);
    }

    public Tailer(TailerListener listener, int numOfLines) {
        this.listener = listener;
        this.numOfLines = numOfLines;
    }

    interface TailerListener {
        /**
         * Handles a line from a Tailer.
         *
         * @param line the line.
         */
        void handle(String line);
    }

    public void tail(File file) throws IOException {
        RandomAccessFile raf = null;
        try{
            raf = new RandomAccessFile(file, "r");
            long startPos = getTailStartPos(raf, numOfLines);
            readLines(raf, startPos);
        } finally {
            try{
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException ignore){

            }
        }
    }

    /**
     * Returns the starting position of UNIX "tail -n".
     */
    private long getTailStartPos(RandomAccessFile file, int n) throws IOException {
        int newlineCount = 0;
        long length = file.length();
        long pos = length - BUFFER_SIZE;
        int buffLength = BUFFER_SIZE;

        if (pos < 0) {
            pos = 0;
            buffLength = (int)length;
        }

        while (true) {
            file.seek(pos);
            file.readFully(buffer, 0, buffLength);

            for (int i = buffLength - 1; i >= 0; i--) {
                if ((char) buffer[i] == '\n') {
                    newlineCount++;

                    if (newlineCount >= n) {
                        pos += (i + 1);
                        return pos;
                    }
                }
            }

            if (pos == 0) {
                break;
            }

            if (pos - BUFFER_SIZE < 0) {
                buffLength = (int)pos;
                pos = 0;
            } else {
                pos -= BUFFER_SIZE;
            }
        }

        return pos;
    }

    /**
     * Read new lines. Code below is taken from org.apache.commons.io.input.Tailer
     *
     * @throws java.io.IOException if an I/O error occurs.
     * @param startPos position in file from where to start reading
     */
    private void readLines(RandomAccessFile file, long startPos) throws IOException {
        StringBuilder sb = new StringBuilder();

        file.seek(startPos);
        int num;
        boolean seenCR = false;
        while (((num = file.read(buffer)) != -1)) {
            for (int i = 0; i < num; i++) {
                byte ch = buffer[i];
                switch (ch) {
                    case '\n':
                        seenCR = false; // swallow CR before LF
                        listener.handle(sb.toString());
                        sb.setLength(0);
                        break;
                    case '\r':
                        if (seenCR) {
                            sb.append('\r');
                        }
                        seenCR = true;
                        break;
                    default:
                        if (seenCR) {
                            seenCR = false; // swallow final CR
                            listener.handle(sb.toString());
                            sb.setLength(0);
                        }
                        sb.append((char) ch); // add character, not its ascii value
                }
            }
        }

        //Drain the left over part
        if (sb.length() != 0) {
            listener.handle(sb.toString());
        }
    }

    private static class PrinterListener implements Tailer.TailerListener {
        private final PrintWriter pw;

        public PrinterListener(PrintWriter pw) {
            this.pw = pw;
        }

        @Override
        public void handle(String line) {
            pw.println(line);
        }
    }

}

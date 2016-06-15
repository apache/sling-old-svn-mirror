/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.compiler.optimization;

import org.apache.sling.scripting.sightly.compiler.commands.Command;
import org.apache.sling.scripting.sightly.compiler.commands.CommandHandler;
import org.apache.sling.scripting.sightly.compiler.commands.CommandStream;
import org.apache.sling.scripting.sightly.compiler.commands.OutText;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;

/**
 * Aggregate consecutive writes in bigger chunks of text
 */
public final class CoalescingWrites implements CommandHandler {

    public static final StreamTransformer TRANSFORMER = new StreamTransformer() {
        @Override
        public CommandStream transform(CommandStream inStream) {
            PushStream outputStream = new PushStream();
            CoalescingWrites instance = new CoalescingWrites(outputStream);
            inStream.addHandler(instance);
            return outputStream;
        }
    };

    private StringBuilder builder = new StringBuilder();
    private final PushStream outStream;

    private CoalescingWrites(PushStream stream) {
        this.outStream = stream;
    }

    @Override
    public void onEmit(Command command) {
        String text = detectText(command);
        if (text != null) {
            builder.append(text);
        } else {
            flushText();
            outStream.write(command);
        }
    }


    @Override
    public void onError(String errorMessage) {
        flushText();
        outStream.signalError(errorMessage);
    }

    @Override
    public void onDone() {
        flushText();
        outStream.close();
    }

    private String detectText(Command command) {
        if (command instanceof OutText) {
            return  ((OutText) command).getText();
        }
        return null;
    }

    private void flushText() {
        if (builder.length() > 0) {
            outStream.write(new OutText(builder.toString()));
            builder = new StringBuilder();
        }
    }
}

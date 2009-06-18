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

import org.slf4j.Logger;

import scala.tools.nsc.Settings;
import scala.tools.nsc.util.Position;

public class LogReporter extends BacklogReporter {
    private final Logger logger;

    public LogReporter(Logger logger, Settings settings) {
        super(settings);
        this.logger = logger;
    }

    @Override
    public void display(Position pos, String msg, Severity severity) {
        super.display(pos, msg, severity);
        if (INFO().equals(severity)) {
            logger.info("{}: {}", msg, pos);
        }
        else if (WARNING().equals(severity)) {
            logger.warn("{}: {}", msg, pos);
        }
        else if (ERROR().equals(severity)) {
            logger.error("{}: {}", msg, pos);
        }
        else {
            throw new IllegalArgumentException("Severtiy out of range");
        }
    }

}

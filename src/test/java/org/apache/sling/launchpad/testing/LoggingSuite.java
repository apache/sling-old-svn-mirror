/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.testing;


import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.slf4j.Logger;

/**
 * Simple TestSuite subclass which logs when each test is starting.
 */
class LoggingSuite extends TestSuite {

    private Logger logger;

    private int lastRuns;

    private int lastErrors;

    private int lastFailures;

    LoggingSuite(String name, Logger logger) {
        super(name);
        this.logger = logger;
    }

    @Override
    public void run(TestResult result) {
        // result.addListener(new SlingTestListener());
        super.run(result);
    }

    @Override
    public void runTest(Test test, TestResult result) {
        final String name = getName(test); 
        final String startMessage = String.format("Running %s", name);
        System.out.println(startMessage);
        logger.info(startMessage);

        super.runTest(test, result);

        final String endMessage = String.format(
            "Tests run: %d, Failures: %d, Errors: %d, Skipped: %d",
            result.runCount() - lastRuns, result.failureCount() - lastFailures,
            result.errorCount() - lastErrors,
            test.countTestCases() - result.runCount() + lastRuns);
        System.out.println(endMessage);
        logger.info(endMessage);

        lastRuns = result.runCount();
        lastFailures = result.failureCount();
        lastErrors = result.errorCount();
    }


    private String getName(Test t) {
        return (t instanceof TestSuite) ? ((TestSuite) t).getName() : t
                .toString();
    }
}

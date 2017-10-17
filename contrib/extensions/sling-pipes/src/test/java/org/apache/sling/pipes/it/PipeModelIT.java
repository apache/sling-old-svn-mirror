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
package org.apache.sling.pipes.it;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Testing a pipe model in a sightly script
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class PipeModelIT extends PipesTestSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipeModelIT.class);

    @Test
    public void testListComponent() throws IOException {
        final String url = String.format("http://localhost:%s/content/list-component.html", httpPort());
        LOGGER.info("fetching {}", url);
        Document document = Jsoup.connect(url).get();
        LOGGER.info("retrieved following response {}", document.toString());
        Elements elements = document.getElementsByClass("fruit");
        assertEquals("there should be 2 elements", 2, elements.size());
    }
}

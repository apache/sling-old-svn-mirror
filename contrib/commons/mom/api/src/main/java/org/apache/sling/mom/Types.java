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
package org.apache.sling.mom;

/**
 * Created by ieb on 13/04/2016.
 */
public final class Types {
    private Types() {
    }

    public interface Name {
    }
    public interface TopicName extends Name {
    }
    public interface QueueName extends Name {
    }
    public interface CommandName {
    }

    public static TopicName topicName(String topicName) {
        return new TopicNameImpl(topicName);
    }

    public static QueueName queueName(String queueName) {
        return new QueueNameImpl(queueName);
    }

    public static CommandName commandName(String commandName) {
        return new CommandNameImpl(commandName);
    }


    private static class QueueNameImpl extends StringWrapper implements QueueName {

        private QueueNameImpl(String value) {
            super(value);
        }
    }

    private static class CommandNameImpl extends StringWrapper implements CommandName {

        private CommandNameImpl(String value) {
            super(value);
        }
    }

    private static class TopicNameImpl extends StringWrapper implements TopicName {

        private TopicNameImpl(String value) {
            super(value);
        }
    }

    private static class StringWrapper implements Comparable<String> {


        private String value;

        private StringWrapper(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return value.equals(obj.toString());
        }

        @Override
        public int compareTo(String o) {
            return value.compareTo(o);
        }

        public String toString() {
            return value;
        }
    }

}

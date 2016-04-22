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
package org.apache.sling.testing.html.microdata;
import org.apache.sling.testing.ClientException;
import org.apache.sling.testing.html.HtmlClient;
import org.apache.sling.testing.html.UrlEncodedValues;
import org.apache.sling.testing.html.Values;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StateImpl implements State {

    private Document document;

    private HtmlClient client;

    public StateImpl(String html, String baseUri, HtmlClient client) {
        document = Jsoup.parse(html, baseUri);
        this.client = client;
    }

    public Items link(String rel) {
        return new ItemsImpl(toItems(document.select("link" + toClass(rel) + ", a" + toClass(rel))));
    }

    public Items form(String rel) {
        return new ItemsImpl(toItems(document.select("form" + toClass(rel))));
    }

    public Items item(String rel) {
        List<Item> items = new ArrayList<Item>();

        for (Item i : items()) {
            if (((ItemImpl) i).el.hasClass(rel)) {
                items.add(i);
            }
        }

        return new ItemsImpl(items);
    }

    public ItemsImpl items() {
        return new ItemsImpl(selectItems(document, new ArrayList<Item>()));
    }

    private String toClass(String name) {
        return "." + name;
    }

    private List<Item> toItems(Elements el) {
        List<Item> items = new ArrayList<Item>();

        for (Element e : el) {
            items.add(new ItemImpl(e, this));
        }

        return items;
    }

    private List<Item> selectItems(Element e, List<Item> items) {
        if (e.hasAttr("itemscope") && !e.hasAttr("itemprop")) {
            items.add(new ItemImpl(e, this));
            return items;
        }

        for (Element c : e.children()) {
            selectItems(c, items);
        }

        return items;
    }

    private class ItemImpl implements Item {

        private Element el;

        private StateImpl state;

        private ItemImpl followed;

        public ItemImpl(Element element, StateImpl state) {
            if (element == null) throw new NullPointerException("element is mandatory");
            this.el = element;
            this.state = state;
        }

        public Items prop(String name) throws ClientException {
            return new ItemsImpl(selectProps(getProxy().el, name, new ArrayList<Item>()));
        }

        private List<Item> selectProps(Element e, String name, List<Item> items) {
            for (Element c : e.children()) {
                if (c.hasAttr("itemprop")) {
                    if (c.attr("itemprop").equals(name)) {
                        items.add(new ItemImpl(c, state));
                    }

                    if (c.hasAttr("itemscope")) {
                        continue;
                    }
                }

                selectProps(c, name, items);
            }

            return items;
        }

        private ItemImpl getProxy() throws ClientException {
            if (el.tagName().equalsIgnoreCase("a") && el.hasAttr("href")) {
                if (followed == null) {
                    followed = follow(el, state);
                }
                return followed;
            }

            return this;
        }

        private ItemImpl follow(Element el, StateImpl state) throws ClientException {
            if (el.hasAttr("itemscope")) {
                return new ItemImpl(el, state);
            }

            if (el.tagName().equalsIgnoreCase("a") && el.hasAttr("href")) {
                String href = el.attr("href");

                if (href.startsWith("#")) {
                    Element first = el.ownerDocument().select(href).first();
                    return first == null ? null : follow(first, state);
                }

                String absHref = el.attr("abs:href");
                StateImpl s = (StateImpl) state.client.get(absHref);

                try {
                    URI uri = new URI(absHref);

                    String fragment = uri.getRawFragment();
                    if (fragment != null) {
                        Element e = s.document.getElementById(fragment);
                        return e == null ? null : follow(e, s);
                    }
                } catch (URISyntaxException ex) {
                    throw new ClientException("Error parsing URI: " + absHref, ex);
                }

                ItemsImpl items = s.items();

                if (items.length() == 1) {
                    return (ItemImpl) items.at(0);
                }

                throw new ClientException("Unable determine item: " + absHref);
            }

            return new ItemImpl(el, state);
        }

        @Override
        public String text() throws ClientException {
            // resolve element
            Element el = getProxy().el;

            // if it's a meta, get the value of the content attr
            if (el.tagName().equalsIgnoreCase("meta") && el.hasAttr("content")) {
                return el.attr("content");
            }

            // else, get the text value using jsoup
            return getProxy().el.text();
        }

        @Override
        public boolean bool() throws ClientException {
            return Boolean.parseBoolean(text());
        }

        @Override
        public String href() {
            return el.attr("href");
        }

        @Override
        public String src() {
            return el.attr("src");
        }

        @Override
        public State navigate() throws ClientException {
            if ((el.tagName().equalsIgnoreCase("a") || el.tagName().equalsIgnoreCase("link")) && el.hasAttr("href")) {
                String href = el.attr("href");

                if (href.startsWith("#")) {
                    return state;
                }

                return state.client.enter(el.attr("abs:href"));
            }

            throw new ClientException("Unable to follow: " + el.toString());
        }

        @Override
        public State submit(Values values) throws ClientException {
            if (el.tagName().equalsIgnoreCase("form")) {
                String action = el.attr("abs:action");
                if (action.length() == 0) {
                    action = el.baseUri();
                }

                String method = el.attr("method");

                if (method.length() == 0 || method.equalsIgnoreCase("get")) {
                    UrlEncodedValues query = new UrlEncodedValues(el, values);
                    String url = action + (action.contains("?") ? "?" : "&") + query.toString();
                    return state.client.enter(url);
                }

                if (method.equalsIgnoreCase("post")) {
                    String enctype = el.attr("enctype");

                    if (enctype.length() == 0 || enctype.equalsIgnoreCase("application/x-www-form-urlencoded")) {
                        UrlEncodedValues v = new UrlEncodedValues(el, values);
                        return state.client.post(action, v.toEntity());
                    }

                    throw new ClientException("Unsupported form enctype: " + enctype);
                }

                throw new ClientException("Unsupported form method: " + method);
            }

            throw new ClientException("The item is not a form");
        }


    }

    private class ItemsImpl implements Items {
        private List<Item> items;

        public ItemsImpl(List<Item> items) {
            this.items = items;
        }

        @Override
        public Iterator<Item> iterator() {
            return items.iterator();
        }

        @Override
        public Item at(int index) {
            return items.get(index);
        }

        @Override
        public int length() {
            return items.size();
        }

        @Override
        public Items prop(String name) throws ClientException {
            return items.get(0).prop(name);
        }

        @Override
        public String text() throws ClientException {
            return items.get(0).text();
        }

        @Override
        public boolean bool() throws ClientException {
            return items.get(0).bool();
        }

        @Override
        public String href() {
            return items.get(0).href();
        }

        @Override
        public String src() {
            return items.get(0).src();
        }

        @Override
        public State navigate() throws ClientException {
            return items.get(0).navigate();
        }

        @Override
        public State submit(Values values) throws ClientException {
            return items.get(0).submit(values);
        }
    }
}

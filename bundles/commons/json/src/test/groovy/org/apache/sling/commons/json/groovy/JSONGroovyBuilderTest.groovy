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
package org.apache.sling.commons.json.groovy;

import org.apache.sling.commons.json.*;
import org.apache.sling.commons.json.test.*;

/**
 * Test of JSONGroovyBuilder.
 *
 * Based on code written by Andres Almiray <aalmiray@users.sourceforge.net> as
 * part of the json-lib project - http://json-lib.sourceforge.net/
 */
public class JSONGroovyBuilderTest extends GroovyTestCase {
	JSONGroovyBuilder builder

	void testBuildDefaultRootEmptyObject(){
		def actual = builder.json {}
		JSONAssert.assertEquals( new JSONObject(), actual)
	}
	void testBuildDefaultRootEmptyArray(){
		def actual = builder.json ([])
		JSONAssert.assertEquals( new JSONArray(), actual)
	}
	void testBuildDefaultRootObjectWithClosure(){
		def actual = builder.json {
			string "json"
			integer 1
			bool true
		}
		def expected = new JSONObject()
				.put("string","json")
				.put("integer",1)
				.put("bool",true)
		JSONAssert.assertEquals(expected, actual)
		assertEquals("json", actual.get("string"))
	}
	void testBuildDefaultRootObjectWithMap(){
		def actual = builder.json([
		'string': "json",
		'integer': 1,
		'bool': true
		])
		def expected = new JSONObject()
				.put("string","json")
				.put("integer",1)
				.put("bool",true)
		JSONAssert.assertEquals(expected, actual)
	}
	void testBuildDefaultRootArrayWithList(){
		def actual = builder.json(["json", 1, true])
		def expected = new JSONArray()
				.put("json")
				.put(1)
				.put(true)
		JSONAssert.assertEquals(expected, actual)
	}
	void testBuildDefaultRootNestedObjects(){
		def actual = builder.json {
			first { integer 42 }
			second { integer 48 }
		}
		def expected = new JSONObject()
				.put( "first", new JSONObject().put("integer",42) )
				.put( "second", new JSONObject().put("integer",48) )
		JSONAssert.assertEquals(expected, actual)
	}
	void testBuildObjectWithMaps(){
		def actual = builder.json {
			books {
				book ([title: "The Definitive Guide to Grails", author: "Graeme Rocher"])
				book ([title: "Groovy in Action", author: "Dierk Konig"])
			}
		}
		def expected = new JSONObject()
				.put( "books", new JSONObject()
				.put( "book", new JSONObject()
				.put("title", "The Definitive Guide to Grails")
				.put("author", "Graeme Rocher") )
				.accumulate( "book", new JSONObject()
				.put("title", "Groovy in Action")
				.put("author", "Dierk Konig") )
				)
		JSONAssert.assertEquals(expected, actual)
	}

	void testNonDefaultRootName() {
		def actual = builder.books {
			book ([title: "The Definitive Guide to Grails", author: "Graeme Rocher"])
			book ([title: "Groovy in Action", author: "Dierk Konig"])
		}
		def expected = new JSONObject()
				.put("books", new JSONObject()
				.put("book", new JSONObject()
				.put("title", "The Definitive Guide to Grails")
				.put("author", "Graeme Rocher"))
				.accumulate( "book", new JSONObject()
				.put("title", "Groovy in Action")
				.put("author", "Dierk Konig"))
				)
		JSONAssert.assertEquals(expected, actual)

	}


	void testBuildObjectWithList(){
		def actual = builder.json {
			list(
			[title: "The Definitive Guide to Grails", author: "Graeme Rocher"],
			[title: "Groovy in Action", author: "Dierk Konig"]
			)
		}
		def expected = new JSONObject().put("list", new JSONArray().
				put(new JSONObject().put("title", "The Definitive Guide to Grails").put("author", "Graeme Rocher")).
				put(new JSONObject().put("title", "Groovy in Action").put("author", "Dierk Konig"))

				)
		JSONAssert.assertEquals(expected, actual)
	}

	void testBuildObjectWithClosures(){
		def actual = builder.json {
			books {
				book  {
					title  "The Definitive Guide to Grails"
					author "Graeme Rocher"
				}
				book  {
					title  "Groovy in Action"
					author  "Dierk Konig"
				}
			}
		}
		def expected = new JSONObject()
				.put( "books", new JSONObject()
				.put( "book", new JSONObject()
				.put("title", "The Definitive Guide to Grails")
				.put("author", "Graeme Rocher") )
				.accumulate( "book", new JSONObject()
				.put("title", "Groovy in Action")
				.put("author", "Dierk Konig") )
				)

		JSONAssert.assertEquals(expected, actual)
	}

	void testBuildObjectWithClosures2(){
		def actual = builder.json {
			books {
				2.times {
					book {
						title "The Definitive Guide to Grails"
						author "Graeme Rocher"
					}
				}
			}
		}
		def expected = new JSONObject()
				.put( "books", new JSONObject()
				.put( "book", new JSONObject()
				.put("title", "The Definitive Guide to Grails")
				.put("author", "Graeme Rocher") )
				.accumulate( "book", new JSONObject()
				.put("title", "The Definitive Guide to Grails")
				.put("author", "Graeme Rocher") )
				)

		JSONAssert.assertEquals(expected, actual)
	}

	void testBuildObjectWithClosures3(){
		def actual = builder.json{
			books {
				book {
					title "The Definitive Guide to Grails"
					author "Graeme Rocher"
				}
				book {
					title "Groovy in Action"
					author "Dierk Konig"
				}
			}
		}
		def expected = new JSONObject()
				.put( "books", new JSONObject()
				.put( "book", new JSONObject()
				.put("title", "The Definitive Guide to Grails")
				.put("author", "Graeme Rocher") )
				.accumulate( "book", new JSONObject()
				.put("title", "Groovy in Action")
				.put("author", "Dierk Konig") )
				)
		JSONAssert.assertEquals(expected, actual)
	}

	void testBuildObjectWithMultipleClosures(){
		def actual = builder.json {
			books ([{
				title "The Definitive Guide to Grails"
				author "Graeme Rocher"
			}, {
				title "Groovy in Action"
				author "Dierk Konig"
			}])
		}
		def expected = new JSONObject()
				.put( "books", new JSONArray()
				.put( new JSONObject()
				.put("title", "The Definitive Guide to Grails")
				.put("author", "Graeme Rocher") )
				.put( new JSONObject()
				.put("title", "Groovy in Action")
				.put("author", "Dierk Konig") )
				)

		JSONAssert.assertEquals(expected, actual)
	}

	void testBuildObject_Map_with_Closure(){
		def actual = builder.json([object:{key "value"}])
		def expected = new JSONObject()
				.put( "object", new JSONObject()
				.put( "key", "value" )
				)
		JSONAssert.assertEquals(expected, actual)
	}

	void testBuildObject_Map_with_Map(){
		def actual = builder.json([object:[key:"value"]])
		def expected = new JSONObject()
				.put( "object", new JSONObject()
				.put( "key", "value" )
				)
		JSONAssert.assertEquals(expected, actual)
	}

	void testBuildObject_Map_with_List(){
		def actual = builder.json([object: [1,2,3]])
		def expected = new JSONObject()
				.put( "object", [1,2,3] )
		JSONAssert.assertEquals(expected, actual)
	}

	void testBuildObject_List_with_Closure(){
		def actual = builder.json([{key "value"}])
		def expected = new JSONArray()
				.put( new JSONObject()
				.put( "key", "value" )
				)
		JSONAssert.assertEquals(expected, actual)
	}
	void testBuildObject_List_with_Map(){
		def actual = builder.json([[key:"value"]])
		def expected = new JSONArray()
				.put( new JSONObject()
				.put( "key", "value" )
				)
		JSONAssert.assertEquals(expected, actual)
	}

	void testBuildObject_List_with_List(){
		def actual = builder.json([[1,2,3]])
		def expected = new JSONArray()
				.put( [1,2,3] )
		JSONAssert.assertEquals(expected, actual)
	}

	void testBuildObject_Method_with_Map(){
		def actual = builder.json {
			node( ['key':"value"] )
		}
		def expected = new JSONObject()
				.put( "node", new JSONObject().put("key","value") )
		JSONAssert.assertEquals(expected, actual)
	}

	void testBuildObject_Method_with_List(){
		def actual = builder.json {
			node( [1,2,3] )
		}
		def expected = new JSONObject()
				.put( "node", new JSONArray([1,2,3]) )
		JSONAssert.assertEquals(expected, actual)
	}

	void testBuildObject_Method_with_Map_multipleArgs(){
		def actual = builder.json {
			node( ['key':"value"], ['key':"value"] )
		}
		def expected = new JSONObject()
				.put( "node", new JSONArray()
				.put( new JSONObject().put("key","value") )
				.put( new JSONObject().put("key","value") )
				)
		JSONAssert.assertEquals(expected, actual)
	}

	protected void setUp(){
		builder = new JSONGroovyBuilder()
	}
}
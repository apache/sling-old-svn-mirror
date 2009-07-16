Loading initial content from bundles
====================================


Repository items to be loaded into the repository, when the bundle is first
installed, may be defined in four ways:

   1. Directories
   2. Files
   3. XML descriptor files
   4. JSON descriptor files

Depending on the bundle entry found in the location indicated by the
Sling-Initial-Content bundle manifest header, nodes are created (and/or updated)
as follows:


Directories
-----------

Unless a node with the name of the directory already exists or has been defined
in an XML or JSON descriptor file (see below) a directory is created as a node
with the primary node type "nt:folder" in the repository.


Files
-----

Unless a node with the name of the file already exists or has been defined in an
XML or JSON descriptor file (see below) a file is created as two nodes in the
repository. The node bearing the name of the file itself is created with the
primary node type "nt:file". Underneath this file node, a resource node with
the primary node type "nt:resource" is created, which is set to the contents
of the file.

The MIME type is derived from the file name extension by first trying to
resolve it from the Bundle entry URL. If this does not resolve to a MIME type,
the Sling MIME type resolution service is used to try to find a mime type. If
all fals, the MIME type is defaulted to "application/octet-stream".


XML Descriptor Files
--------------------

Nodes, Properties and in fact complete subtrees may be described in XML files
using the following skeleton structure:

	<node>
		<!--
			optional on top level, defaults to XML file name without .xml extension
	       	required for child nodes
		-->
		<name>xyz</name>
	   
		<!--
	   		optional, defaults to nt:unstructured
		-->
		<primaryNodeType>nt:file</primaryNodeType>
		
		<!--
		    optional mixin node type
		    may be repeated for multiple mixin node types
		-->
		<mixinNodeType>mix:versionable</mixinNodeType>
		<mixinNodeType>mix:lockable</mixinNodeType>
		
		<!--
			Optional properties for the node. Each <property> element defines
			a single property of the node. The element may be repeated.
		-->
		<property>
			<!--
				required property name
			-->
			<name>prop</name>
			
			<!--
				value of the property.
				For multi-value properties, the values are defined by multiple
				<value> elements nested inside a <values> element instead of a
				single <value> element
			-->
			<value>property value as string</value>
			
			<!--
				Optional type of the property value, defaults to String.
				This must be one of the property type strings defined in the
				JCR PropertyType interface.
			<type>String</type>
		</property>
		
		<!--
			Additional child nodes. May be further nested.
		-->
		<node>
		....
		</node>
	</node>


JSON Descriptor Files
---------------------

Nodes, Properties and in fact complete subtrees may be described in JSON files
using the following skeleton structure (see http://www.json.org for information
on the syntax of JSON) :

# the name of the node is taken from the name of the file without the .json ext.
	{
	
		# optional primary node type, default "nt:unstructured"
		"jcr:primaryType":"sling:ScriptedComponent",
		# optional mixin node types as array
		"jcr:mixinTypes": [ ],
		
	    
	    # "properties" are added as key value pairs, the name of the key being the name
	    # of the property. The value is either the string property value, array for 
	    # multi-values or an object whose value[s] property denotes the property 
	    # value(s) and whose type property denotes the property type
	    "sling:contentClass": "com.day.sling.jcr.test.Test",
	    "sampleMulti": [ "v1", "v2" ],
	    "sampleStruct": 1,
	    "sampleStructMulti": [ 1, 2, 3 ],
	    
	    # reference properties start with jcr:reference
	    "jcr:reference:sampleReference": "/test/content",
	    
	    # path propertie start with jcr:path
	    "jcr:path:sampleReference": "/test/path",
	    	
	    # nested nodes are added as nested maps. 
		"sling:scripts":	{
	            
				"jcr:primaryType": "sling:ScriptList",
				"script1" :{
						"primaryNodeType": "sling:Script",
					    "sling:name": "/test/content/jsp/start.jsp",
						"sling:type": "jsp",
						"sling:glob": "*"
				}
		}
	}

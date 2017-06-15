# Protocols

Many of these protocols are documented elsewhere, however this document is with the source code and so hopefully uptodate.

There is also published documentation at the following locations which may lag the code and this document. This document wont
reproduce the information in the following locations, but will note differences when they are discovered.

https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html
https://cwiki.apache.org/confluence/display/SLING/Chunked+File+Upload+Support

# Request Processing.

In general a request is processed by the Sling Engine encapsulating the data in Sling API interfaces. See the above locations
for more information on how that works.

# Uploads

The default Sling Post servlets support both streamed and non streamed uploads. Non streamed uploads are processed by the
Sling Engine request processing mechanisms before the information is passed to the the Servlet. While this is more flexible
as the client developer does not have to think about the order of the parameters, it is less efficient as file bodies must
be read completely before processing. This leads to more IO. Streaming uploads on the other hand read the request stream
as it is send from the client and process it as it is read. This avoids some of the additional IO, but requires more attention
to detain by a client developer as the parts of the post must arrive in teh correct order. Streaming is the preference method
of upload where large files are involved.

# Non streamed uploads.

## Whole body uploads.

See https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html
no known variance from the published documentation.

## Sling Chunked body uploads.

Note "Chunked" in this context is a special Sling protocol represented in request parameters intended to allow the client
to upload chunks of a file to Sling. The protocol protects agains multiple clients perfoming an upload on the same resource, 
but does not support multiple clients performing chunked uploads at the same time. Multiple requests are used, multiple chunks cannont
generally be sent in a single request.
See https://cwiki.apache.org/confluence/display/SLING/Chunked+File+Upload+Support.

Chunked uploads are supported by writing the data to special sub nodes of the nt:resource node and when the chunks are complete the subnodes are
read back in sequence creating the final binary. If chunks are send in the wrong order, the chunk is rejected.

The definition of the chunknode differs from the published documentation.

        // node type to store chunk
        // offset: offset of chunk in file
        // jcr:data: binary of chunk
        [sling:chunk] > nt:hierarchyNode
          primaryitem jcr:data
          - sling:offset  (long) mandatory
          - jcr:data (binary) mandatory
          
         //-----------------------------------------------------------------------------
         // Mixin node type to identify that a node has chunks
         // sling:fileLength : length of complete file
         // sling:length : cumulative length of all uploaded chunks
        [sling:chunks]
          mixin
          - sling:fileLength (long)
          - sling:length (long)
          + * (sling:chunk) multiple



# Streamed uploads

Streamed uploads were implemented under the following issues.

https://issues.apache.org/jira/browse/SLING-5948
https://issues.apache.org/jira/browse/SLING-6017
https://issues.apache.org/jira/browse/SLING-6027

For streaming to work the Sling Engine must not read the parts of the request. This is achieved by appending a request parameter 
to the URL "uploadmode=stream" or adding a Header "Sling-uploadmode: stream". When that is done the Sling Engine does not 
read the request, but provides a Iterator<Parts> in a request attribute (request-parts-iterator) that will provide each part as it
is sent from the client. In this mode the SlingDefaultPost Servlet invokes the StreamedUploadOperation which iterates through the Parts.

The StreamedUploadOperation does not support the standard Sling Post protocol. Any node that need to be created with non standard properties 
or structure should be created in a seperate POST. If the node PrimaryType needs to be non standard, that POST operation should be before
any upload is performed.

## Full body streamed uploads.

Full body streamed uploads are performed by a normal file upload. The name of the part is used as the File name. The location of the POST is the parent 
resource which should already exist. If the name of the part is "*" then the supplied name of the file upload is used as the name of the resource. This is 
the same as non streamed full body uploads. The behaviour is specific to Sling and may not be the same as adopeted by other systems.

## Chunked streamed uploads.

Chunked streamed upload are also supported with some subtle variations. Since the length of a body part cant be known until it has been fully streamed, a streaming
chunked upload must specify the length of the body part prior to sending the body part. This can be achieved by in one of 3 ways. Sending a <name>@PartLength form field prior
to the part to indicate the length of the Part. Setting a Content-Length header on the part itself, or using a Content-Range header on the part itself. If all of these are missing,
processing assumes that the body part is the last body part in the file and truncates the file to that length, processing all parts so far.
Unlike the non streamed chunked upload protocol, the streamed upload protocol can accept multiple body parts in 1 request. Each body part
must be immediately preceded by the correct request parameters. (ie new @Offset if using Form fields).

### Via request parameters.

The standard Sling Chunked Upload protocol uses request parameters of the form <name>@Length indicating the final length
of the file and <name>@Offset indicating the offset of the next body part and <name>@PartLength to indicate the 
length of the part. <name>@PartLength is in addition to the nonstreamed protocol. <name> is the name of the body part 
that follows. If any of these parameters are present in the request when a part is encountered, chunked processing is performed. 
The chunk processing will detect when all the chunks have been send and perform final processing.

### Via Content-Range headers

Content Range headers are a part of the Http 1.1 Standard documented at https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html section 14.16. 
If Content Range headers are provided on each Body part they are used in prefence to the propriatary Sling Chunk Upload protocol. This approach 
is used by other resuable and chunked upload apis notably the Google Drive API. If content range headers are used, each part is self contained
and no other request parameters or headers are are required. There is an expectation that a content range header will use the form
"Content-Range: bytes 1234-2345/6789" specifying the full lenght and not the form  "Content-Range: bytes 1234-2345/*" which implies a length of 2345.


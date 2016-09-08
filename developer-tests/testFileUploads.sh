#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

set -e
mkdir -p target
cd target
echo Creating file via normal non streamed upload
cp $1 P1060839.jpg
curl -v -F P1060839.jpg=@P1060839.jpg http://admin:admin@localhost:8080/content/test
curl http://admin:admin@localhost:8080/content/test/P1060839.jpg > P1060839_up.jpg 
echo Checking normal upload.
diff P1060839.jpg P1060839_up.jpg 

echo Creating file via normal non streamed upload
cp P1060839.jpg  SP1060839.jpg  
curl -v -H "Sling-uploadmode: stream" -F key1=value1 -F *=@SP1060839.jpg -F PSP1060839.jpg=@SP1060839.jpg -F key2=admin2 http://admin:admin@localhost:8080/content/test
curl http://admin:admin@localhost:8080/content/test/PSP1060839.jpg > PSP1060839_up.jpg 
curl http://admin:admin@localhost:8080/content/test/SP1060839.jpg > SP1060839_up.jpg 
diff P1060839.jpg PSP1060839_up.jpg
diff P1060839.jpg SP1060839_up.jpg

echo Checking chunked upload in 50k blocks.
rm -f P1060839_chunk*
split -b 20k P1060839.jpg  P1060839_chunk
offset=0
length=`wc -c P1060839.jpg | sed "s/ *\([0-9]*\) .*/\1/"`
for i in P1060839_chunk*; do
   size=`wc -c $i  | sed "s/ *\([0-9]*\) .*/\1/"`
   curl -v -H "Sling-uploadmode: stream" -F CP1060839.jpg@Length=$length -F CP1060839.jpg@PartLength=$size -F CP1060839.jpg@Offset=$offset -F CP1060839.jpg=@$i http://admin:admin@localhost:8080/content/test
   let offset=offset+size
done
curl http://admin:admin@localhost:8080/content/test/CP1060839.jpg > CP1060839_up.jpg
diff -u P1060839.jpg CP1060839_up.jpg

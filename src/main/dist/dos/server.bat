::
::  Licensed to the Apache Software Foundation (ASF) under one
::  or more contributor license agreements.  See the NOTICE file
::  distributed with this work for additional information
::  regarding copyright ownership.  The ASF licenses this file
::  to you under the Apache License, Version 2.0 (the
::  "License"); you may not use this file except in compliance
::  with the License.  You may obtain a copy of the License at
::
::   http://www.apache.org/licenses/LICENSE-2.0
::
::  Unless required by applicable law or agreed to in writing,
::  software distributed under the License is distributed on an
::  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
::  KIND, either express or implied.  See the License for the
::  specific language governing permissions and limitations
::  under the License.

@echo off
setlocal

::-----------------------------------------------------------------------------
:: HTTP Service Arguments
::-----------------------------------------------------------------------------

::* Default Port
::
    set SLING_PORT=8080

::* Default Bind Address
::
    set SLING_ADDR=0.0.0.0

::-----------------------------------------------------------------------------
:: General JVM Arguments
::-----------------------------------------------------------------------------

::* Uncomment one of the JVM_TYPE if you wish a different than '-hotspot'
::
::  set JVM_TYPE=-classic 
::  set JVM_TYPE=-server

::* Memory settings
::
    set JVM_MINHEAP=-Xms64m
    set JVM_MAXHEAP=-Xmx256m

::* Uncomment, or redefine one of the follwoing JAVA_HOME if you wish a
::  different than your default one
::
::  set JAVA_HOME=c:\java\jdk1.4.2_08

::* 'Exectuable'
::
    set JVM_START=-jar bin/${project.build.finalName}-standalone.jar

::* Additional JVM Options
::
::  set JVM_OPTS=-Djava.security.auth.login.config=etc/jaas.config

::* Debug Port (only if started with -debug socket)
::
    set JVM_DEBUG_PORT=30303
    
::* Default Debug Options
::
    set JVM_DEBUG_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE
    

::-----------------------------------------------------------------------------
:: should not change below here
::-----------------------------------------------------------------------------

:: change drive and directory
%~d0
cd %~p0

:: set window title
set PROGRAM_TITLE=Apache Sling

:: parse arguments
:while1
	if "%1"=="-debug" (
		set JVM_DEBUG=%JVM_DEBUG_OPTS%
		set JVM_DEBUG_TRANSPORT=dt_socket
		set JVM_DEBUG_SUSPENDED=n
		set JVM_DEBUG_ADR=%JVM_DEBUG_PORT%
		goto next1
	)
	
	if "%1"=="-suspended" (
		set JVM_DEBUG_SUSPENDED=y
		goto next1
	)

	if "%1"=="-quiet" (
		set NO_INFOMSG=y
		goto next1
	)
	
	if "%1"=="-jconsole" (
		set JVM_JCONSOLE=-Dcom.sun.management.jmxremote
		set NO_INFOMSG=y
		goto next1
	)

	if "%1"=="-level" (
		set SLING_LEVEL=%2
		shift /1
		goto next1
	)
	
	if "%1"=="-help" (
		goto usage
	)
	
	if "%1" NEQ "" (
		echo invalid argument %1
		goto usage
	)
	
:next1
	shift /1
	if "%1" NEQ "" goto while1


:: assemble jvm options
set JVM_ADDOPTS=%JVM_OPTS%
set JVM_OPTS=%JVM_MINHEAP% %JVM_MAXHEAP%
if defined JVM_JCONSOLE set JVM_OPTS=%JVM_JCONSOLE% %JVM_OPTS%
if defined JVM_TYPE     set JVM_OPTS=%JVM_TYPE% %JVM_OPTS%
if defined JVM_ADDOPTS  set JVM_OPTS=%JVM_OPTS% %JVM_ADDOPTS%

:: check for JVM
set JAVA="%JAVA_HOME%\bin\java.exe"
if not exist %JAVA% (
	echo No JVM found at %JAVA%
	goto exit
)

:: check for debug
if defined JVM_DEBUG (
	set JVM_OPTS=%JVM_OPTS% %JVM_DEBUG% -Xrunjdwp:transport=%JVM_DEBUG_TRANSPORT%,server=y,suspend=%JVM_DEBUG_SUSPENDED%,address=%JVM_DEBUG_ADR%
)

:: assemble program arguments
if defined SLING_PORT set SLING_ARGS=%SLING_ARGS% -p %SLING_PORT%
if defined SLING_ADDR set SLING_ARGS=%SLING_ARGS% -a %SLING_ADDR%
if defined SLING_LEVEL set SLING_ARGS=%SLING_ARGS% -l %SLING_LEVEL%

:: ensure logging to stdout
set SLING_ARGS=%SLING_ARGS% -f -

:: print info message
if defined NO_INFOMSG goto startcq

echo -------------------------------------------------------------------------------
echo Starting %PROGRAM_TITLE%
echo -------------------------------------------------------------------------------
call %JAVA% %JVM_TYPE% -version
echo -------------------------------------------------------------------------------
if "%JVM_DEBUG_TRANSPORT%"=="dt_socket" (
	echo debugging: Socket (%JVM_DEBUG_PORT%^)
	if "%JVM_DEBUG_SUSPENDED%"=="y" echo            starting jvm suspended^!
	echo -------------------------------------------------------------------------------
)	
echo %JAVA% %JAVA_VM_TYPE% %JVM_OPTS% %JVM_START% %SLING_ARGS%
echo -------------------------------------------------------------------------------


:startcq

title %PROGRAM_TITLE%
%JAVA% %JAVA_VM_TYPE% %JVM_OPTS% %JVM_START% %SLING_ARGS%
goto exit


::-----------------------------------------------------------------------------
:usage

echo %PROGRAM_TITLE%
echo usage: %0 [options]
echo.
echo where options include:
echo     -debug                    enable debug
echo     -suspended                start suspended (only if debug)
echo     -quiet                    don't show info message
echo     -jconsole                 start with -Dcom.sun.management.jmxremote
echo     -level [ level ]          set initial log level (DEBUG, INFO, ...)
echo     -help                     this usage
echo.
echo for additional tuning, edit the first section of the %0 file

::-----------------------------------------------------------------------------
:exit
endlocal

#!/bin/bash

absPath=$(pwd)
serverPath="/Server"
clientPath="/Client"

# Server compile
(
  cd "${absPath}${serverPath}/src"
  javac -d "${absPath}${serverPath}/target" *.java
  ls "${absPath}${serverPath}/target" \
    | grep -v "Server.class" \
    | xargs -I {} cp "${absPath}${serverPath}/target/{}" "${absPath}${clientPath}/target"
)

# Client compile
(
  cd "${absPath}${clientPath}/src"
  javac -d "${absPath}${clientPath}/target" -cp "${absPath}${clientPath}/target" *.java
)

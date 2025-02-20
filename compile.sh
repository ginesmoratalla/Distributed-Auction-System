#!/bin/bash

absPath=$(pwd)
serverPath="/Server"
clientPath="/Client"

# Subscriber compile
(
  cd "${absPath}${clientPath}/src"
  javac -d "${absPath}${clientPath}/target" IAuctionSubscriber.java
  cp "${absPath}${clientPath}/target/IAuctionSubscriber.class" "${absPath}${serverPath}/target"
  echo "[COMPILATION SUCCESS] Subscriber compiled!"
)

# Server compile
(
  cd "${absPath}${serverPath}/src"
  javac -d "${absPath}${serverPath}/target" -cp "${absPath}${serverPath}/target" *.java
  ls "${absPath}${serverPath}/target" \
    | grep -Ev "^(AuctionServer.class|AuctionUser.class|AuctionItemTypeEnum.class)&" \
    | xargs -I {} cp "${absPath}${serverPath}/target/{}" "${absPath}${clientPath}/target"
  echo "[COMPILATION SUCCESS] Server compiled!"
)

# Client compile
(
  cd "${absPath}${clientPath}/src"
  javac -d "${absPath}${clientPath}/target" -cp "${absPath}${clientPath}/target" *.java
  echo "[COMPILATION SUCCESS] Client compiled!"
)

#!/bin/bash

absPath=$(pwd)
serverPath="/Server"
sharedPath="/Shared"
clientPath="/Client"
jGroupsPath="${absPath}/JGroups/jgroups-3.6.20.jar"
importClasses="CryptoManager.class AuctionItem.class AuctionListing.class API.class"

rm -rf ${absPath}/Client/target/* && rm -rf ${absPath}/Server/target/*

# Shared compile
(
  cd "${absPath}${sharedPath}/"
  javac -d "${absPath}${clientPath}/target" *.java
  javac -d "${absPath}${serverPath}/target" *.java
  echo "[COMPILATION SUCCESS] Shared libs compiled!"
)

# Server compile
(
  cd "${absPath}${serverPath}/src"
  javac -d "${absPath}${serverPath}/target" -cp "${jGroupsPath}:${absPath}${serverPath}/target" *.java
  echo "[COMPILATION SUCCESS] Server compiled!"
)

# Client compile
(
  cd "${absPath}${clientPath}/src"
  javac -d "${absPath}${clientPath}/target" -cp "${jGroupsPath}:${absPath}${clientPath}/target" *.java
  echo "[COMPILATION SUCCESS] Client compiled!"
)

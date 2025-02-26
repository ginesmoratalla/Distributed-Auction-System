#!/bin/bash

absPath=$(pwd)
jGroupsPath="${abspath}/JGroups/jgroups-3.6.20.jar"

(
  cd "${absPath}/Server/target"
  java AuctionServer
  java -cp "${jGroupsPath}" AuctionServer 
)

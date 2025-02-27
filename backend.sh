#!/bin/bash

absPath=$(pwd)
jGroupsPath="${absPath}/JGroups/jgroups-3.6.20.jar"

(
  cd "${absPath}/Server/target"
  java -cp "${jGroupsPath}:." -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=127.0.0.1 AuctionServerBackend
)

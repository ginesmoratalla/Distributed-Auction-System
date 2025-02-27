#!/bin/bash

absPath=$(pwd)
jGroupsPath="${absPath}/JGroups/jgroups-3.6.20.jar"

(
  cd "${absPath}/Client/target"
  java -cp "${jGroupsPath}:." Client 
)

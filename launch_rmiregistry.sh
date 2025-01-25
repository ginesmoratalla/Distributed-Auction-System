#!/bin/bash

absPath=$(pwd)

(
  cd "${absPath}/Server/target/"
  rmiregistry
)

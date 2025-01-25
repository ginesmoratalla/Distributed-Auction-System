#!/bin/bash

absPath=$(pwd)

(
  cd "${absPath}/Server/target"
  java Server
)

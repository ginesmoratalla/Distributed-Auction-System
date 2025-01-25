#!/bin/bash

absPath=$(pwd)

(
  cd "${absPath}/Client/target"
  java Client
)

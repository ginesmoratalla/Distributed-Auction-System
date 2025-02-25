#!/bin/bash

absPath=$(pwd)
rm -rf "${absPath}/Server/keys/*"
rm -rf "${absPath}/Client/keys/*"

(
  cd "${absPath}/Key_Generator/"
  javac *.java
  java KeyGenerator
  cp server_auction_rsa "${absPath}/Server/keys"
  cp server_auction_rsa_pub "${absPath}/Server/keys"
  cp server_auction_rsa_pub "${absPath}/Client/keys"
  rm server_auction_rsa_pub server_auction_rsa KeyGenerator.class
)

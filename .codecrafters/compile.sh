#!/bin/sh
#
# This script is used to compile your program on CodeCrafters
#
# This runs before .codecrafters/run.sh
#
# Learn more: https://codecrafters.io/program-interface

set -e # Exit on failure

mvn -B package -Ddir= C:/Users/Asus/Documents/codecrafters-build-git-java

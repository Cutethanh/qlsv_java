#!/bin/sh
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java") && java -cp out qlsv.Main

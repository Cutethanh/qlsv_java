#!/bin/sh
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out $(find src test -name "*.java") && java -Dstdout.encoding=UTF-8 -cp out qlsv.SelfTest

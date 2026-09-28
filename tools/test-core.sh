#!/usr/bin/env bash
set -euo pipefail
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
javac -d "$out" app/src/main/java/com/projectmister/game/{SubstitutionLedger,MatchMath}.java tests/RegressionTests.java
java -cp "$out" com.projectmister/game/RegressionTests

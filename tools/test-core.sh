#!/usr/bin/env bash
set -euo pipefail
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
javac -d "$out" app/src/main/java/com/projectmister/game/{SubstitutionLedger,MatchMath,MatchMotion,LeagueSchedule,RegistrationWindow,EuropeanAccess}.java tests/RegressionTests.java tests/CompetitionTests.java
java -cp "$out" com.projectmister/game/RegressionTests
java -cp "$out" com.projectmister.game.CompetitionTests

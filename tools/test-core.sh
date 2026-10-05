#!/usr/bin/env bash
set -euo pipefail
python3 tools/validate-competitions.py
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
javac -d "$out" app/src/main/java/com/projectmister/game/{SubstitutionLedger,MatchMath,MatchMotion,KitColours,LeagueSchedule,LeagueResults,KnockoutTie,PromotionCampaign,ScotlandPromotion,ScottishStandings,GermanyPromotion,GermanStandings,TurkeyPromotion,TurkishStandings,IberianPromotion,IberianStandings,BelgianPromotion,BelgianStandings,ReserveEligibility,SaveBackup,RegistrationWindow,EuropeanAccess}.java tests/RegressionTests.java tests/CompetitionTests.java tests/LeagueResultsTests.java tests/KnockoutTieTests.java tests/ScotlandPromotionTests.java tests/ScottishStandingsTests.java tests/GermanyPromotionTests.java tests/GermanStandingsTests.java tests/TurkeyPromotionTests.java tests/TurkishStandingsTests.java tests/IberianTests.java tests/SaveBackupTests.java tests/BelgianTests.java
java -cp "$out" com.projectmister/game/RegressionTests
java -cp "$out" com.projectmister.game.CompetitionTests
java -cp "$out" com.projectmister.game.LeagueResultsTests

java -cp "$out" com.projectmister.game.KnockoutTieTests
java -cp "$out" com.projectmister.game.ScotlandPromotionTests
java -cp "$out" com.projectmister.game.ScottishStandingsTests

java -cp "$out" com.projectmister.game.GermanyPromotionTests
java -cp "$out" com.projectmister.game.GermanStandingsTests

java -cp "$out" com.projectmister.game.TurkeyPromotionTests
java -cp "$out" com.projectmister.game.TurkishStandingsTests

java -cp "$out" com.projectmister.game.IberianTests

java -cp "$out" com.projectmister.game.SaveBackupTests

java -cp "$out" com.projectmister.game.BelgianTests

#!/usr/bin/env bash
set -euo pipefail
python3 tools/validate-competitions.py
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
javac -d "$out" app/src/main/java/com/projectmister/game/{SeasonCup,CupSeasons,DomesticCupFormats,EuropeanLeaguePhase,EuropeanKnockout,EuropeanSeason,EuropeanSeasons,SubstitutionLedger,MatchMath,MatchMotion,KitColours,LeagueSchedule,LeagueResults,KnockoutTie,PortugueseLeagueCup,PortugueseLeagueCupSeason,PortugueseLeagueCupSeasons,DomesticCup,DomesticCupHistory,CompetitionCalendar,PromotionCampaign,ScotlandPromotion,ScottishStandings,GermanyPromotion,GermanStandings,TurkeyPromotion,TurkishStandings,IberianPromotion,IberianStandings,BelgianPromotion,BelgianStandings,FrenchPromotion,FrenchStandings,EnglishPromotion,EnglishStandings,ItalianPromotion,ItalianStandings,DutchPromotion,DutchStandings,DutchQualification,ReserveEligibility,SeasonHistory,WorldMarket,SaveBackup,RegistrationWindow,EuropeanAccess}.java tests/SeasonCupTests.java tests/EuropeanLeaguePhaseTests.java tests/EuropeanKnockoutTests.java tests/DomesticCupTests.java tests/PortugueseLeagueCupTests.java tests/PortugueseLeagueCupSeasonTests.java tests/WorldMarketTests.java tests/SeasonHistoryTests.java tests/RegressionTests.java tests/CompetitionTests.java tests/LeagueResultsTests.java tests/KnockoutTieTests.java tests/ScotlandPromotionTests.java tests/ScottishStandingsTests.java tests/GermanyPromotionTests.java tests/GermanStandingsTests.java tests/TurkeyPromotionTests.java tests/TurkishStandingsTests.java tests/IberianTests.java tests/SaveBackupTests.java tests/BelgianTests.java tests/FrenchTests.java tests/EnglishTests.java tests/ItalianTests.java tests/DutchTests.java
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

java -cp "$out" com.projectmister.game.FrenchTests

java -cp "$out" com.projectmister.game.EnglishTests

java -cp "$out" com.projectmister.game.ItalianTests

java -cp "$out" com.projectmister.game.DutchTests

java -cp "$out" com.projectmister.game.SeasonHistoryTests

java -cp "$out" com.projectmister.game.WorldMarketTests

java -cp "$out" com.projectmister.game.PortugueseLeagueCupTests

java -cp "$out" com.projectmister.game.DomesticCupTests

java -cp "$out" com.projectmister.game.SeasonCupTests
java -cp "$out" com.projectmister.game.EuropeanLeaguePhaseTests

java -cp "$out" com.projectmister.game.EuropeanKnockoutTests

java -cp "$out" com.projectmister.game.PortugueseLeagueCupSeasonTests

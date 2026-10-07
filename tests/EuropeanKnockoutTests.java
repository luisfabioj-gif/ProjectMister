package com.projectmister.game;

import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public final class EuropeanKnockoutTests {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void rejects(Runnable run){try{run.run();}catch(IllegalArgumentException|IllegalStateException e){return;}throw new AssertionError("Invalid European state accepted");}
    private static int id(int rank,int year){return year*1000+rank*7;}
    private static EuropeanSeason fresh(EuropeanLeaguePhase.Competition competition,int year,long seed)throws Exception {
        boolean conference=competition==EuropeanLeaguePhase.Competition.CONFERENCE;
        List<EuropeanLeaguePhase.Club> clubs=new ArrayList<>();
        for(int i=0;i<36;i++)clubs.add(new EuropeanLeaguePhase.Club(id(i,year),""+(char)('A'+i/26)+(char)('A'+i%26),conference?i/6:i%4,100000-i*100));
        List<EuropeanLeaguePhase.Fixture> draw=new ArrayList<>();
        for(String line:Files.readAllLines(Paths.get("tests/fixtures/"+(conference?"conference":"champions")+"-synthetic-draw.csv"))) {
            String[] p=line.split(",");draw.add(new EuropeanLeaguePhase.Fixture(Integer.parseInt(p[0]),id(Integer.parseInt(p[1]),year),id(Integer.parseInt(p[2]),year)));
        }
        List<LocalDate> dates=new ArrayList<>();for(int i=0;i<(conference?6:8);i++)dates.add(LocalDate.of(year,9,10).plusWeeks(2L*i));
        EuropeanLeaguePhase phase=new EuropeanLeaguePhase(competition,year,clubs,draw,dates);
        List<LocalDate> knockout=new ArrayList<>();for(int i=0;i<9;i++)knockout.add(LocalDate.of(year+1,2,1).plusWeeks(2L*i));
        return new EuropeanSeason(phase,seed,knockout,true);
    }
    private static void finishLeague(EuropeanSeason season){EuropeanLeaguePhase phase=season.leaguePhase();for(int i=0;i<phase.fixtureCount();i++)phase.record(i,0,0,0,0);}
    private static EuropeanKnockout play(EuropeanKnockout cup,boolean reload,boolean upset) {
        int games=0;LocalDate last=null;
        while(!cup.complete()) {
            int index=cup.currentIndex(),stage=EuropeanKnockout.stageFor(index);KnockoutTie tie=cup.current();LocalDate date=cup.nextDate();
            check(last==null||!date.isBefore(last),"All first legs precede return legs");last=date;
            if(stage==0) {
                int h=(tie.firstHome-cup.season*1000)/7+1,a=(tie.firstAway-cup.season*1000)/7+1;
                check(a>=9&&a<=16&&h>=17&&h<=24&&(a-9)/2+(h-17)/2==3,"Article 19 playoff pairing and seeded second leg");
            }else if(stage==1) {
                int slot=(index-8)%4,rank=(tie.firstAway-cup.season*1000)/7+1;
                int[] low={5,3,7,1};check(rank>=low[slot]&&rank<=low[slot]+1,"Annex B R16 paths");
            }else if(stage==2) {
                int slot=index-16;check(tie.firstAway==cup.at(9+2*slot).winner(),"QF return home follows inherited 1-4 path");
            }else if(stage==3) {
                int slot=index-20;check(tie.firstAway==cup.at(17+2*slot).winner(),"SF return home follows inherited 1-2 path");
            }else check(tie.neutral&&tie.legs==1&&tie.firstHome==cup.at(20).winner(),"Neutral final, silver nominal home");
            tie.recordRegulation(0,0);games++;
            if(reload){cup=EuropeanKnockout.restore(cup.snapshot());tie=cup.at(index);}
            if(tie.phase()==KnockoutTie.Phase.EXTRA_TIME) {
                tie.recordExtraTime(0,0);
                if(reload){cup=EuropeanKnockout.restore(cup.snapshot());tie=cup.at(index);}
            }
            if(tie.phase()==KnockoutTie.Phase.PENALTIES) {
                tie.recordPenalties(upset?3:5,upset?5:3);
                if(reload)cup=EuropeanKnockout.restore(cup.snapshot());
            }
            cup.validateDate(date);
        }
        check(games==45&&cup.drawnCount()==23&&cup.winner()!=cup.runnerUp(),"Complete UEFA tournament totals");
        check(cup.at(22).aggregate(cup.winner())==0,"Shootouts excluded from goals");return cup;
    }
    public static void main(String[] args)throws Exception {
        Set<String> firstDraws=new HashSet<>();
        for(EuropeanLeaguePhase.Competition competition:EuropeanLeaguePhase.Competition.values())for(int seed=0;seed<32;seed++) {
            EuropeanSeason season=fresh(competition,2026,seed);check(season.knockout()==null,"Knockout waits for league phase");finishLeague(season);
            EuropeanKnockout cup=season.knockout();StringBuilder draw=new StringBuilder();for(int i=0;i<8;i++)draw.append(cup.at(i).firstHome).append(':').append(cup.at(i).firstAway).append(';');firstDraws.add(draw.toString());
            play(cup,true,seed%2==0);
            check(cup.leaguePhase().complete(),"Source ranking retained");
        }
        check(firstDraws.size()>24,"Different seeds vary complete bracket draws");
        for(EuropeanLeaguePhase.Competition competition:EuropeanLeaguePhase.Competition.values()) {
            EuropeanSeasons seasons=new EuropeanSeasons(fresh(competition,2026,19));String first="";
            for(int year=2026;year<2036;year++) {
                EuropeanSeason next=fresh(competition,year+1,year);final EuropeanSeasons unfinished=seasons;rejects(()->unfinished.next(next));
                EuropeanSeason active=seasons.active();
                active.leaguePhase().record(0,0,0,0,0);
                seasons=EuropeanSeasons.restore(seasons.snapshot());active=seasons.active();finishLeague(active);
                check(EuropeanSeason.restore(active.snapshot()).knockout()!=null,"League-to-knockout transition after reload");
                play(active.knockout(),false,true);
                check(active.complete()&&active.winner()>=year*1000,"Season completes with this year's participants");
                if(year==2026)first=active.snapshot();
                seasons=EuropeanSeasons.restore(seasons.next(next).snapshot());
                check(seasons.archives().size()==year-2025&&seasons.archives().get(0).equals(first),"Annual rollover preserves full original history");
                check(seasons.active().fresh()&&seasons.active().winner()==-1,"Fresh next season, no retained results");
            }
            try{seasons.archives().add("changed");throw new AssertionError("Mutable European archive");}catch(UnsupportedOperationException expected){}
        }
        EuropeanSeason fresh=fresh(EuropeanLeaguePhase.Competition.CHAMPIONS,2026,4);finishLeague(fresh);EuropeanKnockout cup=fresh.knockout();
        cup.at(0).recordRegulation(0,0);cup.at(0).recordRegulation(0,0);
        String outOfOrder=cup.snapshot();rejects(()->EuropeanKnockout.restore(outOfOrder));
        EuropeanSeason valid=fresh(EuropeanLeaguePhase.Competition.EUROPA,2026,5);finishLeague(valid);valid.knockout().current().recordRegulation(1,0);
        String saved=valid.snapshot();rejects(()->EuropeanSeason.restore(saved.replace("US1|5|","US1|6|")));
        String bracket=valid.knockout().snapshot();rejects(()->EuropeanKnockout.restore(bracket.replace("EXTRA_TIME_PENALTIES","HIGHER_SEED")));
        rejects(()->valid.validateDate(LocalDate.of(2027,1,1)));
        EuropeanSeason completed=fresh(EuropeanLeaguePhase.Competition.CHAMPIONS,2026,6);finishLeague(completed);play(completed.knockout(),false,false);
        EuropeanSeasons archive=new EuropeanSeasons(completed);
        EuropeanSeason wrongYear=fresh(EuropeanLeaguePhase.Competition.CHAMPIONS,2028,6);rejects(()->archive.next(wrongYear));
        EuropeanSeason wrongCup=fresh(EuropeanLeaguePhase.Competition.EUROPA,2027,6);rejects(()->archive.next(wrongCup));
        EuropeanSeason started=fresh(EuropeanLeaguePhase.Competition.CHAMPIONS,2027,6);started.leaguePhase().record(0,0,0,0,0);rejects(()->archive.next(started));
        System.out.println("PASS: 96 UEFA knockout brackets, 30 recurring seasons, inherited venue seeding, 45 fixtures, ET/penalty reloads, immutable history and malformed-save rejection");
    }
}

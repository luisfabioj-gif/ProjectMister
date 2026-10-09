package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

public final class EuropeanDrawTests {
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static List<EuropeanLeaguePhase.Club> clubs(boolean conference) {
        // Actual 2026/27 Champions League pot association distribution, with synthetic IDs/coefficients.
        String[] country=("FR DE ES ENG IT ENG ENG ES ES DE IT PT ENG PT ENG BE ES NL NL FR NO IT DE ES TR UA TR CZ SK DE GR AT IT FR NO AZ").split(" ");
        ArrayList<EuropeanLeaguePhase.Club> field=new ArrayList<>();
        for(int i=0;i<36;i++)field.add(new EuropeanLeaguePhase.Club(500+7*i,country[i],conference?i/6:i/9,100000-i*100));
        return field;
    }
    private static List<LocalDate> dates(int year,boolean co){ArrayList<LocalDate> dates=new ArrayList<>();for(int r=0;r<(co?6:8);r++)dates.add(LocalDate.of(year,9,10).plusWeeks(r*2L));return dates;}
    private static void venues(EuropeanLeaguePhase phase) {
        int rounds=phase.competition==EuropeanLeaguePhase.Competition.CONFERENCE?6:8;
        for(int c:phase.order()) {
            boolean[] home=new boolean[rounds];
            for(int f=0;f<phase.fixtureCount();f++){EuropeanLeaguePhase.Fixture game=phase.fixture(f);if(game.home==c)home[game.round]=true;}
            check(home[0]!=home[1]&&home[rounds-1]!=home[rounds-2],"opening and closing home/away balance");
            for(int r=0;r<rounds-2;r++)check(!(home[r]==home[r+1]&&home[r]==home[r+2]),"no more than two consecutive home or away matches");
        }
    }
    public static void main(String[] args) {
        for(EuropeanLeaguePhase.Competition competition:EuropeanLeaguePhase.Competition.values()) {
            boolean co=competition==EuropeanLeaguePhase.Competition.CONFERENCE;Set<String> draws=new HashSet<>();
            for(int seed=0;seed<12;seed++) {
                EuropeanLeaguePhase phase=EuropeanDraw.create(competition,2026,clubs(co),dates(2026,co),seed,null);
                check(phase.snapshot().equals(EuropeanDraw.create(competition,2026,clubs(co),dates(2026,co),seed,null).snapshot()),"same draw seed reproduces all fixtures");
                venues(phase);draws.add(phase.snapshot());
                check(phase.snapshot().equals(EuropeanLeaguePhase.restore(phase.snapshot()).snapshot()),"generated draw saves canonically");
            }
            check(draws.size()==12,"draw seeds produce distinct opponents");
            EuropeanLeaguePhase previous=null;
            for(int year=2026;year<2032;year++) {
                EuropeanLeaguePhase current=EuropeanDraw.create(competition,year,clubs(co),dates(year,co),year,previous);venues(current);
                if(previous!=null)for(int f=0;f<current.fixtureCount();f++)for(int p=0;p<previous.fixtureCount();p++)
                    check(!(current.fixture(f).home==previous.fixture(p).home&&current.fixture(f).away==previous.fixture(p).away),"previous-season pairing does not repeat at the same venue");
                previous=current;
            }
        }
        ArrayList<EuropeanLeaguePhase.Club> impossible=new ArrayList<>();for(int c=0;c<36;c++)impossible.add(new EuropeanLeaguePhase.Club(c,"PT",c/9,100000-c));
        try{EuropeanDraw.create(EuropeanLeaguePhase.Competition.CHAMPIONS,2026,impossible,dates(2026,false),0,null);throw new AssertionError("impossible association field accepted");}catch(IllegalArgumentException expected){}
        System.out.println("PASS: 36 generated UEFA draws, 18 consecutive editions, actual multi-club association distribution, pots, venues, previous-season exclusions, deterministic saves and impossible-field rejection");
    }
}

package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;
import java.nio.file.*;

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
    private static void englandRegression()throws Exception {
        String[] input=Files.readString(Path.of("tests/fixtures/eng-2027-draw.txt")).trim().split("\n--PREVIOUS--\n");String[] rows=input[0].split("\n"),header=rows[1].split("\\|");
        ArrayList<EuropeanLeaguePhase.Club> field=new ArrayList<>();for(int i=2;i<rows.length;i++){String[] c=rows[i].split("\\|");field.add(new EuropeanLeaguePhase.Club(Integer.parseInt(c[1]),c[2],Integer.parseInt(c[3]),Integer.parseInt(c[4])));}
        EuropeanLeaguePhase previous=EuropeanLeaguePhase.restore(input[1]);check(input[1].equals(previous.snapshot()),"older ULP1 draw remains byte-identical");
        int year=Integer.parseInt(header[0]);long seed=Long.parseLong(header[1]);EuropeanLeaguePhase phase=EuropeanDraw.create(EuropeanLeaguePhase.Competition.CHAMPIONS,year,field,dates(year,false),seed,previous);venues(phase);
        check(phase.seedingAdjusted()&&phase.snapshot().startsWith("ULP2|"),"impossible projected pots carry saved adjustment metadata");
        int changed=0;for(EuropeanLeaguePhase.Club club:phase.clubs()){EuropeanLeaguePhase.Club original=field.stream().filter(c->c.id==club.id).findFirst().get();check(club.association.equals(original.association)&&club.coefficient==original.coefficient&&club.seedPot==original.pot,"every qualified identity, coefficient and original seed is retained");if(club.pot!=club.seedPot)changed++;}
        check(changed==4,"six English clubs with five in pot one require exactly two seed exchanges");check(phase.clubs().stream().filter(c->c.id==field.get(0).id).findFirst().get().pot==0,"titleholder remains in pot one");
        for(int f=0;f<phase.fixtureCount();f++)for(int p=0;p<previous.fixtureCount();p++)check(!(phase.fixture(f).home==previous.fixture(p).home&&phase.fixture(f).away==previous.fixture(p).away),"actual previous-season venue exclusions retained");
        check(phase.snapshot().equals(EuropeanLeaguePhase.restore(phase.snapshot()).snapshot()),"adjusted seeds and fixtures restore canonically");check(phase.snapshot().equals(EuropeanDraw.create(phase.competition,year,field,dates(year,false),seed,previous).snapshot()),"captured Android career draw replays deterministically");
        String corrupted=phase.snapshot().replaceFirst("\\|0\\nC\\|","|4\nC|");try{EuropeanLeaguePhase.restore(corrupted);throw new AssertionError("invalid original seed accepted");}catch(IllegalArgumentException expected){}
    }
    public static void main(String[] args)throws Exception {
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
        englandRegression();
        System.out.println("PASS: 36 generated UEFA draws, 18 consecutive editions, captured Android England regression, explicit projected-seed adjustments, unchanged entrants, strict opponent/venue restrictions, legacy/canonical saves and impossible-field rejection");
    }
}

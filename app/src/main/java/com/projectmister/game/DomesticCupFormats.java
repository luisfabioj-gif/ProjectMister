package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** Verified round structures. Entrant qualification is supplied, never inferred from incomplete leagues. */
public final class DomesticCupFormats {
    public static final String DFB_RULES="https://www.dfb.de/maenner/wettbewerbe/dfb-pokal/modus";
    public static final String DFB_DATES="https://www.dfb.de/maenner/wettbewerbe/dfb-pokal/rahmentermine";
    public static final String FA_RULES="https://www.thefa.com/-/media/thefacom-new/files/competitions/2026-27/rules/rules-of-the-fa-challenge-cup-2026-27.ashx";
    public static final String FA_DATES="https://www.thefa.com/competitions/thefacup/round-dates";
    private static SeasonCup.Round round(String name,String date,int year,boolean neutral,SeasonCup.Draw draw) {
        return new SeasonCup.Round(name,neutral,KnockoutTie.Rule.EXTRA_TIME_PENALTIES,draw,LocalDate.parse(date).plusYears(year-2026));
    }
    /** Dates choose a day in 2026/27 windows; later years are explicitly projected, not official fixtures. */
    public static SeasonCup dfbPokal(int year,long seed,List<SeasonCup.Entrant> field) {
        if(field==null||field.size()!=64)throw new IllegalArgumentException("DFB-Pokal requires 64 qualified clubs");
        int seeded=0,amateurs=0;
        for(SeasonCup.Entrant e:field){if(e==null||e.stage!=0||e.amateur&&e.openingSeeded)throw new IllegalArgumentException("Invalid DFB pot");if(e.openingSeeded)seeded++;if(e.amateur)amateurs++;}
        if(seeded!=32||amateurs!=28)throw new IllegalArgumentException("DFB qualification/pots incomplete");
        return new SeasonCup("DE_POKAL",year,seed,true,Arrays.asList(
            round("First round","2026-08-23",year,false,SeasonCup.Draw.OPENING_POTS),
            round("Second round","2026-10-28",year,false,SeasonCup.Draw.TWO_POTS),
            round("Round of 16","2026-12-02",year,false,SeasonCup.Draw.LOWER_HOME),
            round("Quarter-final","2027-02-03",year,false,SeasonCup.Draw.LOWER_HOME),
            round("Semi-final","2027-04-21",year,false,SeasonCup.Draw.LOWER_HOME),
            round("Final","2027-05-29",year,true,SeasonCup.Draw.OPEN)),field);
    }
    /** Main competition only: 80 first-round entrants and 44 Premier League/Championship third-round entrants. */
    public static SeasonCup faCup(int year,long seed,int[] firstRound,int[] thirdRound) {
        if(firstRound==null||thirdRound==null||firstRound.length!=80||thirdRound.length!=44)throw new IllegalArgumentException("FA Cup requires qualified main-round field");
        ArrayList<SeasonCup.Entrant> field=new ArrayList<>();for(int c:firstRound)field.add(new SeasonCup.Entrant(c,0,false,false));for(int c:thirdRound)field.add(new SeasonCup.Entrant(c,2,false,false));
        String[] names={"First round","Second round","Third round","Fourth round","Fifth round","Quarter-final","Semi-final","Final"};
        String[] dates={"2026-11-07","2026-12-05","2027-01-09","2027-02-13","2027-03-06","2027-04-03","2027-04-24","2027-05-22"};
        ArrayList<SeasonCup.Round> rounds=new ArrayList<>();for(int i=0;i<8;i++)rounds.add(round(names[i],dates[i],year,i>=6,SeasonCup.Draw.OPEN));
        return new SeasonCup("ENG_FA_CUP",year,seed,true,rounds,field);
    }
    private DomesticCupFormats(){}
}

package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** Published national round structures; annual qualifying fields are career outcomes. */
public final class NationalCupFormats {
    private static SeasonCup.Round r(int year,String name,String date,boolean neutral,KnockoutTie.Rule rule,SeasonCup.Draw draw,String... returnDate) {
        LocalDate first=LocalDate.parse(date).plusYears(year-2026);
        return returnDate.length==0?new SeasonCup.Round(name,neutral,rule,draw,first):new SeasonCup.Round(name,neutral,rule,draw,first,LocalDate.parse(returnDate[0]).plusYears(year-2026));
    }
    public static List<SeasonCup.Round> rounds(String competition,int year,boolean preliminary) {
        KnockoutTie.Rule extra=KnockoutTie.Rule.EXTRA_TIME_PENALTIES,pens=KnockoutTie.Rule.PENALTIES;
        SeasonCup.Draw open=SeasonCup.Draw.OPEN,lower=SeasonCup.Draw.LOWER_CATEGORY;
        ArrayList<SeasonCup.Round> out=new ArrayList<>();
        switch(competition) {
            case "ENG_FA_CUP":
                String[] faNames={"First round","Second round","Third round","Fourth round","Fifth round","Quarter-final","Semi-final","Final"};
                String[] faDates={"2026-11-07","2026-12-05","2027-01-09","2027-02-13","2027-03-06","2027-04-03","2027-04-24","2027-05-22"};
                for(int i=0;i<faDates.length;i++)out.add(r(year,faNames[i],faDates[i],i>=6,extra,open));break;
            case "ENG_LEAGUE_CUP":
                if(preliminary)out.add(r(year,"Preliminary round","2026-08-01",false,pens,open));
                out.add(r(year,"First round","2026-08-08",false,pens,open));out.add(r(year,"Second round","2026-08-26",false,pens,open));
                out.add(r(year,"Third round","2026-09-16",false,pens,SeasonCup.Draw.OPENING_POTS));out.add(r(year,"Fourth round","2026-10-28",false,pens,open));
                out.add(r(year,"Quarter-final","2026-12-16",false,pens,open));out.add(r(year,"Semi-final","2027-01-13",false,extra,open,"2027-02-03"));
                out.add(r(year,"Final","2027-03-21",true,extra,open));break;
            case "ES_COPA":
                out.add(r(year,"First round","2026-10-28",false,extra,lower));out.add(r(year,"Second round","2026-12-02",false,extra,lower));
                out.add(r(year,"Round of 32","2026-12-23",false,extra,lower));out.add(r(year,"Round of 16","2027-01-13",false,extra,lower));
                out.add(r(year,"Quarter-final","2027-01-27",false,extra,lower));out.add(r(year,"Semi-final","2027-02-10",false,extra,open,"2027-03-17"));
                out.add(r(year,"Final","2027-04-24",true,extra,open));break;
            case "IT_COPPA":
                out.add(r(year,"Preliminary round","2026-08-08",false,pens,SeasonCup.Draw.BRACKET_HOME));out.add(r(year,"First round","2026-08-16",false,pens,SeasonCup.Draw.BRACKET_HOME));
                out.add(r(year,"Second round","2026-09-02",false,pens,SeasonCup.Draw.BRACKET_HOME));out.add(r(year,"Round of 16","2026-12-02",false,extra,SeasonCup.Draw.BRACKET_HOME));
                out.add(r(year,"Quarter-final","2027-02-10",false,extra,SeasonCup.Draw.BRACKET_HOME));out.add(r(year,"Semi-final","2027-03-03",false,extra,SeasonCup.Draw.BRACKET,"2027-04-21"));
                out.add(r(year,"Final","2027-05-19",true,extra,SeasonCup.Draw.BRACKET));break;
            case "FR_COUPE":
                String[] frNames={"Seventh round","Eighth round","Round of 64","Round of 32","Round of 16","Quarter-final","Semi-final","Final"};
                String[] frDates={"2026-11-14","2026-11-28","2026-12-20","2027-01-09","2027-02-03","2027-03-03","2027-04-21","2027-05-15"};
                for(int i=0;i<frDates.length;i++)out.add(r(year,frNames[i],frDates[i],i==7,i==7?extra:pens,SeasonCup.Draw.FRENCH_HOME));break;
            case "NL_BEKER":
                String[] nlNames={"First round","Second round","Round of 16","Quarter-final","Semi-final","Final"};
                String[] nlDates={"2026-10-28","2026-12-16","2027-01-13","2027-02-03","2027-03-03","2027-04-18"};
                for(int i=0;i<nlDates.length;i++)out.add(r(year,nlNames[i],nlDates[i],i==5,extra,open));break;
            case "BE_CUP":
                out.add(r(year,"Round of 64","2026-09-27",false,extra,open));out.add(r(year,"Round of 32","2026-10-17",false,extra,SeasonCup.Draw.OPENING_POTS));
                out.add(r(year,"Round of 16","2026-12-05",false,extra,open));out.add(r(year,"Quarter-final","2027-02-03",false,extra,open));
                out.add(r(year,"Semi-final","2027-03-03",false,extra,open,"2027-04-21"));out.add(r(year,"Final","2027-05-29",true,extra,open));break;
            case "SCO_CUP":
                String[] scNames={"Preliminary round one","Preliminary round two","Preliminary round three","First round","Second round","Third round","Fourth round","Fifth round","Quarter-final","Semi-final","Final"};
                String[] scDates={"2026-08-01","2026-08-22","2026-09-12","2026-10-03","2026-10-24","2026-11-28","2027-01-16","2027-02-06","2027-03-06","2027-04-17","2027-05-22"};
                for(int i=0;i<scDates.length;i++)out.add(r(year,scNames[i],scDates[i],i>=9,extra,open));break;
            case "SCO_LEAGUE_CUP":
                out.add(r(year,"Round of 16","2026-08-16",false,extra,SeasonCup.Draw.OPENING_POTS));out.add(r(year,"Quarter-final","2026-09-13",false,extra,open));
                out.add(r(year,"Semi-final","2026-11-01",true,extra,open));out.add(r(year,"Final","2026-12-13",true,extra,open));break;
            case "TR_CUP":
                String[] trDates={"2026-09-16","2026-10-07","2026-10-28","2026-12-02","2026-12-23","2027-01-13","2027-02-10"};
                for(int i=0;i<trDates.length;i++)out.add(r(year,i<5?"Qualifying round "+(i+1):i==5?"Round of 16":"Quarter-final",trDates[i],false,extra,i==0?open:SeasonCup.Draw.SEEDED));
                out.add(r(year,"Semi-final","2027-03-03",false,extra,open,"2027-04-21"));out.add(r(year,"Final","2027-05-29",true,extra,open));break;
            default:throw new IllegalArgumentException("Unknown national format");
        }
        return Collections.unmodifiableList(out);
    }
    public static SeasonCup create(String id,int year,long seed,List<SeasonCup.Entrant> field,boolean preliminary) {
        return new SeasonCup(id,year,seed,true,rounds(id,year,preliminary),field);
    }
    public static void validateStructure(SeasonCup cup) {
        List<SeasonCup.Round> expected=rounds(cup.competition,cup.season,cup.competition.equals("ENG_LEAGUE_CUP")&&cup.roundCount()==8);
        if(!cup.simulatedDates||cup.roundCount()!=expected.size())throw new IllegalArgumentException("Changed national cup structure");
        for(int i=0;i<expected.size();i++) {
            SeasonCup.Round a=cup.round(i),b=expected.get(i);
            if(!a.name.equals(b.name)||a.legs!=b.legs||a.rule!=b.rule||a.draw!=b.draw||a.neutral!=b.neutral)throw new IllegalArgumentException("Changed national cup rule");
            for(int j=0;j<b.legs;j++)if(!a.date(j).equals(b.date(j)))throw new IllegalArgumentException("Changed national cup calendar");
        }
        int[] stages=new int[cup.roundCount()];int seeds=0;for(SeasonCup.Entrant e:cup.entrants()){stages[e.stage]++;if(e.openingSeeded)seeds++;}
        int[] field=null;
        switch(cup.competition) {
            case "ENG_FA_CUP":field=new int[]{80,0,44,0,0,0,0,0};break;
            case "ENG_LEAGUE_CUP": {
                int offset=cup.roundCount()==8?1:0,euro=stages[offset+2];
                if(euro<2||euro>10||cup.entrants().size()!=92||seeds!=Math.min(8,euro))throw new IllegalArgumentException("Invalid EFL European entry gates");
                int preliminary=Math.max(0,4*(euro-8)),byes=Math.max(0,2*(8-euro));
                field=new int[cup.roundCount()];if(offset==1)field[0]=preliminary;field[offset]+=72-preliminary-byes;field[offset+1]=20-euro+byes;field[offset+2]=euro;
                if((preliminary>0)!=(offset==1))throw new IllegalArgumentException("Invalid EFL preliminary parity");break;
            }
            case "ES_COPA":field=new int[]{112,0,4,0,0,0,0};break;
            case "IT_COPPA":field=new int[]{8,28,0,8,0,0,0};break;
            case "FR_COUPE":field=new int[]{176,4,18,0,0,0,0,0};break;
            case "NL_BEKER":{int euro=stages[1];if(euro<1||euro>8)throw new IllegalArgumentException("Invalid Dutch European exemptions");field=new int[]{64-2*euro,euro,0,0,0,0};break;}
            case "BE_CUP":field=new int[]{28,18,0,0,0,0};if(seeds!=16)throw new IllegalArgumentException("Invalid Belgian opening seeds");break;
            case "SCO_CUP":field=new int[]{10,35,0,50,10,20,12,0,0,0,0};break;
            case "SCO_LEAGUE_CUP":field=new int[]{16,0,0,0};if(seeds!=8)throw new IllegalArgumentException("Invalid Scottish last-sixteen seeds");break;
            case "TR_CUP":field=new int[]{40,42,41,18,5,0,0,0,0};break;
        }
        if(field!=null&&!java.util.Arrays.equals(stages,field))throw new IllegalArgumentException("Changed national cup entrants or entry rounds");
    }
    private NationalCupFormats(){}
}

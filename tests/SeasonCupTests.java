package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

public final class SeasonCupTests {
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static void rejects(Runnable r){try{r.run();}catch(IllegalArgumentException|IllegalStateException e){return;}throw new AssertionError("Invalid cup accepted");}
    private static int[] range(int a,int b){return java.util.stream.IntStream.range(a,b).toArray();}
    private static List<SeasonCup.Entrant> german(int offset){ArrayList<SeasonCup.Entrant> field=new ArrayList<>();for(int i=0;i<64;i++)field.add(new SeasonCup.Entrant(offset+i,0,i<28,i>=32));return field;}
    private static SeasonCup finish(SeasonCup cup,Random random) {
        LocalDate previous=LocalDate.of(cup.season,7,1);int matches=0;
        while(!cup.complete()) {
            LocalDate date=cup.nextDate();check(!date.isBefore(previous),"leg calendar never rewinds");previous=date;
            KnockoutTie tie=cup.current();tie.recordRegulation(1,1);matches++;
            cup=SeasonCup.restore(cup.snapshot());tie=cup.current();
            // For two-legged rounds current() may correctly select another first leg.
            for(int i=0;i<cup.drawnCount();i++) {
                KnockoutTie pending=cup.at(i);
                if(pending.phase()==KnockoutTie.Phase.EXTRA_TIME)pending.recordExtraTime(0,0);
                if(pending.phase()==KnockoutTie.Phase.PENALTIES){boolean home=random.nextBoolean();pending.recordPenalties(home?5:4,home?4:5);}
            }
            cup=SeasonCup.restore(cup.snapshot());cup.validateDate(date);
        }
        check(matches>=cup.drawnCount(),"all cup fixtures played");return cup;
    }
    public static void main(String[] args) {
        for(int seed=0;seed<32;seed++) {
            SeasonCup german=DomesticCupFormats.dfbPokal(2026,seed,german(1000));
            for(int i=0;i<32;i++){KnockoutTie t=german.at(i);check(t.home()<1032&&t.away()>=1032,"German opening pots and lower home");}
            german=finish(german,new Random(seed));check(german.drawnCount()==63,"German cup completes 63 ties");
            int[] counts=new int[6];for(int i=0;i<german.drawnCount();i++){int s=german.stage(i);counts[s]++;KnockoutTie t=german.at(i);
                check(t.neutral==(s==5),"German neutral final only");if(s>0&&s<5)check(!(t.firstHome>=1028&&t.firstAway<1028),"German amateur home right survives later draws");}
            check(Arrays.equals(counts,new int[]{32,16,8,4,2,1}),"German stages");
            SeasonCup english=finish(DomesticCupFormats.faCup(2026,seed,range(0,80),range(80,124)),new Random(seed));
            int[] stages=new int[8];for(int i=0;i<english.drawnCount();i++){int s=english.stage(i);stages[s]++;KnockoutTie t=english.at(i);if(s<2)check(t.firstHome<80&&t.firstAway<80,"English late entry");check(t.neutral==(s>=6),"English neutral semi-finals and final");}
            check(Arrays.equals(stages,new int[]{40,20,32,16,8,4,2,1}),"English full main cup structure");
        }
        CupSeasons seasons=new CupSeasons(finish(DomesticCupFormats.dfbPokal(2026,8,german(0)),new Random(8)));
        String first=seasons.active().snapshot();
        for(int year=2027;year<=2046;year++) {
            seasons=seasons.next(DomesticCupFormats.dfbPokal(year,year,german(year*100)));
            check(seasons.active().current().playedLegs()==0&&seasons.active().season==year,"new year has fresh qualified field");
            check(seasons.archives().get(0).equals(first),"first trophy archive never overwritten");
            seasons=CupSeasons.restore(seasons.snapshot());
            SeasonCup played=finish(seasons.active(),new Random(year));
            // Finish helper returns reloaded state, so reconstruct same season history through a saved active replacement.
            String old=seasons.snapshot(),last=old.substring(old.lastIndexOf('\n')+1);
            String encoded=Base64.getEncoder().encodeToString(played.snapshot().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            seasons=CupSeasons.restore(old.substring(0,old.length()-last.length())+encoded);
        }
        check(seasons.archives().size()==20,"twenty completed annual archives retained");
        CupSeasons fresh=new CupSeasons(DomesticCupFormats.dfbPokal(2026,0,german(0)));
        rejects(()->fresh.next(DomesticCupFormats.dfbPokal(2027,1,german(0))));
        SeasonCup c=DomesticCupFormats.faCup(2026,1,range(0,80),range(80,124));String save=c.snapshot();
        rejects(()->SeasonCup.restore(save.replace("E|1|0|0|0","E|0|0|0|0")));
        rejects(()->SeasonCup.restore(save+"\nT|40|1;0;1;1;0;0;EXTRA_TIME_PENALTIES|R,1,0"));
        rejects(()->c.validateDate(LocalDate.of(2026,11,8)));
        List<SeasonCup.Round> rounds=Arrays.asList(new SeasonCup.Round("Semi",false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES,SeasonCup.Draw.OPEN,LocalDate.of(2027,4,1),LocalDate.of(2027,4,8)),new SeasonCup.Round("Final",true,KnockoutTie.Rule.EXTRA_TIME_PENALTIES,SeasonCup.Draw.OPEN,LocalDate.of(2027,5,1)));
        List<SeasonCup.Entrant> four=new ArrayList<>();for(int i=0;i<4;i++)four.add(new SeasonCup.Entrant(i,0,false,false));
        SeasonCup two=new SeasonCup("TEST_CUP",2026,8,true,rounds,four);two.current().recordRegulation(1,0);
        check(two.currentIndex()==1&&two.nextDate().equals(LocalDate.of(2027,4,1)),"all first legs precede return legs");
        two=finish(two,new Random(1));check(two.complete(),"two-leg season cup completes");
        System.out.println("PASS: recurring cups, 20 retained seasons, 32 German/English tournaments, entry stages, home rights, two-leg chronology and corrupted saves");
    }
}

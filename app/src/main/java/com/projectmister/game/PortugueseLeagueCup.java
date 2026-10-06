package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** The published 2026/27 draw. Deliberately not reused for the new 2027/28 format. */
public final class PortugueseLeagueCup {
    private static final String[] IDS={"pt:fc-porto","pt:sporting-cp","pt:sl-benfica","pt:sc-braga",
        "pt:fc-famalicao","pt:gil-vicente-fc","pt:maritimo-m","pt:academico-de-viseu"};
    // Bracket order and chronological order differ. SF day allocation is a simulation.
    private static final String[] DATES={"2026-10-28","2026-10-27","2026-10-29","2026-10-29","2027-01-05","2027-01-06","2027-01-09"};
    private static final int[] ORDER={1,0,3,2,4,5,6};
    private final int[] entrants;
    private final KnockoutTie[] ties=new KnockoutTie[7];

    private PortugueseLeagueCup(int[] clubs) {
        entrants=clubs.clone();Set<Integer> seen=new HashSet<>();
        if(clubs.length!=8)throw new IllegalArgumentException("Eight cup entrants required");
        for(int club:clubs)if(club<0||!seen.add(club))throw new IllegalArgumentException("Invalid cup entrant");
        for(int i=0;i<4;i++)ties[i]=tie(clubs[i],clubs[7-i],false);
    }
    public static PortugueseLeagueCup create(String[] worldIds) {
        int[] clubs=new int[8];Arrays.fill(clubs,-1);
        for(int i=0;i<IDS.length;i++)for(int j=0;j<worldIds.length;j++)if(IDS[i].equals(worldIds[j]))clubs[i]=j;
        return new PortugueseLeagueCup(clubs);
    }
    private static KnockoutTie tie(int h,int a,boolean neutral){return new KnockoutTie(h,a,1,h,neutral,KnockoutTie.Rule.PENALTIES);}
    private boolean done(int i){return ties[i]!=null&&ties[i].phase()==KnockoutTie.Phase.COMPLETE;}
    private void draw() {
        if(ties[4]==null&&done(0)&&done(1)&&done(2)&&done(3)) {
            ties[4]=tie(ties[0].winner(),ties[2].winner(),true);
            ties[5]=tie(ties[1].winner(),ties[3].winner(),true);
        }
        if(ties[6]==null&&done(4)&&done(5))ties[6]=tie(ties[4].winner(),ties[5].winner(),true);
    }
    public int currentIndex(){draw();for(int i:ORDER)if(ties[i]!=null&&!done(i))return i;return -1;}
    public KnockoutTie current(){int i=currentIndex();return i<0?null:ties[i];}
    public KnockoutTie at(int i){draw();return ties[i];}
    public boolean complete(){return currentIndex()<0;}
    public int winner(){return done(6)?ties[6].winner():-1;}
    public LocalDate nextDate(){int i=currentIndex();return i<0?null:date(i);}
    public static LocalDate date(int i){return LocalDate.parse(DATES[i]);}
    public static String roundName(int i){return i<4?"Quarter-final "+(i+1):i<6?"Semi-final "+(i-3):"Final";}
    public boolean due(LocalDate nextLeagueDate){return !complete()&&!nextDate().isAfter(nextLeagueDate);}
    public LocalDate nextEventDate(LocalDate nextLeagueDate){return due(nextLeagueDate)?nextDate():nextLeagueDate;}
    public String snapshot() {
        draw();StringBuilder s=new StringBuilder("PTLC26");for(int c:entrants)s.append(',').append(c);
        for(int i:ORDER)if(ties[i]!=null&&ties[i].playedLegs()>0)s.append('\n').append(i).append(':').append(ties[i].snapshot());
        return s.toString();
    }
    public static PortugueseLeagueCup restore(String text,String[] worldIds) {
        if(text==null||text.length()>4096)throw new IllegalArgumentException("Invalid cup save");
        PortugueseLeagueCup cup=create(worldIds);String[] rows=text.split("\n",-1);
        if(!rows[0].equals(cup.snapshot())||rows.length>8)throw new IllegalArgumentException("Invalid cup draw");
        for(int r=1;r<rows.length;r++) {
            int colon=rows[r].indexOf(':');if(colon<1)throw new IllegalArgumentException("Invalid cup event");
            int index=Integer.parseInt(rows[r].substring(0,colon));
            if(index!=cup.currentIndex())throw new IllegalArgumentException("Cup results out of order");
            KnockoutTie saved=KnockoutTie.restore(rows[r].substring(colon+1)),expected=cup.current();
            if(saved.firstHome!=expected.firstHome||saved.firstAway!=expected.firstAway||saved.legs!=1||saved.neutral!=expected.neutral
                ||saved.higherSeed!=expected.higherSeed||saved.rule!=expected.rule||saved.playedLegs()!=1)
                throw new IllegalArgumentException("Cup tie does not match draw");
            cup.ties[index]=saved;
        }
        if(!text.equals(cup.snapshot()))throw new IllegalArgumentException("Non-canonical cup events");
        return cup;
    }
    public void validateDate(LocalDate currentDate) {
        for(int i=0;i<7;i++)if(ties[i]!=null&&ties[i].playedLegs()>0&&date(i).isAfter(currentDate))
            throw new IllegalArgumentException("Future cup result");
        if(!complete()&&nextDate().isBefore(currentDate))throw new IllegalArgumentException("Overdue cup fixture");
    }
}

package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/** 2026/27 Article 19 / Annex B bracket, shared by all three UEFA competitions. */
public final class EuropeanKnockout {
    private static final int[] START={0,8,16,20,22}, COUNT={8,8,4,2,1};
    // Annex B top-to-bottom on each side: 5/6, 3/4, 7/8, 1/2.
    private static final int[] DIRECT={4,2,6,0}, SEEDED={10,12,8,14}, UNSEEDED={20,18,22,16};
    private final String source;
    private final long seed;
    private final int[] direct=new int[8];
    private final List<LocalDate> dates;
    private final ArrayList<KnockoutTie> ties=new ArrayList<>();
    public final EuropeanLeaguePhase.Competition competition;
    public final int season;
    public final boolean simulatedDates;
    private int stage;

    /** Nine dates: two per round through the semi-finals, then a neutral final. */
    public EuropeanKnockout(EuropeanLeaguePhase phase,long seed,List<LocalDate> dates,boolean simulatedDates) {
        if(phase==null||dates==null||dates.size()!=9)throw new IllegalArgumentException("Missing UEFA knockout calendar");
        phase.qualification(); // Reject incomplete phases and unresolved sporting ties.
        this.source=phase.snapshot();this.competition=phase.competition;this.season=phase.season;
        this.seed=seed;this.simulatedDates=simulatedDates;this.dates=Collections.unmodifiableList(new ArrayList<>(dates));
        LocalDate previous=phase.date(competition==EuropeanLeaguePhase.Competition.CONFERENCE?5:7);
        for(LocalDate d:dates){if(d==null||!d.isAfter(previous)||d.isAfter(LocalDate.of(season+1,6,30)))throw new IllegalArgumentException("Invalid UEFA knockout dates");previous=d;}
        int[] ranks=phase.order();Random random=new Random(seed);
        int[][] home=new int[2][4],away=new int[2][4];
        for(int slot=0;slot<4;slot++) {
            int d=random.nextBoolean()?1:0,s=random.nextBoolean()?1:0,u=random.nextBoolean()?1:0;
            for(int side=0;side<2;side++) {
                direct[side*4+slot]=ranks[DIRECT[slot]+(d^side)];
                home[side][slot]=ranks[UNSEEDED[slot]+(u^side)];
                away[side][slot]=ranks[SEEDED[slot]+(s^side)];
            }
        }
        for(int side=0;side<2;side++)for(int slot=0;slot<4;slot++)add(home[side][slot],away[side][slot],false);
    }
    private void add(int h,int a,boolean neutral){ties.add(new KnockoutTie(h,a,neutral?1:2,a,neutral,KnockoutTie.Rule.EXTRA_TIME_PENALTIES));}
    private void advance() {
        if(stage==4)return;
        for(int i=START[stage];i<START[stage]+COUNT[stage];i++)if(ties.get(i).winner()<0)return;
        if(stage==0)for(int i=0;i<8;i++)add(ties.get(i).winner(),direct[i],false);
        // In both QF pairings, the second R16 slot owns the seeded return-home path.
        else if(stage==1)for(int i=0;i<4;i++)add(ties.get(8+2*i).winner(),ties.get(9+2*i).winner(),false);
        // The lower QF path contains rank 1/2. Its eliminator inherits that venue priority.
        else if(stage==2)for(int i=0;i<2;i++)add(ties.get(16+2*i).winner(),ties.get(17+2*i).winner(),false);
        else add(ties.get(20).winner(),ties.get(21).winner(),true); // Silver is nominal home.
        stage++;
    }
    public static int stageFor(int index){if(index<0||index>=23)throw new IllegalArgumentException("Invalid UEFA tie");for(int s=4;s>=0;s--)if(index>=START[s])return s;throw new AssertionError();}
    public static String roundName(int stage){return new String[]{"Knockout play-offs","Round of 16","Quarter-finals","Semi-finals","Final"}[stage];}
    public LocalDate date(int tie,int leg){int s=stageFor(tie);if(leg<0||leg>(s==4?0:1))throw new IllegalArgumentException("Invalid UEFA leg");return dates.get(s*2+leg);}
    private LocalDate nextDate(int index){KnockoutTie t=ties.get(index);return date(index,Math.min(t.playedLegs(),t.legs-1));}
    public int currentIndex() {
        advance();int first=-1;
        for(int i=START[stage];i<ties.size();i++)if(ties.get(i).winner()<0&&(first<0||nextDate(i).isBefore(nextDate(first))))first=i;
        return first;
    }
    public KnockoutTie current(){int i=currentIndex();return i<0?null:ties.get(i);}
    public LocalDate nextDate(){int i=currentIndex();return i<0?null:nextDate(i);}
    public int drawnCount(){advance();return ties.size();}
    public KnockoutTie at(int i){advance();return ties.get(i);}
    public boolean complete(){return currentIndex()<0;}
    public int winner(){return complete()?ties.get(22).winner():-1;}
    public int runnerUp(){return complete()?ties.get(22).loser():-1;}
    public EuropeanLeaguePhase leaguePhase(){return EuropeanLeaguePhase.restore(source);}
    public void validateDate(LocalDate date) {
        if(date==null)throw new IllegalArgumentException("Missing career date");
        leaguePhase().validateDate(date);
        for(int i=0;i<ties.size();i++)for(int leg=0;leg<ties.get(i).playedLegs();leg++)if(date(i,leg).isAfter(date))throw new IllegalArgumentException("Future UEFA knockout result");
        if(!complete()&&nextDate().isBefore(date))throw new IllegalArgumentException("Overdue UEFA knockout leg");
    }
    public String snapshot() {
        advance();StringBuilder out=new StringBuilder("UK1|").append(seed).append('|').append(simulatedDates?1:0).append('\n')
                .append(Base64.getEncoder().encodeToString(source.getBytes(StandardCharsets.UTF_8)));
        for(LocalDate date:dates)out.append("\nD|").append(date);
        for(int i=0;i<ties.size();i++)if(ties.get(i).playedLegs()>0)out.append("\nT|").append(i).append('|').append(ties.get(i).snapshot());
        return out.toString();
    }
    public static EuropeanKnockout restore(String text) {
        if(text==null||text.length()>131072)throw new IllegalArgumentException("Invalid UEFA knockout save size");
        String[] rows=text.split("\n",-1),h=rows[0].split("\\|",-1);
        if(h.length!=3||!h[0].equals("UK1")||(!h[2].equals("0")&&!h[2].equals("1"))||rows.length<11||rows.length>34)throw new IllegalArgumentException("Invalid UEFA knockout save");
        EuropeanLeaguePhase phase=EuropeanLeaguePhase.restore(new String(Base64.getDecoder().decode(rows[1]),StandardCharsets.UTF_8));
        ArrayList<LocalDate> dates=new ArrayList<>();for(int i=2;i<11;i++){if(!rows[i].startsWith("D|"))throw new IllegalArgumentException("Missing UEFA date");dates.add(LocalDate.parse(rows[i].substring(2)));}
        EuropeanKnockout cup=new EuropeanKnockout(phase,Long.parseLong(h[1]),dates,h[2].equals("1"));int previous=-1;
        for(int row=11;row<rows.length;row++) {
            String[] t=rows[row].split("\\|",3);if(t.length!=3||!t[0].equals("T"))throw new IllegalArgumentException("Invalid UEFA tie record");
            int i=Integer.parseInt(t[1]);cup.advance();
            if(i<=previous||i>=cup.ties.size())throw new IllegalArgumentException("Result outside UEFA draw");
            KnockoutTie expected=cup.ties.get(i),saved=KnockoutTie.restore(t[2]);
            if(saved.firstHome!=expected.firstHome||saved.firstAway!=expected.firstAway||saved.legs!=expected.legs||saved.neutral!=expected.neutral||saved.higherSeed!=expected.higherSeed||saved.rule!=expected.rule||saved.playedLegs()==0)throw new IllegalArgumentException("UEFA result violates bracket");
            cup.ties.set(i,saved);previous=i;
        }
        LocalDate latest=null;
        for(int i=0;i<cup.ties.size();i++){KnockoutTie tie=cup.ties.get(i);if(tie.playedLegs()>0){LocalDate d=cup.date(i,tie.playedLegs()-1);if(latest==null||d.isAfter(latest))latest=d;}}
        if(latest!=null)cup.validateDate(latest);
        if(!text.equals(cup.snapshot()))throw new IllegalArgumentException("Non-canonical UEFA knockout save");return cup;
    }
}

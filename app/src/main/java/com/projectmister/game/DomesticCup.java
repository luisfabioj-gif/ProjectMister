package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** Season-owned Portuguese cup. The first edition preserves the published 2026 draw. */
public final class DomesticCup {
    public final int season;
    private final int[] counts=new int[8];
    private static final String[] DATES={"2026-08-30","2026-09-20","2026-10-18","2026-11-22","2026-12-16","2027-02-03","2027-05-22","2027-05-30"};
    private final int worldSize;
    private final long seed;
    private final int[] opening,byes,second,third,late;
    private final ArrayList<KnockoutTie> games=new ArrayList<>();
    private final ArrayList<Integer> stages=new ArrayList<>();
    private int cursor=0,lastStage=0;
    public DomesticCup(int worldSize,long seed,int[] opening,int[] byes,int[] second,int[] third,int[] late) {
        this(2026,worldSize,seed,opening,byes,second,third,late);
    }
    private DomesticCup(int season,int worldSize,long seed,int[] opening,int[] byes,int[] second,int[] third,int[] late) {
        if(season<2026||season>2200||worldSize<64||worldSize>768||opening==null||byes==null||second==null||third==null||late==null||opening.length<2||opening.length%2!=0||late.length>8)
            throw new IllegalArgumentException("Invalid Portuguese cup field");
        if(season==2026&&(opening.length!=94||byes.length!=19||second.length!=14||third.length!=14||late.length!=5))throw new IllegalArgumentException("Invalid initial cup field");
        this.season=season;
        counts[0]=opening.length/2;
        int[] incoming={0,byes.length+second.length,third.length,late.length,0,0,0,0};
        for(int i=1;i<8;i++){int field=counts[i-1]+incoming[i];if(field<2||field%2!=0)throw new IllegalArgumentException("Unbalanced cup round");counts[i]=field/2;}
        if(counts[3]!=16||counts[7]!=1)throw new IllegalArgumentException("Invalid final cup rounds");
        this.worldSize=worldSize;this.seed=seed;this.opening=opening.clone();this.byes=byes.clone();this.second=second.clone();this.third=third.clone();this.late=late.clone();
        Set<Integer> seen=new HashSet<>();
        for(int[] group:new int[][]{opening,byes,second,third,late})for(int c:group)
            if(c<0||c>=worldSize||!seen.add(c))throw new IllegalArgumentException("Duplicate or absent cup club");
        for(int i=0;i<opening.length;i+=2)add(opening[i],opening[i+1],0);
    }
    public static DomesticCup nextSeason(int season,long seed,int[] tiers,boolean[] reserves,int[] europeanClubs) {
        if(season<=2026||tiers==null||reserves==null||tiers.length!=reserves.length||europeanClubs==null)throw new IllegalArgumentException("Invalid recurring cup inputs");
        Set<Integer> late=new HashSet<>();
        for(int club:europeanClubs)if(club<0||club>=tiers.length||reserves[club]||!late.add(club))throw new IllegalArgumentException("Invalid deferred entrant");
        ArrayList<Integer> lower=new ArrayList<>(),second=new ArrayList<>(),third=new ArrayList<>();
        for(int i=0;i<tiers.length;i++)if(!reserves[i]&&!late.contains(i)) {
            if(tiers[i]==3)lower.add(i);else if(tiers[i]==2)second.add(i);else if(tiers[i]==1)third.add(i);else if(tiers[i]!=4)throw new IllegalArgumentException("Invalid cup tier");
        }
        int byes=2*(2*(2*(32-late.size())-third.size())-second.size())-lower.size();
        if(byes<0||byes>=lower.size())throw new IllegalArgumentException("Unsupported qualified lower-tier field");
        Collections.shuffle(lower,new Random(seed));int split=lower.size()-byes;
        DomesticCup cup=new DomesticCup(season,tiers.length,seed,array(lower.subList(0,split)),array(lower.subList(split,lower.size())),array(second),array(third),europeanClubs);
        cup.validateEntrants(reserves);return cup;
    }
    private static int[] array(List<Integer> values){int[] out=new int[values.size()];for(int i=0;i<out.length;i++)out[i]=values.get(i);return out;}
    private void add(int h,int a,int stage) {
        games.add(new KnockoutTie(h,a,1,h,stage>=6,KnockoutTie.Rule.EXTRA_TIME_PENALTIES));stages.add(stage);
    }
    private void advance() {
        while(cursor<games.size()&&games.get(cursor).phase()==KnockoutTie.Phase.COMPLETE)cursor++;
        if(cursor<games.size()||lastStage==7)return;
        ArrayList<Integer> pool=new ArrayList<>();
        for(int i=0;i<games.size();i++)if(stages.get(i)==lastStage)pool.add(games.get(i).winner());
        int stage=lastStage+1;
        if(stage==1)for(int c:byes)pool.add(c);
        Random draw=new Random(seed+stage*7919L);Collections.shuffle(pool,draw);
        int[] entries=stage==1?second:stage==2?third:stage==3?late:new int[0];
        if(stage<=2) {
            // Second-/first-division entrants cannot draw each other and play away.
            ArrayList<Integer> incoming=new ArrayList<>();for(int c:entries)incoming.add(c);Collections.shuffle(incoming,draw);
            for(int away:incoming)add(pool.remove(pool.size()-1),away,stage);
        } else {for(int c:entries)pool.add(c);Collections.shuffle(pool,draw);}
        if(pool.size()%2!=0)throw new IllegalStateException("Odd cup draw");
        for(int i=0;i<pool.size();i+=2)add(pool.get(i),pool.get(i+1),stage);
        lastStage=stage;
        int count=0;for(int s:stages)if(s==stage)count++;
        if(count!=counts[stage])throw new IllegalStateException("Unexpected cup round size");
    }
    public KnockoutTie current(){advance();return cursor<games.size()?games.get(cursor):null;}
    public boolean complete(){return current()==null;}
    public int currentIndex(){advance();return complete()?-1:cursor;}
    public int winner(){return complete()?games.get(games.size()-1).winner():-1;}
    public int drawnCount(){advance();return games.size();}
    public KnockoutTie at(int index){advance();return games.get(index);}
    public int stage(int index){advance();return stages.get(index);}
    public String roundName(int index){int s=stage(index);return s==7?"Final":s==6?"Semi-final":s==5?"Quarter-final":s==4?"Round of 16":"Round "+(s+1);}
    public LocalDate date(int index) {
        int stage=stages.get(index);LocalDate date=LocalDate.parse(DATES[stage]).plusYears(season-2026);
        if(stage==6&&index>0&&stages.get(index-1)==6)date=date.plusDays(1);
        return date;
    }
    public LocalDate nextDate(){return complete()?null:date(cursor);}
    public boolean due(LocalDate horizon){return !complete()&&!nextDate().isAfter(horizon);}
    private static String ids(int[] values){StringJoiner out=new StringJoiner(",");for(int i:values)out.add(String.valueOf(i));return out.toString();}
    private static int[] ids(String text){if(text.length()>2048)throw new IllegalArgumentException("Oversized cup field");String[] words=text.split(",",-1);int[] out=new int[words.length];for(int i=0;i<out.length;i++)out[i]=Integer.parseInt(words[i]);return out;}
    public String snapshot() {
        advance();StringBuilder s=new StringBuilder(season==2026?"TACA26;":"TACA2;"+season+";").append(worldSize).append(';').append(seed);
        for(int[] field:new int[][]{opening,byes,second,third,late})s.append('\n').append(ids(field));
        for(KnockoutTie tie:games)if(tie.playedLegs()>0)s.append('\n').append(tie.snapshot());
        return s.toString();
    }
    public static DomesticCup restore(String value,int worldSize) {
        if(value==null||value.length()>80000)throw new IllegalArgumentException("Oversized cup snapshot");
        String[] lines=value.split("\n",-1);if(lines.length<6||lines.length>worldSize+5)throw new IllegalArgumentException("Invalid cup snapshot");
        String[] h=lines[0].split(";",-1);boolean legacy=h.length==3&&h[0].equals("TACA26");
        if(!legacy&&(h.length!=4||!h[0].equals("TACA2")))throw new IllegalArgumentException("Invalid cup schema");
        int shift=legacy?0:1,season=legacy?2026:Integer.parseInt(h[1]);
        if(Integer.parseInt(h[1+shift])!=worldSize||!legacy&&season==2026)throw new IllegalArgumentException("Wrong cup universe");
        DomesticCup cup=new DomesticCup(season,worldSize,Long.parseLong(h[2+shift]),ids(lines[1]),ids(lines[2]),ids(lines[3]),ids(lines[4]),ids(lines[5]));
        for(int i=6;i<lines.length;i++) {
            KnockoutTie expected=cup.current(),saved=KnockoutTie.restore(lines[i]);
            if(expected==null||saved.firstHome!=expected.firstHome||saved.firstAway!=expected.firstAway||saved.neutral!=expected.neutral
                ||saved.legs!=1||saved.higherSeed!=expected.higherSeed||saved.rule!=expected.rule||saved.playedLegs()!=1)
                throw new IllegalArgumentException("Cup result violates saved draw");
            cup.games.set(cup.cursor,saved);
            if(i<lines.length-1&&saved.phase()!=KnockoutTie.Phase.COMPLETE)throw new IllegalArgumentException("Unfinished earlier cup tie");
        }
        if(!value.equals(cup.snapshot()))throw new IllegalArgumentException("Non-canonical cup save");return cup;
    }
    public void validateDate(LocalDate date) {
        for(int i=0;i<games.size();i++)if(games.get(i).playedLegs()>0&&date(i).isAfter(date))throw new IllegalArgumentException("Future cup result");
        if(!complete()&&nextDate().isBefore(date))throw new IllegalArgumentException("Overdue cup tie");
    }
    public void validateEntrants(boolean[] reserves) {
        if(reserves.length!=worldSize)throw new IllegalArgumentException("Wrong club registry");
        int expected=0;for(boolean reserve:reserves)if(!reserve)expected++;
        if(opening.length+byes.length+second.length+third.length+late.length!=expected)throw new IllegalArgumentException("Incomplete domestic cup field");
        for(int[] field:new int[][]{opening,byes,second,third,late})for(int c:field)if(reserves[c])throw new IllegalArgumentException("Reserve in domestic cup");
    }
}

package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** Saved 146-club Portuguese cup. Opening draw is data; later draws follow career results. */
public final class DomesticCup {
    private static final int[] COUNTS={47,40,27,16,8,4,2,1};
    private static final String[] DATES={"2026-08-30","2026-09-20","2026-10-18","2026-11-22","2026-12-16","2027-02-03","2027-05-22","2027-05-30"};
    private final int worldSize;
    private final long seed;
    private final int[] opening,byes,second,third,late;
    private final ArrayList<KnockoutTie> games=new ArrayList<>();
    private final ArrayList<Integer> stages=new ArrayList<>();
    private int cursor=0,lastStage=0;
    public DomesticCup(int worldSize,long seed,int[] opening,int[] byes,int[] second,int[] third,int[] late) {
        if(worldSize<146||worldSize>256||opening.length!=94||byes.length!=19||second.length!=14||third.length!=14||late.length!=5)
            throw new IllegalArgumentException("Invalid Portuguese cup field");
        this.worldSize=worldSize;this.seed=seed;this.opening=opening.clone();this.byes=byes.clone();this.second=second.clone();this.third=third.clone();this.late=late.clone();
        Set<Integer> seen=new HashSet<>();
        for(int[] group:new int[][]{opening,byes,second,third,late})for(int c:group)
            if(c<0||c>=worldSize||!seen.add(c))throw new IllegalArgumentException("Duplicate or absent cup club");
        for(int i=0;i<opening.length;i+=2)add(opening[i],opening[i+1],0);
    }
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
        if(count!=COUNTS[stage])throw new IllegalStateException("Unexpected cup round size");
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
        int stage=stages.get(index);LocalDate date=LocalDate.parse(DATES[stage]);
        if(stage==6&&index>0&&stages.get(index-1)==6)date=date.plusDays(1);
        return date;
    }
    public LocalDate nextDate(){return complete()?null:date(cursor);}
    public boolean due(LocalDate horizon){return !complete()&&!nextDate().isAfter(horizon);}
    private static String ids(int[] values){StringJoiner out=new StringJoiner(",");for(int i:values)out.add(String.valueOf(i));return out.toString();}
    private static int[] ids(String text){if(text.length()>2048)throw new IllegalArgumentException("Oversized cup field");String[] words=text.split(",",-1);int[] out=new int[words.length];for(int i=0;i<out.length;i++)out[i]=Integer.parseInt(words[i]);return out;}
    public String snapshot() {
        advance();StringBuilder s=new StringBuilder("TACA26;").append(worldSize).append(';').append(seed);
        for(int[] field:new int[][]{opening,byes,second,third,late})s.append('\n').append(ids(field));
        for(KnockoutTie tie:games)if(tie.playedLegs()>0)s.append('\n').append(tie.snapshot());
        return s.toString();
    }
    public static DomesticCup restore(String value,int worldSize) {
        if(value==null||value.length()>80000)throw new IllegalArgumentException("Oversized cup snapshot");
        String[] lines=value.split("\n",-1);if(lines.length<6||lines.length>151)throw new IllegalArgumentException("Invalid cup snapshot");
        String[] h=lines[0].split(";",-1);if(h.length!=3||!h[0].equals("TACA26")||Integer.parseInt(h[1])!=worldSize)throw new IllegalArgumentException("Wrong cup universe");
        DomesticCup cup=new DomesticCup(worldSize,Long.parseLong(h[2]),ids(lines[1]),ids(lines[2]),ids(lines[3]),ids(lines[4]),ids(lines[5]));
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
        for(int[] field:new int[][]{opening,byes,second,third,late})for(int c:field)if(reserves[c])throw new IllegalArgumentException("Reserve in domestic cup");
    }
}

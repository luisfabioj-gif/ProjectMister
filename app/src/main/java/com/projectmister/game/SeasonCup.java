package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/** Season-owned cup draw. Callers supply qualified clubs and verified (or explicitly simulated) dates. */
public final class SeasonCup {
    public enum Draw { OPEN, LOWER_HOME, TWO_POTS, OPENING_POTS }
    public static final class Round {
        public final String name;
        public final int legs;
        public final boolean neutral;
        public final KnockoutTie.Rule rule;
        public final Draw draw;
        private final LocalDate[] dates;
        public Round(String name,boolean neutral,KnockoutTie.Rule rule,Draw draw,LocalDate... dates) {
            if(name==null||name.isEmpty()||name.length()>80||dates==null||(dates.length!=1&&dates.length!=2)||neutral&&dates.length!=1||rule==null||draw==null)
                throw new IllegalArgumentException("Invalid cup round");
            for(int i=0;i<dates.length;i++)if(dates[i]==null||i>0&&!dates[i].isAfter(dates[i-1]))throw new IllegalArgumentException("Invalid leg dates");
            this.name=name;this.neutral=neutral;this.rule=rule;this.draw=draw;this.dates=dates.clone();legs=dates.length;
        }
        public LocalDate date(int leg){return dates[leg];}
    }
    public static final class Entrant {
        public final int club,stage;
        public final boolean amateur,openingSeeded;
        public Entrant(int club,int stage,boolean amateur,boolean openingSeeded) {
            if(club<0||stage<0)throw new IllegalArgumentException("Invalid entrant");
            this.club=club;this.stage=stage;this.amateur=amateur;this.openingSeeded=openingSeeded;
        }
    }
    public final String competition;
    public final int season;
    public final boolean simulatedDates;
    private final long seed;
    private final List<Round> rounds;
    private final List<Entrant> entrants;
    private final Map<Integer,Entrant> clubs=new HashMap<>();
    private final ArrayList<KnockoutTie> ties=new ArrayList<>();
    private final ArrayList<Integer> stages=new ArrayList<>();
    private int stage;
    public SeasonCup(String competition,int season,long seed,boolean simulatedDates,List<Round> rounds,List<Entrant> entrants) {
        if(competition==null||!competition.matches("[A-Z][A-Z0-9_]{1,39}")||season<2026||season>2200||rounds==null||rounds.isEmpty()||rounds.size()>12||entrants==null||entrants.size()<2||entrants.size()>1024)
            throw new IllegalArgumentException("Invalid season cup");
        this.competition=competition;this.season=season;this.seed=seed;this.simulatedDates=simulatedDates;
        this.rounds=Collections.unmodifiableList(new ArrayList<>(rounds));this.entrants=Collections.unmodifiableList(new ArrayList<>(entrants));
        int[] entries=new int[rounds.size()];
        for(Entrant e:entrants)if(e==null||e.stage>=rounds.size()||clubs.put(e.club,e)!=null)throw new IllegalArgumentException("Duplicate or invalid cup entrant");else entries[e.stage]++;
        int survivors=0;
        for(int i=0;i<rounds.size();i++) {
            Round r=rounds.get(i);
            if(r==null||r.date(0).isBefore(LocalDate.of(season,7,1))||r.date(r.legs-1).isAfter(LocalDate.of(season+1,7,31))
                ||i>0&&!r.date(0).isAfter(rounds.get(i-1).date(rounds.get(i-1).legs-1)))throw new IllegalArgumentException("Invalid season calendar");
            int n=survivors+entries[i];if(n<2||n%2!=0)throw new IllegalArgumentException("Unbalanced entry round");survivors=n/2;
        }
        if(survivors!=1)throw new IllegalArgumentException("Cup must produce one champion");
        drawRound(new ArrayList<>(),0);
    }
    private void add(int h,int a,int stage) {
        Round r=rounds.get(stage);
        if(!r.neutral&&(r.draw==Draw.LOWER_HOME||r.draw==Draw.TWO_POTS)&&!clubs.get(h).amateur&&clubs.get(a).amateur){int tmp=h;h=a;a=tmp;}
        ties.add(new KnockoutTie(h,a,r.legs,h,r.neutral,r.rule));stages.add(stage);
    }
    private void drawRound(ArrayList<Integer> pool,int next) {
        for(Entrant e:entrants)if(e.stage==next)pool.add(e.club);
        Collections.sort(pool);Random random=new Random(seed+7919L*next);Collections.shuffle(pool,random);
        Round r=rounds.get(next);
        if(r.draw==Draw.TWO_POTS||r.draw==Draw.OPENING_POTS) {
            ArrayList<Integer> lower=new ArrayList<>(),upper=new ArrayList<>();
            for(int c:pool)if(r.draw==Draw.OPENING_POTS?!clubs.get(c).openingSeeded:clubs.get(c).amateur)lower.add(c);else upper.add(c);
            pool.clear();
            while(!lower.isEmpty()&&!upper.isEmpty())add(lower.remove(lower.size()-1),upper.remove(upper.size()-1),next);
            pool.addAll(lower);pool.addAll(upper);
        }
        for(int i=0;i<pool.size();i+=2)add(pool.get(i),pool.get(i+1),next);
        stage=next;
    }
    private void advance() {
        if(stage==rounds.size()-1)return;
        ArrayList<Integer> winners=new ArrayList<>();
        for(int i=0;i<ties.size();i++)if(stages.get(i)==stage){if(ties.get(i).winner()<0)return;winners.add(ties.get(i).winner());}
        drawRound(winners,stage+1);
    }
    public int currentIndex() {
        advance();int best=-1;LocalDate first=null;
        for(int i=0;i<ties.size();i++)if(ties.get(i).winner()<0) {
            LocalDate date=nextDate(i);if(first==null||date.isBefore(first)){first=date;best=i;}
        }
        return best;
    }
    private LocalDate nextDate(int i){KnockoutTie t=ties.get(i);return rounds.get(stages.get(i)).date(Math.min(t.playedLegs(),t.legs-1));}
    public KnockoutTie current(){int i=currentIndex();return i<0?null:ties.get(i);}
    public LocalDate nextDate(){int i=currentIndex();return i<0?null:nextDate(i);}
    public boolean complete(){return currentIndex()<0;}
    public int winner(){return complete()?ties.get(ties.size()-1).winner():-1;}
    public int drawnCount(){advance();return ties.size();}
    public KnockoutTie at(int i){advance();return ties.get(i);}
    public int stage(int i){advance();return stages.get(i);}
    public Round round(int i){return rounds.get(i);}
    public List<Entrant> entrants(){return entrants;}
    public void validateDate(LocalDate date) {
        if(date==null)throw new IllegalArgumentException("Missing career date");
        for(int i=0;i<ties.size();i++)for(int leg=0;leg<ties.get(i).playedLegs();leg++)
            if(rounds.get(stages.get(i)).date(leg).isAfter(date))throw new IllegalArgumentException("Future cup result");
        if(!complete()&&nextDate().isBefore(date))throw new IllegalArgumentException("Overdue cup tie");
    }
    private static String encode(String s){return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    private static String decode(String s){return new String(Base64.getUrlDecoder().decode(s),StandardCharsets.UTF_8);}
    public String snapshot() {
        advance();StringBuilder out=new StringBuilder("SC1|").append(competition).append('|').append(season).append('|').append(seed).append('|').append(simulatedDates?1:0).append('|').append(rounds.size()).append('|').append(entrants.size());
        for(Round r:rounds){out.append("\nR|").append(encode(r.name)).append('|').append(r.neutral?1:0).append('|').append(r.rule).append('|').append(r.draw);for(LocalDate date:r.dates)out.append('|').append(date);}
        for(Entrant e:entrants)out.append("\nE|").append(e.club).append('|').append(e.stage).append('|').append(e.amateur?1:0).append('|').append(e.openingSeeded?1:0);
        for(int i=0;i<ties.size();i++)if(ties.get(i).playedLegs()>0)out.append("\nT|").append(i).append('|').append(ties.get(i).snapshot());
        return out.toString();
    }
    private static boolean bit(String s){if(!s.equals("0")&&!s.equals("1"))throw new IllegalArgumentException("Invalid flag");return s.equals("1");}
    public static SeasonCup restore(String value) {
        if(value==null||value.length()>524288)throw new IllegalArgumentException("Invalid cup save size");
        String[] lines=value.split("\n",-1),h=lines[0].split("\\|",-1);
        if(h.length!=7||!h[0].equals("SC1"))throw new IllegalArgumentException("Invalid cup schema");
        int nr=Integer.parseInt(h[5]),ne=Integer.parseInt(h[6]);
        if(nr<1||nr>12||ne<2||ne>1024||lines.length<1+nr+ne||lines.length>nr+2*ne)throw new IllegalArgumentException("Invalid cup save count");
        ArrayList<Round> rounds=new ArrayList<>();ArrayList<Entrant> entrants=new ArrayList<>();
        for(int i=1;i<=nr;i++) {
            String[] r=lines[i].split("\\|",-1);if(r.length<6||r.length>7||!r[0].equals("R"))throw new IllegalArgumentException("Invalid round");
            LocalDate[] dates=new LocalDate[r.length-5];for(int j=0;j<dates.length;j++)dates[j]=LocalDate.parse(r[j+5]);
            rounds.add(new Round(decode(r[1]),bit(r[2]),KnockoutTie.Rule.valueOf(r[3]),Draw.valueOf(r[4]),dates));
        }
        for(int i=nr+1;i<=nr+ne;i++) {
            String[] e=lines[i].split("\\|",-1);if(e.length!=5||!e[0].equals("E"))throw new IllegalArgumentException("Invalid entrant");
            entrants.add(new Entrant(Integer.parseInt(e[1]),Integer.parseInt(e[2]),bit(e[3]),bit(e[4])));
        }
        SeasonCup cup=new SeasonCup(h[1],Integer.parseInt(h[2]),Long.parseLong(h[3]),bit(h[4]),rounds,entrants);
        int previous=-1;
        for(int i=1+nr+ne;i<lines.length;i++) {
            String[] t=lines[i].split("\\|",3);if(t.length!=3||!t[0].equals("T"))throw new IllegalArgumentException("Invalid tie record");
            int index=Integer.parseInt(t[1]);cup.advance();
            if(index<=previous||index>=cup.ties.size())throw new IllegalArgumentException("Cup result outside drawn round");
            KnockoutTie expected=cup.ties.get(index),saved=KnockoutTie.restore(t[2]);
            if(saved.firstHome!=expected.firstHome||saved.firstAway!=expected.firstAway||saved.legs!=expected.legs||saved.neutral!=expected.neutral||saved.higherSeed!=expected.higherSeed||saved.rule!=expected.rule||saved.playedLegs()==0)
                throw new IllegalArgumentException("Cup result violates draw");
            cup.ties.set(index,saved);previous=index;
        }
        LocalDate lastPlayed=null;
        for(int i=0;i<cup.ties.size();i++){KnockoutTie t=cup.ties.get(i);if(t.playedLegs()>0){LocalDate d=cup.rounds.get(cup.stages.get(i)).date(t.playedLegs()-1);if(lastPlayed==null||d.isAfter(lastPlayed))lastPlayed=d;}}
        if(lastPlayed!=null&&!cup.complete()&&cup.nextDate().isBefore(lastPlayed))throw new IllegalArgumentException("Cup legs played out of order");
        if(!value.equals(cup.snapshot()))throw new IllegalArgumentException("Non-canonical cup save");return cup;
    }
}

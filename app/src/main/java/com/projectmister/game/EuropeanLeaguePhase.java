package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** UEFA league-phase state, draw validation and Article 18 ranking. No invented entrants or access places. */
public final class EuropeanLeaguePhase {
    public enum Competition { CHAMPIONS, EUROPA, CONFERENCE }
    public static final class Club {
        public final int id,pot,coefficient;
        public final String association;
        /** Coefficients use thousandths to preserve exact ordering. */
        public Club(int id,String association,int pot,int coefficient) {
            if(id<0||association==null||!association.matches("[A-Z]{2,4}")||pot<0||coefficient<0||coefficient>1000000)throw new IllegalArgumentException("Invalid UEFA club");
            this.id=id;this.association=association;this.pot=pot;this.coefficient=coefficient;
        }
    }
    public static final class Fixture {
        public final int round,home,away;
        public Fixture(int round,int home,int away){this.round=round;this.home=home;this.away=away;}
    }
    public static final class Qualification {
        public final int[] roundOf16,seededPlayoff,unseededPlayoff,eliminated;
        private Qualification(int[] order){roundOf16=Arrays.copyOfRange(order,0,8);seededPlayoff=Arrays.copyOfRange(order,8,16);unseededPlayoff=Arrays.copyOfRange(order,16,24);eliminated=Arrays.copyOfRange(order,24,36);}
    }
    public final Competition competition;
    public final int season;
    private final List<Club> clubs;
    private final List<Fixture> fixtures;
    private final List<LocalDate> dates;
    private final Map<Integer,Integer> index=new HashMap<>();
    private final int[][] results;
    public EuropeanLeaguePhase(Competition competition,int season,List<Club> clubs,List<Fixture> fixtures,List<LocalDate> dates) {
        if(competition==null||season<2026||season>2200||clubs==null||clubs.size()!=36||fixtures==null||dates==null)throw new IllegalArgumentException("Invalid UEFA field");
        this.competition=competition;this.season=season;this.clubs=Collections.unmodifiableList(new ArrayList<>(clubs));this.fixtures=Collections.unmodifiableList(new ArrayList<>(fixtures));this.dates=Collections.unmodifiableList(new ArrayList<>(dates));
        int games=competition==Competition.CONFERENCE?6:8,pots=competition==Competition.CONFERENCE?6:4;
        if(dates.size()!=games||fixtures.size()!=18*games)throw new IllegalArgumentException("Incomplete league phase calendar");
        for(int i=0;i<dates.size();i++)if(dates.get(i)==null||dates.get(i).isBefore(LocalDate.of(season,7,1))||dates.get(i).isAfter(LocalDate.of(season+1,6,30))||i>0&&!dates.get(i).isAfter(dates.get(i-1)))throw new IllegalArgumentException("Invalid UEFA dates");
        int[] potCount=new int[pots];
        for(int i=0;i<36;i++){Club c=clubs.get(i);if(c==null||c.pot>=pots||index.put(c.id,i)!=null)throw new IllegalArgumentException("Invalid UEFA club identity");potCount[c.pot]++;}
        for(int count:potCount)if(count!=36/pots)throw new IllegalArgumentException("Unequal UEFA pots");
        boolean[][] playedRound=new boolean[36][games],opponents=new boolean[36][36];int[][] homePots=new int[36][pots],awayPots=new int[36][pots];
        ArrayList<Map<String,Integer>> associations=new ArrayList<>();for(int i=0;i<36;i++)associations.add(new HashMap<>());
        for(Fixture f:fixtures) {
            if(f==null||f.round<0||f.round>=games||!index.containsKey(f.home)||!index.containsKey(f.away)||f.home==f.away)throw new IllegalArgumentException("Invalid UEFA fixture");
            int h=index.get(f.home),a=index.get(f.away);Club hc=clubs.get(h),ac=clubs.get(a);
            if(playedRound[h][f.round]||playedRound[a][f.round]||opponents[h][a]||hc.association.equals(ac.association))throw new IllegalArgumentException("UEFA draw conflict");
            playedRound[h][f.round]=playedRound[a][f.round]=true;opponents[h][a]=opponents[a][h]=true;
            homePots[h][ac.pot]++;awayPots[a][hc.pot]++;
            for(int[] pair:new int[][]{{h,a},{a,h}}){Map<String,Integer> counts=associations.get(pair[0]);String country=clubs.get(pair[1]).association;int count=counts.getOrDefault(country,0)+1;if(count>2)throw new IllegalArgumentException("Too many opponents from one association");counts.put(country,count);}
        }
        for(int i=0;i<36;i++)for(int p=0;p<pots;p++) {
            if(competition==Competition.CONFERENCE) {
                if(homePots[i][p]+awayPots[i][p]!=1)throw new IllegalArgumentException("Conference pot opponent missing");
                if(p%2==0&&(homePots[i][p]+homePots[i][p+1]!=1||awayPots[i][p]+awayPots[i][p+1]!=1))throw new IllegalArgumentException("Conference paired-pot venues unbalanced");
            }else if(homePots[i][p]!=1||awayPots[i][p]!=1)throw new IllegalArgumentException("UEFA pot venues unbalanced");
        }
        results=new int[fixtures.size()][];
    }
    public int fixtureCount(){return fixtures.size();}
    public List<Club> clubs(){return clubs;}
    public Fixture fixture(int i){return fixtures.get(i);}
    public int[] result(int i){return results[i]==null?null:results[i].clone();}
    public LocalDate date(int round){return dates.get(round);}
    public int currentRound(){for(int round=0;round<dates.size();round++)for(int i=0;i<results.length;i++)if(fixtures.get(i).round==round&&results[i]==null)return round;return -1;}
    public boolean complete(){return currentRound()<0;}
    public boolean record(int fixture,int homeGoals,int awayGoals,int homeDiscipline,int awayDiscipline) {
        if(fixture<0||fixture>=results.length||homeGoals<0||awayGoals<0||homeGoals>99||awayGoals>99||homeDiscipline<0||awayDiscipline<0||homeDiscipline>100||awayDiscipline>100)throw new IllegalArgumentException("Invalid UEFA result");
        int[] value={homeGoals,awayGoals,homeDiscipline,awayDiscipline};
        if(results[fixture]!=null){if(!Arrays.equals(results[fixture],value))throw new IllegalStateException("Conflicting UEFA result");return false;}
        if(fixtures.get(fixture).round!=currentRound())throw new IllegalStateException("Earlier UEFA round incomplete");results[fixture]=value;return true;
    }
    /** Points, GD, GF, away goals, wins, away wins, opponents' points/GD/GF, negative discipline, coefficient. */
    public int[][] metrics() {
        int[][] table=new int[36][11];
        for(int i=0;i<36;i++)table[i][10]=clubs.get(i).coefficient;
        for(int i=0;i<results.length;i++)if(results[i]!=null) {
            Fixture f=fixtures.get(i);int[] r=results[i],h=table[index.get(f.home)],a=table[index.get(f.away)];
            h[1]+=r[0]-r[1];a[1]+=r[1]-r[0];h[2]+=r[0];a[2]+=r[1];a[3]+=r[1];h[9]-=r[2];a[9]-=r[3];
            if(r[0]>r[1]){h[0]+=3;h[4]++;}else if(r[1]>r[0]){a[0]+=3;a[4]++;a[5]++;}else{h[0]++;a[0]++;}
        }
        for(Fixture f:fixtures){int[] h=table[index.get(f.home)],a=table[index.get(f.away)];for(int c=0;c<3;c++){h[6+c]+=a[c];a[6+c]+=h[c];}}
        return table;
    }
    public static int compareMetrics(int[] a,int[] b) {
        if(a.length!=11||b.length!=11)throw new IllegalArgumentException("Invalid UEFA ranking row");
        for(int i=0;i<11;i++){int c=Integer.compare(b[i],a[i]);if(c!=0)return c;}return 0;
    }
    public int[] order() {
        int[][] rows=metrics();Integer[] order=new Integer[36];for(int i=0;i<36;i++)order[i]=i;
        Arrays.sort(order,(a,b)->{int c=compareMetrics(rows[a],rows[b]);return c!=0?c:Integer.compare(clubs.get(a).id,clubs.get(b).id);});
        int[] out=new int[36];for(int i=0;i<36;i++)out[i]=clubs.get(order[i]).id;return out;
    }
    public Qualification qualification() {
        if(!complete())throw new IllegalStateException("UEFA league phase incomplete");int[] order=order();int[][] rows=metrics();
        // Display order is stable, but an unresolved sporting tie must never allocate a berth or seed.
        for(int i=0;i<24;i++)if(compareMetrics(rows[index.get(order[i])],rows[index.get(order[i+1])])==0)throw new IllegalStateException("UEFA ranking requires a final decision");
        return new Qualification(order);
    }
    public void validateDate(LocalDate date) {
        if(date==null)throw new IllegalArgumentException("Missing career date");
        for(int i=0;i<results.length;i++)if(results[i]!=null&&dates.get(fixtures.get(i).round).isAfter(date))throw new IllegalArgumentException("Future UEFA result");
        int round=currentRound();if(round>=0&&dates.get(round).isBefore(date))throw new IllegalArgumentException("Overdue UEFA round");
    }
    public String snapshot() {
        StringBuilder out=new StringBuilder("ULP1|").append(competition).append('|').append(season);
        for(Club c:clubs)out.append("\nC|").append(c.id).append('|').append(c.association).append('|').append(c.pot).append('|').append(c.coefficient);
        for(LocalDate date:dates)out.append("\nD|").append(date);
        for(Fixture f:fixtures)out.append("\nF|").append(f.round).append('|').append(f.home).append('|').append(f.away);
        // Round order is canonical even when the supplied draw's fixture list was unsorted.
        for(int round=0;round<dates.size();round++)for(int i=0;i<results.length;i++)if(results[i]!=null&&fixtures.get(i).round==round){out.append("\nS|").append(i);for(int v:results[i])out.append('|').append(v);}
        return out.toString();
    }
    public static EuropeanLeaguePhase restore(String text) {
        if(text==null||text.length()>65536)throw new IllegalArgumentException("Invalid UEFA save size");
        String[] lines=text.split("\n",-1),header=lines[0].split("\\|",-1);
        if(header.length!=3||!header[0].equals("ULP1"))throw new IllegalArgumentException("Invalid UEFA save");
        Competition competition=Competition.valueOf(header[1]);int rounds=competition==Competition.CONFERENCE?6:8,fixtures=18*rounds,base=37+rounds+fixtures;
        if(lines.length<base||lines.length>base+fixtures)throw new IllegalArgumentException("Invalid UEFA save count");
        ArrayList<Club> clubs=new ArrayList<>();ArrayList<LocalDate> dates=new ArrayList<>();ArrayList<Fixture> draw=new ArrayList<>();int at=1;
        for(int i=0;i<36;i++){String[] c=row(lines[at++],"C",5);clubs.add(new Club(Integer.parseInt(c[1]),c[2],Integer.parseInt(c[3]),Integer.parseInt(c[4])));}
        for(int i=0;i<rounds;i++)dates.add(LocalDate.parse(row(lines[at++],"D",2)[1]));
        for(int i=0;i<fixtures;i++){String[] f=row(lines[at++],"F",4);draw.add(new Fixture(Integer.parseInt(f[1]),Integer.parseInt(f[2]),Integer.parseInt(f[3])));}
        EuropeanLeaguePhase phase=new EuropeanLeaguePhase(competition,Integer.parseInt(header[2]),clubs,draw,dates);
        while(at<lines.length){String[] r=row(lines[at++],"S",6);if(!phase.record(Integer.parseInt(r[1]),Integer.parseInt(r[2]),Integer.parseInt(r[3]),Integer.parseInt(r[4]),Integer.parseInt(r[5])))throw new IllegalArgumentException("Duplicate UEFA result");}
        if(!text.equals(phase.snapshot()))throw new IllegalArgumentException("Non-canonical UEFA save");return phase;
    }
    private static String[] row(String line,String marker,int count){String[] parts=line.split("\\|",-1);if(parts.length!=count||!parts[0].equals(marker))throw new IllegalArgumentException("Invalid UEFA row");return parts;}
}

package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** Eight five-club groups, bonus-point shootouts, European byes and seeded knockout entry. */
public final class ScottishLeagueCupSeason {
    public final int season;
    private final long seed;
    private final int[] clubs,exempt;
    private final int[][] groups=new int[8][5];
    private final ArrayList<KnockoutTie> games=new ArrayList<>();
    private SeasonCup knockout;
    public ScottishLeagueCupSeason(int season,long seed,int[] clubs,int[] exempt) {
        if(season<2026||season>2200||clubs==null||clubs.length!=40||exempt==null||exempt.length<2||exempt.length>6)throw new IllegalArgumentException("Invalid Scottish League Cup field");
        this.season=season;this.seed=seed;this.clubs=clubs.clone();this.exempt=exempt.clone();Set<Integer> unique=new HashSet<>();
        for(int c:clubs)if(c<0||!unique.add(c))throw new IllegalArgumentException("Duplicate group club");
        for(int c:exempt)if(c<0||!unique.add(c))throw new IllegalArgumentException("Duplicate European qualifier");
        Random random=new Random(seed);
        for(int pot=0;pot<5;pot++){ArrayList<Integer> entries=new ArrayList<>();for(int i=0;i<8;i++)entries.add(clubs[8*pot+i]);Collections.shuffle(entries,random);for(int g=0;g<8;g++)groups[g][pot]=entries.get(g);}
        LeagueSchedule[] schedule=new LeagueSchedule[8];for(int g=0;g<8;g++)schedule[g]=new LeagueSchedule(groups[g],2);
        for(int round=0;round<5;round++)for(int g=0;g<8;g++)for(LeagueSchedule.Pairing match:schedule[g].round(round)) {
            int h=0,a=0;for(int i=0;i<5;i++){if(groups[g][i]==match.home)h=i;if(groups[g][i]==match.away)a=i;}
            // Orient the five-club cycle independently from the round-robin byes.
            // Each club hosts the next two seeds and visits the previous two.
            boolean home=(a-h+5)%5<=2;int first=home?match.home:match.away,second=home?match.away:match.home;
            games.add(new KnockoutTie(first,second,1,first,false,KnockoutTie.Rule.PENALTIES));
        }
        if(games.size()!=80)throw new IllegalStateException("Unbalanced Scottish groups");
    }
    public int[] clubs(){return clubs.clone();}
    public int[] exemptions(){return exempt.clone();}
    public int[] group(int i){return groups[i].clone();}
    public int groupFixtureCount(){return games.size();}
    public KnockoutTie groupFixture(int i){return games.get(i);}
    public LocalDate groupDate(int round){int[] days={11,15,18,22,25};return LocalDate.of(season,7,days[round]);}
    private int pendingGroup(){for(int i=0;i<games.size();i++)if(games.get(i).winner()<0)return i;return -1;}
    public boolean inGroups(){return pendingGroup()>=0;}
    public String roundName(){int i=pendingGroup();return i>=0?"Group "+(char)('A'+(i%16)/2)+" • Matchday "+(i/16+1):knockout().complete()?"Final":knockout().round(knockout().stage(knockout().currentIndex())).name;}
    public KnockoutTie current(){int i=pendingGroup();return i>=0?games.get(i):knockout().current();}
    public LocalDate nextDate(){int i=pendingGroup();return i>=0?groupDate(i/16):knockout().nextDate();}
    public boolean complete(){return !inGroups()&&knockout().complete();}
    public int winner(){return complete()?knockout().winner():-1;}
    public List<LocalDate> calendar(){ArrayList<LocalDate> dates=new ArrayList<>();for(int i=0;i<5;i++)dates.add(groupDate(i));for(SeasonCup.Round r:NationalCupFormats.rounds("SCO_LEAGUE_CUP",season,false))dates.add(r.date(0));return Collections.unmodifiableList(dates);}
    public Map<Integer,int[]> metrics() {
        Map<Integer,int[]> table=new HashMap<>();for(int club:clubs)table.put(club,new int[8]);
        for(KnockoutTie game:games)if(game.winner()>=0) {
            int[] score=game.regulationScore(0),h=table.get(game.firstHome),a=table.get(game.firstAway);h[6]++;a[6]++;h[7]+=score[0];a[7]+=score[1];
            h[0]+=score[0]>score[1]?3:score[0]<score[1]?0:game.winner()==game.firstHome?2:1;
            a[0]+=score[1]>score[0]?3:score[1]<score[0]?0:game.winner()==game.firstAway?2:1;
            h[1]+=score[0]-score[1];a[1]+=score[1]-score[0];h[2]+=score[0];a[2]+=score[1];a[3]+=score[1];
            if(score[0]>score[1])h[4]++;if(score[1]>score[0]){a[4]++;a[5]++;}
        }
        return table;
    }
    private static int compare(int[] a,int[] b){for(int i=0;i<6;i++){int c=Integer.compare(b[i],a[i]);if(c!=0)return c;}return 0;}
    private long lot(int club){return new Random(seed^0x5c0715L^(7919L*club)).nextLong();}
    private int compareLots(int a,int b){int c=Long.compareUnsigned(lot(a),lot(b));return c!=0?c:Integer.compare(a,b);}
    private int[] rank(int[] field,boolean sameGroup) {
        Map<Integer,int[]> table=metrics(),head=new HashMap<>();
        if(sameGroup) {
            for(int c:field)head.put(c,new int[6]);
            for(KnockoutTie game:games)if(game.winner()>=0&&head.containsKey(game.firstHome)&&head.containsKey(game.firstAway)&&compare(table.get(game.firstHome),table.get(game.firstAway))==0) {
                int[] result=game.regulationScore(0),h=head.get(game.firstHome),a=head.get(game.firstAway);h[0]+=result[0]>result[1]?3:result[0]==result[1]?(game.winner()==game.firstHome?2:1):0;
                a[0]+=result[1]>result[0]?3:result[0]==result[1]?(game.winner()==game.firstAway?2:1):0;h[1]+=result[0]-result[1];a[1]+=result[1]-result[0];h[2]+=result[0];a[2]+=result[1];
            }
        }
        Integer[] order=new Integer[field.length];for(int i=0;i<field.length;i++)order[i]=field[i];
        Arrays.sort(order,(a,b)->{int c=compare(table.get(a),table.get(b));if(c==0&&sameGroup)c=compare(head.get(a),head.get(b));return c!=0?c:compareLots(a,b);});
        int[] result=new int[field.length];for(int i=0;i<result.length;i++)result[i]=order[i];return result;
    }
    public int[] groupOrder(int group){return rank(groups[group],true);}
    public SeasonCup knockout() {
        if(inGroups())return null;
        if(knockout==null) {
            int[] winners=new int[8],runners=new int[8];for(int g=0;g<8;g++){int[] order=groupOrder(g);winners[g]=order[0];runners[g]=order[1];}
            winners=rank(winners,false);runners=rank(runners,false);Set<Integer> seeded=new HashSet<>();for(int club:exempt)seeded.add(club);for(int i=0;i<8-exempt.length;i++)seeded.add(winners[i]);
            ArrayList<SeasonCup.Entrant> field=new ArrayList<>();for(int club:exempt)field.add(new SeasonCup.Entrant(club,0,false,true));
            for(int club:winners)field.add(new SeasonCup.Entrant(club,0,false,seeded.contains(club)));
            for(int i=0;i<8-exempt.length;i++)field.add(new SeasonCup.Entrant(runners[i],0,false,false));
            knockout=NationalCupFormats.create("SCO_LEAGUE_CUP",season,seed+83,field,false);
        }
        return knockout;
    }
    public void validateDate(LocalDate date) {
        if(date==null)throw new IllegalArgumentException("Missing cup date");
        for(int i=0;i<games.size();i++)if(games.get(i).playedLegs()>0&&groupDate(i/16).isAfter(date))throw new IllegalArgumentException("Future Scottish group result");
        if(!complete()&&nextDate().isBefore(date))throw new IllegalArgumentException("Overdue Scottish League Cup");
        if(knockout!=null)knockout.validateDate(date);
    }
    private static String ids(int[] values){StringJoiner out=new StringJoiner(",");for(int club:values)out.add(String.valueOf(club));return out.toString();}
    private static int[] ids(String text){String[] rows=text.split(",",-1);int[] result=new int[rows.length];for(int i=0;i<result.length;i++)result[i]=Integer.parseInt(rows[i]);return result;}
    public String snapshot() {
        StringBuilder out=new StringBuilder("SLC1|").append(season).append('|').append(seed).append('\n').append(ids(clubs)).append('\n').append(ids(exempt));
        for(int i=0;i<games.size();i++)if(games.get(i).playedLegs()>0)out.append("\nG|").append(i).append('|').append(games.get(i).snapshot());
        if(knockout!=null)out.append("\nK|").append(Base64.getEncoder().encodeToString(knockout.snapshot().getBytes(java.nio.charset.StandardCharsets.UTF_8)));return out.toString();
    }
    public static ScottishLeagueCupSeason restore(String text) {
        if(text==null||text.length()>65536)throw new IllegalArgumentException("Invalid Scottish League Cup save size");
        String[] rows=text.split("\n",-1),header=rows[0].split("\\|",-1);if(rows.length<3||rows.length>84||header.length!=3||!header[0].equals("SLC1"))throw new IllegalArgumentException("Invalid Scottish League Cup save");
        ScottishLeagueCupSeason cup=new ScottishLeagueCupSeason(Integer.parseInt(header[1]),Long.parseLong(header[2]),ids(rows[1]),ids(rows[2]));int previous=-1;
        for(int i=3;i<rows.length;i++) {
            if(rows[i].startsWith("G|")) {
                String[] event=rows[i].split("\\|",3);int index=Integer.parseInt(event[1]);KnockoutTie tie=KnockoutTie.restore(event[2]);
                if(index!=previous+1||index>=80||index>0&&cup.games.get(index-1).winner()<0||tie.playedLegs()==0||tie.firstHome!=cup.games.get(index).firstHome||tie.firstAway!=cup.games.get(index).firstAway||tie.legs!=1||tie.neutral||tie.rule!=KnockoutTie.Rule.PENALTIES||tie.higherSeed!=tie.firstHome)throw new IllegalArgumentException("Scottish group result violates draw");
                cup.games.set(index,tie);previous=index;
            } else if(rows[i].startsWith("K|")&&i==rows.length-1&&!cup.inGroups()) {
                SeasonCup saved=SeasonCup.restore(new String(Base64.getDecoder().decode(rows[i].substring(2)),java.nio.charset.StandardCharsets.UTF_8));
                SeasonCup expected=cup.knockout();SeasonCup blank=NationalCupFormats.create("SCO_LEAGUE_CUP",cup.season,cup.seed+83,saved.entrants(),false);
                if(!saved.snapshot().equals(blank.snapshot())&&!saved.snapshot().startsWith(blank.snapshot()+"\n"))throw new IllegalArgumentException("Scottish knockout metadata changed");
                if(!blank.snapshot().equals(NationalCupFormats.create("SCO_LEAGUE_CUP",cup.season,cup.seed+83,expected.entrants(),false).snapshot()))throw new IllegalArgumentException("Scottish knockout qualification changed");
                NationalCupFormats.validateStructure(saved);cup.knockout=saved;
            } else throw new IllegalArgumentException("Invalid Scottish League Cup event");
        }
        if(!text.equals(cup.snapshot()))throw new IllegalArgumentException("Non-canonical Scottish League Cup save");return cup;
    }
}

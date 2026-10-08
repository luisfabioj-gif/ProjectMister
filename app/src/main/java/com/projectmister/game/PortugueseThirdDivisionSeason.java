package com.projectmister.game;

import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Twenty Liga 3 clubs, two regional groups and result-led promotion/survival phases. */
public final class PortugueseThirdDivisionSeason {
    public interface Scores {
        int goals(int home,int away,boolean homeAdvantage);
        default PortugueseLeagueCupSeason.PlayerUse[] players(int club){return new PortugueseLeagueCupSeason.PlayerUse[0];}
        default int redCards(int club){return 0;}
        default int yellowCards(int club){return 0;}
    }
    public final int season,worldSize;
    private final long seed;
    private final int[][] groups;
    private final LeagueResults first,second;
    private final ArrayList<String> decisions=new ArrayList<>();
    private Map<String,String> restoringDecisions;
    private int[][] later;
    private int[][] regionalOrder;
    private final Participation firstUse,secondUse;
    private int firstRounds,secondRounds;
    public PortugueseThirdDivisionSeason(int year,long seed,int worldSize,int[] north,int[] south) {
        if(year<2026||year>2200||worldSize<20||worldSize>768||north==null||south==null||north.length!=10||south.length!=10)throw new IllegalArgumentException("Invalid Liga 3 field");
        this.season=year;this.seed=seed;this.worldSize=worldSize;groups=new int[][]{north.clone(),south.clone()};Set<Integer> clubs=new HashSet<>();
        for(int[] group:groups)for(int c:group)if(c<0||c>=worldSize||!clubs.add(c))throw new IllegalArgumentException("Invalid Liga 3 club");
        first=new LeagueResults(worldSize,true);second=new LeagueResults(worldSize,true);firstUse=new Participation(worldSize);secondUse=new Participation(worldSize);
    }
    public int[] clubs(){int[] clubs=new int[20];System.arraycopy(groups[0],0,clubs,0,10);System.arraycopy(groups[1],0,clubs,10,10);return clubs;}
    public int[] group(int group){return groups[group].clone();}
    public int firstRounds(){return firstRounds;}
    public int secondRounds(){return secondRounds;}
    public boolean complete(){return secondRounds==14;}
    private List<LocalDate> calendar(LocalDate start,int rounds) {
        List<LocalDate> cups=new ArrayList<>();for(String day:new String[]{"08-30","09-20","10-18","11-22","12-16","02-03","05-22","05-23","05-30"})cups.add(LocalDate.parse((day.compareTo("07-01")>=0?season:season+1)+"-"+day));
        return CompetitionCalendar.cupSeason(start,rounds,cups);
    }
    public LocalDate firstDate(int round){return calendar(LocalDate.of(season,8,9),18).get(round);}
    public LocalDate secondDate(int round){return calendar(LocalDate.of(season+1,2,7),14).get(round);}
    private int[] points(LeagueResults ledger,int[] initial){int[] pts=initial.clone();for(int r=0;r<40;r++)for(LeagueResults.Result game:ledger.round(r)){if(game.homeGoals>game.awayGoals)pts[game.home]+=3;else if(game.homeGoals<game.awayGoals)pts[game.away]+=3;else{pts[game.home]++;pts[game.away]++;}}return pts;}
    public static int survivalBonus(int position,int points){if(position<5||position>10||points<0)throw new IllegalArgumentException("Invalid survival bonus");return points<10?0:11-position+(points<15?0:Math.min(4,(points-10)/5));}
    private int[] bonus(int group) {
        int[] bonus=new int[worldSize];if(group>0&&later!=null){int g=group-1;int[] pts=points(first,new int[worldSize]);int[] rank=regionalOrder[g];for(int i=4;i<10;i++)bonus[rank[i]]=survivalBonus(i+1,pts[rank[i]]);}return bonus;
    }
    public int survivalBonusFor(int group,int club){return bonus(group)[club];}
    private int[] rank(int[] clubs,LeagueResults ledger,int[] initial,boolean finished,String key,Scores scores) {
        Participation use=key.startsWith("F")?firstUse:firstUse.plus(secondUse);
        PortugueseThirdDivisionStandings table=new PortugueseThirdDivisionStandings(clubs,ledger,key.startsWith("F")?null:first,initial,use.reds,use.yellows,use.ageDays(),use.ageUses());
        ArrayList<Integer> result=new ArrayList<>();
        for(int start=0;start<table.order.length;) {
            int end=start+1;while(end<table.order.length&&table.tiedAt(end-1))end++;
            ArrayList<Integer> tied=new ArrayList<>();for(int i=start;i<end;i++)tied.add(table.order[i]);
            if(finished&&tied.size()>1) {
                tied=resolveTied(key,tied,scores,0);
            }
            result.addAll(tied);start=end;
        }
        int[] out=new int[result.size()];for(int i=0;i<out.length;i++)out[i]=result.get(i);return out;
    }
    private ArrayList<Integer> resolveTied(String key,ArrayList<Integer> tied,Scores scores,int repeat) {
        if(tied.size()==2){int a=tied.get(0),b=tied.get(1),winner=decision(key,a,b,scores);return new ArrayList<>(Arrays.asList(winner,winner==a?b:a));}
        if(repeat>12)throw new IllegalStateException("Liga 3 neutral tournament remains tied");
        int[] clubs=new int[tied.size()];for(int i=0;i<clubs.length;i++)clubs[i]=tied.get(i);
        String tournament=key+"T"+repeat;LeagueResults mini=new LeagueResults(worldSize,true);LeagueSchedule schedule=new LeagueSchedule(clubs,1,true);
        for(int r=0;r<schedule.roundCount();r++)for(LeagueSchedule.Pairing f:schedule.round(r)) {
            int[] goals=neutralResult(tournament,f.home,f.away,scores);mini.record(r,f.home,f.away,goals[0],goals[1]);
        }
        Participation use=key.startsWith("F")?firstUse:firstUse.plus(secondUse);
        PortugueseThirdDivisionStandings table=new PortugueseThirdDivisionStandings(clubs,mini,null,new int[worldSize],use.reds,use.yellows,use.ageDays(),use.ageUses());
        ArrayList<Integer> out=new ArrayList<>();for(int start=0;start<table.order.length;) {
            int end=start+1;while(end<table.order.length&&table.tiedAt(end-1))end++;
            ArrayList<Integer> remaining=new ArrayList<>();for(int i=start;i<end;i++)remaining.add(table.order[i]);
            if(remaining.size()>1)remaining=resolveTied(key+"D"+repeat+"_"+start,remaining,scores,repeat+1);
            out.addAll(remaining);start=end;
        }return out;
    }
    private int[] neutralResult(String key,int a,int b,Scores scores) {
        String prefix=key+"|"+a+"|"+b+"|";for(String saved:decisions)if(saved.startsWith(prefix))return ids(saved.substring(prefix.length()));
        String result=restoringDecisions==null?null:restoringDecisions.remove(prefix);
        if(result==null){if(restoringDecisions!=null)throw new IllegalArgumentException("Missing Liga 3 neutral result");if(scores==null)throw new IllegalStateException("Missing Liga 3 neutral result");int h=scores.goals(a,b,false),v=scores.goals(b,a,false);result=h+","+v;}
        int[] goals=ids(result);if(goals.length!=2||goals[0]<0||goals[1]<0||goals[0]>99||goals[1]>99)throw new IllegalArgumentException("Invalid Liga 3 neutral score");decisions.add(prefix+result);return goals;
    }
    private int decision(String key,int a,int b,Scores scores) {
        String prefix=key+"|"+a+"|"+b+"|";for(String saved:decisions)if(saved.startsWith(prefix))return KnockoutTie.restore(saved.substring(prefix.length())).winner();
        if(restoringDecisions!=null) {
            String saved=restoringDecisions.remove(prefix);if(saved==null)throw new IllegalArgumentException("Missing Liga 3 deciding game");
            KnockoutTie tie=KnockoutTie.restore(saved);validateDecision(tie,a,b);decisions.add(prefix+saved);return tie.winner();
        }
        if(scores==null)throw new IllegalStateException("Liga 3 deciding result not supplied");
        KnockoutTie tie=new KnockoutTie(a,b,1,a,true,KnockoutTie.Rule.EXTRA_TIME_PENALTIES);tie.recordRegulation(scores.goals(a,b,false),scores.goals(b,a,false));
        if(tie.phase()==KnockoutTie.Phase.EXTRA_TIME)tie.recordExtraTime(scores.goals(a,b,false)/3,scores.goals(b,a,false)/3);
        if(tie.phase()==KnockoutTie.Phase.PENALTIES){boolean home=new Random(seed^key.hashCode()^((long)a<<32)^b).nextBoolean();tie.recordPenalties(home?5:4,home?4:5);}
        decisions.add(prefix+tie.snapshot());return tie.winner();
    }
    private void createSecond(Scores scores) {
        if(later!=null)return;if(firstRounds<18)throw new IllegalStateException("Finish regional Liga 3 groups");
        int[] north=rank(groups[0],first,new int[worldSize],true,"F0",scores),south=rank(groups[1],first,new int[worldSize],true,"F1",scores),champions=new int[8];
        regionalOrder=new int[][]{north,south};System.arraycopy(north,0,champions,0,4);System.arraycopy(south,0,champions,4,4);later=new int[][]{champions,Arrays.copyOfRange(north,4,10),Arrays.copyOfRange(south,4,10)};
    }
    public int[] laterGroup(int group){return later==null?new int[0]:later[group].clone();}
    public int[] order(int phase,int group){return phase==0?regionalOrder!=null?regionalOrder[group].clone():rank(groups[group],first,new int[worldSize],false,"F"+group,null):later==null?new int[0]:rank(later[group],second,bonus(group),complete(),"S"+group,null);}
    public void advanceTo(LocalDate date,Scores scores) {
        while(firstRounds<18&&!firstDate(firstRounds).isAfter(date)) {
            for(int[] group:groups)for(LeagueSchedule.Pairing f:new LeagueSchedule(group,2,true).round(firstRounds)){first.record(firstRounds,f.home,f.away,scores.goals(f.home,f.away,true),scores.goals(f.away,f.home,false));firstUse.record(f.home,scores);firstUse.record(f.away,scores);}firstRounds++;
        }
        if(firstRounds==18)createSecond(scores);
        while(later!=null&&secondRounds<14&&!secondDate(secondRounds).isAfter(date)) {
            for(int g=0;g<3;g++)if(g==0||secondRounds<10)for(LeagueSchedule.Pairing f:new LeagueSchedule(later[g],2,true).round(secondRounds)){second.record(secondRounds,f.home,f.away,scores.goals(f.home,f.away,true),scores.goals(f.away,f.home,false));secondUse.record(f.home,scores);secondUse.record(f.away,scores);}secondRounds++;
        }
        if(complete())for(int g=0;g<3;g++)rank(later[g],second,bonus(g),true,"S"+g,scores);
    }
    public int[] promotionOrder(){if(!complete())throw new IllegalStateException("Finish Liga 3 season");return rank(later[0],second,new int[worldSize],true,"S0",null);}
    public int[] sportingOrder(){if(!complete())throw new IllegalStateException("Finish Liga 3");int[] out=new int[20];System.arraycopy(promotionOrder(),0,out,0,8);for(int rank=0;rank<6;rank++)for(int g=1;g<=2;g++)out[8+rank*2+g-1]=order(1,g)[rank];return out;}
    public int[] relegated(){if(!complete())throw new IllegalStateException("Finish Liga 3 survival phase");int[] north=order(1,1),south=order(1,2);return new int[]{north[4],north[5],south[4],south[5]};}
    public LeagueResults results(int phase){return phase==0?first:second;}
    private static String ids(int[] field){StringJoiner out=new StringJoiner(",");for(int c:field)out.add(String.valueOf(c));return out.toString();}
    private static int[] ids(String row){String[] values=row.split(",",-1);int[] field=new int[values.length];for(int i=0;i<field.length;i++)field[i]=Integer.parseInt(values[i]);return field;}
    private static String encode(String text){return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));}
    private static String decode(String text){return new String(Base64.getDecoder().decode(text),StandardCharsets.UTF_8);}
    public String snapshot(){StringBuilder out=new StringBuilder("PL3_1|").append(season).append('|').append(seed).append('|').append(worldSize).append('\n').append(ids(groups[0])).append('\n').append(ids(groups[1])).append('\n').append(encode(first.snapshot())).append('\n').append(encode(second.snapshot())).append('\n').append(encode(firstUse.snapshot())).append('\n').append(encode(secondUse.snapshot()));for(String decision:decisions)out.append('\n').append(encode(decision));return out.toString();}
    public static PortugueseThirdDivisionSeason restore(String text) {
        if(text==null||text.length()>131072)throw new IllegalArgumentException("Invalid Liga 3 save size");String[] rows=text.split("\n",-1),head=rows[0].split("\\|",-1);
        if(rows.length<7||rows.length>2048||head.length!=4||!head[0].equals("PL3_1"))throw new IllegalArgumentException("Invalid Liga 3 save");
        PortugueseThirdDivisionSeason cup=new PortugueseThirdDivisionSeason(Integer.parseInt(head[1]),Long.parseLong(head[2]),Integer.parseInt(head[3]),ids(rows[1]),ids(rows[2]));
        LeagueResults first=LeagueResults.restore(decode(rows[3]),cup.worldSize),second=LeagueResults.restore(decode(rows[4]),cup.worldSize);
        cup.firstUse.read(decode(rows[5]),first);cup.secondUse.read(decode(rows[6]),second);
        cup.restoringDecisions=new LinkedHashMap<>();
        for(int i=7;i<rows.length;i++) {
            String saved=decode(rows[i]);String[] p=saved.split("\\|",4);
            if(p.length!=4||!p[0].matches("[FS][012](D[0-9]+_[0-9]+)*(T[0-9]+)?"))throw new IllegalArgumentException("Invalid Liga 3 decision");
            int a=Integer.parseInt(p[1]),b=Integer.parseInt(p[2]);if(a<0||b<0||a>=cup.worldSize||b>=cup.worldSize||a==b)throw new IllegalArgumentException("Invalid Liga 3 decider clubs");
            if(!p[0].matches(".*T[0-9]+")){KnockoutTie tie=KnockoutTie.restore(p[3]);validateDecision(tie,a,b);}
            if(cup.restoringDecisions.put(p[0]+"|"+a+"|"+b+"|",p[3])!=null)throw new IllegalArgumentException("Duplicate Liga 3 deciding game");
        }
        for(int round=0;round<18;round++){List<LeagueResults.Result> games=first.round(round);if(games.isEmpty())break;if(games.size()!=10)throw new IllegalArgumentException("Incomplete Liga 3 round");for(int[] group:cup.groups)for(LeagueSchedule.Pairing f:new LeagueSchedule(group,2,true).round(round)){LeagueResults.Result result=first.fixture(round,f.home,f.away);if(result==null)throw new IllegalArgumentException("Changed Liga 3 fixture");cup.first.record(round,f.home,f.away,result.homeGoals,result.awayGoals);}cup.firstRounds++;}
        if(!cup.first.snapshot().equals(first.snapshot()))throw new IllegalArgumentException("Liga 3 results violate chronology");
        if(cup.firstRounds==18)cup.createSecond(null);
        for(int round=0;round<14;round++){List<LeagueResults.Result> games=second.round(round);if(games.isEmpty())break;if(cup.later==null||games.size()!=(round<10?10:4))throw new IllegalArgumentException("Incomplete Liga 3 second round");for(int g=0;g<3;g++)if(g==0||round<10)for(LeagueSchedule.Pairing f:new LeagueSchedule(cup.later[g],2,true).round(round)){LeagueResults.Result result=second.fixture(round,f.home,f.away);if(result==null)throw new IllegalArgumentException("Changed second-phase fixture");cup.second.record(round,f.home,f.away,result.homeGoals,result.awayGoals);}cup.secondRounds++;}
        if(!cup.second.snapshot().equals(second.snapshot()))throw new IllegalArgumentException("Second-phase results violate chronology");
        if(cup.complete())for(int g=0;g<3;g++)cup.rank(cup.later[g],cup.second,cup.bonus(g),true,"S"+g,null);
        if(!cup.restoringDecisions.isEmpty()||!text.equals(cup.snapshot()))throw new IllegalArgumentException("Invalid Liga 3 deciding games");cup.restoringDecisions=null;return cup;
    }
    private static void validateDecision(KnockoutTie tie,int a,int b) {
        if(tie.firstHome!=a||tie.firstAway!=b||tie.higherSeed!=a||tie.legs!=1||!tie.neutral||tie.rule!=KnockoutTie.Rule.EXTRA_TIME_PENALTIES||tie.winner()<0)throw new IllegalArgumentException("Invalid Liga 3 deciding tie");
    }
    static final class Participation {
        final int[] reds,yellows;final TreeMap<Integer,TreeMap<Integer,Integer>> used=new TreeMap<>();
        Participation(int size){reds=new int[size];yellows=new int[size];}
        void record(int club,Scores scores){int r=scores.redCards(club),y=scores.yellowCards(club);if(r<0||r>11||y<0||y>30)throw new IllegalArgumentException("Invalid Liga 3 cards");reds[club]+=r;yellows[club]+=y;TreeMap<Integer,Integer> players=used.computeIfAbsent(club,k->new TreeMap<>());for(PortugueseLeagueCupSeason.PlayerUse p:scores.players(club)){if(p==null||p.player<0||p.ageDays<4000||p.ageDays>30000)throw new IllegalArgumentException("Invalid Liga 3 player use");Integer old=players.put(p.player,p.ageDays);if(old!=null&&old!=p.ageDays)throw new IllegalArgumentException("Changed Liga 3 player age");}}
        Participation plus(Participation other){Participation out=new Participation(reds.length);for(int c=0;c<reds.length;c++){out.reds[c]=reds[c]+other.reds[c];out.yellows[c]=yellows[c]+other.yellows[c];if(used.containsKey(c))out.used.put(c,new TreeMap<>(used.get(c)));if(other.used.containsKey(c))out.used.computeIfAbsent(c,k->new TreeMap<>()).putAll(other.used.get(c));}return out;}
        long[] ageDays(){long[] out=new long[reds.length];for(Map.Entry<Integer,TreeMap<Integer,Integer>> e:used.entrySet())for(int age:e.getValue().values())out[e.getKey()]+=age;return out;}
        int[] ageUses(){int[] out=new int[reds.length];for(Map.Entry<Integer,TreeMap<Integer,Integer>> e:used.entrySet())out[e.getKey()]=e.getValue().size();return out;}
        String snapshot(){StringJoiner out=new StringJoiner(";");for(Map.Entry<Integer,TreeMap<Integer,Integer>> e:used.entrySet()){StringBuilder row=new StringBuilder().append(e.getKey()).append(',').append(reds[e.getKey()]).append(',').append(yellows[e.getKey()]);for(Map.Entry<Integer,Integer> p:e.getValue().entrySet())row.append(',').append(p.getKey()).append(':').append(p.getValue());out.add(row.toString());}return out.toString();}
        void read(String value,LeagueResults ledger){Set<Integer> played=new HashSet<>();for(int r=0;r<100;r++)for(LeagueResults.Result f:ledger.round(r)){played.add(f.home);played.add(f.away);}if(!value.isEmpty())for(String row:value.split(";",-1)){String[] p=row.split(",",-1);if(p.length<3||p.length>103)throw new IllegalArgumentException("Invalid Liga 3 participation");int c=Integer.parseInt(p[0]);if(!played.contains(c)||used.containsKey(c))throw new IllegalArgumentException("Unexpected Liga 3 players");reds[c]=Integer.parseInt(p[1]);yellows[c]=Integer.parseInt(p[2]);if(reds[c]<0||reds[c]>400||yellows[c]<0||yellows[c]>1000)throw new IllegalArgumentException("Invalid Liga 3 discipline");TreeMap<Integer,Integer> players=new TreeMap<>();for(int i=3;i<p.length;i++){String[] a=p[i].split(":",-1);if(a.length!=2)throw new IllegalArgumentException("Invalid player age");int id=Integer.parseInt(a[0]),age=Integer.parseInt(a[1]);if(id<0||age<4000||age>30000||players.put(id,age)!=null)throw new IllegalArgumentException("Invalid player age");}used.put(c,players);}if(!used.keySet().equals(played)||!snapshot().equals(value))throw new IllegalArgumentException("Missing Liga 3 participation");}
    }
    public void validateDate(LocalDate date){if(firstRounds>0&&firstDate(firstRounds-1).isAfter(date)||secondRounds>0&&secondDate(secondRounds-1).isAfter(date))throw new IllegalArgumentException("Future Liga 3 result");if(firstRounds<18&&firstDate(firstRounds).isBefore(date)||firstRounds==18&&secondRounds<14&&secondDate(secondRounds).isBefore(date))throw new IllegalArgumentException("Overdue Liga 3 fixture");}
}

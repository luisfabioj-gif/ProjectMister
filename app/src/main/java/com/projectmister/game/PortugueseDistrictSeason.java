package com.projectmister.game;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Projected regional qualifying leagues among the district clubs in this career world.
 * These simulated fixtures are not represented as the official twenty association championships. */
public final class PortugueseDistrictSeason {
    public final int season,worldSize;
    private final long seed;
    private final int[][] groups;
    private final LeagueResults results;
    private final PortugueseThirdDivisionSeason.Participation use;
    private final PortugueseLowerDeciders deciders;
    private int roundsPlayed;
    public PortugueseDistrictSeason(int year,long seed,int size,int[][] field){
        if(year<2026||year>2200||size>768||field==null||field.length!=4)throw new IllegalArgumentException("Invalid district field");season=year;worldSize=size;this.seed=seed;groups=new int[4][];Set<Integer> seen=new HashSet<>();for(int g=0;g<4;g++){if(field[g].length<5||field[g].length>24)throw new IllegalArgumentException("Incomplete district qualifying group");groups[g]=field[g].clone();for(int c:groups[g])if(c<0||c>=size||!seen.add(c))throw new IllegalArgumentException("Duplicate district club");}results=new LeagueResults(size,true);use=new PortugueseThirdDivisionSeason.Participation(size);deciders=new PortugueseLowerDeciders(seed,size);
    }
    public int[] clubs(){ArrayList<Integer> clubs=new ArrayList<>();for(int[] g:groups)for(int c:g)clubs.add(c);return clubs.stream().mapToInt(Integer::intValue).toArray();}
    public LeagueResults results(){return results;}
    public int roundCount(){int max=0;for(int[] g:groups)max=Math.max(max,new LeagueSchedule(g,2,true).roundCount());return max;}
    public LocalDate date(int round){return LocalDate.of(season,8,16).plusWeeks(round);}
    public boolean complete(){return roundsPlayed==roundCount();}
    public void advanceTo(LocalDate date,PortugueseThirdDivisionSeason.Scores scores){while(roundsPlayed<roundCount()&&!date(roundsPlayed).isAfter(date)){for(int[] g:groups){LeagueSchedule schedule=new LeagueSchedule(g,2,true);if(roundsPlayed<schedule.roundCount())for(LeagueSchedule.Pairing f:schedule.round(roundsPlayed)){results.record(roundsPlayed,f.home,f.away,scores.goals(f.home,f.away,true),scores.goals(f.away,f.home,false));use.record(f.home,scores);use.record(f.away,scores);}}roundsPlayed++;}if(complete())for(int g=0;g<4;g++)deciders.rank(groups[g],results,null,new int[worldSize],use,true,"D"+g,scores);}
    public int[] order(int group){return deciders.rank(groups[group],results,null,new int[worldSize],use,complete(),"D"+group,null);}
    public int[] promoted(){if(!complete())throw new IllegalStateException("Finish projected district qualifiers");int[] out=new int[20];for(int g=0;g<4;g++)System.arraycopy(order(g),0,out,g*5,5);return out;}
    public int[] sportingOrder(){if(!complete())throw new IllegalStateException("Finish district qualifiers");ArrayList<Integer> out=new ArrayList<>();for(int rank=0;rank<24;rank++)for(int g=0;g<4;g++){int[] order=order(g);if(rank<order.length)out.add(order[rank]);}return out.stream().mapToInt(Integer::intValue).toArray();}
    private static String enc(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    private static String dec(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
    private static String ids(int[] ids){StringJoiner out=new StringJoiner(",");for(int c:ids)out.add(String.valueOf(c));return out.toString();}
    public String snapshot(){StringBuilder out=new StringBuilder("PDQ1|").append(season).append('|').append(seed).append('|').append(worldSize);for(int[] g:groups)out.append('\n').append(ids(g));out.append('\n').append(enc(results.snapshot())).append('\n').append(enc(use.snapshot()));for(String e:deciders.events)out.append('\n').append(enc(e));return out.toString();}
    public static PortugueseDistrictSeason restore(String text){if(text==null||text.length()>256*1024)throw new IllegalArgumentException("Invalid district save size");String[] rows=text.split("\n",-1),h=rows[0].split("\\|",-1);if(rows.length<7||rows.length>2048||h.length!=4||!h[0].equals("PDQ1"))throw new IllegalArgumentException("Invalid district save");int[][] groups=new int[4][];for(int g=0;g<4;g++)groups[g]=Arrays.stream(rows[g+1].split(",",-1)).mapToInt(Integer::parseInt).toArray();PortugueseDistrictSeason season=new PortugueseDistrictSeason(Integer.parseInt(h[1]),Long.parseLong(h[2]),Integer.parseInt(h[3]),groups);LeagueResults ledger=LeagueResults.restore(dec(rows[5]),season.worldSize);season.use.read(dec(rows[6]),ledger);List<String> decisions=new ArrayList<>();for(int i=7;i<rows.length;i++)decisions.add(dec(rows[i]));season.deciders.restoreEvents(decisions);for(int r=0;r<season.roundCount();r++){if(ledger.round(r).isEmpty())break;int games=0;for(int[] g:groups){LeagueSchedule schedule=new LeagueSchedule(g,2,true);if(r<schedule.roundCount())for(LeagueSchedule.Pairing f:schedule.round(r)){LeagueResults.Result result=ledger.fixture(r,f.home,f.away);if(result==null)throw new IllegalArgumentException("Changed district fixture");season.results.record(r,f.home,f.away,result.homeGoals,result.awayGoals);games++;}}if(games!=ledger.round(r).size())throw new IllegalArgumentException("Unexpected district fixture");season.roundsPlayed++;}if(!season.results.snapshot().equals(ledger.snapshot()))throw new IllegalArgumentException("Non-chronological district results");if(season.complete())for(int g=0;g<4;g++)season.order(g);season.deciders.finishRestore();if(!season.snapshot().equals(text))throw new IllegalArgumentException("Non-canonical district save");return season;}
    public void validateDate(LocalDate date){if(roundsPlayed>0&&date(roundsPlayed-1).isAfter(date)||!complete()&&date(roundsPlayed).isBefore(date))throw new IllegalArgumentException("District results disagree with career date");}
}

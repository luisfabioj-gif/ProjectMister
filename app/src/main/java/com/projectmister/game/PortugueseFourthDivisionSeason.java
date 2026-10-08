package com.projectmister.game;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Result-led Campeonato de Portugal: regional leagues, survival playoffs, promotion groups and final. */
public final class PortugueseFourthDivisionSeason {
    public final int season,worldSize;
    private final long seed;
    private final int[][] groups;
    private final LeagueResults first,second;
    private final PortugueseThirdDivisionSeason.Participation firstUse,secondUse;
    private final PortugueseLowerDeciders deciders;
    private int firstRounds,secondRounds;
    private int[][] regionalOrder,later;
    private final List<KnockoutTie> survival=new ArrayList<>();
    private KnockoutTie championship;
    public PortugueseFourthDivisionSeason(int year,long seed,int size,int[][] groups) {
        if(year<2026||year>2200||size<56||size>768||groups==null||groups.length!=4||groups[0].length!=(year==2026?14:16))throw new IllegalArgumentException("Invalid Campeonato de Portugal field");season=year;worldSize=size;this.seed=seed;this.groups=new int[4][];Set<Integer> ids=new HashSet<>();for(int g=0;g<4;g++){if(groups[g].length!=groups[0].length)throw new IllegalArgumentException("Unequal regional fields");this.groups[g]=groups[g].clone();for(int c:groups[g])if(c<0||c>=size||!ids.add(c))throw new IllegalArgumentException("Invalid regional club");}
        first=new LeagueResults(size,true);second=new LeagueResults(size,true);firstUse=new PortugueseThirdDivisionSeason.Participation(size);secondUse=new PortugueseThirdDivisionSeason.Participation(size);deciders=new PortugueseLowerDeciders(seed,size);
    }
    public int firstRounds(){return firstRounds;}
    public int secondRounds(){return secondRounds;}
    public int[] clubs(){int[] out=new int[groups[0].length*4];for(int g=0;g<4;g++)System.arraycopy(groups[g],0,out,g*groups[0].length,groups[0].length);return out;}
    private int rounds(){return 2*(groups[0].length-1);}
    public LocalDate firstDate(int round){String[] windows={"08-16","08-23","09-06","09-13","10-11","10-25","11-01","11-08","11-28","12-06","12-13","12-20","01-10","01-17","01-24","01-30","02-07","02-14","02-21","02-28","03-07","03-14","03-21","04-04","04-11","04-17"};ArrayList<LocalDate> dates=new ArrayList<>();for(String d:windows)dates.add(LocalDate.parse((d.compareTo("07-01")>=0?season:season+1)+"-"+d));if(season>2026)for(String d:new String[]{"08-26","09-27","10-04","11-15"})dates.add(LocalDate.parse(season+"-"+d));Collections.sort(dates);return dates.get(round);}
    public LocalDate secondDate(int round){return LocalDate.of(season+1,4,25).plusWeeks(round);}
    public LocalDate finalDate(){return LocalDate.of(season+1,6,20);}
    public boolean complete(){return championship!=null&&championship.winner()>=0;}
    private int[] rank(int[] clubs,LeagueResults phase,boolean finished,String key,PortugueseThirdDivisionSeason.Scores scores){return deciders.rank(clubs,phase,phase==first?null:first,new int[worldSize],phase==first?firstUse:firstUse.plus(secondUse),finished,key,scores);}
    private void createSecond(PortugueseThirdDivisionSeason.Scores scores) {
        if(later!=null)return;regionalOrder=new int[4][];later=new int[2][4];for(int g=0;g<4;g++){regionalOrder[g]=rank(groups[g],first,true,"F"+g,scores);later[g/2][2*(g%2)]=regionalOrder[g][0];later[g/2][2*(g%2)+1]=regionalOrder[g][1];}
        int boundary=10;for(int g=0;g<4;g++){int h=regionalOrder[g][boundary+1],a=regionalOrder[g^1][boundary];survival.add(new KnockoutTie(h,a,2,a,false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES));}
    }
    public void advanceTo(LocalDate date,PortugueseThirdDivisionSeason.Scores scores) {
        while(firstRounds<rounds()&&!firstDate(firstRounds).isAfter(date)){for(int[] group:groups)for(LeagueSchedule.Pairing f:new LeagueSchedule(group,2,true).round(firstRounds)){first.record(firstRounds,f.home,f.away,scores.goals(f.home,f.away,true),scores.goals(f.away,f.home,false));firstUse.record(f.home,scores);firstUse.record(f.away,scores);}firstRounds++;}
        if(firstRounds==rounds())createSecond(scores);
        if(later==null)return;for(int leg=0;leg<2;leg++)if(!secondDate(leg).isAfter(date))for(int i=0;i<4;i++)if(survival.get(i).playedLegs()==leg)PortugueseLowerDeciders.simulate(survival.get(i),scores,seed+leg*1009L+i);
        while(secondRounds<6&&!secondDate(secondRounds).isAfter(date)){for(int[] group:later)for(LeagueSchedule.Pairing f:new LeagueSchedule(group,2,true).round(secondRounds)){second.record(secondRounds,f.home,f.away,scores.goals(f.home,f.away,true),scores.goals(f.away,f.home,false));secondUse.record(f.home,scores);secondUse.record(f.away,scores);}secondRounds++;}
        if(secondRounds==6&&championship==null){int h=rank(later[0],second,true,"S0",scores)[0],a=rank(later[1],second,true,"S1",scores)[0];int[][] table=second.miniTable(clubs());PortugueseThirdDivisionSeason.Participation use=firstUse.plus(secondUse);if(table[h][1]<table[a][1]||table[h][1]==table[a][1]&&use.reds[h]+use.yellows[h]>use.reds[a]+use.yellows[a]){int c=h;h=a;a=c;}championship=new KnockoutTie(h,a,1,h,true,KnockoutTie.Rule.EXTRA_TIME_PENALTIES);}
        if(championship!=null&&!complete()&&!finalDate().isAfter(date))PortugueseLowerDeciders.simulate(championship,scores,seed);
    }
    public int[] order(int phase,int group){return phase==0?regionalOrder==null?rank(groups[group],first,false,"F"+group,null):regionalOrder[group].clone():later==null?new int[0]:rank(later[group],second,secondRounds==6,"S"+group,null);}
    public int[] promoted(){if(secondRounds<6)throw new IllegalStateException("Finish lower promotion groups");int[] n=order(1,0),s=order(1,1);return new int[]{n[0],n[1],s[0],s[1]};}
    public int[] sportingOrder(){if(!complete())throw new IllegalStateException("Finish Campeonato de Portugal");ArrayList<Integer> out=new ArrayList<>();for(int rank=0;rank<4;rank++)for(int g=0;g<2;g++)out.add(order(1,g)[rank]);for(int rank=2;rank<groups[0].length;rank++)for(int g=0;g<4;g++)out.add(regionalOrder[g][rank]);return out.stream().mapToInt(Integer::intValue).toArray();}
    public int[] relegated(){if(!complete())throw new IllegalStateException("Finish lower season");ArrayList<Integer> clubs=new ArrayList<>();for(int[] group:regionalOrder)for(int i=12;i<group.length;i++)clubs.add(group[i]);for(KnockoutTie tie:survival)clubs.add(tie.loser());int[] out=new int[clubs.size()];for(int i=0;i<out.length;i++)out[i]=clubs.get(i);return out;}
    public int winner(){return complete()?championship.winner():-1;}
    public LeagueResults results(int phase){return phase==0?first:second;}
    private static String encode(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    private static String decode(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
    private static String ids(int[] values){StringJoiner out=new StringJoiner(",");for(int c:values)out.add(String.valueOf(c));return out.toString();}
    private static int[] ids(String s){String[] p=s.split(",",-1);int[] out=new int[p.length];for(int i=0;i<p.length;i++)out[i]=Integer.parseInt(p[i]);return out;}
    public String snapshot(){StringBuilder out=new StringBuilder("PCP1|").append(season).append('|').append(seed).append('|').append(worldSize);for(int[] group:groups)out.append('\n').append(ids(group));out.append('\n').append(encode(first.snapshot())).append('\n').append(encode(second.snapshot())).append('\n').append(encode(firstUse.snapshot())).append('\n').append(encode(secondUse.snapshot()));for(int i=0;i<4;i++)out.append('\n').append(survival.size()>i?encode(survival.get(i).snapshot()):"-");out.append('\n').append(championship==null?"-":encode(championship.snapshot()));for(String e:deciders.events)out.append('\n').append(encode(e));return out.toString();}
    public static PortugueseFourthDivisionSeason restore(String text) {
        if(text==null||text.length()>256*1024)throw new IllegalArgumentException("Invalid lower qualifying size");String[] rows=text.split("\n",-1),head=rows[0].split("\\|",-1);if(rows.length<14||rows.length>2048||head.length!=4||!head[0].equals("PCP1"))throw new IllegalArgumentException("Invalid lower qualifying save");int[][] groups=new int[4][];for(int g=0;g<4;g++)groups[g]=ids(rows[g+1]);PortugueseFourthDivisionSeason cup=new PortugueseFourthDivisionSeason(Integer.parseInt(head[1]),Long.parseLong(head[2]),Integer.parseInt(head[3]),groups);
        LeagueResults first=LeagueResults.restore(decode(rows[5]),cup.worldSize),second=LeagueResults.restore(decode(rows[6]),cup.worldSize);cup.firstUse.read(decode(rows[7]),first);cup.secondUse.read(decode(rows[8]),second);List<String> events=new ArrayList<>();for(int i=14;i<rows.length;i++)events.add(decode(rows[i]));cup.deciders.restoreEvents(events);
        for(int r=0;r<cup.rounds();r++){if(first.round(r).isEmpty())break;if(first.round(r).size()!=2*groups[0].length)throw new IllegalArgumentException("Incomplete lower regional round");for(int[] group:groups)for(LeagueSchedule.Pairing f:new LeagueSchedule(group,2,true).round(r)){LeagueResults.Result result=first.fixture(r,f.home,f.away);if(result==null)throw new IllegalArgumentException("Altered lower fixture");cup.first.record(r,f.home,f.away,result.homeGoals,result.awayGoals);}cup.firstRounds++;}if(!cup.first.snapshot().equals(first.snapshot()))throw new IllegalArgumentException("Non-chronological lower results");
        if(cup.firstRounds==cup.rounds())cup.createSecond(null);
        for(int i=0;i<4;i++){if(cup.later==null){if(!rows[9+i].equals("-"))throw new IllegalArgumentException("Premature survival draw");continue;}KnockoutTie saved=KnockoutTie.restore(decode(rows[9+i])),expected=cup.survival.get(i);if(saved.firstHome!=expected.firstHome||saved.firstAway!=expected.firstAway||saved.higherSeed!=expected.higherSeed||saved.legs!=2||saved.neutral||saved.rule!=expected.rule)throw new IllegalArgumentException("Changed survival fixture");cup.survival.set(i,saved);}
        for(int r=0;r<6;r++){if(second.round(r).isEmpty())break;if(cup.later==null||second.round(r).size()!=4)throw new IllegalArgumentException("Incomplete lower promotion round");for(int[] group:cup.later)for(LeagueSchedule.Pairing f:new LeagueSchedule(group,2,true).round(r)){LeagueResults.Result result=second.fixture(r,f.home,f.away);if(result==null)throw new IllegalArgumentException("Altered lower promotion fixture");cup.second.record(r,f.home,f.away,result.homeGoals,result.awayGoals);}cup.secondRounds++;}if(!cup.second.snapshot().equals(second.snapshot()))throw new IllegalArgumentException("Non-chronological lower promotion");
        for(KnockoutTie tie:cup.survival)if(tie.playedLegs()!=Math.min(2,cup.secondRounds)||tie.playedLegs()==2&&tie.winner()<0)throw new IllegalArgumentException("Survival chronology differs from promotion round");
        if(cup.secondRounds==6){
            int h=cup.rank(cup.later[0],cup.second,true,"S0",null)[0],a=cup.rank(cup.later[1],cup.second,true,"S1",null)[0];int[][] table=cup.second.miniTable(cup.clubs());PortugueseThirdDivisionSeason.Participation use=cup.firstUse.plus(cup.secondUse);if(table[h][1]<table[a][1]||table[h][1]==table[a][1]&&use.reds[h]+use.yellows[h]>use.reds[a]+use.yellows[a]){int c=h;h=a;a=c;}KnockoutTie saved=KnockoutTie.restore(decode(rows[13]));if(saved.firstHome!=h||saved.firstAway!=a||saved.legs!=1||!saved.neutral||saved.higherSeed!=h||saved.rule!=KnockoutTie.Rule.EXTRA_TIME_PENALTIES)throw new IllegalArgumentException("Changed lower final");cup.championship=saved;
        }else if(!rows[13].equals("-"))throw new IllegalArgumentException("Premature lower final");
        cup.deciders.finishRestore();if(!text.equals(cup.snapshot()))throw new IllegalArgumentException("Non-canonical lower qualifying save");return cup;
    }
    public void validateDate(LocalDate date){if(firstRounds>0&&firstDate(firstRounds-1).isAfter(date)||secondRounds>0&&secondDate(secondRounds-1).isAfter(date)||complete()&&finalDate().isAfter(date))throw new IllegalArgumentException("Future lower result");if(firstRounds<rounds()&&firstDate(firstRounds).isBefore(date)||firstRounds==rounds()&&secondRounds<6&&secondDate(secondRounds).isBefore(date)||championship!=null&&!complete()&&finalDate().isBefore(date))throw new IllegalArgumentException("Overdue lower fixture");for(KnockoutTie tie:survival)for(int l=0;l<tie.playedLegs();l++)if(secondDate(l).isAfter(date))throw new IllegalArgumentException("Future survival result");}
}

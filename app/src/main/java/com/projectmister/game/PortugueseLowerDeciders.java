package com.projectmister.game;
import java.util.*;

/** Saved neutral sporting resolutions, including a round robin for a tie of three or more clubs. */
final class PortugueseLowerDeciders {
    final long seed;
    final int worldSize;
    final ArrayList<String> events=new ArrayList<>();
    Map<String,String> restoring;
    PortugueseLowerDeciders(long seed,int size){this.seed=seed;worldSize=size;}
    int[] rank(int[] clubs,LeagueResults phase,LeagueResults previous,int[] bonus,PortugueseThirdDivisionSeason.Participation use,boolean finished,String key,PortugueseThirdDivisionSeason.Scores scores) {
        PortugueseThirdDivisionStandings table=new PortugueseThirdDivisionStandings(clubs,phase,previous,bonus,use.reds,use.yellows,use.ageDays(),use.ageUses());
        ArrayList<Integer> result=new ArrayList<>();for(int start=0;start<table.order.length;) {
            int end=start+1;while(end<table.order.length&&table.tiedAt(end-1))end++;
            ArrayList<Integer> tied=new ArrayList<>();for(int i=start;i<end;i++)tied.add(table.order[i]);
            if(finished&&tied.size()>1)tied=resolve(tied,key+"_"+start,use,scores,0);result.addAll(tied);start=end;
        }int[] out=new int[result.size()];for(int i=0;i<out.length;i++)out[i]=result.get(i);return out;
    }
    int[] resolveClubs(int[] clubs,String key,PortugueseThirdDivisionSeason.Participation use,PortugueseThirdDivisionSeason.Scores scores){ArrayList<Integer> field=new ArrayList<>();for(int c:clubs)field.add(c);return resolve(field,key,use,scores,0).stream().mapToInt(Integer::intValue).toArray();}
    private ArrayList<Integer> resolve(ArrayList<Integer> tied,String key,PortugueseThirdDivisionSeason.Participation use,PortugueseThirdDivisionSeason.Scores scores,int iteration) {
        if(iteration>16)throw new IllegalStateException("Neutral lower-division tournament remains tied");
        if(tied.size()==2) {
            int a=tied.get(0),b=tied.get(1);String prefix=key+"|"+a+"|"+b+"|",saved=result(prefix);KnockoutTie tie;
            if(saved==null){requireSimulation(scores);tie=new KnockoutTie(a,b,1,a,true,KnockoutTie.Rule.EXTRA_TIME_PENALTIES);simulate(tie,scores,seed^key.hashCode());saved=tie.snapshot();events.add(prefix+saved);}else tie=KnockoutTie.restore(saved);
            if(tie.firstHome!=a||tie.firstAway!=b||tie.legs!=1||!tie.neutral||tie.higherSeed!=a||tie.rule!=KnockoutTie.Rule.EXTRA_TIME_PENALTIES||tie.winner()<0)throw new IllegalArgumentException("Altered lower-division deciding tie");
            return new ArrayList<>(Arrays.asList(tie.winner(),tie.loser()));
        }
        int[] clubs=new int[tied.size()];for(int i=0;i<clubs.length;i++)clubs[i]=tied.get(i);LeagueResults mini=new LeagueResults(worldSize,true);LeagueSchedule fixtures=new LeagueSchedule(clubs,1,true);
        for(int r=0;r<fixtures.roundCount();r++)for(LeagueSchedule.Pairing f:fixtures.round(r)) {
            String prefix=key+"T"+iteration+"|"+f.home+"|"+f.away+"|",saved=result(prefix);
            if(saved==null){requireSimulation(scores);saved=scores.goals(f.home,f.away,false)+","+scores.goals(f.away,f.home,false);events.add(prefix+saved);}String[] goals=saved.split(",",-1);if(goals.length!=2)throw new IllegalArgumentException("Invalid neutral result");mini.record(r,f.home,f.away,Integer.parseInt(goals[0]),Integer.parseInt(goals[1]));
        }
        PortugueseThirdDivisionStandings table=new PortugueseThirdDivisionStandings(clubs,mini,null,new int[worldSize],use.reds,use.yellows,use.ageDays(),use.ageUses());ArrayList<Integer> out=new ArrayList<>();
        for(int start=0;start<table.order.length;){int end=start+1;while(end<table.order.length&&table.tiedAt(end-1))end++;ArrayList<Integer> remaining=new ArrayList<>();for(int i=start;i<end;i++)remaining.add(table.order[i]);if(remaining.size()>1)remaining=resolve(remaining,key+"D"+start,use,scores,iteration+1);out.addAll(remaining);start=end;}return out;
    }
    private String result(String prefix){for(String event:events)if(event.startsWith(prefix))return event.substring(prefix.length());if(restoring==null)return null;String value=restoring.remove(prefix);if(value==null)throw new IllegalArgumentException("Missing neutral deciding result");events.add(prefix+value);return value;}
    void restoreEvents(List<String> saved){restoring=new LinkedHashMap<>();for(String row:saved){String[] p=row.split("\\|",4);if(p.length!=4||p[0].length()>256||restoring.put(p[0]+"|"+p[1]+"|"+p[2]+"|",p[3])!=null)throw new IllegalArgumentException("Invalid lower deciding games");}}
    void finishRestore(){if(restoring!=null&&!restoring.isEmpty())throw new IllegalArgumentException("Unrelated lower deciding game");restoring=null;}
    private static void requireSimulation(PortugueseThirdDivisionSeason.Scores scores){if(scores==null)throw new IllegalStateException("Missing lower sporting resolution");}
    static void simulate(KnockoutTie tie,PortugueseThirdDivisionSeason.Scores scores,long seed) {
        int h=tie.home(),a=tie.away();tie.recordRegulation(scores.goals(h,a,!tie.neutral),scores.goals(a,h,false));if(tie.phase()==KnockoutTie.Phase.EXTRA_TIME)tie.recordExtraTime(scores.goals(h,a,!tie.neutral)/3,scores.goals(a,h,false)/3);if(tie.phase()==KnockoutTie.Phase.PENALTIES){boolean home=new Random(seed^tie.firstHome^((long)tie.firstAway<<32)).nextBoolean();tie.recordPenalties(home?5:4,home?4:5);}
    }
}

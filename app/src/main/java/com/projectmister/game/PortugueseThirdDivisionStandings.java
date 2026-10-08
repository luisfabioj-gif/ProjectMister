package com.projectmister.game;

import java.util.*;

/** FPF Liga 3 art.17: phase results, whole-competition results, discipline and used-player ages. */
public final class PortugueseThirdDivisionStandings {
    public final Integer[] order;
    private final boolean[] tied;
    public PortugueseThirdDivisionStandings(int[] clubs,LeagueResults phase,LeagueResults previous,int[] bonus,
            int[] reds,int[] yellows,long[] ageDays,int[] ageUses) {
        order=new Integer[clubs.length];for(int i=0;i<clubs.length;i++)order[i]=clubs[i];tied=new boolean[Math.max(0,clubs.length-1)];
        int[][] stats=stats(phase),all=stats(phase),old=previous==null?null:stats(previous);
        if(old!=null)for(int c=0;c<all.length;c++)for(int j=0;j<5;j++)all[c][j]+=old[c][j];
        Arrays.sort(order,(a,b)->Integer.compare(stats[b][1]+bonus[b],stats[a][1]+bonus[a]));
        for(int start=0;start<order.length;) {
            int end=start+1;while(end<order.length&&stats[order[start]][1]+bonus[order[start]]==stats[order[end]][1]+bonus[order[end]])end++;
            if(end-start>1) {
                int[] group=new int[end-start];for(int i=start;i<end;i++)group[i-start]=order[i];
                int[][] mini=phase.miniTable(group),whole=phase.miniTable(group);
                if(previous!=null){int[][] before=previous.miniTable(group);for(int c:group)for(int j=0;j<5;j++)whole[c][j]+=before[c][j];}
                Comparator<Integer> sporting=(a,b)->{
                    int[] ac={mini[a][1],mini[a][2]-mini[a][3],stats[a][2]-stats[a][3],stats[a][4],stats[a][2],-stats[a][3],whole[a][1],whole[a][2]-whole[a][3],all[a][2]-all[a][3],all[a][4],all[a][2],-all[a][3],-reds[a],-yellows[a]};
                    int[] bc={mini[b][1],mini[b][2]-mini[b][3],stats[b][2]-stats[b][3],stats[b][4],stats[b][2],-stats[b][3],whole[b][1],whole[b][2]-whole[b][3],all[b][2]-all[b][3],all[b][4],all[b][2],-all[b][3],-reds[b],-yellows[b]};
                    for(int i=0;i<ac.length;i++){int c=Integer.compare(bc[i],ac[i]);if(c!=0)return c;}
                    return ageUses[a]==0||ageUses[b]==0?0:Long.compare(ageDays[a]*ageUses[b],ageDays[b]*ageUses[a]);
                };
                Arrays.sort(order,start,end,(a,b)->{int c=sporting.compare(a,b);return c==0?Integer.compare(a,b):c;});
                for(int i=start;i<end-1;i++)tied[i]=sporting.compare(order[i],order[i+1])==0;
            }
            start=end;
        }
    }
    public boolean tiedAt(int left){return left>=0&&left<tied.length&&tied[left];}
    private static int[][] stats(LeagueResults ledger) {
        int[][] out=new int[ledger.clubCount][5];for(int r=0;r<100;r++)for(LeagueResults.Result f:ledger.round(r)) {
            out[f.home][0]++;out[f.away][0]++;out[f.home][2]+=f.homeGoals;out[f.home][3]+=f.awayGoals;out[f.away][2]+=f.awayGoals;out[f.away][3]+=f.homeGoals;
            if(f.homeGoals>f.awayGoals){out[f.home][1]+=3;out[f.home][4]++;}else if(f.homeGoals<f.awayGoals){out[f.away][1]+=3;out[f.away][4]++;}else{out[f.home][1]++;out[f.away][1]++;}
        }return out;
    }
}

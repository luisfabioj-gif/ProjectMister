package com.projectmister.game;

import java.util.Arrays;

/** DFL SpOL section 2.3.c, 11 June 2026. Shared positions survive missing history.
 * A mini-table evaluates a whole tied group; no cyclic pairwise comparator. */
public final class GermanStandings {
    public final Integer[] order;
    private final boolean[] tiedWithNext;
    public GermanStandings(Integer[] clubs,int[] points,int[] scored,int[] conceded,LeagueResults ledger) {
        order=clubs.clone();tiedWithNext=new boolean[Math.max(0,clubs.length-1)];
        java.util.Comparator<Integer> base=(a,b)->{
            int c=Integer.compare(points[b],points[a]);if(c!=0)return c;
            c=Integer.compare(scored[b]-conceded[b],scored[a]-conceded[a]);if(c!=0)return c;
            return Integer.compare(scored[b],scored[a]);
        };
        Arrays.sort(order,base);int[] all=new int[clubs.length];for(int i=0;i<clubs.length;i++)all[i]=clubs[i];
        int[][] season=ledger.miniTable(all);
        for(int start=0;start<order.length;) {
            int end=start+1;while(end<order.length&&base.compare(order[start],order[end])==0)end++;
            if(end-start>1) {
                int[] ids=new int[end-start];for(int i=start;i<end;i++)ids[i-start]=order[i];
                int[][] mini=ledger.miniTable(ids);boolean complete=ledger.recordedFromStart&&ledger.meetingsComplete(ids,2);
                java.util.Comparator<Integer> head=(a,b)->{
                    if(!complete)return 0;
                    int c=Integer.compare(mini[b][2]-mini[b][3],mini[a][2]-mini[a][3]);if(c!=0)return c;
                    c=Integer.compare(mini[b][4],mini[a][4]);if(c!=0)return c;
                    return Integer.compare(season[b][4],season[a][4]);
                };
                Arrays.sort(order,start,end,(a,b)->{int c=head.compare(a,b);return c!=0?c:Integer.compare(a,b);});
                for(int i=start;i<end-1;i++)tiedWithNext[i]=head.compare(order[i],order[i+1])==0;
            }
            start=end;
        }
    }
    public int rankAt(int position){if(position<0||position>=order.length)throw new IllegalArgumentException("Invalid position");while(position>0&&tiedAt(position-1))position--;return position+1;}
    public boolean tiedAt(int leftPosition){return leftPosition>=0&&leftPosition<tiedWithNext.length&&tiedWithNext[leftPosition];}
}

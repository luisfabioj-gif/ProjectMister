package com.projectmister.game;

import java.util.*;

/** LFP 518 ter. Unrecorded discipline/lot outcomes remain unresolved. */
public final class FrenchStandings {
    public final Integer[] order;
    private final boolean[] tied;
    public FrenchStandings(Integer[] clubs,int[] points,int[] gf,int[] ga,int[] wins,LeagueResults ledger,boolean finished) {
        order=clubs.clone();tied=new boolean[Math.max(0,clubs.length-1)];int[][] away=ledger.awayTable();
        Comparator<Integer> initial=(a,b)->{int c=Integer.compare(points[b],points[a]);return c!=0?c:Integer.compare(gf[b]-ga[b],gf[a]-ga[a]);};
        Arrays.sort(order,initial);
        for(int start=0;start<order.length;) {
            int end=start+1;while(end<order.length&&initial.compare(order[start],order[end])==0)end++;
            if(end-start>1) {
                int[] group=new int[end-start];for(int i=start;i<end;i++)group[i-start]=order[i];
                int[][] mini=ledger.miniTable(group);
                boolean complete=ledger.recordedFromStart&&ledger.meetingsComplete(group,2);
                Comparator<Integer> sporting=(a,b)->{
                    if(finished&&!complete)return 0;
                    int c;
                    if(complete) {
                        c=Integer.compare(mini[b][1],mini[a][1]);if(c!=0)return c;
                        c=Integer.compare(mini[b][2]-mini[b][3],mini[a][2]-mini[a][3]);if(c!=0)return c;
                    }
                    c=Integer.compare(gf[b],gf[a]);if(c!=0)return c;
                    c=Integer.compare(wins[b],wins[a]);if(c!=0)return c;
                    return ledger.recordedFromStart?Integer.compare(away[b][0],away[a][0]):0;
                };
                Arrays.sort(order,start,end,(a,b)->{int c=sporting.compare(a,b);return c==0?Integer.compare(a,b):c;});
                for(int i=start;i<end-1;i++)tied[i]=sporting.compare(order[i],order[i+1])==0;
            }
            start=end;
        }
    }
    public boolean tiedAt(int left){return left>=0&&left<tied.length&&tied[left];}
    public int rankAt(int index){if(index<0||index>=order.length)throw new IllegalArgumentException("Invalid position");while(index>0&&tiedAt(index-1))index--;return index+1;}
}

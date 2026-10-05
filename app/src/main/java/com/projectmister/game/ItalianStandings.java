package com.projectmister.game;

import java.util.Arrays;
import java.util.Comparator;

/** FIGC 25/A and 244/A classifica avulsa. Draw-of-lots outcomes are not invented. */
public final class ItalianStandings {
    public final Integer[] order;
    private final boolean[] tied;
    public ItalianStandings(Integer[] clubs,int[] points,int[] gf,int[] ga,int[] wins,
                            LeagueResults ledger,boolean finished) {
        order=clubs.clone();tied=new boolean[Math.max(0,clubs.length-1)];
        Arrays.sort(order,(a,b)->Integer.compare(points[b],points[a]));
        for(int start=0;start<order.length;) {
            int end=start+1;while(end<order.length&&points[order[start]]==points[order[end]])end++;
            if(end-start>1) {
                int[] group=new int[end-start];for(int i=start;i<end;i++)group[i-start]=order[i];
                int[][] mini=ledger.miniTable(group);
                boolean history=ledger.recordedFromStart,complete=history&&ledger.meetingsComplete(group,2);
                Comparator<Integer> sporting=(a,b)->{
                    // Missing old match history must not manufacture final head-to-head outcomes.
                    if(finished&&!complete)return 0;
                    int c;
                    if(complete) {
                        c=Integer.compare(mini[b][1],mini[a][1]);if(c!=0)return c;
                    }
                    if(complete){c=Integer.compare(mini[b][2]-mini[b][3],mini[a][2]-mini[a][3]);if(c!=0)return c;}
                    c=Integer.compare(gf[b]-ga[b],gf[a]-ga[a]);if(c!=0)return c;
                    return Integer.compare(gf[b],gf[a]);
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

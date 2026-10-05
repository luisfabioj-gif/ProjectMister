package com.projectmister.game;

import java.util.Arrays;
import java.util.Comparator;

/** Liga Portugal art.18 and RFEF 2026/27 arts.10/17. Unresolved ranks remain shared. */
public final class IberianStandings {
    public final Integer[] order;
    private final boolean[] tied;
    public IberianStandings(String country,Integer[] clubs,int[] points,int[] gf,int[] ga,int[] wins,
                            LeagueResults ledger,boolean finished) {
        if(!country.equals("PT")&&!country.equals("ES"))throw new IllegalArgumentException("Unsupported standings");
        boolean portuguese=country.equals("PT");order=clubs.clone();tied=new boolean[Math.max(0,clubs.length-1)];
        Arrays.sort(order,(a,b)->Integer.compare(points[b],points[a]));
        for(int start=0;start<order.length;) {
            int end=start+1;while(end<order.length&&points[order[start]]==points[order[end]])end++;
            if(end-start>1) {
                int[] group=new int[end-start];for(int i=start;i<end;i++)group[i-start]=order[i];
                int[][] mini=ledger.miniTable(group);
                boolean history=ledger.recordedFromStart,complete=history&&ledger.meetingsComplete(group,2),multi=group.length>2;
                Comparator<Integer> sporting=(a,b)->{
                    // Missing old match history must not manufacture final head-to-head outcomes.
                    if(finished&&!complete)return 0;
                    int c;
                    if(history&&(portuguese||complete&&multi)) {
                        c=Integer.compare(mini[b][1],mini[a][1]);if(c!=0)return c;
                    }
                    if(complete){c=Integer.compare(mini[b][2]-mini[b][3],mini[a][2]-mini[a][3]);if(c!=0)return c;}
                    c=Integer.compare(gf[b]-ga[b],gf[a]-ga[a]);if(c!=0)return c;
                    if(portuguese){c=Integer.compare(wins[b],wins[a]);if(c!=0)return c;}
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
    /** Preserve ties after reserve exclusions, even if reserve rows separated two eligible clubs. */
    public int[] eligible(boolean[] reserve,int count) {
        java.util.ArrayList<Integer> ids=new java.util.ArrayList<>(),positions=new java.util.ArrayList<>();
        for(int i=0;i<order.length;i++)if(!reserve[order[i]]){ids.add(order[i]);positions.add(i);}
        if(ids.size()<count)throw new IllegalStateException("Too few eligible clubs");
        for(int i=0;i<Math.min(count,ids.size()-1);i++)
            if(rankAt(positions.get(i))==rankAt(positions.get(i+1)))throw new IllegalStateException("Unresolved eligible places");
        int[] result=new int[count];for(int i=0;i<count;i++)result[i]=ids.get(i);return result;
    }
}

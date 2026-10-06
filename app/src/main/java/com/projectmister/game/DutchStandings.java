package com.projectmister.game;

import java.util.*;

/** KNVB RWB article 17. Final disciplinary/deciding outcomes remain unresolved. */
public final class DutchStandings {
    public final Integer[] order;
    private final boolean[] tied;
    public DutchStandings(Integer[] clubs,int[] points,int[] played,int[] gf,int[] ga,String[] names,LeagueResults ledger,boolean finished) {
        order=clubs.clone();tied=new boolean[Math.max(0,clubs.length-1)];
        Comparator<Integer> base=(a,b)->{
            int c=Integer.compare(points[b],points[a]);if(c!=0)return c;
            if(!finished){c=Integer.compare(played[a]*3-points[a],played[b]*3-points[b]);if(c!=0)return c;}
            c=Integer.compare(gf[b]-ga[b],gf[a]-ga[a]);if(c!=0)return c;
            return Integer.compare(gf[b],gf[a]);
        };
        Arrays.sort(order,base);
        for(int start=0;start<order.length;) {
            int end=start+1;while(end<order.length&&base.compare(order[start],order[end])==0)end++;
            if(end-start>1) {
                int[] ids=new int[end-start];for(int i=start;i<end;i++)ids[i-start]=order[i];
                int[][] mini=ledger.miniTable(ids);boolean available=ledger.recordedFromStart&&(!finished||ledger.meetingsComplete(ids,2));
                Comparator<Integer> head=(a,b)->{
                    if(!available)return 0;
                    int c=Integer.compare(mini[b][1],mini[a][1]);if(c!=0)return c;
                    c=Integer.compare(mini[b][2]-mini[b][3],mini[a][2]-mini[a][3]);if(c!=0)return c;
                    c=Integer.compare(mini[b][2],mini[a][2]);if(c!=0)return c;
                    return Integer.compare(mini[b][4],mini[a][4]);
                };
                Arrays.sort(order,start,end,(a,b)->{int c=head.compare(a,b);if(c==0&&!finished)c=names[a].compareToIgnoreCase(names[b]);return c==0?Integer.compare(a,b):c;});
                if(finished)for(int i=start;i<end-1;i++)tied[i]=head.compare(order[i],order[i+1])==0;
            }
            start=end;
        }
    }
    public boolean tiedAt(int left){return left>=0&&left<tied.length&&tied[left];}
    public int rankAt(int index){if(index<0||index>=order.length)throw new IllegalArgumentException("Invalid position");while(index>0&&tiedAt(index-1))index--;return index+1;}
}

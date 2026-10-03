package com.projectmister.game;

import java.util.Arrays;

/** SPFL C36: group, points, GD, GF, tied-group head-to-head points and GD.
 * Stable IDs order display-only ties; unresolved ties must not decide admissions. */
public final class ScottishStandings {
    public final Integer[] order;
    private final boolean[] tiedWithNext;
    public ScottishStandings(Integer[] clubs,int[] groups,int[] points,int[] scored,int[] conceded,LeagueResults ledger) {
        order=clubs.clone();tiedWithNext=new boolean[Math.max(0,clubs.length-1)];
        java.util.Comparator<Integer> base=(a,b)->{
            int c=Integer.compare(groups[a],groups[b]);if(c!=0)return c;
            c=Integer.compare(points[b],points[a]);if(c!=0)return c;
            c=Integer.compare(scored[b]-conceded[b],scored[a]-conceded[a]);if(c!=0)return c;
            return Integer.compare(scored[b],scored[a]);
        };
        Arrays.sort(order,base);
        for(int start=0;start<order.length;) {
            int end=start+1;while(end<order.length&&base.compare(order[start],order[end])==0)end++;
            if(end-start>1) {
                int[] ids=new int[end-start];for(int i=start;i<end;i++)ids[i-start]=order[i];
                int[][] mini=ledger.miniTable(ids);
                java.util.Comparator<Integer> head=(a,b)->{
                    if(!ledger.recordedFromStart)return 0;
                    int c=Integer.compare(mini[b][1],mini[a][1]);if(c!=0)return c;
                    return Integer.compare(mini[b][2]-mini[b][3],mini[a][2]-mini[a][3]);
                };
                Arrays.sort(order,start,end,(a,b)->{int c=head.compare(a,b);return c!=0?c:Integer.compare(a,b);});
                for(int i=start;i<end-1;i++)tiedWithNext[i]=head.compare(order[i],order[i+1])==0;
            }
            start=end;
        }
    }
    public int rankAt(int position){
        if(position<0||position>=order.length)throw new IllegalArgumentException("Invalid table position");
        while(position>0&&tiedAt(position-1))position--;
        return position+1;
    }
    public boolean tiedAt(int leftPosition){return leftPosition>=0&&leftPosition<tiedWithNext.length&&tiedWithNext[leftPosition];}
}

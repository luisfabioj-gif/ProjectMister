package com.projectmister.game;
import java.util.Arrays;

/** TFF Football Competition Instructions, article 9. A shared mini-table is
 * retained for multi-club ties; narrowed subsets are not recalculated. */
public final class TurkishStandings {
    public final Integer[] order;
    private final boolean[] tied;
    public TurkishStandings(Integer[] clubs,int[] points,int[] gf,int[] ga,LeagueResults ledger,boolean seasonComplete){
        order=clubs.clone();tied=new boolean[Math.max(0,clubs.length-1)];
        Arrays.sort(order,(a,b)->Integer.compare(points[b],points[a]));
        for(int start=0;start<order.length;){
            int end=start+1;while(end<order.length&&points[order[start]]==points[order[end]])end++;
            if(end-start>1){
                int[] ids=new int[end-start];for(int i=start;i<end;i++)ids[i-start]=order[i];
                int[][] mini=ledger.miniTable(ids);boolean known=ledger.recordedFromStart&&ledger.meetingsComplete(ids,2),multi=ids.length>2;
                java.util.Comparator<Integer> sporting=(a,b)->{
                    if(known){int c=Integer.compare(mini[b][1],mini[a][1]);if(c!=0)return c;
                        c=Integer.compare(mini[b][2]-mini[b][3],mini[a][2]-mini[a][3]);if(c!=0)return c;
                        if(multi){c=Integer.compare(mini[b][2],mini[a][2]);if(c!=0)return c;}}
                    // Without complete history, final tied points cannot safely award a place.
                    if(!known&&seasonComplete)return 0;
                    int c=Integer.compare(gf[b]-ga[b],gf[a]-ga[a]);return c!=0?c:Integer.compare(gf[b],gf[a]);
                };
                Arrays.sort(order,start,end,(a,b)->{int c=sporting.compare(a,b);return c!=0?c:Integer.compare(a,b);});
                for(int i=start;i<end-1;i++)tied[i]=sporting.compare(order[i],order[i+1])==0;
            }
            start=end;
        }
    }
    // The simulation has no forfeiture events. No fake fair-play ranking is substituted.
    public boolean tiedAt(int position){return position>=0&&position<tied.length&&tied[position];}
    public int rankAt(int position){if(position<0||position>=order.length)throw new IllegalArgumentException("Invalid position");while(position>0&&tiedAt(position-1))position--;return position+1;}
}

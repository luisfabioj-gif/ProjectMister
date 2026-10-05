package com.projectmister.game;
import java.util.*;

/** RBFA Book B7.42. Equal final criteria require a test match, never an ID tiebreak. */
public final class BelgianStandings {
    public final Integer[] order;
    private final boolean[] tied;
    public BelgianStandings(Integer[] clubs,int[] points,int[] gf,int[] ga,int[] wins,LeagueResults ledger) {
        order=clubs.clone();tied=new boolean[Math.max(0,clubs.length-1)];int[][] away=ledger.awayTable();
        Comparator<Integer> sporting=(a,b)->{
            int c=Integer.compare(points[b],points[a]);if(c!=0)return c;
            c=Integer.compare(wins[b],wins[a]);if(c!=0)return c;
            c=Integer.compare(gf[b]-ga[b],gf[a]-ga[a]);if(c!=0)return c;
            c=Integer.compare(gf[b],gf[a]);if(c!=0)return c;
            if(!ledger.recordedFromStart)return 0;
            c=Integer.compare(away[b][0],away[a][0]);if(c!=0)return c;
            c=Integer.compare(away[b][1]-away[b][2],away[a][1]-away[a][2]);if(c!=0)return c;
            return Integer.compare(away[b][1],away[a][1]);
        };
        Arrays.sort(order,(a,b)->{int c=sporting.compare(a,b);return c==0?Integer.compare(a,b):c;});
        for(int i=0;i<tied.length;i++)tied[i]=sporting.compare(order[i],order[i+1])==0;
    }
    public boolean tiedAt(int left){return left>=0&&left<tied.length&&tied[left];}
    public int rankAt(int index){if(index<0||index>=order.length)throw new IllegalArgumentException("Invalid position");while(index>0&&tiedAt(index-1))index--;return index+1;}
    public int[] eligible(boolean[] reserves) {
        ArrayList<Integer> eligible=new ArrayList<>(),positions=new ArrayList<>();
        for(int i=0;i<order.length;i++)if(!reserves[order[i]]){eligible.add(order[i]);positions.add(i);}
        if(eligible.size()<5)throw new IllegalStateException("Too few eligible clubs");
        for(int i=0;i<Math.min(5,eligible.size()-1);i++)if(rankAt(positions.get(i))==rankAt(positions.get(i+1)))throw new IllegalStateException("Unresolved eligible places");
        if(positions.get(4)>=order.length-2)throw new IllegalStateException("Relegated club cannot enter playoffs");
        int[] result=new int[5];for(int i=0;i<5;i++)result[i]=eligible.get(i);return result;
    }
}

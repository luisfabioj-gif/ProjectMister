package com.projectmister.game;

import java.util.*;

/** Rebuild period qualification from immutable results, never from final points alone. */
public final class DutchQualification {
    private DutchQualification(){}
    // KNVB's published calendar highlights 10/19/28/38 and its announcement says 10/9/9/10.
    // Article 1's '21' start conflicts with both. See NATIONAL_RULES_VERIFICATION.md.
    private static final int[] ENDS={10,19,28,38};
    public static int periodOfRound(int zeroBasedRound){if(zeroBasedRound<0||zeroBasedRound>=38)throw new IllegalArgumentException("Invalid round");for(int i=0;i<4;i++)if(zeroBasedRound<ENDS[i])return i;throw new AssertionError();}
    /** Returned order: two automatic promotions, then six playoff seeds in final-table order. */
    public static int[] entrants(DutchStandings finalTable,boolean[] reserve,LeagueResults ledger) {
        if(finalTable.order.length!=20||!ledger.recordedFromStart)throw new IllegalStateException("Complete Dutch season history required");
        Set<Integer> members=new HashSet<>(Arrays.asList(finalTable.order));
        int[][][] tables=new int[4][ledger.clubCount][3];
        for(int round=0;round<38;round++) {
            Set<Integer> played=new HashSet<>();int[][] table=tables[periodOfRound(round)];
            for(LeagueResults.Result r:ledger.round(round))if(members.contains(r.home)||members.contains(r.away)) {
                if(!members.contains(r.home)||!members.contains(r.away)||!played.add(r.home)||!played.add(r.away))throw new IllegalStateException("Invalid period fixture");
                table[r.home][1]+=r.homeGoals-r.awayGoals;table[r.away][1]+=r.awayGoals-r.homeGoals;
                table[r.home][2]+=r.homeGoals;table[r.away][2]+=r.awayGoals;
                if(r.homeGoals>r.awayGoals)table[r.home][0]+=3;else if(r.homeGoals<r.awayGoals)table[r.away][0]+=3;else{table[r.home][0]++;table[r.away][0]++;}
            }
            if(played.size()!=20)throw new IllegalStateException("Incomplete period results");
        }
        return select(finalTable,reserve,tables);
    }
    static int[] select(DutchStandings finalTable,boolean[] reserve,int[][][] periodTables) {
        if(finalTable.order.length!=20||periodTables.length!=4)throw new IllegalArgumentException("Invalid period field");
        // An unresolved final group can affect automatic selection, fallback or playoff seeding.
        for(int i=0;i<19;i++)if(finalTable.tiedAt(i))throw new IllegalStateException("Final Dutch ranking unresolved");
        ArrayList<Integer> eligible=new ArrayList<>();for(int id:finalTable.order)if(!reserve[id])eligible.add(id);
        if(eligible.size()<8)throw new IllegalStateException("Insufficient eligible clubs");
        Set<Integer> rights=new HashSet<>();
        for(int[][] table:periodTables) {
            Integer[] order=finalTable.order.clone();
            Comparator<Integer> cmp=(a,b)->{for(int k=0;k<3;k++){int c=Integer.compare(table[b][k],table[a][k]);if(c!=0)return c;}return 0;};
            Arrays.sort(order,cmp);
            int spot=periodSpot(order,cmp,reserve,rights);
            if(spot>=0)rights.add(spot);
        }
        int auto1=eligible.get(0),auto2=eligible.get(1),last=finalTable.order[19];
        rights.remove(auto1);rights.remove(auto2);rights.remove(last);
        for(int id:eligible)if(rights.size()<6&&id!=auto1&&id!=auto2&&id!=last)rights.add(id);
        if(rights.size()!=6)throw new IllegalStateException("Invalid playoff field");
        int[] result=new int[8];result[0]=auto1;result[1]=auto2;int at=2;
        for(int id:finalTable.order)if(rights.contains(id))result[at++]=id;
        return result;
    }
    private static int periodSpot(Integer[] order,Comparator<Integer> cmp,boolean[] reserve,Set<Integer> rights) {
        for(int start=0;start<2;) {
            int end=start+1;while(end<order.length&&cmp.compare(order[start],order[end])==0)end++;
            if(end-start>2)throw new IllegalStateException("Period disciplinary/deciding outcome required");
            int candidate=-1,count=0;
            for(int i=start;i<end;i++)if(!reserve[order[i]]&&!rights.contains(order[i])){candidate=order[i];count++;}
            if(count>1)throw new IllegalStateException("Period disciplinary/deciding outcome required");
            if(count==1)return candidate; // article 8 ex-aequo exception: only one eligible claimant.
            start=end;
        }
        return -1; // Both top places already qualified or reserves: final-table replacement.
    }
}

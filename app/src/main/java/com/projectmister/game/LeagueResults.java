package com.projectmister.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Results are scoped to the club order owned by the career snapshot. Never reconstruct old scores. */
public final class LeagueResults {
    public static final class Result {
        public final int round,home,away,homeGoals,awayGoals;
        private Result(int r,int h,int a,int hg,int ag){round=r;home=h;away=a;homeGoals=hg;awayGoals=ag;}
    }
    public final int clubCount;
    public final boolean recordedFromStart;
    private final ArrayList<Result> results=new ArrayList<>();
    public LeagueResults(int clubs,boolean fromStart) {
        if(clubs<2||clubs>768)throw new IllegalArgumentException("Invalid result universe");
        clubCount=clubs;recordedFromStart=fromStart;
    }
    /** A repeated identical delivery is harmless; conflicting/double-booked fixtures are rejected. */
    public boolean record(int round,int home,int away,int homeGoals,int awayGoals) {
        if(round<0||round>=100||home<0||away<0||home>=clubCount||away>=clubCount||home==away
                ||homeGoals<0||awayGoals<0||homeGoals>99||awayGoals>99)throw new IllegalArgumentException("Invalid league result");
        for(Result old:results)if(old.round==round) {
            if(old.home==home&&old.away==away) {
                if(old.homeGoals!=homeGoals||old.awayGoals!=awayGoals)throw new IllegalStateException("Conflicting league result");
                return false;
            }
            if(old.home==home||old.away==home||old.home==away||old.away==away)throw new IllegalStateException("Club plays twice in a round");
        }
        results.add(new Result(round,home,away,homeGoals,awayGoals));return true;
    }
    public int size(){return results.size();}
    public List<Result> round(int round) {
        ArrayList<Result> list=new ArrayList<>();for(Result r:results)if(r.round==round)list.add(r);
        return Collections.unmodifiableList(list);
    }
    public Result fixture(int round,int home,int away) {
        for(Result r:results)if(r.round==round&&r.home==home&&r.away==away)return r;
        return null;
    }
    /** Columns: played, points, goals for, goals against, away goals. Includes ONLY group members. */
    public int[][] miniTable(int[] members) {
        boolean[] included=new boolean[clubCount];for(int id:members) {
            if(id<0||id>=clubCount||included[id])throw new IllegalArgumentException("Invalid tied group");included[id]=true;
        }
        int[][] table=new int[clubCount][5];
        for(Result r:results)if(included[r.home]&&included[r.away]) {
            int[] h=table[r.home],a=table[r.away];h[0]++;a[0]++;
            h[2]+=r.homeGoals;h[3]+=r.awayGoals;a[2]+=r.awayGoals;a[3]+=r.homeGoals;a[4]+=r.awayGoals;
            if(r.homeGoals>r.awayGoals)h[1]+=3;else if(r.homeGoals<r.awayGoals)a[1]+=3;else{h[1]++;a[1]++;}
        }
        return table;
    }
    /** Verify each pair, not merely the total games in a tied mini-table. */
    public boolean meetingsComplete(int[] members,int expected) {
        if(expected<1)throw new IllegalArgumentException("Invalid meeting count");
        for(int i=0;i<members.length;i++)for(int j=i+1;j<members.length;j++) {
            int count=0;for(Result r:results)if((r.home==members[i]&&r.away==members[j])||(r.home==members[j]&&r.away==members[i]))count++;
            if(count!=expected)return false;
        }
        return true;
    }
    /** Full-season away wins, goals for and goals against, for Belgian ranking. */
    public int[][] awayTable() {
        int[][] table=new int[clubCount][3];
        for(Result r:results){if(r.awayGoals>r.homeGoals)table[r.away][0]++;table[r.away][1]+=r.awayGoals;table[r.away][2]+=r.homeGoals;}
        return table;
    }
    public String snapshot() {
        StringBuilder s=new StringBuilder("1;").append(clubCount).append(';').append(recordedFromStart?1:0);
        for(Result r:results)s.append('|').append(r.round).append(',').append(r.home).append(',').append(r.away)
                .append(',').append(r.homeGoals).append(',').append(r.awayGoals);
        return s.toString();
    }
    public static LeagueResults restore(String encoded,int expectedClubs) {
        if(encoded==null||encoded.length()>131072)throw new IllegalArgumentException("Invalid result data");
        String[] rows=encoded.split("\\|",-1),header=rows[0].split(";",-1);
        if(rows.length>3201||header.length!=3||!header[0].equals("1")||Integer.parseInt(header[1])!=expectedClubs
                ||(!header[2].equals("0")&&!header[2].equals("1")))throw new IllegalArgumentException("Invalid result schema");
        LeagueResults ledger=new LeagueResults(expectedClubs,header[2].equals("1"));
        for(int i=1;i<rows.length;i++) {
            String[] p=rows[i].split(",",-1);if(p.length!=5)throw new IllegalArgumentException("Invalid result row");
            if(!ledger.record(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]),Integer.parseInt(p[3]),Integer.parseInt(p[4])))
                throw new IllegalArgumentException("Repeated saved result");
        }
        return ledger;
    }
}

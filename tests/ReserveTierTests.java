package com.projectmister.game;
import java.util.*;
public final class ReserveTierTests {
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void main(String[] args) {
        String[] ids={"es:real-sociedad","es:celta","es:other-top","es:real-sociedad-b","es:celta-fortuna","es:promoted","es:lower-a","es:lower-b","es:lower-c","fr:foreign"};
        int[] levels={1,1,1,2,2,2,3,3,3,1},strength={80,80,80,60,60,60,60,60,60,90};boolean[] reserves={false,false,false,true,true,false,false,false,false,false};
        ReserveTierOutcome out=new ReserveTierOutcome("ES",ids,levels,reserves,strength,new int[]{5},new int[]{0},12);
        check(out.levels[0]==2&&out.levels[3]==3&&out.levels[4]==2,"Parent relegation forces only its reserve down");
        check(out.levels[5]==1&&out.levels[9]==1,"Promotion and foreign membership retained");
        check(Arrays.equals(out.levels,new ReserveTierOutcome("ES",ids,levels,reserves,strength,new int[]{5},new int[]{0},12).levels),"Lower qualification replay deterministic");
        check(Arrays.equals(levels,new int[]{1,1,1,2,2,2,3,3,3,1}),"Original membership not mutated");
        check(out.rulings.size()==2&&out.rulings.get(1).contains("simulated"),"Replacement provenance explicit");
        for(long seed=0;seed<60;seed++){ReserveTierOutcome multi=new ReserveTierOutcome("ES",ids,levels,reserves,strength,new int[]{5},new int[]{1},seed);check(multi.levels[4]==3&&multi.levels[3]==2,"Second reserve collision resolves");}
        boolean rejected=false;try{new ReserveTierOutcome("ES",ids,levels,reserves,strength,new int[]{3},new int[]{0},1);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Reserve promotion rejected");
        System.out.println("PASS: forced reserve relegation, deterministic eligible lower replacements, fixed division sizes and stable foreign identities");
    }
}

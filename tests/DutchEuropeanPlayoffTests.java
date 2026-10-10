package com.projectmister.game;
import java.time.LocalDate;
import java.util.*;

public final class DutchEuropeanPlayoffTests {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void reject(Runnable action){try{action.run();throw new AssertionError("Invalid playoff accepted");}catch(IllegalArgumentException|IllegalStateException expected){}}
    public static void main(String[] args) {
        int[] order={10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27};
        for(int cupRank=-1;cupRank<18;cupRank++) {
            int cup=cupRank<0?30:order[cupRank];DutchEuropeanPlayoff playoff=new DutchEuropeanPlayoff(2026,order,cup,Collections.emptyList());
            int[] expected=cupRank>=0&&cupRank<3?new int[]{14,15,16,17}:cupRank>=3&&cupRank<=6?Arrays.stream(new int[]{13,14,15,16,17}).filter(c->c!=cup).toArray():new int[]{13,14,15,16};
            check(Arrays.equals(expected,playoff.entrants()),"Cup winner pass-down: "+cupRank);check(playoff.semiDate().equals(LocalDate.of(2027,5,27)),"Published provisional semi-final window");
            check(playoff.current().firstHome==expected[0]&&playoff.current().firstAway==expected[3],"First plays fourth");
            playoff.current().recordRegulation(1,1);playoff.current().recordExtraTime(0,0);playoff.current().recordPenalties(4,5);
            playoff=DutchEuropeanPlayoff.restore(playoff.snapshot());check(playoff.current().firstHome==expected[1]&&playoff.current().firstAway==expected[2],"Second plays third");playoff.current().recordRegulation(2,0);
            check(playoff.current().home()==expected[1]&&playoff.current().away()==expected[3],"Higher league finisher hosts the final");playoff.current().recordRegulation(0,3);
            check(playoff.complete()&&playoff.winner()==expected[3],"Lowest entrant can win the ticket");String saved=playoff.snapshot();check(saved.equals(DutchEuropeanPlayoff.restore(saved).snapshot()),"Exact completed archive");
            String[] ids=new String[32];Arrays.fill(ids,"nl:test");playoff.validate(ids,new boolean[32],2026,LocalDate.of(2027,5,30));final DutchEuropeanPlayoff finished=playoff;reject(()->finished.validate(ids,new boolean[32],2026,LocalDate.of(2027,5,29)));
        }
        DutchEuropeanPlayoff moved=new DutchEuropeanPlayoff(2027,order,30,Arrays.asList(LocalDate.of(2028,5,27),LocalDate.of(2028,5,30)),LocalDate.of(2028,5,27));
        check(moved.semiDate().equals(LocalDate.of(2028,6,1))&&moved.finalDate().equals(LocalDate.of(2028,6,4)),"Recovery reschedule preserves chronology");
        String movedSaved=moved.snapshot();check(movedSaved.equals(DutchEuropeanPlayoff.restore(movedSaved).snapshot()),"Rescheduled dates retained");reject(()->DutchEuropeanPlayoff.restore(movedSaved.replace("NE1|2027","NE1|2028")));reject(()->new DutchEuropeanPlayoff(2026,new int[]{1,2,3,4,5,6,7,7},30,Collections.emptyList()));
        System.out.println("PASS: Dutch European playoff cup pass-down, higher-finisher hosting, extra time/shootouts, exact saves, recovery dates and corrupt-input rejection");
    }
}

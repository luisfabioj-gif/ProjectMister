package com.projectmister.game;
import java.util.*;
public final class RegressionTests {
    static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    public static void main(String[] args) {
        SubstitutionLedger s = new SubstitutionLedger(5);
        s.reset(Arrays.asList(1,2,3,4,5,6,7,8,9,10,11));
        check(s.change(1,12) && s.used()==1, "first substitution");
        check(s.change(12,13) && s.used()==1, "changing pending entrant costs one");
        check(s.change(13,1) && s.used()==0, "full reversal costs zero");
        check(!s.change(99,12), "off player must be on pitch");
        for(int i=1;i<=5;i++) check(s.change(i,i+11), "five changes allowed");
        check(!s.change(6,17), "sixth change denied");
        check(s.change(12,1) && s.used()==4, "undo at limit allowed");
        check(s.change(6,17) && s.used()==5, "reuse freed slot");
        s.commit();
        check(!s.change(17,6), "cannot reverse after play resumes");
        check(!s.change(7,18), "committed limit enforced");
        check(!s.change(7,8), "duplicate XI denied");
        s.reset(Arrays.asList(1,2,3)); s.change(1,4); s.commit();
        check(s.change(4,5) && s.used()==2, "substitute can be substituted in later stoppage");
        check(!s.canChange(2,1), "departed player cannot return via different pair");
        check(MatchMath.mobility(90,100)>MatchMath.mobility(45,100), "pace matters");
        check(MatchMath.mobility(80,95)>MatchMath.mobility(80,55), "fitness matters");
        float last=0;
        for(int i=0;i<=100;i++) { float v=MatchMath.flightProgress(i/100f); check(v>=last && v<=1,"monotonic bounded ball travel"); last=v; }
        check(MatchMath.arc(0,.1f)==0 && MatchMath.arc(1,.1f)==0, "ball lands");
        check(MatchMath.intent(0,1,85)>0 && MatchMath.intent(1,0,85)<0, "score-aware risk");
        System.out.println("PASS: substitution transactions, pace, fatigue, ball travel and score-aware intent");
    }
}

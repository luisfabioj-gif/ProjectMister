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
        check(MatchMath.playbackRate(0)<MatchMath.playbackRate(1)&&MatchMath.playbackRate(1)<MatchMath.playbackRate(2),"three ordered playback rates");
        float[] x={.2f},y={.3f},vx={0},vy={0};
        float previous=0;
        for(int i=0;i<1200;i++){
            MatchMotion.arrive(x,y,vx,vy,0,.7f,.6f,.14f,.32f,1f/120f);
            float speed=(float)Math.hypot(vx[0],vy[0]*68f/105f);
            check(speed<=.14001f,"movement speed bounded");
            check(Math.abs(speed-previous)<=.32f/120f+.0001f,"movement acceleration bounded");previous=speed;
        }
        check(Math.abs(x[0]-.7f)<.003&&Math.abs(y[0]-.6f)<.003,"arrival converges without oscillation");
        check(MatchMath.receivingLead(.5f,.1f,1f)>.5f,"passes lead moving receiver");
        check(MatchMath.receivingLead(.5f,0,1f)==.5f,"stationary receiver is not displaced");
        check(MatchMath.receivingLead(.95f,1f,10f)<=.96f,"pass lead remains on pitch");
        float last=0;
        for(int i=0;i<=100;i++) { float v=MatchMath.flightProgress(i/100f); check(v>=last && v<=1,"monotonic bounded ball travel"); last=v; }
        check(MatchMath.arc(0,.1f)==0 && MatchMath.arc(1,.1f)==0, "ball lands");
        check(MatchMath.intent(0,1,85)>0 && MatchMath.intent(1,0,85)<0, "score-aware risk");
        int purple=0xff80438d,white=0xfff4f7fb;
        check(KitColours.away(purple,purple,white)==white,"identical home and away kits switch to contrasting secondary");
        for(int home:new int[]{0xff000000,0xffffffff,0xffe62929,0xff358344,0xff1976b8,purple})
            check(KitColours.contrast(home,KitColours.away(home,home,home))>=2.2,"fallback separates identical kit colours");
        check(KitColours.away(0xff000000,white,purple)==white,"already contrasting kit preserved");
        System.out.println("PASS: substitution transactions, pace, fatigue, ball travel and score-aware intent");
    }
}

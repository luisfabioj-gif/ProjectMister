package com.projectmister.game;
import java.time.LocalDate;
import java.util.*;

public final class CupEligibilityTests {
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private static CupEligibility fresh(){CupEligibility c=new CupEligibility(2026,LocalDate.of(2026,7,1));for(int id=0;id<4;id++)c.seed(id,id);return c;}
    private static MatchDiscipline.Fixture f(String cup,int phase,int home,int away,String date){return new MatchDiscipline.Fixture(2026,cup,phase,phase,0,true,home,away,LocalDate.parse(date));}
    private static boolean eligible(CupEligibility c,MatchDiscipline.Fixture f,int id){return c.reason(f,id,f.home,20,false,null,false).isEmpty();}
    private static void reject(Runnable action){try{action.run();throw new AssertionError("Invalid participation accepted");}catch(IllegalArgumentException|IllegalStateException expected){checks++;}}
    private static void englishCups(){
        CupEligibility c=fresh();c.record(f("ENG_FA_CUP",1,0,1,"2026-11-07"),Collections.singletonMap(0,0));c.register(0,2,LocalDate.of(2026,11,8),-1);
        check(!eligible(c,f("ENG_FA_CUP",1,2,3,"2026-11-10"),0),"FA player cannot represent a different club in the same round");
        MatchDiscipline.Fixture second=f("ENG_FA_CUP",2,2,3,"2026-12-05");check(eligible(c,second,0),"FA second club allowed in a later competition-proper round");c.record(second,Collections.singletonMap(0,2));
        c.register(0,3,LocalDate.of(2027,1,1),-1);check(!eligible(c,f("ENG_FA_CUP",3,3,1,"2027-01-09"),0),"FA third club blocked");
        check(!c.transferReason(0,1).isEmpty(),"fourth national registration blocked");
        c=fresh();c.record(f("ENG_LEAGUE_CUP",6,0,1,"2027-01-12"),Collections.singletonMap(0,0));c.register(0,2,LocalDate.of(2027,1,13),-1);
        check(!eligible(c,f("ENG_LEAGUE_CUP",6,2,3,"2027-02-02"),0),"cannot switch League Cup semi-final legs");MatchDiscipline.Fixture finalMatch=f("ENG_LEAGUE_CUP",7,2,3,"2027-03-14");check(eligible(c,finalMatch,0),"winter mover may play League Cup final for new club");c.record(finalMatch,Collections.singletonMap(0,2));
        c=fresh();c.record(f("ENG_FA_CUP",1,0,1,"2026-11-07"),Collections.singletonMap(1,1));c.register(0,2,LocalDate.of(2026,11,8),-1);check(eligible(c,f("ENG_FA_CUP",1,2,3,"2026-11-10"),0),"unused substitute does not acquire a cup appearance");
    }
    private static void scotlandAndDeadlines(){
        CupEligibility c=fresh();c.record(f("SCO_CUP",3,0,1,"2026-11-28"),Collections.singletonMap(0,0));c.register(0,2,LocalDate.of(2027,1,1),-1);
        MatchDiscipline.Fixture fourth=f("SCO_CUP",4,2,3,"2027-01-16");check(!eligible(c,fourth,0),"Scottish Cup one-club limit survives permanent transfer");
        c=fresh();c.register(0,2,LocalDate.of(2027,1,1),1);MatchDiscipline.Fixture parent=f("SCO_CUP",4,2,1,"2027-01-16");check(!eligible(c,parent,0),"Scottish Cup loan cannot face parent club");check(eligible(c,fourth,0),"loan may face another opponent");check(!c.reason(fourth,0,2,15,false,null,false).isEmpty(),"Scottish minimum age enforced");
        c=fresh();c.register(0,2,LocalDate.of(2027,4,24),-1);MatchDiscipline.Fixture finalMatch=f("ENG_FA_CUP",7,2,3,"2027-05-15");
        check(!c.reason(finalMatch,0,2,20,false,LocalDate.of(2027,4,23),false).isEmpty(),"FA final uses semi-final default deadline");
        check(c.reason(finalMatch,0,2,20,false,LocalDate.of(2027,4,24),false).isEmpty(),"date-only registrations before the deadline accepted");
    }
    private static void europeAndGeneralLimits(){
        CupEligibility c=fresh();c.record(f("UEFA_CHAMPIONS",2,0,1,"2026-07-22"),Collections.singletonMap(0,0));c.register(0,2,LocalDate.of(2026,7,23),-1);
        check(!eligible(c,f("UEFA_EUROPA",3,2,3,"2026-08-06"),0),"UEFA competition switch does not bypass qualifying club restriction");MatchDiscipline.Fixture main=f("UEFA_EUROPA",10,2,3,"2026-09-17");check(eligible(c,main,0),"qualifying-only mover may join another club from league phase");c.record(main,Collections.singletonMap(0,2));
        c.register(0,0,LocalDate.of(2027,1,1),-1);MatchDiscipline.Fixture lp=f("UEFA_CHAMPIONS",10,0,3,"2027-01-27"),ko=f("UEFA_CHAMPIONS",11,0,3,"2027-02-16");
        check(!eligible(c,lp,0),"main-phase player cannot switch league-phase club");check(!eligible(c,ko,0),"new knockout club requires saved winter list permission");check(c.reason(ko,0,0,20,false,null,true).isEmpty(),"article 32 registered winter mover admitted");c.record(ko,Collections.singletonMap(0,0),Collections.singleton(0));
        c=fresh();c.record(f("DE_LEAGUE",0,0,1,"2026-08-22"),Collections.singletonMap(0,0));c.register(0,2,LocalDate.of(2026,8,23),-1);c.record(f("DE_POKAL",1,2,3,"2026-08-29"),Collections.singletonMap(0,2));c.register(0,3,LocalDate.of(2026,8,30),-1);check(!eligible(c,f("DE_LEAGUE",0,3,1,"2026-09-12"),0),"two-club appearance limit spans different competitions");
        c=c.nextSeason(2027,LocalDate.of(2027,7,1));check(c.playedClubs(0).isEmpty()&&c.transferReason(0,1).isEmpty(),"annual edition resets appearances and registration club count");
    }
    private static void savesAndAtomicity()throws Exception {
        CupEligibility c=fresh();MatchDiscipline.Fixture first=f("ENG_FA_CUP",1,0,1,"2026-11-07");Map<Integer,Integer> teams=new HashMap<>();teams.put(0,0);teams.put(1,1);c.record(first,teams);
        c.record(f("ENG_LEAGUE",0,0,1,"2026-11-14"),Collections.singletonMap(0,0));c.record(f("ENG_LEAGUE",0,0,1,"2026-11-21"),Collections.singletonMap(0,0));check(c.recentParticipants(1).isEmpty(),"last-two actual sheets replace older participants");
        String text=c.snapshot();CupEligibility loaded=CupEligibility.restore(text);loaded.validate(2026,4,4,LocalDate.of(2026,11,21));loaded.validateTeams(new int[]{0,1,2,3});check(text.equals(loaded.snapshot()),"exact participation reload");
        for(int slot=0;slot<3;slot++){Map<String,Object> fields=new HashMap<>();fields.put("save_"+slot+"_participation_version",1);fields.put("save_"+slot+"_cup_participation",text);check(fields.equals(SaveBackup.decode(SaveBackup.encode(fields))),"strictly typed participation backup in every slot");}
        reject(()->loaded.record(first,teams));check(text.equals(loaded.snapshot()),"duplicate match rejection is atomic");
        reject(()->loaded.validate(2026,4,4,LocalDate.of(2026,11,20)));reject(()->loaded.validateTeams(new int[]{1,1,2,3}));reject(()->CupEligibility.restore(text+"\nJ|0|0|2026-07-01|-1|1|0"));
        Map<Integer,Integer> wrong=new HashMap<>();wrong.put(0,0);wrong.put(1,2);reject(()->loaded.record(f("ENG_LEAGUE",0,0,1,"2026-11-28"),wrong));check(text.equals(loaded.snapshot()),"mixed unregistered match sheet rejected without partial appearances");
        loaded.register(0,2,LocalDate.of(2026,11,22),-1);check(!eligible(loaded,f("ENG_FA_CUP",1,2,3,"2026-11-28"),0),"FA same-round restriction survives reload then transfer");
    }
    public static void main(String[] args)throws Exception {englishCups();scotlandAndDeadlines();europeAndGeneralLimits();savesAndAtomicity();System.out.println("PASS "+checks+" saved cup appearances, transfer identities, loan parents, UEFA phase exceptions, general club limits, deadlines and atomic backups");}
}

package com.projectmister.game;
import java.time.LocalDate;
import java.util.*;

public final class MatchDisciplineTests {
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private static final List<Integer> ROSTER=Arrays.asList(0,1,2,3);
    private static MatchDiscipline.Fixture fixture(String competition,int phase,int number,LocalDate date){return new MatchDiscipline.Fixture(2026,competition,phase,number,0,true,0,1,date);}
    private static MatchDiscipline fresh(){return new MatchDiscipline(2026,LocalDate.of(2026,7,1));}
    private static void played(MatchDiscipline d,MatchDiscipline.Fixture f,int yellow){d.record(f,ROSTER,Arrays.asList(0,1),yellow==0?Collections.emptyMap():Collections.singletonMap(0,yellow),yellow==2?Collections.singleton(0):Collections.emptySet());}
    private static void sitsOut(MatchDiscipline d,MatchDiscipline.Fixture f){d.record(f,ROSTER,Arrays.asList(1,2),Collections.emptyMap(),Collections.emptySet());}
    private static void ordinaryCups(){
        String[] cups={"DE_POKAL","ES_COPA","NL_BEKER","IT_COPPA","ENG_FA_CUP","ENG_LEAGUE_CUP","SCO_CUP","SCO_LEAGUE_CUP","BE_CUP","TR_CUP"};
        int[] limits={5,3,2,2,2,2,2,2,3,4};
        for(int k=0;k<cups.length;k++) {
            MatchDiscipline d=fresh();int phase=cups[k].equals("BE_CUP")?5:cups[k].equals("SCO_LEAGUE_CUP")?2:1;
            for(int n=0;n<limits[k];n++)played(d,fixture(cups[k],phase,n,LocalDate.of(2026,9,1).plusDays(n*7)),1);
            MatchDiscipline.Fixture next=fixture(cups[k],phase,limits[k],LocalDate.of(2026,11,1));
            check(d.suspendedMatches(next,0)==1,cups[k]+" exact first caution threshold");
            String before=d.snapshot();try{played(d,next,0);throw new AssertionError("Suspended appearance accepted");}catch(IllegalArgumentException expected){}check(before.equals(d.snapshot()),"invalid suspended appearance is atomic");
            sitsOut(d,next);check(d.suspendedMatches(fixture(cups[k],phase,limits[k]+1,LocalDate.of(2026,11,8)),0)==0,cups[k]+" ban served by the next eligible team fixture");
            check(d.snapshot().equals(MatchDiscipline.restore(d.snapshot()).snapshot()),cups[k]+" exact reload");
        }
    }
    private static void cyclesAndSeparation(){
        MatchDiscipline d=fresh();LocalDate date=LocalDate.of(2026,8,1);int round=0;
        for(int count=1;count<=6;count++){played(d,fixture("NL_BEKER",1,round++,date),1);date=date.plusDays(7);boolean ban=count==2||count>=4;check((d.suspendedMatches(fixture("NL_BEKER",1,round,date),0)>0)==ban,"Dutch second, fourth and every further caution");if(ban){sitsOut(d,fixture("NL_BEKER",1,round++,date));date=date.plusDays(7);}}
        d=fresh();round=0;date=LocalDate.of(2026,8,1);
        for(int count=1;count<=16;count++){played(d,fixture("PT_LEAGUE",0,round++,date),1);date=date.plusDays(5);boolean ban=count==5||count==9||count==12||count==14||count==16;check((d.suspendedMatches(fixture("PT_LEAGUE",0,round,date),0)>0)==ban,"Portuguese fifth ninth twelfth and subsequent even cautions");if(ban){sitsOut(d,fixture("PT_LEAGUE",0,round++,date));date=date.plusDays(5);}}
        d=fresh();for(int i=0;i<8;i++)played(d,fixture("PT_CUP",i, i,LocalDate.of(2026,9,1).plusDays(i*7)),1);
        check(d.suspendedMatches(fixture("PT_LEAGUE",0,0,LocalDate.of(2026,12,1)),0)==0,"Portuguese cup yellows never enter league accumulation");
        d=fresh();for(int i=0;i<5;i++)played(d,fixture("DE_POKAL",1,i,LocalDate.of(2026,9,1).plusDays(i*7)),1);
        played(d,fixture("UEFA_CHAMPIONS",10,0,LocalDate.of(2026,10,15)),0);
        check(d.suspendedMatches(fixture("DE_POKAL",1,5,LocalDate.of(2026,12,1)),0)==1,"other competition and elapsed weeks do not serve a cup ban");
    }
    private static void europe(){
        MatchDiscipline d=fresh();played(d,fixture("UEFA_CHAMPIONS",1,0,LocalDate.of(2026,7,15)),1);played(d,fixture("UEFA_CHAMPIONS",2,1,LocalDate.of(2026,7,29)),1);
        check(d.cautions(fixture("UEFA_CHAMPIONS",3,2,LocalDate.of(2026,8,12)),0)==1,"one unused caution removed after two eligible qualifying rounds");
        played(d,fixture("UEFA_EUROPA",3,2,LocalDate.of(2026,8,12)),1);played(d,fixture("UEFA_EUROPA",4,3,LocalDate.of(2026,8,26)),1);
        MatchDiscipline.Fixture league=fixture("UEFA_EUROPA",10,4,LocalDate.of(2026,9,15));check(d.suspendedMatches(league,0)==0,"qualifying pending yellow ban expires before league phase");
        d.prepare(league,ROSTER);check(d.cautions(league,0)==0,"qualifying caution count resets");played(d,league,1);
        played(d,fixture("UEFA_EUROPA",10,5,LocalDate.of(2026,10,1)),1);played(d,fixture("UEFA_EUROPA",12,6,LocalDate.of(2027,3,1)),1);
        check(d.suspendedMatches(fixture("UEFA_EUROPA",13,7,LocalDate.of(2027,4,1)),0)==0,"three main-phase cautions are insufficient in 2026/27");
        played(d,fixture("UEFA_EUROPA",13,7,LocalDate.of(2027,4,1)),1);
        MatchDiscipline.Fixture semi=fixture("UEFA_EUROPA",14,8,LocalDate.of(2027,5,1));check(d.suspendedMatches(semi,0)==1,"quarter-final fourth caution ban survives semifinal reset");
        d.prepare(semi,ROSTER);check(d.cautions(semi,0)==0,"quarter-final caution total cleared");sitsOut(d,semi);check(d.suspendedMatches(fixture("UEFA_EUROPA",14,9,LocalDate.of(2027,5,8)),0)==0,"UEFA ban served exactly once");
        d=fresh();played(d,fixture("UEFA_CONFERENCE",4,0,LocalDate.of(2026,8,26)),2);
        check(d.suspendedMatches(league,0)==1,"qualifying sending-off ban survives league-phase reset and competition switch");
        d.prepare(league,ROSTER);check(d.cautions(league,0)==0,"double yellow does not become accumulation cautions");
    }
    private static void stageAndYearResets(){
        MatchDiscipline d=fresh();played(d,fixture("SCO_CUP",2,0,LocalDate.of(2026,10,1)),1);played(d,fixture("SCO_CUP",3,1,LocalDate.of(2026,11,1)),1);
        MatchDiscipline.Fixture fourth=fixture("SCO_CUP",4,2,LocalDate.of(2027,1,16));d.prepare(fourth,ROSTER);check(d.cautions(fourth,0)==0&&d.suspendedMatches(fourth,0)==1,"Scottish round-four count reset preserves already incurred ban");sitsOut(d,fourth);
        played(d,fixture("SCO_CUP",7,3,LocalDate.of(2027,4,17)),1);played(d,fixture("SCO_CUP",8,4,LocalDate.of(2027,5,22)),1);check(d.suspendedMatches(fixture("SCO_CUP",8,5,LocalDate.of(2027,5,25)),0)==0,"Scottish accumulation ceases before semifinal");
        d=fresh();played(d,fixture("BE_CUP",3,0,LocalDate.of(2026,8,1)),1);played(d,fixture("BE_CUP",4,1,LocalDate.of(2026,8,8)),1);MatchDiscipline.Fixture fifth=fixture("BE_CUP",5,2,LocalDate.of(2026,9,1));d.prepare(fifth,ROSTER);check(d.cautions(fifth,0)==0&&d.suspendedMatches(fifth,0)==0,"Belgian fifth-round reset includes pending yellow bans");
        d=fresh();played(d,fixture("DE_POKAL",5,0,LocalDate.of(2027,5,1)),2);MatchDiscipline next=d.nextSeason(2027,LocalDate.of(2027,7,1));check(next.suspendedMatches(new MatchDiscipline.Fixture(2027,"DE_POKAL",1,0,0,true,2,3,LocalDate.of(2027,8,1)),0)==1,"sending-off survives transfer and annual edition");
        d=fresh();played(d,fixture("BE_CUP",5,0,LocalDate.of(2027,5,1)),2);next=d.nextSeason(2027,LocalDate.of(2027,7,1));check(next.suspendedMatches(new MatchDiscipline.Fixture(2027,"BE_CUP",5,0,0,true,2,3,LocalDate.of(2027,8,1)),0)==0,"Belgian unserved second-yellow cup sanction does not carry to next season");
    }
    private static void franceAndValidation(){
        MatchDiscipline d=fresh();for(int i=0;i<5;i++)played(d,fixture(i%2==0?"FR_LEAGUE":"FR_COUPE",1,i,LocalDate.of(2026,9,1).plusDays(i*7)),1);
        MatchDiscipline.Fixture beforeDecision=fixture("FR_LEAGUE",0,5,LocalDate.of(2026,9,30));d.prepare(beforeDecision,ROSTER);check(d.cautions(beforeDecision,0)==0&&d.suspendedMatches(beforeDecision,0)==0,"French commission clears prior cautions before delayed Tuesday ban");played(d,beforeDecision,1);
        MatchDiscipline.Fixture ban=fixture("FR_COUPE",3,6,LocalDate.of(2026,10,6));check(d.suspendedMatches(ban,0)==1,"French five cautions across league and cup activate Tuesday ban");sitsOut(d,ban);check(d.cautions(ban,0)==1,"French cautions incurred after commission decision remain");
        String saved=d.snapshot();check(saved.equals(MatchDiscipline.restore(saved).snapshot()),"saved delayed ban and exact service reload");d.validate(2026,4,LocalDate.of(2026,10,6));
        try{d.validate(2026,4,LocalDate.of(2026,10,5));throw new AssertionError("Future match accepted");}catch(IllegalArgumentException expected){checks++;}
        try{d.validate(2026,1,LocalDate.of(2026,10,6));throw new AssertionError("Unknown roster accepted");}catch(IllegalArgumentException expected){checks++;}
        try{sitsOut(d,ban);throw new AssertionError("Duplicate match accepted");}catch(IllegalStateException expected){checks++;}check(saved.equals(d.snapshot()),"duplicate recording cannot consume another ban");
    }
    private static void repeatedDismissals(){
        for(String cup:new String[]{"ENG_FA_CUP","SCO_CUP"}) {
            MatchDiscipline d=fresh();played(d,fixture(cup,1,0,LocalDate.of(2026,9,1)),2);sitsOut(d,fixture(cup,2,1,LocalDate.of(2026,10,1)));played(d,fixture(cup,3,2,LocalDate.of(2026,11,1)),2);
            MatchDiscipline.Fixture next=fixture(cup,4,3,LocalDate.of(2027,1,1));check(d.suspendedMatches(next,0)==2,"England/Scotland second sending-off adds one match");sitsOut(d,next);check(d.suspendedMatches(fixture(cup,4,4,LocalDate.of(2027,1,8)),0)==1,"two-match sanction requires two eligible fixtures");check(d.snapshot().equals(MatchDiscipline.restore(d.snapshot()).snapshot()),"sending-off recurrence survives reload");
        }
    }
    private static void lastAppearanceAndGlobalExpiry(){
        MatchDiscipline d=fresh();for(int i=0;i<3;i++)played(d,fixture("ES_COPA",1,i,LocalDate.of(2026,10,1).plusDays(i*7)),1);MatchDiscipline.Fixture last=fixture("ES_COPA",1,2,LocalDate.of(2026,10,15));d.lastSpanishCupAppearance(last,ROSTER);
        check(d.suspendedMatches(fixture("ES_COPA",2,3,LocalDate.of(2026,12,1)),0)==0,"Spanish final club appearance does not incur a cycle ban");
        d=fresh();for(int i=0;i<3;i++)played(d,fixture("UEFA_CHAMPIONS",1,i,LocalDate.of(2026,7,1).plusDays(i*7)),1);d.beginEuropeanLeaguePhase();check(d.playerSummary(0).stream().noneMatch(x->x.contains("suspended")||x.contains("caution")),"Qualifying cautions and pending bans expire for eliminated players too");
    }
    private static void backupRoundTrip()throws Exception {
        MatchDiscipline d=fresh();played(d,fixture("DE_POKAL",1,0,LocalDate.of(2026,9,1)),2);
        for(int slot=0;slot<3;slot++) {
            String prefix="save_"+slot+"_";Map<String,Object> fields=new HashMap<>();
            fields.put(prefix+"discipline_version",1);fields.put(prefix+"match_discipline",d.snapshot());
            Map<String,Object> restored=SaveBackup.decode(SaveBackup.encode(fields));
            MatchDiscipline loaded=MatchDiscipline.restore((String)restored.get(prefix+"match_discipline"));
            check(restored.equals(fields)&&loaded.snapshot().equals(d.snapshot()),"actual dismissal ledger survives typed backup in every slot");
            check(loaded.suspendedMatches(fixture("DE_POKAL",2,1,LocalDate.of(2026,10,1)),0)==1,"restored backup preserves pending automatic suspension");
        }
    }
    public static void main(String[] args)throws Exception {ordinaryCups();cyclesAndSeparation();europe();stageAndYearResets();franceAndValidation();repeatedDismissals();lastAppearanceAndGlobalExpiry();backupRoundTrip();System.out.println("PASS "+checks+" disciplinary thresholds, cup separation, stage/season resets, transfer bans, service, delayed French decisions and exact saves/backups");}
}

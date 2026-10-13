package com.projectmister.game;
import java.time.*;
import java.util.*;
public final class LeagueDecisionTests {
    private static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    public static void main(String[] args) {
        for(int size=2;size<=5;size++)for(long seed=0;seed<12;seed++) {
            LeagueDecisions decisions=new LeagueDecisions(2026,20);int[] entrants=new int[size];for(int i=0;i<size;i++)entrants[i]=i;
            decisions.ensure("FINAL1",entrants,false,seed,LocalDate.of(2027,5,12),Arrays.asList(LocalDate.of(2027,5,14),LocalDate.of(2027,5,22)));
            LocalDate previous=LocalDate.of(2027,5,10);while(!decisions.complete()){LeagueDecisions.Event event=decisions.next(0);check(!event.date.isBefore(previous),"Deciding stream chronological");previous=event.date;event.tie.recordRegulation(0,0);if(!event.series){event.tie.recordExtraTime(0,0);event.tie.recordPenalties(5,4);}decisions.resolveSeries();String saved=decisions.snapshot();decisions=LeagueDecisions.restore(saved);check(saved.equals(decisions.snapshot()),"Deciding scores/session restore exactly");}
            Integer[] order=new Integer[size];boolean[] tied=new boolean[size-1];Arrays.fill(tied,true);for(int i=0;i<size;i++)order[i]=i;decisions.apply("FINAL1",order,tied);Set<Integer> unique=new HashSet<>(Arrays.asList(order));check(unique.size()==size,"Resolved order keeps every entrant once");for(boolean value:tied)check(!value,"Resolved places no longer shared");
            if(size>2)check(decisions.shootoutSessions().size()==size-1,"Fully equal series uses saved penalty placement session");
            LeagueDecisions next=decisions.nextSeason(2027);check(next.editions().size()==2&&next.editions().get(0).snapshot().equals(decisions.snapshot()),"Archived edition immutable at rollover");check(next.snapshot().equals(LeagueDecisions.restore(next.snapshot()).snapshot()),"Archive canonical reload");
        }
        LeagueDecisions cards=new LeagueDecisions(2026,3);LeagueResults ledger=new LeagueResults(3,true);ledger.record(0,0,1,0,0);cards.recordDiscipline(0,0,1,2,1,1,0);check(Arrays.equals(cards.disciplinaryPoints(ledger),new int[]{5,1,0}),"French/RFEF recorded yellow/direct-red scoring");cards.recordDiscipline(0,0,1,2,1,1,0);boolean rejected=false;try{cards.recordDiscipline(0,0,1,3,1,1,0);}catch(IllegalStateException expected){rejected=true;}check(rejected,"Conflicting discipline rejected");ledger.record(1,1,2,0,0);check(cards.disciplinaryPoints(ledger)==null,"Missing disciplinary history stays unknown");
        cards.ensure("FINAL1",new int[]{0,1},true,78L,LocalDate.of(2027,5,12),Collections.emptyList());check(cards.complete()&&cards.fixtures().isEmpty()&&cards.lotResults().size()==1,"Saved lot resolves without invented fixtures");
        String saved=cards.snapshot();check(saved.equals(LeagueDecisions.restore(saved).snapshot()),"Saved lot reload exact");
        rejected=false;try{LeagueDecisions.restore(saved.replace("G|FINAL1|1|78|0,1|","G|FINAL1|1|78|0,0|"));}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Duplicate deciding entrant rejected");
        LeagueDecisions indexed=new LeagueDecisions(2026,20);
        indexed.ensure("FINAL1",new int[]{0,1,2},false,1L,LocalDate.of(2027,5,12),Collections.emptyList());
        indexed.ensure("FINAL2",new int[]{3,4},false,2L,LocalDate.of(2027,5,12),Collections.emptyList());
        check(indexed.fixtureIndex(indexed.next(3))==3,"Managed second-group event has its saved global index");
        for(int i=0;i<indexed.fixtures().size();i++)check(indexed.fixtureIndex(indexed.fixtures().get(i))==i,"Independently recreated previews retain saved fixture identity");
        String indexedSave=indexed.snapshot();indexed=LeagueDecisions.restore(indexedSave);
        check(indexed.fixtureIndex(indexed.next(3))==3,"Deciding fixture index survives reload");
        rejected=false;try{indexed.fixtureIndex(cards.next(0));}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Missing deciding event rejected");
        System.out.println("PASS: 48 neutral deciding match/series campaigns, saved shootout sessions, exact archives, discipline and malformed decision rejection");
    }
}

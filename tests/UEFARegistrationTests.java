package com.projectmister.game;
import java.time.*;
import java.util.*;

public final class UEFARegistrationTests {
    private static int checks;
    private static final String EUROPA="UEFA_EUROPA";
    private static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;}
    private static UEFARegistration.Profile p(int id,int club,boolean keeper,int training,boolean b,LocalDate joined,int rating){return new UEFARegistration.Profile(id,club,rating,keeper,training==2,training>=1,b,joined,joined,false);}
    private static ArrayList<UEFARegistration.Profile> roster(boolean trained,int count){ArrayList<UEFARegistration.Profile> out=new ArrayList<>();for(int id=0;id<count;id++)out.add(p(id,0,id<3,trained&&id>=3&&id<7?2:trained&&id>=7&&id<11?1:0,id==2,LocalDate.of(2026,7,1),80-id));return out;}
    private static ArrayList<Integer> a(UEFARegistration.Book b){ArrayList<Integer> ids=new ArrayList<>();for(UEFARegistration.Entry e:b.listA())ids.add(e.id);return ids;}
    private static UEFARegistration.Book prepare(UEFARegistration r,int phase,String first,String match,String now,int round,List<UEFARegistration.Profile> profiles){return r.prepare(0,EUROPA,phase,LocalDate.parse(first),LocalDate.parse(match),LocalDate.parse(now),round,profiles,Collections.emptySet());}
    public static void main(String[] args)throws Exception {
        check(UEFARegistration.deadline(2026,"UEFA_CHAMPIONS",1).equals(LocalDate.of(2026,7,2))&&UEFARegistration.deadline(2026,EUROPA,1).equals(LocalDate.of(2026,7,3)),"Champions League qualifying deadline is one day earlier");
        UEFARegistration lists=new UEFARegistration(2026);ArrayList<UEFARegistration.Profile> squad=roster(false,22);
        UEFARegistration.Book list=prepare(lists,10,"2026-09-17","2026-09-17","2026-09-17",0,squad);
        check(list.listA().size()==17&&list.capacity()==17,"Missing homegrown places reduce List A to seventeen");
        check(list.goalkeepersA()==2&&list.goalkeepersTotal()==3&&list.listB().size()==1,"Two real List A keepers plus a real List B keeper satisfy the minimum");
        String before=lists.snapshot();check(!lists.submit(list,Arrays.asList(0,1,2,3),squad,LocalDate.of(2026,9,3)).isEmpty()&&before.equals(lists.snapshot()),"Closed-list edit rejected atomically");
        lists=new UEFARegistration(2026);squad=roster(true,28);list=prepare(lists,10,"2026-09-17","2026-09-17","2026-08-31",0,squad);
        check(list.capacity()==25&&list.listA().size()==25&&list.clubTrained()==4&&list.associationTrained()==4,"Four club-trained and four association-trained fill all reserved places");
        ArrayList<Integer> tooMany=new ArrayList<>();for(int i=0;i<28;i++)if(i!=2)tooMany.add(i);before=lists.snapshot();check(!lists.submit(list,tooMany,squad,LocalDate.of(2026,8,31)).isEmpty()&&before.equals(lists.snapshot()),"More than twenty-five entries cannot be submitted");
        lists=new UEFARegistration(2026);squad=roster(true,21);list=prepare(lists,10,"2026-09-17","2026-09-17","2026-09-17",0,squad);
        squad.add(p(30,0,false,0,false,LocalDate.of(2026,10,15),99));list=prepare(lists,10,"2026-10-22","2026-10-22","2026-10-22",2,squad);
        check(!list.contains(30)&&!lists.reason(0,EUROPA,10,30).isEmpty(),"October free agent is excluded from the frozen league-phase list");
        check(!list.contains(21),"Unknown player cannot enter a submitted list");
        before=lists.snapshot();lists=UEFARegistration.restore(before);lists.validate(2026,100,4,LocalDate.of(2026,10,22));check(before.equals(lists.snapshot()),"Frozen lists and keeper quotas reload exactly");
        lists=new UEFARegistration(2026);squad=roster(true,21);for(int id=30;id<33;id++)squad.add(p(id,0,false,0,false,LocalDate.of(2026,7,6),100-id));
        list=prepare(lists,1,"2026-07-09","2026-07-09","2026-07-08",0,squad);check(list.lateAdditions()==2&&list.contains(30)&&list.contains(31)&&!list.contains(32),"Qualifying permits exactly two additions by the day before the first leg");
        ArrayList<Integer> three=a(list);three.add(32);before=lists.snapshot();check(!lists.submit(list,three,squad,LocalDate.of(2026,7,8)).isEmpty()&&before.equals(lists.snapshot()),"Third late qualifying registration rejected without consuming counters");
        list=prepare(lists,1,"2026-07-09","2026-07-09","2026-07-09",0,squad);squad.add(p(33,0,false,0,false,LocalDate.of(2026,7,10),100));list=prepare(lists,1,"2026-07-09","2026-07-16","2026-07-16",0,squad);check(!list.contains(33),"Signing between qualifying legs cannot bypass the first-leg deadline");
        lists=new UEFARegistration(2026);squad=roster(true,21);prepare(lists,10,"2026-09-17","2026-09-17","2026-09-17",0,squad);for(int id=30;id<34;id++)squad.add(p(id,0,false,0,false,LocalDate.of(2027,1,1),100-id));
        list=prepare(lists,11,"2027-02-18","2027-02-18","2027-02-04",0,squad);check(list.winterAdditions()==3&&list.winterRegistered(30)&&!list.contains(33),"Knockout list admits three actual new players and excludes the fourth");
        ArrayList<Integer> four=a(list);four.add(33);before=lists.snapshot();check(!lists.submit(list,four,squad,LocalDate.of(2027,2,4)).isEmpty()&&before.equals(lists.snapshot()),"Fourth winter registration rejected atomically");
        ArrayList<UEFARegistration.Profile> later=new ArrayList<>(squad);later.add(p(34,0,false,0,false,LocalDate.of(2027,2,5),100));list=prepare(lists,12,"2027-03-11","2027-03-11","2027-03-11",0,later);check(!list.contains(34),"Later knockout rounds retain the closed February list");
        before=lists.snapshot();lists=UEFARegistration.restore(before);lists.validate(2026,100,4,LocalDate.of(2027,3,11));check(before.equals(lists.snapshot()),"Winter addition counters survive saves");
        UEFARegistration next=lists.nextSeason(2027);check(next.books().isEmpty()&&next.archives().get(0).equals(before),"Annual player-list archive is exact and next-year lists are empty");check(next.snapshot().equals(UEFARegistration.restore(next.snapshot()).snapshot()),"Annual archive reload is canonical");
        lists=new UEFARegistration(2026);squad=roster(true,21);prepare(lists,10,"2026-09-17","2026-09-17","2026-09-17",0,squad);squad.remove(0);squad.add(p(50,0,true,0,false,LocalDate.of(2027,1,14),60));
        list=lists.prepare(0,EUROPA,10,LocalDate.of(2027,1,21),LocalDate.of(2027,1,21),LocalDate.of(2027,1,21),6,squad,Collections.singleton(0));check(list.listAContains(50)&&!list.listAContains(0)&&list.goalkeepersA()==2,"Permanent goalkeeper departure can be replaced after matchday six");
        list=prepare(lists,11,"2027-02-18","2027-02-18","2027-02-04",0,squad);check(list.winterRegistered(50)&&list.winterAdditions()==1,"League-phase replacement goalkeeper consumes one winter place");
        lists=new UEFARegistration(2026);squad=roster(true,21);prepare(lists,10,"2026-09-17","2026-09-17","2026-09-17",0,squad);squad.remove(10);squad.add(p(10,0,false,0,false,LocalDate.of(2027,1,1),80));list=prepare(lists,11,"2027-02-18","2027-02-18","2027-02-04",0,squad);check(list.winterRegistered(10),"Returning player with a new registration is a winter addition even if formerly listed");
        ArrayList<UEFARegistration.Profile> twoKeepers=roster(true,21);twoKeepers.remove(2);UEFARegistration incomplete=new UEFARegistration(2026);prepare(incomplete,10,"2026-09-17","2026-09-17","2026-09-17",0,twoKeepers);check(!incomplete.reason(0,EUROPA,10,0).isEmpty(),"No phantom third goalkeeper makes an incomplete list playable");
        Map<String,Object> backup=new HashMap<>();for(int slot=0;slot<3;slot++){backup.put("save_"+slot+"_uefa_registration_version",1);backup.put("save_"+slot+"_uefa_player_lists",lists.snapshot());}check(backup.equals(SaveBackup.decode(SaveBackup.encode(backup))),"Every slot backs up typed UEFA player lists");
        lists=new UEFARegistration(2026);squad=roster(true,21);prepare(lists,10,"2026-09-17","2026-09-17","2026-09-17",0,squad);squad.add(p(30,0,false,0,false,LocalDate.of(2026,10,15),99));
        MatchDiscipline discipline=new MatchDiscipline(2026,LocalDate.of(2026,7,1));MatchDiscipline.Fixture oldClub=new MatchDiscipline.Fixture(2026,EUROPA,10,0,0,false,1,2,LocalDate.of(2026,9,17));discipline.record(oldClub,Arrays.asList(30,90),Arrays.asList(30,90),Collections.singletonMap(30,2),Collections.singleton(30));
        MatchDiscipline.Fixture leagueMatch=new MatchDiscipline.Fixture(2026,EUROPA,10,2,0,false,0,2,LocalDate.of(2026,10,22));list=prepare(lists,10,"2026-09-17","2026-10-22","2026-10-22",2,squad);List<Integer> eligible=a(list);for(UEFARegistration.Entry entry:list.listB())eligible.add(entry.id);List<Integer> appeared=Arrays.asList(0,3,4,5,6,7,8,9,10,11,12);discipline.record(leagueMatch,eligible,appeared,Collections.emptyMap(),Collections.emptySet());check(discipline.suspendedMatches(leagueMatch,30)==1,"Transferred player absent from A/B cannot serve a UEFA ban through the new club's match");
        list=prepare(lists,11,"2027-02-18","2027-02-18","2027-02-18",0,squad);check(list.winterRegistered(30),"Former other-club UEFA participant receives bounded winter registration");eligible=a(list);for(UEFARegistration.Entry entry:list.listB())eligible.add(entry.id);MatchDiscipline.Fixture knockoutMatch=new MatchDiscipline.Fixture(2026,EUROPA,11,0,0,false,0,2,LocalDate.of(2027,2,18));discipline.record(knockoutMatch,eligible,appeared,Collections.emptyMap(),Collections.emptySet());check(discipline.suspendedMatches(knockoutMatch,30)==0,"Actual A/B registration permits eligible knockout ban service");
        boolean wrongKeeper=false;try{UEFARegistration.restore(lists.snapshot().replace("A|3|0|","A|3|1|")).validateGoalkeepers(new HashSet<>(Arrays.asList(0,1,2)));}catch(IllegalArgumentException expected){wrongKeeper=true;}check(wrongKeeper,"Forged goalkeeper flag cannot turn an outfield player into a registered keeper");
        System.out.println("PASS "+checks+" UEFA A/B quotas, real goalkeeper minimums, frozen deadlines, qualifying additions, winter limits, returning players, outgoing keepers and exact annual backups");
    }
}

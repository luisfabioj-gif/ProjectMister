package com.projectmister.game;

import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public final class NationalCupTests {
    static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);}
    static final class Registry {
        final String[] ids;final int[] tiers,levels;final boolean[] reserves;
        Registry(String country)throws Exception {
            List<String> rows=Files.readAllLines(Paths.get("tests/fixtures/national-cup-world.csv"));rows.remove(0);
            ids=new String[rows.size()];tiers=new int[ids.length];levels=new int[ids.length];reserves=new boolean[ids.length];
            for(int i=0;i<rows.size();i++){String[] v=rows.get(i).split(",");ids[i]=v[3];tiers[i]=v[0].equals(country)?Integer.parseInt(v[1]):4;levels[i]=Integer.parseInt(v[2]);reserves[i]=v[4].equals("1");}
        }
        List<SeasonCup> create(String country,int year,long seed,int[] order,int[] exemptions){return NationalCupFactory.create(country,year,seed,ids,tiers,levels,reserves,order,exemptions);}
    }
    static void settle(KnockoutTie tie) {
        if(tie.phase()==KnockoutTie.Phase.REGULATION)tie.recordRegulation(0,0);
        if(tie.phase()==KnockoutTie.Phase.EXTRA_TIME)tie.recordExtraTime(0,0);
        if(tie.phase()==KnockoutTie.Phase.PENALTIES)tie.recordPenalties(4,3);
    }
    static void checkRoundRule(SeasonCup cup,KnockoutTie tie) {
        SeasonCup.Round r=cup.round(cup.stage(cup.currentIndex()));
        Map<Integer,SeasonCup.Entrant> entries=new HashMap<>();for(SeasonCup.Entrant e:cup.entrants())entries.put(e.club,e);
        int h=entries.get(tie.firstHome).level,a=entries.get(tie.firstAway).level;
        if(r.draw==SeasonCup.Draw.LOWER_CATEGORY)check(h>=a,"Spanish inferior category hosts");
        if(r.draw==SeasonCup.Draw.FRENCH_HOME&&!r.neutral)check(a-h<2,"French two-level home switch");
        if(r.draw==SeasonCup.Draw.BRACKET_HOME) {
            int hi=-1,ai=-1;for(int i=0;i<cup.entrants().size();i++){int club=cup.entrants().get(i).club;if(club==tie.firstHome)hi=i;if(club==tie.firstAway)ai=i;}
            boolean publishedException=cup.season==2026&&cup.stage(cup.currentIndex())==1&&hi==24&&ai==16;
            check(hi<ai||publishedException,"Italian seeded home rights and published Palermo exception");
        }
    }
    public static void main(String[] ignored)throws Exception {
        int editions=0,matches=0;
        for(String country:new String[]{"ENG","ES","IT","FR","NL","BE","SCO","TR"}) {
            Registry world=new Registry(country);NationalCupCampaign campaign=new NationalCupCampaign(world.create(country,2026,1900,null,null));
            int competitionCount=country.equals("ENG")?2:1;check(campaign.competitions().size()==competitionCount,"country cup registry");
            for(int year=2026;year<2030;year++) {
                LocalDate last=LocalDate.of(year,7,1);int events=0;
                while(!campaign.complete()) {
                    SeasonCup cup=campaign.active();LocalDate date=cup.nextDate();check(!date.isBefore(last),"two cup event order never rewinds");last=date;
                    int index=cup.currentIndex();String competition=cup.competition;KnockoutTie tie=cup.current();checkRoundRule(cup,tie);
                    for(SeasonCup.Entrant e:cup.entrants())check(world.ids[e.club].startsWith(country.toLowerCase()+":")&&!world.reserves[e.club],"foreign and reserve clubs excluded");
                    // Save the unresolved phase as well as the settled match, including return legs.
                    tie.recordRegulation(0,0);if(tie.phase()==KnockoutTie.Phase.EXTRA_TIME)tie.recordExtraTime(0,0);
                    String pending=campaign.snapshot();campaign=NationalCupCampaign.restore(pending);check(pending.equals(campaign.snapshot()),"pending shootout/return leg retained");
                    KnockoutTie pendingTie=campaign.competition(competition).at(index);
                    if(pendingTie.phase()!=KnockoutTie.Phase.REGULATION&&pendingTie.phase()!=KnockoutTie.Phase.COMPLETE)settle(pendingTie);
                    campaign.validateDate(date);events++;matches++;
                    campaign=NationalCupCampaign.restore(campaign.snapshot());
                }
                for(CupSeasons history:campaign.competitions()) {
                    SeasonCup cup=history.active();NationalCupFormats.validateStructure(cup);editions++;
                    check(cup.winner()>=0&&cup.drawnCount()==cup.entrants().size()-1,"every entrant resolves into exactly one champion");
                    for(int i=0;i<cup.drawnCount();i++)check(cup.at(i).winner()>=0,"no orphaned tie");
                }
                check(events>0,"annual competition plays fixtures");
                int[] prior=new int[world.ids.length];for(int i=0;i<prior.length;i++)prior[i]=i;
                List<String> old=new ArrayList<>(campaign.archives());
                campaign=campaign.next(world.create(country,year+1,1901+year,prior,null));
                check(!campaign.complete()&&campaign.archives().size()==(year-2025)*competitionCount,"annual draw and immutable archives");
                check(campaign.archives().containsAll(old),"older winners preserved");
            }
            List<LocalDate> league=CompetitionCalendar.cupSeason(LocalDate.of(2030,8,9),46,campaign.calendar());
            for(LocalDate date:league)for(LocalDate cup:campaign.calendar())check(Math.abs(java.time.temporal.ChronoUnit.DAYS.between(date,cup))>=3,"national cups reserve rest windows");
            System.out.println("PASS: "+country+" recurring cups, four seasons, live-result phases, canonical saves, archives and calendar");
        }
        Registry english=new Registry("ENG");List<Integer> prem=new ArrayList<>();for(int i=0;i<english.tiers.length;i++)if(english.tiers[i]==1)prem.add(i);
        for(int count:new int[]{7,8,9,10}) {
            int[] euro=new int[count];for(int i=0;i<count;i++)euro[i]=prem.get(i);
            List<SeasonCup> cups=english.create("ENG",2027,94,null,euro);SeasonCup cup=cups.get(1);
            check(cup.entrants().size()==92,"EFL field stays all 92 eligible clubs across UEFA exemption changes");
            check(cup.roundCount()==(count>8?8:7),"preliminary changes with European places");
            while(!cup.complete())settle(cup.current());check(cup.drawnCount()==91,"EFL changing entry gates produce complete trophy");
        }
        List<SeasonCup.Round> german=Arrays.asList(new SeasonCup.Round("Final",true,KnockoutTie.Rule.EXTRA_TIME_PENALTIES,SeasonCup.Draw.OPEN,LocalDate.of(2027,5,29)));
        CupSeasons legacy=new CupSeasons(new SeasonCup("DE_POKAL",2026,2,true,german,Arrays.asList(new SeasonCup.Entrant(0,0,false,false),new SeasonCup.Entrant(1,0,true,false))));
        check(NationalCupCampaign.restore(legacy.snapshot()).active().snapshot().equals(legacy.active().snapshot()),"legacy CS1 cup saves migrate without remapping");
        boolean rejected=false;try{NationalCupCampaign.restore("NC1\nbroken");}catch(IllegalArgumentException e){rejected=true;}check(rejected,"damaged campaign rejected");
        System.out.println("PASS: "+editions+" verified national cup engine editions, "+matches+" dated match events; European exemption parity and legacy save migration");
    }
}

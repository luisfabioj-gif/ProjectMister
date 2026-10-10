package com.projectmister.game;

import java.io.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Annual career integration: qualifiers, changed fields, deterministic dates and exact archives. */
public final class RecurringEuropeanTests {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void settle(KnockoutTie tie){tie.recordRegulation(0,0);if(tie.phase()==KnockoutTie.Phase.EXTRA_TIME)tie.recordExtraTime(0,0);if(tie.phase()==KnockoutTie.Phase.PENALTIES)tie.recordPenalties(5,4);}
    public static void main(String[] args)throws Exception {
        CompetitionCatalog catalog=CompetitionCatalog.read(new FileInputStream("app/src/main/assets/competitions/2026-27.json"));EuropeanCatalog europe=EuropeanCatalog.read(new FileInputStream("app/src/main/assets/competitions/europe-2026.json"));
        CareerDivision found=null;for(CompetitionCatalog.Division division:catalog.divisions)if(division.countryCode.equals("PT")&&division.tier==1)found=CareerDivision.countryCareer(catalog,division).withWorldClubs(catalog,null,europe.clubs());final CareerDivision world=found;
        int[] local=Arrays.stream(world.members(1)).filter(c->!world.reserves[c]).toArray(),levels=new int[world.names.length],coefficients=new int[world.names.length];for(int club=0;club<levels.length;club++){levels[club]=world.level(club);coefficients[club]=world.strengths[club]*1000+levels.length-club;}
        List<LocalDate> domestic=Arrays.asList(LocalDate.of(2027,1,27),LocalDate.of(2027,3,17),LocalDate.of(2026,9,16));EuropeanCampaign campaign=europe.first(world,411,domestic);String firstArchive="";int events=0,qualifiers=0;
        for(int year=2026;year<=2028;year++){
            Set<LocalDate> planned=new HashSet<>(campaign.calendarForClub(local[0]));LocalDate last=LocalDate.of(year,7,1);int current=0;
            while(!campaign.complete()){
                EuropeanCampaign.Event event=campaign.next(local[0]);check(event!=null&&!event.date.isBefore(last),"Annual UEFA stream remains chronological");last=event.date;
                for(LocalDate cup:domestic)check(Math.abs(ChronoUnit.DAYS.between(cup,event.date))>=2,"UEFA avoids domestic ties and preserves recovery");
                if(event.league())campaign.recordLeague(event,0,0,0,0);else settle(event.tie);
                if(event.qualifying!=null)qualifiers++;events++;current++;
                campaign.validate(world.clubIds,world.reserves,year,last);
                if(current%149==0){String saved=campaign.snapshot();campaign=EuropeanCampaign.restore(saved);check(saved.equals(campaign.snapshot()),"Recurring campaign restores canonically during each phase");check(planned.equals(new HashSet<>(campaign.calendarForClub(local[0]))),"Qualification cannot move league reservations");}
            }
            Map<String,Integer> performance=EuropeanPerformance.associationPoints(campaign,world.clubIds,europe.admittedCounts());check(performance.size()==54,"Full admitted denominators include eliminated associations");
            if(campaign.qualifying()!=null){int[] phase=EuropeanPerformance.points(campaign,world.names.length),qual=campaign.qualifying().associationMatchPoints();int sum=0,count=0;for(EuropeanAdmissions.Entry e:campaign.qualifying().admissions().entries())if(world.association(e.club).equals("PT")){sum+=phase[e.club]+qual[e.club];count++;}check(performance.get("PT")==sum/count,"Qualifier points and eliminated clubs affect EPS denominator exactly once");}
            String completed=campaign.season(EuropeanLeaguePhase.Competition.CHAMPIONS).snapshot();if(year==2026)firstArchive=completed;
            EuropeanDomesticSeason result=new EuropeanDomesticSeason(year,world.clubIds,world.reserves,levels,world.strengths,"PT",local,local[0],-1,year*17L);
            EuropeanAdmissions admissions=new EuropeanAdmissions(result,world.clubIds,world.reserves,coefficients,campaign.season(EuropeanLeaguePhase.Competition.CHAMPIONS).winner(),campaign.season(EuropeanLeaguePhase.Competition.EUROPA).winner(),campaign.season(EuropeanLeaguePhase.Competition.CONFERENCE).winner(),new String[]{"ENG","ES"});
            ArrayList<LocalDate> nextDates=new ArrayList<>();for(LocalDate date:domestic)nextDates.add(date.plusYears(1));domestic=nextDates;
            campaign=campaign.next(admissions,year*19L,domestic);String saved=campaign.snapshot();campaign=EuropeanCampaign.restore(saved);check(saved.equals(campaign.snapshot()),"New annual qualifier reload preserves previous fields");
            check(campaign.competitions().get(0).active().snapshot().equals(completed),"Last completed edition remains visible before the next draw");
            if(year>2026)check(campaign.competitions().get(0).archives().get(0).equals(firstArchive),"Original edition archive never changes");
            check(campaign.qualifyingArchives().size()==Math.max(0,year-2026),"Qualifying archives grow once per completed edition");
        }
        check(events>2000&&qualifiers>500,"Two annual qualifying campaigns and three complete UEFA editions played");
        System.out.println("PASS: three annual UEFA editions, "+events+" events, "+qualifiers+" qualifiers, rescheduled dates, full EPS denominators, canonical saves and exact league/qualifying archives");
    }
}

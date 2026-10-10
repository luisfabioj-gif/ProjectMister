package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

public final class EuropeanCampaignTests {
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static void rejects(Runnable r){try{r.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Invalid UEFA campaign accepted");}
    private static List<EuropeanSeason> fresh(int year,EuropeanCampaign previous) {
        ArrayList<EuropeanSeason> out=new ArrayList<>();
        for(EuropeanLeaguePhase.Competition competition:EuropeanLeaguePhase.Competition.values()) {
            ArrayList<EuropeanLeaguePhase.Club> clubs=new ArrayList<>();
            for(int i=0;i<36;i++)clubs.add(new EuropeanLeaguePhase.Club(competition.ordinal()*36+i,""+(char)('A'+i/26)+(char)('A'+i%26),i/(competition==EuropeanLeaguePhase.Competition.CONFERENCE?6:9),100000-i*500));
            EuropeanLeaguePhase phase=EuropeanDraw.create(competition,year,clubs,EuropeanCalendar.league(competition,year),year*17L+competition.ordinal(),previous==null?null:previous.season(competition).leaguePhase());
            out.add(new EuropeanSeason(phase,year,EuropeanCalendar.knockout(competition,year),true));
        }return out;
    }
    public static void main(String[] args) {
        String[] ids=new String[108];boolean[] reserves=new boolean[108];for(int i=0;i<108;i++)ids[i]=(""+(char)('A'+i%36/26)+(char)('A'+i%36%26)).toLowerCase(Locale.ROOT)+":club-"+i;
        EuropeanCampaign campaign=new EuropeanCampaign(fresh(2026,null));String originalArchive="";
        for(int year=2026;year<2029;year++) {
            final EuropeanCampaign unfinished=campaign;rejects(()->unfinished.next(fresh(yearOf(unfinished)+1,unfinished)));
            LocalDate last=LocalDate.of(year,7,1);int events=0,managed=0;String before=campaign.snapshot();
            while(!campaign.complete()) {
                EuropeanCampaign.Event event=campaign.next(0);check(!event.date.isBefore(last),"Chronological concurrent competitions");last=event.date;
                if(event.contains(0))managed++;
                if(event.league())campaign.recordLeague(event,0,0,0,0);
                else {event.tie.recordRegulation(0,0);if(event.tie.phase()==KnockoutTie.Phase.EXTRA_TIME)event.tie.recordExtraTime(0,0);if(event.tie.phase()==KnockoutTie.Phase.PENALTIES)event.tie.recordPenalties(5,4);}
                events++;campaign.validate(ids,reserves,year,last);
                if(events%83==0){String saved=campaign.snapshot();campaign=EuropeanCampaign.restore(saved);check(saved.equals(campaign.snapshot()),"Canonical mid-season restoration");}
            }
            check(events==531&&managed>=8,"All 531 fixtures and actual managed participation");check(!before.equals(campaign.snapshot()),"Results are saved");
            for(EuropeanLeaguePhase.Competition c:EuropeanLeaguePhase.Competition.values())check(campaign.season(c).winner()>=0,"Each competition produces its own winner");
            if(year==2026)originalArchive=campaign.season(EuropeanLeaguePhase.Competition.CHAMPIONS).snapshot();
            final EuropeanCampaign completed=campaign;String[] changed=ids.clone();changed[0]="zz:wrong";rejects(()->completed.validate(changed,reserves,yearOf(completed),LocalDate.of(yearOf(completed)+1,6,30)));
            campaign=campaign.next(fresh(year+1,campaign));campaign=EuropeanCampaign.restore(campaign.snapshot());
            check(campaign.competitions().get(0).archives().get(0).equals(originalArchive),"Original edition archive remains exact");
        }
        check(campaign.year==2029&&campaign.next(0).date.getYear()==2029,"Fourth edition has fresh dates and results");
        final EuropeanCampaign state=campaign;rejects(()->state.validate(ids,reserves,2028,LocalDate.of(2029,7,1)));rejects(()->EuropeanCampaign.restore(state.snapshot()+"\n"));
        List<EuropeanSeason> duplicate=fresh(2026,null);duplicate.set(1,duplicate.get(0));rejects(()->new EuropeanCampaign(duplicate));
        System.out.println("PASS: three concurrent UEFA careers, 1,593 chronological match events, watched-result inputs, exact archives, fresh seasons, canonical reloads and registry/year/corruption rejection");
    }
    private static int yearOf(EuropeanCampaign campaign){return campaign.year;}
}

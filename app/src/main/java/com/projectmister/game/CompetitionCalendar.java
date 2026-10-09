package com.projectmister.game;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Simulation scheduling around cup windows; these are not official league fixture dates. */
public final class CompetitionCalendar {
    private static final String[] CUP_DATES={"2026-08-30","2026-09-20","2026-10-18","2026-10-27","2026-10-28","2026-10-29",
        "2026-11-22","2026-12-16","2027-01-05","2027-01-06","2027-01-09","2027-02-03","2027-05-22","2027-05-23","2027-05-30"};
    public static List<LocalDate> portugueseCupSeason(LocalDate start,int rounds) {
        ArrayList<LocalDate> cups=new ArrayList<>();for(String cup:CUP_DATES)cups.add(LocalDate.parse(cup).plusYears(start.getYear()-2026));
        return cupSeason(start,rounds,cups);
    }
    public static List<LocalDate> cupSeason(LocalDate start,int rounds,List<LocalDate> cups) {
        if(start==null||start.getYear()<2026||start.getYear()>2200||rounds<1||rounds>50||cups==null||cups.contains(null))throw new IllegalArgumentException("Unsupported cup calendar");
        ArrayList<LocalDate> dates=new ArrayList<>();
        for(int round=0;round<rounds;round++) {
            LocalDate target=start.plusWeeks(round),chosen=null;
            for(int offset:new int[]{0,-1,1,-2,2,-3,3,-4,4}) {
                LocalDate candidate=target.plusDays(offset);
                if(candidate.isBefore(start)||!dates.isEmpty()&&ChronoUnit.DAYS.between(dates.get(dates.size()-1),candidate)<3)continue;
                boolean safe=true;for(LocalDate cup:cups)if(Math.abs(ChronoUnit.DAYS.between(candidate,cup))<3){safe=false;break;}
                if(safe){chosen=candidate;break;}
            }
            // Keep established dates when possible. Consecutive cup/rank-decision windows
            // can block the entire nine-day search above, especially at season opening.
            // Postpone the league fixture until rest is available instead of aborting rollover.
            if(chosen==null) {
                LocalDate candidate=target.plusDays(5),limit=target.plusDays(35);
                if(!dates.isEmpty()&&candidate.isBefore(dates.get(dates.size()-1).plusDays(3)))candidate=dates.get(dates.size()-1).plusDays(3);
                for(;!candidate.isAfter(limit);candidate=candidate.plusDays(1)) {
                    boolean safe=true;for(LocalDate cup:cups)if(Math.abs(ChronoUnit.DAYS.between(candidate,cup))<3){safe=false;break;}
                    if(safe){chosen=candidate;break;}
                }
            }
            if(chosen==null)throw new IllegalStateException("No league date with three days of recovery within the scheduling window");dates.add(chosen);
        }
        return Collections.unmodifiableList(dates);
    }
    private CompetitionCalendar(){}
}

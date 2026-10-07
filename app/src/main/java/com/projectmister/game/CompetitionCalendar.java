package com.projectmister.game;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Simulation scheduling around cup windows; these are not official league fixture dates. */
public final class CompetitionCalendar {
    private static final String[] CUP_DATES={"2026-08-30","2026-09-20","2026-10-18","2026-10-27","2026-10-28","2026-10-29",
        "2026-11-22","2026-12-16","2027-01-05","2027-01-06","2027-01-09","2027-02-03","2027-05-22","2027-05-23","2027-05-30"};
    public static List<LocalDate> portugueseCupSeason(LocalDate start,int rounds) {
        if(start.getYear()!=2026||rounds<1||rounds>50)throw new IllegalArgumentException("Unsupported cup calendar");
        ArrayList<LocalDate> dates=new ArrayList<>();
        for(int round=0;round<rounds;round++) {
            LocalDate target=start.plusWeeks(round),chosen=null;
            for(int offset:new int[]{0,-1,1,-2,2,-3,3,-4,4}) {
                LocalDate candidate=target.plusDays(offset);
                if(candidate.isBefore(start)||!dates.isEmpty()&&ChronoUnit.DAYS.between(dates.get(dates.size()-1),candidate)<3)continue;
                boolean safe=true;for(String cup:CUP_DATES)if(Math.abs(ChronoUnit.DAYS.between(candidate,LocalDate.parse(cup)))<3){safe=false;break;}
                if(safe){chosen=candidate;break;}
            }
            if(chosen==null)throw new IllegalStateException("No safe league date");dates.add(chosen);
        }
        return Collections.unmodifiableList(dates);
    }
    private CompetitionCalendar(){}
}

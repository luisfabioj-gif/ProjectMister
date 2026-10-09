package com.projectmister.game;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class CompetitionCalendarTests {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args) {
        for(int year=2027;year<=2050;year++) {
            ArrayList<LocalDate> cups=new ArrayList<>(PortugueseLeagueCupCareer.dates(year));
            cups.add(LocalDate.of(year,8,11)); // Saved league-phase sporting rank decision.
            for(String md:new String[]{"08-30","09-20","10-18","11-22","12-16","02-03","05-22","05-23","05-30"})
                cups.add(LocalDate.parse((md.compareTo("07-01")>=0?year:year+1)+"-"+md));
            for(int rounds:new int[]{34,38,46}) {
                LocalDate start=LocalDate.of(year,8,9);
                List<LocalDate> dates=CompetitionCalendar.cupSeason(start,rounds,cups);
                check(dates.size()==rounds&&dates.get(0).equals(LocalDate.of(year,8,14)),"crowded opening postpones league until recovery is available");
                for(int i=0;i<dates.size();i++) {
                    check(!dates.get(i).isBefore(start),"league does not start before its season");
                    if(i>0)check(ChronoUnit.DAYS.between(dates.get(i-1),dates.get(i))>=3,"league progression and recovery");
                    for(LocalDate cup:cups)check(Math.abs(ChronoUnit.DAYS.between(dates.get(i),cup))>=3,"both cups and rank decisions retain recovery windows");
                }
                Collections.reverse(cups);
                check(dates.equals(CompetitionCalendar.cupSeason(start,rounds,cups)),"schedule is stable across reload and input order");
            }
        }
        ArrayList<LocalDate> impossible=new ArrayList<>();
        for(int day=0;day<40;day++)impossible.add(LocalDate.of(2027,8,9).plusDays(day));
        try {CompetitionCalendar.cupSeason(LocalDate.of(2027,8,9),34,impossible);throw new AssertionError("impossible rest schedule accepted");}
        catch(IllegalStateException expected){}
        System.out.println("PASS: 72 crowded recurring Portuguese calendars, recovery, deterministic reload and impossible-window rejection");
    }
}

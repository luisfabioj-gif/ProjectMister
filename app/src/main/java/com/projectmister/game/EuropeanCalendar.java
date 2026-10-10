package com.projectmister.game;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Published opening windows with explicit career reschedules and projected later editions. */
public final class EuropeanCalendar {
    private static final String[] CL={"2026-09-08","2026-10-13","2026-10-20","2026-11-03","2026-11-24","2026-12-08","2027-01-19","2027-01-27"};
    private static final String[] EL={"2026-09-17","2026-10-15","2026-10-22","2026-11-05","2026-11-26","2026-12-10","2027-01-21","2027-01-28"};
    private static final String[] CO={"2026-10-15","2026-10-22","2026-11-05","2026-11-26","2026-12-10","2026-12-17"};
    private static final String[] CL_KO={"2027-02-16","2027-02-23","2027-03-09","2027-03-16","2027-04-06","2027-04-13","2027-04-27","2027-05-04","2027-06-05"};
    private static final String[] EL_KO={"2027-02-18","2027-02-25","2027-03-11","2027-03-18","2027-04-08","2027-04-15","2027-04-29","2027-05-06","2027-05-26"};
    private static final String[] CO_KO={"2027-02-18","2027-02-25","2027-03-11","2027-03-18","2027-04-08","2027-04-15","2027-04-29","2027-05-06","2027-06-02"};
    private static List<LocalDate> dates(String[] source,int year) {
        if(year<2026||year>2200)throw new IllegalArgumentException("Unsupported UEFA calendar");
        ArrayList<LocalDate> out=new ArrayList<>();for(String d:source)out.add(LocalDate.parse(d).plusYears(year-2026));return Collections.unmodifiableList(out);
    }
    public static List<LocalDate> league(EuropeanLeaguePhase.Competition competition,int year){return dates(competition==EuropeanLeaguePhase.Competition.CHAMPIONS?CL:competition==EuropeanLeaguePhase.Competition.EUROPA?EL:CO,year);}
    public static List<LocalDate> knockout(EuropeanLeaguePhase.Competition competition,int year){return dates(competition==EuropeanLeaguePhase.Competition.CHAMPIONS?CL_KO:competition==EuropeanLeaguePhase.Competition.EUROPA?EL_KO:CO_KO,year);}
    public static List<LocalDate> all(EuropeanLeaguePhase.Competition competition,int year){ArrayList<LocalDate> out=new ArrayList<>(league(competition,year));out.addAll(knockout(competition,year));return Collections.unmodifiableList(out);}
    public static List<LocalDate> reserved(int year,Collection<LocalDate> source){
        if(source==null||source.size()>200)throw new IllegalArgumentException("Invalid domestic calendar");TreeSet<LocalDate> dates=new TreeSet<>();
        for(LocalDate date:source){if(date==null||date.isBefore(LocalDate.of(year,7,1))||date.isAfter(LocalDate.of(year+1,6,30)))throw new IllegalArgumentException("Domestic date outside UEFA edition");dates.add(date);}return Collections.unmodifiableList(new ArrayList<>(dates));
    }
    private static boolean safe(LocalDate date,List<LocalDate> occupied){for(LocalDate other:occupied)if(Math.abs(ChronoUnit.DAYS.between(date,other))<2)return false;return true;}
    private static LocalDate resolve(LocalDate target,LocalDate earliest,List<LocalDate> occupied){
        for(int distance=0;distance<=21;distance++)for(int direction:new int[]{1,-1}){LocalDate date=target.plusDays(distance*direction);if((earliest==null||!date.isBefore(earliest))&&safe(date,occupied))return date;}throw new IllegalStateException("No UEFA date with required recovery");
    }
    /** One immutable date per matchday, chosen before any results are recorded. */
    public static List<LocalDate> career(EuropeanLeaguePhase.Competition competition,int year,List<LocalDate> domestic){
        List<LocalDate> occupied=reserved(year,domestic);ArrayList<LocalDate> result=new ArrayList<>();LocalDate previous=LocalDate.of(year,8,31);
        for(LocalDate target:all(competition,year)){LocalDate date=resolve(target,previous.plusDays(2),occupied);if(date.isAfter(LocalDate.of(year+1,6,30)))throw new IllegalStateException("UEFA season exceeds June");result.add(date);previous=date;}return Collections.unmodifiableList(result);
    }
    /** CL and EL/Conference date for each of eight qualifying legs, in transfer-safe bands. */
    public static List<LocalDate> qualifying(int year,List<LocalDate> domestic){
        List<LocalDate> occupied=reserved(year,domestic);ArrayList<LocalDate> result=new ArrayList<>();LocalDate previous=LocalDate.of(year,7,1);
        for(int leg=0;leg<8;leg++){LocalDate latest=previous;for(int competition=0;competition<2;competition++){LocalDate target=LocalDate.of(year,7,7).plusDays(leg*7L+competition*2L);LocalDate date=resolve(target,previous.plusDays(2),occupied);result.add(date);if(date.isAfter(latest))latest=date;}previous=latest;}
        if(previous.isAfter(LocalDate.of(year,8,31)))throw new IllegalStateException("Qualifying exceeds August");return Collections.unmodifiableList(result);
    }
    private EuropeanCalendar(){}
}

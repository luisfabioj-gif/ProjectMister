package com.projectmister.game;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Fictional world youth histories plus actual career club-registration periods.
 * These are simulated player attributes, not claims about real footballers. */
public final class PlayerTrainingHistory {
    private static final class Period {
        int club,wholeSeasons,loanParent=-1;
        String association;
        LocalDate start,end;
    }
    private static final class Record {LocalDate birth;final ArrayList<Period> periods=new ArrayList<>();}
    private final TreeMap<Integer,Record> players=new TreeMap<>();
    private final TreeMap<String,LocalDate[]> creditedSeasons=new TreeMap<>();
    private String cachedSnapshot;
    private static void id(int id){if(id<0||id>=CareerLimits.MAX_CLUBS*64)throw new IllegalArgumentException("Invalid youth-history player");}
    private static void association(String value){if(value==null||!value.matches("[A-Z]{2,3}"))throw new IllegalArgumentException("Invalid training association");}
    public void seed(int id,LocalDate birth,int club,String association,LocalDate registeredFrom,int fictionalWholeSeasons) {
        id(id);if(players.containsKey(id))return;association(association);
        if(birth==null||birth.isBefore(LocalDate.of(1900,1,1))||birth.isAfter(LocalDate.of(2200,1,1))||club<0||club>=CareerLimits.MAX_CLUBS||registeredFrom==null||registeredFrom.isBefore(birth)||fictionalWholeSeasons<0||fictionalWholeSeasons>3)throw new IllegalArgumentException("Invalid generated youth history");
        Record record=new Record();record.birth=birth;Period p=new Period();p.club=club;p.association=association;p.start=registeredFrom;p.wholeSeasons=fictionalWholeSeasons;record.periods.add(p);players.put(id,record);cachedSnapshot=null;
    }
    public boolean contains(int id){return players.containsKey(id);}
    public LocalDate birth(int id){Record r=players.get(id);if(r==null)throw new IllegalArgumentException("Unknown youth-history player");return r.birth;}
    public LocalDate continuousSince(int id,int club){Record r=players.get(id);if(r==null)return null;Period p=r.periods.get(r.periods.size()-1);return p.end==null&&p.club==club?p.start:null;}
    public void register(int id,int club,String association,LocalDate date) {register(id,club,association,date,-1);}
    public void register(int id,int club,String association,LocalDate date,int loanParent) {
        id(id);association(association);Record r=players.get(id);if(r==null||date==null||club<0||club>=CareerLimits.MAX_CLUBS)throw new IllegalArgumentException("Unknown training registration");Period old=r.periods.get(r.periods.size()-1);
        if(date.isBefore(old.start)||old.end!=null||loanParent<-1||loanParent>=CareerLimits.MAX_CLUBS||loanParent==club)throw new IllegalArgumentException("Training registration moved backwards");if(old.club==club)return;
        old.end=date;Period next=new Period();next.club=club;next.association=association;next.start=date;next.loanParent=loanParent;r.periods.add(next);cachedSnapshot=null;
    }
    private boolean trained(int id,int club,String association,LocalDate date) {
        Record r=players.get(id);if(r==null||date==null)return false;double months=0;int seasons=0;
        for(Period p:r.periods)if(club>=0?p.club==club:p.association.equals(association)) {
            LocalDate from=p.start.isAfter(r.birth.plusYears(15))?p.start:r.birth.plusYears(15),to=p.end==null||p.end.isAfter(date)?date:p.end;if(to.isAfter(r.birth.plusYears(21)))to=r.birth.plusYears(21);
            if(to.isAfter(from)){long full=ChronoUnit.MONTHS.between(from,to);if(!from.plusMonths(full+1).isAfter(to))full++;LocalDate remainder=from.plusMonths(full);months+=full+ChronoUnit.DAYS.between(remainder,to)/(double)remainder.lengthOfMonth();}int whole=p.wholeSeasons;for(Map.Entry<String,LocalDate[]> credit:creditedSeasons.entrySet()){LocalDate[] credited=credit.getValue();if(credit.getKey().startsWith(p.club+":")&&credited[1].isAfter(date)&&!p.start.isAfter(credited[0])&&(p.end==null||!p.end.isBefore(credited[1]))&&!r.birth.plusYears(15).isAfter(LocalDate.of(credited[0].getYear()+1,6,30))&&!r.birth.plusYears(21).isBefore(LocalDate.of(credited[0].getYear(),7,1)))whole--;}seasons+=Math.max(0,whole);
        }
        return seasons>=3||months>=36-1e-9;
    }
    public boolean clubTrained(int id,int club,LocalDate date){return trained(id,club,null,date);}
    public boolean associationTrained(int id,String association,LocalDate date){association(association);return trained(id,-1,association,date);}
    public boolean listB(int id,int club,int year,LocalDate date) {
        Record r=players.get(id);LocalDate from=continuousSince(id,club);if(r==null||date==null||from==null||r.birth.isBefore(LocalDate.of(year-21,1,1)))return false;
        int age=java.time.Period.between(r.birth,date).getYears();
        if(age==16&&!date.isBefore(from.plusYears(2)))return true;
        LocalDate afterFifteen=from.isBefore(r.birth.plusYears(15))?r.birth.plusYears(15):from;
        if(age>=17&&!date.isBefore(afterFifteen.plusYears(2)))return true;
        LocalDate start=date.minusYears(3);if(start.isBefore(r.birth.plusYears(15)))return false;
        int loans=0;String home=r.periods.get(r.periods.size()-1).association;
        for(Period p:r.periods) {
            LocalDate end=p.end==null?date:p.end;if(!end.isAfter(start)||!p.start.isBefore(date))continue;
            if(p.club==club)continue;
            if(p.loanParent!=club||!p.association.equals(home)||p.end==null||p.end.isAfter(p.start.plusYears(1))||++loans>1)return false;
        }
        return loans==1&&!r.periods.get(0).start.isAfter(start);
    }
    /** Credit only an actual simulated first-team season whose full boundaries are known. */
    public void completeSeason(int club,LocalDate first,LocalDate last) {
        if(first==null||last==null||last.isBefore(first))throw new IllegalArgumentException("Invalid training season");
        if(club<0||club>=CareerLimits.MAX_CLUBS||first.getYear()<2026||last.isAfter(LocalDate.of(first.getYear()+1,7,31)))throw new IllegalArgumentException("Invalid training season boundaries");
        String key=club+":"+first.getYear();LocalDate[] old=creditedSeasons.get(key);if(old!=null){if(!old[0].equals(first)||!old[1].equals(last))throw new IllegalArgumentException("Changed credited training calendar");return;}
        creditedSeasons.put(key,new LocalDate[]{first,last});
        cachedSnapshot=null;
        for(Record r:players.values())for(Period p:r.periods)if(p.club==club&&!p.start.isAfter(first)&&(p.end==null||!p.end.isBefore(last))&&!r.birth.plusYears(15).isAfter(LocalDate.of(first.getYear()+1,6,30))&&!r.birth.plusYears(21).isBefore(LocalDate.of(first.getYear(),7,1)))p.wholeSeasons=Math.min(100,p.wholeSeasons+1);
    }
    public String snapshot(){if(cachedSnapshot!=null)return cachedSnapshot;StringBuilder out=new StringBuilder("TH1");for(Map.Entry<Integer,Record> e:players.entrySet()){out.append("\nP|").append(e.getKey()).append('|').append(e.getValue().birth);for(Period p:e.getValue().periods)out.append("\nT|").append(e.getKey()).append('|').append(p.club).append('|').append(p.association).append('|').append(p.start).append('|').append(p.end==null?"-":p.end).append('|').append(p.wholeSeasons).append('|').append(p.loanParent);}for(Map.Entry<String,LocalDate[]> e:creditedSeasons.entrySet())out.append("\nS|").append(e.getKey()).append('|').append(e.getValue()[0]).append('|').append(e.getValue()[1]);return cachedSnapshot=out.toString();}
    public static PlayerTrainingHistory restore(String text) {
        if(text==null||text.length()>24*1024*1024)throw new IllegalArgumentException("Oversized youth history");String[] rows=text.split("\n",-1);if(!rows[0].equals("TH1")||rows.length>500000)throw new IllegalArgumentException("Invalid youth-history schema");PlayerTrainingHistory out=new PlayerTrainingHistory();
        for(int i=1;i<rows.length;i++){String[] p=rows[i].split("\\|",-1);if(p[0].equals("P")&&p.length==3){int id=Integer.parseInt(p[1]);id(id);Record r=new Record();r.birth=LocalDate.parse(p[2]);if(out.players.put(id,r)!=null)throw new IllegalArgumentException("Duplicate player birth");}else if(p[0].equals("T")&&p.length==8){Record r=out.players.get(Integer.parseInt(p[1]));if(r==null)throw new IllegalArgumentException("Training row precedes birth");Period t=new Period();t.club=Integer.parseInt(p[2]);t.association=p[3];association(t.association);t.start=LocalDate.parse(p[4]);t.end=p[5].equals("-")?null:LocalDate.parse(p[5]);t.wholeSeasons=Integer.parseInt(p[6]);t.loanParent=Integer.parseInt(p[7]);if(t.club<0||t.club>=CareerLimits.MAX_CLUBS||t.loanParent<-1||t.loanParent>=CareerLimits.MAX_CLUBS||t.loanParent==t.club||t.start.isBefore(r.birth)||t.end!=null&&t.end.isBefore(t.start)||t.wholeSeasons<0||t.wholeSeasons>100||r.periods.size()>=400)throw new IllegalArgumentException("Invalid training period");if(!r.periods.isEmpty()){Period previous=r.periods.get(r.periods.size()-1);if(previous.end==null||!previous.end.equals(t.start))throw new IllegalArgumentException("Training periods overlap or omit a registration transition");}r.periods.add(t);}else if(p[0].equals("S")&&p.length==4){String[] key=p[1].split(":",-1);if(key.length!=2)throw new IllegalArgumentException("Invalid training season key");int club=Integer.parseInt(key[0]),year=Integer.parseInt(key[1]);LocalDate first=LocalDate.parse(p[2]),last=LocalDate.parse(p[3]);if(club<0||club>=CareerLimits.MAX_CLUBS||year<2026||year>2200||first.getYear()!=year||last.isBefore(first)||last.isAfter(LocalDate.of(year+1,7,31))||out.creditedSeasons.put(p[1],new LocalDate[]{first,last})!=null)throw new IllegalArgumentException("Invalid credited training season");}else throw new IllegalArgumentException("Invalid youth-history row");}
        for(Record r:out.players.values())if(r.periods.isEmpty()||r.periods.get(r.periods.size()-1).end!=null)throw new IllegalArgumentException("Missing current training club");if(!text.equals(out.snapshot()))throw new IllegalArgumentException("Noncanonical youth history");return out;
    }
    public void validate(int[] teams,LocalDate current) {
        if(teams==null||current==null||players.size()!=teams.length)throw new IllegalArgumentException("Incomplete youth-history roster");for(int id=0;id<teams.length;id++){Record r=players.get(id);if(r==null||r.birth.isAfter(current)||r.birth.isBefore(LocalDate.of(1900,1,1)))throw new IllegalArgumentException("Unknown player birth");Period p=r.periods.get(r.periods.size()-1);if(p.club!=teams[id]||p.start.isAfter(current))throw new IllegalArgumentException("Training club disagrees with saved registration");for(Period t:r.periods)if(t.end!=null&&t.end.isAfter(current))throw new IllegalArgumentException("Future training transition");}for(LocalDate[] dates:creditedSeasons.values())if(dates[1].isAfter(current))throw new IllegalArgumentException("Future training-season credit");
    }
}

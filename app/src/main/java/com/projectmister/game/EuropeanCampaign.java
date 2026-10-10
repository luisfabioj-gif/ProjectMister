package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/** Three concurrent UEFA editions with one chronological career event stream. */
public final class EuropeanCampaign {
    public static final class Event {
        public final EuropeanLeaguePhase.Competition competition;
        public final LocalDate date;
        public final int home,away,fixture;
        public final KnockoutTie tie;public final EuropeanQualifying.Event qualifying;
        private Event(EuropeanLeaguePhase.Competition competition,LocalDate date,int home,int away,int fixture,KnockoutTie tie){this(competition,date,home,away,fixture,tie,null);}
        private Event(EuropeanLeaguePhase.Competition competition,LocalDate date,int home,int away,int fixture,KnockoutTie tie,EuropeanQualifying.Event qualifying){this.competition=competition;this.date=date;this.home=home;this.away=away;this.fixture=fixture;this.tie=tie;this.qualifying=qualifying;}
        public boolean league(){return fixture>=0;}
        public boolean contains(int club){return home==club||away==club;}
    }
    private final EnumMap<EuropeanLeaguePhase.Competition,EuropeanSeasons> competitions=new EnumMap<>(EuropeanLeaguePhase.Competition.class);
    private EuropeanQualifying qualifying;
    private final List<String> qualifyingArchives=new ArrayList<>();
    private String previous="";
    private long drawSeed;
    private boolean materialized=true;
    public final int year;
    public EuropeanCampaign(List<EuropeanSeason> seasons) {
        this(wrap(seasons),true);
    }
    private static List<EuropeanSeasons> wrap(List<EuropeanSeason> seasons){if(seasons==null)throw new IllegalArgumentException("Missing UEFA seasons");ArrayList<EuropeanSeasons> out=new ArrayList<>();for(EuropeanSeason s:seasons)out.add(new EuropeanSeasons(s));return out;}
    private EuropeanCampaign(List<EuropeanSeasons> seasons,boolean unused) {
        if(seasons==null||seasons.size()!=3)throw new IllegalArgumentException("Three UEFA seasons required");int edition=-1;Set<Integer> field=new HashSet<>();
        for(EuropeanSeasons s:seasons){if(s==null)throw new IllegalArgumentException("Missing UEFA edition");EuropeanLeaguePhase phase=s.active().leaguePhase();
            if(competitions.put(phase.competition,s)!=null||edition>=0&&edition!=phase.season)throw new IllegalArgumentException("Conflicting UEFA editions");edition=phase.season;
            for(EuropeanLeaguePhase.Club c:phase.clubs())if(!field.add(c.id))throw new IllegalArgumentException("Club in multiple UEFA league phases");
        }year=edition;
    }
    private EuropeanCampaign(EuropeanCampaign previous,EuropeanAdmissions admissions,long seed,List<String> archives,List<LocalDate> domestic){
        if(previous==null||!previous.complete()||admissions==null||admissions.year!=previous.year+1)throw new IllegalArgumentException("Complete UEFA year and annual admissions required");year=admissions.year;competitions.putAll(previous.competitions);this.previous=previous.legacySnapshot();qualifying=new EuropeanQualifying(admissions,seed,domestic);drawSeed=seed;materialized=false;qualifyingArchives.addAll(archives);
    }
    public EuropeanCampaign next(EuropeanAdmissions admissions,long seed){return next(admissions,seed,Collections.emptyList());}
    public EuropeanCampaign next(EuropeanAdmissions admissions,long seed,List<LocalDate> domestic){ArrayList<String> history=new ArrayList<>(qualifyingArchives);if(qualifying!=null)history.add(qualifying.snapshot());return new EuropeanCampaign(this,admissions,seed,history,domestic);}
    public EuropeanQualifying qualifying(){return qualifying;}
    public List<String> qualifyingArchives(){return Collections.unmodifiableList(qualifyingArchives);}
    public boolean leaguePhasesReady(){materialize();return materialized;}
    public List<EuropeanSeasons> competitions(){materialize();return Collections.unmodifiableList(new ArrayList<>(competitions.values()));}
    public EuropeanSeason season(EuropeanLeaguePhase.Competition competition){materialize();if(!materialized)throw new IllegalStateException("Finish qualifying before the league-phase draw");return competitions.get(competition).active();}
    private void materialize(){
        if(materialized||qualifying==null||!qualifying.complete())return;ArrayList<EuropeanSeason> seasons=new ArrayList<>();EuropeanAdmissions a=qualifying.admissions();
        for(EuropeanLeaguePhase.Competition c:EuropeanLeaguePhase.Competition.values()) {
            ArrayList<Integer> field=new ArrayList<>(qualifying.field(c));int holder=c==EuropeanLeaguePhase.Competition.CHAMPIONS?competitions.get(c).active().winner():c==EuropeanLeaguePhase.Competition.EUROPA?competitions.get(EuropeanLeaguePhase.Competition.CONFERENCE).active().winner():-1;
            field.sort((x,y)->x.equals(y)?0:x==holder?-1:y==holder?1:Integer.compare(a.coefficients[y],a.coefficients[x]));ArrayList<EuropeanLeaguePhase.Club> clubs=new ArrayList<>();for(int i=0;i<field.size();i++){int club=field.get(i);clubs.add(new EuropeanLeaguePhase.Club(club,EuropeanDomesticSeason.association(a.identities[club]),i/(c==EuropeanLeaguePhase.Competition.CONFERENCE?6:9),a.coefficients[club]));}
            List<LocalDate> calendar=EuropeanCalendar.career(c,year,qualifying.domesticCalendar());int rounds=c==EuropeanLeaguePhase.Competition.CONFERENCE?6:8;
            EuropeanLeaguePhase phase=EuropeanDraw.create(c,year,clubs,calendar.subList(0,rounds),drawSeed+c.ordinal()*7919L,competitions.get(c).active().leaguePhase());seasons.add(new EuropeanSeason(phase,drawSeed^0x31ff80L,calendar.subList(rounds,calendar.size()),true));
        }
        for(EuropeanSeason season:seasons){EuropeanLeaguePhase.Competition c=season.leaguePhase().competition;competitions.put(c,competitions.get(c).next(season));}materialized=true;
    }
    public static String name(EuropeanLeaguePhase.Competition competition){return competition==EuropeanLeaguePhase.Competition.CHAMPIONS?"Champions League":competition==EuropeanLeaguePhase.Competition.EUROPA?"Europa League":"Conference League";}
    public boolean complete(){materialize();if(!materialized)return false;for(EuropeanSeasons s:competitions.values())if(!s.active().complete())return false;return true;}
    /** Prioritize the managed fixture inside a simultaneous matchday, without skipping earlier dates. */
    public Event next(int managedClub) {
        if(qualifying!=null&&!qualifying.complete()){EuropeanQualifying.Event e=qualifying.next(managedClub);return e==null?null:new Event(e.competition(),e.date,e.tie.home(),e.tie.away(),-2,e.tie,e);}
        materialize();
        Event next=null;
        for(EuropeanSeasons editions:competitions.values()) {
            EuropeanSeason season=editions.active();EuropeanLeaguePhase phase=season.leaguePhase();
            if(!phase.complete()) {
                int round=phase.currentRound();
                for(int i=0;i<phase.fixtureCount();i++){EuropeanLeaguePhase.Fixture f=phase.fixture(i);if(f.round==round&&phase.result(i)==null){Event event=new Event(phase.competition,phase.date(round),f.home,f.away,i,null);next=earlier(next,event,managedClub);}}
            }else {
                EuropeanKnockout cup=season.knockout();KnockoutTie tie=cup.current();if(tie!=null)next=earlier(next,new Event(phase.competition,cup.nextDate(),tie.home(),tie.away(),-1,tie),managedClub);
            }
        }return next;
    }
    private static Event earlier(Event current,Event candidate,int managedClub){return current==null||candidate.date.isBefore(current.date)||candidate.date.equals(current.date)&&candidate.contains(managedClub)&&!current.contains(managedClub)?candidate:current;}
    public String round(Event event){if(event.qualifying!=null)return EuropeanQualifying.roundName(event.qualifying.draw.round)+" · "+EuropeanQualifying.pathName(event.qualifying.draw.path)+" · leg "+(event.tie.playedLegs()+1);if(event.league())return "League phase · matchday "+(season(event.competition).leaguePhase().fixture(event.fixture).round+1);EuropeanKnockout cup=season(event.competition).knockout();return EuropeanKnockout.roundName(EuropeanKnockout.stageFor(cup.currentIndex()))+(event.tie.legs==2?" · leg "+(event.tie.playedLegs()+1):"");}
    public void recordLeague(Event event,int home,int away,int homeCards,int awayCards){if(event==null||!event.league())throw new IllegalArgumentException("Missing UEFA league fixture");season(event.competition).leaguePhase().record(event.fixture,home,away,homeCards,awayCards);}
    public EuropeanLeaguePhase.Competition competitionForClub(int club){if(!leaguePhasesReady()){int c=qualifying.competitionForClub(club);return c<0?null:EuropeanLeaguePhase.Competition.values()[c];}for(EuropeanSeasons s:competitions.values())for(EuropeanLeaguePhase.Club c:s.active().leaguePhase().clubs())if(c.id==club)return s.active().leaguePhase().competition;return null;}
    /** Reserve every potential date for an admitted club so qualification and elimination cannot move league fixtures. */
    public List<LocalDate> calendarForClub(int club){if(qualifying!=null){if(qualifying.admissions().entry(club)==null)return Collections.emptyList();ArrayList<LocalDate> dates=new ArrayList<>(qualifying.calendar());for(EuropeanLeaguePhase.Competition c:EuropeanLeaguePhase.Competition.values())dates.addAll(EuropeanCalendar.career(c,year,qualifying.domesticCalendar()));return Collections.unmodifiableList(dates);}EuropeanLeaguePhase.Competition c=competitionForClub(club);return c==null?Collections.emptyList():season(c).calendar();}
    public EuropeanCampaign next(List<EuropeanSeason> next) {
        if(!complete()||next==null||next.size()!=3)throw new IllegalStateException("Complete all UEFA editions before rollover");ArrayList<EuropeanSeasons> editions=new ArrayList<>();
        for(EuropeanSeason s:next){if(s==null)throw new IllegalArgumentException("Missing next UEFA season");editions.add(competitions.get(s.leaguePhase().competition).next(s));}
        return new EuropeanCampaign(editions,true);
    }
    public void validate(String[] identities,boolean[] reserves,int year,LocalDate date){if(this.year!=year||identities==null||reserves==null||identities.length!=reserves.length)throw new IllegalArgumentException("Wrong UEFA career year or registry");
        if(qualifying!=null){qualifying.validate(identities,reserves,year,date);for(String history:qualifyingArchives){EuropeanQualifying saved=EuropeanQualifying.restore(history);if(!saved.complete()||saved.admissions().year>=year)throw new IllegalArgumentException("Invalid UEFA qualifying archive");saved.validate(identities,reserves,saved.admissions().year,LocalDate.of(saved.admissions().year,8,31));}}
        materialize();
        for(EuropeanSeasons editions:competitions.values()){editions.active().validateDate(date);validateEdition(editions.active(),identities,reserves);for(String saved:editions.archives())validateEdition(EuropeanSeason.restore(saved),identities,reserves);}
    }
    private static void validateEdition(EuropeanSeason edition,String[] ids,boolean[] reserves){for(EuropeanLeaguePhase.Club c:edition.leaguePhase().clubs())if(c.id>=ids.length||reserves[c.id]||!ids[c.id].startsWith(c.association.toLowerCase(Locale.ROOT)+":"))throw new IllegalArgumentException("Changed UEFA club identity");}
    private static String encode(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    private String legacySnapshot(){StringBuilder out=new StringBuilder("UC1");for(EuropeanSeasons s:competitions.values())out.append('\n').append(encode(s.snapshot()));return out.toString();}
    public String snapshot(){materialize();if(qualifying==null)return legacySnapshot();StringBuilder out=new StringBuilder("UC2|").append(year).append('|').append(drawSeed).append('\n').append(encode(qualifying.snapshot())).append('\n').append(encode(previous)).append('\n').append(materialized?encode(legacySnapshot()):"-");for(String history:qualifyingArchives)out.append('\n').append(encode(history));return out.toString();}
    private static String decode(String value){return new String(Base64.getDecoder().decode(value),StandardCharsets.UTF_8);}
    public static EuropeanCampaign restore(String text){
        if(text==null||text.length()>96*1024*1024)throw new IllegalArgumentException("Oversized UEFA career");String[] rows=text.split("\n",-1);
        if(rows[0].startsWith("UC2|")){
            String[] h=rows[0].split("\\|",-1);if(h.length!=3||rows.length<4||rows.length>104)throw new IllegalArgumentException("Invalid recurring UEFA career");EuropeanCampaign previous=restore(decode(rows[2]));if(previous.qualifying!=null)throw new IllegalArgumentException("Nested UEFA career metadata");EuropeanQualifying q=EuropeanQualifying.restore(decode(rows[1]));ArrayList<String> history=new ArrayList<>();int previousYear=-1;for(int i=4;i<rows.length;i++){EuropeanQualifying old=EuropeanQualifying.restore(decode(rows[i]));if(!old.complete()||old.admissions().year>=q.admissions().year||previousYear>=0&&old.admissions().year!=previousYear+1)throw new IllegalArgumentException("Invalid qualifying archive sequence");previousYear=old.admissions().year;history.add(old.snapshot());}
            EuropeanCampaign out=new EuropeanCampaign(previous,q.admissions(),Long.parseLong(h[2]),history,q.domesticCalendar());out.qualifying=q;if(out.year!=Integer.parseInt(h[1]))throw new IllegalArgumentException("Wrong UEFA edition");
            if(!rows[3].equals("-")){if(!q.complete())throw new IllegalArgumentException("League phase before qualifying completed");out.materialize();EuropeanCampaign active=restore(decode(rows[3]));if(active.qualifying!=null||active.year!=out.year)throw new IllegalArgumentException("Invalid active UEFA field");for(EuropeanLeaguePhase.Competition c:EuropeanLeaguePhase.Competition.values()){EuropeanSeasons expected=out.competitions.get(c),saved=active.competitions.get(c);String phase=saved.active().leaguePhase().snapshot().split("\\nS\\|",2)[0];if(!phase.equals(expected.active().leaguePhase().snapshot())||!expected.archives().equals(saved.archives()))throw new IllegalArgumentException("UEFA field or archives conflict with qualifying");}out.competitions.clear();out.competitions.putAll(active.competitions);}
            if(!text.equals(out.snapshot()))throw new IllegalArgumentException("Non-canonical recurring UEFA career");return out;
        }
        if(rows.length!=4||!rows[0].equals("UC1"))throw new IllegalArgumentException("Invalid UEFA career");ArrayList<EuropeanSeasons> seasons=new ArrayList<>();for(int i=1;i<4;i++)seasons.add(EuropeanSeasons.restore(decode(rows[i])));EuropeanCampaign out=new EuropeanCampaign(seasons,true);if(!text.equals(out.snapshot()))throw new IllegalArgumentException("Non-canonical UEFA career");return out;
    }
}

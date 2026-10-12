package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** Saved national registration and actual first-team appearances. Unused benches
 * never cup-tie a player. Upgrades start a new ledger without inventing old games. */
public final class CupEligibility {
    private static final int MAX_PLAYERS=CareerLimits.MAX_CLUBS*64;
    private static final class Registration {
        int club,parent;
        LocalDate joined;
        boolean baseline;
        final TreeSet<Integer> registeredClubs=new TreeSet<>();
    }
    private static final class Sheet {
        final String key;
        final LocalDate date;
        final TreeSet<Integer> players;
        Sheet(String key,LocalDate date,Collection<Integer> ids){this.key=key;this.date=date;players=new TreeSet<>(ids);}
    }
    public final int year;
    public final LocalDate recordedFrom;
    private final TreeMap<Integer,Registration> registrations=new TreeMap<>();
    private final TreeMap<Integer,TreeMap<String,TreeMap<Integer,TreeSet<Integer>>>> appearances=new TreeMap<>();
    private final TreeMap<Integer,ArrayList<Sheet>> recent=new TreeMap<>();
    private final TreeMap<String,LocalDate> recorded=new TreeMap<>();
    public CupEligibility(int year,LocalDate from) {
        if(year<2026||year>2200||from==null||from.isBefore(LocalDate.of(year,7,1))||from.isAfter(LocalDate.of(year+1,7,31)))throw new IllegalArgumentException("Invalid participation edition");
        this.year=year;recordedFrom=from;
    }
    private static void identity(int id,int club){if(id<0||id>=MAX_PLAYERS||club<0||club>=CareerLimits.MAX_CLUBS)throw new IllegalArgumentException("Invalid registration identity");}
    private void fixture(MatchDiscipline.Fixture f){if(f==null||f.year!=year||f.date.isBefore(recordedFrom))throw new IllegalArgumentException("Wrong participation fixture");}
    /** The existing roster is registered, but its unrecorded former appearances are unknown. */
    public void seed(int id,int club) {
        identity(id,club);if(registrations.containsKey(id))return;
        Registration r=new Registration();r.club=club;r.parent=-1;r.joined=recordedFrom;r.baseline=true;r.registeredClubs.add(club);registrations.put(id,r);
    }
    public String transferReason(int id,int destination) {
        identity(id,destination);Registration r=registrations.get(id);
        return r!=null&&!r.registeredClubs.contains(destination)&&r.registeredClubs.size()>=3?"Already registered with three clubs this season":"";
    }
    public void register(int id,int club,LocalDate date,int loanParent) {
        identity(id,club);if(date==null||date.isBefore(recordedFrom)||date.isAfter(LocalDate.of(year+1,7,31))||loanParent< -1||loanParent>=CareerLimits.MAX_CLUBS||loanParent==club)throw new IllegalArgumentException("Invalid registration change");
        String reason=transferReason(id,club);if(!reason.isEmpty())throw new IllegalArgumentException(reason);
        Registration r=registrations.get(id);if(r!=null&&date.isBefore(r.joined))throw new IllegalArgumentException("Registration moved backwards");
        if(r==null){r=new Registration();registrations.put(id,r);}r.club=club;r.parent=loanParent;r.joined=date;r.baseline=false;r.registeredClubs.add(club);
    }
    private static String scope(MatchDiscipline.Fixture f){return f.european()?"UEFA":f.competition;}
    private TreeMap<Integer,TreeSet<Integer>> played(int id,String competition) {
        TreeMap<String,TreeMap<Integer,TreeSet<Integer>>> all=appearances.get(id);
        return all==null?new TreeMap<>():all.getOrDefault(competition,new TreeMap<>());
    }
    public Set<Integer> playedClubs(int id) {
        TreeSet<Integer> clubs=new TreeSet<>();TreeMap<String,TreeMap<Integer,TreeSet<Integer>>> all=appearances.get(id);
        if(all!=null)for(TreeMap<Integer,TreeSet<Integer>> competition:all.values())clubs.addAll(competition.keySet());return Collections.unmodifiableSet(clubs);
    }
    /** Deadline is the default-round deadline, not a postponed match's actual date.
     * UEFA winter permission must come from the saved, bounded registration list. */
    public String reason(MatchDiscipline.Fixture f,int id,int club,int age,boolean goalkeeper,LocalDate deadline,boolean uefaWinterRegistered) {
        fixture(f);Registration r=registrations.get(id);
        if(r==null||r.club!=club)return "Not registered with this club";
        if(!r.baseline&&r.joined.isAfter(f.date))return "Registration takes effect after this match";
        if(deadline!=null&&!r.baseline&&r.joined.isAfter(deadline))return "Registered after this round's deadline ("+deadline+")";
        Set<Integer> allClubs=playedClubs(id);if(!allClubs.contains(club)&&allClubs.size()>=2)return "Already played official first-team matches for two clubs this season";
        TreeMap<Integer,TreeSet<Integer>> cups=played(id,scope(f));
        if(f.competition.equals("ENG_FA_CUP")) {
            if(!cups.containsKey(club)&&cups.size()>=2)return "Already played for two clubs in this FA Cup edition";
            for(Map.Entry<Integer,TreeSet<Integer>> e:cups.entrySet())if(e.getKey()!=club&&(f.phase==0||e.getValue().contains(f.phase)))return "Already represented another club in this FA Cup round";
        } else if(f.competition.equals("ENG_LEAGUE_CUP")) {
            if(!cups.containsKey(club)&&cups.size()>=2)return "Already played for two clubs in this League Cup edition";
            if(f.phase==6)for(Map.Entry<Integer,TreeSet<Integer>> e:cups.entrySet())if(e.getKey()!=club&&e.getValue().contains(6))return "Already represented another club in this League Cup semi-final";
        } else if(f.competition.equals("SCO_CUP")) {
            if(age<16)return "Scottish Cup players must be at least 16";
            if(r.parent==f.home||r.parent==f.away)return "A loan player cannot face his parent club in the Scottish Cup";
            for(int team:cups.keySet())if(team!=club)return "Already played for another club in this Scottish Cup edition";
        } else if(f.european()) {
            for(Map.Entry<Integer,TreeSet<Integer>> e:cups.entrySet())if(e.getKey()!=club) {
                boolean oldMain=e.getValue().stream().anyMatch(p->p>=MatchDiscipline.UEFA_LEAGUE);
                boolean oldKnockout=e.getValue().stream().anyMatch(p->p>=MatchDiscipline.UEFA_PLAYOFF);
                if(f.phase<MatchDiscipline.UEFA_LEAGUE)return "Already played UEFA qualifying for another club this season";
                if(oldMain&&(f.phase<MatchDiscipline.UEFA_PLAYOFF||!uefaWinterRegistered)||oldKnockout)return "A change after UEFA league-phase appearances needs the permitted winter registration";
            }
        }
        return "";
    }
    public Set<Integer> recentParticipants(int club) {
        TreeSet<Integer> ids=new TreeSet<>();for(Sheet s:recent.getOrDefault(club,new ArrayList<>()))ids.addAll(s.players);return Collections.unmodifiableSet(ids);
    }
    /** Preflight both clubs before any record changes. */
    public void record(MatchDiscipline.Fixture f,Map<Integer,Integer> appearedClubs) {record(f,appearedClubs,Collections.emptySet());}
    public void record(MatchDiscipline.Fixture f,Map<Integer,Integer> appearedClubs,Set<Integer> uefaWinterRegistered) {
        fixture(f);if(recorded.containsKey(f.key()))throw new IllegalStateException("Participation fixture already recorded");
        if(appearedClubs==null||appearedClubs.size()>32)throw new IllegalArgumentException("Invalid participant count");
        TreeMap<Integer,TreeSet<Integer>> sheets=new TreeMap<>();sheets.put(f.home,new TreeSet<>());sheets.put(f.away,new TreeSet<>());
        for(Map.Entry<Integer,Integer> e:appearedClubs.entrySet()) {
            identity(e.getKey(),e.getValue());Registration r=registrations.get(e.getKey());
            if(r==null||r.club!=e.getValue()||!sheets.containsKey(e.getValue())||!r.baseline&&r.joined.isAfter(f.date))throw new IllegalArgumentException("Unregistered participant");
            String reason=reason(f,e.getKey(),e.getValue(),16,false,null,uefaWinterRegistered.contains(e.getKey()));if(!reason.isEmpty())throw new IllegalArgumentException(reason);
            sheets.get(e.getValue()).add(e.getKey());
        }
        for(Map.Entry<Integer,TreeSet<Integer>> e:sheets.entrySet()) {
            ArrayList<Sheet> old=recent.get(e.getKey());if(old!=null&&!old.isEmpty()&&old.get(old.size()-1).date.isAfter(f.date))throw new IllegalArgumentException("Club appearances moved backwards");
        }
        for(Map.Entry<Integer,Integer> e:appearedClubs.entrySet())appearances.computeIfAbsent(e.getKey(),k->new TreeMap<>()).computeIfAbsent(scope(f),k->new TreeMap<>()).computeIfAbsent(e.getValue(),k->new TreeSet<>()).add(f.phase);
        for(Map.Entry<Integer,TreeSet<Integer>> e:sheets.entrySet()) {
            ArrayList<Sheet> values=recent.computeIfAbsent(e.getKey(),k->new ArrayList<>());values.add(new Sheet(f.key(),f.date,e.getValue()));if(values.size()>2)values.remove(0);
        }
        recorded.put(f.key(),f.date);
    }
    public CupEligibility nextSeason(int nextYear,LocalDate from) {
        if(nextYear!=year+1)throw new IllegalArgumentException("Missing participation season");CupEligibility next=new CupEligibility(nextYear,from);
        for(Map.Entry<Integer,Registration> e:registrations.entrySet()){Registration a=e.getValue(),b=new Registration();b.club=a.club;b.parent=a.parent;b.joined=a.joined;b.baseline=a.baseline;b.registeredClubs.add(a.club);next.registrations.put(e.getKey(),b);}return next;
    }
    private static String ids(Collection<Integer> ids){StringJoiner out=new StringJoiner(",");for(int id:ids)out.add(String.valueOf(id));return out.toString();}
    private static TreeSet<Integer> ids(String text,int max){TreeSet<Integer> out=new TreeSet<>();if(!text.isEmpty())for(String s:text.split(",",-1)){int n=Integer.parseInt(s);if(n<0||n>=max||!out.add(n))throw new IllegalArgumentException("Invalid saved identities");}if(!ids(out).equals(text))throw new IllegalArgumentException("Noncanonical identities");return out;}
    public String snapshot() {
        StringBuilder out=new StringBuilder("CE1|").append(year).append('|').append(recordedFrom);
        for(Map.Entry<Integer,Registration> e:registrations.entrySet()){Registration r=e.getValue();out.append("\nJ|").append(e.getKey()).append('|').append(r.club).append('|').append(r.joined).append('|').append(r.parent).append('|').append(r.baseline?1:0).append('|').append(ids(r.registeredClubs));}
        for(Map.Entry<Integer,TreeMap<String,TreeMap<Integer,TreeSet<Integer>>>> p:appearances.entrySet())for(Map.Entry<String,TreeMap<Integer,TreeSet<Integer>>> c:p.getValue().entrySet())for(Map.Entry<Integer,TreeSet<Integer>> t:c.getValue().entrySet())out.append("\nP|").append(p.getKey()).append('|').append(c.getKey()).append('|').append(t.getKey()).append('|').append(ids(t.getValue()));
        for(Map.Entry<Integer,ArrayList<Sheet>> e:recent.entrySet())for(Sheet s:e.getValue())out.append("\nH|").append(e.getKey()).append('|').append(s.date).append('|').append(s.key).append('|').append(ids(s.players));
        for(Map.Entry<String,LocalDate> e:recorded.entrySet())out.append("\nF|").append(e.getKey()).append('|').append(e.getValue());return out.toString();
    }
    public static CupEligibility restore(String text) {
        if(text==null||text.length()>16*1024*1024)throw new IllegalArgumentException("Oversized participation save");String[] rows=text.split("\n",-1),h=rows[0].split("\\|",-1);
        if(h.length!=3||!h[0].equals("CE1")||rows.length>200000)throw new IllegalArgumentException("Invalid participation schema");CupEligibility out=new CupEligibility(Integer.parseInt(h[1]),LocalDate.parse(h[2]));
        for(int i=1;i<rows.length;i++) {
            String[] r=rows[i].split("\\|",-1);
            if(r[0].equals("J")&&r.length==7){int id=Integer.parseInt(r[1]),club=Integer.parseInt(r[2]);identity(id,club);Registration j=new Registration();j.club=club;j.joined=LocalDate.parse(r[3]);j.parent=Integer.parseInt(r[4]);j.baseline=r[5].equals("1");j.registeredClubs.addAll(ids(r[6],CareerLimits.MAX_CLUBS));if(j.parent< -1||j.parent>=CareerLimits.MAX_CLUBS||j.parent==club||!r[5].matches("[01]")||j.registeredClubs.isEmpty()||j.registeredClubs.size()>3||!j.registeredClubs.contains(club)||out.registrations.put(id,j)!=null)throw new IllegalArgumentException("Invalid saved registration");}
            else if(r[0].equals("P")&&r.length==5){int id=Integer.parseInt(r[1]),club=Integer.parseInt(r[3]);identity(id,club);TreeSet<Integer> phases=ids(r[4],100);if(!r[2].matches("(?:UEFA|(?:ENG|PT|ES|FR|IT|NL|DE|BE|SCO|TR)_[A-Z][A-Z0-9_]{0,29})")||phases.isEmpty()||out.appearances.computeIfAbsent(id,k->new TreeMap<>()).computeIfAbsent(r[2],k->new TreeMap<>()).put(club,phases)!=null)throw new IllegalArgumentException("Invalid saved appearances");}
            else if(r[0].equals("H")&&r.length==5){int club=Integer.parseInt(r[1]);identity(0,club);ArrayList<Sheet> values=out.recent.computeIfAbsent(club,k->new ArrayList<>());values.add(new Sheet(r[3],LocalDate.parse(r[2]),ids(r[4],MAX_PLAYERS)));if(values.size()>2)throw new IllegalArgumentException("Too many recent first-team matches");}
            else if(r[0].equals("F")&&r.length==3){LocalDate date=LocalDate.parse(r[2]);if(r[1].length()>160||!r[1].matches("[A-Z0-9_:-]+")||!r[1].endsWith(":"+date)||out.recorded.put(r[1],date)!=null)throw new IllegalArgumentException("Invalid saved participation fixture");}
            else throw new IllegalArgumentException("Invalid participation row");
        }
        if(!text.equals(out.snapshot()))throw new IllegalArgumentException("Noncanonical participation save");return out;
    }
    public void validate(int edition,int playerCount,int clubCount,LocalDate current) {
        if(edition!=year||playerCount<1||clubCount<2||clubCount>CareerLimits.MAX_CLUBS||current==null||current.isBefore(recordedFrom)||registrations.size()!=playerCount)throw new IllegalArgumentException("Wrong participation career");
        for(Map.Entry<Integer,Registration> e:registrations.entrySet()){Registration r=e.getValue();if(e.getKey()>=playerCount||r.club>=clubCount||r.parent>=clubCount||r.joined.isAfter(current)||r.joined.isBefore(LocalDate.of(1900,1,1))||r.registeredClubs.last()>=clubCount)throw new IllegalArgumentException("Unknown saved registration");}
        for(Map.Entry<Integer,TreeMap<String,TreeMap<Integer,TreeSet<Integer>>>> e:appearances.entrySet()){if(!registrations.containsKey(e.getKey()))throw new IllegalArgumentException("Unregistered appearance record");for(TreeMap<Integer,TreeSet<Integer>> teams:e.getValue().values())if(teams.lastKey()>=clubCount)throw new IllegalArgumentException("Unknown appearance club");}
        for(Map.Entry<String,LocalDate> e:recorded.entrySet())if(e.getValue().isBefore(recordedFrom)||e.getValue().isAfter(current))throw new IllegalArgumentException("Future participation match");
        for(Map.Entry<Integer,ArrayList<Sheet>> e:recent.entrySet()){if(e.getKey()>=clubCount)throw new IllegalArgumentException("Unknown first-team history");LocalDate previous=recordedFrom;for(Sheet s:e.getValue()){if(s.date.isBefore(previous)||!s.date.equals(recorded.get(s.key)))throw new IllegalArgumentException("Inconsistent recent first-team history");for(int id:s.players)if(id>=playerCount||!registrations.containsKey(id))throw new IllegalArgumentException("Unknown recent participant");previous=s.date;}}
    }
    public void validateTeams(int[] teams){if(teams==null||teams.length!=registrations.size())throw new IllegalArgumentException("Incomplete registration roster");for(int id=0;id<teams.length;id++)if(registrations.get(id)==null||registrations.get(id).club!=teams[id])throw new IllegalArgumentException("Registration disagrees with saved player club");}
}

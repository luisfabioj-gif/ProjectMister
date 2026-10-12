package com.projectmister.game;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Ordinary player cautions and automatic second-yellow bans. A ban is served by
 * an eligible first-team fixture, never by advancing the clock or playing elsewhere.
 * The 2026/27 rules are projected into later fictional career editions. */
public final class MatchDiscipline {
    public static final int UEFA_LEAGUE=10, UEFA_PLAYOFF=11, UEFA_LAST16=12,
        UEFA_QUARTER=13, UEFA_SEMI=14, UEFA_FINAL=15;
    public static final class Fixture {
        public final String competition;
        public final int year,phase,round,leg,home,away;
        public final boolean roundEnd;
        public final LocalDate date;
        public Fixture(int year,String competition,int phase,int round,int leg,boolean roundEnd,int home,int away,LocalDate date) {
            if(year<2026||year>2200||competition==null||!competition.matches("(?:UEFA|ENG|PT|ES|FR|IT|NL|DE|BE|SCO|TR)_[A-Z][A-Z0-9_]{0,29}")||competition.startsWith("UEFA_")&&!(phase>=1&&phase<=4||phase>=UEFA_LEAGUE&&phase<=UEFA_FINAL)||phase<0||phase>99||round<0||round>99||leg<0||leg>1||home<0||away<0||home==away||date==null||date.isBefore(LocalDate.of(year,7,1))||date.isAfter(LocalDate.of(year+1,7,31)))throw new IllegalArgumentException("Invalid disciplinary fixture");
            this.year=year;this.competition=competition;this.phase=phase;this.round=round;this.leg=leg;this.roundEnd=roundEnd;this.home=home;this.away=away;this.date=date;
        }
        String key(){return competition+":"+phase+":"+round+":"+leg+":"+home+":"+away+":"+date;}
        public boolean european(){return competition.startsWith("UEFA_");}
    }
    private static final class Counter {
        int cautions,next,phase=-1,eligibleRounds,completedRounds;
        LocalDate resetOn;
    }
    private static final class Ban {
        final String scope;
        final boolean yellow;
        final LocalDate effective;
        int remaining;
        Ban(String scope,boolean yellow,LocalDate effective,int remaining){this.scope=scope;this.yellow=yellow;this.effective=effective;this.remaining=remaining;}
    }
    private static final class Player {
        final TreeMap<String,Counter> counters=new TreeMap<>();
        final TreeMap<String,Integer> sendingOffs=new TreeMap<>();
        final ArrayList<Ban> bans=new ArrayList<>();
    }
    public final int year;
    public final LocalDate recordedFrom;
    private final TreeMap<Integer,Player> players=new TreeMap<>();
    private final TreeMap<String,LocalDate> recorded=new TreeMap<>();
    public MatchDiscipline(int year,LocalDate recordedFrom) {
        if(year<2026||year>2200||recordedFrom==null||recordedFrom.isBefore(LocalDate.of(year,7,1))||recordedFrom.isAfter(LocalDate.of(year+1,7,31)))throw new IllegalArgumentException("Invalid disciplinary edition");
        this.year=year;this.recordedFrom=recordedFrom;
    }
    private static String counterScope(Fixture f){if(f.european())return "UEFA";if(f.competition.equals("FR_COUPE")||f.competition.equals("FR_LEAGUE"))return "FR_DOMESTIC";return f.competition;}
    private static String dismissalScope(Fixture f) {
        if(f.european())return "UEFA";
        String country=f.competition.substring(0,f.competition.indexOf('_'));
        if(Arrays.asList("ENG","PT","FR","TR").contains(country))return country+"_DOMESTIC";
        return counterScope(f);
    }
    private static boolean applies(Ban ban,Fixture f){return ban.scope.equals(counterScope(f))||ban.scope.equals(dismissalScope(f));}
    private static int firstThreshold(Fixture f) {
        if(f.european())return f.phase<UEFA_LEAGUE?3:4;
        switch(f.competition){
            case "DE_POKAL":return 5;
            case "ES_COPA":return 3;
            case "NL_BEKER":case "IT_COPPA":case "SCO_CUP":case "SCO_LEAGUE_CUP":case "ENG_FA_CUP":case "ENG_LEAGUE_CUP":return 2;
            case "BE_CUP":return f.phase<5?2:3;
            case "TR_CUP":return 4;
            case "FR_COUPE":case "FR_LEAGUE":case "PT_LEAGUE":return 5;
            default:return 0; // Portugal's cup cautions do not feed the professional league cycle.
        }
    }
    private static int followingThreshold(Fixture f,int previous) {
        if(f.european())return previous+2;
        if(f.competition.equals("ENG_FA_CUP"))return previous+2;
        if(f.competition.equals("NL_BEKER"))return previous==2?4:previous+1;
        if(f.competition.equals("PT_LEAGUE"))return previous==5?9:previous==9?12:previous+2;
        return previous+firstThreshold(f);
    }
    private static boolean accumulationActive(Fixture f) {
        return !(f.competition.equals("SCO_CUP")&&f.phase>=7||f.competition.equals("SCO_LEAGUE_CUP")&&f.phase>=4||f.competition.equals("ENG_FA_CUP")&&f.phase>=7||f.competition.equals("ENG_LEAGUE_CUP")&&f.phase>=6);
    }
    private static boolean resetCautions(Fixture f,int previous) {
        if(f.european())return previous<UEFA_LEAGUE&&f.phase>=UEFA_LEAGUE||previous<UEFA_SEMI&&f.phase>=UEFA_SEMI;
        if(f.competition.equals("ES_COPA"))return previous<6&&f.phase>=6;
        if(f.competition.equals("SCO_CUP"))return previous<1&&f.phase>=1||previous<4&&f.phase>=4;
        if(f.competition.equals("SCO_LEAGUE_CUP"))return previous<2&&f.phase>=2;
        return f.competition.equals("BE_CUP")&&previous<5&&f.phase>=5;
    }
    private static boolean expirePendingYellow(Fixture f,int previous) {
        return f.european()&&previous<UEFA_LEAGUE&&f.phase>=UEFA_LEAGUE||f.competition.equals("BE_CUP")&&previous<5&&f.phase>=5;
    }
    private Player player(int id){if(id<0||id>=CareerLimits.MAX_CLUBS*64)throw new IllegalArgumentException("Invalid disciplinary player");return players.computeIfAbsent(id,k->new Player());}
    private void checkEdition(Fixture f){if(f==null||f.year!=year||f.date.isBefore(recordedFrom))throw new IllegalArgumentException("Wrong disciplinary edition or date");}
    /** Call only for the current fixture, after club registration and before selecting its match sheet. */
    public void prepare(Fixture f,Collection<Integer> registeredPlayers) {
        checkEdition(f);if(registeredPlayers==null)throw new IllegalArgumentException("Missing registered players");
        for(int id:registeredPlayers) {
            Player p=player(id);String scope=counterScope(f);Counter c=p.counters.computeIfAbsent(scope,k->new Counter());
            if(c.resetOn!=null&&!f.date.isBefore(c.resetOn)){c.cautions=0;c.next=firstThreshold(f);c.resetOn=null;}
            if(expirePendingYellow(f,c.phase))p.bans.removeIf(b->b.yellow&&b.scope.equals(scope));
            if(resetCautions(f,c.phase)){c.cautions=0;c.next=firstThreshold(f);}
            if(c.next==0)c.next=firstThreshold(f);
            c.phase=Math.max(c.phase,f.phase);
            if(f.european()&&f.phase<UEFA_LEAGUE&&suspendedMatches(f,id)==0)c.eligibleRounds|=1<<(f.phase-1);
        }
    }
    /** Read-only; future calendar/profile previews must not erase a current-round ban. */
    public int suspendedMatches(Fixture f,int id) {
        checkEdition(f);Player p=players.get(id);if(p==null)return 0;Counter c=p.counters.get(counterScope(f));int phase=c==null?-1:c.phase,total=0;
        for(Ban b:p.bans)if(applies(b,f)&&!f.date.isBefore(b.effective)&&!(b.yellow&&expirePendingYellow(f,phase)))total+=b.remaining;
        return total;
    }
    public int cautions(Fixture f,int id){Player p=players.get(id);Counter c=p==null?null:p.counters.get(counterScope(f));return c==null?0:c.cautions;}
    public String reason(Fixture f,int id){int n=suspendedMatches(f,id);return n==0?"":"Suspended: "+n+" eligible "+(f.european()?"UEFA":f.competition.replace('_',' '))+" match"+(n==1?"":"es")+" remaining";}
    public boolean recorded(Fixture f){return recorded.containsKey(f.key());}
    public void beginEuropeanLeaguePhase() {
        for(Player p:players.values()) {
            Counter c=p.counters.get("UEFA");if(c==null||c.phase>=UEFA_LEAGUE)continue;
            c.cautions=0;c.next=4;c.phase=UEFA_LEAGUE;p.bans.removeIf(b->b.yellow&&b.scope.equals("UEFA"));
        }
    }
    /** RFEF 119.4: the caution completing a cycle in the club's last competition
     * match incurs no accumulation ban. The result must be known before applying it. */
    public void lastSpanishCupAppearance(Fixture f,Collection<Integer> registeredPlayers) {
        if(!f.competition.equals("ES_COPA"))return;
        for(int id:registeredPlayers){Player p=players.get(id);if(p!=null)p.bans.removeIf(b->b.yellow&&b.scope.equals("ES_COPA")&&b.effective.equals(f.date.plusDays(1)));}
    }
    public List<String> playerSummary(int id) {
        ArrayList<String> out=new ArrayList<>();Player p=players.get(id);if(p==null)return out;
        for(Map.Entry<String,Counter> e:p.counters.entrySet())if(e.getValue().cautions>0)out.add(e.getKey().replace('_',' ')+": "+e.getValue().cautions+" caution"+(e.getValue().cautions==1?"":"s"));
        for(Ban b:p.bans)out.add(b.scope.replace('_',' ')+": "+b.remaining+" match"+(b.remaining==1?"":"es")+" suspended, effective "+b.effective);
        return Collections.unmodifiableList(out);
    }
    /** All squad members can serve a ban; only actual participants can receive match cards.
     * Service precedes new sanctions so the match that incurred a ban cannot serve it. */
    public void record(Fixture f,Collection<Integer> registeredPlayers,Collection<Integer> participants,Map<Integer,Integer> yellows,Set<Integer> secondYellowDismissals) {
        checkEdition(f);if(recorded.containsKey(f.key()))throw new IllegalStateException("Disciplinary fixture already recorded");
        if(registeredPlayers==null||participants==null||yellows==null||secondYellowDismissals==null)throw new IllegalArgumentException("Missing disciplinary match sheet");
        Set<Integer> roster=new HashSet<>(registeredPlayers),played=new HashSet<>(participants);
        if(roster.size()!=registeredPlayers.size()||played.size()!=participants.size()||!roster.containsAll(played)||!played.containsAll(yellows.keySet())||!played.containsAll(secondYellowDismissals))throw new IllegalArgumentException("Invalid disciplinary appearances");
        for(int id:roster)if(id<0||id>=CareerLimits.MAX_CLUBS*64)throw new IllegalArgumentException("Invalid disciplinary player");
        for(int id:played)if(suspendedMatches(f,id)>0)throw new IllegalArgumentException("Suspended player appeared");
        for(Map.Entry<Integer,Integer> e:yellows.entrySet())if(e.getValue()<1||e.getValue()>2||e.getValue()==2&&!secondYellowDismissals.contains(e.getKey()))throw new IllegalArgumentException("Unresolved second yellow");
        prepare(f,roster);
        for(int id:roster) {
            Player p=player(id);
            for(Iterator<Ban> it=p.bans.iterator();it.hasNext();) {
                Ban b=it.next();if(applies(b,f)&&!f.date.isBefore(b.effective)){if(--b.remaining==0)it.remove();break;}
            }
        }
        for(int id:played) {
            Player p=player(id);Counter c=p.counters.get(counterScope(f));
            if(secondYellowDismissals.contains(id)) {
                String scope=dismissalScope(f);int previous=p.sendingOffs.getOrDefault(scope,0);p.sendingOffs.put(scope,previous+1);
                int matches=scope.equals("ENG_DOMESTIC")||f.competition.equals("SCO_CUP")||f.competition.equals("SCO_LEAGUE_CUP")?previous+1:1;
                p.bans.add(new Ban(scope,false,f.date.plusDays(1),matches));
                if(counterScope(f).equals("FR_DOMESTIC"))c.resetOn=f.date.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
                continue;
            }
            int count=yellows.getOrDefault(id,0);
            if(count>0&&c.next>0&&accumulationActive(f)) {
                c.cautions+=count;
                if(c.cautions>=c.next) {
                    LocalDate effective=f.date.plusDays(1);
                    if(counterScope(f).equals("FR_DOMESTIC")) {
                        // Career commissions meet each Wednesday; their yellow bans take effect on Tuesday.
                        c.resetOn=f.date.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
                        effective=c.resetOn.with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
                    }
                    p.bans.add(new Ban(counterScope(f),true,effective,1));c.next=followingThreshold(f,c.next);
                }
            }
        }
        if(f.european()&&f.phase<UEFA_LEAGUE&&f.roundEnd)for(int id:roster) {
            Counter c=player(id).counters.get("UEFA");int bit=1<<(f.phase-1);
            if((c.completedRounds&bit)==0) {
                c.completedRounds|=bit;int eligible=Integer.bitCount(c.completedRounds&c.eligibleRounds);
                int previousThreshold=c.next==3?0:c.next-2;
                if((c.eligibleRounds&bit)!=0&&eligible>0&&eligible%2==0&&c.cautions>previousThreshold)c.cautions--;
            }
        }
        recorded.put(f.key(),f.date);
    }
    public MatchDiscipline nextSeason(int nextYear,LocalDate from) {
        if(nextYear!=year+1)throw new IllegalArgumentException("Missing disciplinary season");MatchDiscipline next=new MatchDiscipline(nextYear,from);
        for(Map.Entry<Integer,Player> entry:players.entrySet()) {
            Player p=new Player();for(Ban b:entry.getValue().bans) {
                boolean expires=b.yellow&&Arrays.asList("UEFA","DE_POKAL","ES_COPA","BE_CUP","SCO_CUP","SCO_LEAGUE_CUP","ENG_FA_CUP","ENG_LEAGUE_CUP","TR_CUP","PT_LEAGUE").contains(b.scope)||b.scope.equals("BE_CUP")&&!b.yellow;
                if((b.scope.equals("SCO_CUP")||b.scope.equals("SCO_LEAGUE_CUP"))&&from.isAfter(b.effective.plusYears(3)))expires=true;
                if(!expires)p.bans.add(new Ban(b.scope,b.yellow,b.effective,b.remaining));
            }
            if(!p.bans.isEmpty())next.players.put(entry.getKey(),p);
        }return next;
    }
    private static String date(LocalDate d){return d==null?"-":d.toString();}
    public String snapshot() {
        StringBuilder out=new StringBuilder("MD1|").append(year).append('|').append(recordedFrom);
        for(Map.Entry<Integer,Player> e:players.entrySet()) {
            for(Map.Entry<String,Counter> value:e.getValue().counters.entrySet()) {Counter c=value.getValue();out.append("\nC|").append(e.getKey()).append('|').append(value.getKey()).append('|').append(c.cautions).append('|').append(c.next).append('|').append(c.phase).append('|').append(c.eligibleRounds).append('|').append(c.completedRounds).append('|').append(date(c.resetOn));}
            for(Ban b:e.getValue().bans)out.append("\nB|").append(e.getKey()).append('|').append(b.scope).append('|').append(b.yellow?1:0).append('|').append(b.effective).append('|').append(b.remaining);
            for(Map.Entry<String,Integer> count:e.getValue().sendingOffs.entrySet())out.append("\nR|").append(e.getKey()).append('|').append(count.getKey()).append('|').append(count.getValue());
        }
        for(Map.Entry<String,LocalDate> e:recorded.entrySet())out.append("\nF|").append(e.getKey()).append('|').append(e.getValue());
        return out.toString();
    }
    public static MatchDiscipline restore(String text) {
        if(text==null||text.length()>12*1024*1024)throw new IllegalArgumentException("Oversized disciplinary save");String[] rows=text.split("\n",-1),head=rows[0].split("\\|",-1);
        if(head.length!=3||!head[0].equals("MD1")||rows.length>150000)throw new IllegalArgumentException("Invalid disciplinary schema");MatchDiscipline out=new MatchDiscipline(Integer.parseInt(head[1]),LocalDate.parse(head[2]));
        for(int i=1;i<rows.length;i++) {
            String[] r=rows[i].split("\\|",-1);
            if(r[0].equals("C")&&r.length==9) {
                Counter c=new Counter();c.cautions=Integer.parseInt(r[3]);c.next=Integer.parseInt(r[4]);c.phase=Integer.parseInt(r[5]);c.eligibleRounds=Integer.parseInt(r[6]);c.completedRounds=Integer.parseInt(r[7]);c.resetOn=r[8].equals("-")?null:LocalDate.parse(r[8]);
                if(!r[2].matches("[A-Z][A-Z0-9_]{1,39}")||c.cautions<0||c.cautions>200||c.next<0||c.next>202||c.phase<0||c.phase>99||c.eligibleRounds<0||c.eligibleRounds>15||c.completedRounds<0||c.completedRounds>15||out.player(Integer.parseInt(r[1])).counters.put(r[2],c)!=null)throw new IllegalArgumentException("Invalid caution counter");
            } else if(r[0].equals("B")&&r.length==6) {
                Ban b=new Ban(r[2],r[3].equals("1"),LocalDate.parse(r[4]),Integer.parseInt(r[5]));
                if(!r[2].matches("[A-Z][A-Z0-9_]{1,39}")||!r[3].matches("[01]")||b.remaining<1||b.remaining>20)throw new IllegalArgumentException("Invalid suspension");out.player(Integer.parseInt(r[1])).bans.add(b);
            } else if(r[0].equals("R")&&r.length==4) {
                int count=Integer.parseInt(r[3]);if(count<1||count>20||!r[2].matches("[A-Z][A-Z0-9_]{1,39}")||out.player(Integer.parseInt(r[1])).sendingOffs.put(r[2],count)!=null)throw new IllegalArgumentException("Invalid sending-off history");
            } else if(r[0].equals("F")&&r.length==3) {
                LocalDate d=LocalDate.parse(r[2]);if(r[1].length()>160||!r[1].matches("[A-Z0-9_:-]+")||out.recorded.put(r[1],d)!=null)throw new IllegalArgumentException("Invalid disciplinary fixture record");
            } else throw new IllegalArgumentException("Invalid disciplinary row");
        }
        if(!text.equals(out.snapshot()))throw new IllegalArgumentException("Non-canonical disciplinary save");return out;
    }
    public void validate(int edition,int playerCount,LocalDate current) {
        if(edition!=year||playerCount<1||current==null||recordedFrom.isAfter(current))throw new IllegalArgumentException("Wrong disciplinary career");
        for(int id:players.keySet())if(id>=playerCount)throw new IllegalArgumentException("Unknown disciplinary player");
        for(Map.Entry<String,LocalDate> e:recorded.entrySet())if(e.getValue().isBefore(recordedFrom)||e.getValue().isAfter(current)||!e.getKey().endsWith(":"+e.getValue()))throw new IllegalArgumentException("Future disciplinary match");
    }
}

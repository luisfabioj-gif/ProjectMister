package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Annual domestic admissions, titleholders, EPS pass-down and a projected access-list rebalance. */
public final class EuropeanAdmissions {
    public enum Path { CL_CH, CL_LP, EL_CH, EL_MP, CO_CH, CO_MP }
    public static final class Entry {
        public final int club,round;public final Path path;public final boolean cup;
        Entry(int club,Path path,int round,boolean cup){this.club=club;this.path=path;this.round=round;this.cup=cup;}
        public EuropeanLeaguePhase.Competition competition(){return EuropeanAdmissions.competition(path);}
        public String label(){return EuropeanCampaign.name(competition())+" · "+(round==5?"League phase":round==4?"Qualifying play-off":"Q"+round)+" · "+(path==Path.CL_CH||path==Path.EL_CH||path==Path.CO_CH?"champions path":"league/main path");}
    }
    public final int year;
    final String[] identities;final boolean[] reserves;final int[] coefficients;
    private final EuropeanDomesticSeason domestic;
    private final int championsHolder,europaHolder,conferenceHolder;
    private final String[] performance;
    private final SortedMap<Integer,Entry> entries=new TreeMap<>();
    public EuropeanAdmissions(EuropeanDomesticSeason domestic,String[] ids,boolean[] reserves,int[] coefficients,int championsHolder,int europaHolder,int conferenceHolder,String[] performance){
        if(domestic==null||ids==null||reserves==null||coefficients==null||ids.length!=reserves.length||ids.length!=coefficients.length||performance==null||performance.length!=2||performance[0].equals(performance[1]))throw new IllegalArgumentException("Incomplete annual UEFA admissions");
        year=domestic.year+1;this.domestic=domestic;identities=ids.clone();this.reserves=reserves.clone();this.coefficients=coefficients.clone();this.championsHolder=championsHolder;this.europaHolder=europaHolder;this.conferenceHolder=conferenceHolder;this.performance=performance.clone();
        for(int club=0;club<ids.length;club++)if(ids[club]==null||!ids[club].matches("[a-z]{2,4}:[a-z0-9-]+")||coefficients[club]<0||coefficients[club]>1000000)throw new IllegalArgumentException("Invalid annual UEFA registry");
        for(String country:domestic.associations()) {
            EuropeanAccess.Profile profile=EuropeanAccess.profile(country);int[] order=domestic.order(country);ArrayList<EuropeanAccess.Berth> lower=new ArrayList<>();
            for(EuropeanAccess.Berth berth:profile.league)if(berth.competition.equals("Champions League"))put(first(order),entry(berth,false));else lower.add(berth);
            int winner=domestic.cup(country);if(winner<0)throw new IllegalArgumentException("Missing domestic cup outcome: "+country);if(!entries.containsKey(winner))put(winner,entry(profile.cup,true));else put(first(order),entry(profile.cup,true));
            for(EuropeanAccess.Berth berth:lower){int club=country.equals("NL")&&berth.competition.equals("Conference League")&&domestic.dutchPlayoff()!=null?domestic.dutchPlayoff().winner():first(order);put(club,entry(berth,false));}
            if(profile.conferenceFromLeagueCup){int leagueWinner=domestic.leagueCup(country);put(entries.containsKey(leagueWinner)?first(order):leagueWinner,new Entry(-1,Path.CO_MP,4,true));}
        }
        holder(championsHolder,Path.CL_CH,true);holder(europaHolder,Path.CL_CH,false);
        holder(conferenceHolder,Path.EL_MP,false);
        for(String association:performance) {
            int[] order=domestic.order(association);int club=-1;for(int c:order){Entry e=entries.get(c);if(e==null||e.competition()!=EuropeanLeaguePhase.Competition.CHAMPIONS||e.round!=5){club=c;break;}}
            if(club<0)throw new IllegalArgumentException("Missing EPS domestic replacement: "+association);Entry displaced=entries.remove(club);put(club,new Entry(-1,Path.CL_CH,5,false));
            Set<Integer> passed=new HashSet<>();passed.add(club);
            while(displaced!=null){int replacement=nextForPassDown(order,displaced,passed);passed.add(replacement);Entry previous=entries.remove(replacement);put(replacement,displaced);displaced=previous;}
        }
        rebalance(Path.CL_CH,10);rebalance(Path.CL_LP,4);
        Projection p=project();int requiredEuropa=36-p.direct[1]-p.winners[Path.EL_CH.ordinal()][4];rebalance(Path.EL_MP,requiredEuropa*2);
        repairConferenceVacancies();p=project();int requiredConference=36-p.direct[2]-p.winners[Path.CO_CH.ordinal()][4];rebalance(Path.CO_MP,requiredConference*2);
        p=project();for(int c=0;c<3;c++)if(p.total[c]!=36)throw new IllegalStateException("Unbalanced projected UEFA access: "+Arrays.toString(p.total));
    }
    public EuropeanDomesticSeason domestic(){return domestic;}
    public String[] performanceAssociations(){return performance.clone();}
    public List<Entry> entries(){return Collections.unmodifiableList(new ArrayList<>(entries.values()));}
    public Entry entry(int club){return entries.get(club);}
    public static EuropeanLeaguePhase.Competition competition(Path path){return path.ordinal()<2?EuropeanLeaguePhase.Competition.CHAMPIONS:path.ordinal()<4?EuropeanLeaguePhase.Competition.EUROPA:EuropeanLeaguePhase.Competition.CONFERENCE;}
    private String association(int club){return EuropeanDomesticSeason.association(identities[club]);}
    private int first(int[] order){for(int club:order)if(!entries.containsKey(club))return club;throw new IllegalStateException("Not enough eligible domestic clubs for annual access");}
    private static Entry entry(EuropeanAccess.Berth berth,boolean cup){Path path=berth.competition.equals("Champions League")?(berth.stage.contains("champions")?Path.CL_CH:Path.CL_LP):berth.competition.equals("Europa League")?Path.EL_MP:Path.CO_MP;int round=berth.stage.startsWith("League phase")?5:berth.stage.startsWith("Play-off")?4:berth.stage.startsWith("Q3")?3:berth.stage.startsWith("Q2")?2:1;return new Entry(-1,path,round,cup);}
    private void put(int club,Entry entry){if(club<0||club>=identities.length||reserves[club]||entries.containsKey(club))throw new IllegalArgumentException("Duplicate or ineligible UEFA admission");entries.put(club,new Entry(club,entry.path,entry.round,entry.cup));}
    private int nextForPassDown(int[] order,Entry displaced,Set<Integer> passed){for(int club:order){if(passed.contains(club))continue;Entry e=entries.get(club);if(e==null||e.round!=5&&e.competition().ordinal()>=displaced.competition().ordinal()&&(e.competition()!=displaced.competition()||e.round<=displaced.round))return club;}throw new IllegalStateException("Insufficient EPS pass-down clubs");}
    private boolean highestOutstanding(int club,EuropeanLeaguePhase.Competition competition){for(int c:domestic.order(association(club))){Entry e=entries.get(c);if(e!=null&&e.competition()==competition&&e.round==5)continue;return c==club;}return domestic.cup(association(club))==club;}
    private void holder(int club,Path path,boolean championOnly){
        if(club<0||club>=identities.length||reserves[club])throw new IllegalArgumentException("Invalid UEFA titleholder");Entry e=entries.get(club);EuropeanLeaguePhase.Competition target=competition(path);
        if(e!=null&&(e.competition().ordinal()<target.ordinal()||e.competition()==target&&e.round==5)) {
            ArrayList<Entry> eligible=new ArrayList<>();for(Entry candidate:entries.values())if(candidate.competition()==target&&candidate.round<5&&(!championOnly||candidate.path==Path.CL_CH)&&(championOnly||candidate.cup||highestOutstanding(candidate.club,target)))eligible.add(candidate);
            eligible.sort((a,b)->Integer.compare(coefficients[b.club],coefficients[a.club]));if(eligible.isEmpty())throw new IllegalStateException("No titleholder vacancy replacement");Entry replacement=eligible.get(0);entries.remove(replacement.club);put(replacement.club,new Entry(-1,replacement.path,5,replacement.cup));
        }else{entries.remove(club);put(club,new Entry(-1,path,5,e!=null&&e.cup));}
    }
    static final class Projection {final int[][] fields=new int[6][6],winners=new int[6][6],losers=new int[6][6];final int[] direct=new int[3],total=new int[3];}
    Projection project(){
        Projection p=new Projection();for(Entry e:entries.values())if(e.round==5)p.direct[e.competition().ordinal()]++;else p.fields[e.path.ordinal()][e.round]++;
        for(int round=1;round<=4;round++)for(Path path:Path.values()) {
            int i=path.ordinal(),n=p.fields[i][round];p.winners[i][round]=(n+1)/2;p.losers[i][round]=n/2;
            if(round<4)p.fields[i][round+1]+=p.winners[i][round];else p.direct[competition(path).ordinal()]+=p.winners[i][round];
            Destination d=loserDestination(path,round);if(d!=null){if(d.round==5)p.direct[competition(d.path).ordinal()]+=p.losers[i][round];else p.fields[d.path.ordinal()][d.round]+=p.losers[i][round];}
        }
        // Direct includes qualifying winners. Store actual direct/moved entrants separately for balancing.
        System.arraycopy(p.direct,0,p.total,0,3);for(Path path:Path.values())p.direct[competition(path).ordinal()]-=p.winners[path.ordinal()][4];return p;
    }
    /** Future access lists are projections. Keep every association's admitted clubs
     * and all guaranteed phase berths, adjusting qualifying rounds when titleholder
     * movements would otherwise create an additional league-phase bye. */
    private void repairConferenceVacancies() {
        while(project().total[2]>36){Projection before=project();ArrayList<Entry> candidates=new ArrayList<>(entries.values());
            candidates.removeIf(e->e.competition()!=EuropeanLeaguePhase.Competition.CONFERENCE||e.round<2||e.round==5);
            candidates.sort((a,b)->{int c=Boolean.compare(a.cup,b.cup);if(c!=0)return c;c=Integer.compare(EuropeanAccess.rank(association(b.club)),EuropeanAccess.rank(association(a.club)));return c!=0?c:Integer.compare(coefficients[a.club],coefficients[b.club]);});boolean repaired=false;
            outer:for(Entry e:candidates)for(int round=e.round-1;round>=1;round--){
                entries.put(e.club,new Entry(e.club,e.path,round,e.cup));Projection after=project();
                if(after.total[0]==36&&after.total[1]==36&&after.total[2]<before.total[2]&&after.total[2]>=36){repaired=true;break outer;}entries.put(e.club,e);
            }
            if(!repaired)throw new IllegalStateException("No projected UEFA vacancy repair: "+Arrays.toString(before.total));
        }
    }
    private void rebalance(Path path,int target) {
        if(target<0)throw new IllegalStateException("Invalid UEFA access vacancy");int attempts=0;
        while(project().fields[path.ordinal()][4]<target) {
            if(++attempts>CareerLimits.MAX_CLUBS)throw new IllegalStateException("UEFA access rebalance did not converge");ArrayList<Entry> candidates=new ArrayList<>();
            for(Entry e:entries.values())if(e.path==path&&e.round<4)candidates.add(e);
            candidates.sort((a,b)->{int c=Integer.compare(b.round,a.round);if(c!=0)return c;if(path==Path.CL_CH||path==Path.CL_LP)return Integer.compare(coefficients[b.club],coefficients[a.club]);c=Boolean.compare(b.cup,a.cup);if(c!=0)return c;c=Integer.compare(EuropeanAccess.rank(association(a.club)),EuropeanAccess.rank(association(b.club)));return c!=0?c:Integer.compare(coefficients[b.club],coefficients[a.club]);});
            if(candidates.isEmpty())throw new IllegalStateException("Missing UEFA qualifying replacement: "+path);Entry e=candidates.get(0);entries.put(e.club,new Entry(e.club,e.path,e.round+1,e.cup));
        }
        if(project().fields[path.ordinal()][4]>target)throw new IllegalStateException("Too many UEFA qualifying entrants: "+path+" "+project().fields[path.ordinal()][4]+" > "+target+"; totals "+Arrays.toString(project().total)+"; holders "+championsHolder+","+europaHolder+","+conferenceHolder);
    }
    static final class Destination{final Path path;final int round;Destination(Path path,int round){this.path=path;this.round=round;}}
    static Destination loserDestination(Path path,int round){
        if(path==Path.CL_CH)return round==1?new Destination(Path.CO_CH,2):round==2?new Destination(Path.EL_CH,3):round==3?new Destination(Path.EL_CH,4):new Destination(Path.EL_MP,5);
        if(path==Path.CL_LP)return round==2?new Destination(Path.EL_MP,3):round>=3?new Destination(Path.EL_MP,5):null;
        if(path==Path.EL_CH)return round==3?new Destination(Path.CO_CH,4):round==4?new Destination(Path.CO_MP,5):null;
        if(path==Path.EL_MP)return new Destination(Path.CO_MP,Math.min(5,round+1));return null;
    }
    private static String encode(String value){return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));}
    public String snapshot(){StringBuilder out=new StringBuilder("UA1|").append(championsHolder).append('|').append(europaHolder).append('|').append(conferenceHolder).append('|').append(performance[0]).append('|').append(performance[1]);for(int club=0;club<identities.length;club++)out.append("\nC|").append(identities[club]).append('|').append(reserves[club]?1:0).append('|').append(coefficients[club]);return out.append("\nD|").append(encode(domestic.snapshot())).toString();}
    public static EuropeanAdmissions restore(String text){
        if(text==null||text.length()>512*1024)throw new IllegalArgumentException("Oversized UEFA admissions");String[] rows=text.split("\n",-1),h=rows[0].split("\\|",-1);if(h.length!=6||!h[0].equals("UA1")||rows.length<110||rows.length>CareerLimits.MAX_CLUBS+2||!rows[rows.length-1].startsWith("D|"))throw new IllegalArgumentException("Invalid UEFA admissions");int size=rows.length-2;String[] ids=new String[size];boolean[] reserves=new boolean[size];int[] coefficients=new int[size];
        for(int club=0;club<size;club++){String[] c=rows[club+1].split("\\|",-1);if(c.length!=4||!c[0].equals("C")||(!c[2].equals("0")&&!c[2].equals("1")))throw new IllegalArgumentException("Invalid UEFA admission club");ids[club]=c[1];reserves[club]=c[2].equals("1");coefficients[club]=Integer.parseInt(c[3]);}
        EuropeanDomesticSeason domestic=EuropeanDomesticSeason.restore(new String(Base64.getDecoder().decode(rows[rows.length-1].substring(2)),StandardCharsets.UTF_8),ids,reserves);EuropeanAdmissions out=new EuropeanAdmissions(domestic,ids,reserves,coefficients,Integer.parseInt(h[1]),Integer.parseInt(h[2]),Integer.parseInt(h[3]),new String[]{h[4],h[5]});if(!text.equals(out.snapshot()))throw new IllegalArgumentException("Non-canonical UEFA admissions");return out;
    }
}

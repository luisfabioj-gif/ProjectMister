package com.projectmister.game;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Saved final equality decisions, separate from the regular league's points and result ledger. */
public final class LeagueDecisions {
    public final int year,clubs;
    private final Map<String,Group> groups=new LinkedHashMap<>();
    private final TreeMap<String,int[]> discipline=new TreeMap<>();
    private final ArrayList<String> archives=new ArrayList<>();
    public static final class Event {
        public final String group;public final int fixture;public final KnockoutTie tie;public final LocalDate date;public final boolean series;
        Event(String group,int fixture,KnockoutTie tie,LocalDate date,boolean series){this.group=group;this.fixture=fixture;this.tie=tie;this.date=date;this.series=series;}
        public boolean contains(int club){return tie.firstHome==club||tie.firstAway==club;}
    }
    private static final class Group {
        String key,stage;int[] entrants,order;boolean lots;long seed;
        final List<KnockoutTie> ties=new ArrayList<>(),shootouts=new ArrayList<>();final List<LocalDate> dates=new ArrayList<>();
    }
    public LeagueDecisions(int year,int clubs){if(year<2026||year>2200||clubs<2||clubs>CareerLimits.MAX_CLUBS)throw new IllegalArgumentException("Invalid league decision season");this.year=year;this.clubs=clubs;}
    public void recordDiscipline(int round,int home,int away,int homeYellows,int awayYellows,int homeReds,int awayReds) {
        if(round<0||round>=100||home<0||away<0||home>=clubs||away>=clubs||home==away)throw new IllegalArgumentException("Invalid disciplinary fixture");
        for(int v:new int[]{homeYellows,awayYellows,homeReds,awayReds})if(v<0||v>30)throw new IllegalArgumentException("Invalid disciplinary points");
        String key=round+","+home+","+away;int[] values={homeYellows,awayYellows,homeReds,awayReds};int[] old=discipline.get(key);
        if(old!=null&&!Arrays.equals(old,values))throw new IllegalStateException("Conflicting disciplinary result");discipline.put(key,values);
    }
    public int[] disciplinaryPoints(LeagueResults ledger) {
        if(!ledger.recordedFromStart)return null;
        int[] points=new int[clubs];for(LeagueResults.Result result:ledger.results()){int[] cards=discipline.get(result.round+","+result.home+","+result.away);if(cards==null)return null;points[result.home]+=cards[0]+3*cards[2];points[result.away]+=cards[1]+3*cards[3];}return points;
    }
    private static String key(String stage,int[] ids){int[] sorted=ids.clone();Arrays.sort(sorted);return stage+":"+Arrays.toString(sorted);}
    public boolean contains(String stage,int[] entrants){return groups.containsKey(key(stage,entrants));}
    public List<LocalDate> dates(){ArrayList<LocalDate> dates=new ArrayList<>();for(Group group:groups.values())dates.addAll(group.dates);return Collections.unmodifiableList(dates);}
    /** The board fixes a neutral deciding series; unresolved series positions use a saved shootout placement draw. */
    public boolean ensure(String stage,int[] entrants,boolean lots,long seed,LocalDate earliest,Collection<LocalDate> blocked) {
        String key=key(stage,entrants);if(groups.containsKey(key))return false;
        if(!stage.matches("(FINAL[12]|SPLIT1)")||entrants.length<2||entrants.length>24||earliest==null||blocked==null)throw new IllegalArgumentException("Invalid deciding group");
        Group group=new Group();group.stage=stage;group.key=key;group.entrants=entrants.clone();Arrays.sort(group.entrants);group.lots=lots;group.seed=seed;Set<Integer> seen=new HashSet<>();for(int c:entrants)if(c<0||c>=clubs||!seen.add(c))throw new IllegalArgumentException("Invalid deciding entrant");
        if(lots){ArrayList<Integer> shuffled=new ArrayList<>();for(int c:group.entrants)shuffled.add(c);Collections.shuffle(shuffled,new Random(seed));group.order=shuffled.stream().mapToInt(Integer::intValue).toArray();}
        else {
            LeagueSchedule schedule=new LeagueSchedule(group.entrants,1);LocalDate date=earliest;
            for(int round=0;round<schedule.roundCount();round++) {
                for(;;date=date.plusDays(1)){if(date.isAfter(LocalDate.of(year+1,6,30)))throw new IllegalStateException("Deciding series cannot fit before the season deadline");boolean safe=true;for(LocalDate other:blocked)if(Math.abs(ChronoUnit.DAYS.between(date,other))<2){safe=false;break;}if(safe)break;}
                for(LeagueSchedule.Pairing fixture:schedule.round(round)){group.ties.add(new KnockoutTie(fixture.home,fixture.away,1,fixture.home,true,KnockoutTie.Rule.EXTRA_TIME_PENALTIES));group.dates.add(date);}date=date.plusDays(2);
            }
        }
        groups.put(key,group);return true;
    }
    public Event next(int managed) {
        Event next=null;for(Group group:groups.values())for(int i=0;i<group.ties.size();i++)if(pending(group,group.ties.get(i))){Event event=new Event(group.key,i,group.ties.get(i),group.dates.get(i),group.entrants.length>2);if(next==null||event.date.isBefore(next.date)||event.date.equals(next.date)&&event.contains(managed)&&!next.contains(managed))next=event;}
        return next;
    }
    public List<Event> fixtures(){ArrayList<Event> fixtures=new ArrayList<>();for(Group group:groups.values())for(int i=0;i<group.ties.size();i++)fixtures.add(new Event(group.key,i,group.ties.get(i),group.dates.get(i),group.entrants.length>2));return Collections.unmodifiableList(fixtures);}
    /** Event previews are recreated on every read; their object identity is not
     * the identity of the saved match. Use the stored group and fixture instead. */
    public int fixtureIndex(Event event) {
        if(event==null)throw new IllegalArgumentException("Missing deciding event");
        int offset=0;for(Group group:groups.values()) {
            if(group.key.equals(event.group)) {
                if(event.fixture<0||event.fixture>=group.ties.size()||group.ties.get(event.fixture)!=event.tie||!group.dates.get(event.fixture).equals(event.date))throw new IllegalArgumentException("Changed deciding event");
                return offset+event.fixture;
            }
            offset+=group.ties.size();
        }
        throw new IllegalArgumentException("Unknown deciding event");
    }
    public List<int[]> lotResults(){ArrayList<int[]> results=new ArrayList<>();for(Group group:groups.values())if(group.lots)results.add(group.order.clone());return results;}
    public List<KnockoutTie> shootoutSessions(){ArrayList<KnockoutTie> sessions=new ArrayList<>();for(Group group:groups.values())sessions.addAll(group.shootouts);return Collections.unmodifiableList(sessions);}
    public List<LeagueDecisions> editions(){ArrayList<LeagueDecisions> editions=new ArrayList<>();for(String saved:archives)editions.add(restore(saved));editions.add(this);return Collections.unmodifiableList(editions);}
    public LeagueDecisions nextSeason(int nextYear){if(nextYear!=year+1||!complete())throw new IllegalStateException("Complete all deciding fixtures before the next season");LeagueDecisions next=new LeagueDecisions(nextYear,clubs);next.archives.addAll(archives);next.archives.add(activeSnapshot());if(next.archives.size()>20)next.archives.remove(0);return next;}
    public boolean complete(){return next(-1)==null;}
    public boolean complete(String stage){for(Group group:groups.values())if(group.stage.equals(stage)&&group.order==null)for(KnockoutTie tie:group.ties)if(pending(group,tie))return false;return true;}
    private static boolean pending(Group group,KnockoutTie tie){return group.entrants.length>2?tie.playedLegs()==0:tie.winner()<0;}
    private int[] order(Group group) {
        if(group.order!=null)return group.order.clone();for(KnockoutTie tie:group.ties)if(pending(group,tie))return null;
        int[][] totals=new int[clubs][3];List<Integer> order=new ArrayList<>();for(int c:group.entrants)order.add(c);
        for(KnockoutTie tie:group.ties){int h=tie.firstHome,a=tie.firstAway;int hg=tie.aggregate(h),ag=tie.aggregate(a);if(group.entrants.length==2){totals[tie.winner()][0]+=3;}else{totals[h][0]+=hg>ag?3:hg==ag?1:0;totals[a][0]+=ag>hg?3:hg==ag?1:0;}totals[h][1]+=hg-ag;totals[a][1]+=ag-hg;totals[h][2]+=hg;totals[a][2]+=ag;}
        java.util.Comparator<Integer> compare=(a,b)->{for(int criterion=0;criterion<3;criterion++){int result=Integer.compare(totals[b][criterion],totals[a][criterion]);if(result!=0)return result;}return 0;};order.sort(compare);
        int session=0;Random random=new Random(group.seed);
        for(int start=0;start<order.size();) {int end=start+1;while(end<order.size()&&compare.compare(order.get(start),order.get(end))==0)end++;if(end-start>1){ArrayList<Integer> field=new ArrayList<>(order.subList(start,end));Collections.shuffle(field,random);ArrayList<Integer> placed=new ArrayList<>();int winner=field.get(0);for(int i=1;i<field.size();i++){int challenger=field.get(i);boolean simulatedHome=random.nextBoolean();KnockoutTie penalties;
                if(session<group.shootouts.size()){penalties=group.shootouts.get(session);if(penalties.firstHome!=winner||penalties.firstAway!=challenger)throw new IllegalArgumentException("Changed deciding shootout session");}
                else{penalties=new KnockoutTie(winner,challenger,1,winner,true,KnockoutTie.Rule.PENALTIES);penalties.recordRegulation(0,0);penalties.recordPenalties(simulatedHome?5:4,simulatedHome?4:5);group.shootouts.add(penalties);}
                placed.add(0,penalties.loser());winner=penalties.winner();session++;
            }placed.add(0,winner);for(int i=start;i<end;i++)order.set(i,placed.get(i-start));}start=end;}
        if(session!=group.shootouts.size())throw new IllegalArgumentException("Unexpected shootout session");return order.stream().mapToInt(Integer::intValue).toArray();
    }
    public void resolveSeries(){for(Group group:groups.values())if(!group.lots)order(group);}
    public void apply(String stage,Integer[] order,boolean[] tied) {
        for(int start=0;start<order.length;){int end=start+1;while(end<order.length&&tied[end-1])end++;if(end-start>1){int[] entrants=new int[end-start];for(int i=start;i<end;i++)entrants[i-start]=order[i];Group group=groups.get(key(stage,entrants));int[] resolved=group==null?null:order(group);if(resolved!=null){for(int i=start;i<end;i++)order[i]=resolved[i-start];for(int i=start;i<end-1;i++)tied[i]=false;}}start=end;}
    }
    public void validate(int season,int size,LocalDate current,LeagueResults ledger) {
        if(year!=season||clubs!=size||current==null)throw new IllegalArgumentException("Changed league decision registry");
        Set<String> fixtures=new HashSet<>();for(LeagueResults.Result result:ledger.results())fixtures.add(result.round+","+result.home+","+result.away);if(!fixtures.containsAll(discipline.keySet()))throw new IllegalArgumentException("Discipline without a regular fixture");
        for(Group group:groups.values())for(int i=0;i<group.ties.size();i++){KnockoutTie tie=group.ties.get(i);LocalDate date=group.dates.get(i);if(tie.playedLegs()>0&&date.isAfter(current)||tie.playedLegs()==0&&date.isBefore(current))throw new IllegalArgumentException("Conflicting deciding result date");}
    }
    private String activeSnapshot() {
        resolveSeries();
        StringBuilder out=new StringBuilder("LD1|").append(year).append('|').append(clubs);
        for(String key:discipline.keySet()){out.append("\nD|").append(key);for(int v:discipline.get(key))out.append('|').append(v);}
        for(Group group:groups.values()){out.append("\nG|").append(group.stage).append('|').append(group.lots?1:0).append('|').append(group.seed).append('|').append(join(group.entrants));if(group.lots)out.append('|').append(join(group.order));for(int i=0;i<group.ties.size();i++)out.append("\nT|").append(group.dates.get(i)).append('|').append(Base64.getEncoder().encodeToString(group.ties.get(i).snapshot().getBytes(StandardCharsets.UTF_8)));for(KnockoutTie shootout:group.shootouts)out.append("\nS|").append(Base64.getEncoder().encodeToString(shootout.snapshot().getBytes(StandardCharsets.UTF_8)));}return out.toString();
    }
    public String snapshot(){StringBuilder out=new StringBuilder(activeSnapshot());for(String saved:archives)out.append("\nA|").append(Base64.getEncoder().encodeToString(saved.getBytes(StandardCharsets.UTF_8)));return out.toString();}
    private static String join(int[] ids){StringJoiner out=new StringJoiner(",");for(int c:ids)out.add(Integer.toString(c));return out.toString();}
    private static int[] ids(String text){return Arrays.stream(text.split(",",-1)).mapToInt(Integer::parseInt).toArray();}
    public static LeagueDecisions restore(String text) {
        if(text==null||text.length()>4194304)throw new IllegalArgumentException("Invalid league decisions");String[] rows=text.split("\n",-1),header=rows[0].split("\\|",-1);if(header.length!=3||!header[0].equals("LD1"))throw new IllegalArgumentException("Invalid decision schema");LeagueDecisions out=new LeagueDecisions(Integer.parseInt(header[1]),Integer.parseInt(header[2]));Group group=null;
        for(int i=1;i<rows.length;i++){String[] row=rows[i].split("\\|",-1);if(row[0].equals("D")){if(row.length!=6)throw new IllegalArgumentException("Invalid cards");int[] fixture=ids(row[1]);if(fixture.length!=3)throw new IllegalArgumentException("Invalid disciplinary fixture");out.recordDiscipline(fixture[0],fixture[1],fixture[2],Integer.parseInt(row[2]),Integer.parseInt(row[3]),Integer.parseInt(row[4]),Integer.parseInt(row[5]));}
            else if(row[0].equals("G")){if(row.length<5||row.length>6||!row[1].matches("(FINAL[12]|SPLIT1)")||!row[2].matches("[01]"))throw new IllegalArgumentException("Invalid deciding draw");group=new Group();group.stage=row[1];group.lots=row[2].equals("1");group.seed=Long.parseLong(row[3]);group.entrants=ids(row[4]);int[] sorted=group.entrants.clone();Arrays.sort(sorted);if(!Arrays.equals(sorted,group.entrants)||sorted.length<2||sorted.length>24)throw new IllegalArgumentException("Invalid deciding entrants");for(int at=0;at<sorted.length;at++)if(sorted[at]<0||sorted[at]>=out.clubs||at>0&&sorted[at]==sorted[at-1])throw new IllegalArgumentException("Invalid deciding entrant");group.key=key(group.stage,sorted);if(out.groups.put(group.key,group)!=null)throw new IllegalArgumentException("Duplicate deciding group");if(group.lots){if(row.length!=6)throw new IllegalArgumentException("Missing lot result");group.order=ids(row[5]);int[] ordered=group.order.clone();Arrays.sort(ordered);if(!Arrays.equals(ordered,sorted))throw new IllegalArgumentException("Changed lot entrants");}else if(row.length!=5)throw new IllegalArgumentException("Unexpected lot result");}
            else if(row[0].equals("T")){if(row.length!=3||group==null||group.lots)throw new IllegalArgumentException("Unexpected deciding fixture");group.dates.add(LocalDate.parse(row[1]));group.ties.add(KnockoutTie.restore(new String(Base64.getDecoder().decode(row[2]),StandardCharsets.UTF_8)));}
            else if(row[0].equals("S")){if(row.length!=2||group==null||group.lots||group.entrants.length<3)throw new IllegalArgumentException("Unexpected shootout session");KnockoutTie tie=KnockoutTie.restore(new String(Base64.getDecoder().decode(row[1]),StandardCharsets.UTF_8));if(tie.rule!=KnockoutTie.Rule.PENALTIES||tie.legs!=1||!tie.neutral||tie.winner()<0||tie.aggregate(tie.firstHome)!=0||tie.aggregate(tie.firstAway)!=0)throw new IllegalArgumentException("Invalid deciding shootout");group.shootouts.add(tie);}
            else if(row[0].equals("A")){if(row.length!=2||out.archives.size()>=20)throw new IllegalArgumentException("Invalid deciding archive");String saved=new String(Base64.getDecoder().decode(row[1]),StandardCharsets.UTF_8);if(saved.contains("\nA|"))throw new IllegalArgumentException("Nested deciding archives");LeagueDecisions previous=restore(saved);if(!previous.complete()||previous.clubs!=out.clubs||previous.year>=out.year||!out.archives.isEmpty()&&restore(out.archives.get(out.archives.size()-1)).year+1!=previous.year)throw new IllegalArgumentException("Invalid deciding archive chronology");for(Event fixture:previous.fixtures())if(fixture.tie.playedLegs()==0)throw new IllegalArgumentException("Unplayed archived decision");out.archives.add(saved);}
            else throw new IllegalArgumentException("Unknown league decision record");}
        for(Group g:out.groups.values())if(!g.lots){LeagueSchedule schedule=new LeagueSchedule(g.entrants,1);int index=0;LocalDate previous=null;for(int round=0;round<schedule.roundCount();round++){LocalDate roundDate=null;for(LeagueSchedule.Pairing f:schedule.round(round)){if(index>=g.ties.size())throw new IllegalArgumentException("Missing deciding fixture");KnockoutTie t=g.ties.get(index);LocalDate date=g.dates.get(index++);if(t.firstHome!=f.home||t.firstAway!=f.away||t.legs!=1||!t.neutral||t.rule!=KnockoutTie.Rule.EXTRA_TIME_PENALTIES||date.isBefore(LocalDate.of(out.year,8,1))||date.isAfter(LocalDate.of(out.year+1,6,30))||roundDate!=null&&!roundDate.equals(date))throw new IllegalArgumentException("Changed deciding fixture");roundDate=date;}if(previous!=null&&ChronoUnit.DAYS.between(previous,roundDate)<2)throw new IllegalArgumentException("Invalid deciding recovery");previous=roundDate;}if(index!=g.ties.size())throw new IllegalArgumentException("Extra deciding fixture");}
        out.resolveSeries();
        if(!out.snapshot().equals(text))throw new IllegalArgumentException("Non-canonical league decisions");return out;
    }
}

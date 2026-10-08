package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** Published 2027/28 League Cup format. Entrants, preliminary qualifiers and dates are supplied outcomes. */
public final class PortugueseLeagueCupSeason {
    public static final String RULES="https://www.ligaportugal.pt/backoffice/assets/20260701_RTLIGA_27_28_329c7f6d07.pdf";
    public static final class Fixture {
        public final int home,away,stage,index;
        public final boolean neutral,league;
        public final LocalDate date;
        private Fixture(int h,int a,int stage,int index,boolean neutral,LocalDate date){home=h;away=a;this.stage=stage;this.index=index;this.neutral=neutral;this.date=date;league=stage==1;}
    }
    /** Age in days at the final league-phase date, for each player who actually appeared. */
    public static final class PlayerUse {
        public final int player,ageDays;
        public PlayerUse(int player,int ageDays){if(player<0||ageDays<365*12||ageDays>365*100)throw new IllegalArgumentException("Invalid used player");this.player=player;this.ageDays=ageDays;}
    }
    public final int season;
    public final boolean simulatedDates;
    private final long seed;
    private final int[] clubs,direct,preliminary;
    private final List<LocalDate> dates;
    private final ArrayList<String> events=new ArrayList<>();
    private final List<int[]> fixtures=new ArrayList<>(),results=new ArrayList<>();
    private final Map<Integer,Map<Integer,Integer>> used=new HashMap<>();
    private final ArrayList<KnockoutTie> ties=new ArrayList<>();
    private int stage,cursor;
    private int[] leagueClubs,resolvedRank;

    /** direct is ordered by prior Liga 1 position, then domestic cup qualification.
     * preliminary contains the actual article-7 qualifiers in home/away order, or is empty for an even field.
     * Dates: preliminary, league MD1/MD2, playoff, QF, SF1/SF2, final. */
    public PortugueseLeagueCupSeason(int year,long seed,int[] clubs,int[] direct,int[] preliminary,List<LocalDate> dates,boolean simulatedDates) {
        if(year<2027||year>2200||clubs==null||clubs.length<16||clubs.length>64||direct==null||direct.length<1||direct.length>7||preliminary==null||dates==null||dates.size()!=8)throw new IllegalArgumentException("Invalid League Cup season");
        this.season=year;this.seed=seed;this.clubs=clubs.clone();this.direct=direct.clone();this.preliminary=preliminary.clone();this.dates=Collections.unmodifiableList(new ArrayList<>(dates));this.simulatedDates=simulatedDates;
        Set<Integer> all=new HashSet<>(),excluded=new HashSet<>();for(int c:clubs)if(c<0||!all.add(c))throw new IllegalArgumentException("Duplicate cup club");
        for(int c:direct)if(!all.contains(c)||!excluded.add(c))throw new IllegalArgumentException("Invalid direct qualifier");
        int field=clubs.length-direct.length;
        if(preliminary.length!=(field%2==1?2:0)||field-field%2<2*(8-direct.length))throw new IllegalArgumentException("Missing or unexpected preliminary qualifiers");
        for(int c:preliminary)if(!all.contains(c)||!excluded.add(c))throw new IllegalArgumentException("Invalid preliminary qualifier");
        LocalDate previous=LocalDate.of(year,7,1).minusDays(1);
        for(LocalDate date:dates){if(date==null||!date.isAfter(previous)||date.isAfter(LocalDate.of(year+1,6,30)))throw new IllegalArgumentException("Invalid League Cup calendar");previous=date;}
        if(preliminary.length==2){stage=0;ties.add(tie(preliminary[0],preliminary[1],false));}else createLeague();
    }
    private static KnockoutTie tie(int h,int a,boolean neutral){return new KnockoutTie(h,a,1,h,neutral,KnockoutTie.Rule.PENALTIES);}
    private boolean excluded(int c){for(int d:direct)if(c==d)return true;return preliminary.length==2&&c==ties.get(0).loser();}
    private void createLeague() {
        List<Integer> pool=new ArrayList<>();for(int c:clubs)if(!excluded(c))pool.add(c);
        Collections.sort(pool);Collections.shuffle(pool,new Random(seed));leagueClubs=new int[pool.size()];for(int i=0;i<pool.size();i++){leagueClubs[i]=pool.get(i);used.put(pool.get(i),new HashMap<>());}
        // Two perfect matchings of a shuffled cycle: one home and one away, two distinct opponents.
        for(int i=0;i<pool.size();i+=2)fixtures.add(new int[]{pool.get(i),pool.get(i+1)});
        for(int i=1;i<pool.size();i+=2)fixtures.add(new int[]{pool.get(i),pool.get((i+1)%pool.size())});
        stage=1;cursor=0;ties.clear();
    }
    private void advance() {
        if(stage==1) {
            if(results.size()<fixtures.size())return;
            int[] rank=qualifiedOrder();int count=8-direct.length;ties.clear();
            for(int i=0;i<count;i++)ties.add(tie(rank[i],rank[2*count-1-i],false));stage=2;cursor=0;return;
        }
        while(cursor<ties.size()&&ties.get(cursor).winner()>=0)cursor++;
        if(cursor<ties.size()||stage==5)return;
        if(stage==0){createLeague();return;}
        List<Integer> winners=new ArrayList<>();for(KnockoutTie t:ties)winners.add(t.winner());ties.clear();cursor=0;
        if(stage==2) {
            int[] rank=order();Map<Integer,Integer> positions=new HashMap<>();for(int i=0;i<rank.length;i++)positions.put(rank[i],i);
            winners.sort(Comparator.comparingInt(positions::get));List<Integer> field=new ArrayList<>();for(int c:direct)field.add(c);field.addAll(winners);
            for(int i=0;i<4;i++)ties.add(tie(field.get(i),field.get(7-i),false));stage=3;
        }else if(stage==3){ties.add(tie(winners.get(0),winners.get(2),true));ties.add(tie(winners.get(1),winners.get(3),true));stage=4;}
        else{ties.add(tie(winners.get(0),winners.get(1),true));stage=5;}
    }
    public Fixture current() {
        advance();if(stage==5&&cursor==ties.size())return null;
        if(stage==1){int[] f=fixtures.get(results.size());int round=results.size()/(fixtures.size()/2);return new Fixture(f[0],f[1],1,results.size(),false,dates.get(1+round));}
        KnockoutTie t=ties.get(cursor);int d=stage==0?0:stage==2?3:stage==3?4:stage==4?5+cursor:7;
        return new Fixture(t.home(),t.away(),stage,cursor,t.neutral,dates.get(d));
    }
    public KnockoutTie currentTie(){Fixture f=current();return f==null||f.league?null:KnockoutTie.restore(ties.get(cursor).snapshot());}
    public boolean complete(){return current()==null;}
    public int winner(){return complete()?ties.get(0).winner():-1;}
    public int[] clubs(){return clubs.clone();}
    public int[] direct(){return direct.clone();}
    public int[] preliminary(){return preliminary.clone();}
    public List<LocalDate> calendar(){return dates;}
    public List<String> events(){return Collections.unmodifiableList(new ArrayList<>(events));}
    public String roundName(){Fixture f=current();return f==null?"Final":f.stage==0?"Preliminary qualifier":f.stage==1?"League phase • Matchday "+(f.index/(fixtures.size()/2)+1):f.stage==2?"Play-off":f.stage==3?"Quarter-final":f.stage==4?"Semi-final":"Final";}
    public int leagueFixtureCount(){return fixtures.size();}
    public int[] leagueFixture(int i){return fixtures.get(i).clone();}
    public int[] leagueResult(int i){return i>=results.size()?null:results.get(i).clone();}
    public int[] leagueClubs(){return leagueClubs==null?new int[0]:leagueClubs.clone();}
    public void recordRegulation(int home,int away) {
        Fixture f=current();if(f==null||f.league)throw new IllegalStateException("No knockout fixture pending");
        ties.get(cursor).recordRegulation(home,away);events.add("R|"+f.home+"|"+f.away+"|"+home+"|"+away);
    }
    public void recordPenalties(int home,int away) {
        Fixture f=current();if(f==null||f.league)throw new IllegalStateException("No shootout pending");
        ties.get(cursor).recordPenalties(home,away);events.add("S|"+f.home+"|"+f.away+"|"+home+"|"+away);
    }
    private static Map<Integer,Integer> appearances(PlayerUse[] players,Map<Integer,Integer> earlier) {
        if(players==null||players.length<11||players.length>16)throw new IllegalArgumentException("Record only players who appeared");
        Map<Integer,Integer> result=new TreeMap<>();
        for(PlayerUse p:players)if(p==null||result.put(p.player,p.ageDays)!=null||earlier.containsKey(p.player)&&earlier.get(p.player)!=p.ageDays)throw new IllegalArgumentException("Conflicting used-player identity or age");
        return result;
    }
    public void recordLeague(int home,int away,PlayerUse[] homePlayers,PlayerUse[] awayPlayers) {
        Fixture f=current();if(f==null||!f.league)throw new IllegalStateException("No league-phase fixture pending");
        if(home<0||away<0||home>99||away>99)throw new IllegalArgumentException("Invalid league score");
        Map<Integer,Integer> hp=appearances(homePlayers,used.get(f.home)),ap=appearances(awayPlayers,used.get(f.away));
        for(int player:hp.keySet())if(ap.containsKey(player))throw new IllegalArgumentException("Player appeared for both sides");
        used.get(f.home).putAll(hp);used.get(f.away).putAll(ap);results.add(new int[]{home,away});
        events.add("L|"+f.home+"|"+f.away+"|"+home+"|"+away+"|"+players(hp)+"|"+players(ap));
    }
    /** Points, goal difference, goals, collective opponents' points/GD/goals. */
    public Map<Integer,int[]> metrics() {
        Map<Integer,int[]> table=new HashMap<>();if(leagueClubs==null)return table;
        for(int club:leagueClubs)table.put(club,new int[6]);
        for(int i=0;i<results.size();i++){int[] f=fixtures.get(i),r=results.get(i),h=table.get(f[0]),a=table.get(f[1]);h[0]+=r[0]>r[1]?3:r[0]==r[1]?1:0;a[0]+=r[1]>r[0]?3:r[0]==r[1]?1:0;h[1]+=r[0]-r[1];a[1]+=r[1]-r[0];h[2]+=r[0];a[2]+=r[1];}
        for(int[] f:fixtures){int[] h=table.get(f[0]),a=table.get(f[1]);for(int i=0;i<3;i++){h[i+3]+=a[i];a[i+3]+=h[i];}}
        return table;
    }
    private int compare(int a,int b,Map<Integer,int[]> table) {
        int[] x=table.get(a),y=table.get(b);for(int i=0;i<6;i++){int c=Integer.compare(y[i],x[i]);if(c!=0)return c;}
        Map<Integer,Integer> pa=used.get(a),pb=used.get(b);if(pa.isEmpty()||pb.isEmpty())return 0;
        long sa=0,sb=0;for(int age:pa.values())sa+=age;for(int age:pb.values())sb+=age;
        return Long.compare(sa*pb.size(),sb*pa.size());
    }
    public int[] order() {
        if(resolvedRank!=null)return resolvedRank.clone();
        if(leagueClubs==null)return new int[0];Map<Integer,int[]> table=metrics();Integer[] rank=new Integer[leagueClubs.length];for(int i=0;i<rank.length;i++)rank[i]=leagueClubs[i];
        Arrays.sort(rank,(a,b)->{int c=compare(a,b,table);return c!=0?c:Integer.compare(a,b);});int[] out=new int[rank.length];for(int i=0;i<out.length;i++)out[i]=rank[i];return out;
    }
    private int[] qualifiedOrder() {
        if(resolvedRank!=null)return resolvedRank.clone();
        int[] order=order();Map<Integer,int[]> table=metrics();int limit=2*(8-direct.length);
        for(int i=0;i<Math.min(limit,order.length-1);i++)if(compare(order[i],order[i+1],table)==0)throw new IllegalStateException("League Cup ranking requires a sporting decision");return order;
    }
    public boolean rankingDecisionRequired() {
        if(stage!=1||results.size()!=fixtures.size())return false;
        try{qualifiedOrder();return false;}catch(IllegalStateException unresolved){return true;}
    }
    public LocalDate rankingDate(){return dates.get(2).plusDays(3);}
    /** Article 27 imports the general LPF unresolved-ranking procedure. This is a documented
     * interpretation: neutral decider for two; a neutral round robin for larger equal groups. */
    public void resolveRanking(PortugueseThirdDivisionSeason.Scores scores){resolveRanking(scores,null);}
    private void resolveRanking(PortugueseThirdDivisionSeason.Scores scores,List<String> saved){
        if(!rankingDecisionRequired())throw new IllegalStateException("No League Cup ranking decision pending");
        int[] rank=order();int size=0;for(int c:clubs)size=Math.max(size,c+1);
        PortugueseThirdDivisionSeason.Participation use=new PortugueseThirdDivisionSeason.Participation(size);for(int c:leagueClubs)use.used.put(c,new TreeMap<>(used.get(c)));
        PortugueseLowerDeciders decisions=new PortugueseLowerDeciders(seed,size);if(saved!=null)decisions.restoreEvents(saved);
        Map<Integer,int[]> table=metrics();for(int start=0;start<rank.length;){int end=start+1;while(end<rank.length&&compare(rank[start],rank[end],table)==0)end++;if(end-start>1&&start<2*(8-direct.length)){int[] ordered=decisions.resolveClubs(Arrays.copyOfRange(rank,start,end),"L_"+start,use,scores);System.arraycopy(ordered,0,rank,start,ordered.length);}start=end;}
        decisions.finishRestore();resolvedRank=rank;for(String result:decisions.events)events.add("D|"+Base64.getEncoder().encodeToString(result.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    private void replayEvents(String[] rows,LocalDate date){
        for(int i=5;i<rows.length;i++){
            if(rows[i].startsWith("D|")){if(date!=null&&rankingDate().isAfter(date))throw new IllegalArgumentException("Future League Cup neutral decision");List<String> saved=new ArrayList<>();while(i<rows.length&&rows[i].startsWith("D|")){saved.add(new String(Base64.getDecoder().decode(rows[i].substring(2)),java.nio.charset.StandardCharsets.UTF_8));i++;}i--;resolveRanking(null,saved);}
            else{if(date!=null){Fixture f=current();if(f==null||f.date.isAfter(date))throw new IllegalArgumentException("Future League Cup result");}replay(rows[i]);}
        }
    }
    public void validateDate(LocalDate date) {
        if(date==null)throw new IllegalArgumentException("Missing career date");String[] rows=snapshot().split("\n",-1);PortugueseLeagueCupSeason replay=restoreHeader(rows);replay.replayEvents(rows,date);
        if(rankingDecisionRequired()){if(rankingDate().isBefore(date))throw new IllegalArgumentException("Unresolved League Cup decision overdue");return;}
        Fixture next=current();if(next!=null&&next.date.isBefore(date))throw new IllegalArgumentException("Overdue League Cup fixture");
    }
    private static String ids(int[] values){StringJoiner out=new StringJoiner(",");for(int v:values)out.add(String.valueOf(v));return values.length==0?"-":out.toString();}
    private static int[] ids(String text){if(text.equals("-"))return new int[0];String[] p=text.split(",",-1);int[] out=new int[p.length];for(int i=0;i<p.length;i++)out[i]=Integer.parseInt(p[i]);return out;}
    private static String players(Map<Integer,Integer> players){StringJoiner out=new StringJoiner(",");for(int id:new TreeSet<>(players.keySet()))out.add(id+":"+players.get(id));return out.toString();}
    private static PlayerUse[] players(String text){String[] p=text.split(",",-1);PlayerUse[] out=new PlayerUse[p.length];for(int i=0;i<p.length;i++){String[] row=p[i].split(":",-1);if(row.length!=2)throw new IllegalArgumentException("Invalid player appearance");out[i]=new PlayerUse(Integer.parseInt(row[0]),Integer.parseInt(row[1]));}return out;}
    public String snapshot() {
        StringBuilder out=new StringBuilder("PTLC27|").append(season).append('|').append(seed).append('|').append(simulatedDates?1:0).append('\n').append(ids(clubs)).append('\n').append(ids(direct)).append('\n').append(ids(preliminary));
        StringJoiner calendar=new StringJoiner(",");for(LocalDate date:dates)calendar.add(date.toString());out.append('\n').append(calendar);
        for(String event:events)out.append('\n').append(event);return out.toString();
    }
    private static PortugueseLeagueCupSeason restoreHeader(String[] rows) {
        String[] h=rows[0].split("\\|",-1);if(rows.length<5||h.length!=4||!h[0].equals("PTLC27")||!h[3].equals("0")&&!h[3].equals("1"))throw new IllegalArgumentException("Invalid League Cup save");
        List<LocalDate> dates=new ArrayList<>();for(String d:rows[4].split(",",-1))dates.add(LocalDate.parse(d));
        return new PortugueseLeagueCupSeason(Integer.parseInt(h[1]),Long.parseLong(h[2]),ids(rows[1]),ids(rows[2]),ids(rows[3]),dates,h[3].equals("1"));
    }
    private void replay(String text) {
        String[] e=text.split("\\|",-1);if(e.length<5)throw new IllegalArgumentException("Invalid League Cup event");Fixture f=current();
        if(f==null||f.home!=Integer.parseInt(e[1])||f.away!=Integer.parseInt(e[2]))throw new IllegalArgumentException("League Cup event violates draw");
        int h=Integer.parseInt(e[3]),a=Integer.parseInt(e[4]);
        if(e[0].equals("L")&&e.length==7)recordLeague(h,a,players(e[5]),players(e[6]));
        else if(e[0].equals("R")&&e.length==5)recordRegulation(h,a);
        else if(e[0].equals("S")&&e.length==5)recordPenalties(h,a);
        else throw new IllegalArgumentException("Unknown League Cup event");
    }
    public static PortugueseLeagueCupSeason restore(String text) {
        if(text==null||text.length()>256*1024)throw new IllegalArgumentException("Invalid League Cup save size");String[] rows=text.split("\n",-1);
        if(rows.length>2048)throw new IllegalArgumentException("Too many League Cup events");PortugueseLeagueCupSeason cup=restoreHeader(rows);
        cup.replayEvents(rows,null);if(!text.equals(cup.snapshot()))throw new IllegalArgumentException("Non-canonical League Cup save");return cup;
    }
}

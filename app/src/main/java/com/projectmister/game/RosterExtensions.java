package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** Ordered supplementary roster identities. Academy promotions and market hires
 * share one append-only order, so adding either cannot renumber the other. */
public final class RosterExtensions {
    public static final class Origin {
        public final String market,club;
        public final LocalDate birth;
        public final int cohort;
        private Origin(String market,String club,LocalDate birth,int cohort){this.market=market;this.club=club;this.birth=birth;this.cohort=cohort;}
        public boolean academy(){return market==null;}
    }
    private final ArrayList<Origin> origins=new ArrayList<>();
    public List<Origin> origins(){return Collections.unmodifiableList(origins);}
    public int size(){return origins.size();}
    public static String[] clubIdentities(String[] world,int count){if(world!=null)return world.clone();String[] ids=new String[count];for(int i=0;i<count;i++)ids[i]="pt:classic-"+i;return ids;}
    public Set<Integer> goalkeepers(int clubs){if(clubs<2||clubs>CareerLimits.MAX_CLUBS)throw new IllegalArgumentException("Invalid goalkeeper registry");TreeSet<Integer> keepers=new TreeSet<>();for(int club=0;club<clubs;club++){keepers.add(club*20);keepers.add(club*20+1);}for(int i=0;i<origins.size();i++){Origin origin=origins.get(i);if(origin.academy()||new WorldMarket.Candidate(origin.market).position.equals("GK"))keepers.add(clubs*20+i);}return Collections.unmodifiableSet(keepers);}
    public void market(String key){new WorldMarket.Candidate(key);for(Origin o:origins)if(key.equals(o.market))throw new IllegalArgumentException("Duplicate supplementary market player");origins.add(new Origin(key,null,null,0));}
    public Origin academy(String club,LocalDate birth) {
        if(club==null||!club.matches("[a-z0-9:-]{1,100}")||birth==null||birth.isBefore(LocalDate.of(1980,1,1))||birth.isAfter(LocalDate.of(2200,1,1)))throw new IllegalArgumentException("Invalid academy origin");
        int cohort=0;for(Origin o:origins)if(club.equals(o.club))cohort++;if(cohort>=32)throw new IllegalArgumentException("Academy promotion capacity reached");Origin origin=new Origin(null,club,birth,cohort);origins.add(origin);return origin;
    }
    public void validate(List<String> market,String[] clubs) {
        if(market==null||clubs==null||clubs.length<2||clubs.length>CareerLimits.MAX_CLUBS||origins.size()>clubs.length*32+WorldMarket.MAX_HIRES)throw new IllegalArgumentException("Invalid supplementary roster");
        Set<String> registry=new HashSet<>(Arrays.asList(clubs));ArrayList<String> actual=new ArrayList<>();Map<String,Integer> cohorts=new HashMap<>();
        for(Origin o:origins)if(o.academy()){int expected=cohorts.getOrDefault(o.club,0);if(!registry.contains(o.club)||o.cohort!=expected||expected>=32)throw new IllegalArgumentException("Changed academy club or cohort");cohorts.put(o.club,expected+1);}else actual.add(o.market);
        if(!actual.equals(market))throw new IllegalArgumentException("Supplementary market order disagrees with recruitment history");
    }
    public String snapshot(){StringBuilder out=new StringBuilder("RE1");for(Origin o:origins)if(o.academy())out.append("\nG|").append(o.club).append('|').append(o.birth).append('|').append(o.cohort);else out.append("\nM|").append(o.market);return out.toString();}
    public static RosterExtensions restore(String text) {
        if(text==null||text.length()>4*1024*1024)throw new IllegalArgumentException("Oversized supplementary roster");String[] rows=text.split("\n",-1);if(!rows[0].equals("RE1")||rows.length>CareerLimits.MAX_CLUBS*32+WorldMarket.MAX_HIRES+1)throw new IllegalArgumentException("Invalid supplementary roster schema");RosterExtensions out=new RosterExtensions();
        for(int i=1;i<rows.length;i++){String[] r=rows[i].split("\\|",-1);if(r.length==2&&r[0].equals("M"))out.market(r[1]);else if(r.length==4&&r[0].equals("G")){Origin o=out.academy(r[1],LocalDate.parse(r[2]));if(o.cohort!=Integer.parseInt(r[3]))throw new IllegalArgumentException("Invalid academy cohort");}else throw new IllegalArgumentException("Invalid supplementary player row");}
        if(!text.equals(out.snapshot()))throw new IllegalArgumentException("Noncanonical supplementary roster");return out;
    }
    public static RosterExtensions legacy(List<String> market){RosterExtensions out=new RosterExtensions();for(String key:market)out.market(key);return out;}
}

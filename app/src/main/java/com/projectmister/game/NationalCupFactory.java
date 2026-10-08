package com.projectmister.game;

import java.util.*;

/** Builds the main national cup fields from a stable career registry and prior sporting order. */
public final class NationalCupFactory {
    private final String country;
    private final String[] identities;
    private final int[] tiers,levels;
    private final boolean[] reserves;
    private final Map<Integer,Integer> positions=new HashMap<>();
    private NationalCupFactory(String country,String[] identities,int[] tiers,int[] levels,boolean[] reserves,int[] previousOrder) {
        if(identities==null||tiers.length!=identities.length||levels.length!=tiers.length||reserves.length!=tiers.length)throw new IllegalArgumentException("Incomplete cup registry");
        this.country=country;this.identities=identities;this.tiers=tiers;this.levels=levels;this.reserves=reserves;
        if(previousOrder!=null)for(int i=0;i<previousOrder.length;i++)if(previousOrder[i]<0||previousOrder[i]>=tiers.length||positions.put(previousOrder[i],i)!=null)throw new IllegalArgumentException("Invalid qualification order");
    }
    private List<Integer> tier(int level) {
        ArrayList<Integer> clubs=new ArrayList<>();for(int i=0;i<tiers.length;i++)if(!reserves[i]&&tiers[i]==level&&identities[i].startsWith(country.toLowerCase(Locale.ROOT)+":"))clubs.add(i);
        clubs.sort(Comparator.comparingInt(c->positions.getOrDefault(c,10000+c)));return clubs;
    }
    private List<Integer> lowerLevel(int level) {List<Integer> clubs=tier(3);clubs.removeIf(c->levels[c]!=level);return clubs;}
    private List<Integer> take(List<Integer> clubs,int count){if(count<0||clubs.size()<count)throw new IllegalArgumentException("Missing "+country+" qualifying clubs: "+count);return new ArrayList<>(clubs.subList(0,count));}
    private static void enter(List<SeasonCup.Entrant> field,List<Integer> clubs,int stage,int[] tiers,int[] levels,Set<Integer> seeds) {
        for(int c:clubs)field.add(new SeasonCup.Entrant(c,stage,tiers[c]==3,seeds.contains(c),tiers[c]<=2?tiers[c]:levels[c]));
    }
    private void enter(List<SeasonCup.Entrant> field,List<Integer> clubs,int stage){enter(field,clubs,stage,tiers,levels,Collections.emptySet());}
    private List<Integer> known(List<Integer> pool,String... keys) {
        ArrayList<Integer> result=new ArrayList<>();for(String key:keys){int found=-1;for(int club:pool)if(identities[club].equals(country.toLowerCase(Locale.ROOT)+":"+key)){found=club;break;}if(found<0)throw new IllegalArgumentException("Missing published cup entrant: "+key);result.add(found);}return result;
    }
    private List<Integer> direct(List<Integer> upper,int[] european,int fallback,int year) {
        LinkedHashSet<Integer> clubs=new LinkedHashSet<>();if(european!=null)for(int c:european)if(upper.contains(c))clubs.add(c);
        if(clubs.isEmpty()&&year==2026&&country.equals("ENG"))clubs.addAll(known(upper,"arsenal","aston-villa","liverpool","manchester-city","manchester-united","afc-bournemouth","crystal-palace","sunderland","brighton-hove-albion"));
        if(clubs.isEmpty()&&year==2026&&country.equals("NL"))clubs.addAll(known(upper,"psv","feyenoord","n-e-c-nijmegen","az","ajax","fc-twente"));
        if(clubs.isEmpty())clubs.addAll(take(upper,fallback));return new ArrayList<>(clubs);
    }
    public static List<SeasonCup> create(String country,int year,long seed,String[] identities,int[] tiers,int[] levels,boolean[] reserves,int[] previousOrder,int[] european) {
        return new NationalCupFactory(country,identities,tiers,levels,reserves,previousOrder).create(year,seed,european);
    }
    private List<SeasonCup> create(int year,long seed,int[] european) {
        List<Integer> upper=tier(1),second=tier(2),lower=tier(3);ArrayList<SeasonCup> cups=new ArrayList<>();ArrayList<SeasonCup.Entrant> field=new ArrayList<>();
        switch(country) {
            case "ENG": {
                if(upper.size()!=20||second.size()!=24)throw new IllegalArgumentException("Incomplete English professional leagues");
                List<Integer> leagueLower=new ArrayList<>(lower);leagueLower.removeIf(c->levels[c]>4);
                List<Integer> qualifiers=new ArrayList<>(lower);qualifiers.removeAll(leagueLower);Collections.shuffle(qualifiers,new Random(seed));
                enter(field,take(leagueLower,48),0);enter(field,take(qualifiers,32),0);enter(field,upper,2);enter(field,second,2);cups.add(NationalCupFormats.create("ENG_FA_CUP",year,seed,field,false));field=new ArrayList<>();
                List<Integer> exempt=direct(upper,european,year==2026?9:7,year);if(exempt.size()<2||exempt.size()>10)throw new IllegalArgumentException("Unsupported English European field");
                ArrayList<Integer> efl=new ArrayList<>(second);List<Integer> professionalLower=new ArrayList<>(lower);professionalLower.removeIf(c->levels[c]>4);efl.addAll(take(professionalLower,48));
                int prelim=Math.max(0,4*(exempt.size()-8)),byes=Math.max(0,2*(8-exempt.size())),offset=prelim>0?1:0;
                List<Integer> preliminary=take(new ArrayList<>(efl.subList(Math.max(0,efl.size()-prelim),efl.size())),prelim);
                if(year==2026&&european==null)preliminary=known(efl,"cup-tranmere-rovers","cup-rochdale","cup-york-city","cup-crawley-town");
                List<Integer> delayed=take(efl,byes);efl.removeAll(preliminary);efl.removeAll(delayed);
                enter(field,preliminary,0);enter(field,efl,offset);ArrayList<Integer> ordinary=new ArrayList<>(upper);ordinary.removeAll(exempt);ordinary.addAll(delayed);enter(field,ordinary,offset+1);
                // Protect Champions/Europa participants in round three; Conference has no September clash.
                Set<Integer> protectedClubs=new HashSet<>(take(exempt,Math.min(8,exempt.size())));enter(field,exempt,offset+2,tiers,levels,protectedClubs);
                cups.add(NationalCupFormats.create("ENG_LEAGUE_CUP",year,seed+101,field,prelim>0));break;
            }
            case "ES": {
                List<Integer> superCup=year==2026?known(upper,"fc-barcelona","real-madrid","real-sociedad","atletico-de-madrid"):take(upper,4),first=new ArrayList<>(upper);first.removeAll(superCup);first.addAll(second);
                enter(field,take(lower,116-upper.size()-second.size()),0);enter(field,first,0);enter(field,superCup,2);
                cups.add(NationalCupFormats.create("ES_COPA",year,seed,field,false));break;
            }
            case "IT": {
                List<Integer> last16=year==2026?known(upper,"inter","napoli","roma","como","milan","juventus","atalanta","bologna"):take(upper,8),ordinary=new ArrayList<>(upper);ordinary.removeAll(last16);
                List<Integer> prelim=year==2026?known(second,"l-r-vicenza","arezzo","benevento","ascoli"):take(new ArrayList<>(second.subList(Math.max(0,second.size()-4),second.size())),4),regularSecond=new ArrayList<>(second);regularSecond.removeAll(prelim);
                enter(field,last16,3);enter(field,ordinary,1);enter(field,regularSecond,1);enter(field,prelim,0);enter(field,take(lower,4),0);
                if(year==2026) {
                    List<Integer> all=new ArrayList<>(upper);all.addAll(second);all.addAll(lower);
                    List<Integer> seedOrder=known(all,"inter","napoli","roma","como","milan","juventus","atalanta","bologna","lazio","udinese","sassuolo","torino","parma","cagliari","fiorentina","genoa","lecce","venezia","frosinone","monza","cremonese","hellas-verona","pisa","catanzaro","palermo","modena","juve-stabia","avellino","mantova","padova","cesena","carrarese","sampdoria","virtus-entella","empoli","sudtirol","l-r-vicenza","arezzo","benevento","ascoli","cup-catania","cup-union-brescia","cup-ravenna","cup-potenza");
                    field.sort(Comparator.comparingInt(e->seedOrder.indexOf(e.club)));
                } else field.sort(Comparator.comparingInt(e->e.level*10000+positions.getOrDefault(e.club,1000+e.club)));
                cups.add(NationalCupFormats.create("IT_COPPA",year,seed,field,false));break;
            }
            case "FR": {
                List<Integer> overseas=new ArrayList<>();for(int c:lower)if(identities[c].contains(":overseas-"))overseas.add(c);overseas=take(overseas,4);
                ArrayList<Integer> mainland=new ArrayList<>(lower);mainland.removeAll(overseas);Collections.shuffle(mainland,new Random(seed));enter(field,second,0);enter(field,take(mainland,176-second.size()),0);enter(field,overseas,1);enter(field,upper,2);
                cups.add(NationalCupFormats.create("FR_COUPE",year,seed,field,false));break;
            }
            case "NL": {
                List<Integer> exempt=direct(upper,european,year==2026?6:4,year),ordinary=new ArrayList<>(upper);ordinary.removeAll(exempt);ordinary.addAll(second);
                enter(field,ordinary,0);enter(field,take(lower,64-2*exempt.size()-ordinary.size()),0);enter(field,exempt,1);
                cups.add(NationalCupFormats.create("NL_BEKER",year,seed,field,false));break;
            }
            case "BE": {
                enter(field,second,0);enter(field,take(lower,2*(32-upper.size())-second.size()),0);
                enter(field,upper,1,tiers,levels,new HashSet<>(take(upper,16)));cups.add(NationalCupFormats.create("BE_CUP",year,seed,field,false));break;
            }
            case "SCO": {
                List<Integer> early=new ArrayList<>(lower);early.removeIf(c->levels[c]<6);Collections.shuffle(early,new Random(seed));enter(field,take(early,10),0);enter(field,new ArrayList<>(early.subList(10,45)),1);
                enter(field,lowerLevel(5),3);enter(field,lowerLevel(4),4);enter(field,lowerLevel(3),5);enter(field,second,5);enter(field,upper,6);
                cups.add(NationalCupFormats.create("SCO_CUP",year,seed,field,false));break;
            }
            case "TR": {
                List<Integer> l3=take(lowerLevel(3),35),l4=take(lowerLevel(4),53),regional=take(lowerLevel(5),20);
                enter(field,regional,0);enter(field,new ArrayList<>(l4.subList(33,53)),0);enter(field,new ArrayList<>(l4.subList(0,33)),1);enter(field,new ArrayList<>(l3.subList(26,35)),1);
                enter(field,new ArrayList<>(l3.subList(0,26)),2);enter(field,new ArrayList<>(second.subList(11,20)),2);enter(field,new ArrayList<>(upper.subList(12,18)),2);
                enter(field,new ArrayList<>(second.subList(0,11)),3);enter(field,new ArrayList<>(upper.subList(5,12)),3);enter(field,take(upper,5),4);
                field.sort(Comparator.comparingInt(e->positions.getOrDefault(e.club,10000+e.club)));cups.add(NationalCupFormats.create("TR_CUP",year,seed,field,false));break;
            }
            default:throw new IllegalArgumentException("Country has a dedicated cup engine");
        }
        return Collections.unmodifiableList(cups);
    }
    private NationalCupFactory(){throw new AssertionError();}
}

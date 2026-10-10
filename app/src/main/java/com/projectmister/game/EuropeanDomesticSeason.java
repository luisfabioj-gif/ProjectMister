package com.projectmister.game;

import java.util.*;

/** Saved domestic outcomes for annual UEFA admissions. Foreign fixtures are career simulations. */
public final class EuropeanDomesticSeason {
    public final int year;
    private final SortedMap<String,int[]> orders=new TreeMap<>();
    private final SortedMap<String,Integer> cups=new TreeMap<>(),leagueCups=new TreeMap<>();
    private final String[] identities;
    private final int[][] table;
    public EuropeanDomesticSeason(int year,String[] identities,boolean[] reserves,int[] levels,int[] strengths,String local,int[] localOrder,int localCup,int localLeagueCup,long seed) {
        if(year<2026||year>2200||identities==null||reserves==null||levels==null||strengths==null||identities.length!=reserves.length||identities.length!=levels.length||identities.length!=strengths.length||localOrder==null||localOrder.length<10)throw new IllegalArgumentException("Incomplete domestic qualification results");
        this.year=year;this.identities=identities.clone();table=new int[identities.length][8];Random random=new Random(seed);SortedMap<String,List<Integer>> groups=new TreeMap<>();
        for(int club=0;club<identities.length;club++)if(!reserves[club]&&levels[club]==1)groups.computeIfAbsent(association(identities[club]),unused->new ArrayList<>()).add(club);
        for(String country:groups.keySet()) {
            List<Integer> field=groups.get(country);if(country.equals(local)){validateOrder(localOrder,country,reserves);orders.put(country,localOrder.clone());}
            else {
                for(int h:field)for(int a:field)if(h!=a)record(h,a,goals(random,strengths[h]-strengths[a]+4),goals(random,strengths[a]-strengths[h]));
                field.sort((a,b)->compare(a,b));
                // The foreign pool is a simulation; a saved draw resolves otherwise equal simulated totals.
                for(int at=0;at<field.size();) {int end=at+1;while(end<field.size()&&compare(field.get(at),field.get(end))==0)end++;if(end-at>1){List<Integer> tied=field.subList(at,end);Collections.shuffle(tied,random);for(int j=0;j<tied.size();j++)table[tied.get(j)][7]=tied.size()-j;}at=end;}
                orders.put(country,field.stream().mapToInt(Integer::intValue).toArray());
            }
            ArrayList<Integer> cupField=new ArrayList<>();for(int club=0;club<identities.length;club++)if(!reserves[club]&&association(identities[club]).equals(country))cupField.add(club);
            int winner=country.equals(local)?localCup:cupWinner(cupField,strengths,random);
            if(winner>=0){if(winner>=identities.length||reserves[winner]||!association(identities[winner]).equals(country))throw new IllegalArgumentException("Invalid domestic cup winner");cups.put(country,winner);}
            if(country.equals("ENG")){int leagueWinner=country.equals(local)?localLeagueCup:cupWinner(cupField,strengths,random);if(leagueWinner<0||leagueWinner>=identities.length||reserves[leagueWinner]||!association(identities[leagueWinner]).equals(country))throw new IllegalArgumentException("Missing English League Cup winner");leagueCups.put(country,leagueWinner);}
        }
        if(!orders.containsKey(local)||!cups.containsKey(local))throw new IllegalArgumentException("Missing completed local competition");
    }
    private EuropeanDomesticSeason(int year,String[] identities,int[][] table,SortedMap<String,int[]> orders,SortedMap<String,Integer> cups,SortedMap<String,Integer> leagueCups){this.year=year;this.identities=identities;this.table=table;this.orders.putAll(orders);this.cups.putAll(cups);this.leagueCups.putAll(leagueCups);}
    private void validateOrder(int[] order,String country,boolean[] reserves){Set<Integer> seen=new HashSet<>();for(int club:order)if(club<0||club>=identities.length||reserves[club]||!association(identities[club]).equals(country)||!seen.add(club))throw new IllegalArgumentException("Invalid final domestic order");}
    public static String association(String identity){return identity.substring(0,identity.indexOf(':')).toUpperCase(Locale.ROOT);}
    private static int goals(Random random,int advantage){int goals=0;for(int i=0;i<5;i++)if(random.nextInt(100)<Math.max(7,Math.min(65,24+advantage/2)))goals++;return goals;}
    private static int cupWinner(List<Integer> field,int[] strengths,Random random){ArrayList<Integer> round=new ArrayList<>(field);if(round.isEmpty())return -1;Collections.shuffle(round,random);while(round.size()>1){ArrayList<Integer> next=new ArrayList<>();for(int i=0;i<round.size();i+=2){if(i+1==round.size()){next.add(round.get(i));continue;}int h=round.get(i),a=round.get(i+1),hg=goals(random,strengths[h]-strengths[a]),ag=goals(random,strengths[a]-strengths[h]);next.add(hg>ag?h:ag>hg?a:random.nextBoolean()?h:a);}round=next;}return round.get(0);}
    private void record(int h,int a,int hg,int ag){for(int c:new int[]{h,a})table[c][0]++;table[h][4]+=hg;table[h][5]+=ag;table[a][4]+=ag;table[a][5]+=hg;if(hg>ag){table[h][1]++;table[a][3]++;table[h][6]+=3;}else if(ag>hg){table[a][1]++;table[h][3]++;table[a][6]+=3;}else{table[h][2]++;table[a][2]++;table[h][6]++;table[a][6]++;}}
    private int compare(int a,int b){for(int criterion:new int[]{6,7,4,1}){int av=criterion==7?table[a][4]-table[a][5]:table[a][criterion],bv=criterion==7?table[b][4]-table[b][5]:table[b][criterion];int result=Integer.compare(bv,av);if(result!=0)return result;}return 0;}
    public Set<String> associations(){return Collections.unmodifiableSet(orders.keySet());}
    public int[] order(String country){int[] order=orders.get(country);return order==null?new int[0]:order.clone();}
    public int cup(String country){return cups.getOrDefault(country,-1);}
    public int leagueCup(String country){return leagueCups.getOrDefault(country,-1);}
    public int[] totals(int club){return table[club].clone();}
    public String snapshot(){StringBuilder out=new StringBuilder("UD1|").append(year).append('|').append(identities.length);for(int i=0;i<identities.length;i++){out.append("\nC|").append(identities[i]);for(int total:table[i])out.append('|').append(total);}for(String country:orders.keySet()){out.append("\nA|").append(country).append('|').append(cup(country)).append('|').append(leagueCup(country));for(int club:orders.get(country))out.append('|').append(club);}return out.toString();}
    public static EuropeanDomesticSeason restore(String text,String[] ids,boolean[] reserves){
        if(text==null||text.length()>196608||ids==null||reserves==null||ids.length!=reserves.length)throw new IllegalArgumentException("Invalid domestic qualification snapshot");String[] rows=text.split("\n",-1),h=rows[0].split("\\|",-1);if(h.length!=3||!h[0].equals("UD1")||Integer.parseInt(h[2])!=ids.length||rows.length<ids.length+2)throw new IllegalArgumentException("Invalid qualification registry");int year=Integer.parseInt(h[1]);if(year<2026||year>2200)throw new IllegalArgumentException("Invalid qualification year");int[][] table=new int[ids.length][8];
        for(int i=0;i<ids.length;i++){String[] c=rows[i+1].split("\\|",-1);if(c.length!=10||!c[0].equals("C")||!c[1].equals(ids[i]))throw new IllegalArgumentException("Changed qualification club identity");for(int j=0;j<8;j++){table[i][j]=Integer.parseInt(c[j+2]);if(table[i][j]<0||table[i][j]>100000)throw new IllegalArgumentException("Invalid domestic total");}if(table[i][0]!=table[i][1]+table[i][2]+table[i][3]||table[i][6]!=3*table[i][1]+table[i][2])throw new IllegalArgumentException("Conflicting domestic totals");}
        SortedMap<String,int[]> orders=new TreeMap<>();SortedMap<String,Integer> cups=new TreeMap<>(),leagueCups=new TreeMap<>();Set<Integer> seen=new HashSet<>();
        for(int i=ids.length+1;i<rows.length;i++){String[] c=rows[i].split("\\|",-1);if(c.length<5||!c[0].equals("A")||!c[1].matches("[A-Z]{2,4}")||orders.containsKey(c[1]))throw new IllegalArgumentException("Invalid domestic ranking");int[] order=new int[c.length-4];for(int j=0;j<order.length;j++){int club=Integer.parseInt(c[j+4]);if(club<0||club>=ids.length||reserves[club]||!association(ids[club]).equals(c[1])||!seen.add(club))throw new IllegalArgumentException("Invalid domestic entrant");order[j]=club;}orders.put(c[1],order);for(int j=2;j<=3;j++){int club=Integer.parseInt(c[j]);if(club>=0){if(club>=ids.length||reserves[club]||!association(ids[club]).equals(c[1]))throw new IllegalArgumentException("Invalid saved cup winner");(j==2?cups:leagueCups).put(c[1],club);}else if(club!=-1)throw new IllegalArgumentException("Invalid cup outcome");}}
        EuropeanDomesticSeason out=new EuropeanDomesticSeason(year,ids.clone(),table,orders,cups,leagueCups);if(!out.snapshot().equals(text))throw new IllegalArgumentException("Non-canonical domestic outcomes");return out;
    }
}

package com.projectmister.game;

import java.util.*;

/** Stable local membership after parent/reserve collisions. Lower pool results are career simulations. */
public final class ReserveTierOutcome {
    public final int[] levels;
    public final List<String> rulings;
    public ReserveTierOutcome(String country,String[] ids,int[] original,boolean[] reserves,int[] strengths,
                              int[] promoted,int[] relegated,long seed) {
        int n=ids.length;
        if(original.length!=n||reserves.length!=n||strengths.length!=n||promoted.length!=relegated.length)
            throw new IllegalArgumentException("Invalid reserve transition inputs");
        levels=original.clone();ArrayList<String> notes=new ArrayList<>();Set<Integer> moved=new HashSet<>();
        for(int c:promoted){if(c<0||c>=n||original[c]!=2||reserves[c]||!moved.add(c))throw new IllegalArgumentException("Ineligible promotion");levels[c]=1;}
        for(int c:relegated){if(c<0||c>=n||original[c]!=1||!moved.add(c))throw new IllegalArgumentException("Invalid relegation");levels[c]=2;}
        int vacancies=0;
        for(int c=0;c<n;c++)if(local(ids[c],country)&&reserves[c]&&levels[c]<=2) {
            String parent=ReserveEligibility.parent(ids[c]);int p=-1;for(int i=0;i<n;i++)if(ids[i].equals(parent))p=i;
            if(p>=0&&levels[c]<=levels[p]){levels[c]=levels[p]+1;vacancies++;notes.add(ids[c]+" moves below its parent "+ids[p]+".");}
        }
        if(vacancies>0) {
            ArrayList<Integer> field=new ArrayList<>();
            for(int c=0;c<n;c++)if(local(ids[c],country)&&original[c]==3&&!reserves[c])field.add(c);
            // Simulate the available third-tier qualification pool once; retain the resulting members in the save.
            int[][] totals=new int[n][4];Random random=new Random(seed);
            for(int h:field)for(int a:field)if(h!=a){int hg=goals(random,strengths[h]-strengths[a]+4),ag=goals(random,strengths[a]-strengths[h]);totals[h][0]+=hg>ag?3:hg==ag?1:0;totals[a][0]+=ag>hg?3:hg==ag?1:0;totals[h][1]+=hg-ag;totals[a][1]+=ag-hg;totals[h][2]+=hg;totals[a][2]+=ag;}
            Collections.shuffle(field,random);for(int i=0;i<field.size();i++)totals[field.get(i)][3]=i;
            field.sort((a,b)->{for(int criterion=0;criterion<4;criterion++){int c=Integer.compare(totals[b][criterion],totals[a][criterion]);if(c!=0)return c;}return 0;});
            for(int c:field)if(vacancies>0&&levels[c]==3&&ReserveEligibility.eligibleAt(ids,levels,levels,c,2)){levels[c]=2;vacancies--;notes.add(ids[c]+" fills a reserve-team vacancy from simulated lower qualification results.");}
            if(vacancies!=0)throw new IllegalStateException("Not enough eligible lower clubs to replace forced reserves");
        }
        for(int tier=1;tier<=2;tier++){int before=0,after=0;for(int c=0;c<n;c++)if(local(ids[c],country)){if(original[c]==tier)before++;if(levels[c]==tier)after++;}if(before!=after)throw new IllegalStateException("Unbalanced reserve transition");}
        rulings=Collections.unmodifiableList(notes);
    }
    public static Long restoreSeed(String text){if(text==null||text.isEmpty())return null;if(text.length()>20)throw new IllegalArgumentException("Invalid reserve draw seed");long seed=Long.parseLong(text);if(!Long.toString(seed).equals(text))throw new IllegalArgumentException("Non-canonical reserve draw seed");return seed;}
    private static boolean local(String id,String country){return id.startsWith(country.toLowerCase(Locale.ROOT)+":");}
    private static int goals(Random random,int advantage){int count=0;double chance=Math.max(.10,Math.min(.45,.22+advantage*.003));for(int i=0;i<6;i++)if(random.nextDouble()<chance)count++;return count;}
}

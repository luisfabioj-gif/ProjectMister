package com.projectmister.game;

import java.util.*;

/** Country-specific ties: Portuguese drawn legs; Spanish higher-seed advancement after extra time. */
public final class IberianPromotion implements PromotionCampaign {
    private final String country;
    private final int[] lower,upper;
    private final boolean upperFirst;
    private final ArrayList<KnockoutTie> games=new ArrayList<>();
    public IberianPromotion(String country,int[] eligibleLower,int[] upperEntrants,boolean upperFirst) {
        if(!country.equals("PT")&&!country.equals("ES"))throw new IllegalArgumentException("Unsupported promotion country");
        if(eligibleLower.length!=(country.equals("PT")?3:6)||upperEntrants.length!=3)throw new IllegalArgumentException("Wrong field size");
        Set<Integer> seen=new HashSet<>();for(int[] field:new int[][]{eligibleLower,upperEntrants})for(int id:field)if(id<0||!seen.add(id))throw new IllegalArgumentException("Invalid entrant");
        this.country=country;lower=eligibleLower.clone();upper=upperEntrants.clone();this.upperFirst=upperFirst;
        if(country.equals("PT"))games.add(new KnockoutTie(upperFirst?upper[0]:lower[2],upperFirst?lower[2]:upper[0],2,upper[0],false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES));
        else games.add(spanishTie(lower[2],lower[5]));
    }
    private int seed(int id){for(int i=0;i<lower.length;i++)if(lower[i]==id)return i;throw new IllegalArgumentException("Unknown club");}
    private KnockoutTie spanishTie(int a,int b){int high=seed(a)<seed(b)?a:b,low=high==a?b:a;return new KnockoutTie(low,high,2,high,false,KnockoutTie.Rule.EXTRA_TIME_HIGHER_SEED);}
    private void advance(){if(country.equals("PT")||games.get(games.size()-1).winner()<0||games.size()==3)return;
        games.add(games.size()==1?spanishTie(lower[3],lower[4]):spanishTie(games.get(0).winner(),games.get(1).winner()));}
    public String country(){return country;}
    public int[] automaticPromoted(){return Arrays.copyOf(lower,2);}
    public int[] automaticRelegated(){return country.equals("ES")?upper.clone():new int[]{upper[1],upper[2]};}
    public int[] upperEntrants(){return upper.clone();}
    public int[] lowerEntrants(){return lower.clone();}
    public String roundName(int i){return country.equals("PT")?"Liga Portugal promotion/relegation play-off":i<2?"Promotion semi-final "+(i+1):"Promotion final";}
    public KnockoutTie current(){advance();KnockoutTie last=games.get(games.size()-1);return last.winner()>=0&&(country.equals("PT")||games.size()==3)?null:last;}
    public boolean complete(){return current()==null;}
    public List<KnockoutTie> ties(){advance();return Collections.unmodifiableList(games);}
    public int[] promoted(){if(!complete())throw new IllegalStateException("Unfinished promotion");int winner=games.get(games.size()-1).winner();return country.equals("PT")&&winner==upper[0]?automaticPromoted():new int[]{lower[0],lower[1],winner};}
    public int[] relegated(){if(!complete())throw new IllegalStateException("Unfinished promotion");return country.equals("ES")||games.get(0).winner()!=upper[0]?upper.clone():automaticRelegated();}
    public String snapshot(){advance();StringBuilder s=new StringBuilder(country+"#1;"+(upperFirst?1:0));for(int id:lower)s.append(';').append(id);for(int id:upper)s.append(';').append(id);for(KnockoutTie t:games)s.append('#').append(t.snapshot());return s.toString();}
    public static IberianPromotion restore(String text){
        if(text==null||text.length()>3072)throw new IllegalArgumentException("Invalid promotion data");
        String[] rows=text.split("#",-1);if(rows.length<3||rows.length>5)throw new IllegalArgumentException("Invalid campaign");
        String country=rows[0];int n=country.equals("PT")?3:6;String[] h=rows[1].split(";",-1);
        if(h.length!=n+5||!h[0].equals("1")||!h[1].matches("[01]"))throw new IllegalArgumentException("Invalid promotion header");
        int[] lower=new int[n],upper=new int[3];for(int i=0;i<n;i++)lower[i]=Integer.parseInt(h[i+2]);for(int i=0;i<3;i++)upper[i]=Integer.parseInt(h[i+n+2]);
        IberianPromotion p=new IberianPromotion(country,lower,upper,h[1].equals("1"));
        for(int i=2;i<rows.length;i++){
            KnockoutTie expected=p.current(),saved=KnockoutTie.restore(rows[i]);
            if(expected==null||saved.firstHome!=expected.firstHome||saved.firstAway!=expected.firstAway||saved.higherSeed!=expected.higherSeed||saved.legs!=2||saved.neutral||saved.rule!=expected.rule)throw new IllegalArgumentException("Altered promotion fixture");
            p.games.set(i-2,saved);if(i<rows.length-1&&saved.winner()<0)throw new IllegalArgumentException("Incomplete earlier tie");
        }
        return p;
    }
}

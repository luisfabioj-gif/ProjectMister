package com.projectmister.game;
import java.util.*;

/** 2026/27: eligible champion plus the winner of seeded two-leg 2–5 playoffs. */
public final class BelgianPromotion implements PromotionCampaign {
    private final int[] lower,upper;
    private final ArrayList<KnockoutTie> games=new ArrayList<>();
    public BelgianPromotion(int[] eligibleLower,int[] relegatedUpper) {
        if(eligibleLower.length!=5||relegatedUpper.length!=2)throw new IllegalArgumentException("Wrong field size");
        Set<Integer> seen=new HashSet<>();for(int[] field:new int[][]{eligibleLower,relegatedUpper})for(int id:field)if(id<0||!seen.add(id))throw new IllegalArgumentException("Invalid entrant");
        lower=eligibleLower.clone();upper=relegatedUpper.clone();games.add(tie(lower[1],lower[4]));
    }
    private int seed(int club){for(int i=0;i<lower.length;i++)if(lower[i]==club)return i;throw new IllegalArgumentException("Unknown club");}
    private KnockoutTie tie(int a,int b){int high=seed(a)<seed(b)?a:b,low=high==a?b:a;return new KnockoutTie(low,high,2,high,false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES);}
    private void advance(){if(games.get(games.size()-1).winner()<0||games.size()==3)return;
        games.add(games.size()==1?tie(lower[2],lower[3]):tie(games.get(0).winner(),games.get(1).winner()));}
    public String country(){return "BE";}
    public int[] automaticPromoted(){return new int[]{lower[0]};}
    public int[] automaticRelegated(){return upper.clone();}
    public int[] upperEntrants(){return upper.clone();}
    public int[] lowerEntrants(){return lower.clone();}
    public String roundName(int i){return i<2?"Promotion semi-final "+(i+1):"Promotion final";}
    public KnockoutTie current(){advance();KnockoutTie last=games.get(games.size()-1);return last.winner()>=0&&games.size()==3?null:last;}
    public boolean complete(){return current()==null;}
    public List<KnockoutTie> ties(){advance();return Collections.unmodifiableList(games);}
    public int[] promoted(){if(!complete())throw new IllegalStateException("Unfinished promotion");return new int[]{lower[0],games.get(2).winner()};}
    public int[] relegated(){if(!complete())throw new IllegalStateException("Unfinished promotion");return upper.clone();}
    public String snapshot(){advance();StringBuilder s=new StringBuilder("BE#1");for(int id:lower)s.append(';').append(id);for(int id:upper)s.append(';').append(id);for(KnockoutTie t:games)s.append('#').append(t.snapshot());return s.toString();}
    public static BelgianPromotion restore(String text) {
        if(text==null||text.length()>3072)throw new IllegalArgumentException("Invalid promotion data");
        String[] rows=text.split("#",-1);if(rows.length<3||rows.length>5||!rows[0].equals("BE"))throw new IllegalArgumentException("Invalid campaign");
        String[] h=rows[1].split(";",-1);if(h.length!=8||!h[0].equals("1"))throw new IllegalArgumentException("Invalid header");
        int[] lower=new int[5],upper=new int[2];for(int i=0;i<5;i++)lower[i]=Integer.parseInt(h[i+1]);for(int i=0;i<2;i++)upper[i]=Integer.parseInt(h[i+6]);
        BelgianPromotion p=new BelgianPromotion(lower,upper);
        for(int i=2;i<rows.length;i++) {
            KnockoutTie expected=p.current(),saved=KnockoutTie.restore(rows[i]);
            if(expected==null||saved.firstHome!=expected.firstHome||saved.firstAway!=expected.firstAway||saved.higherSeed!=expected.higherSeed||saved.legs!=2||saved.neutral||saved.rule!=expected.rule)throw new IllegalArgumentException("Altered fixture");
            p.games.set(i-2,saved);if(i<rows.length-1&&saved.winner()<0)throw new IllegalArgumentException("Incomplete earlier tie");
        }
        return p;
    }
}

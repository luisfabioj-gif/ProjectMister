package com.projectmister.game;

import java.util.*;

/** KNVB 2026/27: six period/table qualifiers and Eredivisie 16, all ties over two legs. */
public final class DutchPromotion implements PromotionCampaign {
    private final int[] lower,upper;
    private final ArrayList<KnockoutTie> games=new ArrayList<>();
    public DutchPromotion(int[] lowerEight,int[] upperBottomThree) {
        if(lowerEight.length!=8||upperBottomThree.length!=3)throw new IllegalArgumentException("Wrong field size");
        Set<Integer> seen=new HashSet<>();
        for(int[] field:new int[][]{lowerEight,upperBottomThree})for(int id:field)
            if(id<0||!seen.add(id))throw new IllegalArgumentException("Invalid entrant");
        lower=lowerEight.clone();upper=upperBottomThree.clone();
        games.add(tie(lower[7],lower[2]));
    }
    private int seed(int club){if(club==upper[0])return -1;for(int i=2;i<8;i++)if(lower[i]==club)return i;throw new IllegalArgumentException("Unknown entrant");}
    private KnockoutTie tie(int a,int b){int high=seed(a)<seed(b)?a:b,low=high==a?b:a;return new KnockoutTie(low,high,2,high,false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES);}
    private void advance() {
        KnockoutTie last=games.get(games.size()-1);
        if(last.winner()<0||games.size()==6)return;
        switch(games.size()) {
            case 1:games.add(tie(lower[6],lower[3]));break;
            case 2:games.add(tie(lower[5],lower[4]));break;
            case 3:games.add(tie(games.get(0).winner(),games.get(1).winner()));break;
            case 4:games.add(tie(upper[0],games.get(2).winner()));break;
            default:games.add(tie(games.get(3).winner(),games.get(4).winner()));
        }
    }
    public String country(){return "NL";}
    public int[] automaticPromoted(){return new int[]{lower[0],lower[1]};}
    public int[] automaticRelegated(){return new int[]{upper[1],upper[2]};}
    public int[] upperEntrants(){return upper.clone();}
    public int[] lowerEntrants(){return lower.clone();}
    public String roundName(int i){return i<3?"Promotion first round "+(i+1):i<5?"Promotion semi-final "+(i-2):"Promotion final";}
    public KnockoutTie current(){advance();KnockoutTie last=games.get(games.size()-1);return last.winner()>=0&&games.size()==6?null:last;}
    public boolean complete(){return current()==null;}
    public List<KnockoutTie> ties(){advance();return Collections.unmodifiableList(games);}
    public int[] promoted(){if(!complete())throw new IllegalStateException("Unfinished promotion");return games.get(5).winner()==upper[0]?automaticPromoted():new int[]{lower[0],lower[1],games.get(5).winner()};}
    public int[] relegated(){if(!complete())throw new IllegalStateException("Unfinished promotion");return games.get(5).winner()==upper[0]?automaticRelegated():upper.clone();}
    public String snapshot(){advance();StringBuilder s=new StringBuilder("NL#1");for(int id:lower)s.append(';').append(id);for(int id:upper)s.append(';').append(id);for(KnockoutTie t:games)s.append('#').append(t.snapshot());return s.toString();}
    public static DutchPromotion restore(String text) {
        if(text==null||text.length()>4096)throw new IllegalArgumentException("Invalid promotion data");
        String[] rows=text.split("#",-1);
        if(rows.length<3||rows.length>8||!rows[0].equals("NL"))throw new IllegalArgumentException("Invalid campaign");
        String[] h=rows[1].split(";",-1);if(h.length!=12||!h[0].equals("1"))throw new IllegalArgumentException("Invalid header");
        int[] lower=new int[8],upper=new int[3];for(int i=0;i<8;i++)lower[i]=Integer.parseInt(h[i+1]);for(int i=0;i<3;i++)upper[i]=Integer.parseInt(h[i+9]);
        DutchPromotion p=new DutchPromotion(lower,upper);
        for(int i=2;i<rows.length;i++) {
            KnockoutTie expected=p.current(),saved=KnockoutTie.restore(rows[i]);
            if(expected==null||saved.firstHome!=expected.firstHome||saved.firstAway!=expected.firstAway||saved.higherSeed!=expected.higherSeed||saved.legs!=expected.legs||saved.neutral||saved.rule!=expected.rule)throw new IllegalArgumentException("Altered fixture");
            p.games.set(i-2,saved);if(i<rows.length-1&&saved.winner()<0)throw new IllegalArgumentException("Incomplete earlier tie");
        }
        return p;
    }
}

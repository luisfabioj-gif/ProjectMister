package com.projectmister.game;

public final class EnglishTests {
    static void check(boolean b){if(!b)throw new AssertionError();}
    static PromotionCampaign reload(PromotionCampaign p){String s=p.snapshot();PromotionCampaign r=PromotionCampaign.restore(s);check(r.snapshot().equals(s));return r;}
    public static void main(String[] args) {
        for(int mask=0;mask<32;mask++) {
            PromotionCampaign p=new EnglishPromotion(new int[]{20,21,22,23,24,25,26,27},new int[]{17,18,19});
            int[] winners=new int[5];int fixtures=0;
            for(int round=0;round<5;round++) {
                KnockoutTie t=p.current();
                if(round==0)check(t.firstHome==24&&t.firstAway==27);
                if(round==1)check(t.firstHome==25&&t.firstAway==26);
                if(round==2)check(t.firstHome==Math.max(winners[0],winners[1])&&t.firstAway==22);
                if(round==3)check(t.firstHome==Math.min(winners[0],winners[1])&&t.firstAway==23);
                if(round==4)check(t.firstHome==winners[2]&&t.firstAway==winners[3]&&t.neutral);
                check(t.legs==(round==2||round==3?2:1));check(t.neutral==(round==4));
                int win=(mask&(1<<round))==0?t.firstHome:t.firstAway;winners[round]=win;
                while(t.phase()==KnockoutTie.Phase.REGULATION){t.recordRegulation(t.home()==win?2:0,t.away()==win?2:0);fixtures++;p=reload(p);t=p.ties().get(round);}
                p=reload(p);
            }
            check(fixtures==7&&p.complete());check(p.promoted().length==3&&p.promoted()[2]==winners[4]&&p.relegated().length==3);
        }
        PromotionCampaign p=new EnglishPromotion(new int[]{20,21,22,23,24,25,26,27},new int[]{17,18,19});
        while(!p.complete()) {
            KnockoutTie t=p.current();
            if(t.phase()==KnockoutTie.Phase.REGULATION)t.recordRegulation(0,0);
            else if(t.phase()==KnockoutTie.Phase.EXTRA_TIME)t.recordExtraTime(0,0);else t.recordPenalties(4,5);
            p=reload(p);
        }
        try{PromotionCampaign.restore(p.snapshot().replace("EXTRA_TIME_PENALTIES","PENALTIES"));throw new AssertionError();}catch(IllegalArgumentException|IllegalStateException expected){}
        int[] pts={30,30,0},gf={40,40,0},ga={20,20,0},wins={8,9,0};Integer[] pair={0,1};
        LeagueResults ledger=new LeagueResults(3,true);ledger.record(0,0,1,3,0);ledger.record(1,1,0,2,1);
        EnglishStandings pl=new EnglishStandings(1,pair,pts,gf,ga,wins,ledger,true);
        check(pl.order[0]==0); // equal direct points: away goals, not wins
        check(new EnglishStandings(1,pair,pts,gf,ga,wins,ledger,false).tiedAt(0));
        check(new EnglishStandings(2,pair,pts,gf,ga,wins,ledger,true).order[0]==0); // direct GD before wins
        ledger=new LeagueResults(3,true);ledger.record(0,0,1,0,0);ledger.record(1,1,0,0,0);
        check(new EnglishStandings(2,pair,pts,gf,ga,wins,ledger,true).order[0]==1); // EFL wins, unlike PL
        check(new EnglishStandings(1,pair,pts,gf,ga,wins,ledger,true).tiedAt(0));
        wins[1]=8;ledger.record(2,2,1,0,1);
        check(new EnglishStandings(2,pair,pts,gf,ga,wins,ledger,true).order[0]==1); // season away GF
        check(new EnglishStandings(2,pair,pts,gf,ga,wins,new LeagueResults(3,false),true).tiedAt(0));
        gf[0]=41;ga[0]=21;check(new EnglishStandings(2,pair,pts,gf,ga,wins,ledger,true).order[0]==0); // overall GF first
        System.out.println("PASS: English 32 paths, quarter-final reseeding, seven fixtures, neutral final, saved phases and tier-specific ranking");
    }
}

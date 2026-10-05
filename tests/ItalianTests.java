package com.projectmister.game;

public final class ItalianTests {
    static final int[] LOWER={20,21,22,23,24,25,26,27}, UPPER={0,1,16,17,18,19};
    static final int[] POINTS={80,78,70,65,64,63,62,61};
    static void check(boolean value){if(!value)throw new AssertionError();}
    static ItalianPromotion reload(ItalianPromotion p){String s=p.snapshot();ItalianPromotion r=(ItalianPromotion)PromotionCampaign.restore(s);check(r.snapshot().equals(s));return r;}
    static ItalianPromotion win(ItalianPromotion p,int winner){int round=p.ties().size()-1;KnockoutTie t=p.current();while(t.phase()==KnockoutTie.Phase.REGULATION){t.recordRegulation(t.home()==winner?2:0,t.away()==winner?2:0);p=reload(p);t=p.ties().get(round);}check(t.winner()==winner);return p;}
    public static void main(String[] args) {
        for(int mask=0;mask<32;mask++) {
            ItalianPromotion p=new ItalianPromotion(LOWER,POINTS,UPPER,false,false);int[] winners=new int[5];int legs=0;
            for(int r=0;r<5;r++) {
                KnockoutTie t=p.current();check(!t.neutral);legs+=t.legs;
                if(r==0)check(t.firstHome==24&&t.firstAway==27);
                if(r==1)check(t.firstHome==25&&t.firstAway==26);
                if(r==2)check(t.firstHome==winners[1]&&t.firstAway==22);
                if(r==3)check(t.firstHome==winners[0]&&t.firstAway==23);
                if(r==4)check(t.firstHome==Math.max(winners[2],winners[3])&&t.firstAway==Math.min(winners[2],winners[3]));
                winners[r]=(mask&(1<<r))==0?t.firstHome:t.firstAway;p=win(p,winners[r]);
            }
            check(legs==8&&p.complete()&&p.promoted()[2]==winners[4]);check(p.relegated()[0]==17&&p.champion()==0);
        }
        for(int flags=0;flags<4;flags++) {
            ItalianPromotion mixed=new ItalianPromotion(LOWER,POINTS,UPPER,(flags&1)!=0,(flags&2)!=0);
            while(!mixed.complete())mixed=win(mixed,mixed.current().firstAway);
            check(mixed.ties().size()==5+Integer.bitCount(flags)&&mixed.promoted().length==mixed.relegated().length);
            mixed=reload(mixed);check(mixed.complete());
        }
        // A fourteen-point gap still requires playoffs; fifteen promotes third automatically.
        int[] pts=POINTS.clone();pts[2]=79;pts[1]=80;pts[0]=81;
        ItalianPromotion p=new ItalianPromotion(LOWER,pts,UPPER,false,false);check(!p.complete());
        pts[2]=80;p=reload(new ItalianPromotion(LOWER,pts,UPPER,false,false));check(p.complete()&&p.ties().isEmpty()&&p.promoted()[2]==22);
        // Serie A deciders: one title leg, two survival legs, neither uses extra time.
        p=new ItalianPromotion(LOWER,pts,UPPER,true,true);
        while(!p.complete()) {
            KnockoutTie t=p.current();int r=p.ties().size()-1;
            check(t.rule==KnockoutTie.Rule.PENALTIES&&t.legs==(r==0?1:2));
            if(r==0)check(t.firstHome==0&&t.firstAway==1);else check(t.firstHome==17&&t.firstAway==16);
            if(t.phase()==KnockoutTie.Phase.REGULATION)t.recordRegulation(0,0);else t.recordPenalties(4,5);
            p=reload(p);
        }
        check(p.champion()==1&&p.relegated()[0]==16&&p.promoted().length==3);
        // Drawn preliminary => extra time then higher seed; drawn semis/final => higher seed.
        p=new ItalianPromotion(LOWER,POINTS,UPPER,false,false);
        while(!p.complete()) {
            KnockoutTie t=p.current();int r=p.ties().size()-1;
            if(t.phase()==KnockoutTie.Phase.REGULATION)t.recordRegulation(0,0);
            else {check(r<2&&t.phase()==KnockoutTie.Phase.EXTRA_TIME);t.recordExtraTime(0,0);}
            p=reload(p);
        }
        check(p.promoted()[2]==22&&p.ties().get(4).extraTimeScore()[0]==-1);
        // Equal league points in the final remove the higher-seed shortcut.
        pts=POINTS.clone();pts[3]=pts[2];p=new ItalianPromotion(LOWER,pts,UPPER,false,false);
        for(int i=0;i<4;i++)p=win(p,p.current().higherSeed);
        check(p.current().rule==KnockoutTie.Rule.EXTRA_TIME_PENALTIES);
        p.current().recordRegulation(1,1);p=reload(p);p.current().recordRegulation(1,1);p=reload(p);
        check(p.current().phase()==KnockoutTie.Phase.EXTRA_TIME);p.current().recordExtraTime(0,0);p=reload(p);
        check(p.current().phase()==KnockoutTie.Phase.PENALTIES);p.current().recordPenalties(3,4);p=reload(p);
        check(p.promoted()[2]==23&&p.ties().get(4).aggregate(23)==2);
        try{ItalianPromotion.restore(p.snapshot().replace(";EXTRA_TIME_PENALTIES",";HIGHER_SEED"));throw new AssertionError();}catch(IllegalArgumentException|IllegalStateException expected){}
        try{ItalianPromotion.restore(p.snapshot()+"#"+p.ties().get(4).snapshot());throw new AssertionError();}catch(IllegalArgumentException|IllegalStateException expected){}
        // National table: head-to-head points and GD precede overall goal difference.
        int[] points={30,30,0},gf={50,30,0},ga={10,20,0},wins={10,10,0};Integer[] pair={0,1};
        LeagueResults ledger=new LeagueResults(3,true);ledger.record(0,0,1,0,1);ledger.record(1,1,0,1,0);
        ItalianStandings s=new ItalianStandings(pair,points,gf,ga,wins,ledger,true);check(s.order[0]==1);
        ledger=new LeagueResults(3,true);ledger.record(0,0,1,1,0);ledger.record(1,1,0,3,0);
        s=new ItalianStandings(pair,points,gf,ga,wins,ledger,true);check(s.order[0]==1);
        ledger=new LeagueResults(3,true);ledger.record(0,0,1,1,0);ledger.record(1,1,0,1,0);
        s=new ItalianStandings(pair,points,gf,ga,wins,ledger,true);check(s.order[0]==0);
        gf[1]=50;ga[1]=10;s=new ItalianStandings(pair,points,gf,ga,wins,ledger,true);check(s.tiedAt(0)&&s.rankAt(1)==1);
        s=new ItalianStandings(pair,points,gf,ga,wins,new LeagueResults(3,false),true);check(s.tiedAt(0));
        System.out.println("PASS: Italian 32 playoff paths, 14-point boundary, equal-points final, title/survival deciders, phase restores and national standings");
    }
}

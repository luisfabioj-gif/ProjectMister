package com.projectmister.game;

import java.util.*;

/** Annex D match and bonus points, in thousandths. Shootouts do not change match points. */
public final class EuropeanPerformance {
    private EuropeanPerformance(){}
    public static int bonus(EuropeanLeaguePhase.Competition competition,int rank) {
        if(rank<1||rank>36)throw new IllegalArgumentException("Invalid UEFA rank");
        if(competition==EuropeanLeaguePhase.Competition.CHAMPIONS)return Math.max(6000,12250-rank*250);
        if(competition==EuropeanLeaguePhase.Competition.EUROPA)return Math.max(0,6250-rank*250);
        return rank<=9?4250-rank*250:Math.max(0,3125-rank*125);
    }
    public static int[] points(EuropeanCampaign campaign,int clubCount) {
        if(campaign==null||!campaign.complete()||clubCount<108)throw new IllegalArgumentException("Complete UEFA results required");
        int[] points=new int[clubCount];
        for(EuropeanSeasons editions:campaign.competitions()) {
            EuropeanSeason season=editions.active();EuropeanLeaguePhase phase=season.leaguePhase();int[] order=phase.order();
            for(int i=0;i<order.length;i++)points[order[i]]+=bonus(phase.competition,i+1);
            for(int i=0;i<phase.fixtureCount();i++){EuropeanLeaguePhase.Fixture f=phase.fixture(i);int[] r=phase.result(i);award(points,f.home,f.away,r[0],r[1],2000);}
            EuropeanKnockout cup=season.knockout();int bonus=phase.competition==EuropeanLeaguePhase.Competition.CHAMPIONS?1500:phase.competition==EuropeanLeaguePhase.Competition.EUROPA?1000:500;
            for(int i=0;i<cup.drawnCount();i++) {
                KnockoutTie tie=cup.at(i);if(EuropeanKnockout.stageFor(i)>0){points[tie.firstHome]+=bonus;points[tie.firstAway]+=bonus;}
                for(int leg=0;leg<tie.legs;leg++){int[] score=tie.regulationScore(leg);int[] extra=tie.extraTimeScore();if(leg==tie.legs-1&&extra[0]>=0){score[0]+=extra[0];score[1]+=extra[1];}int home=tie.homeForLeg(leg),away=home==tie.firstHome?tie.firstAway:tie.firstHome;award(points,home,away,score[0],score[1],2000);}
            }
        }return points;
    }
    static void award(int[] points,int home,int away,int homeGoals,int awayGoals,int win) {
        if(home<0||away<0||home>=points.length||away>=points.length)throw new IllegalArgumentException("Invalid UEFA performance registry");
        if(homeGoals>awayGoals)points[home]+=win;else if(awayGoals>homeGoals)points[away]+=win;else{points[home]+=win/2;points[away]+=win/2;}
    }
    public static Map<String,Integer> associationPoints(EuropeanCampaign campaign,String[] identities) {
        return associationPoints(campaign,identities,Collections.emptyMap());
    }
    public static Map<String,Integer> associationPoints(EuropeanCampaign campaign,String[] identities,Map<String,Integer> initialCounts) {
        int[] points=points(campaign,identities.length);Map<String,Integer> sums=new TreeMap<>(),counts=new TreeMap<>();
        EuropeanQualifying qualifying=campaign.qualifying();
        if(qualifying!=null){int[] qualifiers=qualifying.associationMatchPoints();for(EuropeanAdmissions.Entry entry:qualifying.admissions().entries()){String association=EuropeanDomesticSeason.association(identities[entry.club]);counts.merge(association,1,Integer::sum);sums.merge(association,qualifiers[entry.club]+points[entry.club],Integer::sum);}}
        else{for(EuropeanSeasons editions:campaign.competitions())for(EuropeanLeaguePhase.Club club:editions.active().leaguePhase().clubs()){sums.merge(club.association,points[club.id],Integer::sum);counts.merge(club.association,1,Integer::sum);}for(String association:initialCounts.keySet()){Integer n=initialCounts.get(association);if(n==null||n<counts.getOrDefault(association,0))throw new IllegalArgumentException("Invalid opening UEFA participant count");counts.put(association,n);sums.putIfAbsent(association,0);}}
        Map<String,Integer> result=new TreeMap<>();for(String association:sums.keySet())result.put(association,sums.get(association)/counts.get(association));return Collections.unmodifiableMap(result);
    }
}

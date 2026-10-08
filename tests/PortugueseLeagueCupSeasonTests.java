package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

public final class PortugueseLeagueCupSeasonTests {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void rejects(Runnable action){try{action.run();}catch(IllegalArgumentException|IllegalStateException e){return;}throw new AssertionError("Invalid League Cup state accepted");}
    private static int[] range(int n){int[] out=new int[n];for(int i=0;i<n;i++)out[i]=100+i*3;return out;}
    private static List<LocalDate> dates(int year){return Arrays.asList(LocalDate.of(year,7,25),LocalDate.of(year,8,1),LocalDate.of(year,8,8),LocalDate.of(year,9,5),LocalDate.of(year,10,27),LocalDate.of(year+1,1,5),LocalDate.of(year+1,1,6),LocalDate.of(year+1,1,9));}
    private static PortugueseLeagueCupSeason fresh(int year,int seed,int field,int direct) {
        int[] clubs=range(field),auto=Arrays.copyOf(clubs,direct),preliminary=(field-direct)%2==1?new int[]{clubs[field-2],clubs[field-1]}:new int[0];
        return new PortugueseLeagueCupSeason(year,seed,clubs,auto,preliminary,dates(year),true);
    }
    private static PortugueseLeagueCupSeason.PlayerUse[] players(int club,boolean sameAge,boolean changes) {
        PortugueseLeagueCupSeason.PlayerUse[] result=new PortugueseLeagueCupSeason.PlayerUse[changes?16:11];
        for(int i=0;i<result.length;i++)result[i]=new PortugueseLeagueCupSeason.PlayerUse(club*100+i,7000+(sameAge?0:club)+i);return result;
    }
    public static void main(String[] args) {
        int completed=0;
        for(int direct:new int[]{5,6,7})for(int field:new int[]{32,33,34})for(int seed=0;seed<8;seed++) {
            PortugueseLeagueCupSeason cup=fresh(2027+seed,seed,field,direct);int games=0;LocalDate previous=null;Set<Integer> participants=new HashSet<>();
            int[] playoffRank=null,qfField=null,sfWinners=new int[4];
            while(!cup.complete()) {
                PortugueseLeagueCupSeason.Fixture f=cup.current();check(previous==null||!f.date.isBefore(previous),"chronological cup stages");previous=f.date;participants.add(f.home);participants.add(f.away);
                if(f.league) {
                    check(cup.currentTie()==null,"league draws must not become knockouts");cup.recordLeague(0,0,players(f.home,false,false),players(f.away,false,false));
                }else {
                    if(f.stage==2) {
                        if(playoffRank==null)playoffRank=cup.order();int n=2*(8-direct);
                        check(f.home==playoffRank[f.index]&&f.away==playoffRank[n-1-f.index],"playoff best vs worst, best at home");
                    }
                    if(f.stage==3) {
                        if(qfField==null){qfField=new int[8];System.arraycopy(range(field),0,qfField,0,direct);System.arraycopy(playoffRank,0,qfField,direct,8-direct);}
                        check(f.home==qfField[f.index]&&f.away==qfField[7-f.index],"quarter-final seeding");sfWinners[f.index]=f.home;
                    }
                    if(f.stage==4)check(f.neutral&&f.home==sfWinners[f.index]&&f.away==sfWinners[f.index+2],"fixed neutral semifinal bracket");
                    cup.recordRegulation(1,1);cup=PortugueseLeagueCupSeason.restore(cup.snapshot());
                    check(cup.currentTie().phase()==KnockoutTie.Phase.PENALTIES,"direct penalties survive save");cup.recordPenalties(5,4);
                }
                cup=PortugueseLeagueCupSeason.restore(cup.snapshot());cup.validateDate(f.date);games++;
            }
            int leagueSize=field-direct-(field-direct)%2;
            check(games==leagueSize+8-direct+7+(field-direct)%2,"all scheduled matches played");check(participants.size()==field,"all qualified clubs participate");
            int[] clubs=cup.leagueClubs();for(int club:clubs) {
                int home=0,away=0;Set<Integer> opponents=new HashSet<>();
                for(int i=0;i<cup.leagueFixtureCount();i++){int[] f=cup.leagueFixture(i);if(f[0]==club){home++;opponents.add(f[1]);}if(f[1]==club){away++;opponents.add(f[0]);}}
                check(home==1&&away==1&&opponents.size()==2,"exactly one home and away against distinct clubs");
            }
            check(cup.winner()>=0,"champion retained");String save=cup.snapshot();check(save.equals(PortugueseLeagueCupSeason.restore(save).snapshot()),"full cup save round trip");completed++;
        }
        PortugueseLeagueCupSeason tied=fresh(2027,2,32,6);
        while(tied.leagueResult(tied.leagueFixtureCount()-1)==null) {
            PortugueseLeagueCupSeason.Fixture f=tied.current();tied.recordLeague(0,0,players(f.home,true,false),players(f.away,true,false));
        }
        String unresolved=tied.snapshot();check(unresolved.equals(PortugueseLeagueCupSeason.restore(unresolved).snapshot()),"unresolved ranking remains saveable");rejects(tied::current);
        check(tied.rankingDecisionRequired(),"unresolved sporting decision explicitly reported");tied.validateDate(LocalDate.of(2027,8,8));
        tied.resolveRanking(new PortugueseThirdDivisionSeason.Scores(){final Random random=new Random(9);public int goals(int h,int a,boolean advantage){return random.nextInt(4);}public PortugueseLeagueCupSeason.PlayerUse[] players(int c){return PortugueseLeagueCupSeasonTests.players(c,true,false);}});
        String decided=tied.snapshot();check(decided.contains("\nD|")&&!tied.rankingDecisionRequired(),"neutral sporting decisions settle equal qualification ranks");PortugueseLeagueCupSeason resolved=PortugueseLeagueCupSeason.restore(decided);check(decided.equals(resolved.snapshot()),"neutral decisions replay canonically");resolved.validateDate(LocalDate.of(2027,8,11));rejects(()->resolved.validateDate(LocalDate.of(2027,8,8)));check(resolved.current().stage==2,"playoff starts after saved neutral deciding games");
        PortugueseLeagueCupSeason ages=fresh(2027,7,32,6);int younger=ages.leagueClubs()[0],other=ages.leagueClubs()[1];Map<Integer,Integer> appearances=new HashMap<>();
        while(ages.leagueResult(ages.leagueFixtureCount()-1)==null) {
            PortugueseLeagueCupSeason.Fixture game=ages.current();PortugueseLeagueCupSeason.PlayerUse[][] played=new PortugueseLeagueCupSeason.PlayerUse[2][];
            int[] teams={game.home,game.away};
            for(int side=0;side<2;side++) {
                int team=teams[side],prior=appearances.getOrDefault(team,0);played[side]=new PortugueseLeagueCupSeason.PlayerUse[11];
                for(int i=0;i<11;i++) {
                    boolean newcomer=team==younger&&prior==1&&i>=6;int playerIndex=newcomer?i+5:i;
                    int age=team==younger?(newcomer?6570:10950):team==other?9746:16425;
                    played[side][i]=new PortugueseLeagueCupSeason.PlayerUse(team*100+playerIndex,age);
                }
                appearances.put(team,prior+1);
            }
            ages.recordLeague(0,0,played[0],played[1]);ages=PortugueseLeagueCupSeason.restore(ages.snapshot());
        }
        check(ages.order()[0]==younger&&ages.order()[1]==other,"age averages count each used player once, not each appearance");
        PortugueseLeagueCupSeason cup=fresh(2027,3,33,6);rejects(()->cup.recordLeague(0,0,players(100,false,false),players(103,false,false)));rejects(()->cup.recordPenalties(5,4));
        cup.recordRegulation(0,0);String pending=cup.snapshot();
        String bad=pending.replace("\nR|","\nX|");rejects(()->PortugueseLeagueCupSeason.restore(bad));
        cup.recordPenalties(5,4);PortugueseLeagueCupSeason.Fixture f=cup.current();
        PortugueseLeagueCupSeason.PlayerUse[] eleven=players(f.home,false,false);eleven[10]=eleven[0];String before=cup.snapshot();
        rejects(()->cup.recordLeague(1,0,eleven,players(f.away,false,false)));check(before.equals(cup.snapshot()),"invalid appearances leave state unchanged");
        rejects(()->new PortugueseLeagueCupSeason(2027,0,range(33),Arrays.copyOf(range(33),6),new int[0],dates(2027),true));
        rejects(()->new PortugueseLeagueCupSeason(2027,0,range(32),Arrays.copyOf(range(32),6),new int[]{190,193},dates(2027),true));
        rejects(()->cup.validateDate(LocalDate.of(2027,7,1)));
        PortugueseLeagueCupSeasons seasons=new PortugueseLeagueCupSeasons(fresh(2027,0,33,6));String firstArchive="";
        for(int year=2027;year<2037;year++) {
            PortugueseLeagueCupSeason next=fresh(year+1,year,33,6);final PortugueseLeagueCupSeasons unfinished=seasons;rejects(()->unfinished.next(next));
            PortugueseLeagueCupSeason active=seasons.active();
            while(!active.complete()) {
                PortugueseLeagueCupSeason.Fixture game=active.current();
                if(game.league)active.recordLeague(0,0,players(game.home,false,false),players(game.away,false,false));
                else active.recordRegulation(2,0);
            }
            if(year==2027)firstArchive=active.snapshot();
            seasons=PortugueseLeagueCupSeasons.restore(seasons.next(next).snapshot());
            check(seasons.archives().size()==year-2026&&seasons.archives().get(0).equals(firstArchive),"ten annual editions preserve first champion and every result");
            check(seasons.active().season==year+1&&seasons.active().snapshot().split("\n",-1).length==5,"newly qualified edition starts without results");
        }
        System.out.println("PASS: "+completed+" new-format Portuguese League Cups, conditional qualifiers, two-game league phases, used-player-age ties, seeded paths, shootout reloads and invalid-save rejection");
    }
}

package com.projectmister.game;

import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public final class EuropeanLeaguePhaseTests {
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static void rejects(Runnable r){try{r.run();}catch(IllegalArgumentException|IllegalStateException e){return;}throw new AssertionError("Invalid UEFA state accepted");}
    private static List<EuropeanLeaguePhase.Club> clubs(boolean conference,boolean equal){ArrayList<EuropeanLeaguePhase.Club> out=new ArrayList<>();for(int i=0;i<36;i++)out.add(new EuropeanLeaguePhase.Club(300+7*i,""+(char)('A'+i/26)+(char)('A'+i%26),conference?i/6:i%4,equal?0:100000-i*100));return out;}
    private static List<EuropeanLeaguePhase.Fixture> draw(boolean conference)throws Exception {
        List<EuropeanLeaguePhase.Fixture> out=new ArrayList<>();for(String line:Files.readAllLines(Paths.get("tests/fixtures/"+(conference?"conference":"champions")+"-synthetic-draw.csv"))){String[] p=line.split(",");out.add(new EuropeanLeaguePhase.Fixture(Integer.parseInt(p[0]),300+7*Integer.parseInt(p[1]),300+7*Integer.parseInt(p[2])));}return out;
    }
    private static List<LocalDate> dates(boolean conference){List<LocalDate> dates=new ArrayList<>();for(int r=0;r<(conference?6:8);r++)dates.add(LocalDate.of(2026,9,10).plusWeeks(r*2L));return dates;}
    private static EuropeanLeaguePhase fresh(EuropeanLeaguePhase.Competition c,boolean equal)throws Exception {boolean co=c==EuropeanLeaguePhase.Competition.CONFERENCE;return new EuropeanLeaguePhase(c,2026,clubs(co,equal),draw(co),dates(co));}
    public static void main(String[] args)throws Exception {
        for(EuropeanLeaguePhase.Competition competition:EuropeanLeaguePhase.Competition.values())for(int seed=0;seed<12;seed++) {
            EuropeanLeaguePhase phase=fresh(competition,false);Random random=new Random(seed);
            rejects(phase::qualification);
            for(int i=0;i<phase.fixtureCount();i++) {
                int h=random.nextInt(5),a=random.nextInt(4),hd=random.nextInt(6),ad=random.nextInt(6);
                check(phase.record(i,h,a,hd,ad)&&!phase.record(i,h,a,hd,ad),"UEFA result idempotence");
                if(i%18==17){phase=EuropeanLeaguePhase.restore(phase.snapshot());phase.validateDate(phase.date(i/18));}
            }
            check(phase.complete(),"UEFA league phase completes");EuropeanLeaguePhase.Qualification q=phase.qualification();
            check(q.roundOf16.length==8&&q.seededPlayoff.length==8&&q.unseededPlayoff.length==8&&q.eliminated.length==12,"UEFA 8 direct, 16 playoffs, 12 eliminated");
            Set<Integer> all=new HashSet<>();for(int[] group:new int[][]{q.roundOf16,q.seededPlayoff,q.unseededPlayoff,q.eliminated})for(int c:group)check(all.add(c),"UEFA qualification unique");
            int[][] rows=phase.metrics();for(int club=0;club<36;club++) {
                int[] opposition=new int[3];for(int i=0;i<phase.fixtureCount();i++){EuropeanLeaguePhase.Fixture f=phase.fixture(i);int id=300+7*club;if(f.home==id||f.away==id){int opponent=((f.home==id?f.away:f.home)-300)/7;for(int col=0;col<3;col++)opposition[col]+=rows[opponent][col];}}
                for(int col=0;col<3;col++)check(rows[club][6+col]==opposition[col],"collective opponent strength ranked correctly");
            }
            String saved=phase.snapshot();check(saved.equals(EuropeanLeaguePhase.restore(saved).snapshot()),"UEFA full save round trip");
            final EuropeanLeaguePhase complete=phase;rejects(()->complete.record(0,99,99,0,0));rejects(()->complete.validateDate(LocalDate.of(2026,9,1)));
        }
        for(int criterion=0;criterion<11;criterion++) {
            int[] a=new int[11],b=new int[11];a[criterion]=1;for(int j=criterion+1;j<11;j++)b[j]=999;
            check(EuropeanLeaguePhase.compareMetrics(a,b)<0,"UEFA ranking criterion priority "+criterion);
        }
        EuropeanLeaguePhase tied=fresh(EuropeanLeaguePhase.Competition.CHAMPIONS,true);for(int i=0;i<tied.fixtureCount();i++)tied.record(i,0,0,0,0);rejects(tied::qualification);
        EuropeanLeaguePhase fresh=fresh(EuropeanLeaguePhase.Competition.CHAMPIONS,false);rejects(()->fresh.record(18,1,0,0,0));
        List<EuropeanLeaguePhase.Fixture> bad=draw(false);bad.set(1,bad.get(0));rejects(()->new EuropeanLeaguePhase(EuropeanLeaguePhase.Competition.CHAMPIONS,2026,clubs(false,false),bad,dates(false)));
        List<EuropeanLeaguePhase.Club> sameCountry=clubs(false,false);EuropeanLeaguePhase.Fixture pair=fresh.fixture(0);int h=(pair.home-300)/7,a=(pair.away-300)/7;
        EuropeanLeaguePhase.Club away=sameCountry.get(a);sameCountry.set(a,new EuropeanLeaguePhase.Club(away.id,sameCountry.get(h).association,away.pot,away.coefficient));
        List<EuropeanLeaguePhase.Fixture> draw=draw(false);rejects(()->new EuropeanLeaguePhase(EuropeanLeaguePhase.Competition.CHAMPIONS,2026,sameCountry,draw,dates(false)));
        String saved=fresh.snapshot();rejects(()->EuropeanLeaguePhase.restore(saved.replace("C|300|AA|0|100000","C|307|AA|0|100000")));
        fresh.record(0,1,0,1,2);String result=fresh.snapshot();rejects(()->EuropeanLeaguePhase.restore(result+"\nS|0|1|0|1|2"));
        System.out.println("PASS: 36 UEFA league phases, all three formats, pot/association/venue constraints, every ranking criterion, qualification, result chronology and save integrity");
    }
}

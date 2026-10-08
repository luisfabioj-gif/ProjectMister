package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

public final class ScottishLeagueCupTests {
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    private static void settle(KnockoutTie tie,int home,int away){tie.recordRegulation(home,away);if(tie.phase()==KnockoutTie.Phase.EXTRA_TIME)tie.recordExtraTime(0,0);if(tie.phase()==KnockoutTie.Phase.PENALTIES)tie.recordPenalties(5,4);}
    public static void main(String[] args)throws Exception {
        NationalCupTests.Registry world=new NationalCupTests.Registry("SCO");int editions=0;
        for(int euro=2;euro<=6;euro++) {
            int[] exemptions=new int[euro];int p=0;for(int i=0;i<world.ids.length&&p<euro;i++)if(world.tiers[i]==1)exemptions[p++]=i;
            ScottishLeagueCupSeasons seasons=new ScottishLeagueCupSeasons(ScottishLeagueCupFactory.create(2026,937,world.ids,world.tiers,world.levels,world.reserves,null,exemptions));
            for(int year=2026;year<2029;year++) {
                ScottishLeagueCupSeason cup=seasons.active();Map<Integer,int[]> venues=new HashMap<>();Set<String> pairs=new HashSet<>();
                for(int i=0;i<80;i++){KnockoutTie tie=cup.groupFixture(i);venues.computeIfAbsent(tie.firstHome,c->new int[2])[0]++;venues.computeIfAbsent(tie.firstAway,c->new int[2])[1]++;check(pairs.add(Math.min(tie.firstHome,tie.firstAway)+":"+Math.max(tie.firstHome,tie.firstAway)),"group opponents meet once");}
                check(venues.size()==40,"all group clubs play");for(int[] venue:venues.values())check(venue[0]==2&&venue[1]==2,"two home and two away group matches");
                KnockoutTie first=cup.current();int home=first.firstHome,away=first.firstAway;first.recordRegulation(1,1);
                cup=ScottishLeagueCupSeason.restore(cup.snapshot());check(cup.current().phase()==KnockoutTie.Phase.PENALTIES,"group shootout resumes");cup.current().recordPenalties(5,4);
                check(cup.metrics().get(home)[0]==2&&cup.metrics().get(away)[0]==1,"group bonus points");check(cup.metrics().get(home)[2]==1&&cup.metrics().get(away)[3]==1,"penalties excluded from goals and away goals");
                while(cup.inGroups()){settle(cup.current(),0,0);cup=ScottishLeagueCupSeason.restore(cup.snapshot());}
                SeasonCup knockout=cup.knockout();check(knockout.entrants().size()==16,"sixteen qualify");int seeded=0;Set<Integer> qualified=new HashSet<>();for(SeasonCup.Entrant e:knockout.entrants()){qualified.add(e.club);if(e.openingSeeded)seeded++;}check(seeded==8,"eight seeded last-sixteen entrants");for(int club:exemptions)check(qualified.contains(club),"all European byes admitted");
                int winners=0,runners=0;for(int g=0;g<8;g++){int[] order=cup.groupOrder(g);if(qualified.contains(order[0]))winners++;if(qualified.contains(order[1]))runners++;}check(winners==8&&runners==8-euro,"winners and changing runner-up count");
                LocalDate last=cup.groupDate(4);while(!cup.complete()){LocalDate date=cup.nextDate();check(date.isAfter(last)||date.equals(last),"Scottish cup chronology");last=date;settle(cup.current(),0,0);cup=ScottishLeagueCupSeason.restore(cup.snapshot());cup.validateDate(date);}
                check(cup.winner()>=0&&cup.roundName().equals("Final"),"completed edition winner and final label");
                // The finished edition must survive the season wrapper and backup allowlist.
                String[] editionRows=seasons.snapshot().split("\n");editionRows[1]=Base64.getEncoder().encodeToString(cup.snapshot().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                ScottishLeagueCupSeasons restored=ScottishLeagueCupSeasons.restore(String.join("\n",editionRows));
                seasons=restored.next(ScottishLeagueCupFactory.create(year+1,940+year,world.ids,world.tiers,world.levels,world.reserves,null,exemptions));
                seasons=ScottishLeagueCupSeasons.validate(seasons.snapshot(),world.ids,world.reserves,year+1,LocalDate.of(year+1,7,1));check(seasons.archives().size()==year-2025,"recurring immutable cup history");editions++;
            }
        }
        ScottishLeagueCupSeason actual=ScottishLeagueCupFactory.create(2026,5,world.ids,world.tiers,world.levels,world.reserves,null,null);
        Set<String> exempt=new HashSet<>();for(int club:actual.exemptions())exempt.add(world.ids[club]);check(exempt.equals(new HashSet<>(Arrays.asList("sco:celtic","sco:rangers","sco:heart-of-midlothian","sco:motherwell","sco:hibernian"))),"published 2026 Scottish European exemptions");
        while(actual.inGroups())settle(actual.current(),0,0);actual.knockout();String raw=actual.snapshot();boolean rejected=false;try{ScottishLeagueCupSeason.restore(raw.replace("SLC1|2026|5","SLC1|2027|5"));}catch(IllegalArgumentException bad){rejected=true;}check(rejected,"outer and knockout year must agree");
        Map<String,Object> backup=new HashMap<>();backup.put("save_0_scottish_league_cup",new ScottishLeagueCupSeasons(actual).snapshot());check(SaveBackup.decode(SaveBackup.encode(backup)).equals(backup),"Scottish cup backup field survives");
        System.out.println("PASS: "+editions+" Scottish League Cups, 2–6 UEFA byes, balanced group venues, shootout bonus points, seeded last sixteen, recurring archives and save metadata rejection");
    }
}

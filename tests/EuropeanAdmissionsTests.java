package com.projectmister.game;
import java.io.*;
import java.time.LocalDate;
import java.util.*;

/** Real registry, independently simulated domestic outcomes and complete Article 3 transfer chains. */
public final class EuropeanAdmissionsTests {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        CompetitionCatalog catalog=CompetitionCatalog.read(new FileInputStream("app/src/main/assets/competitions/2026-27.json"));EuropeanCatalog europe=EuropeanCatalog.read(new FileInputStream("app/src/main/assets/competitions/europe-2026.json"));int campaigns=0,matches=0;
        for(CompetitionCatalog.Division active:catalog.divisions)if(active.tier==1) {
            CareerDivision world=CareerDivision.countryCareer(catalog,active).withWorldClubs(catalog,null,europe.clubs());int[] order=Arrays.stream(world.members(1)).filter(c->!world.reserves[c]).toArray(),levels=new int[world.names.length],coefficients=new int[world.names.length];for(int club=0;club<levels.length;club++){levels[club]=world.level(club);coefficients[club]=world.strengths[club]*1000+levels.length-club;}
            check(world.snapshot().equals(CareerDivision.restore(world.snapshot()).snapshot()),"Expanded registries round-trip without local cup clubs");
            for(int iteration=0;iteration<8;iteration++) {
                int cup=iteration%2==0?order[iteration%order.length]:Arrays.stream(world.members(2)).filter(club->!world.reserves[club]).findFirst().getAsInt();
                EuropeanDomesticSeason domestic=new EuropeanDomesticSeason(2026,world.clubIds,world.reserves,levels,world.strengths,world.country,order,cup,order[(iteration+2)%order.length],7701+iteration);
                check(domestic.associations().size()==54,"All 54 admitted associations represented");
                check(domestic.snapshot().equals(EuropeanDomesticSeason.restore(domestic.snapshot(),world.clubIds,world.reserves).snapshot()),"Canonical domestic outcomes");
                int a=order[iteration%order.length],b=domestic.order("GR")[iteration%domestic.order("GR").length],c=domestic.order("IT")[(iteration+1)%10];if(c==a||c==b)c=domestic.order("DE")[8];
                String[] associations=domestic.associations().stream().filter(country->!EuropeanAccess.profile(country).league.isEmpty()).toArray(String[]::new);String[] eps={associations[(campaigns*2)%associations.length],associations[(campaigns*2+1)%associations.length]};
                EuropeanAdmissions admissions=new EuropeanAdmissions(domestic,world.clubIds,world.reserves,coefficients,a,b,c,eps);
                check(admissions.entry(a).competition()==EuropeanLeaguePhase.Competition.CHAMPIONS&&admissions.entry(a).round==5,"CL titleholder guaranteed direct entry");
                check(admissions.entry(b).competition()==EuropeanLeaguePhase.Competition.CHAMPIONS&&admissions.entry(b).round==5,"EL titleholder guaranteed direct entry");
                check(admissions.snapshot().equals(EuropeanAdmissions.restore(admissions.snapshot()).snapshot()),"Admissions reproduce from domestic results and titleholders");
                EuropeanQualifying qualifying=new EuropeanQualifying(admissions,1783+iteration);LocalDate last=LocalDate.of(2027,7,1);int events=0;
                while(!qualifying.complete()){EuropeanQualifying.Event event=qualifying.next(0);check(!event.date.isBefore(last),"Concurrent qualifying dates never rewind");last=event.date;KnockoutTie tie=event.tie;tie.recordRegulation(0,0);if(tie.phase()==KnockoutTie.Phase.EXTRA_TIME)tie.recordExtraTime(0,0);if(tie.phase()==KnockoutTie.Phase.PENALTIES)tie.recordPenalties(5,4);events++;qualifying.validate(world.clubIds,world.reserves,2027,last);if(events%29==0){String saved=qualifying.snapshot();qualifying=EuropeanQualifying.restore(saved);check(saved.equals(qualifying.snapshot()),"Mid-qualifier canonical reload");}}
                Set<Integer> qualified=new HashSet<>();for(EuropeanLeaguePhase.Competition competition:EuropeanLeaguePhase.Competition.values()){check(qualifying.field(competition).size()==36,"Exactly 36 qualified clubs in each league phase");for(int club:qualifying.field(competition))check(qualified.add(club),"No club in two competitions");}
                check(qualifying.field(EuropeanLeaguePhase.Competition.CHAMPIONS).contains(a)&&qualifying.field(EuropeanLeaguePhase.Competition.CHAMPIONS).contains(b),"Titleholders cannot lose guaranteed places");
                check(events>150,"Real foreign qualifying opponents and demotions exercised");campaigns++;matches+=events;
            }
        }
        System.out.println("PASS: "+campaigns+" complete annual admissions and qualifying campaigns, "+matches+" two-leg match events, all 54 associations, titleholders, EPS, cup pass-down, 108 disjoint qualified clubs and canonical saves");
    }
}

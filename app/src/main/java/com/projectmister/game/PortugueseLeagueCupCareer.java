package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** Career field and validation boundary for the published new League Cup. */
public final class PortugueseLeagueCupCareer {
    public static List<LocalDate> dates(int year){return Collections.unmodifiableList(Arrays.asList(LocalDate.of(year,7,25),LocalDate.of(year,8,1),LocalDate.of(year,8,8),LocalDate.of(year,9,5),LocalDate.of(year,10,27),LocalDate.of(year+1,1,5),LocalDate.of(year+1,1,6),LocalDate.of(year+1,1,9)));}
    /** Article 7 pair, or a documented simulated organiser replacement when reserve restrictions
     * make that literal pair unavailable. Prefer actual lower promotion and barrage outcomes. */
    public static int[] preliminary(PortugueseLowerPromotion lower,int[] tiers,boolean[] reserves,int[] direct,int[] secondOrder){
        Set<Integer> excluded=new HashSet<>();for(int c:direct)excluded.add(c);ArrayList<Integer> candidates=new ArrayList<>();
        int[] literal=lower.leagueCupPreliminary();for(int c:literal)if(tiers[c]<=2&&!reserves[c]&&!excluded.contains(c))candidates.add(c);
        for(int c:lower.thirdEntrants())if(tiers[c]<=2&&!reserves[c]&&!excluded.contains(c)&&!candidates.contains(c))candidates.add(c);
        for(int c:lower.secondEntrants())if(tiers[c]<=2&&!reserves[c]&&!excluded.contains(c)&&!candidates.contains(c))candidates.add(c);
        for(int i=secondOrder.length-1;i>=0;i--){int c=secondOrder[i];if(tiers[c]<=2&&!reserves[c]&&!excluded.contains(c)&&!candidates.contains(c))candidates.add(c);}
        if(candidates.size()<2)throw new IllegalArgumentException("Insufficient non-reserve preliminary replacements");return new int[]{candidates.get(0),candidates.get(1)};
    }
    public static PortugueseLeagueCupSeason create(int year,long seed,String[] identities,int[] tiers,boolean[] reserves,int[] european,int[] preliminary) {
        if(year<2027||identities==null||tiers.length!=identities.length||reserves.length!=tiers.length||european==null)throw new IllegalArgumentException("Incomplete Portuguese League Cup qualification");
        ArrayList<Integer> professionals=new ArrayList<>();for(int i=0;i<tiers.length;i++)if(tiers[i]<=2&&!reserves[i]&&identities[i].startsWith("pt:"))professionals.add(i);
        int[] field=new int[professionals.size()];for(int i=0;i<field.length;i++)field[i]=professionals.get(i);
        int[] direct=Arrays.stream(european).filter(professionals::contains).toArray();return new PortugueseLeagueCupSeason(year,seed,field,direct,(field.length-direct.length)%2==0?new int[0]:preliminary,dates(year),true);
    }
    public static PortugueseLeagueCupSeasons validate(String text,String[] identities,int[] tiers,boolean[] reserves,int year,LocalDate date) {
        PortugueseLeagueCupSeasons seasons=PortugueseLeagueCupSeasons.restore(text);PortugueseLeagueCupSeason cup=seasons.active();
        if(cup.season!=year||!cup.simulatedDates||!cup.calendar().equals(dates(year)))throw new IllegalArgumentException("Changed Portuguese League Cup calendar");
        Set<Integer> expected=new HashSet<>();for(int i=0;i<tiers.length;i++)if(tiers[i]<=2&&!reserves[i]&&identities[i].startsWith("pt:"))expected.add(i);
        Set<Integer> active=new HashSet<>();for(int c:cup.clubs())active.add(c);if(!active.equals(expected))throw new IllegalArgumentException("Incomplete Portuguese League Cup field");
        cup.validateDate(date);validateClubs(cup,identities,reserves);for(String archive:seasons.archives())validateClubs(PortugueseLeagueCupSeason.restore(archive),identities,reserves);return seasons;
    }
    private static void validateClubs(PortugueseLeagueCupSeason cup,String[] identities,boolean[] reserves){for(int c:cup.clubs())if(c>=identities.length||reserves[c]||!identities[c].startsWith("pt:"))throw new IllegalArgumentException("Foreign or reserve League Cup club");}
    private PortugueseLeagueCupCareer(){}
}

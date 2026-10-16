package com.projectmister.game;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;
public final class ClubProfilesTests {
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception {
        Path path=Path.of("app/src/main/assets/competitions/club-profiles-2026.json");ClubProfiles profiles=ClubProfiles.read(Files.newInputStream(path));
        CompetitionCatalog catalog=CompetitionCatalog.read(Files.newInputStream(Path.of("app/src/main/assets/competitions/2026-27.json")));int count=0;
        for(CompetitionCatalog.Division d:catalog.divisions)for(CompetitionCatalog.Club c:d.clubs){
            ClubProfiles.Profile p=profiles.get(c.id);check(p!=null,"Missing real club "+c.id);count++;
            List<String> slots=p.nationalitySlots(20);check(slots.size()==20&&slots.equals(p.nationalitySlots(20)),"Stable twenty-player apportionment "+c.id);
            Map<String,Integer> actual=new HashMap<>();for(String n:slots){check(PlayerNames.supports(n),"Missing name pool "+n);actual.put(n,actual.getOrDefault(n,0)+1);}
            for(String n:p.nationalities.keySet()){int assigned=actual.getOrDefault(n,0),low=p.nationalities.get(n)*20/p.rosterSize;check(assigned==low||assigned==low+1,"Closest discrete nationality mix "+c.id+" "+n);check(PlayerNames.supports(n),"Observed nationality has coherent names: "+n);}
            check(p.primary!=p.secondary&&p.capacity>0&&!p.stadium.isEmpty(),"Distinct real kit palette and observed ground "+c.id);
        }
        check(count==365&&profiles.size()==365,"Exactly all 365 selectable clubs covered");
        check(profiles.get("eng:arsenal").strength>profiles.get("eng:hull-city").strength,"Relative strength reflects observed squad valuation");
        CompetitionCatalog.Division start=catalog.divisions.get(0);CareerDivision old=CareerDivision.countryCareer(catalog,start),updated=old.withProfiles(profiles);
        check(Arrays.equals(old.clubIds,updated.clubIds)&&Arrays.equals(old.clubTiers,updated.clubTiers),"Profile refresh preserves stable world identities and memberships");
        for(int i=0;i<updated.clubIds.length;i++){ClubProfiles.Profile p=profiles.get(updated.clubIds[i]);if(p!=null)check(updated.primary[i]==p.primary&&updated.strengths[i]==p.strength,"Applied current profile "+p.id);}
        check(updated.snapshot().equals(CareerDivision.restore(updated.snapshot()).snapshot()),"Profiled career world reloads canonically");
        JSONObject invalid=new JSONObject(Files.readString(path));invalid.getJSONArray("clubs").getJSONObject(0).put("rosterSize",12);boolean rejected=false;
        try{ClubProfiles.read(new ByteArrayInputStream(invalid.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Incomplete observed roster rejected");
        System.out.println("PASS all 365 observed club profiles, 7300 apportioned identities, cultural name pools, kit colours, grounds and stable career worlds");
    }
}

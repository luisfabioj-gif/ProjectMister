package com.projectmister.game;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.json.*;

/** Verified 2026/27 identities. Later careers retain the lower cup pool and simulate qualification. */
public final class GermanCupCatalog {
    final JSONArray clubs;
    private final Set<String> unseeded=new HashSet<>();
    private GermanCupCatalog(JSONObject o)throws Exception {
        if(o.getInt("schema")!=1||o.getInt("season")!=2026)throw new IllegalArgumentException("Unsupported German cup catalog");
        clubs=o.getJSONArray("clubs");JSONArray lowerPot=o.getJSONArray("unseededProfessional");
        if(clubs.length()!=28||lowerPot.length()!=4)throw new IllegalArgumentException("Incomplete German cup field");
        Set<String> seen=new HashSet<>();
        for(int i=0;i<clubs.length();i++) {
            JSONObject c=clubs.getJSONObject(i);int level=c.getInt("level");
            if(level<3||level>5||!c.getString("id").matches("de:cup-[a-z0-9-]+")||c.getString("name").trim().isEmpty()||!seen.add(c.getString("id")))throw new IllegalArgumentException("Invalid German cup club");
        }
        for(int i=0;i<lowerPot.length();i++)if(!unseeded.add(lowerPot.getString(i)))throw new IllegalArgumentException("Duplicate German cup pot club");
    }
    public static GermanCupCatalog read(InputStream in)throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
        while((n=in.read(b))!=-1){if(out.size()+n>32768)throw new IllegalArgumentException("Oversized German cup catalog");out.write(b,0,n);}
        return new GermanCupCatalog(new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8)));
    }
    public SeasonCup create(CareerDivision world,long seed)throws Exception {
        Set<Integer> lower=new HashSet<>();
        for(String identity:unseeded) {
            int found=-1;for(int i=0;i<world.clubIds.length;i++)if(world.clubIds[i].equals(identity))found=i;
            if(found<0)throw new IllegalArgumentException("Missing German cup pot club");lower.add(found);
        }
        return create(world,2026,seed,lower);
    }
    /** Future qualification uses the retained 64-club pool, with the four lowest retained Liga 2 clubs in the lower opening pot. */
    public static SeasonCup next(CareerDivision world,int year,long seed,Integer[] previousSecondOrder) {
        if(previousSecondOrder==null)throw new IllegalArgumentException("Missing German league standings");
        Set<Integer> lower=new HashSet<>();
        for(int i=previousSecondOrder.length-1;i>=0&&lower.size()<4;i--) {
            int club=previousSecondOrder[i];if(club<0||club>=world.names.length)throw new IllegalArgumentException("Invalid German final standing");
            if(world.clubTiers[club]==2)lower.add(club);
        }
        return create(world,year,seed,lower);
    }
    private static SeasonCup create(CareerDivision world,int year,long seed,Set<Integer> lower) {
        if(world==null||!world.linked||!world.country.equals("DE")||lower.size()!=4)throw new IllegalArgumentException("German cup requires complete career field");
        ArrayList<SeasonCup.Entrant> field=new ArrayList<>();
        for(int i=0;i<world.names.length;i++) {
            if(!world.association(i).equals("DE"))continue;
            if(world.reserves[i])throw new IllegalArgumentException("Reserve team in German cup");
            field.add(new SeasonCup.Entrant(i,0,world.clubTiers[i]==3,world.clubTiers[i]!=3&&!lower.contains(i)));
        }
        if(field.size()!=64)throw new IllegalArgumentException("German cup requires 64 local clubs");
        return DomesticCupFormats.dfbPokal(year,seed,field);
    }
    public static CupSeasons validate(String text,CareerDivision world,int year,LocalDate date) {
        if(world==null||!world.linked||!world.country.equals("DE")||!world.hasCupClubs())throw new IllegalArgumentException("Missing German cup world");
        CupSeasons seasons=CupSeasons.restore(text);
        if(seasons.active().season!=year)throw new IllegalArgumentException("Wrong German cup season");
        validateEdition(seasons.active(),world);seasons.active().validateDate(date);
        for(String archived:seasons.archives())validateEdition(SeasonCup.restore(archived),world);
        return seasons;
    }
    private static void validateEdition(SeasonCup cup,CareerDivision world) {
        if(!cup.competition.equals("DE_POKAL")||!cup.simulatedDates)throw new IllegalArgumentException("Unexpected German cup format");
        SeasonCup expected=DomesticCupFormats.dfbPokal(cup.season,0,cup.entrants());
        Set<Integer> ids=new HashSet<>();
        for(SeasonCup.Entrant e:cup.entrants())if(e.club>=world.names.length||!world.association(e.club).equals("DE")||world.reserves[e.club]||e.amateur!=(world.clubTiers[e.club]==3)||!ids.add(e.club))throw new IllegalArgumentException("Invalid German cup registry");
        for(int i=0;i<6;i++) {
            SeasonCup.Round a=cup.round(i),b=expected.round(i);
            if(!a.name.equals(b.name)||a.legs!=b.legs||a.neutral!=b.neutral||a.rule!=b.rule||a.draw!=b.draw||!a.date(0).equals(b.date(0)))throw new IllegalArgumentException("Changed German cup round");
        }
        try{cup.round(6);throw new IllegalArgumentException("Extra German cup round");}catch(IndexOutOfBoundsException correct){}
    }
}

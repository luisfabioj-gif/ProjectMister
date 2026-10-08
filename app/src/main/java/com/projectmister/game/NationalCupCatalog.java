package com.projectmister.game;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.json.*;

/** Real club identities from association publications; pre-career lower qualifying is simulated. */
public final class NationalCupCatalog {
    private final JSONObject countries;
    private NationalCupCatalog(JSONObject root)throws Exception {
        if(root.getInt("schema")!=1||root.getInt("season")!=2026)throw new IllegalArgumentException("Unsupported national cup catalog");
        countries=root.getJSONObject("countries");
        for(String country:new String[]{"ENG","ES","IT","FR","NL","BE","SCO","TR"}) {
            JSONArray clubs=clubs(country);Set<String> ids=new HashSet<>();
            if(clubs.length()<4||clubs.length()>220)throw new IllegalArgumentException("Incomplete lower cup pool");
            for(int i=0;i<clubs.length();i++){JSONObject club=clubs.getJSONObject(i);int level=club.getInt("level");
                if(!club.getString("id").matches(country.toLowerCase(Locale.ROOT)+":[a-z0-9-]+")||club.getString("name").trim().isEmpty()||level<3||level>20||club.optBoolean("reserve",false)||!ids.add(club.getString("id")))throw new IllegalArgumentException("Invalid national cup identity");}
        }
    }
    public JSONArray clubs(String country)throws Exception{return countries.getJSONObject(country).getJSONArray("clubs");}
    public static NationalCupCatalog read(InputStream input)throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
        while((n=input.read(buffer))!=-1){if(out.size()+n>256*1024)throw new IllegalArgumentException("Oversized national cup catalog");out.write(buffer,0,n);}
        return new NationalCupCatalog(new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8)));
    }
    public static List<SeasonCup> create(CareerDivision world,int year,long seed,int[] previousOrder,int[] european) {
        return NationalCupFactory.create(world.country,year,seed,world.clubIds,world.clubTiers,world.clubLevels,world.reserves,previousOrder,european);
    }
    public static NationalCupCampaign validate(String raw,CareerDivision world,int year,LocalDate date) {
        if(world==null||!world.linked||world.country.equals("PT")||!world.hasCupClubs())throw new IllegalArgumentException("Missing national cup registry");
        if(raw.startsWith("CS1\n")) {
            GermanCupCatalog.validate(raw,world,year,date);
            return NationalCupCampaign.restore(raw);
        }
        NationalCupCampaign campaign=NationalCupCampaign.restore(raw);
        Set<String> expected=new HashSet<>();
        if(world.country.equals("DE"))expected.add("DE_POKAL");
        else if(world.country.equals("ENG")){expected.add("ENG_FA_CUP");expected.add("ENG_LEAGUE_CUP");}
        else expected.add(world.country.equals("ES")?"ES_COPA":world.country.equals("IT")?"IT_COPPA":world.country.equals("FR")?"FR_COUPE":world.country.equals("NL")?"NL_BEKER":world.country.equals("BE")?"BE_CUP":world.country.equals("SCO")?"SCO_CUP":"TR_CUP");
        Set<String> found=new HashSet<>();
        for(CupSeasons editions:campaign.competitions()) {
            SeasonCup active=editions.active();if(active.season!=year||!found.add(active.competition))throw new IllegalArgumentException("Wrong national cup season");
            validateEdition(active,world);active.validateDate(date);
            if(!world.country.equals("DE"))for(SeasonCup.Entrant e:active.entrants())if(e.level!=world.level(e.club)||e.amateur!=(world.clubTiers[e.club]==3))throw new IllegalArgumentException("Changed active cup club category");
            for(String saved:editions.archives())validateEdition(SeasonCup.restore(saved),world);
        }
        if(!found.equals(expected))throw new IllegalArgumentException("Missing national competition");return campaign;
    }
    private static void validateEdition(SeasonCup cup,CareerDivision world) {
        for(SeasonCup.Entrant club:cup.entrants())if(club.club>=world.names.length||world.reserves[club.club]||!world.association(club.club).equals(world.country))throw new IllegalArgumentException("Foreign or reserve national cup entrant");
        if(cup.competition.equals("DE_POKAL")) {
            SeasonCup expected=DomesticCupFormats.dfbPokal(cup.season,0,cup.entrants());
            if(cup.roundCount()!=6||cup.entrants().size()!=64||!cup.simulatedDates)throw new IllegalArgumentException("Invalid German cup format");
            for(int i=0;i<6;i++){SeasonCup.Round a=cup.round(i),b=expected.round(i);if(a.draw!=b.draw||a.rule!=b.rule||a.legs!=b.legs||a.neutral!=b.neutral||!a.name.equals(b.name)||!a.date(0).equals(b.date(0)))throw new IllegalArgumentException("Changed German cup round");}
        } else NationalCupFormats.validateStructure(cup);
    }
}

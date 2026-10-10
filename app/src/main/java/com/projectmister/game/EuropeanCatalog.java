package com.projectmister.game;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Published initial entrants, mapped to immutable career club indices. */
public final class EuropeanCatalog {
    private final JSONObject data;
    private EuropeanCatalog(JSONObject data)throws Exception {
        if(data.getInt("schema")!=1||data.getInt("season")!=2026)throw new IllegalArgumentException("Unsupported European catalog");this.data=data;
        Set<String> identities=new HashSet<>();
        for(EuropeanLeaguePhase.Competition competition:EuropeanLeaguePhase.Competition.values()) {
            JSONArray field=data.getJSONObject("fields").getJSONArray(competition.name());if(field.length()!=36)throw new IllegalArgumentException("Incomplete European field");
            int[] pots=new int[competition==EuropeanLeaguePhase.Competition.CONFERENCE?6:4];
            for(int i=0;i<field.length();i++){JSONObject c=field.getJSONObject(i);String identity=c.getString("id");int pot=c.getInt("pot");if(!identity.matches("[a-z]{2,4}:[a-z0-9-]+")||!identities.add(identity)||pot<0||pot>=pots.length||c.getInt("simulationCoefficient")<0)throw new IllegalArgumentException("Invalid European club");pots[pot]++;}
            for(int n:pots)if(n!=36/pots.length)throw new IllegalArgumentException("Invalid European pot size");
        }
    }
    public JSONArray clubs()throws Exception{return data.getJSONArray("clubs");}
    public Map<String,Integer> admittedCounts(){TreeMap<String,Integer> counts=new TreeMap<>();try{JSONObject values=data.getJSONObject("openingAdmittedCounts");Iterator<String> keys=values.keys();while(keys.hasNext()){String key=keys.next();int n=values.getInt(key);if(!key.matches("[A-Z]{2,4}")||n<1||n>12)throw new IllegalArgumentException("Invalid opening UEFA admissions");counts.put(key,n);}}catch(JSONException invalid){throw new IllegalArgumentException("Missing opening UEFA admissions",invalid);}return Collections.unmodifiableMap(counts);}
    public static EuropeanCatalog read(InputStream input)throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1){if(out.size()+n>256*1024)throw new IllegalArgumentException("Oversized European catalog");out.write(buffer,0,n);}return new EuropeanCatalog(new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8)));
    }
    public EuropeanCampaign first(CareerDivision world,long seed) {return first(world,seed,Collections.emptyList());}
    public EuropeanCampaign first(CareerDivision world,long seed,List<java.time.LocalDate> domestic) {
        if(world==null||!world.linked)throw new IllegalArgumentException("Linked European world required");
        Map<String,Integer> registry=new HashMap<>();for(int i=0;i<world.clubIds.length;i++)registry.put(world.clubIds[i],i);ArrayList<EuropeanSeason> seasons=new ArrayList<>();
        try {
            for(EuropeanLeaguePhase.Competition competition:EuropeanLeaguePhase.Competition.values()) {
                JSONArray field=data.getJSONObject("fields").getJSONArray(competition.name());ArrayList<EuropeanLeaguePhase.Club> clubs=new ArrayList<>();
                for(int i=0;i<field.length();i++){JSONObject c=field.getJSONObject(i);Integer club=registry.get(c.getString("id"));if(club==null||world.reserves[club])throw new IllegalArgumentException("Missing UEFA career club: "+c.getString("id"));clubs.add(new EuropeanLeaguePhase.Club(club,world.association(club),c.getInt("pot"),c.getInt("simulationCoefficient")));}
                seasons.add(create(competition,2026,clubs,seed+competition.ordinal()*7919L,null,domestic));
            }
        }catch(Exception invalid){throw new IllegalArgumentException("Cannot create UEFA career",invalid);}return new EuropeanCampaign(seasons);
    }
    public static EuropeanSeason create(EuropeanLeaguePhase.Competition competition,int year,List<EuropeanLeaguePhase.Club> clubs,long seed,EuropeanLeaguePhase previous) {
        return create(competition,year,clubs,seed,previous,Collections.emptyList());
    }
    public static EuropeanSeason create(EuropeanLeaguePhase.Competition competition,int year,List<EuropeanLeaguePhase.Club> clubs,long seed,EuropeanLeaguePhase previous,List<java.time.LocalDate> domestic) {
        List<java.time.LocalDate> dates=EuropeanCalendar.career(competition,year,domestic);int rounds=competition==EuropeanLeaguePhase.Competition.CONFERENCE?6:8;
        EuropeanLeaguePhase phase=EuropeanDraw.create(competition,year,clubs,dates.subList(0,rounds),seed,previous);
        return new EuropeanSeason(phase,seed^0x31ff80L,dates.subList(rounds,dates.size()),true);
    }
}

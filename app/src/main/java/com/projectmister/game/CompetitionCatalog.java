package com.projectmister.game;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Season-specific club identities. Display names and array positions are never save identities. */
public final class CompetitionCatalog {
    public static final class Club {
        public final String id, name;
        public final boolean reserve;
        private Club(JSONObject o) throws Exception {
            id = required(o, "id"); name = required(o, "name"); reserve = o.getBoolean("reserve");
        }
    }
    public static final class Division {
        public final String id, countryCode, country, name, source;
        public final int tier;
        public final List<Club> clubs;
        private Division(JSONObject o, HashSet<String> clubIds) throws Exception {
            id=required(o,"id"); countryCode=required(o,"countryCode");country=required(o,"country");
            name=required(o,"name");source=required(o,"source");tier=o.getInt("tier");
            if(tier<1 || tier>2 || !source.startsWith("https://"))throw new IllegalArgumentException("Invalid division metadata");
            JSONArray a=o.getJSONArray("clubs");List<Club> list=new ArrayList<>();
            if(a.length()<2 || a.length()>32)throw new IllegalArgumentException("Invalid division size");
            for(int i=0;i<a.length();i++) {
                Club club=new Club(a.getJSONObject(i));
                if(!clubIds.add(club.id))throw new IllegalArgumentException("Duplicate club identity: "+club.id);
                list.add(club);
            }
            clubs=Collections.unmodifiableList(list);
        }
    }
    public final String season, verifiedOn;
    public final List<Division> divisions;
    private CompetitionCatalog(JSONObject o) throws Exception {
        if(o.getInt("schemaVersion")!=1)throw new IllegalArgumentException("Unsupported catalog version");
        season=required(o,"season");verifiedOn=required(o,"verifiedOn");
        JSONArray a=o.getJSONArray("divisions");List<Division> list=new ArrayList<>();
        HashSet<String> clubIds=new HashSet<>(),divisionIds=new HashSet<>(),tiers=new HashSet<>();
        for(int i=0;i<a.length();i++) {
            Division d=new Division(a.getJSONObject(i),clubIds);
            if(!divisionIds.add(d.id)||!tiers.add(d.countryCode+":"+d.tier))throw new IllegalArgumentException("Duplicate division");
            list.add(d);
        }
        divisions=Collections.unmodifiableList(list);
    }
    private static String required(JSONObject o,String key)throws Exception {
        String value=o.getString(key).trim();if(value.isEmpty())throw new IllegalArgumentException("Empty "+key);return value;
    }
    /** Caller owns and closes input. Bound size so a damaged optional catalog cannot exhaust memory. */
    public static CompetitionCatalog read(InputStream input)throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
        while((count=input.read(buffer))!=-1) {
            if(bytes.size()+count>512*1024)throw new IllegalArgumentException("Oversized catalog");
            bytes.write(buffer,0,count);
        }
        return new CompetitionCatalog(new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8)));
    }
}

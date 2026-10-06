package com.projectmister.game;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Bounded, versioned backup with explicit primitive types; no Java object deserialization. */
public final class SaveBackup {
    public static final int MAX_BYTES=8*1024*1024;
    private static final int MAGIC=0x42584931, VERSION=1, MAX_ENTRIES=2000;
    private static final Map<String,Class<?>> FIELDS=new HashMap<>();
    static {
        fields("career_world,classic_inbox,classic_notes,classic_pass,classic_scout,classic_scout_due,classic_shortlist,classic_tackle,classic_training,classic_wibwob,club_match_ga,club_match_gf,date,drawn,formation,ga,gf,league_results,lost,manager_dob,manager_first,manager_gender,manager_last,manager_league,p_apps,p_assists,p_contract,p_def,p_finish,p_fit,p_forwardruns,p_freerole,p_goals,p_injury,p_loan,p_longshots,p_morale,p_onloan,p_overall,p_pace,p_pass,p_phys,p_runball,p_team,p_tech,p_transfer,p_wage,played,playstyle,points,promotion,role_pos,role_slots,roles,season_start,season_history,market_hires,split_order,stad_name,stad_project_type,staff_am_name,staff_coach_name,staff_scout_name,training,won",String.class);
        fields("budget,classic_board,classic_rep,classic_wage_budget,club,fac_bars,fac_fanzone,fac_gym,fac_hosp,fac_media,fac_medical,fac_museum,fac_parking,fac_rest,fac_shop,fac_training,fac_youth,fin_balance,fin_cm_e,fin_cm_i,fin_gate,fin_lm_e,fin_lm_i,fin_ly_e,fin_ly_i,fin_maint,fin_merch,fin_month,fin_other_e,fin_other_i,fin_pwages,fin_sales,fin_sponsor,fin_stadium,fin_swages,fin_sy_e,fin_sy_i,fin_transfers,fin_travel,fin_tv,fin_youth,fixture_version,manager_contract_years,manager_country_index,manager_discipline,manager_league_index,manager_motivating,manager_negotiating,manager_player_knowledge,manager_tactical,manager_wage,manager_youth,matchday,stad_avg,stad_cap,stad_e,stad_homegames,stad_limit,stad_n,stad_project_code,stad_project_cost,stad_project_gain,stad_project_weeks,stad_s,stad_seats,stad_w,staff_am_rating,staff_coach_rating,staff_scout_rating",Integer.class);
        fields("classic_counter,classic_mask,classic_menbehind,classic_offside,classic_press,exists,split_provisional,stad_covered,stad_heating",Boolean.class);
    }
    private static void fields(String names,Class<?> type){for(String name:names.split(","))FIELDS.put(name,type);}
    private static Class<?> type(String key) {
        if(key.matches("save_[0-2]_[a-z_]+"))return FIELDS.get(key.substring(7));
        if(key.matches("editor_name_[a-z0-9:-]+"))return String.class;
        if(key.matches("editor_(primary|secondary)_[a-z0-9:-]+"))return Integer.class;
        return key.equals("audio_crowd")||key.equals("audio_effects")?Boolean.class:null;
    }
    private static void validate(String key,Object value)throws IOException {
        Class<?> expected=type(key);
        if(key.length()>160||expected==null||value==null||value.getClass()!=expected)throw new IOException("Unsupported backup field");
    }
    private static byte[] digest(byte[] data)throws IOException {
        try{return MessageDigest.getInstance("SHA-256").digest(data);}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
    }
    private static void string(DataOutputStream out,String value)throws IOException {
        byte[] bytes=value.getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES/2)throw new IOException("Backup value too large");out.writeInt(bytes.length);out.write(bytes);
    }
    private static String string(DataInputStream in)throws IOException {
        int size=in.readInt();if(size<0||size>MAX_BYTES/2||size>in.available())throw new IOException("Invalid backup string");
        byte[] bytes=new byte[size];in.readFully(bytes);
        try{return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();}
        catch(java.nio.charset.CharacterCodingException e){throw new IOException("Invalid text encoding",e);}
    }
    public static byte[] encode(Map<String,?> values)throws IOException {
        if(values.size()>MAX_ENTRIES)throw new IOException("Too many backup fields");
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(buffer);
        out.writeInt(MAGIC);out.writeInt(VERSION);out.writeInt(values.size());
        for(String key:new TreeSet<>(values.keySet())) {
            Object value=values.get(key);validate(key,value);string(out,key);
            if(value instanceof String){out.writeByte(1);string(out,(String)value);}
            else if(value instanceof Integer){out.writeByte(2);out.writeInt((Integer)value);}
            else{out.writeByte(3);out.writeBoolean((Boolean)value);}
            if(buffer.size()>MAX_BYTES-32)throw new IOException("Backup too large");
        }
        byte[] body=buffer.toByteArray();out.write(digest(body));return buffer.toByteArray();
    }
    public static Map<String,Object> decode(byte[] bytes)throws IOException {
        if(bytes.length<44||bytes.length>MAX_BYTES)throw new IOException("Invalid backup size");
        byte[] body=Arrays.copyOf(bytes,bytes.length-32),hash=Arrays.copyOfRange(bytes,bytes.length-32,bytes.length);
        if(!MessageDigest.isEqual(hash,digest(body)))throw new IOException("Backup is damaged");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(body));
        if(in.readInt()!=MAGIC||in.readInt()!=VERSION)throw new IOException("Unsupported backup version");
        int count=in.readInt();if(count<0||count>MAX_ENTRIES)throw new IOException("Invalid backup count");
        Map<String,Object> values=new LinkedHashMap<>();
        for(int i=0;i<count;i++) {
            String key=string(in);int tag=in.readUnsignedByte();Object value;
            if(tag==1)value=string(in);else if(tag==2)value=in.readInt();
            else if(tag==3){int b=in.readUnsignedByte();if(b>1)throw new IOException("Invalid boolean");value=b==1;}
            else throw new IOException("Invalid backup type");
            validate(key,value);if(values.put(key,value)!=null)throw new IOException("Duplicate backup field");
        }
        if(in.available()!=0)throw new IOException("Unexpected backup data");
        return values;
    }
    public static byte[] read(InputStream stream)throws IOException {
        if(stream==null)throw new IOException("File unavailable");
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] block=new byte[8192];int n;
        while((n=stream.read(block))!=-1){if(out.size()+n>MAX_BYTES)throw new IOException("Backup too large");out.write(block,0,n);}
        return out.toByteArray();
    }
}

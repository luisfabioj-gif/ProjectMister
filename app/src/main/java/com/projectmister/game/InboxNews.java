package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** Versioned news stored inside the existing inbox field, including legacy undated news. */
public final class InboxNews {
    public static final class Item {
        public String category,title,body,source="Club office",action="",response="";
        public LocalDate date;
        public boolean read;
        public Item(String category,String title,String body,LocalDate date){this.category=category;this.title=title;this.body=body;this.date=date;}
    }
    private static String safe(String s){return s==null?"":s.replace('\u001e',' ').replace('\u001f',' ');}
    public static String encode(List<Item> items){
        StringJoiner out=new StringJoiner("\u001e","IX2\u001e","");
        for(Item n:items)out.add(String.join("\u001f",safe(n.category),safe(n.title),safe(n.body),n.date==null?"":n.date.toString(),n.read?"1":"0",safe(n.source),safe(n.action),safe(n.response)));
        return out.toString();
    }
    public static List<Item> decode(String value){
        List<Item> out=new ArrayList<>();if(value==null||value.isEmpty())return out;
        boolean modern=value.startsWith("IX2\u001e");if(modern)value=value.substring(4);
        for(String line:value.split("\u001e",-1)){
            String[] p=line.split("\u001f",modern?-1:3);if(p.length!=(modern?8:3))continue;
            LocalDate date=null;try{if(modern&&!p[3].isEmpty())date=LocalDate.parse(p[3]);}catch(java.time.DateTimeException ignored){}
            Item n=new Item(p[0],p[1],pressText(p[2]),date);
            if(modern){n.read=p[4].equals("1");n.source=pressText(p[5]);n.action=p[6];n.response=p[7];}out.add(n);
            if(out.size()==100)break;
        }
        return out;
    }
    public static boolean hasAction(List<Item> items,String action){for(Item n:items)if(n.action.equals(action))return true;return false;}
    public static boolean reply(Item item,String response){if(!item.action.startsWith("interview:")||!item.response.isEmpty())return false;item.response=response;item.read=true;return true;}
    public static String pressText(String value){return value.replace(" • fictional newspaper", "").replace(" • fictional journalist", "").replace(" This is a fictional press interview.", "");}
    private InboxNews(){}
}

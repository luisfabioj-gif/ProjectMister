package com.projectmister.game;
import java.text.NumberFormat;
import java.util.Locale;
/** Human-facing labels; sporting tie resolution remains in the rules engines. */
public final class DisplayFormat {
    public static String ordinal(int value) {
        if(value<1)return "—";
        int last=value%100;
        return value+(last>=11&&last<=13?"th":value%10==1?"st":value%10==2?"nd":value%10==3?"rd":"th");
    }
    public static String number(int value){return NumberFormat.getIntegerInstance(Locale.UK).format(value);}
    public static String moneyK(int value) {
        if(Math.abs((long)value)>=1000)return String.format(Locale.UK,"€%,.1fm",value/1000.0);
        return "€"+number(value)+"k";
    }
    private DisplayFormat(){}
}

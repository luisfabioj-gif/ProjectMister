package com.projectmister.game;

/** Rendering-only clash resolution; never changes a club's saved identity colours. */
public final class KitColours {
    private static double channel(int value){double v=value/255.0;return v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4);}
    private static double luminance(int c){return .2126*channel((c>>16)&255)+.7152*channel((c>>8)&255)+.0722*channel(c&255);}
    public static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    public static int away(int home,int primary,int secondary){
        if(contrast(home,primary)>=2.2)return primary;
        if(contrast(home,secondary)>=2.2)return secondary;
        int light=0xfff4f7fb,dark=0xff142332;
        return contrast(home,light)>=contrast(home,dark)?light:dark;
    }
    private KitColours(){}
}

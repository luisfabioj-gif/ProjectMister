package com.projectmister.game;

/** Explicit stable-ID parent relations for the currently shipped Iberian reserves. */
public final class ReserveEligibility {
    private ReserveEligibility(){}
    static String parent(String id){
        switch(id){
            case "pt:fc-porto-b":return "pt:fc-porto";
            case "pt:sl-benfica-b":return "pt:sl-benfica";
            case "pt:sporting-cp-b":return "pt:sporting-cp";
            case "es:celta-fortuna":return "es:celta";
            case "es:real-sociedad-b":return "es:real-sociedad";
            default:return "";
        }
    }
    public static boolean parentRelegationConflict(String[] ids,boolean[] reserves,int[] down){
        for(int i=0;i<ids.length;i++)if(reserves[i])for(int club:down)if(parent(ids[i]).equals(ids[club]))return true;
        return false;
    }
}

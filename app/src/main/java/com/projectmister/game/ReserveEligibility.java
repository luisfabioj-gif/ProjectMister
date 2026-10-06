package com.projectmister.game;

/** Explicit stable-ID parent relations for the currently supported reserve teams. */
public final class ReserveEligibility {
    private ReserveEligibility(){}
    static String parent(String id){
        switch(id){
            case "pt:fc-porto-b":return "pt:fc-porto";
            case "pt:sl-benfica-b":return "pt:sl-benfica";
            case "pt:sporting-cp-b":return "pt:sporting-cp";
            case "es:celta-fortuna":return "es:celta";
            case "es:real-sociedad-b":return "es:real-sociedad";
            case "be:club-nxt":return "be:club-brugge";
            case "be:jong-genk":return "be:krc-genk";
            case "be:jong-kaa-gent":return "be:kaa-gent";
            case "be:rsca-futures":return "be:rsc-anderlecht";
            case "nl:jong-ajax":return "nl:ajax";
            case "nl:jong-az":return "nl:az";
            case "nl:jong-fc-utrecht":return "nl:fc-utrecht";
            case "nl:jong-psv":return "nl:psv";
            default:return "";
        }
    }
    public static boolean parentRelegationConflict(String[] ids,boolean[] reserves,int[] down){
        for(int i=0;i<ids.length;i++)if(reserves[i])for(int club:down)if(parent(ids[i]).equals(ids[club]))return true;
        return false;
    }
}

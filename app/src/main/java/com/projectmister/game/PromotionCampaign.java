package com.projectmister.game;

import java.util.List;

/** Country-owned playoff progress; club identities never depend on the active tier. */
public interface PromotionCampaign {
    String country();
    int[] automaticPromoted();
    int[] automaticRelegated();
    int[] upperEntrants();
    int[] lowerEntrants();
    String roundName(int index);
    KnockoutTie current();
    boolean complete();
    List<KnockoutTie> ties();
    int[] promoted();
    int[] relegated();
    String snapshot();
    static PromotionCampaign restore(String value) {
        if(value.startsWith("TR#"))return TurkeyPromotion.restore(value);
        return value.startsWith("DE#")?GermanyPromotion.restore(value):ScotlandPromotion.restore(value);
    }
}

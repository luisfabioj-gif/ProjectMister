package com.projectmister.game;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Live seat-plan rendering; artwork stays sharp at any screen density. */
final class StandDesignView extends View {
    private final StandDesigns.Design design;private final String club;private final int secondary;
    private final Paint p=new Paint(3);
    StandDesignView(Context c,StandDesigns.Design d,String club,int secondary){super(c);this.design=d;this.club=GameNames.display(club);this.secondary=secondary;setTag("boss-stand-preview");setContentDescription("Stand design preview");}
    @Override protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w);setMeasuredDimension(width,resolveSize(Math.round(width*.57f),h));}
    @Override protected void onDraw(Canvas c){
        float w=getWidth(),h=getHeight();c.drawColor(0xff101a20);
        p.setShader(new LinearGradient(0,0,0,h,0xff314757,0xff10191b,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
        p.setColor(0xff14252c);c.drawRoundRect(w*.035f,h*.13f,w*.965f,h*.84f,12,12,p);
        for(int row=0;row<8;row++)for(int col=0;col<28;col++){
            boolean alternate=design.pattern==1?(col/4)%2==1:design.pattern==2?(col/3+row/2)%2==1:design.pattern==3?((col+row)/4)%2==1:false;
            p.setColor(alternate?secondary:StandDesigns.COLOURS[design.colour]);float x=w*(.065f+col*.0315f),y=h*(.19f+row*.067f);
            c.drawRoundRect(x,y,x+w*.023f,y+h*.044f,2,2,p);p.setColor(0x33000000);c.drawRect(x,y+h*.032f,x+w*.023f,y+h*.048f,p);
        }
        p.setShader(new LinearGradient(0,h*.72f,0,h*.94f,0xff73818b,0xff29333b,Shader.TileMode.CLAMP));c.drawRect(w*.035f,h*.75f,w*.965f,h*.94f,p);p.setShader(null);
        p.setColor(0xffd8ddd5);p.setTypeface(BossStyle.DISPLAY);p.setTextAlign(Paint.Align.CENTER);String banner=design.banner.isEmpty()?club:GameNames.display(design.banner);p.setTextSize(Math.min(h*.083f,w*.82f/Math.max(1,banner.length())*1.7f));c.drawText(banner.toUpperCase(java.util.Locale.UK),w*.5f,h*.875f,p);
        p.setColor(0xff243e2c);c.drawRect(0,h*.95f,w,h,p);
    }
}

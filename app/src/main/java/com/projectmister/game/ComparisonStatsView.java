package com.projectmister.game;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Real layout columns and comparison bars; never relies on whitespace or monospace padding. */
public final class ComparisonStatsView extends LinearLayout {
    private final String[] labels;
    private final ArrayList<TextView> left=new ArrayList<>(),right=new ArrayList<>();
    private final ArrayList<Bar> bars=new ArrayList<>();
    private final boolean compact;
    private final int homeColour,awayColour;
    public ComparisonStatsView(Context context,String home,String away,int homeColour,int awayColour,boolean compact,String[] labels){
        super(context);setOrientation(VERTICAL);this.labels=labels.clone();this.compact=compact;
        this.homeColour=homeColour;this.awayColour=KitColours.away(homeColour,awayColour,0xffeef3f8);
        LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView a=text(home,compact?10:13,true),b=text(away,compact?10:13,true);a.setGravity(Gravity.START);b.setGravity(Gravity.END);
        a.setMaxLines(2);b.setMaxLines(2);a.setEllipsize(android.text.TextUtils.TruncateAt.END);b.setEllipsize(android.text.TextUtils.TruncateAt.END);
        header.addView(a,new LayoutParams(0,-2,1));header.addView(b,new LayoutParams(0,-2,1));
        header.setPadding(0,0,0,dp(compact?3:14));addView(header);
        for(String label:labels){
            LinearLayout item=new LinearLayout(context);item.setOrientation(VERTICAL);item.setPadding(0,dp(compact?1:8),0,dp(compact?1:8));
            LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);
            TextView x=text("—",compact?10:16,true),title=text(label,compact?9:13,false),y=text("—",compact?10:16,true);
            x.setGravity(Gravity.START);title.setGravity(Gravity.CENTER);y.setGravity(Gravity.END);
            row.addView(x,new LayoutParams(0,-2,1));row.addView(title,new LayoutParams(0,-2,compact?3.4f:2.6f));row.addView(y,new LayoutParams(0,-2,1));item.addView(row);
            Bar bar=new Bar(context);LayoutParams barParams=new LayoutParams(-1,dp(compact?2:4));barParams.topMargin=dp(compact?1:6);item.addView(bar,barParams);addView(item);left.add(x);right.add(y);bars.add(bar);
        }
    }
    public void row(int row,String a,String b,int av,int bv){
        if(!left.get(row).getText().toString().equals(a))left.get(row).setText(a);
        if(!right.get(row).getText().toString().equals(b))right.get(row).setText(b);
        left.get(row).setContentDescription(labels[row]+", home: "+a);right.get(row).setContentDescription(labels[row]+", away: "+b);
        bars.get(row).values(av,bv);
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,boolean bold){TextView t=new TextView(getContext());t.setText(value);t.setTextSize(size);t.setTextColor(bold?BossStyle.INK:BossStyle.MUTED);t.setTypeface(bold?BossStyle.MEDIUM:BossStyle.BODY);t.setIncludeFontPadding(!compact);t.setFontFeatureSettings("tnum");if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    private final class Bar extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private int a=-1,b=-1;
        Bar(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        void values(int left,int right){if(a==left&&b==right)return;a=left;b=right;invalidate();}
        @Override protected void onDraw(Canvas c){float h=getHeight(),r=h/2;p.setColor(BossStyle.LINE);c.drawRoundRect(0,0,getWidth(),h,r,r,p);if(a<0||b<0||a+b==0)return;float gap=Math.max(1,getWidth()*.008f),width=(getWidth()-gap)*a/(float)(a+b);p.setColor(homeColour);c.drawRoundRect(0,0,width,h,r,r,p);p.setColor(awayColour);c.drawRoundRect(width+gap,0,getWidth(),h,r,r,p);}
    }
}

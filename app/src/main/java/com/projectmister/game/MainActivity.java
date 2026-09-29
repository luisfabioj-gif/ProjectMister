package com.projectmister.game;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.Choreographer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private static final int SAVE_SLOTS = 3;
    private static final int PLAYERS_PER_TEAM = 20;
    private static final int TOTAL_PLAYERS = 18 * PLAYERS_PER_TEAM;
    private static final LocalDate SEASON_START = LocalDate.of(2026, 8, 9);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK);
    private static final String[] COUNTRY_OPTIONS = {
            "🇵🇹 Portugal", "🇪🇸 Spain", "🇫🇷 France", "🇮🇹 Italy",
            "🇩🇪 Germany", "🏴 England", "🇳🇱 Netherlands", "🇧🇷 Brazil"
    };
    private static final String[] LEAGUE_OPTIONS = {
            "🇵🇹 Portugal • Primeira Liga"
    };

    private final String[] defaultClubNames = {
            "Lisboa Águias", "Lisboa Leões", "Porto Dragões", "Braga Guerreiros",
            "Guimarães Castelo", "Famal Norte", "Moreira FC", "Vila do Conde",
            "Barcelos FC", "Estoril Praia", "Lisboa Corvos", "Açores FC",
            "Madeira Nacional", "Alverca Atlético", "Arouca FC", "Amadora Estrela",
            "Tondela SC", "Vila das Aves"
    };

    private final String[] shortNames = {
            "LAG", "LLE", "PDR", "BGR", "GCT", "FAM", "MOR", "VDC", "BAR",
            "EST", "LCO", "ACO", "MDN", "ALV", "ARO", "AMA", "TON", "VDA"
    };

    private final int[] strength = {
            88, 87, 86, 81, 78, 75, 72, 71, 70, 72, 68, 71, 69, 67, 70, 67, 66, 65
    };

    private final int[] budgets = {
            42, 38, 36, 20, 12, 10, 8, 9, 7, 10, 6, 8, 7, 6, 8, 6, 5, 5
    };

    private final int[] defaultPrimary = {
            0xFFE53935, 0xFF1B8F4D, 0xFF1565C0, 0xFFD32F2F, 0xFFF5F5F5, 0xFF1565C0,
            0xFF2E7D32, 0xFF2E7D32, 0xFFD32F2F, 0xFFFBC02D, 0xFF20252B, 0xFFD32F2F,
            0xFF20252B, 0xFFD32F2F, 0xFFFBC02D, 0xFFD32F2F, 0xFF2E7D32, 0xFFD32F2F
    };

    private final int[] defaultSecondary = {
            0xFFF5F5F5, 0xFFF5F5F5, 0xFFF5F5F5, 0xFFF5F5F5, 0xFF20252B, 0xFFF5F5F5,
            0xFFF5F5F5, 0xFFF5F5F5, 0xFF1565C0, 0xFF1565C0, 0xFFF5F5F5, 0xFFF5F5F5,
            0xFFF5F5F5, 0xFF1565C0, 0xFF1565C0, 0xFFF5F5F5, 0xFFFBC02D, 0xFFF5F5F5
    };

    private final String[] paletteNames = {
            "Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Black", "White", "Sky Blue", "Pink"
    };

    private final int[] palette = {
            0xFFE53935, 0xFF2E7D32, 0xFF1565C0, 0xFFFBC02D, 0xFFF57C00,
            0xFF7B1FA2, 0xFF20252B, 0xFFF5F5F5, 0xFF039BE5, 0xFFD81B60
    };

    private final String[] clubNames = new String[18];
    private final int[] primaryColours = new int[18];
    private final int[] secondaryColours = new int[18];

    private final int[] played = new int[18];
    private final int[] won = new int[18];
    private final int[] drawn = new int[18];
    private final int[] lost = new int[18];
    private final int[] goalsFor = new int[18];
    private final int[] goalsAgainst = new int[18];
    private final int[] points = new int[18];

    private final ArrayList<Player> players = new ArrayList<>();
    private final Random random = new Random();

    private SharedPreferences prefs;
    private PortraitManager portraits;
    private MatchAudio audio;
    private final SubstitutionLedger substitutions = new SubstitutionLedger(5);
    private final java.util.HashSet<Integer> matchParticipants = new java.util.HashSet<>();
    private final java.util.HashSet<Integer> aiDeparted = new java.util.HashSet<>();
    private int aiSubsUsed;
    private int lastAiReview=-1;
    private boolean matchInProgress;
    private boolean substitutionsPending;
    private final java.util.HashMap<Integer,Integer> matchTackles=new java.util.HashMap<>();
    private final java.util.HashMap<Integer,Integer> matchCompletedPasses=new java.util.HashMap<>();
    private final java.util.HashMap<Integer,Integer> matchMissedPasses=new java.util.HashMap<>();
    private final java.util.HashMap<Integer,Integer> matchYellows=new java.util.HashMap<>();
    private final java.util.HashMap<Integer,Integer> matchGoals=new java.util.HashMap<>();
    private final java.util.HashMap<Integer,Integer> matchAssists=new java.util.HashMap<>();
    private int pendingGoalTeam=-1,pendingGoalSlot=-1;
    private float pendingGoalDelay;
    private int pendingAssistId=-1;
    private boolean tickerWasActive;
    private boolean pausedBeforeBackground;
    private int lastPasserId = -1;
    private int lastPassTeam = -1;
    private int selectedClub = -1;
    private int selectedSlot = -1;
    private int matchday = 0;
    private LocalDate currentDate = SEASON_START;
    private String trainingFocus = "Balanced";
    private String tacticFormation = "4-3-3";
    private String playStyle = "Balanced";
    private int currentTransferBudget = 0;
    private final int[] playerRoleStatus = new int[TOTAL_PLAYERS]; // 2=start, 1=sub, 0=reserve
    private final int[] playerSelectedSlot = new int[TOTAL_PLAYERS]; // starter slot index if selected in XI
    private final String[] playerSelectedPosition = new String[TOTAL_PLAYERS];
    private final int[] clubMatchGF = new int[34];
    private final int[] clubMatchGA = new int[34];
    private Runnable backAction = null;
    private String managerFirstName = "";
    private String managerLastName = "";
    private String managerDob = "01/01/1990";
    private String managerGender = "Male";
    private int managerCountryIndex = 0;
    private int managerLeagueIndex = 0;
    private int managerWeeklyWageK = 32;
    private int managerContractYears = 3;
    private int managerTactical = 12;
    private int managerMotivating = 11;
    private int managerDiscipline = 10;
    private int managerPlayerKnowledge = 12;
    private int managerYouth = 10;
    private int managerNegotiating = 11;

    private String assistantManagerName = "Marco Vieira";
    private int assistantManagerRating = 62;
    private String headCoachName = "Tiago Nunes";
    private int headCoachRating = 64;
    private String chiefScoutName = "Rui Mendes";
    private int chiefScoutRating = 61;


    // CM 01/02-inspired career systems. These are original Project Mister implementations
    // based on documented gameplay concepts, not copied source code.
    private boolean attributeMasking = true;
    private final int[] scoutKnowledge = new int[TOTAL_PLAYERS]; // 0 unknown, 1 basic, 2 partial, 3 report, 4 full
    private final int[] scoutDueRound = new int[TOTAL_PLAYERS];
    private final boolean[] shortlisted = new boolean[TOTAL_PLAYERS];
    private final String[] managerNotes = new String[TOTAL_PLAYERS];
    private int comparePlayerId = -1;
    private int boardConfidence = 65;
    private int managerReputation = 45;
    private int wageBudgetK = 850;
    private String passingInstruction = "Mixed";
    private String tacklingInstruction = "Normal";
    private boolean pressingInstruction = false;
    private boolean offsideTrapInstruction = false;
    private boolean counterAttackInstruction = false;
    private boolean menBehindBallInstruction = false;
    private String wibWobShape = "Balanced";
    // Fitness, Tactics, Skills, Shooting, Goalkeeping: 0 None, 1 Light, 2 Medium, 3 Intensive
    private final int[] trainingIntensity = {2, 2, 2, 2, 2};
    private final ArrayList<NewsItem> inbox = new ArrayList<>();
    private String liveWeather = "Dry";
    private int liveRefereeStrictness = 10;
    private int liveHomeFouls = 0;
    private int liveAwayFouls = 0;
    private int liveHomeYellows = 0;
    private int liveAwayYellows = 0;
    private final ArrayList<String> liveCommentaryHistory = new ArrayList<>();

    // Classic 2D live match engine
    private Choreographer.FrameCallback matchFrameCallback;
    private float liveSimulationAccumulator = 0f;
    private static final float LIVE_FIXED_STEP = 1f / 120f;
    private boolean liveMatchActive = false;
    private boolean livePaused = false;
    private int liveSpeed = 1;
    private float liveMinuteFloat = 0f;
    private int liveMinute = 0;
    private int liveHome = -1;
    private int liveAway = -1;
    private int liveHomeGoals = 0;
    private int liveAwayGoals = 0;
    private int liveHomeShots = 0;
    private int liveAwayShots = 0;
    private int liveHomeOnTarget = 0;
    private int liveAwayOnTarget = 0;
    private int liveHomePossession = 50;
    private int liveAwayPossession = 50;
    private int liveHomeCorners = 0;
    private int liveAwayCorners = 0;
    private String liveCommentary = "The teams are ready.";
    private String liveMentality = "Balanced";
    private String liveFormation = "4-3-3";
    private final ArrayList<int[]> liveOtherFixtures = new ArrayList<>();
    private MatchPitchView livePitchView;
    private TextView liveScoreText;
    private TextView liveClockText;
    private TextView liveCommentaryText;
    private TextView liveStatsText;
    private int liveFrameCounter = 0;
    private int liveSubsUsed = 0;
    private final ArrayList<Integer> liveXIIds = new ArrayList<>();
    private final ArrayList<Integer> liveBenchIds = new ArrayList<>();
    private final ArrayList<Integer> liveUsedOffIds = new ArrayList<>();


    // v2.2 club finance and stadium development
    private int financeBalanceK = 0;
    private int financeCurrentMonthIncomeK = 0;
    private int financeCurrentMonthExpenseK = 0;
    private int financeLastMonthIncomeK = 0;
    private int financeLastMonthExpenseK = 0;
    private int financeSeasonIncomeK = 0;
    private int financeSeasonExpenseK = 0;
    private int financeLastYearIncomeK = 0;
    private int financeLastYearExpenseK = 0;
    private int financeMonthStamp = 0;

    private int financeGateReceiptsK = 0;
    private int financeBroadcastK = 0;
    private int financeSponsorshipK = 0;
    private int financeMerchandiseK = 0;
    private int financePlayerSalesK = 0;
    private int financeOtherIncomeK = 0;

    private int financePlayerWagesK = 0;
    private int financeStaffWagesK = 0;
    private int financeTransferSpendK = 0;
    private int financeStadiumSpendK = 0;
    private int financeMaintenanceK = 0;
    private int financeTravelK = 0;
    private int financeYouthScoutingK = 0;
    private int financeOtherExpenseK = 0;

    private String stadiumName = "";
    private int stadiumCapacity = 0;
    private int stadiumSeatedCapacity = 0;
    private int stadiumExpansionLimit = 0;
    private int stadiumAverageAttendance = 0;
    private int stadiumHomeGames = 0;
    private boolean stadiumCovered = false;
    private boolean stadiumUndersoilHeating = true;

    private int stadiumNorthLevel = 1;
    private int stadiumEastLevel = 1;
    private int stadiumSouthLevel = 1;
    private int stadiumWestLevel = 1;

    private int stadiumTrainingLevel = 2;
    private int stadiumMedicalLevel = 2;
    private int stadiumGymLevel = 2;
    private int stadiumYouthLevel = 2;
    private int stadiumShopLevel = 2;
    private int stadiumBarsLevel = 2;
    private int stadiumRestaurantsLevel = 2;
    private int stadiumHospitalityLevel = 2;
    private int stadiumFanZoneLevel = 2;
    private int stadiumParkingLevel = 2;
    private int stadiumMediaLevel = 2;
    private int stadiumMuseumLevel = 1;

    private String stadiumProjectType = "";
    private int stadiumProjectCode = 0;
    private int stadiumProjectWeeks = 0;
    private int stadiumProjectCostK = 0;
    private int stadiumProjectCapacityGain = 0;

    // Original sound design inspired by the uploaded classic 2D match reference.
    private android.media.SoundPool liveSoundPool;
    private android.media.MediaPlayer liveCrowdPlayer;
    private int liveSoundKick = 0;
    private int liveSoundWhistle = 0;
    private int liveSoundGoal = 0;
    private int liveSoundSave = 0;

    // v0.8 event-driven presentation: commentary, runs, defensive reactions and ball physics share one state.
    private long liveLastFrameNanos = 0L;
    private float liveActionTimer = 0f;
    private String pendingContactCue=null;
    private float pendingContactDelay;
    private float liveActionDuration = 0.70f;
    private int livePossessionTeam = -1;
    private int liveCarrierSlot = 9;
    private int livePendingPossessionTeam = -1;
    private int livePendingCarrierSlot = -1;
    private boolean liveNeedsKickoffReset = false;
    private float liveHomePossessionUnits = 0f;
    private float liveAwayPossessionUnits = 0f;
    private int liveHomePasses = 0;
    private int liveAwayPasses = 0;
    private int liveHomePassesComplete = 0;
    private int liveAwayPassesComplete = 0;
    private int liveHomeTackles = 0;
    private int liveAwayTackles = 0;
    private int liveCommentaryTeam = -1;
    private final ArrayList<Integer> liveHomeLineupIds = new ArrayList<>();
    private final ArrayList<Integer> liveAwayLineupIds = new ArrayList<>();
    private Button liveFormationButton;
    private Button liveInstructionsButton;
    private Button liveClassicButton;
    private Button liveSubButton;
    private Button liveSpeedButton;
    private Button livePauseButton;
    private Button liveTacticsButton;
    private boolean livePausedBeforeTactics = false;
    private boolean liveTacticsSessionActive = false;
    private int livePendingSubOnId = -1;
    private boolean liveHalfTimePending = false;
    private boolean liveHalfTimeBreakTaken = false;
    private boolean liveHalfTimeTacticsActive = false;

    // v0.8 match-flow state: phases, set pieces, loose balls and transition play.
    private String livePhase = "BUILD_UP";
    private int liveTransitionActions = 0;
    private int liveLastPossessionTeam = -1;
    private int liveSequencePasses = 0;
    private String livePendingSetPiece = "";
    private int liveSetPieceTeam = -1;
    private float liveSetPieceX = 0.5f;
    private float liveSetPieceY = 0.5f;
    private boolean liveLooseBall = false;
    private float liveLooseBallX = 0.5f;
    private float liveLooseBallY = 0.5f;

    private final int background = Color.rgb(11, 16, 24);
    private final int panel = Color.rgb(20, 28, 39);
    private final int panelLight = Color.rgb(31, 43, 57);
    private final int accent = Color.rgb(89, 221, 167);
    private final int danger = Color.rgb(185, 55, 62);
    private final int warning = Color.rgb(230, 150, 35);
    private final int text = Color.WHITE;
    private final int muted = Color.rgb(180, 196, 205);

    private static class Player {
        int id;
        int team;
        String name;
        String position;
        int age;
        int overall;
        int pace;
        int technique;
        int passing;
        int finishing;
        int defending;
        int physical;
        int appearances;
        int goals;
        int assists;
        int fitness;
        int morale;
        int valueMillions;
        int contractYears;
        boolean transferListed;
        boolean loanListed;
        boolean onLoan;

        // Hidden / deeper player truth inspired by classic management sims.
        int currentAbility;
        int potentialAbility;
        int consistency;
        int importantMatches;
        int adaptability;
        int professionalism;
        int injuryProneness;
        int pressure;
        int temperament;
        int ambition;
        int weeklyWageK;
        String squadRole = "Squad Player";
        int injuredWeeks = 0;
        boolean freeRole = false;
        boolean forwardRuns = false;
        boolean runWithBall = false;
        boolean longShots = false;

        Player(int id, int team) {
            this.id = id;
            this.team = team;
        }

        int technicalGoalkeeperScore() {
            if (!"GK".equals(position)) return overall;
            return Math.max(45, Math.min(99, (overall * 2 + technique + physical) / 4));
        }
    }

    private static class CalendarEvent {
        LocalDate date;
        String competition;
        String stage;

        CalendarEvent(int year, int month, int day, String competition, String stage) {
            this.date = LocalDate.of(year, month, day);
            this.competition = competition;
            this.stage = stage;
        }
    }

    private static class Fixture {
        LocalDate date;
        String competition;
        String stage;
        int opponent;
        boolean home;
        boolean played;
        int goalsFor = -1;
        int goalsAgainst = -1;

        Fixture(LocalDate date, String competition, String stage, int opponent, boolean home) {
            this.date = date;
            this.competition = competition;
            this.stage = stage;
            this.opponent = opponent;
            this.home = home;
        }
    }

    private static class NewsItem {
        String category;
        String title;
        String body;

        NewsItem(String category, String title, String body) {
            this.category = category;
            this.title = title;
            this.body = body;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        safeEnableImmersiveNavigation();
        prefs = getSharedPreferences("project_mister", MODE_PRIVATE);
        loadEditorData();
        generatePlayers();
        portraits = new PortraitManager(this);
        showMainMenu();
    }

    private void safeEnableImmersiveNavigation() {
        // v2.1.1 Hotfix 4:
        // Temporarily leave Android system navigation unchanged.
        // The original v2.1 immersive navigation method remains
        // below for later reintroduction after device testing.
    }

    private void enableImmersiveNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) safeEnableImmersiveNavigation();
    }

    @Override
    public void onBackPressed() {
        if (backAction != null) {
            backAction.run();
        } else {
            super.onBackPressed();
        }
    }

    private void confirmLeaveLiveMatch() {
        new BossDialog.Builder(this)
                .setTitle("Leave live match?")
                .setMessage("The current match will be abandoned and will not count.")
                .setNegativeButton("Stay", null)
                .setPositiveButton("Leave", (d, w) -> {
                    stopLiveMatchTicker();
                    stopLiveMatchAudio();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    matchInProgress=false;
                    loadSave(selectedSlot);
                    showDashboard();
                })
                .show();
    }


    private LinearLayout createPage(String title, String subtitle, boolean showHome) {
        stopLiveMatchTicker();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(background);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(16), dp(18), dp(20));
        scroll.addView(page);
        TextView brand = makeText("BOSS XI  /  FOOTBALL MANAGEMENT", 11, accent);
        brand.setLetterSpacing(.09f);
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout brandRow=new LinearLayout(this);brandRow.setGravity(Gravity.CENTER_VERTICAL);brandRow.addView(BrandMark.view(this,40));brand.setPadding(dp(12),0,0,0);brandRow.addView(brand);page.addView(brandRow);
        TextView heading = makeText(title, 26, text);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        heading.setPadding(0, dp(8), 0, dp(4));
        page.addView(heading);
        TextView sub = makeText(subtitle, 13, muted);
        sub.setPadding(0, 0, 0, dp(18));
        page.addView(sub);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (showHome && selectedClub >= 0 && !liveTacticsSessionActive) {
            LinearLayout nav = new LinearLayout(this);
            nav.setBackgroundColor(panel);
            nav.setPadding(dp(8), dp(4), dp(8), dp(4));
            String[] labels = {"Home", "Squad", "Tactics", "Inbox"};
            Runnable[] actions = {this::showDashboard, () -> showTeamPlayers(selectedClub), this::showTactics, this::showInbox};
            for (int i=0; i<labels.length; i++) {
                final Runnable action=actions[i];
                Button b=makeButton(labels[i], v -> action.run());
                b.setTextSize(12);
                b.setPadding(dp(4),0,dp(4),0);
                boolean selected=(i==0 && title.equals(clubNames[selectedClub])) || (i==1 && title.endsWith(" Players")) || (i==2 && title.equals("Tactics")) || (i==3 && title.equals("Inbox"));
                if(selected) {b.setTextColor(accent);b.setBackground(buttonBackground(panelLight));}
                else b.setBackgroundColor(Color.TRANSPARENT);
                nav.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));
            }
            root.addView(nav);
        }
        setContentView(root);
        applySafeInsets(root);
        return page;
    }

    private void applySafeInsets(View root) {
        final int l=root.getPaddingLeft(), t=root.getPaddingTop(), r=root.getPaddingRight(), b=root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 35) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(l+bars.left,t+bars.top,r+bars.right,b+bars.bottom);
            }
            return insets;
        });
        root.post(root::requestApplyInsets);
    }

    private TextView makeText(String value, int size, int colour) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(size);
        tv.setTextColor(colour);
        tv.setLineSpacing(0, 1.10f);
        tv.setFontFeatureSettings("tnum");
        return tv;
    }

    private GradientDrawable rounded(int colour) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colour);
        bg.setCornerRadius(dp(14));
        return bg;
    }

    private int lighten(int colour, int amount) {
        return Color.rgb(
                Math.min(255, Color.red(colour) + amount),
                Math.min(255, Color.green(colour) + amount),
                Math.min(255, Color.blue(colour) + amount)
        );
    }

    private int darken(int colour, int amount) {
        return Color.rgb(
                Math.max(0, Color.red(colour) - amount),
                Math.max(0, Color.green(colour) - amount),
                Math.max(0, Color.blue(colour) - amount)
        );
    }

    private GradientDrawable buttonBackground(int baseColour) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(baseColour);
        bg.setCornerRadius(dp(10));
        return bg;
    }

    private int idealButtonTextColour(int backgroundColour) {
        int luminance = (Color.red(backgroundColour) * 299 + Color.green(backgroundColour) * 587 + Color.blue(backgroundColour) * 114) / 1000;
        return luminance > 168 ? Color.BLACK : Color.WHITE;
    }

    private Button makeButton(String label, View.OnClickListener click) {
        Button button = new Button(this);
        button.setText(label.replaceFirst("^[^\\p{L}\\p{N}]+", ""));
        button.setTextSize(14);
        button.setMinimumHeight(dp(48));
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(idealButtonTextColour(panelLight));
        button.setAllCaps(false);
        button.setBackground(buttonBackground(panelLight));
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        button.setElevation(0);
        button.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x25FFFFFF), buttonBackground(panelLight), null));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        button.setLayoutParams(params);
        if (click != null) button.setOnClickListener(click);
        return button;
    }

    private Button makeAccentButton(String label, View.OnClickListener click) {
        Button button = makeButton(label, click);
        button.setTextColor(idealButtonTextColour(accent));
        button.setBackground(buttonBackground(accent));
        return button;
    }

    private LinearLayout makePanel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.setBackground(rounded(panel));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(14));
        box.setLayoutParams(params);
        return box;
    }

    private View makeSwatch(int colour) {
        View v = new View(this);
        v.setBackground(rounded(colour));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(22), dp(22));
        lp.setMargins(0, 0, dp(8), 0);
        v.setLayoutParams(lp);
        return v;
    }


    private LinearLayout makeInfoStatRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(buttonBackground(Color.rgb(11, 60, 74)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(lp);

        TextView left = makeText(label, 13, muted);
        left.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        left.setLayoutParams(leftLp);
        row.addView(left);

        TextView right = makeText(value, 16, text);
        right.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(right);
        return row;
    }

    private LinearLayout makeHighlightTile(String label, String value, int baseColour) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(10), dp(10), dp(10), dp(10));
        tile.setBackground(buttonBackground(baseColour));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(3), 0, dp(3), dp(8));
        tile.setLayoutParams(lp);

        TextView t1 = makeText(label, 11, idealButtonTextColour(baseColour));
        t1.setTypeface(Typeface.DEFAULT_BOLD);
        tile.addView(t1);
        TextView t2 = makeText(value, 18, idealButtonTextColour(baseColour));
        t2.setTypeface(Typeface.DEFAULT_BOLD);
        tile.addView(t2);
        return tile;
    }

    private LinearLayout makeClubIdentityRow(int club) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(makeSwatch(primaryColours[club]));
        row.addView(makeSwatch(secondaryColours[club]));

        TextView name = makeText(clubNames[club], 18, text);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setPadding(dp(4), 0, 0, 0);
        row.addView(name);
        return row;
    }

    private void showMainMenu() {
        backAction = null;
        LinearLayout page = createPage("Career Hub", "BOSS XI v2.3 • Matchday & Presentation", false);

        for (int slot = 0; slot < SAVE_SLOTS; slot++) {
            final int s = slot;
            LinearLayout box = makePanel();

            TextView slotTitle = makeText("SAVE " + (slot + 1), 13, accent);
            slotTitle.setTypeface(Typeface.DEFAULT_BOLD);
            box.addView(slotTitle);

            if (slotExists(slot)) {
                int club = prefs.getInt(key(slot, "club"), 0);
                int md = prefs.getInt(key(slot, "matchday"), 0);
                int pts = getSavedPoints(slot, club);
                String date = prefs.getString(key(slot, "date"), SEASON_START.toString());

                TextView details = makeText(
                        clubNames[club] + "\nMatchday " + md + "  •  " + pts + " pts\n" + date,
                        18,
                        text
                );
                details.setTypeface(Typeface.DEFAULT_BOLD);
                details.setPadding(0, dp(8), 0, dp(12));
                box.addView(details);

                box.addView(makeAccentButton("Continue Career", v -> {
                    loadSave(s);
                    showDashboard();
                }));

                Button delete = makeButton("Delete Save", v -> confirmDeleteSave(s));
                delete.setTextColor(idealButtonTextColour(danger));
                delete.setBackground(buttonBackground(danger));
                box.addView(delete);
            } else {
                TextView empty = makeText("Empty career slot\nCreate your manager profile and begin a new journey.", 17, muted);
                empty.setPadding(0, dp(8), 0, dp(12));
                box.addView(empty);

                box.addView(makeAccentButton("Start New Career", v -> {
                    selectedSlot = s;
                    resetCareerState();
                    generatePlayers();
                    initialiseNewManagerDefaults();
                    showNewCareerSetup();
                }));
            }

            page.addView(box);
        }

        TextView editorTitle = makeText("GAME TOOLS", 13, muted);
        editorTitle.setTypeface(Typeface.DEFAULT_BOLD);
        editorTitle.setPadding(0, dp(6), 0, dp(8));
        page.addView(editorTitle);
        page.addView(makeButton("✏  Game Editor — Teams & Colours", v -> showTeamEditor()));
    }

    private void initialiseNewManagerDefaults() {
        managerFirstName = "";
        managerLastName = "";
        managerDob = "01/01/1990";
        managerGender = "Male";
        managerCountryIndex = 0;
        managerLeagueIndex = 0;
    }

    private void showNewCareerSetup() {
        backAction = () -> showMainMenu();
        LinearLayout page = createPage(
                "New Career",
                "Save " + (selectedSlot + 1) + " • Enter manager details before choosing a club",
                true
        );

        TextView intro = makeText("Manager profile", 14, muted);
        intro.setTypeface(Typeface.DEFAULT_BOLD);
        intro.setPadding(0, 0, 0, dp(8));
        page.addView(intro);

        EditText firstNameInput = new EditText(this);
        styleCareerInput(firstNameInput, "First name");
        firstNameInput.setText(managerFirstName);
        page.addView(firstNameInput);

        EditText lastNameInput = new EditText(this);
        styleCareerInput(lastNameInput, "Last name");
        lastNameInput.setText(managerLastName);
        page.addView(lastNameInput);

        EditText dobInput = new EditText(this);
        styleCareerInput(dobInput, "Date of birth (DD/MM/YYYY)");
        dobInput.setText(managerDob);
        page.addView(dobInput);

        final Button genderButton = makeButton("Gender: " + managerGender, null);
        genderButton.setOnClickListener(v -> {
            final String[] options = {"Male", "Female"};
            new BossDialog.Builder(this)
                    .setTitle("Select gender")
                    .setItems(options, (d, which) -> {
                        managerGender = options[which];
                        genderButton.setText("Gender: " + managerGender);
                    })
                    .show();
        });
        page.addView(genderButton);

        final Button countryButton = makeButton("Country of origin: " + COUNTRY_OPTIONS[managerCountryIndex], null);
        countryButton.setOnClickListener(v -> new BossDialog.Builder(this)
                .setTitle("Country of origin")
                .setItems(COUNTRY_OPTIONS, (d, which) -> {
                    managerCountryIndex = which;
                    countryButton.setText("Country of origin: " + COUNTRY_OPTIONS[managerCountryIndex]);
                })
                .show());
        page.addView(countryButton);

        final Button leagueButton = makeButton("Starting league: " + LEAGUE_OPTIONS[managerLeagueIndex], null);
        leagueButton.setOnClickListener(v -> new BossDialog.Builder(this)
                .setTitle("Starting league")
                .setItems(LEAGUE_OPTIONS, (d, which) -> {
                    managerLeagueIndex = which;
                    leagueButton.setText("Starting league: " + LEAGUE_OPTIONS[managerLeagueIndex]);
                })
                .show());
        page.addView(leagueButton);

        TextView note = makeText("Flags show your country of origin. More starting leagues can be added in later database updates.", 13, muted);
        note.setPadding(0, 0, 0, dp(12));
        page.addView(note);

        page.addView(makeAccentButton("Continue to Club Selection", v -> {
            String first = firstNameInput.getText().toString().trim();
            String last = lastNameInput.getText().toString().trim();
            String dob = dobInput.getText().toString().trim();
            if (first.isEmpty() || last.isEmpty() || dob.isEmpty()) {
                Toast.makeText(this, "Please complete first name, last name and date of birth.", Toast.LENGTH_SHORT).show();
                return;
            }
            managerFirstName = first;
            managerLastName = last;
            managerDob = dob;
            showClubSelection();
        }));

        page.addView(makeButton("Back to Career Hub", v -> showMainMenu()));
    }

    private void styleCareerInput(EditText input, String hint) {
        input.setHint(hint);
        input.setTextColor(text);
        input.setHintTextColor(muted);
        input.setTextSize(17);
        input.setSingleLine(true);
        input.setBackground(rounded(panel));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(10));
        input.setLayoutParams(lp);
    }

    private String managerFullName() {
        return (managerFirstName + " " + managerLastName).trim();
    }

    private void showClubSelection() {
        backAction = () -> showMainMenu();
        LinearLayout page = createPage(
                "Choose your club",
                "Save " + (selectedSlot + 1) + " • " + managerFullName() + " • " + LEAGUE_OPTIONS[managerLeagueIndex],
                true
        );

        for (int i = 0; i < clubNames.length; i++) {
            final int club = i;
            LinearLayout box = makePanel();
            box.addView(makeClubIdentityRow(club));

            TextView info = makeText(
                    "Strength " + strength[i] + "/100   •   Budget €" + budgets[i] + "m\nEurope: " + europeanCompetitionForClub(i),
                    14,
                    muted
            );
            info.setPadding(0, dp(8), 0, dp(10));
            box.addView(info);

            box.addView(makeButton("Manage " + clubNames[i], v -> {
                selectedClub = club;
                resetCareerState();
                generatePlayers();
                initialiseTacticsForClub();
                initialiseClassicCareerSystems();
                saveCurrentGame();
                showDashboard();
            }));
            page.addView(box);
        }
    }

    private void showDashboard() {
        backAction=() -> { saveCurrentGame(); showMainMenu(); };
        LinearLayout page=createPage(clubNames[selectedClub],currentDate.format(DATE_FORMAT)+"  •  Matchweek "+(matchday+1),true);
        LinearLayout fixture=makePanel();
        fixture.addView(profileSectionTitle("NEXT FIXTURE"));
        int opponent=leagueOpponentForRound(matchday);
        fixture.addView(makeText(clubNames[selectedClub]+"  v  "+clubNames[opponent],21,text));
        fixture.addView(makeText((selectedHomeForRound(matchday)?"Home":"Away")+"  •  League  •  "+tacticFormation,13,muted));
        fixture.addView(makeAccentButton("Match centre",v->startLiveMatchday()));
        page.addView(fixture);
        int rank=1;
        for(int i=0;i<clubNames.length;i++) if(i!=selectedClub && (points[i]>points[selectedClub] || (points[i]==points[selectedClub] && goalsFor[i]-goalsAgainst[i]>goalsFor[selectedClub]-goalsAgainst[selectedClub]))) rank++;
        int fit=0,count=0,injured=0;
        for(Player p:players) if(p.team==selectedClub) { fit+=p.fitness;count++;if(p.injuredWeeks>0)injured++; }
        LinearLayout overview=makePanel();
        overview.addView(profileSectionTitle("CLUB PULSE"));
        overview.addView(profileInfoRow("League position",rank+" / 18  •  "+points[selectedClub]+" pts"));
        overview.addView(profileInfoRow("Bank balance",moneyK(financeBalanceK)));
        overview.addView(profileInfoRow("Squad condition",(count==0?0:fit/count)+"%  •  "+injured+" injured"));
        overview.addView(profileInfoRow("Board confidence",boardConfidence+"%"));
        overview.addView(profileInfoRow("Manager",managerFullName()));
        if(matchday>0 && matchday-1<clubMatchGF.length) overview.addView(profileInfoRow("Last result",clubMatchGF[matchday-1]+" – "+clubMatchGA[matchday-1]));
        StringBuilder form=new StringBuilder();
        for(int i=Math.max(0,matchday-5);i<Math.min(matchday,clubMatchGF.length);i++) if(clubMatchGF[i]>=0) form.append(clubMatchGF[i]>clubMatchGA[i]?"W  ":clubMatchGF[i]<clubMatchGA[i]?"L  ":"D  ");
        overview.addView(profileInfoRow("Recent form",form.length()==0?"Season opening":form.toString()));
        page.addView(overview);
        LinearLayout alerts=makePanel();
        alerts.addView(profileSectionTitle("MANAGER BRIEFING"));
        alerts.addView(makeText(boardExpectationText(),14,text));
        if(injured>0) alerts.addView(makeText(injured+" player(s) unavailable — review your XI.",13,muted));
        if(count>0 && fit/count<80) alerts.addView(makeText("Assistant advice: consider recovery training and rotation.",13,accent));
        if(!inbox.isEmpty()) alerts.addView(makeText(inbox.get(0).title,14,text));
        alerts.addView(makeButton("Read inbox",v->showInbox()));
        page.addView(alerts);
        page.addView(profileSectionTitle("FIRST TEAM"));
        dashboardRow(page,"Squad",()->showTeamPlayers(selectedClub),"Tactics",this::showTactics);
        dashboardRow(page,"Training",this::showTraining,"Staff",this::showStaffHub);
        dashboardRow(page,"Transfers",this::showTransferHub,"Scouting",this::showScoutCentre);
        page.addView(profileSectionTitle("CLUB & SEASON"));
        dashboardRow(page,"Fixtures",this::showCompetitionCalendar,"League table",this::showLeagueTable);
        dashboardRow(page,"Finances",this::showFinances,"Stadium",this::showStadiumCentre);
        dashboardRow(page,"Board",this::showClubOffice,"Manager",this::showManagerProfile);
        dashboardRow(page,"Player database",this::showAllTeamsPlayers,"Sound settings",this::showSoundSettings);
    }

    private void dashboardRow(LinearLayout page,String a,Runnable ra,String b,Runnable rb) {
        LinearLayout row=new LinearLayout(this);
        Button left=makeButton(a,v->ra.run()),right=makeButton(b,v->rb.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(52),1); lp.setMargins(0,0,dp(5),dp(8));
        row.addView(left,lp);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(52),1); rp.setMargins(dp(5),0,0,dp(8));
        row.addView(right,rp);page.addView(row);
    }

    private void showSoundSettings() {
        String[] labels={"Stadium crowd", "Match effects"};
        boolean[] enabled={prefs.getBoolean("audio_crowd",true),prefs.getBoolean("audio_effects",true)};
        new BossDialog.Builder(this).setTitle("Match audio").setMultiChoiceItems(labels,enabled,(d,i,checked)->{
            prefs.edit().putBoolean(i==0?"audio_crowd":"audio_effects",checked).apply();
            if(audio!=null) audio.settings(prefs.getBoolean("audio_crowd",true),prefs.getBoolean("audio_effects",true));
        }).setPositiveButton("Done",null).show();
    }

    private void showManagerProfile() {
        backAction = () -> showDashboard();
        String name = managerFullName().isEmpty() ? "Manager" : managerFullName();
        LinearLayout page = createPage(
                "Manager Profile",
                clubNames[selectedClub] + " • " + name,
                false
        );

        int mp = played[selectedClub];
        int mw = won[selectedClub];
        int md = drawn[selectedClub];
        int ml = lost[selectedClub];
        int gf = goalsFor[selectedClub];
        int ga = goalsAgainst[selectedClub];
        int winPct = mp == 0 ? 0 : Math.round(100f * mw / mp);

        LinearLayout hero = makePanel();
        hero.setBackground(rounded(Color.rgb(9, 31, 43)));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.TOP);

        View portrait = new RealisticHumanPortraitView(
                name.hashCode(),
                "MANAGER",
                primaryColours[selectedClub],
                secondaryColours[selectedClub],
                43,
                false
        );
        LinearLayout.LayoutParams portraitLp = new LinearLayout.LayoutParams(dp(112), dp(112));
        portraitLp.setMargins(0, 0, dp(16), 0);
        top.addView(portrait, portraitLp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView role = makeText("FIRST-TEAM MANAGER", 12, accent);
        role.setTypeface(Typeface.DEFAULT_BOLD);
        role.setLetterSpacing(0.04f);
        info.addView(role);
        TextView title = makeText(name, 25, text);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(3), 0, dp(5));
        info.addView(title);
        info.addView(makeText(
                clubNames[selectedClub] + "\n"
                        + COUNTRY_OPTIONS[Math.max(0, Math.min(managerCountryIndex, COUNTRY_OPTIONS.length - 1))]
                        + "  •  DOB " + managerDob,
                13,
                muted
        ));
        top.addView(info);
        hero.addView(top);

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.setPadding(0, dp(14), 0, 0);
        metrics.addView(profileMetricCard("REPUTATION", managerReputation + "/100", accent));
        metrics.addView(profileMetricCard("BOARD", boardConfidence + "%", boardConfidence >= 55 ? accent : warning));
        metrics.addView(profileMetricCard("WIN RATE", winPct + "%", winPct >= 50 ? accent : text));
        hero.addView(metrics);
        page.addView(hero);

        LinearLayout contract = makePanel();
        contract.addView(profileSectionTitle("CONTRACT & CLUB"));
        contract.addView(profileInfoRow("Club", clubNames[selectedClub]));
        contract.addView(profileInfoRow("Weekly wage", "€" + managerWeeklyWageK + "k"));
        contract.addView(profileInfoRow("Contract length", managerContractYears + " year(s)"));
        contract.addView(profileInfoRow("Approx. expiry", currentDate.plusYears(managerContractYears).format(DATE_FORMAT)));
        contract.addView(profileInfoRow("Board expectation", boardExpectationText()));
        page.addView(contract);

        LinearLayout ability = makePanel();
        ability.addView(profileSectionTitle("MANAGER ABILITIES • 1–20"));
        ability.addView(makeModernAttributeRow("Tactical knowledge", managerTactical, Color.rgb(87, 166, 221)));
        ability.addView(makeModernAttributeRow("Motivating", managerMotivating, accent));
        ability.addView(makeModernAttributeRow("Discipline", managerDiscipline, Color.rgb(231, 179, 70)));
        ability.addView(makeModernAttributeRow("Player knowledge", managerPlayerKnowledge, Color.rgb(87, 166, 221)));
        ability.addView(makeModernAttributeRow("Youth development", managerYouth, Color.rgb(183, 133, 226)));
        ability.addView(makeModernAttributeRow("Negotiating", managerNegotiating, Color.rgb(225, 111, 62)));
        page.addView(ability);

        LinearLayout record = makePanel();
        record.addView(profileSectionTitle("CAREER RECORD"));
        record.addView(makeManagerHistoryHeader());
        record.addView(makeManagerHistoryRow(clubNames[selectedClub], mp, mw, md, ml, gf, ga, winPct));
        TextView summary = makeText(
                "Matches " + mp + "   •   Wins " + mw + "   •   Draws " + md + "   •   Losses " + ml,
                13,
                muted
        );
        summary.setPadding(0, dp(9), 0, 0);
        record.addView(summary);
        page.addView(record);

        LinearLayout milestonePanel = makePanel();
        milestonePanel.addView(profileSectionTitle("MANAGER STATUS"));
        String milestone = mw >= 20 ? "Established winner • " + mw + " career wins"
                : mw >= 10 ? "Building momentum • " + mw + " career wins"
                : mw > 0 ? "Early career progress • " + mw + " win(s)"
                : "The managerial career is just beginning.";
        milestonePanel.addView(makeText(milestone, 14, text));
        page.addView(milestonePanel);
    }

    private LinearLayout makeManagerProfileRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(7), 0, dp(7));
        TextView left = makeText(label, 14, muted);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(left);
        TextView right = makeText(value, 14, text);
        right.setTypeface(Typeface.DEFAULT_BOLD);
        right.setGravity(Gravity.END);
        row.addView(right);
        return row;
    }

    private LinearLayout makeManagerAttributeRow(String label, int value) {
        LinearLayout row = makeManagerProfileRow(label, String.valueOf(Math.max(1, Math.min(20, value))));
        return row;
    }

    private LinearLayout makeManagerHistoryHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(5), dp(6), dp(5), dp(6));
        row.setBackgroundColor(Color.rgb(29, 57, 82));
        row.addView(makeManagerHistoryCell("CLUB", 2.3f, true));
        row.addView(makeManagerHistoryCell("P", 0.55f, true));
        row.addView(makeManagerHistoryCell("W", 0.55f, true));
        row.addView(makeManagerHistoryCell("D", 0.55f, true));
        row.addView(makeManagerHistoryCell("L", 0.55f, true));
        row.addView(makeManagerHistoryCell("GF", 0.65f, true));
        row.addView(makeManagerHistoryCell("GA", 0.65f, true));
        row.addView(makeManagerHistoryCell("W%", 0.75f, true));
        return row;
    }

    private LinearLayout makeManagerHistoryRow(String club, int p, int w, int d, int l, int gf, int ga, int winPct) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(5), dp(8), dp(5), dp(8));
        row.setBackgroundColor(Color.rgb(14, 39, 53));
        row.addView(makeManagerHistoryCell(club, 2.3f, false));
        row.addView(makeManagerHistoryCell(String.valueOf(p), 0.55f, false));
        row.addView(makeManagerHistoryCell(String.valueOf(w), 0.55f, false));
        row.addView(makeManagerHistoryCell(String.valueOf(d), 0.55f, false));
        row.addView(makeManagerHistoryCell(String.valueOf(l), 0.55f, false));
        row.addView(makeManagerHistoryCell(String.valueOf(gf), 0.65f, false));
        row.addView(makeManagerHistoryCell(String.valueOf(ga), 0.65f, false));
        row.addView(makeManagerHistoryCell(winPct + "%", 0.75f, false));
        return row;
    }

    private TextView makeManagerHistoryCell(String value, float weight, boolean header) {
        TextView tv = makeText(value, header ? 10 : 12, header ? muted : text);
        tv.setSingleLine(true);
        if (header) tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        return tv;
    }

    private class ManagerPortraitView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        ManagerPortraitView() { super(MainActivity.this); }
        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            paint.setShader(new android.graphics.LinearGradient(0, 0, 0, h, Color.rgb(38, 55, 69), Color.rgb(13, 25, 34), android.graphics.Shader.TileMode.CLAMP));
            r.set(0,0,w,h); canvas.drawRoundRect(r, dp(10), dp(10), paint); paint.setShader(null);
            float cx=w*.50f, cy=h*.37f, rx=w*.18f, ry=h*.17f;
            int skin=Color.rgb(211,168,128), hair=Color.rgb(39,31,27);
            paint.setColor(Color.rgb(25,30,35)); r.set(w*.13f,h*.65f,w*.87f,h*.98f); canvas.drawRoundRect(r,dp(16),dp(16),paint);
            paint.setColor(Color.rgb(233,233,233)); r.set(w*.39f,h*.64f,w*.61f,h*.77f); canvas.drawRoundRect(r,dp(4),dp(4),paint);
            paint.setColor(skin); r.set(w*.44f,h*.54f,w*.56f,h*.69f); canvas.drawRoundRect(r,dp(4),dp(4),paint);
            paint.setShader(new android.graphics.RadialGradient(cx-rx*.35f, cy-ry*.35f, ry*1.45f, lighten(skin,22), darken(skin,30), android.graphics.Shader.TileMode.CLAMP));
            r.set(cx-rx,cy-ry,cx+rx,cy+ry); canvas.drawOval(r,paint); paint.setShader(null);
            paint.setColor(hair); r.set(cx-rx,cy-ry*1.05f,cx+rx,cy-ry*.18f); canvas.drawArc(r,180,180,true,paint);
            paint.setColor(Color.rgb(245,245,245)); canvas.drawCircle(cx-rx*.38f,cy-ry*.05f,dp(4),paint); canvas.drawCircle(cx+rx*.38f,cy-ry*.05f,dp(4),paint);
            paint.setColor(Color.rgb(45,55,60)); canvas.drawCircle(cx-rx*.38f,cy-ry*.05f,dp(1.7f),paint); canvas.drawCircle(cx+rx*.38f,cy-ry*.05f,dp(1.7f),paint);
            paint.setColor(darken(skin,38)); paint.setStrokeWidth(dp(1.3f)); canvas.drawLine(cx,cy, cx-dp(2),cy+dp(11),paint);
            paint.setColor(Color.rgb(116,67,62)); paint.setStrokeWidth(dp(1.6f)); canvas.drawLine(cx-dp(8),cy+dp(18),cx+dp(8),cy+dp(18),paint);
        }
    }


    private void initLiveAudio() {
        if(audio==null) {try {audio=new MatchAudio(this);} catch(RuntimeException e){android.util.Log.w("BOSSXI","Audio unavailable",e);return;}}
        audio.settings(prefs.getBoolean("audio_crowd",true),prefs.getBoolean("audio_effects",true));
    }

    private void startLiveCrowdAudio() { initLiveAudio(); if(audio!=null)audio.start(); }

    private void queueContactSound(String cue) { pendingContactCue=cue;pendingContactDelay=liveActionDuration; }

    private void playLiveSound(String cue) { if(audio!=null) audio.cue(cue); }

    private void stopLiveMatchAudio() { if(audio!=null) { audio.close(); audio=null; } }

    private void startLiveMatchday() {
        if (countRole(2) != 11) {
            Toast.makeText(this, "Select exactly 11 starters in Tactics before the match.", Toast.LENGTH_LONG).show();
            showTactics();
            return;
        }

        saveCurrentGame();
        matchInProgress=true;pendingContactCue=null;
        matchGoals.clear();matchAssists.clear();pendingGoalTeam=-1;
        int opponent = leagueOpponentForRound(matchday);
        boolean selectedHome = selectedHomeForRound(matchday);
        liveHome = selectedHome ? selectedClub : opponent;
        liveAway = selectedHome ? opponent : selectedClub;

        ArrayList<Integer> remaining = new ArrayList<>();
        for (int i = 0; i < clubNames.length; i++) {
            if (i != liveHome && i != liveAway) remaining.add(i);
        }
        Collections.shuffle(remaining, new Random(9000L + matchday));
        liveOtherFixtures.clear();
        for (int i = 0; i + 1 < remaining.size(); i += 2) {
            liveOtherFixtures.add(new int[]{remaining.get(i), remaining.get(i + 1)});
        }

        liveHomeGoals = 0;
        liveAwayGoals = 0;
        liveHomeShots = 0;
        liveAwayShots = 0;
        liveHomeOnTarget = 0;
        liveAwayOnTarget = 0;
        liveHomeCorners = 0;
        liveAwayCorners = 0;
        liveHomePossession = 50;
        liveAwayPossession = 50;
        liveMinuteFloat = 0f;
        liveMinute = 0;
        liveSpeed = 1;
        livePaused = false;
        liveMentality = styleToMentality(playStyle);
        liveFormation = tacticFormation;
        liveWeather = randomWeather();
        liveRefereeStrictness = 6 + random.nextInt(10);
        liveHomeFouls = 0;
        liveAwayFouls = 0;
        liveHomeYellows = 0;
        liveAwayYellows = 0;
        liveCommentaryHistory.clear();
        liveCommentary = "Kick-off is moments away. Weather: " + liveWeather + ".";
        liveCommentaryHistory.add(liveCommentary);
        liveCommentaryTeam = -1;
        liveFrameCounter = 0;
        liveSubsUsed = 0;
        liveUsedOffIds.clear();
        liveHalfTimePending = false;
        liveHalfTimeBreakTaken = false;
        liveHalfTimeTacticsActive = false;
        liveTacticsSessionActive = false;
        liveLastFrameNanos = 0L;
        liveActionTimer = 0f;
        liveActionDuration = 0.55f;
        livePossessionTeam = liveHome;
        liveCarrierSlot = 9;
        livePendingPossessionTeam = -1;
        livePendingCarrierSlot = -1;
        liveNeedsKickoffReset = true;
        liveHomePossessionUnits = 0f;
        liveAwayPossessionUnits = 0f;
        liveHomePasses = liveAwayPasses = 0;
        liveHomePassesComplete = liveAwayPassesComplete = 0;
        liveHomeTackles = liveAwayTackles = 0;
        livePhase = "BUILD_UP";
        liveTransitionActions = 0;
        liveLastPossessionTeam = -1;
        liveSequencePasses = 0;
        livePendingSetPiece = "";
        liveSetPieceTeam = -1;
        liveLooseBall = false;
        liveLooseBallX = liveLooseBallY = 0.5f;
                          prepareLiveLineup();
        prepareLivePitchLineups();
        substitutions.reset(liveXIIds);substitutionsPending=false;
        matchTackles.clear();matchCompletedPasses.clear();matchMissedPasses.clear();matchYellows.clear();
        matchParticipants.clear(); matchParticipants.addAll(liveHomeLineupIds); matchParticipants.addAll(liveAwayLineupIds);
        aiDeparted.clear(); aiSubsUsed=0;lastAiReview=-1; lastPasserId=-1; lastPassTeam=-1;
        initLiveAudio();
        startLiveCrowdAudio();
        playLiveSound("whistle");

        showLiveMatch();
    }

    private void showLiveMatch() {
        stopLiveMatchTicker();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        safeEnableImmersiveNavigation();
        backAction = () -> confirmLeaveLiveMatch();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(8), dp(5), dp(8), dp(5));
        root.setBackgroundColor(background);

        // CM 03/04-inspired match header: club crests flank a bright central score/clock card.
        LinearLayout scoreboard = new LinearLayout(this);
        scoreboard.setOrientation(LinearLayout.HORIZONTAL);
        scoreboard.setGravity(Gravity.CENTER_VERTICAL);
        scoreboard.setPadding(dp(8), dp(3), dp(8), dp(3));
        scoreboard.setBackground(rounded(Color.rgb(5, 28, 40)));

        LinearLayout homeBlock = new LinearLayout(this);
        homeBlock.setOrientation(LinearLayout.HORIZONTAL);
        homeBlock.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        TextView homeName = makeText(clubNames[liveHome], 13, text);
        homeName.setTypeface(Typeface.DEFAULT_BOLD);
        homeName.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        homeName.setPadding(dp(2), 0, dp(7), 0);
        homeBlock.addView(homeName, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ClubShieldView homeShield = new ClubShieldView(liveHome);
        homeBlock.addView(homeShield, new LinearLayout.LayoutParams(dp(62), dp(58)));
        scoreboard.addView(homeBlock, new LinearLayout.LayoutParams(0, dp(62), 1f));

        LinearLayout scoreCard = new LinearLayout(this);
        scoreCard.setOrientation(LinearLayout.VERTICAL);
        scoreCard.setGravity(Gravity.CENTER);
        scoreCard.setPadding(dp(12), dp(3), dp(12), dp(3));
        scoreCard.setBackground(rounded(panelLight));
        liveScoreText = makeText(liveHomeGoals + "  -  " + liveAwayGoals, 26, text);
        liveScoreText.setTypeface(Typeface.DEFAULT_BOLD);
        liveScoreText.setGravity(Gravity.CENTER);
        scoreCard.addView(liveScoreText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.15f));
        liveClockText = makeText("00:00      1st", 14, accent);
        liveClockText.setTypeface(Typeface.DEFAULT_BOLD);
        liveClockText.setGravity(Gravity.CENTER);
        scoreCard.addView(liveClockText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.85f));
        LinearLayout.LayoutParams scoreLp = new LinearLayout.LayoutParams(dp(158), dp(62));
        scoreLp.setMargins(dp(8), 0, dp(8), 0);
        scoreboard.addView(scoreCard, scoreLp);

        LinearLayout awayBlock = new LinearLayout(this);
        awayBlock.setOrientation(LinearLayout.HORIZONTAL);
        awayBlock.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        ClubShieldView awayShield = new ClubShieldView(liveAway);
        awayBlock.addView(awayShield, new LinearLayout.LayoutParams(dp(62), dp(58)));
        TextView awayName = makeText(clubNames[liveAway], 13, text);
        awayName.setTypeface(Typeface.DEFAULT_BOLD);
        awayName.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        awayName.setPadding(dp(7), 0, dp(2), 0);
        awayBlock.addView(awayName, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        scoreboard.addView(awayBlock, new LinearLayout.LayoutParams(0, dp(62), 1f));

        root.addView(scoreboard, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(68)));

        liveCommentaryText = makeText(liveCommentary, 16, Color.WHITE);
        liveCommentaryText.setTypeface(Typeface.DEFAULT_BOLD);
        liveCommentaryText.setGravity(Gravity.CENTER);
        liveCommentaryText.setPadding(dp(10), dp(6), dp(10), dp(6));
        liveCommentaryText.setBackground(rounded(panelLight));
        LinearLayout.LayoutParams commentaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        commentaryParams.setMargins(0, dp(4), 0, dp(4));
        root.addView(liveCommentaryText, commentaryParams);

        LinearLayout middle = new LinearLayout(this);
        middle.setOrientation(LinearLayout.HORIZONTAL);
        middle.setGravity(Gravity.CENTER_VERTICAL);

        if(livePitchView==null || liveMinute==0) livePitchView = new MatchPitchView();
        if(livePitchView.getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup)livePitchView.getParent()).removeView(livePitchView);
        livePitchView.refreshFormationAnchors();
        if (liveMinute == 0) livePitchView.resetForKickoff(livePossessionTeam);
        LinearLayout.LayoutParams pitchParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 3.70f);
        pitchParams.setMargins(0, 0, dp(7), 0);
        middle.addView(livePitchView, pitchParams);

        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setPadding(dp(6), dp(3), dp(6), dp(3));
        side.setBackground(rounded(panel));
        LinearLayout matchBrand=new LinearLayout(this);matchBrand.setGravity(Gravity.CENTER_VERTICAL);matchBrand.addView(BrandMark.view(this,22));TextView matchBrandText=makeText(" BOSS XI • LIVE",10,accent);matchBrand.addView(matchBrandText);side.addView(matchBrand);

        TextView details = makeText("MATCH STATS", 10, accent);
        details.setTypeface(Typeface.DEFAULT_BOLD);
        details.setGravity(Gravity.CENTER);
        side.addView(details, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(22)));

        liveStatsText = makeText(liveStatsCompactSummary(), 11, text);
        liveStatsText.setTypeface(Typeface.MONOSPACE);
        liveStatsText.setGravity(Gravity.CENTER_HORIZONTAL);
        liveStatsText.setPadding(dp(3), 0, dp(3), dp(3));
        side.addView(liveStatsText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(102)));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        livePauseButton = (Button) liveGridButton(livePaused ? "▶ Resume" : "Ⅱ Pause", v -> {
            livePaused = !livePaused;
            liveLastFrameNanos = 0L;
            liveSimulationAccumulator = 0f;
            refreshLiveHeader();
        });
        liveSpeedButton = (Button) liveGridButton("Speed • " + liveSpeedLabel(), v -> showLiveSpeedDialog());
        liveTacticsButton = (Button) liveGridButton("Tactics", v -> showLiveTacticsScreen());
        controls.addView(liveControlRow(livePauseButton, liveTacticsButton));
        controls.addView(liveControlRow(
                liveSpeedButton,
                liveGridButton("Stats", v -> showLiveStatsDialog())
        ));
        controls.addView(liveControlRow(
                liveGridButton("Ratings", v -> showLiveRatingsDialog()),
                liveGridButton("Commentary", v -> showLiveCommentaryDialog())
        ));
        controls.addView(liveControlRow(
                liveGridButton("Overview", v -> showLiveOverviewDialog()),
                liveGridButton("← Exit", v -> confirmLeaveLiveMatch())
        ));
        ScrollView controlScroll=new ScrollView(this);
        controlScroll.addView(controls);
        side.addView(controlScroll,new LinearLayout.LayoutParams(-1,0,1));

        middle.addView(side, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.30f));
        root.addView(middle, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        applySafeInsets(root);
        safeEnableImmersiveNavigation();
        liveMatchActive = true;
        refreshLiveHeader();
        startLiveMatchTicker();
    }

    private class ClubShieldView extends View {
        private final int team;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path shield = new Path();
        ClubShieldView(int team) { super(MainActivity.this); this.team = team; }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w=getWidth(), h=getHeight();
            float left=w*.13f, right=w*.87f, top=h*.08f, mid=h*.67f, bottom=h*.94f, cx=w*.50f;
            shield.reset();
            shield.moveTo(left, top); shield.lineTo(right, top); shield.lineTo(right, mid);
            shield.quadTo(right, h*.82f, cx, bottom); shield.quadTo(left, h*.82f, left, mid); shield.close();
            c.save(); c.clipPath(shield);
            p.setColor(primaryColours[team]); c.drawRect(left, top, cx, bottom, p);
            p.setColor(secondaryColours[team]); c.drawRect(cx, top, right, bottom, p);
            p.setColor(Color.argb(34,255,255,255)); c.drawRect(left, top, right, h*.30f, p);
            c.restore();
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3)); p.setColor(Color.rgb(220,226,230)); c.drawPath(shield,p); p.setStyle(Paint.Style.FILL);
            p.setColor(contrastText(primaryColours[team])); p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(dp(10));
            c.drawText(shortNames[team], cx, h*.57f, p);
        }
    }

    private void showLiveSpeedDialog() {
        final String[] speeds = {"Slow", "Medium", "Fast"};
        final int[] values = {0, 1, 2};
        new BossDialog.Builder(this)
                .setTitle("Set match speed")
                .setItems(speeds, (dialog, which) -> {
                    setLiveSpeed(values[which]);
                    liveSimulationAccumulator = 0f;
                    if (liveSpeedButton != null) liveSpeedButton.setText("Speed • " + liveSpeedLabel());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLiveTacticsScreen() {
        stopLiveMatchTicker();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        safeEnableImmersiveNavigation();
        if (!liveTacticsSessionActive) {
            liveTacticsSessionActive = true;
            livePausedBeforeTactics = liveHalfTimeTacticsActive ? false : livePaused;
        }
        livePaused = true;
        backAction = () -> returnFromLiveTactics();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setPadding(dp(8), dp(8), dp(8), dp(8));
        root.setBackgroundColor(background);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(dp(8), dp(8), dp(8), dp(8));
        left.setBackground(rounded(panel));

        TextView head = makeText(liveHalfTimeTacticsActive ? "HALF-TIME • LIVE TACTICS" : "LIVE TACTICS", 17, accent);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        left.addView(head);
        TextView sub = makeText(liveHalfTimeTacticsActive
                ? "Half-time. Make substitutions and tactical changes, then return to start the second half."
                : "Adjust instructions, then tap a substitute and tap a player on the pitch to make a change.", 12, muted);
        sub.setPadding(0, dp(4), 0, dp(8));
        left.addView(sub);
        if(liveHalfTimeTacticsActive) {
            left.addView(makeText(clubNames[liveHome]+" "+liveHomeGoals+" – "+liveAwayGoals+" "+clubNames[liveAway],16,text));
            left.addView(makeText("Shots "+liveHomeShots+" – "+liveAwayShots+"  •  Possession "+liveHomePossession+"%",12,muted));
            left.addView(makeButton("Player condition & ratings",v->showLiveRatingsDialog()));
        }

        left.addView(makeLiveTacticsActionButton("Formation", liveFormation, v -> showLiveTacticsFormationDialog()));
        left.addView(makeLiveTacticsActionButton("Mentality", liveMentality, v -> showLiveMentalityDialog()));
        left.addView(makeLiveTacticsActionButton("Passing", passingInstruction, v -> {
            passingInstruction = nextPassing(passingInstruction);
            saveCurrentGame();
            refreshLiveHeader();
            showLiveTacticsScreen();
        }));
        left.addView(makeLiveTacticsActionButton("Tackling", tacklingInstruction, v -> {
            tacklingInstruction = nextTackling(tacklingInstruction);
            saveCurrentGame();
            refreshLiveHeader();
            showLiveTacticsScreen();
        }));
        left.addView(makeLiveTacticsToggleButton("Pressing", pressingInstruction, v -> {
            pressingInstruction = !pressingInstruction;
            saveCurrentGame();
            refreshLiveHeader();
            showLiveTacticsScreen();
        }));
        left.addView(makeLiveTacticsToggleButton("Offside Trap", offsideTrapInstruction, v -> {
            offsideTrapInstruction = !offsideTrapInstruction;
            saveCurrentGame();
            refreshLiveHeader();
            if (livePitchView != null) livePitchView.refreshFormationAnchors();
            showLiveTacticsScreen();
        }));
        left.addView(makeLiveTacticsToggleButton("Counter Attack", counterAttackInstruction, v -> {
            counterAttackInstruction = !counterAttackInstruction;
            saveCurrentGame();
            refreshLiveHeader();
            showLiveTacticsScreen();
        }));
        left.addView(makeLiveTacticsToggleButton("Men Behind Ball", menBehindBallInstruction, v -> {
            menBehindBallInstruction = !menBehindBallInstruction;
            saveCurrentGame();
            refreshLiveHeader();
            showLiveTacticsScreen();
        }));

        TextView subInfo = makeText(livePendingSubOnId >= 0
                ? "Selected sub: " + safeName(findPlayer(livePendingSubOnId)) + " • tap a player on the pitch"
                : "No substitute selected", 12, livePendingSubOnId >= 0 ? accent : muted);
        subInfo.setPadding(0, dp(8), 0, dp(8));
        left.addView(subInfo);

        Button clearSelected = makeButton("Clear selected sub", v -> {
            livePendingSubOnId = -1;
            showLiveTacticsScreen();
        });
        clearSelected.setBackground(buttonBackground(livePendingSubOnId >= 0 ? warning : panelLight));
        left.addView(clearSelected);

        Button backBtn = makeButton("← Return to Match", v -> returnFromLiveTactics());
        backBtn.setBackground(buttonBackground(panelLight));
        LinearLayout leftPanel=new LinearLayout(this);
        leftPanel.setOrientation(LinearLayout.VERTICAL);

        ScrollView instructionsScroll=new ScrollView(this);instructionsScroll.addView(left);
        leftPanel.addView(instructionsScroll,new LinearLayout.LayoutParams(-1,0,1));
        leftPanel.addView(backBtn,new LinearLayout.LayoutParams(-1,dp(48)));
        root.addView(leftPanel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.15f));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setPadding(dp(8), 0, 0, 0);

        TextView teamTitle = makeText(clubNames[selectedClub] + " • " + liveFormation, 18, text);
        teamTitle.setTypeface(Typeface.DEFAULT_BOLD);
        right.addView(teamTitle);
        TextView tip = makeText("Pitch view of the starting XI. Tap a bench player below, then tap a starter to swap.", 12, muted);
        tip.setPadding(0, dp(2), 0, dp(6));
        right.addView(tip);
        TextView roleLegend = makeText("● GK  GOLD    ● DEF  BLUE    ● MID  GREEN    ● ATT  RED    •    Condition shown on every player", 11, muted);
        roleLegend.setPadding(0, 0, 0, dp(5));
        right.addView(roleLegend);

        FrameLayout pitchFrame = new FrameLayout(this);
        pitchFrame.setBackground(rounded(panel));
        LinearLayout.LayoutParams pitchLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        pitchFrame.setTag("live-tactics-pitch");
        int widestLine=("3-5-2".equals(liveFormation)||"5-3-2".equals(liveFormation))?5:4;
        pitchFrame.setMinimumHeight(dp(widestLine*52+16));
        ScrollView pitchScroll=new ScrollView(this);
        pitchScroll.setFillViewport(true);
        pitchScroll.addView(pitchFrame,new ScrollView.LayoutParams(-1,-2));
        right.addView(pitchScroll, pitchLp);
        pitchFrame.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            if(r>l&&b>t&&(r-l!=or-ol||b-t!=ob-ot||pitchFrame.getChildCount()==0))
                pitchFrame.post(() -> { if(pitchFrame.isAttachedToWindow())populateLiveTacticsPitch(pitchFrame); });
        });

        TextView benchTitle = makeText("SUBSTITUTES • used " + liveSubsUsed + "/5", 13, accent);
        benchTitle.setTypeface(Typeface.DEFAULT_BOLD);
        benchTitle.setPadding(0, dp(8), 0, dp(4));
        right.addView(benchTitle);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout benchRow = new LinearLayout(this);
        benchRow.setOrientation(LinearLayout.HORIZONTAL);
        benchRow.setPadding(0, 0, dp(4), 0);
        for (int id : liveBenchIds) {
            Player p = findPlayer(id);
            if (p == null) continue;
            Button b = new Button(this);
            b.setAllCaps(false);
            int benchColour = livePlayerPositionColour(p);
            boolean reversible = liveUsedOffIds.contains(id);
            b.setText(shortPlayerName(p) + "\n" + p.position + " • OVR " + p.overall
                    + "\nCond " + p.fitness + "%" + (reversible ? " • REVERSIBLE" : ""));
            b.setTextSize(10);
            b.setMinHeight(0);
            b.setMinimumHeight(0);
            b.setPadding(dp(8), dp(7), dp(8), dp(7));
            b.setTextColor(idealButtonTextColour(benchColour));
            b.setBackground(liveBenchBackground(benchColour, id == livePendingSubOnId));
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(142), LinearLayout.LayoutParams.WRAP_CONTENT);
            blp.setMargins(0, 0, dp(6), 0);
            b.setLayoutParams(blp);
            b.setOnClickListener(v -> {
                if (substitutions.hasDeparted(id)) {
                    Toast.makeText(this, "This player has already left the match.", Toast.LENGTH_SHORT).show();
                    return;
                }
                livePendingSubOnId = id;
                showLiveTacticsScreen();
            });
            benchRow.addView(b);
        }
        if (benchRow.getChildCount() == 0) {
            TextView none = makeText("No substitutes available", 13, muted);
            none.setPadding(dp(8), dp(8), dp(8), dp(8));
            benchRow.addView(none);
        }
        scroll.addView(benchRow);
        right.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(right, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2.35f));
        setContentView(root);
        applySafeInsets(root);
    }

    private void returnFromLiveTactics() {
        liveTacticsSessionActive = false;
        if (liveHalfTimeTacticsActive) {
            liveHalfTimeTacticsActive = false;
            liveHalfTimePending = false;
            liveHalfTimeBreakTaken = true;
            livePaused = false;
            livePossessionTeam = liveAway;
            liveCarrierSlot = 9;
            livePendingPossessionTeam = liveAway;
            livePendingCarrierSlot = 9;
            liveNeedsKickoffReset = true;
            liveActionTimer = 0.45f;
            playLiveSound("whistle");
            recordCommentary(-1, "The second half is ready to begin.");
        } else {
            livePaused = livePausedBeforeTactics;
        }
        livePendingSubOnId = -1;
        showLiveMatch();
    }

    private View makeLiveTacticsActionButton(String label, String value, View.OnClickListener click) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label + "\n" + value);
        b.setTextSize(11);
        b.setTextColor(idealButtonTextColour(panelLight));
        b.setBackground(buttonBackground(panelLight));
        b.setPadding(dp(8), dp(8), dp(8), dp(8));
        b.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        b.setLayoutParams(lp);
        return b;
    }

    private View makeLiveTacticsToggleButton(String label, boolean enabled, View.OnClickListener click) {
        return makeLiveTacticsActionButton(label, enabled ? "ON" : "OFF", click);
    }

    private void showLiveTacticsFormationDialog() {
        final String[] options = {"4-3-3", "4-2-3-1", "4-4-2", "3-5-2", "5-3-2"};
        new BossDialog.Builder(this)
                .setTitle("Formation")
                .setSingleChoiceItems(options, Arrays.asList(options).indexOf(liveFormation), (dialog, which) -> {
                    liveFormation = options[which];
                    tacticFormation = liveFormation;
                    saveCurrentGame();
                    if (livePitchView != null) livePitchView.refreshFormationAnchors();
                    dialog.dismiss();
                    showLiveTacticsScreen();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLiveMentalityDialog() {
        final String[] options = {"Defensive", "Balanced", "Attacking"};
        int checked = liveMentality.equals("Defensive") ? 0 : liveMentality.equals("Attacking") ? 2 : 1;
        new BossDialog.Builder(this)
                .setTitle("Mentality")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    liveMentality = options[which];
                    saveCurrentGame();
                    dialog.dismiss();
                    showLiveTacticsScreen();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void populateLiveTacticsPitch(FrameLayout frame) {
        frame.removeAllViews();
        View bg = new View(this) {
            private final PitchArt art=new PitchArt();
            @Override protected void onDraw(Canvas c){art.draw(c,getWidth(),getHeight());}
        };
        // Explicit dimensions avoid a MATCH_PARENT child measuring to zero in a
        // wrap-content FrameLayout inside the tactics ScrollView.
        frame.addView(bg, new FrameLayout.LayoutParams(frame.getWidth(), frame.getHeight()));

        ArrayList<Integer> lineup = selectedClub == liveHome ? liveHomeLineupIds : liveAwayLineupIds;
        boolean homeSide = selectedClub == liveHome;
        float[] xs = new float[11];
        float[] ys = new float[11];
        fillMiniFormation(xs, ys, liveFormation, homeSide);
        String[] slots = formationSlots();

        int fw = frame.getWidth();
        int fh = frame.getHeight();
        int tokenSize=dp(48);
        int tokenWidth=Math.max(dp(48),Math.min(dp(72),(fw-dp(32))/5));
        int padX = tokenWidth/2+dp(10);
        int padY = tokenSize/2+dp(4);
        int innerW = Math.max(dp(100), fw - padX * 2);
        int innerH = Math.max(1, fh - padY * 2);

        for (int i = 0; i < Math.min(11, lineup.size()); i++) {
            Player p = findPlayer(lineup.get(i));
            if (p == null) continue;
            String liveRole = slots[Math.min(i, slots.length - 1)];
            int roleColour = liveRoleColour(liveRole);
            Button chip = new Button(this);
            chip.setAllCaps(false);
            chip.setGravity(Gravity.CENTER);
            chip.setText(liveRole + " · " + p.fitness + "%\n" + shortPlayerName(p));
            chip.setTextSize(10f);
            chip.setTextColor(text);
            chip.setBackground(livePlayerTokenBackground(roleColour));
            chip.setPadding(dp(3), dp(3), dp(3), dp(3));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(tokenWidth, tokenSize);
            chip.setLayoutParams(lp);
            final int offId = p.id;
            chip.setOnClickListener(v -> {
                if (livePendingSubOnId < 0) {
                    Toast.makeText(this, "Select a substitute from the bench first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!substitutions.canChange(offId,livePendingSubOnId)) {
                    Toast.makeText(this, "All 5 substitutions have been used.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Player on = findPlayer(livePendingSubOnId);
                new BossDialog.Builder(this)
                        .setTitle("Make substitution?")
                        .setMessage((on == null ? "Selected player" : on.name) + " IN for " + p.name + "?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            performLiveSubstitution(offId, livePendingSubOnId);
                            livePendingSubOnId = -1;
                            showLiveTacticsScreen();
                        })
                        .show();
            });
            frame.addView(chip);
            int x = padX + (int)(xs[i] * innerW) - tokenWidth/2;
            int y = padY + (int)(ys[i] * innerH) - tokenSize/2;
            chip.setX(x);
            chip.setY(y);
        }
    }


    private int liveRoleColour(String role) {
        if (role == null) return Color.rgb(45, 165, 95);
        String r = role.toUpperCase(Locale.UK);
        if (r.startsWith("GK")) return Color.rgb(236, 181, 40);
        if (r.equals("DL") || r.equals("DC") || r.equals("DR") || r.equals("RB") || r.equals("LB")
                || r.equals("CB") || r.equals("LWB") || r.equals("RWB")) {
            return Color.rgb(45, 105, 210);
        }
        if (r.equals("FC") || r.equals("ST") || r.equals("RW") || r.equals("LW")) {
            return Color.rgb(205, 62, 67);
        }
        return Color.rgb(44, 158, 92);
    }

    private int livePlayerPositionColour(Player p) {
        if (p == null || p.position == null) return Color.rgb(44, 158, 92);
        String pos = p.position.toUpperCase(Locale.UK);
        if ("GK".equals(pos)) return Color.rgb(236, 181, 40);
        if ("RB".equals(pos) || "LB".equals(pos) || "CB".equals(pos)) {
            return Color.rgb(45, 105, 210);
        }
        if ("ST".equals(pos) || "RW".equals(pos) || "LW".equals(pos) || "FC".equals(pos)) {
            return Color.rgb(205, 62, 67);
        }
        return Color.rgb(44, 158, 92);
    }

    private GradientDrawable livePlayerTokenBackground(int colour) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(9));
        d.setColor(panelLight);
        d.setStroke(dp(2), colour);
        return d;
    }

    private GradientDrawable liveBenchBackground(int colour, boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(10));
        d.setColor(colour);
        d.setStroke(dp(selected ? 3 : 1), selected ? Color.WHITE : Color.argb(120, 255, 255, 255));
        return d;
    }

    private boolean isLiveSubstitutionReversal(int offId,int onId) {
        return substitutions.isReturn(onId) && substitutions.canChange(offId,onId);
    }

    private void fillMiniFormation(float[] xs, float[] ys, String formation, boolean home) {
        int[] lines;
        if ("4-2-3-1".equals(formation)) lines = new int[]{4, 2, 3, 1};
        else if ("4-4-2".equals(formation)) lines = new int[]{4, 4, 2};
        else if ("3-5-2".equals(formation)) lines = new int[]{3, 5, 2};
        else if ("5-3-2".equals(formation)) lines = new int[]{5, 3, 2};
        else lines = new int[]{4, 3, 3};
        xs[0] = home ? 0f : 1f;
        ys[0] = 0.50f;
        int slot = 1;
        float[] lineX = lines.length == 4 ? new float[]{0.25f, 0.50f, 0.75f, 1f} : new float[]{0.30f, 0.65f, 1f};
        for (int li = 0; li < lines.length; li++) {
            int count = lines[li];
            float x = lineX[Math.min(li, lineX.length - 1)];
            if (!home) x = 1f - x;
            for (int j = 0; j < count && slot < 11; j++) {
                xs[slot] = x;
                ys[slot] = count == 1 ? .5f : j / (float)(count - 1);
                slot++;
            }
        }
        while (slot < 11) {
            xs[slot] = home ? 0.76f : 0.24f;
            ys[slot] = (slot - 8f) / 4f;
            slot++;
        }
    }


    private void performLiveSubstitution(int offId, int onId) {
        Player off = findPlayer(offId);
        Player on = findPlayer(onId);
        if (on == null) return;

        boolean reversal = isLiveSubstitutionReversal(offId, onId);
        if(!liveBenchIds.contains(onId) || on.injuredWeeks>0 || !substitutions.change(offId,onId)) {
            Toast.makeText(this,"Change unavailable: check eligibility and substitutions remaining.",Toast.LENGTH_SHORT).show(); return;
        }

        liveXIIds.remove(Integer.valueOf(offId));
        liveXIIds.add(on.id);
        liveBenchIds.remove(Integer.valueOf(on.id));
        liveBenchIds.add(offId);

        substitutionsPending=true;
        liveSubsUsed=substitutions.used();
        liveUsedOffIds.clear();
        for(int id:liveBenchIds) if(substitutions.isReturn(id)) liveUsedOffIds.add(id);

        syncSelectedLineupToPitch(offId, on.id);
        playerRoleStatus[offId] = 1;
        playerSelectedSlot[offId] = -1;
        playerSelectedPosition[offId] = off != null ? off.position : "SUB";
        playerRoleStatus[onId] = 2;
        ArrayList<Integer> ids = selectedClub == liveHome ? liveHomeLineupIds : liveAwayLineupIds;
        int slotIndex = ids.indexOf(onId);
        if (slotIndex >= 0 && slotIndex < formationSlots().length) {
            playerSelectedSlot[onId] = slotIndex;
            playerSelectedPosition[onId] = formationSlots()[slotIndex];
        }

        saveCurrentGame();
        if (reversal) {
            recordCommentary(selectedClub,
                    "SUB REVERSED: " + on.name + " returns for " + (off == null ? "Player" : off.name)
                            + ". The change no longer counts against the substitution total.");
            Toast.makeText(this, "Substitution reversed • counter restored.", Toast.LENGTH_SHORT).show();
        } else {
            recordCommentary(selectedClub,
                    "SUB: " + (off == null ? "Player" : off.name) + " OFF, " + on.name + " ON for " + clubNames[selectedClub] + ".");
            Toast.makeText(this, "Substitution made.", Toast.LENGTH_SHORT).show();
        }
        refreshLiveHeader();
    }

    private String shortPlayerName(Player p) {
        if (p == null || p.name == null || p.name.trim().isEmpty()) return "Player";
        String[] bits = p.name.trim().split(" ");
        return bits[bits.length - 1];
    }

    private String safeName(Player p) {
        return p == null ? "No player" : p.name;
    }

    private View liveSideButton(String label, View.OnClickListener click) {
        Button b = compactButton(label, click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        lp.setMargins(0, dp(2), 0, dp(2));
        b.setLayoutParams(lp);
        b.setTextSize(11);
        b.setPadding(dp(4), dp(2), dp(4), dp(2));
        return b;
    }

    private View liveGridButton(String label, View.OnClickListener click) {
        Button b = compactButton(label, click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        b.setLayoutParams(lp);
        b.setTextSize(13);
        b.setMinHeight(dp(48));
        b.setMinimumHeight(dp(48));
        b.setPadding(dp(6), dp(5), dp(6), dp(5));
        return b;
    }

    private LinearLayout liveControlRow(View left, View right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));
        row.addView(left);
        row.addView(right);
        return row;
    }

    private int contrastText(int colour) {
        int luminance = (Color.red(colour) * 299 + Color.green(colour) * 587 + Color.blue(colour) * 114) / 1000;
        return luminance > 165 ? Color.BLACK : Color.WHITE;
    }

    private LinearLayout.LayoutParams compactParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1f);
        p.setMargins(dp(2), dp(2), dp(2), dp(6));
        return p;
    }

    private Button compactButton(String label, View.OnClickListener click) {
        Button b = makeButton(label, click);
        b.setTextSize(13);
        b.setPadding(dp(4), dp(2), dp(4), dp(2));
        return b;
    }

    private String liveSpeedLabel() { return liveSpeed==0?"Slow":liveSpeed==1?"Medium":"Fast"; }

    private void setLiveSpeed(int speed) {
        liveSpeed = Math.max(0,Math.min(2,speed));
        liveSimulationAccumulator = 0f;
        refreshLiveHeader();
    }

    private void startLiveMatchTicker() {
        liveLastFrameNanos = 0L;
        liveSimulationAccumulator = 0f;
        matchFrameCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (!liveMatchActive) return;

                if (liveLastFrameNanos == 0L) liveLastFrameNanos = frameTimeNanos;
                float realDt = Math.min(0.040f, Math.max(0f, (frameTimeNanos - liveLastFrameNanos) / 1_000_000_000f));
                liveLastFrameNanos = frameTimeNanos;
                if(audio!=null) audio.pressure("FINAL_THIRD".equals(livePhase)? .75f : "TRANSITION".equals(livePhase)? .55f : .2f, liveMinute>=80);

                if (!livePaused) {
                    // Decouple presentation FPS from simulation speed. At high match speeds we run
                    // many tiny 120 Hz simulation steps instead of one large jump per screen frame.
                    liveSimulationAccumulator += realDt * MatchMath.playbackRate(liveSpeed);
                    int subSteps = 0;
                    final int maxSubSteps = 48;
                    while (liveSimulationAccumulator >= LIVE_FIXED_STEP && subSteps < maxSubSteps && liveMatchActive && !liveHalfTimePending) {
                        advanceLiveSimulation(LIVE_FIXED_STEP);
                        liveSimulationAccumulator -= LIVE_FIXED_STEP;
                        subSteps++;
                    }
                    // Do not allow a rare long Android frame to create a visible catch-up burst.
                    if (subSteps >= maxSubSteps) liveSimulationAccumulator = Math.min(liveSimulationAccumulator, LIVE_FIXED_STEP * 2f);

                    if (livePitchView != null) livePitchView.postInvalidateOnAnimation();

                    liveFrameCounter++;
                    if (liveFrameCounter % 8 == 0 || liveMinuteFloat >= 90f) refreshLiveHeader();

                    if (liveHalfTimePending && !liveHalfTimeBreakTaken) {
                        livePaused = true;
                        liveHalfTimeTacticsActive = true;
                        showLiveTacticsScreen();
                        return;
                    }

                    if (liveMinuteFloat >= 90f) {
                        finishLiveMatch();
                        return;
                    }
                }

                Choreographer.getInstance().postFrameCallback(this);
            }
        };
        Choreographer.getInstance().postFrameCallback(matchFrameCallback);
    }

    private void advanceLiveSimulation(float dt) {
        if(substitutionsPending) {
            substitutions.commit();substitutionsPending=false;
            liveUsedOffIds.clear();matchParticipants.addAll(liveXIIds);
        }
        float nextMinute = liveMinuteFloat + dt;
        if (!liveHalfTimeBreakTaken && nextMinute >= 45f) nextMinute = 45f;
        liveMinuteFloat = Math.min(90f, nextMinute);
        int newMinute = (int) liveMinuteFloat;
        while (liveMinute < newMinute && liveMinute < 90) {
            liveMinute++;
            processLiveMinute(liveMinute);
        }

        if (livePossessionTeam == liveHome) liveHomePossessionUnits += dt;
        else if (livePossessionTeam == liveAway) liveAwayPossessionUnits += dt;
        float possTotal = liveHomePossessionUnits + liveAwayPossessionUnits;
        if (possTotal > 0.01f) {
            liveHomePossession = Math.round(100f * liveHomePossessionUnits / possTotal);
            liveAwayPossession = 100 - liveHomePossession;
        }

        if(pendingGoalTeam>=0) {
            pendingGoalDelay-=dt;
            if(pendingGoalDelay<=0) completePendingGoal();
        }
        if(pendingContactCue!=null){pendingContactDelay-=dt;if(pendingContactDelay<=0){
            playLiveSound(pendingContactCue);
            if("catch".equals(pendingContactCue))playLiveSound("save");
            pendingContactCue=null;
        }}
        liveActionTimer -= dt;
        if (liveActionTimer <= 0f && liveMinuteFloat < 90f) beginNextLiveAction();
        if (livePitchView != null) livePitchView.stepSimulation(dt);
    }

    private void stopLiveMatchTicker() {
        liveMatchActive = false;
        liveLastFrameNanos = 0L;
        liveSimulationAccumulator = 0f;
        if (matchFrameCallback != null) {
            Choreographer.getInstance().removeFrameCallback(matchFrameCallback);
            matchFrameCallback = null;
        }
    }

    private void processLiveMinute(int minute) {
        if (minute == 45 && !liveHalfTimeBreakTaken) {
            livePendingPossessionTeam = liveAway;
            livePendingCarrierSlot = 9;
            liveNeedsKickoffReset = true;
            liveHalfTimePending = true;
            playLiveSound("whistle");
            recordCommentary(-1, "HALF-TIME. Make your changes before the second half.");
            liveActionTimer = Math.max(liveActionTimer, 0.90f);
        }

        if (minute % 5 == 0) {
            for(int team:new int[]{liveHome,liveAway}) for(int id:team==liveHome?liveHomeLineupIds:liveAwayLineupIds) {
                Player p=findPlayer(id);
                if(p!=null) p.fitness=Math.max(35,p.fitness-(p.physical>=80?1:2)-(team==selectedClub&&pressingInstruction?1:0));
            }
        }
    }

    private void beginNextLiveAction() {
        if (livePitchView == null || liveMinute >= 90) return;
        int review=liveMinute/10;
        if(liveMinute>=45 && review!=lastAiReview) {lastAiReview=review;manageOpposition(liveMinute);}

        if (liveLooseBall) {
            resolveLooseBall();
            return;
        }
        if (livePendingSetPiece != null && !livePendingSetPiece.isEmpty()) {
            performPendingSetPiece();
            return;
        }

        if (livePendingPossessionTeam >= 0) {
            livePossessionTeam = livePendingPossessionTeam;
            liveCarrierSlot = Math.max(0, Math.min(10, livePendingCarrierSlot));
            livePendingPossessionTeam = -1;
            livePendingCarrierSlot = -1;
        }

        if (liveLastPossessionTeam >= 0 && liveLastPossessionTeam != livePossessionTeam) {
            liveTransitionActions = 2;
            lastPasserId=-1;lastPassTeam=-1;
            liveSequencePasses = 0;
        }
        liveLastPossessionTeam = livePossessionTeam;

        if (liveNeedsKickoffReset) {
            livePitchView.resetForKickoff(livePossessionTeam);
            liveCarrierSlot = 9;
            liveNeedsKickoffReset = false;
            livePhase = "BUILD_UP";
            liveActionDuration = 0.72f;
            liveActionTimer = liveActionDuration;
            recordCommentary(livePossessionTeam, clubNames[livePossessionTeam] + " restart from the centre spot and look to build patiently.");
            return;
        }

        Player carrier = playerForPitchSlot(livePossessionTeam, liveCarrierSlot);
        if (carrier == null) {
            liveCarrierSlot = 9;
            carrier = playerForPitchSlot(livePossessionTeam, liveCarrierSlot);
            if (carrier == null) return;
        }

        float ballX = livePitchView.getBallX();
        float progress = livePossessionTeam == liveHome ? ballX : 1f - ballX;
        livePhase = calculateLivePhase(livePossessionTeam, progress);
        boolean transition = "TRANSITION".equals(livePhase);
        boolean isKeeper = liveCarrierSlot == 0 || "GK".equals(carrier.position);
        boolean wideCarrier = livePitchView.getPlayerY(livePossessionTeam, liveCarrierSlot) < 0.24f
                || livePitchView.getPlayerY(livePossessionTeam, liveCarrierSlot) > 0.76f;

        if (isKeeper) {
            performPassAction(livePossessionTeam, liveCarrierSlot, shouldPlayLongPass(livePossessionTeam) && random.nextDouble() < 0.45);
            if (liveTransitionActions > 0) liveTransitionActions--;
            return;
        }

        double shotChance = 0.0;
        if (progress > 0.67f) shotChance = 0.13 + Math.max(0, carrier.finishing - 62) / 245.0;
        if (progress > 0.82f) shotChance += 0.10;
        if (carrier.longShots && progress > 0.54f) shotChance += 0.035;
        if (livePossessionTeam == selectedClub && "Attacking".equals(liveMentality)) shotChance += 0.025;
        if (transition && progress > 0.56f) shotChance += 0.045;
        shotChance = Math.min(0.39, shotChance);

        double r = random.nextDouble();
        if ("BUILD_UP".equals(livePhase)) {
            if (r < 0.76) performPassAction(livePossessionTeam, liveCarrierSlot, r < 0.14 && shouldPlayLongPass(livePossessionTeam));
            else if (r < 0.90) performDribbleAction(livePossessionTeam, liveCarrierSlot);
            else performDuelAction(livePossessionTeam, liveCarrierSlot);
        } else if ("MIDFIELD".equals(livePhase)) {
            if (r < 0.58) performPassAction(livePossessionTeam, liveCarrierSlot, shouldPlayLongPass(livePossessionTeam) && random.nextDouble() < 0.28);
            else if (r < 0.76) performDribbleAction(livePossessionTeam, liveCarrierSlot);
            else if (wideCarrier && r < 0.86) performCrossAction(livePossessionTeam, liveCarrierSlot);
            else performDuelAction(livePossessionTeam, liveCarrierSlot);
        } else {
            if (r < shotChance) performShotAction(livePossessionTeam, liveCarrierSlot);
            else if (wideCarrier && r < shotChance + 0.19) performCrossAction(livePossessionTeam, liveCarrierSlot);
            else if (r < shotChance + 0.56) performPassAction(livePossessionTeam, liveCarrierSlot, shouldPlayLongPass(livePossessionTeam) && random.nextDouble() < 0.20);
            else if (r < shotChance + 0.76) performDribbleAction(livePossessionTeam, liveCarrierSlot);
            else performDuelAction(livePossessionTeam, liveCarrierSlot);
        }

        if (liveTransitionActions > 0) liveTransitionActions--;
    }

    private String calculateLivePhase(int team, float progress) {
        if (liveTransitionActions > 0) return "TRANSITION";
        if (progress < 0.34f) return "BUILD_UP";
        if (progress < 0.68f) return "MIDFIELD";
        return "FINAL_THIRD";
    }

    private void scheduleSetPiece(String type, int team, float x, float y) {
        livePendingSetPiece = type;
        liveSetPieceTeam = team;
        liveSetPieceX = Math.max(0.015f, Math.min(0.985f, x));
        liveSetPieceY = Math.max(0.015f, Math.min(0.985f, y));
    }

    private void performPendingSetPiece() {
        String type = livePendingSetPiece;
        int team = liveSetPieceTeam;
        livePendingSetPiece = "";
        liveSetPieceTeam = -1;
        if (team < 0) return;
        livePossessionTeam = team;
        liveLastPossessionTeam = team;

        if ("CORNER".equals(type)) {
            int takerSlot = liveSetPieceY < 0.5f ? 8 : 10;
            if (playerForPitchSlot(team, takerSlot) == null) takerSlot = wideAttackSlot(team);
            int targetSlot = bestBoxTarget(team, takerSlot);
            Player taker = playerForPitchSlot(team, takerSlot);
            Player target = playerForPitchSlot(team, targetSlot);
            livePitchView.placePlayer(team, takerSlot, liveSetPieceX, liveSetPieceY);
            livePitchView.placeBall(liveSetPieceX, liveSetPieceY);
            livePitchView.prepareReceivingRun(team, targetSlot, true);
            liveActionDuration = 1.05f;
            liveActionTimer = liveActionDuration;
            livePitchView.animateBallToSlot(team, takerSlot, team, targetSlot, liveActionDuration);
            livePendingPossessionTeam = team;
            livePendingCarrierSlot = targetSlot;
            recordCommentary(team, (taker == null ? clubNames[team] : taker.name) + " whips the corner into the danger area"
                    + (target == null ? "." : " towards " + target.name + "."));
            return;
        }

        if ("THROW_IN".equals(type)) {
            int throwerSlot = nearestDefenderSlot(team, liveSetPieceX, liveSetPieceY);
            if (throwerSlot <= 0) throwerSlot = 2;
            int targetSlot = choosePassTarget(team, throwerSlot, false);
            Player thrower = playerForPitchSlot(team, throwerSlot);
            Player target = playerForPitchSlot(team, targetSlot);
            livePitchView.placePlayer(team, throwerSlot, liveSetPieceX, liveSetPieceY);
            livePitchView.placeBall(liveSetPieceX, liveSetPieceY);
            livePitchView.prepareReceivingRun(team, targetSlot, false);
            liveActionDuration = 0.78f;
            liveActionTimer = liveActionDuration;
            livePitchView.animateBallToSlot(team, throwerSlot, team, targetSlot, liveActionDuration);
            livePendingPossessionTeam = team;
            livePendingCarrierSlot = targetSlot;
            recordCommentary(team, (thrower == null ? clubNames[team] : thrower.name) + " takes the throw quickly"
                    + (target == null ? "." : " into " + target.name + "'s feet."));
            return;
        }

        if ("GOAL_KICK".equals(type)) {
            liveCarrierSlot = 0;
            livePitchView.placeBall(team == liveHome ? 0.08f : 0.92f, 0.50f);
            Player keeper = playerForPitchSlot(team, 0);
            recordCommentary(team, (keeper == null ? "The goalkeeper" : keeper.name) + " sets the ball for the goal kick.");
            performPassAction(team, 0, shouldPlayLongPass(team));
        }
    }

    private void resolveLooseBall() {
        int homeSlot = nearestDefenderSlot(liveHome, liveLooseBallX, liveLooseBallY);
        int awaySlot = nearestDefenderSlot(liveAway, liveLooseBallX, liveLooseBallY);
        Player hp = playerForPitchSlot(liveHome, homeSlot);
        Player ap = playerForPitchSlot(liveAway, awaySlot);
        double homeScore = hp == null ? 50 : hp.pace * 0.35 + hp.physical * 0.35 + hp.overall * 0.30;
        double awayScore = ap == null ? 50 : ap.pace * 0.35 + ap.physical * 0.35 + ap.overall * 0.30;
        double homeChance = homeScore / Math.max(1.0, homeScore + awayScore);
        int winner = random.nextDouble() < homeChance ? liveHome : liveAway;
        int slot = winner == liveHome ? homeSlot : awaySlot;
        Player p = playerForPitchSlot(winner, slot);
        liveLooseBall = false;
        livePitchView.animateBallToSlot(winner, slot, winner, slot, 0.42f);
        livePendingPossessionTeam = winner;
        livePendingCarrierSlot = slot;
        liveActionDuration = 0.46f;
        liveActionTimer = liveActionDuration;
        recordCommentary(winner, (p == null ? clubNames[winner] : p.name) + " reacts first to the loose ball.");
    }

    private boolean isOffsidePass(int team, int targetSlot) {
        int defending = team == liveHome ? liveAway : liveHome;
        float targetX = livePitchView.getPlayerX(team, targetSlot);
        float ballX = livePitchView.getBallX();
        float[] line = new float[5];
        for (int i = 0; i < 5; i++) line[i] = livePitchView.getPlayerX(defending, i);
        Arrays.sort(line);
        if (team == liveHome) {
            float secondLast = line[3];
            if (defending == selectedClub && offsideTrapInstruction) secondLast -= 0.015f;
            return targetX > 0.5f && targetX > ballX + 0.012f && targetX > secondLast + 0.008f;
        } else {
            float secondLast = line[1];
            if (defending == selectedClub && offsideTrapInstruction) secondLast += 0.015f;
            return targetX < 0.5f && targetX < ballX - 0.012f && targetX < secondLast - 0.008f;
        }
    }

    private boolean shouldPlayLongPass(int team) {
        if (team != selectedClub) return random.nextDouble() < 0.22;
        if ("Long".equals(passingInstruction)) return true;
        if ("Direct".equals(passingInstruction)) return random.nextDouble() < 0.72;
        if ("Short".equals(passingInstruction)) return random.nextDouble() < 0.08;
        return random.nextDouble() < 0.28;
    }

    private void performPassAction(int team, int fromSlot, boolean longPass) {
        if (liveSpeed <= 3) playLiveSound("kick");
        int targetSlot = choosePassTarget(team, fromSlot, longPass);
        Player passer = playerForPitchSlot(team, fromSlot);
        Player target = playerForPitchSlot(team, targetSlot);
        if (passer == null || target == null) return;

        if (team == liveHome) liveHomePasses++; else liveAwayPasses++;
        livePitchView.prepareReceivingRun(team, targetSlot, longPass);

        boolean forwardBall = team == liveHome
                ? livePitchView.getPlayerX(team, targetSlot) > livePitchView.getPlayerX(team, fromSlot) + 0.02f
                : livePitchView.getPlayerX(team, targetSlot) < livePitchView.getPlayerX(team, fromSlot) - 0.02f;
        if (forwardBall && isOffsidePass(team, targetSlot) && random.nextDouble() < 0.90) {
            int defending = team == liveHome ? liveAway : liveHome;
            liveActionDuration = longPass ? 0.86f : 0.68f;
            liveActionTimer = liveActionDuration;
            livePitchView.animateBallToSlot(team, fromSlot, team, targetSlot, liveActionDuration);
            livePitchView.flashOffsideLine(team == liveHome
                    ? Math.max(livePitchView.getBallX(), livePitchView.getPlayerX(defending, 1))
                    : Math.min(livePitchView.getBallX(), livePitchView.getPlayerX(defending, 1)));
            livePendingPossessionTeam = defending;
            livePendingCarrierSlot = Math.min(4, Math.max(1, nearestDefenderSlot(defending, livePitchView.getPlayerX(team, targetSlot), livePitchView.getPlayerY(team, targetSlot))));
            liveSequencePasses = 0;
            recordCommentary(defending, "The flag is up! " + target.name + " went too early and is caught offside.");
            return;
        }

        double quality = (passer.passing + passer.technique) / 2.0;
        double success = 0.72 + (quality - 65) / 210.0;
        if (longPass) success -= 0.10;
        success -= Math.max(0,80-passer.fitness)*.0014;
        int rival=team==liveHome?liveAway:liveHome;
        Player marker=playerForPitchSlot(rival,nearestDefenderSlot(rival,livePitchView.getPlayerX(team,targetSlot),livePitchView.getPlayerY(team,targetSlot)));
        if(marker!=null) success-=(marker.defending-65)*.001;
        if ("TRANSITION".equals(livePhase) && longPass) success -= 0.025;
        if (team == selectedClub && "Short".equals(passingInstruction) && !longPass) success += 0.05;
        if (team == selectedClub && "Long".equals(passingInstruction) && longPass) success += 0.035;
        int defending = team == liveHome ? liveAway : liveHome;
        if (defending == selectedClub && pressingInstruction) success -= 0.045;
        success = Math.max(0.52, Math.min(0.94, success));

        liveActionDuration = longPass ? 0.96f : 0.66f + random.nextFloat() * 0.18f;
        liveActionTimer = liveActionDuration;

        if (random.nextDouble() < success) {
            if (team == liveHome) liveHomePassesComplete++; else liveAwayPassesComplete++;
            matchCompletedPasses.put(passer.id,matchCompletedPasses.getOrDefault(passer.id,0)+1);
            livePitchView.animateBallToSlot(team, fromSlot, team, targetSlot, liveActionDuration);
            livePendingPossessionTeam = team;
            livePendingCarrierSlot = targetSlot;
            liveSequencePasses++;
            lastPasserId=passer.id;lastPassTeam=team;
            String line;
            if ("TRANSITION".equals(livePhase) && forwardBall) {
                line = passer.name + " releases " + target.name + " quickly into the space on the break.";
            } else if (longPass) {
                line = passer.name + " looks up and sends a diagonal towards " + target.name + ".";
            } else if ("BUILD_UP".equals(livePhase) && liveSequencePasses >= 3) {
                line = clubNames[team] + " keep their shape as " + passer.name + " moves it on to " + target.name + ".";
            } else {
                int pick = random.nextInt(6);
                if (pick == 0) line = passer.name + " plays it into " + target.name + "'s feet.";
                else if (pick == 1) line = passer.name + " slides the ball into the path of " + target.name + ".";
                else if (pick == 2) line = target.name + " shows for it and " + passer.name + " finds him.";
                else if (pick == 3) line = passer.name + " switches the angle of attack through " + target.name + ".";
                else if (pick == 4) line = passer.name + " gives it first time to " + target.name + ".";
                else line = passer.name + " waits for the run and feeds " + target.name + ".";
            }
            recordCommentary(team, line);
        } else {
            matchMissedPasses.put(passer.id,matchMissedPasses.getOrDefault(passer.id,0)+1);
            float targetY = livePitchView.getPlayerY(team, targetSlot);
            if ((targetY < 0.14f || targetY > 0.86f) && random.nextDouble() < 0.28) {
                float outY = targetY < 0.5f ? 0.012f : 0.988f;
                float outX = livePitchView.getPlayerX(team, targetSlot);
                livePitchView.animateBallToPoint(team, fromSlot, outX, outY, liveActionDuration);
                scheduleSetPiece("THROW_IN", defending, outX, outY);
                liveSequencePasses = 0;
                recordCommentary(defending, passer.name + " overhits the pass and it runs out for a throw to " + clubNames[defending] + ".");
            } else {
                int interceptorSlot = nearestDefenderSlot(defending, livePitchView.getPlayerX(team, targetSlot), livePitchView.getPlayerY(team, targetSlot));
                Player interceptor = playerForPitchSlot(defending, interceptorSlot);
                livePitchView.animateBallToSlot(team, fromSlot, defending, interceptorSlot, liveActionDuration);
                livePendingPossessionTeam = defending;
                livePendingCarrierSlot = interceptorSlot;
                liveSequencePasses = 0;
                recordCommentary(defending, (interceptor == null ? clubNames[defending] : interceptor.name)
                        + " tracks the runner and steps across to intercept.");
            }
        }
    }

    private int choosePassTarget(int team,int fromSlot,boolean longPass) {
        float x=livePitchView.getPlayerX(team,fromSlot),y=livePitchView.getPlayerY(team,fromSlot);
        int other=team==liveHome?liveAway:liveHome,best=fromSlot==0?4:0;
        float bestScore=-Float.MAX_VALUE,dir=team==liveHome?1:-1;
        Player passer=playerForPitchSlot(team,fromSlot);
        float vision=passer==null?.6f:passer.passing/100f;
        for(int i=0;i<11;i++) if(i!=fromSlot) {
            float tx=livePitchView.getPlayerX(team,i),ty=livePitchView.getPlayerY(team,i);
            float dx=tx-x,dy=ty-y,dist=(float)Math.hypot(dx,dy),space=1,blocked=0;
            for(int j=1;j<11;j++) {
                float ox=livePitchView.getPlayerX(other,j),oy=livePitchView.getPlayerY(other,j);
                space=Math.min(space,(float)Math.hypot(tx-ox,ty-oy));
                float u=MatchMath.clamp(((ox-x)*dx+(oy-y)*dy)/Math.max(.001f,dist*dist),0,1);
                if(u>.08f && u<.94f && Math.hypot(ox-x-u*dx,oy-y-u*dy)<.045)blocked+=1;
            }
            float ideal=longPass?.32f:.15f;
            float score=space*2.8f-Math.abs(dist-ideal)*1.8f-blocked*vision*.45f;
            score+=dx*dir*(.9f+teamIntent(team)*.5f);
            score+=random.nextFloat()*(.25f+(1-vision)*.4f);
            if(i==0)score-=.16f;
            if(score>bestScore){bestScore=score;best=i;}
        }
        return best;
    }

    private float teamIntent(int team) {
        int gf=team==liveHome?liveHomeGoals:liveAwayGoals,ga=team==liveHome?liveAwayGoals:liveHomeGoals;
        float intent=MatchMath.intent(gf,ga,liveMinute);
        if(team==selectedClub) intent+=("Attacking".equals(liveMentality)?.55f:"Defensive".equals(liveMentality)?-.55f:0);
        return MatchMath.clamp(intent,-1,1);
    }

    private void manageOpposition(int minute) {
        if(aiSubsUsed>=5 || minute<45)return;
        int team=selectedClub==liveHome?liveAway:liveHome;
        ArrayList<Integer> xi=team==liveHome?liveHomeLineupIds:liveAwayLineupIds;
        int slot=-1;float need=0;
        for(int i=1;i<xi.size();i++) {
            Player p=findPlayer(xi.get(i)); if(p==null)continue;
            float n=(100-p.fitness)+(p.injuredWeeks>0?100:0)+(i>=8&&teamIntent(team)>0?8:0);
            if(n>need){need=n;slot=i;}
        }
        if(slot<0 || (need<12 && minute<65))return;
        Player off=findPlayer(xi.get(slot)),best=null;
        float quality=-1000;
        for(Player p:players) if(p.team==team && p.injuredWeeks==0 && !xi.contains(p.id) && !aiDeparted.contains(p.id) && !"GK".equals(p.position)) {
            float q=p.overall+p.fitness*.35f-(p.position.equals(off.position)?0:25);
            if(q>quality){quality=q;best=p;}
        }
        if(best==null)return;
        xi.set(slot,best.id);aiDeparted.add(off.id);aiSubsUsed++;matchParticipants.add(best.id);
        recordCommentary(team,"SUB: "+best.name+" replaces "+off.name+" for "+clubNames[team]+".");
    }

    private void performDribbleAction(int team, int slot) {
        Player carrier = playerForPitchSlot(team, slot);
        int defending = team == liveHome ? liveAway : liveHome;
        int defenderSlot = nearestDefenderSlot(defending, livePitchView.getPlayerX(team, slot), livePitchView.getPlayerY(team, slot));
        Player defender = playerForPitchSlot(defending, defenderSlot);
        if (carrier == null) return;

        double duel = 0.58 + (carrier.pace + carrier.technique - 140) / 260.0;
        if (defender != null) duel -= (defender.defending + defender.physical - 140) / 330.0;
        duel = Math.max(0.35, Math.min(0.78, duel));
        liveActionDuration = 0.72f + random.nextFloat() * 0.20f;
        liveActionTimer = liveActionDuration;

        if (random.nextDouble() < duel) {
            float x = livePitchView.getPlayerX(team, slot) + (team == liveHome ? 0.075f : -0.075f);
            float y = livePitchView.getPlayerY(team, slot) + (random.nextFloat() - 0.5f) * 0.08f;
            livePitchView.animateDribble(team, slot, x, y, liveActionDuration);
            livePendingPossessionTeam = team;
            livePendingCarrierSlot = slot;
            String opponentName = defender == null ? "his marker" : defender.name;
            recordCommentary(team, carrier.name + " takes on " + opponentName + " and carries the ball forward.");
        } else {
            if (defender != null) {
                if (defending == liveHome) liveHomeTackles++; else liveAwayTackles++;
            matchTackles.put(defender.id,matchTackles.getOrDefault(defender.id,0)+1);
                livePitchView.animateBallToSlot(team, slot, defending, defenderSlot, liveActionDuration);
                livePendingPossessionTeam = defending;
                livePendingCarrierSlot = defenderSlot;
                recordCommentary(defending, defender.name + " times the challenge and wins the ball from " + carrier.name + ".");
            } else {
                performPassAction(team, slot, false);
            }
        }
    }

    private void performDuelAction(int attacking, int attackerSlot) {
        playLiveSound("tackle");
        int defending = attacking == liveHome ? liveAway : liveHome;
        Player attacker = playerForPitchSlot(attacking, attackerSlot);
        int defenderSlot = nearestDefenderSlot(defending, livePitchView.getPlayerX(attacking, attackerSlot), livePitchView.getPlayerY(attacking, attackerSlot));
        Player defender = playerForPitchSlot(defending, defenderSlot);
        if (attacker == null || defender == null) { performPassAction(attacking, attackerSlot, false); return; }

        double foulChance = 0.08 + liveRefereeStrictness * 0.005;
        if (defending == selectedClub && "Hard".equals(tacklingInstruction)) foulChance += 0.09;
        if (defending == selectedClub && "Easy".equals(tacklingInstruction)) foulChance -= 0.035;
        liveActionDuration = 0.70f;
        liveActionTimer = liveActionDuration;

        if (random.nextDouble() < Math.max(0.03, foulChance)) {
            playLiveSound("whistle");
            if (defending == liveHome) liveHomeFouls++; else liveAwayFouls++;
            boolean yellow = random.nextDouble() < 0.10 + liveRefereeStrictness * 0.018;
            if (yellow) {
                if (defending == liveHome) liveHomeYellows++; else liveAwayYellows++;
                matchYellows.put(defender.id,matchYellows.getOrDefault(defender.id,0)+1);
                recordCommentary(attacking, defender.name + " catches " + attacker.name + " late. Yellow card.");
            } else {
                recordCommentary(attacking, attacker.name + " is brought down by " + defender.name + ". Free kick.");
            }
            livePitchView.holdBallAt(attacking, attackerSlot);
            livePendingPossessionTeam = attacking;
            livePendingCarrierSlot = attackerSlot;
            return;
        }

        double win = 0.50 + (defender.defending + defender.physical - attacker.technique - attacker.pace) / 320.0;
        if (defending == selectedClub && pressingInstruction) win += 0.05;
        win = Math.max(0.30, Math.min(0.72, win));
        if (random.nextDouble() < win) {
            if (defending == liveHome) liveHomeTackles++; else liveAwayTackles++;
            livePitchView.animateBallToSlot(attacking, attackerSlot, defending, defenderSlot, liveActionDuration);
            livePendingPossessionTeam = defending;
            livePendingCarrierSlot = defenderSlot;
            recordCommentary(defending, defender.name + " steps in strongly and comes away with the ball.");
        } else {
            float x = livePitchView.getPlayerX(attacking, attackerSlot) + (attacking == liveHome ? 0.055f : -0.055f);
            float y = livePitchView.getPlayerY(attacking, attackerSlot);
            livePitchView.animateDribble(attacking, attackerSlot, x, y, liveActionDuration);
            livePendingPossessionTeam = attacking;
            livePendingCarrierSlot = attackerSlot;
            recordCommentary(attacking, attacker.name + " rides the challenge from " + defender.name + ".");
        }
    }

    private void performCrossAction(int team, int fromSlot) {
        if (liveSpeed <= 3) playLiveSound("kick");
        Player crosser = playerForPitchSlot(team, fromSlot);
        int targetSlot = bestBoxTarget(team, fromSlot);
        Player target = playerForPitchSlot(team, targetSlot);
        if (crosser == null || target == null) { performPassAction(team, fromSlot, false); return; }
        if (team == liveHome) liveHomePasses++; else liveAwayPasses++;

        livePitchView.prepareReceivingRun(team, targetSlot, true);
        double success = 0.56 + (crosser.passing + crosser.technique - 135) / 315.0;
        success = Math.max(0.40, Math.min(0.80, success));
        liveActionDuration = 0.96f;
        liveActionTimer = liveActionDuration;
        int defending = team == liveHome ? liveAway : liveHome;

        if (random.nextDouble() < success) {
            if (team == liveHome) liveHomePassesComplete++; else liveAwayPassesComplete++;
            livePitchView.animateBallToSlot(team, fromSlot, team, targetSlot, liveActionDuration);
            livePendingPossessionTeam = team;
            livePendingCarrierSlot = targetSlot;
            recordCommentary(team, crosser.name + " gets his head up and bends a cross into the run of " + target.name + ".");
        } else {
            int defenderSlot = nearestDefenderSlot(defending, livePitchView.getPlayerX(team, targetSlot), livePitchView.getPlayerY(team, targetSlot));
            Player defender = playerForPitchSlot(defending, defenderSlot);
            if (random.nextDouble() < 0.24) {
                float cornerX = team == liveHome ? 0.988f : 0.012f;
                float cornerY = livePitchView.getPlayerY(team, fromSlot) < 0.5f ? 0.012f : 0.988f;
                livePitchView.animateBallToPoint(team, fromSlot, cornerX, cornerY, liveActionDuration);
                if (team == liveHome) liveHomeCorners++; else liveAwayCorners++;
                scheduleSetPiece("CORNER", team, cornerX, cornerY);
                recordCommentary(team, (defender == null ? "A defender" : defender.name) + " blocks the cross behind. Corner.");
            } else {
                livePitchView.animateBallToSlot(team, fromSlot, defending, defenderSlot, liveActionDuration);
                livePendingPossessionTeam = defending;
                livePendingCarrierSlot = defenderSlot;
                recordCommentary(defending, (defender == null ? clubNames[defending] : defender.name) + " tracks the runner and heads the cross away.");
            }
        }
    }

    private void performShotAction(int team, int slot) {
        playLiveSound("shot");
        Player shooter = playerForPitchSlot(team, slot);
        int defending = team == liveHome ? liveAway : liveHome;
        Player keeper = playerForPitchSlot(defending, 0);
        if (shooter == null) return;

        if (team == liveHome) liveHomeShots++; else liveAwayShots++;
        double attackQ = effectiveTeamQuality(team, true);
        double defenceQ = effectiveTeamQuality(defending, false);
        double onTarget = 0.42 + (shooter.finishing - 65) / 170.0 + (attackQ - defenceQ) / 300.0;
        if ("Windy".equals(liveWeather) || "Heavy Rain".equals(liveWeather)) onTarget -= 0.04;
        onTarget = Math.max(0.25, Math.min(0.73, onTarget));

        liveActionDuration = 0.78f;
        liveActionTimer = liveActionDuration;
        float goalX = team == liveHome ? 0.985f : 0.015f;
        float goalY = 0.44f + random.nextFloat() * 0.12f;

        if (random.nextDouble() < onTarget) {
            if (team == liveHome) liveHomeOnTarget++; else liveAwayOnTarget++;
            double goalChance = 0.19 + (shooter.finishing - 68) / 235.0 + (attackQ - defenceQ) / 430.0;
            if (liveMinute > 75) goalChance += (shooter.importantMatches - 10) / 390.0;
            goalChance = Math.max(0.10, Math.min(0.37, goalChance));

            if (random.nextDouble() < goalChance) {
                livePitchView.animateBallToPoint(team, slot, goalX, goalY, liveActionDuration);
                pendingGoalTeam=team;pendingGoalSlot=slot;pendingGoalDelay=liveActionDuration;
                pendingAssistId=lastPassTeam==team?lastPasserId:-1;
                recordCommentary(team,shooter.name+" strikes towards goal!");
                livePendingPossessionTeam = defending;
                livePendingCarrierSlot = 9;
                liveNeedsKickoffReset = true;
                liveActionTimer += 1.80f;
            } else {
                double keeperQuality = keeper == null ? 68 : keeper.overall * 0.65 + keeper.technicalGoalkeeperScore() * 0.35;
                double holdChance = Math.max(0.32, Math.min(0.72, 0.40 + (keeperQuality - shooter.finishing) / 170.0));
                if (random.nextDouble() < holdChance) {
                    livePitchView.animateBallToSlot(team, slot, defending, 0, liveActionDuration);
                    livePendingPossessionTeam = defending;
                    livePendingCarrierSlot = 0;
                    queueContactSound("catch");
                    recordCommentary(defending, (keeper == null ? "The goalkeeper" : keeper.name) + " gets both hands behind " + shooter.name + "'s effort and holds it.");
                } else {
                    float reboundX = team == liveHome ? 0.83f + random.nextFloat() * 0.08f : 0.17f - random.nextFloat() * 0.08f;
                    float reboundY = 0.28f + random.nextFloat() * 0.44f;
                    livePitchView.animateBallToPoint(team, slot, reboundX, reboundY, liveActionDuration);
                    liveLooseBall = true;
                    liveLooseBallX = reboundX;
                    liveLooseBallY = reboundY;
                    queueContactSound("save");
                    recordCommentary(-1, (keeper == null ? "The goalkeeper" : keeper.name) + " can only parry it — the ball is loose in the area!");
                }
            }
        } else {
            if (random.nextDouble() < 0.23) {
                if (team == liveHome) liveHomeCorners++; else liveAwayCorners++;
                float cornerY = random.nextBoolean() ? 0.012f : 0.988f;
                livePitchView.animateBallToPoint(team, slot, goalX, cornerY, liveActionDuration);
                scheduleSetPiece("CORNER", team, goalX, cornerY);
                recordCommentary(team, shooter.name + " sees the shot take a deflection and spin behind for a corner.");
            } else {
                float missY = random.nextBoolean() ? 0.28f : 0.72f;
                livePitchView.animateBallToPoint(team, slot, goalX, missY, liveActionDuration);
                scheduleSetPiece("GOAL_KICK", defending, defending == liveHome ? 0.08f : 0.92f, 0.50f);
                queueContactSound("miss");
                String[] miss = {"drags the shot wide.", "fires over the bar.", "pulls the effort past the post.", "cannot keep the shot down."};
                recordCommentary(team, shooter.name + " " + miss[random.nextInt(miss.length)] + " Goal kick.");
            }
        }
    }

    private int bestBoxTarget(int team, int exceptSlot) {
        int[] preference = {9, 8, 10, 7, 6, 5};
        for (int p : preference) if (p != exceptSlot && playerForPitchSlot(team, p) != null) return p;
        return exceptSlot == 9 ? 8 : 9;
    }

    private int wideAttackSlot(int team) {
        return random.nextBoolean() ? 8 : 10;
    }

    private int nearestDefenderSlot(int team, float x, float y) {
        int best = 1;
        float bestD = Float.MAX_VALUE;
        for (int i = 1; i < 11; i++) {
            float dx = livePitchView.getPlayerX(team, i) - x;
            float dy = livePitchView.getPlayerY(team, i) - y;
            float d = dx * dx + dy * dy;
            if (d < bestD) { bestD = d; best = i; }
        }
        return best;
    }

    private void prepareLivePitchLineups() {
        liveHomeLineupIds.clear();
        liveAwayLineupIds.clear();
        if (liveHome == selectedClub) liveHomeLineupIds.addAll(sortedSelectedXI());
        else liveHomeLineupIds.addAll(bestElevenForTeam(liveHome));
        if (liveAway == selectedClub) liveAwayLineupIds.addAll(sortedSelectedXI());
        else liveAwayLineupIds.addAll(bestElevenForTeam(liveAway));
        while (liveHomeLineupIds.size() > 11) liveHomeLineupIds.remove(liveHomeLineupIds.size() - 1);
        while (liveAwayLineupIds.size() > 11) liveAwayLineupIds.remove(liveAwayLineupIds.size() - 1);
    }

    private ArrayList<Integer> sortedSelectedXI() {
        ArrayList<Player> selected = new ArrayList<>();
        for (int id : liveXIIds) {
            Player p = findPlayer(id);
            if (p != null) selected.add(p);
        }
        Collections.sort(selected, (a, b) -> Integer.compare(playerSelectedSlot[a.id], playerSelectedSlot[b.id]));
        ArrayList<Integer> ids = new ArrayList<>();
        for (Player p : selected) ids.add(p.id);
        return ids;
    }

    private boolean matchesStarterSlot(String natural, String slot) {
        if (slot.equals(natural)) return true;
        if ("DC".equals(slot)) return natural.startsWith("D") || natural.equals("SW");
        if ("DL".equals(slot) || "DR".equals(slot)) return natural.contains("D") || natural.contains("WB");
        if ("LWB".equals(slot) || "RWB".equals(slot)) return natural.contains("D") || natural.contains("M") || natural.contains("WB");
        if ("DMC".equals(slot)) return natural.contains("DM") || natural.contains("MC");
        if ("MC".equals(slot)) return natural.contains("MC") || natural.contains("DM") || natural.contains("AMC");
        if ("AML".equals(slot) || "AMR".equals(slot) || "ML".equals(slot) || "MR".equals(slot)) return natural.contains("AM") || natural.contains("M") || natural.contains("W");
        if ("AMC".equals(slot)) return natural.contains("AM") || natural.contains("MC");
        if ("FC".equals(slot)) return natural.contains("F") || natural.contains("S");
        return false;
    }

    private ArrayList<Integer> bestElevenForTeam(int team) {
        ArrayList<Player> squad = new ArrayList<>();
        for (Player p : players) if (p.team == team) squad.add(p);
        ArrayList<Player> chosen = new ArrayList<>();
        Player keeper = null;
        for (Player p : squad) if ("GK".equals(p.position) && (keeper == null || p.overall > keeper.overall)) keeper = p;
        if (keeper != null) chosen.add(keeper);

        ArrayList<Player> outfield = new ArrayList<>();
        for (Player p : squad) if (!"GK".equals(p.position)) outfield.add(p);
        Collections.sort(outfield, (a,b) -> Integer.compare(b.overall, a.overall));

        int defenders = 0, mids = 0, forwards = 0;
        for (Player p : outfield) {
            int rank = positionRank(p.position);
            if (rank <= 3 && defenders < 4) { chosen.add(p); defenders++; }
        }
        for (Player p : outfield) {
            int rank = positionRank(p.position);
            if (rank >= 4 && rank <= 8 && mids < 3 && !chosen.contains(p)) { chosen.add(p); mids++; }
        }
        for (Player p : outfield) {
            int rank = positionRank(p.position);
            if (rank >= 9 && forwards < 3 && !chosen.contains(p)) { chosen.add(p); forwards++; }
        }
        for (Player p : outfield) if (chosen.size() < 11 && !chosen.contains(p)) chosen.add(p);
        sortPlayersByPositionThenOverall(chosen);
        ArrayList<Integer> ids = new ArrayList<>();
        for (Player p : chosen) if (ids.size() < 11) ids.add(p.id);
        return ids;
    }

    private Player playerForPitchSlot(int team, int slot) {
        ArrayList<Integer> ids = team == liveHome ? liveHomeLineupIds : liveAwayLineupIds;
        if (slot < 0 || slot >= ids.size()) return null;
        return findPlayer(ids.get(slot));
    }

    private String pitchName(int team, int slot) {
        Player p = playerForPitchSlot(team, slot);
        if (p == null) return "";
        String[] parts = p.name.trim().split(" ");
        return parts.length == 0 ? p.name : parts[parts.length - 1];
    }

    private void recordCommentary(int team, String line) {
        liveCommentaryTeam = team;
        liveCommentary = line;
        liveCommentaryHistory.add(liveMinute + "'  " + line);
        while (liveCommentaryHistory.size() > 18) liveCommentaryHistory.remove(0);
        refreshLiveHeader();
    }

    private void recordCommentary(String line) {
        recordCommentary(-1, line);
    }

    private Player randomPlayerForTeam(int team, boolean attacking) {
        ArrayList<Player> choices = new ArrayList<>();
        if (liveMatchActive && team == selectedClub && !liveXIIds.isEmpty()) {
            for (int id : liveXIIds) {
                Player p = findPlayer(id);
                if (p != null && (!attacking || isAttackingPosition(p.position))) choices.add(p);
            }
            if (choices.isEmpty()) {
                for (int id : liveXIIds) {
                    Player p = findPlayer(id);
                    if (p != null) choices.add(p);
                }
            }
        } else {
            for (Player p : players) {
                if (p.team == team && (!attacking || isAttackingPosition(p.position))) choices.add(p);
            }
        }
        if (choices.isEmpty()) return null;
        return choices.get(random.nextInt(choices.size()));
    }

    private double effectiveTeamQuality(int team, boolean attacking) {
        double total = 0.0;
        int count = 0;
        for (Player p : players) {
            if (p.team != team) continue;
            if (liveMatchActive && !(team==liveHome?liveHomeLineupIds:liveAwayLineupIds).contains(p.id)) continue;
            double core = p.overall;
            core += (p.morale - 75) * 0.05;
            core += (p.fitness - 85) * 0.04;
            core += (p.consistency - 10) * 0.18;
            if (attacking) core += (p.passing + p.technique + p.finishing - 210) * 0.025;
            else core += (p.defending + p.physical + p.passing - 210) * 0.022;
            total += core;
            count++;
        }
        if (count == 0) return strength[team];
        double q = total / count;
        if (team == selectedClub) {
            if (pressingInstruction) q += 0.8;
            if (offsideTrapInstruction && !attacking) q += 0.5;
            if (menBehindBallInstruction && !attacking) q += 1.2;
            if ("Short".equals(passingInstruction) && attacking) q += 0.6;
            if ("Direct".equals(passingInstruction) && attacking) q += 0.35;
        }
        return q;
    }

    private int possessionTacticModifier() {
        int mod = 0;
        if ("Short".equals(passingInstruction)) mod += 3;
        if ("Long".equals(passingInstruction)) mod -= 2;
        if (pressingInstruction) mod += 1;
        if (menBehindBallInstruction) mod -= 3;
        if ("Wide".equals(wibWobShape)) mod += 1;
        return mod;
    }

    private String randomWeather() {
        String[] weather = {"Dry", "Dry", "Cloudy", "Wet", "Windy", "Heavy Rain"};
        return weather[random.nextInt(weather.length)];
    }

    private String randomPlayerName(int team, boolean attacking) {
        ArrayList<Player> choices = new ArrayList<>();

        if (liveMatchActive && team == selectedClub && !liveXIIds.isEmpty()) {
            for (int id : liveXIIds) {
                Player p = findPlayer(id);
                if (p == null) continue;
                if (!attacking || isAttackingPosition(p.position)) choices.add(p);
            }
            if (choices.isEmpty()) {
                for (int id : liveXIIds) {
                    Player p = findPlayer(id);
                    if (p != null) choices.add(p);
                }
            }
        } else {
            for (Player p : players) {
                if (p.team != team) continue;
                if (!attacking || isAttackingPosition(p.position)) choices.add(p);
            }
        }

        if (choices.isEmpty()) return clubNames[team] + " player";
        return choices.get(random.nextInt(choices.size())).name;
    }

    private boolean isAttackingPosition(String position) {
        return position.equals("ST") || position.equals("RW") || position.equals("LW")
                || position.equals("AM") || position.equals("CM") || position.equals("RM")
                || position.equals("LM");
    }

    private void awardLiveGoal(int team,String scorerName) {
        for(Player p:players) if(p.team==team && p.name.equals(scorerName)) {
            matchGoals.put(p.id,matchGoals.getOrDefault(p.id,0)+1);
            if(pendingAssistId>=0 && pendingAssistId!=p.id) matchAssists.put(pendingAssistId,matchAssists.getOrDefault(pendingAssistId,0)+1);
            break;
        }
        lastPasserId=-1;lastPassTeam=-1;pendingAssistId=-1;
    }

    private void completePendingGoal() {
        int team=pendingGoalTeam,slot=pendingGoalSlot;
        if(team<0)return;
        pendingGoalTeam=-1;
        if(team==liveHome)liveHomeGoals++;else liveAwayGoals++;
        Player scorer=playerForPitchSlot(team,slot);
        awardLiveGoal(team,scorer==null?"":scorer.name);
        playLiveSound(team==liveHome?"goal":"save");
        if(livePitchView!=null)livePitchView.startGoalCelebration(team,slot);
        recordCommentary(team,"GOAL! "+safeName(scorer)+" finishes the move for "+clubNames[team]+"!");
        refreshLiveHeader();
    }

    private void refreshLiveHeader() {
        if (liveScoreText != null) liveScoreText.setText(liveHomeGoals + "  -  " + liveAwayGoals);

        String half = liveMinute < 45 ? "1st" : liveMinute < 90 ? "2nd" : "FT";
        if (liveClockText != null) {
            int displayMinute = Math.min(90, (int)liveMinuteFloat);
            int displaySecond = liveMinuteFloat >= 90f ? 0 : Math.min(59, Math.max(0, Math.round((liveMinuteFloat - displayMinute) * 60f)));
            String clock = liveMinute >= 90 ? "90:00      FT" : String.format(Locale.UK, "%02d:%02d      %s", displayMinute, displaySecond, half);
            if (livePaused && !liveHalfTimeTacticsActive) clock += "  PAUSED";
            liveClockText.setText(clock);
        }

        if (liveCommentaryText != null) {
            liveCommentaryText.setText(liveCommentary);
            int colour = liveCommentaryTeam >= 0 && liveCommentaryTeam < primaryColours.length
                    ? primaryColours[liveCommentaryTeam] : panelLight;
            GradientDrawable commentaryBg=rounded(panelLight);
            commentaryBg.setStroke(dp(1),colour);
            liveCommentaryText.setBackground(commentaryBg);
            liveCommentaryText.setTextColor(text);
        }
        if (liveStatsText != null) liveStatsText.setText(liveStatsCompactSummary());
        if (liveFormationButton != null) liveFormationButton.setText("Formation • " + liveFormation);
        if (liveSpeedButton != null) liveSpeedButton.setText("Speed • " + liveSpeedLabel());
        if (livePauseButton != null) livePauseButton.setText(livePaused ? "▶ Resume" : "Ⅱ Pause");
        if (liveTacticsButton != null) liveTacticsButton.setText("Tactics");
        if (liveInstructionsButton != null) liveInstructionsButton.setText("Instructions • " + liveMentality);
        if (liveClassicButton != null) liveClassicButton.setText("Team • " + passingInstruction + " / " + tacklingInstruction);
        if (liveSubButton != null) liveSubButton.setText("Subs • " + liveSubsUsed + "/5");
    }

    private String liveStatsCompactSummary() {
        int homeAcc = liveHomePasses == 0 ? 0 : Math.round(100f * liveHomePassesComplete / liveHomePasses);
        int awayAcc = liveAwayPasses == 0 ? 0 : Math.round(100f * liveAwayPassesComplete / liveAwayPasses);
        return String.format(Locale.UK,
                "%-8s %3d  %3d\n%-8s %3d  %3d\n%-8s %3d  %3d\n%-8s %3d  %3d\n%-8s %3d  %3d\n%-8s %3d  %3d\n%-8s %3d  %3d",
                "Poss%", liveHomePossession, liveAwayPossession,
                "Shots", liveHomeShots, liveAwayShots,
                "Target", liveHomeOnTarget, liveAwayOnTarget,
                "Corners", liveHomeCorners, liveAwayCorners,
                "Pass%", homeAcc, awayAcc,
                "Tackles", liveHomeTackles, liveAwayTackles,
                "Fouls", liveHomeFouls, liveAwayFouls);
    }

    private String liveStatsSummary() {
        int homeAcc = liveHomePasses == 0 ? 0 : Math.round(100f * liveHomePassesComplete / liveHomePasses);
        int awayAcc = liveAwayPasses == 0 ? 0 : Math.round(100f * liveAwayPassesComplete / liveAwayPasses);
        return String.format(Locale.UK,
                "%-13s %3d%%   %3d%%\n%-13s %3d     %3d\n%-13s %3d     %3d\n%-13s %3d     %3d\n%-13s %3d%%   %3d%%\n%-13s %3d     %3d\n%-13s %3d     %3d",
                "Possession", liveHomePossession, liveAwayPossession,
                "Shots", liveHomeShots, liveAwayShots,
                "On target", liveHomeOnTarget, liveAwayOnTarget,
                "Corners", liveHomeCorners, liveAwayCorners,
                "Pass acc.", homeAcc, awayAcc,
                "Tackles", liveHomeTackles, liveAwayTackles,
                "Fouls", liveHomeFouls, liveAwayFouls);
    }

    private void showLiveOverviewDialog() {
        StringBuilder recent = new StringBuilder();
        for (String line : liveCommentaryHistory) recent.append(line).append("\n");
        new BossDialog.Builder(this)
                .setTitle("Live Overview")
                .setMessage(
                        clubNames[liveHome] + " " + liveHomeGoals + " - " + liveAwayGoals + " " + clubNames[liveAway]
                                + "\n\nWeather: " + liveWeather
                                + "\nReferee strictness: " + liveRefereeStrictness + "/20"
                                + "\nFormation: " + liveFormation
                                + "\nMentality: " + liveMentality
                                + "\nPassing: " + passingInstruction
                                + "\nPressing: " + (pressingInstruction ? "Yes" : "No")
                                + "\n\nRECENT COMMENTARY\n" + recent)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showLiveCommentaryDialog() {
        StringBuilder sb = new StringBuilder();
        if (liveCommentaryHistory.isEmpty()) sb.append("No commentary yet.");
        else for (String line : liveCommentaryHistory) sb.append(line).append("\n\n");
        new BossDialog.Builder(this)
                .setTitle("Match Commentary")
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void showLiveStatsDialog() {
        new BossDialog.Builder(this)
                .setTitle("Match Stats")
                .setMessage(clubNames[liveHome] + "        " + liveHomeGoals + " - " + liveAwayGoals + "        " + clubNames[liveAway]
                        + "\n\n" + liveStatsSummary())
                .setPositiveButton("Close", null)
                .show();
    }

    private void showLiveRatingsDialog() {
        StringBuilder sb = new StringBuilder();
        ArrayList<Player> teamPlayers = new ArrayList<>();

        for (int id : liveXIIds) {
            Player p = findPlayer(id);
            if (p != null) teamPlayers.add(p);
        }
        if (teamPlayers.isEmpty()) {
            for (Player p : players) if (p.team == selectedClub) teamPlayers.add(p);
        }

        sortPlayersByPositionThenOverall(teamPlayers);
        for (Player p : teamPlayers) {
            double rating=6.0+matchGoals.getOrDefault(p.id,0)*.9+matchAssists.getOrDefault(p.id,0)*.5
                    +Math.min(1.2,matchTackles.getOrDefault(p.id,0)*.15)
                    +Math.min(.6,matchCompletedPasses.getOrDefault(p.id,0)*.025)
                    -Math.min(.8,matchMissedPasses.getOrDefault(p.id,0)*.08)-matchYellows.getOrDefault(p.id,0)*.25;
            rating=Math.max(1,Math.min(10,rating));
            sb.append(String.format(Locale.UK,"%s  •  %s  •  %.1f\nCondition %d%%  •  Goals %d  •  Cards %d\n\n",p.name,p.position,rating,p.fitness,matchGoals.getOrDefault(p.id,0),matchYellows.getOrDefault(p.id,0)));
        }

        new BossDialog.Builder(this)
                .setTitle("Match contribution • event-based")
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void prepareLiveLineup() {
        ensureTacticsValid();
        liveXIIds.clear();
        liveBenchIds.clear();

        for (Player p : players) {
            if (p.team != selectedClub) continue;
            if (playerRoleStatus[p.id] == 2) liveXIIds.add(p.id);
            else if (playerRoleStatus[p.id] == 1) liveBenchIds.add(p.id);
        }

        // The match can only start with exactly 11 selected starters.
        // Do not silently auto-pick here; respect the user's tactical selection.
    }

    private void showSubstitutionDialog() {
        if (liveBenchIds.isEmpty()) {
            Toast.makeText(this, "No substitutes available.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Player> starters = new ArrayList<>();
        for (int id : liveXIIds) {
            Player p = findPlayer(id);
            if (p != null) starters.add(p);
        }
        sortPlayersByPositionThenOverall(starters);

        String[] labels = new String[starters.size()];
        for (int i = 0; i < starters.size(); i++) {
            Player p = starters.get(i);
            labels[i] = "OUT  •  " + p.position + "  " + p.name + "  OVR " + p.overall;
        }

        new BossDialog.Builder(this)
                .setTitle("Substitution " + (liveSubsUsed + 1) + " of 5")
                .setItems(labels, (dialog, which) -> showSubOnDialog(starters.get(which).id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSubOnDialog(int offId) {
        ArrayList<Player> bench = new ArrayList<>();
        for (int id : liveBenchIds) {
            Player p = findPlayer(id);
            if (p != null) bench.add(p);
        }
        sortPlayersByPositionThenOverall(bench);

        String[] labels = new String[bench.size()];
        for (int i = 0; i < bench.size(); i++) {
            Player p = bench.get(i);
            labels[i] = "IN  •  " + p.position + "  " + p.name + "  OVR " + p.overall;
        }

        new BossDialog.Builder(this)
                .setTitle("Choose substitute")
                .setItems(labels, (dialog, which) -> {
                    performLiveSubstitution(offId,bench.get(which).id);

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void syncSelectedLineupToPitch(int offId, int onId) {
        ArrayList<Integer> ids = selectedClub == liveHome ? liveHomeLineupIds : liveAwayLineupIds;
        int slot = ids.indexOf(offId);
        if (slot >= 0) ids.set(slot, onId);
        else if (ids.size() < 11) ids.add(onId);
        if (livePitchView != null) livePitchView.refreshFormationAnchors();
    }

    private String styleToMentality(String style) {
        if ("Attacking".equals(style) || "High Press".equals(style) || "Direct".equals(style)) return "Attacking";
        if ("Defensive".equals(style) || "Counter Attack".equals(style)) return "Defensive";
        return "Balanced";
    }

    private void showFormationDialog() {
        final String[] options = {"4-3-3", "4-2-3-1", "4-4-2", "3-5-2", "5-3-2"};
        new BossDialog.Builder(this)
                .setTitle("Formation")
                .setSingleChoiceItems(options, Arrays.asList(options).indexOf(liveFormation), (dialog, which) -> {
                    liveFormation = options[which];
                    tacticFormation = liveFormation;
                    saveCurrentGame();
                    if (livePitchView != null) livePitchView.refreshFormationAnchors();
                    recordCommentary(selectedClub, "Tactical change: " + clubNames[selectedClub] + " switch to " + liveFormation + ".");
                    dialog.dismiss();
                    refreshLiveHeader();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showInstructionsDialog() {
        final String[] options = {"Defensive", "Balanced", "Attacking"};
        int checked = liveMentality.equals("Defensive") ? 0 : liveMentality.equals("Attacking") ? 2 : 1;
        new BossDialog.Builder(this)
                .setTitle("Team Instructions")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    liveMentality = options[which];
                    playStyle = liveMentality;
                    saveCurrentGame();
                    recordCommentary(selectedClub, "Instruction changed: " + liveMentality + " mentality.");
                    dialog.dismiss();
                    refreshLiveHeader();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRolesDialog() {
        new BossDialog.Builder(this)
                .setTitle("Roles")
                .setMessage("BOSS XI currently assigns roles automatically from each player's position and attributes.\n\n"
                        + "The next tactics update can add individual roles such as Advanced Forward, Playmaker, Ball-Winning Midfielder and Wing-Back.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void finishLiveMatch() {
        if (!liveMatchActive) return;
        if(pendingGoalTeam>=0)completePendingGoal();
        playLiveSound("whistle");
        new Handler(Looper.getMainLooper()).postDelayed(this::stopLiveMatchAudio, 900);
        stopLiveMatchTicker();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        liveMinute = 90;

        if (matchday >= 0 && matchday < clubMatchGF.length) {
            if (liveHome == selectedClub) {
                clubMatchGF[matchday] = liveHomeGoals;
                clubMatchGA[matchday] = liveAwayGoals;
            } else {
                clubMatchGF[matchday] = liveAwayGoals;
                clubMatchGA[matchday] = liveHomeGoals;
            }
        }

        updateTable(liveHome, liveAway, liveHomeGoals, liveAwayGoals);

        StringBuilder results = new StringBuilder();
        results.append(clubNames[liveHome]).append("  ").append(liveHomeGoals)
                .append(" - ").append(liveAwayGoals).append("  ").append(clubNames[liveAway]).append("\n\n");

        for (int[] fixture : liveOtherFixtures) {
            int home = fixture[0];
            int away = fixture[1];
            int homeGoals = simulateGoals(home, away, true);
            int awayGoals = simulateGoals(away, home, false);
            updateTable(home, away, homeGoals, awayGoals);
            results.append(clubNames[home]).append("  ").append(homeGoals)
                    .append(" - ").append(awayGoals).append("  ").append(clubNames[away]).append("\n\n");
        }

        for(java.util.Map.Entry<Integer,Integer> e:matchGoals.entrySet()) {Player p=findPlayer(e.getKey());if(p!=null)p.goals+=e.getValue();}
        for(java.util.Map.Entry<Integer,Integer> e:matchAssists.entrySet()) {Player p=findPlayer(e.getKey());if(p!=null)p.assists+=e.getValue();}
        updatePlayerMatchStats();
        matchInProgress=false;
        applyTrainingSession();
        updateBoardAfterMatch();
        processFinanceWeek();
        advanceStadiumProject();
        addNews("MATCH", "Full-time: " + clubNames[liveHome] + " " + liveHomeGoals + "-" + liveAwayGoals + " " + clubNames[liveAway],
                "Possession " + liveHomePossession + "-" + liveAwayPossession + " • Shots " + liveHomeShots + "-" + liveAwayShots);
        matchday++;
        processScoutingAssignments();
        processInjuriesAndRecovery();
        if (matchday == 34) processSeasonEnd();
        LocalDate playedDate = currentDate;
        currentDate = currentDate.plusDays(7);
        saveCurrentGame();
        backAction = () -> showDashboard();

        LinearLayout page = createPage(
                "Full Time",
                "Matchday " + matchday + " • " + playedDate.format(DATE_FORMAT),
                true
        );

        TextView mainResult = makeText(
                clubNames[liveHome] + "  " + liveHomeGoals + " - " + liveAwayGoals + "  " + clubNames[liveAway],
                22,
                accent
        );
        mainResult.setTypeface(Typeface.DEFAULT_BOLD);
        mainResult.setPadding(0, 0, 0, dp(14));
        page.addView(mainResult);

        LinearLayout summary = makePanel();
        summary.addView(makeText("MATCH SUMMARY", 13, accent));
        summary.addView(makeText(
                liveStatsSummary() + "\n\nFinal mentality: " + liveMentality
                        + "\nFormation: " + liveFormation
                        + "\nTraining completed: " + trainingFocus,
                15,
                text
        ));
        page.addView(summary);

        TextView all = makeText("ALL RESULTS\n\n" + results, 15, text);
        all.setBackground(rounded(panel));
        all.setPadding(dp(16), dp(16), dp(16), dp(16));
        page.addView(all);

        page.addView(makeText("", 8, text));
        page.addView(makeAccentButton("Continue", v -> showDashboard()));
        page.addView(makeButton("League Table", v -> showLeagueTable()));
        page.addView(makeButton("Player Database", v -> showAllTeamsPlayers()));
    }

    private class MatchPitchView extends View {
        private final Paint pitchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ballOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final PitchArt pitchArt=new PitchArt();

        private final float[] homeX = new float[11];
        private final float[] homeY = new float[11];
        private final float[] awayX = new float[11];
        private final float[] awayY = new float[11];
        private final float[] homeBaseX = new float[11];
        private final float[] homeBaseY = new float[11];
        private final float[] awayBaseX = new float[11];
        private final float[] awayBaseY = new float[11];
        private final float[] homeVX = new float[11];
        private final float[] homeVY = new float[11];
        private final float[] awayVX = new float[11];
        private final float[] awayVY = new float[11];

        private float ballX = 0.5f;
        private float ballY = 0.5f;
        private float ballStartX = 0.5f;
        private float ballStartY = 0.5f;
        private float ballTargetX = 0.5f;
        private float ballTargetY = 0.5f;
        private int ballTargetTeam = -1;
        private int ballTargetSlot = -1;
        private float ballTravel = 1f;
        private float ballTravelDuration = 0.6f;
        private float ballHeight = 0f;
        private float ballPeakHeight = 0f;
        private int goalCelebrationTeam = -1;
        private int goalCelebrationScorerSlot = -1;
        private float goalCelebrationTime = 0f;
        private float celebrationTargetX = 0.5f;
        private float celebrationTargetY = 0.5f;
        private boolean trackTarget = false;
        private int dribbleTeam = -1;
        private int dribbleSlot = -1;
        private float dribbleTargetX = 0.5f;
        private float dribbleTargetY = 0.5f;
        private final float[] homeRunX = new float[11];
        private final float[] homeRunY = new float[11];
        private final float[] awayRunX = new float[11];
        private final float[] awayRunY = new float[11];
        private final float[] homeRunTime = new float[11];
        private final float[] awayRunTime = new float[11];
        private float offsideFlashX = -1f;
        private float offsideFlashTime = 0f;
        private final Paint offsidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        MatchPitchView() {
            super(MainActivity.this);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            pitchPaint.setColor(Color.rgb(45, 151, 58));
            stripePaint.setColor(Color.argb(24, 255, 255, 255));
            linePaint.setColor(Color.argb(225, 245, 245, 245));
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(dp(1));
            outlinePaint.setStyle(Paint.Style.STROKE);
            outlinePaint.setStrokeWidth(dp(1));
            outlinePaint.setColor(Color.WHITE);
            ballPaint.setColor(Color.WHITE);
            ballOutlinePaint.setColor(Color.BLACK);
            ballOutlinePaint.setStyle(Paint.Style.STROKE);
            ballOutlinePaint.setStrokeWidth(dp(1));
            labelPaint.setColor(Color.WHITE);
            labelPaint.setTextSize(dp(7));
            labelPaint.setTypeface(Typeface.DEFAULT_BOLD);
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setShadowLayer(dp(2), 0, dp(1), Color.BLACK);
            shadowPaint.setColor(Color.argb(80, 0, 0, 0));
            offsidePaint.setColor(Color.argb(220, 255, 214, 64));
            offsidePaint.setStrokeWidth(dp(2));
            refreshFormationAnchors();
            for (int i = 0; i < 11; i++) {
                homeX[i] = homeBaseX[i]; homeY[i] = homeBaseY[i];
                awayX[i] = awayBaseX[i]; awayY[i] = awayBaseY[i];
            }
        }

        void refreshFormationAnchors() {
            String homeFormation = liveHome == selectedClub ? liveFormation : "4-3-3";
            String awayFormation = liveAway == selectedClub ? liveFormation : "4-3-3";
            fillFormation(homeBaseX, homeBaseY, homeFormation, true);
            fillFormation(awayBaseX, awayBaseY, awayFormation, false);
        }

        private void fillFormation(float[] xs, float[] ys, String formation, boolean home) {
            int[] lines;
            if ("4-2-3-1".equals(formation)) lines = new int[]{4, 2, 3, 1};
            else if ("4-4-2".equals(formation)) lines = new int[]{4, 4, 2};
            else if ("3-5-2".equals(formation)) lines = new int[]{3, 5, 2};
            else if ("5-3-2".equals(formation)) lines = new int[]{5, 3, 2};
            else lines = new int[]{4, 3, 3};

            xs[0] = home ? 0.06f : 0.94f;
            ys[0] = 0.50f;
            int slot = 1;
            float[] lineX = lines.length == 4
                    ? new float[]{0.22f, 0.40f, 0.60f, 0.78f}
                    : new float[]{0.22f, 0.49f, 0.75f};
            for (int li = 0; li < lines.length; li++) {
                int count = lines[li];
                float x = lineX[Math.min(li, lineX.length - 1)];
                if (!home) x = 1f - x;
                for (int j = 0; j < count && slot < 11; j++) {
                    xs[slot] = x;
                    ys[slot] = (j + 1f) / (count + 1f);
                    slot++;
                }
            }
            while (slot < 11) {
                xs[slot] = home ? 0.74f : 0.26f;
                ys[slot] = (slot - 8f) / 4f;
                slot++;
            }
        }

        void resetForKickoff(int team) {
            refreshFormationAnchors();
            ballX = 0.5f;
            ballY = 0.5f;
            ballStartX = ballTargetX = ballX;
            ballStartY = ballTargetY = ballY;
            ballTravel = 1f;
            ballHeight = 0f;
            ballPeakHeight = 0f;
            goalCelebrationTime = 0f;
            goalCelebrationTeam = -1;
            trackTarget = false;
            dribbleTeam = -1;
            dribbleSlot = -1;
            if (liveMinute <= 1) {
                if (team == liveHome) { homeX[9] = 0.485f; homeY[9] = 0.50f; }
                else { awayX[9] = 0.515f; awayY[9] = 0.50f; }
            } else {
                startRun(team, 9, team == liveHome ? 0.485f : 0.515f, 0.50f, 0.55f);
            }
        }

        void holdBallAt(int team, int slot) {
            ballTravel = 1f;
            ballHeight = 0f;
            ballPeakHeight = 0f;
            trackTarget = true;
            ballTargetTeam = team;
            ballTargetSlot = slot;
            ballX = getPlayerX(team, slot);
            ballY = getPlayerY(team, slot);
        }

        void animateBallToSlot(int fromTeam, int fromSlot, int toTeam, int toSlot, float duration) {
            ballStartX = ballX;
            ballStartY = ballY;
            if (Math.abs(ballStartX - 0.5f) < 0.001f && Math.abs(ballStartY - 0.5f) < 0.001f) {
                ballStartX = getPlayerX(fromTeam, fromSlot);
                ballStartY = getPlayerY(fromTeam, fromSlot);
            }
            ballTargetTeam = toTeam;
            ballTargetSlot = toSlot;
            ballTargetX = getPlayerX(toTeam, toSlot);
            ballTargetY = getPlayerY(toTeam, toSlot);
            float[] rx=toTeam==liveHome?homeRunX:awayRunX, ry=toTeam==liveHome?homeRunY:awayRunY;
            float[] rt=toTeam==liveHome?homeRunTime:awayRunTime;
            if(rt[toSlot]>0) {
                float dx=rx[toSlot]-ballTargetX,dy=ry[toSlot]-ballTargetY;
                float distance=(float)Math.hypot(dx,dy*.648f);
                float lead=Math.min(1,Math.max(.015f,duration*.07f)/Math.max(.001f,distance));
                ballTargetX+=dx*lead;ballTargetY+=dy*lead;
            }
            startRun(toTeam,toSlot,ballTargetX,ballTargetY,duration+.6f);
            ballTravel = 0f;
            ballTravelDuration = Math.max(0.20f, duration);
            float travelDistance = (float)Math.hypot(ballTargetX - ballStartX, ballTargetY - ballStartY);
            ballPeakHeight = travelDistance > 0.30f ? Math.min(0.095f, travelDistance * 0.15f) : 0.003f;
            ballHeight = 0f;
            trackTarget = true;
            dribbleTeam = -1;
            dribbleSlot = -1;
        }

        void animateBallToPoint(int fromTeam, int fromSlot, float x, float y, float duration) {
            ballStartX = ballX;
            ballStartY = ballY;
            ballTargetX = clamp(x, 0.01f, 0.99f);
            ballTargetY = clamp(y, 0.02f, 0.98f);
            ballTravel = 0f;
            ballTravelDuration = Math.max(0.20f, duration);
            float travelDistance = (float)Math.hypot(ballTargetX - ballStartX, ballTargetY - ballStartY);
            ballPeakHeight = Math.min(0.035f, 0.006f + travelDistance * 0.040f);
            ballHeight = 0f;
            trackTarget = false;
            ballTargetTeam = -1;
            ballTargetSlot = -1;
            dribbleTeam = -1;
            dribbleSlot = -1;
        }

        void animateDribble(int team, int slot, float x, float y, float duration) {
            dribbleTeam = team;
            dribbleSlot = slot;
            dribbleTargetX = clamp(x, 0.06f, 0.94f);
            dribbleTargetY = clamp(y, 0.06f, 0.94f);
            ballStartX = ballX;
            ballStartY = ballY;
            ballTargetX = dribbleTargetX;
            ballTargetY = dribbleTargetY;
            ballTravel = 0f;
            ballTravelDuration = Math.max(0.25f, duration);
            ballPeakHeight = 0f;
            ballHeight = 0f;
            trackTarget = false;
        }

        void prepareReceivingRun(int team, int slot, boolean longRun) {
            if (slot <= 0 || slot > 10) return;
            float dir = team == liveHome ? 1f : -1f;
            float x = getPlayerX(team, slot) + dir * (longRun ? 0.105f : 0.055f);
            float y = getPlayerY(team, slot) + (random.nextFloat() - 0.5f) * (longRun ? 0.09f : 0.05f);
            startRun(team, slot, x, y, longRun ? 1.05f : 0.78f);
        }

        void startRun(int team, int slot, float x, float y, float seconds) {
            if (slot < 0 || slot > 10) return;
            float[] rx = team == liveHome ? homeRunX : awayRunX;
            float[] ry = team == liveHome ? homeRunY : awayRunY;
            float[] rt = team == liveHome ? homeRunTime : awayRunTime;
            rx[slot] = clamp(x, 0.045f, 0.955f);
            ry[slot] = clamp(y, 0.045f, 0.955f);
            rt[slot] = Math.max(0.25f, seconds);
        }

        void placePlayer(int team, int slot, float x, float y) {
            if (slot < 0 || slot > 10) return;
            // Set-piece players now glide into place rather than teleporting between frames.
            startRun(team, slot, clamp(x, 0.02f, 0.98f), clamp(y, 0.02f, 0.98f), 0.42f);
        }

        void placeBall(float x, float y) {
            ballX = ballStartX = ballTargetX = clamp(x, 0.01f, 0.99f);
            ballY = ballStartY = ballTargetY = clamp(y, 0.01f, 0.99f);
            ballTravel = 1f;
            ballHeight = 0f;
            ballPeakHeight = 0f;
            trackTarget = false;
            ballTargetTeam = -1;
            ballTargetSlot = -1;
        }

        void flashOffsideLine(float x) {
            offsideFlashX = clamp(x, 0.04f, 0.96f);
            offsideFlashTime = 0.90f;
        }

        private int activeRunnerSlot(int team) {
            float[] rt = team == liveHome ? homeRunTime : awayRunTime;
            int best = -1;
            float bestTime = 0f;
            for (int i = 1; i < 11; i++) {
                if (rt[i] > bestTime) { bestTime = rt[i]; best = i; }
            }
            return best;
        }

        private int closestOutfieldSlot(float[] xs, float[] ys, float x, float y, int maxSlot) {
            int best = 1;
            float bestD = Float.MAX_VALUE;
            for (int i = 1; i <= Math.min(10, maxSlot); i++) {
                float dx = xs[i] - x;
                float dy = ys[i] - y;
                float d = dx * dx + dy * dy;
                if (d < bestD) { bestD = d; best = i; }
            }
            return best;
        }

        float getPlayerX(int team, int slot) {
            if (slot < 0 || slot > 10) return 0.5f;
            return team == liveHome ? homeX[slot] : awayX[slot];
        }

        float getPlayerY(int team, int slot) {
            if (slot < 0 || slot > 10) return 0.5f;
            return team == liveHome ? homeY[slot] : awayY[slot];
        }

        void startGoalCelebration(int team, int scorerSlot) {
            goalCelebrationTeam = team;
            goalCelebrationScorerSlot = scorerSlot;
            goalCelebrationTime = 1.65f;
            float scorerY = getPlayerY(team, scorerSlot);
            celebrationTargetX = random.nextBoolean() ? (team==liveHome?.955f:.045f) : getPlayerX(team,scorerSlot);
            celebrationTargetY = scorerY < 0.5f ? 0.075f : 0.925f;
            trackTarget = false;
            ballHeight = 0f;
        }

        float getBallX() { return ballX; }
        float getBallY() { return ballY; }

        void stepSimulation(float dt) {
            if (livePaused) return;

            for (int i = 0; i < 11; i++) {
                homeRunTime[i] = Math.max(0f, homeRunTime[i] - dt);
                awayRunTime[i] = Math.max(0f, awayRunTime[i] - dt);
            }
            offsideFlashTime = Math.max(0f, offsideFlashTime - dt);
            goalCelebrationTime = Math.max(0f, goalCelebrationTime - dt);
            if (goalCelebrationTime <= 0f) {
                goalCelebrationTeam = -1;
                goalCelebrationScorerSlot = -1;
            }

            float teamShift = (ballX - 0.5f) * 0.17f;
            float verticalPull = (ballY - 0.5f) * 0.10f;
            updateTeamMovement(liveHome, homeX, homeY, homeBaseX, homeBaseY, teamShift, verticalPull, dt);
            updateTeamMovement(liveAway, awayX, awayY, awayBaseX, awayBaseY, teamShift, verticalPull, dt);

            if (dribbleTeam >= 0 && dribbleSlot >= 0 && ballTravel < 1f) {
                float[] xs = dribbleTeam == liveHome ? homeX : awayX;
                float[] ys = dribbleTeam == liveHome ? homeY : awayY;
                ballTargetX = xs[dribbleSlot];
                ballTargetY = ys[dribbleSlot];
            }

            if (ballTravel < 1f) {
                ballTravel = Math.min(1f, ballTravel + dt / ballTravelDuration);
                float eased = MatchMath.flightProgress(ballTravel);
                ballX = ballStartX + (ballTargetX - ballStartX) * eased;
                ballY = ballStartY + (ballTargetY - ballStartY) * eased;
                ballHeight = MatchMath.arc(ballTravel, ballPeakHeight);
            } else if (trackTarget && ballTargetTeam >= 0 && ballTargetSlot >= 0) {
                float follow=1f-(float)Math.exp(-14f*dt);
                ballX += (getPlayerX(ballTargetTeam, ballTargetSlot)-ballX)*follow;
                ballY += (getPlayerY(ballTargetTeam, ballTargetSlot)-ballY)*follow;
                ballHeight = 0f;
            } else {
                ballHeight = 0f;
            }
        }

        private void updateTeamMovement(int team, float[] xs, float[] ys, float[] baseX, float[] baseY,
                                        float horizontalBallShift, float verticalBallPull, float dt) {
            boolean hasBall = livePossessionTeam == team;
            float attackDir = team == liveHome ? 1f : -1f;
            float intent=teamIntent(team);
            float selectedShapeShift = intent * .045f * attackDir;
            if (team == selectedClub) {
                if ("High Line".equals(wibWobShape)) selectedShapeShift = 0.038f * attackDir;
                else if ("Deep Block".equals(wibWobShape)) selectedShapeShift = -0.048f * attackDir;
            }

            int opponent = team == liveHome ? liveAway : liveHome;
            float[] oppX = opponent == liveHome ? homeX : awayX;
            float[] oppY = opponent == liveHome ? homeY : awayY;
            int runnerSlot = activeRunnerSlot(opponent);
            int markerSlot = -1;
            if (!hasBall && runnerSlot > 0) markerSlot = closestOutfieldSlot(xs, ys, oppX[runnerSlot], oppY[runnerSlot], 5);

            float[] runX = team == liveHome ? homeRunX : awayRunX;
            float[] runY = team == liveHome ? homeRunY : awayRunY;
            float[] runTime = team == liveHome ? homeRunTime : awayRunTime;

            for (int i = 0; i < 11; i++) {
                float tx = baseX[i];
                float ty = baseY[i];

                if (i == 0) {
                    boolean threat = team == liveHome ? ballX < 0.38f : ballX > 0.62f;
                    boolean deepThreat = team == liveHome ? ballX < 0.22f : ballX > 0.78f;
                    tx = baseX[i] + attackDir * (deepThreat ? 0.072f : threat ? 0.040f : 0.012f);
                    ty = 0.50f + (ballY - 0.50f) * (deepThreat ? 0.42f : 0.25f);
                    if (liveLooseBall && ((team == liveHome && liveLooseBallX < 0.23f) || (team == liveAway && liveLooseBallX > 0.77f))) {
                        tx += (liveLooseBallX - tx) * 0.28f;
                        ty += (liveLooseBallY - ty) * 0.28f;
                    }
                } else {
                    tx += horizontalBallShift * (hasBall ? 0.90f : 0.56f) + selectedShapeShift;
                    ty += verticalBallPull * (hasBall ? 0.65f : 0.46f);

                    // Back four move as a line rather than independently drifting.
                    if (!hasBall && i >= 1 && (team==liveHome?baseX[i]<.30f:baseX[i]>.70f)) {
                        tx = baseX[i] + horizontalBallShift * 0.46f + selectedShapeShift;
                        if (team == selectedClub && offsideTrapInstruction) tx += attackDir * 0.028f;
                        ty += (ballY - ty) * 0.08f;
                    }

                    // One defender follows the most dangerous active runner while the others hold the line.
                    if (!hasBall && i == markerSlot && runnerSlot > 0) {
                        tx += (oppX[runnerSlot] - tx) * 0.48f;
                        ty += (oppY[runnerSlot] - ty) * 0.58f;
                    }

                    // Nearest midfield/forward player presses the ball; the rest stay compact.
                    if (!hasBall && team == selectedClub && pressingInstruction && i >= 5) {
                        float dx = ballX - xs[i], dy = ballY - ys[i];
                        float d = dx * dx + dy * dy;
                        if (d < 0.055f) { tx += dx * 0.28f; ty += dy * 0.28f; }
                    }
                    if(hasBall && i!=liveCarrierSlot && i>0 && runTime[i]<=0) {
                        float carrierX=getPlayerX(team,liveCarrierSlot),carrierY=getPlayerY(team,liveCarrierSlot);
                        if(Math.abs(xs[i]-carrierX)<.25f) {
                            float supportX=carrierX+attackDir*(i%2==0?.07f:-.10f);
                            float supportY=carrierY+(baseY[i]<carrierY?-.14f:.14f);
                            tx=tx*.72f+supportX*.28f;ty=ty*.75f+supportY*.25f;
                        }
                    }
                    if (!hasBall) ty += (0.5f - ty) * 0.045f;
                    if (team == selectedClub && "Central Overload".equals(wibWobShape)) ty += (0.5f - ty) * 0.18f;
                    if (team == selectedClub && "Wide".equals(wibWobShape)) ty += (ty < 0.5f ? -0.04f : 0.04f);
                }

                if (goalCelebrationTime > 0f && team == goalCelebrationTeam && i > 0 && (i==goalCelebrationScorerSlot || Math.hypot(xs[i]-xs[goalCelebrationScorerSlot],ys[i]-ys[goalCelebrationScorerSlot])<.36f)) {
                    float spreadX = ((i % 3) - 1) * 0.012f;
                    float spreadY = ((i % 4) - 1.5f) * 0.018f;
                    if (i == goalCelebrationScorerSlot) { spreadX = 0f; spreadY = 0f; }
                    tx = celebrationTargetX + spreadX;
                    ty = celebrationTargetY + spreadY;
                } else if (runTime[i] > 0f) {
                    tx = tx * 0.22f + runX[i] * 0.78f;
                    ty = ty * 0.22f + runY[i] * 0.78f;
                }
                if (team == dribbleTeam && i == dribbleSlot && ballTravel < 1f) {
                    tx = dribbleTargetX;
                    ty = dribbleTargetY;
                }
                tx = clamp(tx, 0.035f, 0.965f);
                ty = clamp(ty, 0.035f, 0.965f);

                float[] vx = team == liveHome ? homeVX : awayVX;
                float[] vy = team == liveHome ? homeVY : awayVY;
                boolean celebrating = goalCelebrationTime > 0f && team == goalCelebrationTeam && i > 0;
                Player athlete=playerForPitchSlot(team,i);
                float mobility=athlete==null?1:MatchMath.mobility(athlete.pace,athlete.fitness);
                float maxSpeed=(i==0?.085f:celebrating?.14f:runTime[i]>0?.145f:.105f)*mobility;
                // Keep separate support lanes, including small clearance from opponents.
                if(!celebrating && i>0) {
                    for(int j=1;j<11;j++) if(j!=i) {
                        float dx=xs[i]-xs[j],dy=(ys[i]-ys[j])*.648f;
                        float dist=(float)Math.hypot(dx,dy);
                        if(dist<.045f){if(dist<.0001f){dx=(i<j?-.001f:.001f);dist=.001f;}
                            float push=(.045f-dist)*.75f;tx+=dx/dist*push;ty+=dy/dist*push/.648f;}
                    }
                }
                MatchMotion.arrive(xs,ys,vx,vy,i,clamp(tx,.035f,.965f),clamp(ty,.035f,.965f),maxSpeed,.32f*mobility,dt);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            pitchArt.draw(canvas,w,h);

            float m = Math.max(dp(8), Math.min(w, h) * 0.025f);

            if (offsideFlashTime > 0f && offsideFlashX >= 0f) {
                float ox = offsideFlashX * w;
                canvas.drawLine(ox, m, ox, h - m, offsidePaint);
            }

            float radius = Math.max(dp(7), Math.min(w, h) * 0.022f);
            drawTeam(canvas, liveHome, homeX, homeY, radius, w, h);
            drawTeam(canvas, liveAway, awayX, awayY, radius, w, h);

            float bx = ballX * w;
            float groundY = ballY * h;
            float liftPx = ballHeight * h * 1.10f;
            float by = groundY - liftPx;
            float heightScale = 1f + Math.min(0.28f, ballHeight * 5.2f);
            float ballRadius = Math.max(dp(3), radius * 0.48f) * heightScale;
            float shadowScale = 1f - Math.min(0.42f, ballHeight * 7.0f);
            canvas.drawOval(new RectF(
                    bx - ballRadius * shadowScale,
                    groundY - ballRadius * 0.38f * shadowScale + dp(2),
                    bx + ballRadius * shadowScale,
                    groundY + ballRadius * 0.38f * shadowScale + dp(2)), shadowPaint);
            canvas.drawCircle(bx, by, ballRadius, ballPaint);
            canvas.drawCircle(bx, by, ballRadius, ballOutlinePaint);
        }

        private void drawTeam(Canvas canvas, int team, float[] xs, float[] ys, float radius, int w, int h) {
            for (int i = 0; i < 11; i++) {
                float px = xs[i] * w;
                float py = ys[i] * h;
                canvas.drawCircle(px + dp(1), py + dp(2), radius * 1.05f, shadowPaint);
                int c = i == 0 ? Color.rgb(238, 190, 45) : primaryColours[team];
                playerPaint.setColor(c);
                canvas.drawCircle(px, py, radius, playerPaint);
                canvas.drawCircle(px, py, radius, outlinePaint);

                String name = pitchName(team, i);
                float oldSize=labelPaint.getTextSize();
                labelPaint.setTextSize(dp(9));
                labelPaint.setColor(contrastText(c));
                canvas.drawText(String.valueOf(i+1),px,py+dp(3),labelPaint);
                labelPaint.setColor(Color.WHITE);labelPaint.setTextSize(oldSize);
                if (!name.isEmpty() && (i==liveCarrierSlot && team==livePossessionTeam || i==ballTargetSlot && team==ballTargetTeam)) canvas.drawText(name, px, py - radius - dp(3), labelPaint);
            }
        }

        private float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private int simulateGoals(int attackingTeam, int defendingTeam, boolean home) {
        int goals = random.nextInt(3);
        int difference = strength[attackingTeam] - strength[defendingTeam];
        if (difference >= 8 && random.nextDouble() < 0.55) goals++;
        if (difference >= 15 && random.nextDouble() < 0.35) goals++;
        if (difference <= -10 && goals > 0 && random.nextDouble() < 0.35) goals--;
        if (home && random.nextDouble() < 0.18) goals++;
        return Math.min(goals, 6);
    }

    private void updateTable(int home, int away, int homeGoals, int awayGoals) {
        played[home]++;
        played[away]++;
        goalsFor[home] += homeGoals;
        goalsAgainst[home] += awayGoals;
        goalsFor[away] += awayGoals;
        goalsAgainst[away] += homeGoals;

        if (homeGoals > awayGoals) {
            won[home]++;
            lost[away]++;
            points[home] += 3;
        } else if (awayGoals > homeGoals) {
            won[away]++;
            lost[home]++;
            points[away] += 3;
        } else {
            drawn[home]++;
            drawn[away]++;
            points[home]++;
            points[away]++;
        }
    }

    private void showLeagueTable() {
        backAction = () -> showDashboard();
        LinearLayout page = createPage("Liga Portugal", "Season 2026/27 • Matchday " + matchday, true);
        page.addView(makeButton("Qualification & relegation rules", v -> showQualificationGuide()));

        Integer[] order = new Integer[clubNames.length];
        for (int i = 0; i < clubNames.length; i++) order[i] = i;
        Arrays.sort(order, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                if (points[a] != points[b]) return Integer.compare(points[b], points[a]);
                int gdA = goalsFor[a] - goalsAgainst[a];
                int gdB = goalsFor[b] - goalsAgainst[b];
                if (gdA != gdB) return Integer.compare(gdB, gdA);
                return Integer.compare(goalsFor[b], goalsFor[a]);
            }
        });

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(10), dp(8), dp(10), dp(8));
        header.setBackground(buttonBackground(Color.rgb(35, 73, 110)));
        header.addView(makeTableHeadCell("#", 0.45f));
        header.addView(makeTableHeadCell("Club", 2.5f));
        header.addView(makeTableHeadCell("P", 0.55f));
        header.addView(makeTableHeadCell("W", 0.55f));
        header.addView(makeTableHeadCell("D", 0.55f));
        header.addView(makeTableHeadCell("L", 0.55f));
        header.addView(makeTableHeadCell("GD", 0.75f));
        header.addView(makeTableHeadCell("PTS", 0.8f));
        page.addView(header);

        for (int position = 0; position < order.length; position++) {
            int club = order[position];
            int gd = goalsFor[club] - goalsAgainst[club];
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(8), dp(10), dp(8));
            int rowColour = club == selectedClub ? Color.rgb(19, 86, 83) : (position % 2 == 0 ? Color.rgb(11, 48, 63) : Color.rgb(8, 39, 52));
            row.setBackground(buttonBackground(rowColour));
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.setMargins(0, 0, 0, dp(7));
            row.setLayoutParams(rlp);
            row.addView(makeLeagueCell(String.valueOf(position + 1), 0.45f, accent, true));
            row.addView(makeLeagueCell(clubNames[club], 2.5f, text, true));
            row.addView(makeLeagueCell(String.valueOf(played[club]), 0.55f, muted, false));
            row.addView(makeLeagueCell(String.valueOf(won[club]), 0.55f, muted, false));
            row.addView(makeLeagueCell(String.valueOf(drawn[club]), 0.55f, muted, false));
            row.addView(makeLeagueCell(String.valueOf(lost[club]), 0.55f, muted, false));
            row.addView(makeLeagueCell((gd > 0 ? "+" : "") + gd, 0.75f, gd >= 0 ? accent : danger, false));
            row.addView(makeLeagueCell(String.valueOf(points[club]), 0.8f, Color.WHITE, true));
            page.addView(row);
        }
    }

    private void showQualificationGuide() {
        backAction = () -> showLeagueTable();
        LinearLayout page = createPage("Qualification guide", "2026/27 finish → 2027/28 European entry", true);
        LinearLayout intro = makePanel();
        intro.addView(profileSectionTitle("PROVISIONAL ACCESS"));
        intro.addView(makeText("League places are a projection. Cup winners, UEFA titleholders, performance places and licensing can change the final admissions. This guide does not award places in an existing career.", 14, text));
        page.addView(intro);
        String[] countries = {"ENG", "ES", "IT", "DE", "FR", "PT", "NL", "BE", "SCO", "TR"};
        String[] names = {"England", "Spain", "Italy", "Germany", "France", "Portugal", "Netherlands", "Belgium", "Scotland", "Türkiye"};
        for (int i = 0; i < countries.length; i++) {
            EuropeanAccess.Profile access = EuropeanAccess.profile(countries[i]);
            LinearLayout card = makePanel();
            card.addView(profileSectionTitle(names[i].toUpperCase(Locale.UK)));
            for (int rank = 1; rank <= access.league.size(); rank++) {
                card.addView(makeText(rank + "  •  " + access.league.get(rank - 1).label(), 13, text));
            }
            card.addView(makeText("Domestic cup winner  •  " + access.cup.label(), 13, accent));
            if (access.conferenceFromLeagueCup) card.addView(makeText("League Cup winner  •  Conference League play-off", 13, accent));
            if (countries[i].equals("NL")) card.addView(makeText("The Conference berth is decided by domestic European play-offs, not awarded automatically to fourth place.", 12, muted));
            page.addView(card);
        }
        LinearLayout notes = makePanel();
        notes.addView(profileSectionTitle("HOW TO READ THE GUIDE"));
        notes.addView(makeText("Q1 / Q2 / Q3 = qualifying rounds. Play-off = final qualifying round. League phase = entry to the main competition. Cup runners-up do not inherit the winner's European place. Two European performance places depend on the current season's association results.", 13, text));
        notes.addView(makeText("Portugal: 17th and 18th go down; 16th enters the promotion/relegation play-off against the eligible third-placed second-division club. A second division and end-of-season admission system are still required to enact this in the career.", 13, muted));
        notes.addView(makeText("Source: UEFA circular 54/2026, 9 September 2026. Provisional 2027/28 access list; checked 29 September 2026.", 12, muted));
        page.addView(notes);
    }

    private TextView makeTableHeadCell(String value, float weight) {
        TextView tv = makeText(value, 11, Color.WHITE);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        return tv;
    }

    private TextView makeLeagueCell(String value, float weight, int colour, boolean bold) {
        TextView tv = makeText(value, 13, colour);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setSingleLine(true);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        return tv;
    }

    private void showAllTeamsPlayers() {
        backAction = () -> showDashboard();
        LinearLayout page = createPage("Player Database", "Browse players from every club", true);

        ArrayList<Player> leaders = new ArrayList<>(players);
        Collections.sort(leaders, (a, b) -> Integer.compare(b.overall, a.overall));
        TextView top = makeText(
                "REPUTATION WATCH\n" + leaders.get(0).name + "  " + knownOverallLabel(leaders.get(0)) + " • " + clubNames[leaders.get(0).team]
                        + "\n" + leaders.get(1).name + "  " + knownOverallLabel(leaders.get(1)) + " • " + clubNames[leaders.get(1).team]
                        + "\n" + leaders.get(2).name + "  " + knownOverallLabel(leaders.get(2)) + " • " + clubNames[leaders.get(2).team],
                15,
                text
        );
        top.setBackground(rounded(panel));
        top.setPadding(dp(14), dp(14), dp(14), dp(14));
        page.addView(top);
        page.addView(makeText("", 8, text));

        for (int team = 0; team < clubNames.length; team++) {
            final int t = team;
            Player best = bestPlayerForTeam(team);
            String suffix = best == null ? "" : " • Best " + knownOverallLabel(best);
            page.addView(makeButton(clubNames[team] + suffix, v -> showTeamPlayers(t)));
        }
    }

    private Player bestPlayerForTeam(int team) {
        Player best = null;
        for (Player p : players) {
            if (p.team == team && (best == null || p.overall > best.overall)) best = p;
        }
        return best;
    }

    private void showTeamPlayers(int team) {
        backAction = () -> {
            if (team == selectedClub) showDashboard();
            else showAllTeamsPlayers();
        };

        LinearLayout page = createPage(
                clubNames[team] + " Players",
                team == selectedClub ? "Your squad • tap a player for full stats" : "Opposition squad • tap a player for full stats",
                true
        );

        ArrayList<Player> teamPlayers = new ArrayList<>();
        for (Player p : players) if (p.team == team) teamPlayers.add(p);
        sortPlayersByPositionThenOverall(teamPlayers);

        for (Player p : teamPlayers) {
            String status = "";
            if (p.transferListed) status += "  [T]";
            if (p.loanListed) status += "  [L]";
            if (p.onLoan) status += "  [LOAN]";
            if (team == selectedClub) status += "  [" + tacticsStatusLabel(p.id) + "]";
            String label = p.position + "  " + p.name + "   " + knownOverallLabel(p) + status;
            page.addView(makeButton(label, v -> showPlayerProfile(p.id, team)));
        }

        if (team != selectedClub) page.addView(makeButton("Back to Player Database", v -> showAllTeamsPlayers()));
        else page.addView(makeButton("Back to Dashboard", v -> showDashboard()));
    }

    private int positionRank(String position) {
        switch (position) {
            case "GK": return 0;
            case "RB": return 1;
            case "CB": return 2;
            case "LB": return 3;
            case "DM": return 4;
            case "CM": return 5;
            case "AM": return 6;
            case "RM": return 7;
            case "LM": return 8;
            case "RW": return 9;
            case "LW": return 10;
            case "ST": return 11;
            default: return 12;
        }
    }

    private void sortPlayersByPositionThenOverall(ArrayList<Player> list) {
        Collections.sort(list, (a, b) -> {
            int ra = positionRank(a.position);
            int rb = positionRank(b.position);
            if (ra != rb) return Integer.compare(ra, rb);
            return Integer.compare(b.overall, a.overall);
        });
    }

    private void showPlayerProfile(int playerId, int returnTeam) {
        Player p = findPlayer(playerId);
        if (p == null) return;
        backAction = () -> showTeamPlayers(returnTeam);

        int knowledge = playerKnowledge(p);
        LinearLayout page = createPage(
                "Player Profile",
                clubNames[p.team] + " • " + p.name,
                true
        );

        LinearLayout hero = makePanel();
        hero.setBackground(rounded(Color.rgb(9, 31, 43)));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.TOP);

        View portrait = new RealisticHumanPortraitView(
                p.id,
                "PLAYER",
                primaryColours[p.team],
                secondaryColours[p.team],
                p.age,
                true
        );
        LinearLayout.LayoutParams portraitParams = new LinearLayout.LayoutParams(dp(112), dp(112));
        portraitParams.setMargins(0, 0, dp(16), 0);
        top.addView(portrait, portraitParams);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView role = makeText(p.position + "  •  " + clubNames[p.team], 12, accent);
        role.setTypeface(Typeface.DEFAULT_BOLD);
        role.setLetterSpacing(0.04f);
        info.addView(role);

        TextView playerName = makeText(p.name, 25, text);
        playerName.setTypeface(Typeface.DEFAULT_BOLD);
        playerName.setPadding(0, dp(3), 0, dp(5));
        info.addView(playerName);

        String status = p.injuredWeeks > 0
                ? "INJURED • " + p.injuredWeeks + " week(s)"
                : p.onLoan ? "ON LOAN"
                : p.squadRole;
        info.addView(makeText(
                "Age " + p.age + "  •  " + status + "\n"
                        + "Knowledge " + knowledge + "/4  •  "
                        + (shortlisted[p.id] ? "★ Shortlisted" : "Not shortlisted"),
                13,
                p.injuredWeeks > 0 ? warning : muted
        ));
        top.addView(info);
        hero.addView(top);

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.setPadding(0, dp(14), 0, 0);
        metrics.addView(profileMetricCard("OVERALL", knowledge >= 3 ? String.valueOf(p.overall) : "—", accent));
        metrics.addView(profileMetricCard("CONDITION", p.fitness + "%", conditionProfileColour(p.fitness)));
        metrics.addView(profileMetricCard("MORALE", p.morale + "%", moraleProfileColour(p.morale)));
        hero.addView(metrics);
        page.addView(hero);

        LinearLayout overview = makePanel();
        overview.addView(profileSectionTitle("CAREER & CONTRACT"));
        overview.addView(profileInfoRow("Market value", knownValueLabel(p)));
        overview.addView(profileInfoRow("Weekly wage", knowledge >= 2 || p.team == selectedClub ? "€" + p.weeklyWageK + "k" : "Unknown"));
        overview.addView(profileInfoRow("Contract remaining", p.contractYears + " year(s)"));
        overview.addView(profileInfoRow("Squad role", p.squadRole));
        overview.addView(profileInfoRow("Appearances", String.valueOf(p.appearances)));
        overview.addView(profileInfoRow("Goals / Assists", p.goals + " / " + p.assists));
        page.addView(overview);

        LinearLayout attributes = makePanel();
        attributes.addView(profileSectionTitle("PLAYER ATTRIBUTES • 1–20"));
        attributes.addView(makeModernAttributeRow("Technique", to20(p.technique), accent));
        attributes.addView(makeModernAttributeRow("Passing", to20(p.passing), accent));
        attributes.addView(makeModernAttributeRow("Finishing", to20(p.finishing), Color.rgb(225, 111, 62)));
        attributes.addView(makeModernAttributeRow("Pace", to20(p.pace), Color.rgb(71, 170, 215)));
        attributes.addView(makeModernAttributeRow("Physical", to20(p.physical), Color.rgb(183, 133, 226)));
        attributes.addView(makeModernAttributeRow("Defending", to20(p.defending), Color.rgb(86, 145, 224)));
        attributes.addView(makeModernAttributeRow("Consistency", Math.max(1, Math.min(20, p.consistency)), Color.rgb(231, 179, 70)));
        attributes.addView(makeModernAttributeRow("Big matches", Math.max(1, Math.min(20, p.importantMatches)), Color.rgb(231, 179, 70)));
        attributes.addView(makeModernAttributeRow("Temperament", Math.max(1, Math.min(20, p.temperament)), Color.rgb(231, 179, 70)));
        if(knowledge<3 && p.team!=selectedClub && attributeMasking) {
            attributes.removeAllViews();attributes.addView(profileSectionTitle("PLAYER ATTRIBUTES"));
            attributes.addView(makeText("Further scouting is needed to reveal numerical attributes.",14,muted));
        }
        page.addView(attributes);

        if (knowledge >= 3 || p.team == selectedClub || !attributeMasking) {
            LinearLayout assessment = makePanel();
            assessment.addView(profileSectionTitle(p.team == selectedClub ? "COACH ASSESSMENT" : "SCOUT ASSESSMENT"));
            assessment.addView(profileInfoRow("Current ability", classicAbilityBand(p.currentAbility)));
            assessment.addView(profileInfoRow("Potential", classicAbilityBand(p.potentialAbility)));
            assessment.addView(profileInfoRow("Professionalism", traitLabel(p.professionalism)));
            assessment.addView(profileInfoRow("Adaptability", traitLabel(p.adaptability)));
            assessment.addView(profileInfoRow("Pressure", traitLabel(p.pressure)));
            assessment.addView(profileInfoRow("Injury risk", inverseTraitLabel(p.injuryProneness)));
            TextView report = makeText("\n" + buildModernPlayerReport(p), 14, text);
            report.setLineSpacing(0f, 1.15f);
            assessment.addView(report);
            page.addView(assessment);
        } else {
            LinearLayout hidden = makePanel();
            hidden.addView(profileSectionTitle("SCOUTING REQUIRED"));
            hidden.addView(makeText("Your staff do not yet have enough knowledge to provide a full player assessment.", 14, muted));
            page.addView(hidden);
        }

        LinearLayout actions = makePanel();
        actions.addView(profileSectionTitle("ACTIONS"));
        actions.addView(makeButton(shortlisted[p.id] ? "★ Remove from Shortlist" : "☆ Add to Shortlist", v -> {
            shortlisted[p.id] = !shortlisted[p.id];
            addNews("SCOUT", p.name + (shortlisted[p.id] ? " added to shortlist" : " removed from shortlist"), clubNames[p.team]);
            saveCurrentGame();
            showPlayerProfile(p.id, returnTeam);
        }));

        if (p.team != selectedClub && playerKnowledge(p) < 4) {
            actions.addView(makeAccentButton("🔎  Assign Scout", v -> assignScout(p)));
        }

        actions.addView(makeButton(comparePlayerId < 0 ? "⇄  Start Player Comparison" : "⇄  Compare with selected player", v -> {
            if (comparePlayerId < 0 || comparePlayerId == p.id) {
                comparePlayerId = p.id;
                Toast.makeText(this, "Comparison player selected. Open another player.", Toast.LENGTH_SHORT).show();
            } else {
                Player first = findPlayer(comparePlayerId);
                comparePlayerId = -1;
                if (first != null) showPlayerComparison(first, p);
            }
        }));

        actions.addView(makeButton("📝  Manager Note" + (managerNotes[p.id] == null || managerNotes[p.id].isEmpty() ? "" : " • saved"), v -> editManagerNote(p)));

        if (p.team == selectedClub) {
            actions.addView(makeButton(p.transferListed ? "Remove from Transfer List" : "Add to Transfer List", v -> {
                p.transferListed = !p.transferListed;
                if (p.transferListed) p.loanListed = false;
                saveCurrentGame();
                showPlayerProfile(p.id, returnTeam);
            }));
            actions.addView(makeButton(p.loanListed ? "Remove from Loan List" : "Add to Loan List", v -> {
                p.loanListed = !p.loanListed;
                if (p.loanListed) p.transferListed = false;
                saveCurrentGame();
                showPlayerProfile(p.id, returnTeam);
            }));
            actions.addView(makeButton("⚙  Individual Instructions", v -> showPlayerInstructions(p, returnTeam)));
        } else if (isTransferWindowOpen(currentDate)) {
            actions.addView(makeAccentButton("💰  Make Transfer Offer", v -> showTransferOfferDialog(p, false)));
            actions.addView(makeButton("🤝  Make Loan Offer", v -> showTransferOfferDialog(p, true)));
        } else {
            TextView closed = makeText("Transfer registrations are currently closed.", 13, muted);
            closed.setPadding(0, dp(8), 0, 0);
            actions.addView(closed);
        }
        page.addView(actions);

        page.addView(makeButton("← Back to " + clubNames[returnTeam], v -> showTeamPlayers(returnTeam)));
    }

    private TextView profileSectionTitle(String title) {
        TextView tv = makeText(title, 12, accent);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setLetterSpacing(0.05f);
        tv.setPadding(0, 0, 0, dp(8));
        return tv;
    }

    private LinearLayout profileMetricCard(String label, String value, int highlight) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(6), dp(8), dp(6), dp(8));
        card.setBackground(rounded(Color.rgb(17, 47, 60)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        card.setLayoutParams(lp);
        TextView v = makeText(value, 19, highlight);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setGravity(Gravity.CENTER);
        card.addView(v);
        TextView l = makeText(label, 9, muted);
        l.setTypeface(Typeface.DEFAULT_BOLD);
        l.setGravity(Gravity.CENTER);
        l.setLetterSpacing(0.05f);
        card.addView(l);
        return card;
    }

    private LinearLayout profileInfoRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));
        TextView left = makeText(label, 13, muted);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(left);
        TextView right = makeText(value, 13, text);
        right.setTypeface(Typeface.DEFAULT_BOLD);
        right.setGravity(Gravity.END);
        row.addView(right);
        return row;
    }

    private LinearLayout makeModernAttributeRow(String label, int value, int barColour) {
        int v = Math.max(1, Math.min(20, value));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, dp(5), 0, dp(6));

        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.HORIZONTAL);
        TextView name = makeText(label, 13, text);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        title.addView(name);
        TextView number = makeText(String.valueOf(v), 13, v >= 15 ? accent : v >= 10 ? text : warning);
        number.setTypeface(Typeface.DEFAULT_BOLD);
        title.addView(number);
        wrap.addView(title);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(0, dp(5), 0, 0);
        int filled = Math.round(v / 2f);
        for (int i = 0; i < 10; i++) {
            View block = new View(this);
            block.setBackgroundColor(i < filled ? barColour : Color.rgb(42, 63, 72));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(5), 1f);
            bp.setMargins(i == 0 ? 0 : dp(1), 0, 0, 0);
            bar.addView(block, bp);
        }
        wrap.addView(bar);
        return wrap;
    }

    private int conditionProfileColour(int condition) {
        return condition >= 90 ? accent : condition >= 75 ? warning : danger;
    }

    private int moraleProfileColour(int morale) {
        return morale >= 85 ? accent : morale >= 65 ? Color.rgb(87, 166, 221) : warning;
    }

    private String buildModernPlayerReport(Player p) {
        String strengthText;
        int best = Math.max(Math.max(p.technique, p.passing), Math.max(p.pace, Math.max(p.finishing, p.defending)));
        if (best == p.finishing) strengthText = "finishing and attacking threat";
        else if (best == p.defending) strengthText = "defensive reading and ball recovery";
        else if (best == p.passing) strengthText = "distribution and passing range";
        else if (best == p.pace) strengthText = "pace and movement";
        else strengthText = "technique and close control";

        String development = p.currentAbility + 10 < p.potentialAbility
                ? "There is meaningful development room remaining."
                : "He is already close to his current projected ceiling.";
        return "Profile: strongest in " + strengthText + ". " + development
                + " Professionalism is " + traitLabel(p.professionalism).toLowerCase(Locale.UK)
                + " and his injury risk is " + inverseTraitLabel(p.injuryProneness).toLowerCase(Locale.UK) + ".";
    }

    private class RealisticHumanPortraitView extends ImageView {
        RealisticHumanPortraitView(int seed,String role,int primary,int secondary,int ageHint,boolean footballKit) {
            super(MainActivity.this);
            setScaleType(ImageView.ScaleType.FIT_CENTER);
            setBackground(rounded(panelLight));
            setContentDescription("Fictional " + (footballKit ? "player" : "staff") + " portrait");
            setClipToOutline(true);
            if(portraits!=null) portraits.load(seed,ageHint,footballKit,"MANAGER".equals(role) && "Female".equals(managerGender),bitmap->{
                if(bitmap!=null) setImageBitmap(bitmap);
                else setImageResource(R.drawable.boss_xi_logo);
            });
        }
    }

    private class ClassicPortraitView extends View {
        private final Player player;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        ClassicPortraitView(Player p) {
            super(MainActivity.this);
            this.player = p;
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();

            int shirt = primaryColours[player.team];
            int trim = secondaryColours[player.team];
            int[] skinTones = {
                    Color.rgb(239, 204, 174), Color.rgb(218, 176, 137),
                    Color.rgb(185, 132, 94), Color.rgb(128, 86, 58)
            };
            int skin = skinTones[Math.abs(player.id * 7 + player.age) % skinTones.length];
            int hair = Color.rgb(34, 30, 28);
            if (player.id % 6 == 0) hair = Color.rgb(87, 58, 34);
            if (player.id % 9 == 0) hair = Color.rgb(175, 126, 66);
            if (player.id % 13 == 0) hair = Color.rgb(22, 22, 22);

            paint.setShader(new android.graphics.LinearGradient(0, 0, 0, h,
                    Color.rgb(52, 68, 76), Color.rgb(18, 29, 35), android.graphics.Shader.TileMode.CLAMP));
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, dp(10), dp(10), paint);
            paint.setShader(null);

            paint.setColor(Color.argb(55, 255, 255, 255));
            canvas.drawCircle(w * .22f, h * .18f, dp(22), paint);

            float cx = w * .50f;
            float cy = h * .37f;
            float rx = w * (.175f + (player.id % 3) * .008f);
            float ry = h * (.165f + (player.id % 4) * .004f);

            // Jersey and shoulders.
            paint.setShader(new android.graphics.LinearGradient(0, h*.64f, 0, h,
                    lighten(shirt, 18), darken(shirt, 24), android.graphics.Shader.TileMode.CLAMP));
            rect.set(w*.12f, h*.64f, w*.88f, h*.99f);
            canvas.drawRoundRect(rect, dp(18), dp(18), paint);
            paint.setShader(null);
            paint.setColor(trim);
            canvas.drawRect(w*.465f, h*.66f, w*.535f, h*.99f, paint);
            paint.setColor(Color.argb(120,255,255,255));
            canvas.drawRect(w*.18f,h*.70f,w*.34f,h*.725f,paint);

            // Neck.
            paint.setShader(new android.graphics.LinearGradient(w*.44f,0,w*.56f,0,
                    darken(skin,28), lighten(skin,10), android.graphics.Shader.TileMode.CLAMP));
            rect.set(w*.43f,h*.53f,w*.57f,h*.69f);
            canvas.drawRoundRect(rect,dp(5),dp(5),paint);
            paint.setShader(null);

            // Ears.
            paint.setColor(darken(skin,6));
            canvas.drawOval(cx-rx-dp(4), cy-dp(9), cx-rx+dp(4), cy+dp(10), paint);
            canvas.drawOval(cx+rx-dp(4), cy-dp(9), cx+rx+dp(4), cy+dp(10), paint);

            // Face with directional lighting to feel more photographic.
            paint.setShader(new android.graphics.RadialGradient(
                    cx-rx*.38f, cy-ry*.34f, ry*1.6f,
                    lighten(skin, 24), darken(skin, 32), android.graphics.Shader.TileMode.CLAMP));
            rect.set(cx-rx, cy-ry, cx+rx, cy+ry);
            canvas.drawOval(rect, paint);
            paint.setShader(null);

            // Cheek / temple shadow.
            paint.setColor(Color.argb(34, 45, 24, 18));
            canvas.drawOval(cx+rx*.20f, cy-ry*.15f, cx+rx*.88f, cy+ry*.63f, paint);

            // Hairline with deterministic styles.
            paint.setColor(hair);
            int hairStyle = Math.abs(player.id + player.team) % 5;
            if (hairStyle == 0) {
                rect.set(cx-rx*1.02f, cy-ry*1.05f, cx+rx*1.02f, cy-ry*.12f);
                canvas.drawArc(rect,180,180,true,paint);
            } else if (hairStyle == 1) {
                rect.set(cx-rx*.95f, cy-ry*1.07f, cx+rx*.78f, cy-ry*.10f);
                canvas.drawArc(rect,180,188,true,paint);
                canvas.drawRect(cx-rx*.9f,cy-ry*.65f,cx-rx*.66f,cy-ry*.15f,paint);
            } else if (hairStyle == 2) {
                for (int i=-3;i<=3;i++) canvas.drawCircle(cx+i*dp(4),cy-ry*.88f-dp(Math.abs(i)%2*2),dp(5),paint);
            } else if (hairStyle == 3) {
                rect.set(cx-rx*.58f,cy-ry*1.04f,cx+rx*.58f,cy-ry*.68f);
                canvas.drawRoundRect(rect,dp(7),dp(7),paint);
            }

            // Brows.
            paint.setColor(darken(hair, 5));
            paint.setStrokeWidth(dp(2.1f));
            float eyeY=cy-ry*.08f, eyeDx=rx*.42f;
            canvas.drawLine(cx-eyeDx-dp(6),eyeY-dp(6),cx-eyeDx+dp(5),eyeY-dp(7),paint);
            canvas.drawLine(cx+eyeDx-dp(5),eyeY-dp(7),cx+eyeDx+dp(6),eyeY-dp(6),paint);

            // Eyes with sclera, iris and catchlight.
            paint.setColor(Color.rgb(238,236,228));
            canvas.drawOval(cx-eyeDx-dp(6),eyeY-dp(2),cx-eyeDx+dp(5),eyeY+dp(5),paint);
            canvas.drawOval(cx+eyeDx-dp(5),eyeY-dp(2),cx+eyeDx+dp(6),eyeY+dp(5),paint);
            int iris = (player.id % 4 == 0) ? Color.rgb(74,93,82) : (player.id % 4 == 1 ? Color.rgb(78,58,42) : Color.rgb(54,67,74));
            paint.setColor(iris);
            canvas.drawCircle(cx-eyeDx,eyeY+dp(1),dp(2.4f),paint);
            canvas.drawCircle(cx+eyeDx,eyeY+dp(1),dp(2.4f),paint);
            paint.setColor(Color.BLACK);
            canvas.drawCircle(cx-eyeDx,eyeY+dp(1),dp(1.1f),paint);
            canvas.drawCircle(cx+eyeDx,eyeY+dp(1),dp(1.1f),paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx-eyeDx-dp(.7f),eyeY,dp(.7f),paint);
            canvas.drawCircle(cx+eyeDx-dp(.7f),eyeY,dp(.7f),paint);

            // Nose with highlight and shadow.
            paint.setColor(darken(skin,34));
            paint.setStrokeWidth(dp(1.2f));
            canvas.drawLine(cx+dp(1),cy-dp(2),cx-dp(2),cy+dp(12),paint);
            canvas.drawLine(cx-dp(2),cy+dp(12),cx+dp(4),cy+dp(12),paint);
            paint.setColor(Color.argb(70,255,255,255));
            canvas.drawLine(cx-dp(1),cy, cx-dp(2),cy+dp(8),paint);

            // Mouth / lips.
            paint.setColor(Color.rgb(124,68,66));
            paint.setStrokeWidth(dp(1.5f));
            canvas.drawLine(cx-dp(9),cy+dp(20),cx+dp(9),cy+dp(20),paint);
            paint.setColor(Color.argb(55,255,255,255));
            canvas.drawLine(cx-dp(5),cy+dp(19),cx+dp(4),cy+dp(19),paint);

            // Beard / stubble variants.
            if (player.id % 3 == 0 || player.age >= 29) {
                paint.setColor(Color.argb(player.id % 3 == 0 ? 48 : 30, 25, 22, 20));
                rect.set(cx-rx*.72f, cy+ry*.20f, cx+rx*.72f, cy+ry*.95f);
                canvas.drawArc(rect, 8, 164, true, paint);
            }

            // Subtle frame and bottom metadata strip.
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.argb(155,255,255,255));
            rect.set(dp(2),dp(2),w-dp(2),h-dp(2));
            canvas.drawRoundRect(rect,dp(9),dp(9),paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(220,10,18,24));
            rect.set(dp(5),h-dp(29),w-dp(5),h-dp(5));
            canvas.drawRoundRect(rect,dp(5),dp(5),paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(dp(9));
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(player.position + "   AGE " + player.age, dp(11), h-dp(13), paint);
        }
    }

    private int playerKnowledge(Player p) {
        return 4;
    }

    private String knownOverallLabel(Player p) {
        int k = playerKnowledge(p);
        if (k >= 3) return "OVR " + p.overall;
        if (k == 2) return "OVR ~" + (Math.round(p.overall / 5f) * 5);
        if (k == 1) return p.overall >= 82 ? "High reputation" : p.overall >= 72 ? "Established" : "Unproven";
        return "Unscouted";
    }

    private String knownValueLabel(Player p) {
        int k = playerKnowledge(p);
        if (k >= 2) return "€" + p.valueMillions + "m";
        if (k == 1) return p.valueMillions >= 15 ? "High" : p.valueMillions >= 6 ? "Medium" : "Low";
        return "Unknown";
    }

    private int to20(int value) {
        return Math.max(1, Math.min(20, Math.round(value / 5f)));
    }

    private String classicAttributeLine(String name, int internalValue, int knowledge, int revealAt) {
        return String.format(Locale.UK, "%-13s  %2d", name, to20(internalValue));
    }

    private String classicAbilityBand(int ability) {
        if (ability >= 170) return "Elite";
        if (ability >= 145) return "Top division";
        if (ability >= 120) return "Strong professional";
        if (ability >= 95) return "Developing";
        return "Raw";
    }

    private String traitLabel(int value) {
        if (value >= 17) return "Excellent";
        if (value >= 13) return "Good";
        if (value >= 9) return "Average";
        if (value >= 5) return "Questionable";
        return "Poor";
    }

    private String inverseTraitLabel(int value) {
        if (value <= 4) return "Very low";
        if (value <= 8) return "Low";
        if (value <= 13) return "Average";
        if (value <= 17) return "High";
        return "Very high";
    }

    private void showPlayerComparison(Player a, Player b) {
        String message =
                String.format(Locale.UK, "%-13s %8s %8s\n", "", shortName(a.name), shortName(b.name))
                        + String.format(Locale.UK, "%-13s %8d %8d\n", "OVR", a.overall, b.overall)
                        + String.format(Locale.UK, "%-13s %8d %8d\n", "Pace", to20(a.pace), to20(b.pace))
                        + String.format(Locale.UK, "%-13s %8d %8d\n", "Technique", to20(a.technique), to20(b.technique))
                        + String.format(Locale.UK, "%-13s %8d %8d\n", "Passing", to20(a.passing), to20(b.passing))
                        + String.format(Locale.UK, "%-13s %8d %8d\n", "Finishing", to20(a.finishing), to20(b.finishing))
                        + String.format(Locale.UK, "%-13s %8d %8d\n", "Defending", to20(a.defending), to20(b.defending))
                        + String.format(Locale.UK, "%-13s %8d %8d\n", "Physical", to20(a.physical), to20(b.physical))
                        + String.format(Locale.UK, "%-13s %8s %8s", "Potential", classicAbilityBand(a.potentialAbility), classicAbilityBand(b.potentialAbility));
        new BossDialog.Builder(this)
                .setTitle("Player Comparison")
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show();
    }

    private String shortName(String name) {
        if (name == null) return "Player";
        if (name.length() <= 8) return name;
        return name.substring(0, 8);
    }

    private void editManagerNote(Player p) {
        EditText input = new EditText(this);
        input.setText(managerNotes[p.id] == null ? "" : managerNotes[p.id]);
        input.setHint("Example: Scout again in January");
        new BossDialog.Builder(this)
                .setTitle("Manager note — " + p.name)
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String note = input.getText().toString().replace("\u001E", " ").replace("\u001F", " ");
                    managerNotes[p.id] = note;
                    saveCurrentGame();
                })
                .show();
    }

    private void showPlayerInstructions(Player p, int returnTeam) {
        String[] labels = {
                (p.freeRole ? "✓ " : "") + "Free Role",
                (p.forwardRuns ? "✓ " : "") + "Forward Runs",
                (p.runWithBall ? "✓ " : "") + "Run With Ball",
                (p.longShots ? "✓ " : "") + "Long Shots"
        };
        new BossDialog.Builder(this)
                .setTitle("Individual Instructions — " + p.name)
                .setItems(labels, (d, which) -> {
                    if (which == 0) p.freeRole = !p.freeRole;
                    if (which == 1) p.forwardRuns = !p.forwardRuns;
                    if (which == 2) p.runWithBall = !p.runWithBall;
                    if (which == 3) p.longShots = !p.longShots;
                    saveCurrentGame();
                    showPlayerProfile(p.id, returnTeam);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showTransferOfferDialog(Player p, boolean loan) {
        if (!isTransferWindowOpen(currentDate)) {
            Toast.makeText(this, "The transfer window is closed.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (loan) {
            final int[] fees = {0, 1, 2};
            String[] labels = {"No loan fee", "€1m loan fee", "€2m loan fee"};
            new BossDialog.Builder(this)
                    .setTitle("Loan offer for " + p.name)
                    .setItems(labels, (dialog, which) -> resolveLoanOffer(p, fees[which]))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            int base = Math.max(1, p.valueMillions);
            final int[] offers = {
                    Math.max(1, (int)Math.round(base * 0.8)),
                    base,
                    Math.max(base + 1, (int)Math.round(base * 1.2)),
                    Math.max(base + 2, (int)Math.round(base * 1.5))
            };
            String[] labels = {
                    "€" + offers[0] + "m  •  opening offer",
                    "€" + offers[1] + "m  •  market value",
                    "€" + offers[2] + "m  •  strong offer",
                    "€" + offers[3] + "m  •  premium offer"
            };
            new BossDialog.Builder(this)
                    .setTitle("Transfer offer for " + p.name)
                    .setMessage("Club: " + clubNames[p.team] + "\nValue: €" + p.valueMillions + "m\nYour budget: €" + currentTransferBudget + "m")
                    .setItems(labels, (dialog, which) -> resolveTransferOffer(p, offers[which]))
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void resolveTransferOffer(Player p, int offer) {
        if (offer > currentTransferBudget) {
            Toast.makeText(this, "You do not have enough transfer budget.", Toast.LENGTH_LONG).show();
            return;
        }

        double ratio = offer / (double)Math.max(1, p.valueMillions);
        double chance = 0.10;
        if (p.transferListed) chance += 0.34;
        if (ratio >= 1.0) chance += 0.26;
        if (ratio >= 1.2) chance += 0.22;
        if (ratio >= 1.5) chance += 0.15;
        if (p.contractYears <= 1) chance += 0.10;
        chance = Math.min(0.95, chance);

        if (random.nextDouble() < chance) {
            int oldTeam = p.team;
            showContractOfferDialog(p, offer, oldTeam);
        } else {
            addNews("TRANSFER", "Offer rejected for " + p.name, clubNames[p.team] + " rejected €" + offer + "m.");
            new BossDialog.Builder(this)
                    .setTitle("Offer rejected")
                    .setMessage(clubNames[p.team] + " rejected the €" + offer + "m offer for " + p.name + ".")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void showContractOfferDialog(Player p, int fee, int oldTeam) {
        int demand = Math.max(8, p.weeklyWageK);
        int[] wages = {
                Math.max(5, (int)Math.round(demand * 0.85)),
                demand,
                (int)Math.round(demand * 1.20),
                (int)Math.round(demand * 1.40)
        };
        int[] years = {3, 4, 4, 5};
        String[] roles = {"Rotation", "Squad Player", "Important Player", "Star Player"};
        String[] labels = new String[4];
        for (int i = 0; i < 4; i++) labels[i] = roles[i] + " • €" + wages[i] + "k/w • " + years[i] + " years";

        new BossDialog.Builder(this)
                .setTitle("Contract talks — " + p.name)
                .setMessage("Transfer fee agreed: €" + fee + "m\nEstimated wage demand: €" + demand + "k/w\nChoose a contract package.")
                .setItems(labels, (d, which) -> resolveContractOffer(p, fee, oldTeam, wages[which], years[which], roles[which]))
                .setNegativeButton("Walk away", null)
                .show();
    }

    private void resolveContractOffer(Player p, int fee, int oldTeam, int wageK, int years, String role) {
        double wageRatio = wageK / (double)Math.max(1, p.weeklyWageK);
        double chance = 0.24 + Math.min(0.44, wageRatio * 0.24);
        if ("Important Player".equals(role)) chance += 0.10;
        if ("Star Player".equals(role)) chance += 0.16;
        chance += (managerReputation - 50) / 250.0;
        chance -= Math.max(0, p.ambition - 14) * 0.018;
        chance += Math.max(0, p.professionalism - 12) * 0.008;
        chance = Math.max(0.12, Math.min(0.94, chance));

        if (random.nextDouble() < chance) {
            currentTransferBudget -= fee;
            recordTransferPurchase(fee);
            p.team = selectedClub;
            p.contractYears = years;
            p.weeklyWageK = wageK;
            p.squadRole = role;
            p.transferListed = false;
            p.loanListed = false;
            p.onLoan = false;
            p.morale = Math.min(100, p.morale + 8);
            playerRoleStatus[p.id] = 0;
            scoutKnowledge[p.id] = 4;
            addNews("TRANSFER", p.name + " signs for " + clubNames[selectedClub], "Fee €" + fee + "m • " + role + " • €" + wageK + "k/w");
            saveCurrentGame();
            new BossDialog.Builder(this)
                    .setTitle("Signing complete")
                    .setMessage(p.name + " has agreed a " + years + "-year contract and joins from " + clubNames[oldTeam] + ".")
                    .setPositiveButton("View player", (d, w) -> showPlayerProfile(p.id, selectedClub))
                    .show();
        } else {
            addNews("CONTRACT", p.name + " rejects contract", "The transfer fee was agreed but personal terms could not be reached.");
            new BossDialog.Builder(this)
                    .setTitle("Contract rejected")
                    .setMessage(p.name + " rejected the proposed personal terms. The transfer has collapsed.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void resolveLoanOffer(Player p, int fee) {
        if (fee > currentTransferBudget) {
            Toast.makeText(this, "You do not have enough transfer budget.", Toast.LENGTH_LONG).show();
            return;
        }

        double chance = 0.24 + fee * 0.12;
        if (p.loanListed) chance += 0.42;
        if (p.age <= 22) chance += 0.12;
        chance = Math.min(0.92, chance);

        if (random.nextDouble() < chance) {
            int oldTeam = p.team;
            currentTransferBudget -= fee;
            recordTransferLoanFee(fee);
            p.team = selectedClub;
            p.transferListed = false;
            p.loanListed = false;
            p.onLoan = true;
            playerRoleStatus[p.id] = 0;
            saveCurrentGame();
            new BossDialog.Builder(this)
                    .setTitle("Loan accepted")
                    .setMessage(clubNames[oldTeam] + " accepted your loan proposal for " + p.name + ".")
                    .setPositiveButton("View player", (d, w) -> showPlayerProfile(p.id, selectedClub))
                    .show();
        } else {
            new BossDialog.Builder(this)
                    .setTitle("Loan rejected")
                    .setMessage(clubNames[p.team] + " rejected your loan offer for " + p.name + ".")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private String attributeLine(String name, int value) {
        return String.format(Locale.UK, "%-11s %2d   %s", name, value, statBar(value));
    }

    private String statBar(int value) {
        int blocks = Math.max(1, Math.min(10, value / 10));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(i < blocks ? "■" : "□");
        return sb.toString();
    }

    private void showTransferHub() {
        backAction = () -> showDashboard();
        LinearLayout page = createPage("Transfers", currentDate.format(DATE_FORMAT) + " • " + transferWindowStatus(currentDate), true);

        LinearLayout windows = makePanel();
        windows.addView(makeText("AVAILABLE BUDGET  •  €" + currentTransferBudget + "m", 17, accent));
        TextView title = makeText("PORTUGAL 2026/27 REGISTRATION WINDOWS", 13, accent);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        windows.addView(title);
        windows.addView(makeText(
                "Summer: 1 Jul 2026 – 4 Sep 2026\nWinter: 4 Jan 2027 – 1 Feb 2027\n\n"
                        + (isTransferWindowOpen(currentDate)
                        ? "Status: OPEN — registrations permitted"
                        : "Status: CLOSED — you can still prepare transfer/loan lists"),
                15,
                text
        ));
        page.addView(windows);

        page.addView(makeButton("★  Shortlist & Scouting", v -> showScoutCentre()));
        page.addView(makeButton("🌍  Search Player Database", v -> showAllTeamsPlayers()));

        ArrayList<Player> transferList = new ArrayList<>();
        ArrayList<Player> loanList = new ArrayList<>();
        for (Player p : players) {
            if (p.team == selectedClub && p.transferListed) transferList.add(p);
            if (p.team == selectedClub && p.loanListed) loanList.add(p);
        }

        TextView transferTitle = makeText("TRANSFER LIST (" + transferList.size() + ")", 13, muted);
        transferTitle.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(transferTitle);
        if (transferList.isEmpty()) page.addView(makeText("No players currently transfer-listed.", 15, muted));
        for (Player p : transferList) page.addView(makeButton(p.name + " • OVR " + p.overall, v -> showPlayerProfile(p.id, selectedClub)));

        TextView loanTitle = makeText("\nLOAN LIST (" + loanList.size() + ")", 13, muted);
        loanTitle.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(loanTitle);
        if (loanList.isEmpty()) page.addView(makeText("No players currently loan-listed.", 15, muted));
        for (Player p : loanList) page.addView(makeButton(p.name + " • OVR " + p.overall, v -> showPlayerProfile(p.id, selectedClub)));

        page.addView(makeText("", 6, text));
        page.addView(makeButton("Browse Your Squad", v -> showTeamPlayers(selectedClub)));
        page.addView(makeButton("Browse All Teams", v -> showAllTeamsPlayers()));
    }

    private String transferWindowStatus(LocalDate date) {
        for (RegistrationWindow window : RegistrationWindow.forCountry("PT")) {
            if (window.includes(date)) return "OPEN • Closes " + window.closes.format(DATE_FORMAT);
            if (date.isBefore(window.opens)) return "CLOSED • Opens " + window.opens.format(DATE_FORMAT);
        }
        return "CLOSED • Next season's dates not yet confirmed";
    }

    private boolean isTransferWindowOpen(LocalDate date) {
        for (RegistrationWindow window : RegistrationWindow.forCountry("PT"))
            if (window.includes(date)) return true;
        return false;
    }

    private void showStaffHub() {
        backAction = () -> showDashboard();
        LinearLayout page = createPage("Staff", "First-team football department", true);

        LinearLayout intro = makePanel();
        intro.addView(profileSectionTitle("FOOTBALL DEPARTMENT"));
        intro.addView(makeText(
                "Your senior staff influence match preparation, training quality, player development and recruitment knowledge.",
                14,
                text
        ));
        page.addView(intro);

        page.addView(staffCard("Assistant Manager", assistantManagerName, assistantManagerRating,
                "Tactical preparation, motivation and match feedback.", () -> hireStaff("Assistant Manager")));
        page.addView(staffCard("Head Coach", headCoachName, headCoachRating,
                "Training quality, technical coaching and player development.", () -> hireStaff("Head Coach")));
        page.addView(staffCard("Chief Scout", chiefScoutName, chiefScoutRating,
                "Player identification, potential judgement and recruitment knowledge.", () -> hireStaff("Chief Scout")));
    }

    private LinearLayout staffCard(String role, String name, int rating, String desc, Runnable action) {
        LinearLayout box = makePanel();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        View portrait = new RealisticHumanPortraitView(
                name.hashCode() + role.hashCode(),
                role,
                primaryColours[selectedClub],
                secondaryColours[selectedClub],
                38 + Math.abs(name.hashCode() % 19),
                false
        );
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(80), dp(80));
        pp.setMargins(0, 0, dp(13), 0);
        top.addView(portrait, pp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView roleText = makeText(role.toUpperCase(Locale.UK), 11, accent);
        roleText.setTypeface(Typeface.DEFAULT_BOLD);
        roleText.setLetterSpacing(0.04f);
        info.addView(roleText);
        TextView nameText = makeText(name, 19, text);
        nameText.setTypeface(Typeface.DEFAULT_BOLD);
        info.addView(nameText);
        info.addView(makeText("Staff rating  " + rating + "/100", 13, rating >= 75 ? accent : text));
        TextView descView = makeText(desc, 12, muted);
        descView.setPadding(0, dp(5), 0, 0);
        info.addView(descView);
        top.addView(info);
        box.addView(top);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, dp(10), 0, 0);
        Button view = makeAccentButton("View Profile", v -> showStaffProfile(role));
        Button replace = makeButton("Hire / Replace", v -> action.run());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bp.setMargins(0, 0, dp(4), 0);
        buttons.addView(view, bp);
        LinearLayout.LayoutParams bp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bp2.setMargins(dp(4), 0, 0, 0);
        buttons.addView(replace, bp2);
        box.addView(buttons);
        return box;
    }

    private void showStaffProfile(String role) {
        backAction = () -> showStaffHub();
        String name = profileStaffName(role);
        int rating = profileStaffRating(role);
        int age = 38 + Math.abs(name.hashCode() % 19);
        LinearLayout page = createPage(role + " Profile", clubNames[selectedClub] + " • " + name, true);

        LinearLayout hero = makePanel();
        hero.setBackground(rounded(Color.rgb(9, 31, 43)));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.TOP);

        View portrait = new RealisticHumanPortraitView(
                name.hashCode() + role.hashCode(), role,
                primaryColours[selectedClub], secondaryColours[selectedClub], age, false
        );
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(112), dp(112));
        pp.setMargins(0, 0, dp(16), 0);
        top.addView(portrait, pp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView roleText = makeText(role.toUpperCase(Locale.UK), 12, accent);
        roleText.setTypeface(Typeface.DEFAULT_BOLD);
        info.addView(roleText);
        TextView staffName = makeText(name, 24, text);
        staffName.setTypeface(Typeface.DEFAULT_BOLD);
        staffName.setPadding(0, dp(3), 0, dp(6));
        info.addView(staffName);
        info.addView(makeText("Portugal  •  Age " + age + "\n" + clubNames[selectedClub], 13, muted));
        top.addView(info);
        hero.addView(top);

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.setPadding(0, dp(14), 0, 0);
        metrics.addView(profileMetricCard("RATING", rating + "/100", rating >= 75 ? accent : text));
        metrics.addView(profileMetricCard("CONTRACT", "Rolling", Color.rgb(87, 166, 221)));
        metrics.addView(profileMetricCard("WAGE", "€" + profileStaffWeeklyWageK(rating) + "k", Color.rgb(231, 179, 70)));
        hero.addView(metrics);
        page.addView(hero);

        LinearLayout abilities = makePanel();
        abilities.addView(profileSectionTitle("KEY ATTRIBUTES • 1–20"));
        String[] labels = profileStaffAttributeLabels(role);
        for (int i = 0; i < labels.length; i++) {
            int value = profileStaffAttribute(rating, role, i);
            int colour = i % 3 == 0 ? accent : i % 3 == 1 ? Color.rgb(87, 166, 221) : Color.rgb(231, 179, 70);
            abilities.addView(makeModernAttributeRow(labels[i], value, colour));
        }
        page.addView(abilities);

        LinearLayout impact = makePanel();
        impact.addView(profileSectionTitle("ROLE IMPACT"));
        impact.addView(makeText(profileStaffImpact(role, rating), 14, text));
        page.addView(impact);

        LinearLayout contract = makePanel();
        contract.addView(profileSectionTitle("EMPLOYMENT"));
        contract.addView(profileInfoRow("Club", clubNames[selectedClub]));
        contract.addView(profileInfoRow("Weekly wage", "€" + profileStaffWeeklyWageK(rating) + "k"));
        contract.addView(profileInfoRow("Contract", "Rolling first-team contract"));
        contract.addView(profileInfoRow("Reputation", rating >= 80 ? "Excellent" : rating >= 72 ? "Very good" : "Established"));
        page.addView(contract);

        page.addView(makeAccentButton("Hire / Replace " + role, v -> hireStaff(role)));
        page.addView(makeButton("← Back to Staff", v -> showStaffHub()));
    }

    private String profileStaffName(String role) {
        if ("Assistant Manager".equals(role)) return assistantManagerName;
        if ("Head Coach".equals(role)) return headCoachName;
        return chiefScoutName;
    }

    private int profileStaffRating(String role) {
        if ("Assistant Manager".equals(role)) return assistantManagerRating;
        if ("Head Coach".equals(role)) return headCoachRating;
        return chiefScoutRating;
    }

    private int profileStaffWeeklyWageK(int rating) {
        return Math.max(8, (rating - 45) * 2);
    }

    private String[] profileStaffAttributeLabels(String role) {
        if ("Assistant Manager".equals(role)) {
            return new String[]{"Tactical knowledge", "Motivating", "Man management", "Judging ability", "Discipline", "Adaptability"};
        }
        if ("Head Coach".equals(role)) {
            return new String[]{"Technical coaching", "Tactical coaching", "Fitness coaching", "Youth development", "Motivating", "Discipline"};
        }
        return new String[]{"Judging ability", "Judging potential", "Tactical knowledge", "Adaptability", "Player knowledge", "Negotiating"};
    }

    private int profileStaffAttribute(int rating, String role, int index) {
        int base = Math.max(7, Math.min(18, Math.round(rating / 5f)));
        int variation = Math.abs((role.hashCode() * 31 + index * 17 + rating) % 5) - 2;
        if (("Chief Scout".equals(role) && index < 2) || ("Head Coach".equals(role) && index < 2) || ("Assistant Manager".equals(role) && index < 3)) {
            variation += 1;
        }
        return Math.max(5, Math.min(20, base + variation));
    }

    private String profileStaffImpact(String role, int rating) {
        String grade = rating >= 80 ? "elite-level" : rating >= 74 ? "high-quality" : rating >= 68 ? "solid" : "developing";
        if ("Assistant Manager".equals(role)) {
            return "Provides " + grade + " support for tactical preparation, player motivation, opposition analysis and match feedback. A stronger assistant improves the quality of pre-match advice.";
        }
        if ("Head Coach".equals(role)) {
            return "Provides " + grade + " day-to-day coaching. Higher quality coaching supports training efficiency, player development and the consistency of technical work.";
        }
        return "Provides " + grade + " recruitment support. Better scouting improves player knowledge, confidence in potential estimates and the usefulness of recruitment reports.";
    }

    private void hireStaff(String role) {
        final String[] candidates;
        final int[] ratings;
        if ("Assistant Manager".equals(role)) {
            candidates = new String[]{"Miguel Duarte", "Paulo Simões", "Helder Costa"};
            ratings = new int[]{68, 74, 79};
        } else if ("Head Coach".equals(role)) {
            candidates = new String[]{"Nuno Gama", "Sérgio Pires", "Carlos Vale"};
            ratings = new int[]{69, 75, 82};
        } else {
            candidates = new String[]{"Ruben Lopes", "Vasco Fernandes", "João Patrício"};
            ratings = new int[]{67, 73, 80};
        }
        String[] labels = new String[candidates.length];
        for (int i = 0; i < candidates.length; i++) labels[i] = candidates[i] + "  •  Rating " + ratings[i];
        new BossDialog.Builder(this)
                .setTitle("Hire " + role)
                .setItems(labels, (dialog, which) -> {
                    if ("Assistant Manager".equals(role)) {
                        assistantManagerName = candidates[which];
                        assistantManagerRating = ratings[which];
                        managerReputation = Math.min(100, managerReputation + 1);
                    } else if ("Head Coach".equals(role)) {
                        headCoachName = candidates[which];
                        headCoachRating = ratings[which];
                    } else {
                        chiefScoutName = candidates[which];
                        chiefScoutRating = ratings[which];
                        for (int i = 0; i < scoutKnowledge.length; i++) scoutKnowledge[i] = Math.max(scoutKnowledge[i], 2);
                    }
                    saveCurrentGame();
                    Toast.makeText(this, role + " hired.", Toast.LENGTH_SHORT).show();
                    showStaffHub();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTactics() {
        backAction = () -> showDashboard();
        ensureTacticsValid();

        LinearLayout page = createPage(
                "Tactics",
                clubNames[selectedClub] + " • " + tacticFormation + " • " + playStyle,
                true
        );

        // Classic Championship Manager-inspired tactical board:
        // compact controls at the top, then the squad split into XI / subs / reserves.
        LinearLayout classicHeader = makePanel();
        classicHeader.setBackground(rounded(Color.rgb(54, 88, 126)));

        TextView title = makeText("TEAM TACTICS", 18, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        classicHeader.addView(title);

        TextView nextMatch = makeText(
                "Starting XI " + countRole(2) + "/11   •   Substitutes " + countRole(1) + "/7\n"
                        + "Tap any player row to choose XI / bench / reserve and assign a position.",
                14,
                Color.WHITE
        );
        nextMatch.setPadding(0, dp(5), 0, 0);
        classicHeader.addView(nextMatch);
        page.addView(classicHeader);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, 0, 0, dp(8));

        Button formation = makeCompactTacticsButton("FORMATION\n" + tacticFormation, v -> showTacticsFormationDialog());
        Button style = makeCompactTacticsButton("STYLE\n" + playStyle, v -> showPlayStyleDialog());
        Button teamInstr = makeCompactTacticsButton("TEAM\nINSTRUCTIONS", v -> showClassicTeamInstructions());
        Button shape = makeCompactTacticsButton("WIB/WOB\n" + wibWobShape, v -> showWibWobShapeDialog());

        tabs.addView(formation);
        tabs.addView(style);
        tabs.addView(teamInstr);
        tabs.addView(shape);
        page.addView(tabs);

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, 0, 0, dp(8));

        Button autoPick = makeCompactTacticsButton("AUTO PICK", v -> {
            initialiseTacticsForClub();
            saveCurrentGame();
            showTactics();
        });
        autoPick.setTextColor(idealButtonTextColour(accent));
        autoPick.setBackground(buttonBackground(accent));

        Button clear = makeCompactTacticsButton("CLEAR TEAM", v -> new BossDialog.Builder(this)
                .setTitle("Clear team selection?")
                .setMessage("This will remove every player from the Starting XI and substitutes so you can rebuild the team from scratch.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (dialog, which) -> {
                    clearTeamSelection();
                    saveCurrentGame();
                    showTactics();
                })
                .show());
        clear.setTextColor(idealButtonTextColour(danger));
        clear.setBackground(buttonBackground(danger));

        actionRow.addView(autoPick);
        actionRow.addView(clear);
        page.addView(actionRow);

        ArrayList<Player> starters = new ArrayList<>();
        ArrayList<Player> substitutes = new ArrayList<>();
        ArrayList<Player> reserves = new ArrayList<>();

        for (Player p : players) {
            if (p.team != selectedClub) continue;
            int role = playerRoleStatus[p.id];
            if (role == 2) starters.add(p);
            else if (role == 1) substitutes.add(p);
            else reserves.add(p);
        }

        Collections.sort(starters, (a, b) -> Integer.compare(playerSelectedSlot[a.id], playerSelectedSlot[b.id]));
        sortPlayersByPositionThenOverall(substitutes);
        sortPlayersByPositionThenOverall(reserves);

        addTacticsTableSection(page, "STARTING XI", starters, 11);
        addTacticsTableSection(page, "SUBSTITUTES", substitutes, 7);
        addTacticsTableSection(page, "RESERVES / UNSELECTED", reserves, -1);

        if (countRole(2) != 11) {
            LinearLayout warningBox = makePanel();
            warningBox.setBackground(rounded(warning));
            TextView warningText = makeText(
                    "Select exactly 11 starters before beginning the next match.",
                    14,
                    Color.WHITE
            );
            warningText.setTypeface(Typeface.DEFAULT_BOLD);
            warningBox.addView(warningText);
            page.addView(warningBox);
        }
    }

    private Button makeCompactTacticsButton(String label, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(4), dp(7), dp(4), dp(7));
        b.setTextColor(idealButtonTextColour(panelLight));
        b.setBackground(buttonBackground(panelLight));
        b.setOnClickListener(click);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        lp.setMargins(dp(2), 0, dp(2), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void addTacticsTableSection(LinearLayout page, String title, ArrayList<Player> group, int target) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);

        TextView sectionTitle = makeText(
                target > 0 ? title + "   " + group.size() + "/" + target : title + "   " + group.size(),
                13,
                Color.WHITE
        );
        sectionTitle.setTypeface(Typeface.DEFAULT_BOLD);
        sectionTitle.setPadding(dp(8), dp(8), dp(8), dp(8));
        sectionTitle.setBackgroundColor(Color.rgb(54, 88, 126));
        section.addView(sectionTitle);

        section.addView(makeTacticsTableHeader());

        if (group.isEmpty()) {
            TextView empty = makeText("No players selected", 14, muted);
            empty.setPadding(dp(10), dp(12), dp(10), dp(12));
            empty.setBackgroundColor(panel);
            section.addView(empty);
        } else {
            for (int i = 0; i < group.size(); i++) {
                section.addView(makeTacticsPlayerRow(group.get(i), i));
            }
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(12));
        section.setLayoutParams(lp);
        page.addView(section);
    }

    private LinearLayout makeTacticsTableHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(5), dp(6), dp(5), dp(6));
        row.setBackgroundColor(Color.rgb(29, 57, 82));

        row.addView(makeTacticsCell("CON", 0.75f, true, muted));
        row.addView(makeTacticsCell("No", 0.55f, true, muted));
        row.addView(makeTacticsCell("POS", 0.75f, true, muted));
        row.addView(makeTacticsCell("NAME", 2.7f, true, muted));
        row.addView(makeTacticsCell("OVR", 0.65f, true, muted));
        row.addView(makeTacticsCell("APP", 0.65f, true, muted));
        row.addView(makeTacticsCell("GLS", 0.65f, true, muted));
        row.addView(makeTacticsCell("AST", 0.65f, true, muted));
        return row;
    }

    private LinearLayout makeTacticsPlayerRow(Player p, int rowIndex) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(5), dp(7), dp(5), dp(7));
        row.setBackgroundColor(rowIndex % 2 == 0 ? Color.rgb(14, 39, 53) : Color.rgb(18, 47, 62));

        int conditionColour = p.fitness >= 90 ? accent : p.fitness >= 75 ? warning : danger;
        row.addView(makeTacticsCell(p.fitness + "%", 0.75f, false, conditionColour));
        row.addView(makeTacticsCell(String.valueOf((p.id % PLAYERS_PER_TEAM) + 1), 0.55f, false, text));
        row.addView(makeTacticsCell(displayTacticsPosition(p), 0.85f, false, accent));
        row.addView(makeTacticsCell(p.name, 2.7f, false, text));
        row.addView(makeTacticsCell(String.valueOf(p.overall), 0.65f, false, text));
        row.addView(makeTacticsCell(String.valueOf(p.appearances), 0.65f, false, text));
        row.addView(makeTacticsCell(String.valueOf(p.goals), 0.65f, false, text));
        row.addView(makeTacticsCell(String.valueOf(p.assists), 0.65f, false, text));

        row.setOnClickListener(v -> showTacticsAssignmentDialog(p));

        return row;
    }

    private TextView makeTacticsCell(String value, float weight, boolean header, int colour) {
        TextView cell = makeText(value, header ? 10 : 12, colour);
        cell.setSingleLine(true);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        if (header) cell.setTypeface(Typeface.DEFAULT_BOLD);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                weight
        );
        cell.setLayoutParams(lp);
        return cell;
    }

    private String displayTacticsPosition(Player p) {
        if (playerRoleStatus[p.id] == 2 && playerSelectedSlot[p.id] >= 0) {
            String[] slots = formationSlots();
            if (playerSelectedSlot[p.id] < slots.length) return slots[playerSelectedSlot[p.id]];
        }
        if (playerSelectedPosition[p.id] != null && !playerSelectedPosition[p.id].isEmpty()) return playerSelectedPosition[p.id];
        return p.position;
    }

    private String[] formationSlots() {
        if ("4-2-3-1".equals(tacticFormation)) return new String[]{"GK", "DL", "DC", "DC", "DR", "DMC", "MC", "AML", "AMC", "AMR", "FC"};
        if ("4-4-2".equals(tacticFormation)) return new String[]{"GK", "DL", "DC", "DC", "DR", "ML", "MC", "MC", "MR", "FC", "FC"};
        if ("3-5-2".equals(tacticFormation)) return new String[]{"GK", "DC", "DC", "DC", "LWB", "MC", "MC", "AMC", "RWB", "FC", "FC"};
        if ("5-3-2".equals(tacticFormation)) return new String[]{"GK", "DL", "DC", "DC", "DC", "DR", "MC", "MC", "AMC", "FC", "FC"};
        return new String[]{"GK", "DL", "DC", "DC", "DR", "MC", "MC", "MC", "AML", "AMR", "FC"};
    }

    private void showTacticsAssignmentDialog(Player p) {
        ArrayList<String> options = new ArrayList<>();
        options.add("Reserve / Unselected");
        options.add("Substitute Bench");
        for (String slot : formationSlots()) options.add("Starting XI • " + slot);
        new BossDialog.Builder(this)
                .setTitle(p.name + " • choose role")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        playerRoleStatus[p.id] = 0;
                        playerSelectedSlot[p.id] = -1;
                        playerSelectedPosition[p.id] = p.position;
                    } else if (which == 1) {
                        if (countRole(1) >= 7 && playerRoleStatus[p.id] != 1) {
                            Toast.makeText(this, "Substitutes are already full.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (playerRoleStatus[p.id] == 2 && playerSelectedSlot[p.id] >= 0) clearStarterSlot(playerSelectedSlot[p.id]);
                        playerRoleStatus[p.id] = 1;
                        playerSelectedSlot[p.id] = -1;
                        playerSelectedPosition[p.id] = p.position;
                    } else {
                        assignPlayerToStarterSlot(p, which - 2);
                    }
                    saveCurrentGame();
                    showTactics();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void assignPlayerToStarterSlot(Player p, int slot) {
        if (slot < 0 || slot >= formationSlots().length) return;
        if (playerRoleStatus[p.id] != 2 && countRole(2) >= 11) {
            Toast.makeText(this, "Starting XI already has 11 players. Replace a position directly.", Toast.LENGTH_SHORT).show();
        }
        clearStarterSlot(slot);
        playerRoleStatus[p.id] = 2;
        playerSelectedSlot[p.id] = slot;
        playerSelectedPosition[p.id] = formationSlots()[slot];
    }

    private void clearStarterSlot(int slot) {
        for (Player other : players) {
            if (other.team == selectedClub && playerRoleStatus[other.id] == 2 && playerSelectedSlot[other.id] == slot) {
                playerRoleStatus[other.id] = 0;
                playerSelectedSlot[other.id] = -1;
                playerSelectedPosition[other.id] = other.position;
            }
        }
    }

    private void clearTeamSelection() {
        for (Player p : players) {
            if (p.team == selectedClub) {
                playerRoleStatus[p.id] = 0;
                playerSelectedSlot[p.id] = -1;
                playerSelectedPosition[p.id] = p.position;
            }
        }
    }

    private void showTacticsFormationDialog() {
        String[] options = {"4-3-3", "4-2-3-1", "4-4-2", "3-5-2", "5-3-2"};
        new BossDialog.Builder(this)
                .setTitle("Formation")
                .setSingleChoiceItems(options, Arrays.asList(options).indexOf(tacticFormation), (dialog, which) -> {
                    tacticFormation = options[which];
                    saveCurrentGame();
                    dialog.dismiss();
                    showTactics();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPlayStyleDialog() {
        String[] options = {"Balanced", "Possession", "High Press", "Counter Attack", "Direct", "Attacking", "Defensive"};
        int checked = Arrays.asList(options).indexOf(playStyle);
        new BossDialog.Builder(this)
                .setTitle("Style of Play")
                .setSingleChoiceItems(options, Math.max(0, checked), (dialog, which) -> {
                    playStyle = options[which];
                    saveCurrentGame();
                    dialog.dismiss();
                    showTactics();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClassicTeamInstructions() {
        String[] options = {
                "Passing: " + passingInstruction,
                "Tackling: " + tacklingInstruction,
                "Pressing: " + (pressingInstruction ? "ON" : "OFF"),
                "Offside Trap: " + (offsideTrapInstruction ? "ON" : "OFF"),
                "Counter Attack: " + (counterAttackInstruction ? "ON" : "OFF"),
                "Men Behind Ball: " + (menBehindBallInstruction ? "ON" : "OFF")
        };
        new BossDialog.Builder(this)
                .setTitle("Classic Team Instructions")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) passingInstruction = nextPassing(passingInstruction);
                    if (which == 1) tacklingInstruction = nextTackling(tacklingInstruction);
                    if (which == 2) pressingInstruction = !pressingInstruction;
                    if (which == 3) offsideTrapInstruction = !offsideTrapInstruction;
                    if (which == 4) counterAttackInstruction = !counterAttackInstruction;
                    if (which == 5) menBehindBallInstruction = !menBehindBallInstruction;
                    saveCurrentGame();
                    if (liveMatchActive) {
                        recordCommentary(selectedClub, "Tactical instruction changed by " + clubNames[selectedClub] + ".");
                        dialog.dismiss();
                        if (livePitchView != null) livePitchView.refreshFormationAnchors();
                        refreshLiveHeader();
                    } else {
                        showTactics();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private String nextPassing(String current) {
        String[] values = {"Mixed", "Short", "Direct", "Long"};
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) return values[(i + 1) % values.length];
        return "Mixed";
    }

    private String nextTackling(String current) {
        if ("Easy".equals(current)) return "Normal";
        if ("Normal".equals(current)) return "Hard";
        return "Easy";
    }

    private void showWibWobShapeDialog() {
        String[] shapes = {"Balanced", "Central Overload", "Wide", "High Line", "Deep Block"};
        int checked = Math.max(0, Arrays.asList(shapes).indexOf(wibWobShape));
        new BossDialog.Builder(this)
                .setTitle("With Ball / Without Ball Shape")
                .setMessage("A mobile-friendly version of classic zone-based positioning. The shape changes how the team behaves in possession and out of possession.")
                .setSingleChoiceItems(shapes, checked, (dialog, which) -> {
                    wibWobShape = shapes[which];
                    saveCurrentGame();
                    dialog.dismiss();
                    showTactics();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String positionGroup(String position) {
        int rank = positionRank(position);
        if (rank == 0) return "Goalkeepers";
        if (rank <= 3) return "Defenders";
        if (rank <= 8) return "Midfielders";
        return "Forwards";
    }

    private void cycleTacticsRole(int playerId) {
        int current = playerRoleStatus[playerId];
        if (current == 0) {
            if (countRole(2) >= 11) {
                Toast.makeText(this, "Starting XI already has 11 players.", Toast.LENGTH_SHORT).show();
                return;
            }
            playerRoleStatus[playerId] = 2;
        } else if (current == 2) {
            if (countRole(1) >= 7) {
                playerRoleStatus[playerId] = 0;
            } else {
                playerRoleStatus[playerId] = 1;
            }
        } else {
            playerRoleStatus[playerId] = 0;
        }
    }

    private int countRole(int role) {
        int count = 0;
        for (Player p : players) if (p.team == selectedClub && playerRoleStatus[p.id] == role) count++;
        return count;
    }

    private String tacticsStatusLabel(int id) {
        if (playerRoleStatus[id] == 2) return "XI";
        if (playerRoleStatus[id] == 1) return "SUB";
        return "RES";
    }

    private void initialiseTacticsForClub() {
        Arrays.fill(playerRoleStatus, 0);
        Arrays.fill(playerSelectedSlot, -1);
        Arrays.fill(playerSelectedPosition, "");
        Arrays.fill(playerSelectedSlot, -1);
        if (selectedClub < 0) return;

        ArrayList<Player> squad = new ArrayList<>();
        Player bestGK = null;
        for (Player p : players) {
            if (p.team != selectedClub) continue;
            squad.add(p);
            if (p.position.equals("GK") && (bestGK == null || p.overall > bestGK.overall)) bestGK = p;
        }

        Collections.sort(squad, (a, b) -> Integer.compare(b.overall, a.overall));
        int starters = 0;
        if (bestGK != null) {
            playerRoleStatus[bestGK.id] = 2;
            starters++;
        }
        for (Player p : squad) {
            if (starters >= 11) break;
            if (bestGK != null && p.id == bestGK.id) continue;
            playerRoleStatus[p.id] = 2;
            starters++;
        }

        int subs = 0;
        for (Player p : squad) {
            if (playerRoleStatus[p.id] != 0) continue;
            if (subs >= 7) break;
            playerRoleStatus[p.id] = 1;
            playerSelectedSlot[p.id] = -1;
            playerSelectedPosition[p.id] = p.position;
            subs++;
        }
    }

    private void ensureTacticsValid() {
        // Keep the user's manual selection intact, including an intentionally cleared team.
        // New careers are auto-picked once during club selection.
        for (Player p : players) {
            if (p.team != selectedClub && playerRoleStatus[p.id] != 0) {
                playerRoleStatus[p.id] = 0;
            }
        }
    }

    private void showTraining() {
        backAction = () -> showDashboard();
        LinearLayout page = createPage("Training", "Classic schedules • growth versus fatigue", true);

        LinearLayout info = makePanel();
        info.addView(makeText(
                "Each category has four intensity levels: None, Light, Medium and Intensive.\n"
                        + "Higher intensity improves development but reduces condition and increases injury risk.\n"
                        + "Potential ability, professionalism and age influence how quickly a player develops.",
                15,
                text
        ));
        page.addView(info);

        String[] categories = {"Fitness", "Tactics", "Skills", "Shooting", "Goalkeeping"};
        for (int i = 0; i < categories.length; i++) {
            final int category = i;
            Button b = makeButton(categories[i] + "  •  " + intensityName(trainingIntensity[i]), v -> {
                trainingIntensity[category] = (trainingIntensity[category] + 1) % 4;
                trainingFocus = categories[category];
                saveCurrentGame();
                showTraining();
            });
            if (trainingIntensity[i] == 3) { b.setTextColor(idealButtonTextColour(warning)); b.setBackground(buttonBackground(warning)); }
            else if (trainingIntensity[i] == 2) { b.setTextColor(idealButtonTextColour(accent)); b.setBackground(buttonBackground(accent)); }
            page.addView(b);
        }

        LinearLayout summary = makePanel();
        int load = 0;
        for (int value : trainingIntensity) load += value;
        summary.addView(makeText("TOTAL TRAINING LOAD  •  " + load + "/15", 14, accent));
        summary.addView(makeText(load >= 13 ? "Very demanding — high fatigue/injury risk" : load >= 10 ? "Demanding" : load >= 6 ? "Balanced" : "Light workload", 14, text));
        page.addView(summary);
        page.addView(makeButton("View Squad Development", v -> showTeamPlayers(selectedClub)));
    }

    private String intensityName(int value) {
        if (value == 0) return "None";
        if (value == 1) return "Light";
        if (value == 2) return "Medium";
        return "Intensive";
    }

    private void applyTrainingSession() {
        int totalLoad = 0;
        for (int value : trainingIntensity) totalLoad += value;

        for (Player p : players) {
            if (p.team != selectedClub) continue;
            if (p.injuredWeeks > 0) continue;

            double ageFactor = p.age <= 21 ? 1.35 : p.age <= 25 ? 1.12 : p.age <= 29 ? 0.90 : 0.55;
            double proFactor = 0.65 + p.professionalism / 20.0;
            double potentialRoom = Math.max(0, p.potentialAbility - p.currentAbility) / 80.0;
            double facilityDevelopment = 0.84 + stadiumTrainingLevel * 0.06 + stadiumGymLevel * 0.025;
            double baseChance = Math.min(0.82, (0.07 + potentialRoom * 0.20) * ageFactor * proFactor * facilityDevelopment);

            if (random.nextDouble() < baseChance * (trainingIntensity[0] / 2.0)) {
                p.pace = Math.min(99, p.pace + 1);
                p.physical = Math.min(99, p.physical + (random.nextBoolean() ? 1 : 0));
            }
            if (random.nextDouble() < baseChance * (trainingIntensity[1] / 2.0)) {
                p.passing = Math.min(99, p.passing + (random.nextBoolean() ? 1 : 0));
                p.defending = Math.min(99, p.defending + (random.nextBoolean() ? 1 : 0));
            }
            if (random.nextDouble() < baseChance * (trainingIntensity[2] / 2.0)) {
                p.technique = Math.min(99, p.technique + 1);
                p.passing = Math.min(99, p.passing + (random.nextBoolean() ? 1 : 0));
            }
            if (random.nextDouble() < baseChance * (trainingIntensity[3] / 2.0)) {
                p.finishing = Math.min(99, p.finishing + 1);
            }
            if (p.position.equals("GK") && random.nextDouble() < baseChance * (trainingIntensity[4] / 2.0)) {
                p.defending = Math.min(99, p.defending + 1);
                p.technique = Math.min(99, p.technique + (random.nextBoolean() ? 1 : 0));
            }

            int conditionCost = Math.max(0, totalLoad - 8) / 2;
            p.fitness = Math.max(55, p.fitness - conditionCost);
            double injuryRisk = 0.002 + Math.max(0, totalLoad - 10) * 0.0025 + p.injuryProneness * 0.0005;
            injuryRisk *= Math.max(0.55, 1.08 - stadiumMedicalLevel * 0.07 - stadiumGymLevel * 0.035);
            if (random.nextDouble() < injuryRisk) {
                p.injuredWeeks = 1 + random.nextInt(4);
                addNews("MEDICAL", p.name + " injured in training", "Estimated absence: " + p.injuredWeeks + " week(s).");
            }
            recalculateOverall(p);
            p.currentAbility = Math.min(p.potentialAbility, Math.max(p.currentAbility, p.overall * 2));
        }
    }

    private void recalculateOverall(Player p) {
        int roleScore;
        if (p.position.equals("GK")) {
            roleScore = (p.technique + p.passing + p.physical + p.defending) / 4;
        } else if (p.position.equals("CB") || p.position.equals("LB") || p.position.equals("RB") || p.position.equals("DM")) {
            roleScore = (p.defending * 2 + p.physical + p.pace + p.passing) / 5;
        } else if (p.position.equals("ST") || p.position.equals("RW") || p.position.equals("LW")) {
            roleScore = (p.finishing * 2 + p.pace + p.technique + p.physical) / 5;
        } else {
            roleScore = (p.technique + p.passing * 2 + p.pace + p.physical) / 5;
        }
        p.overall = Math.max(p.overall, Math.min(99, roleScore));
        p.valueMillions = Math.max(1, (p.overall * p.overall) / 360);
    }

    private void updatePlayerMatchStats() {
        for (Player p : players) {
            if(p.team==liveHome || p.team==liveAway) {
                if(matchParticipants.contains(p.id)) { p.appearances++;p.morale=Math.max(30,Math.min(100,p.morale+1)); }
                else p.fitness=Math.min(100,p.fitness+3);
                continue;
            }
            if (random.nextDouble() < 0.62) {
                p.appearances++;
                p.fitness = Math.max(55, p.fitness - random.nextInt(7));
                p.morale = Math.max(50, Math.min(100, p.morale + random.nextInt(7) - 2));

                double goalChance = 0.01;
                if (p.position.equals("ST")) goalChance = 0.20;
                else if (p.position.equals("RW") || p.position.equals("LW") || p.position.equals("AM")) goalChance = 0.11;
                else if (p.position.equals("CM") || p.position.equals("RM") || p.position.equals("LM")) goalChance = 0.06;
                if (random.nextDouble() < goalChance) p.goals++;

                double assistChance = p.passing / 900.0;
                if (random.nextDouble() < assistChance) p.assists++;
            } else {
                p.fitness = Math.min(100, p.fitness + 3);
            }
        }
    }

    private void showCompetitionCalendar() {
        showCalendarTab("League");
    }

    private void showCalendarTab(String tab) {
        backAction = () -> showDashboard();

        LinearLayout page = createPage(
                "Calendar",
                clubNames[selectedClub] + " • fixtures & results",
                false
        );

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, 0, 0, dp(6));
        tabs.addView(calendarTabButton("League", tab), calendarTabParams());
        tabs.addView(calendarTabButton("Taça", tab), calendarTabParams());
        tabs.addView(calendarTabButton("League Cup", tab), calendarTabParams());
        tabs.addView(calendarTabButton("Europe", tab), calendarTabParams());
        page.addView(tabs);

        ArrayList<Fixture> fixtures = buildClubFixtures(tab);
        if (fixtures.isEmpty()) {
            TextView none = makeText("No fixtures in this competition.", 14, muted);
            none.setPadding(0, dp(14), 0, 0);
            page.addView(none);
            return;
        }

        HorizontalScrollView tableScroll = new HorizontalScrollView(this);
        tableScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setMinimumWidth(dp(690));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(6), dp(6), dp(6), dp(6));
        header.setBackgroundColor(Color.rgb(45, 78, 108));
        header.addView(calendarCell("DATE", 1.15f, true, Color.WHITE));
        header.addView(calendarCell("V", .38f, true, Color.WHITE));
        header.addView(calendarCell("OPPONENT", 2.15f, true, Color.WHITE));
        header.addView(calendarCell("STAGE", 1.85f, true, Color.WHITE));
        header.addView(calendarCell("STATUS", 1.05f, true, Color.WHITE));
        table.addView(header);

        for (int i = 0; i < fixtures.size(); i++) {
            Fixture f = fixtures.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(6), dp(4), dp(6), dp(4));
            row.setBackgroundColor(i % 2 == 0 ? Color.rgb(12, 37, 50) : Color.rgb(16, 44, 58));

            String venue = f.home ? "H" : "A";
            String status = f.played ? ("FT  " + f.goalsFor + "-" + f.goalsAgainst) : "Upcoming";
            int statusColour = f.played ? accent : muted;
            row.addView(calendarCell(f.date.format(DateTimeFormatter.ofPattern("dd/MM/yy")), 1.15f, false, text));
            row.addView(calendarCell(venue, .38f, false, accent));
            row.addView(calendarCell(fixtureOpponentName(f), 2.15f, false, text));
            row.addView(calendarCell(f.stage, 1.85f, false, muted));
            row.addView(calendarCell(status, 1.05f, false, statusColour));

            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(34));
            rlp.setMargins(0,0,0,dp(1));
            row.setLayoutParams(rlp);
            table.addView(row);
        }
        tableScroll.addView(table);
        page.addView(tableScroll);
    }

    private TextView calendarCell(String value, float weight, boolean header, int colour) {
        TextView tv = makeText(value, header ? 10 : 11, colour);
        tv.setSingleLine(true);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        if (header) tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight));
        return tv;
    }

    private Button calendarTabButton(String label, String selected) {
        Button b = compactButton(label, v -> showCalendarTab(label));
        if (label.equals(selected)) { b.setTextColor(idealButtonTextColour(accent)); b.setBackground(buttonBackground(accent)); }
        return b;
    }

    private LinearLayout.LayoutParams calendarTabParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(36), 1f);
        p.setMargins(dp(2), 0, dp(2), dp(4));
        return p;
    }

    private ArrayList<Fixture> buildClubFixtures(String tab) {
        ArrayList<Fixture> list = new ArrayList<>();

        if ("League".equals(tab)) {
            for (int round = 0; round < 34; round++) {
                Fixture f = new Fixture(
                        SEASON_START.plusDays(round * 7L),
                        "Liga Portugal",
                        "League Matchday " + (round + 1),
                        leagueOpponentForRound(round),
                        selectedHomeForRound(round)
                );
                if (round < matchday && clubMatchGF[round] >= 0) {
                    f.played = true;
                    f.goalsFor = clubMatchGF[round];
                    f.goalsAgainst = clubMatchGA[round];
                }
                list.add(f);
            }
            return list;
        }

        if ("Taça".equals(tab)) {
            int[] months = {10, 11, 12, 2, 3, 4, 5};
            int[] days = {18, 22, 17, 10, 3, 21, 30};
            int[] years = {2026, 2026, 2026, 2027, 2027, 2027, 2027};
            String[] stages = {"3rd Round", "4th Round", "Round of 16", "Quarter-final", "Semi-final 1st leg", "Semi-final 2nd leg", "Final"};
            for (int i = 0; i < stages.length; i++) {
                LocalDate date = LocalDate.of(years[i], months[i], days[i]);
                int opp = date.isBefore(currentDate) ? domesticCupOpponent(i, 5) : -999;
                Fixture f = new Fixture(date, "Taça de Portugal", stages[i], opp, i % 2 == 0);
                applyDeterministicResultIfPlayed(f);
                if (!f.played && f.opponent == -999) f.stage = stages[i] + " • Draw pending";
                list.add(f);
            }
            return list;
        }

        if ("League Cup".equals(tab)) {
            LocalDate[] dates = {
                    LocalDate.of(2026, 10, 28),
                    LocalDate.of(2027, 1, 6),
                    LocalDate.of(2027, 1, 9)
            };
            String[] stages = {"Quarter-final", "Semi-final", "Final"};
            for (int i = 0; i < stages.length; i++) {
                int opp = dates[i].isBefore(currentDate) ? domesticCupOpponent(i, 11) : -999;
                Fixture f = new Fixture(dates[i], "Taça da Liga", stages[i], opp, i != 1);
                applyDeterministicResultIfPlayed(f);
                if (!f.played && f.opponent == -999) f.stage = stages[i] + " • Draw pending";
                list.add(f);
            }
            return list;
        }

        String competition = europeanCompetitionForClub(selectedClub);
        if ("No European competition".equals(competition)) return list;
        int stageIndex = 0;
        for (CalendarEvent e : buildEuropeanCalendar()) {
            if (!e.competition.equals(competition)) continue;
            int opponent = -(stageIndex + 1);
            Fixture f = new Fixture(e.date, competition, e.stage, opponent, stageIndex % 2 == 0);
            applyDeterministicResultIfPlayed(f);
            list.add(f);
            stageIndex++;
        }
        return list;
    }

    private int leagueOpponentForRound(int round) {
        int halfRound = Math.floorMod(round, 17);
        int opponent = (selectedClub + halfRound + 1) % 18;
        if (opponent == selectedClub) opponent = (opponent + 1) % 18;
        return opponent;
    }

    private boolean selectedHomeForRound(int round) {
        boolean firstHalfHome = ((round + selectedClub) % 2 == 0);
        return round < 17 ? firstHalfHome : !firstHalfHome;
    }

    private int domesticCupOpponent(int stage, int salt) {
        int opponent = Math.floorMod(selectedClub + stage * 3 + salt, 18);
        if (opponent == selectedClub) opponent = (opponent + 1) % 18;
        return opponent;
    }

    private String fixtureOpponentName(Fixture f) {
        if (f.opponent == -999) return "TBD";
        if (f.opponent >= 0) return clubNames[f.opponent];
        String[] europeanOpponents = {
                "Madrid Blanco", "Catalunya Blau", "Manchester Red", "London Cannon",
                "Milano Nerazzurri", "München Rot", "Paris Étoile", "Amsterdam 1900",
                "Dortmund Gelb", "Napoli Azzurro", "Marseille Sud", "Prague Lions",
                "Roma Capitale", "Istanbul Kartal"
        };
        int idx = Math.floorMod((-f.opponent - 1) + selectedClub, europeanOpponents.length);
        return europeanOpponents[idx];
    }

    private void applyDeterministicResultIfPlayed(Fixture f) {
        if (!f.date.isBefore(currentDate)) return;
        long seed = f.date.toEpochDay() * 97L + selectedClub * 1009L + f.stage.hashCode();
        Random r = new Random(seed);
        int qualityBonus = strength[selectedClub] >= 82 ? 1 : 0;
        f.goalsFor = Math.min(5, r.nextInt(3) + qualityBonus);
        f.goalsAgainst = Math.min(5, r.nextInt(3));
        f.played = true;
    }

    private ArrayList<CalendarEvent> buildEuropeanCalendar() {
        ArrayList<CalendarEvent> e = new ArrayList<>();

        e.add(new CalendarEvent(2026, 9, 8, "Champions League", "League phase MD1 • 8–10 Sep"));
        e.add(new CalendarEvent(2026, 10, 13, "Champions League", "League phase MD2 • 13–14 Oct"));
        e.add(new CalendarEvent(2026, 10, 20, "Champions League", "League phase MD3 • 20–21 Oct"));
        e.add(new CalendarEvent(2026, 11, 3, "Champions League", "League phase MD4 • 3–4 Nov"));
        e.add(new CalendarEvent(2026, 11, 24, "Champions League", "League phase MD5 • 24–25 Nov"));
        e.add(new CalendarEvent(2026, 12, 8, "Champions League", "League phase MD6 • 8–9 Dec"));
        e.add(new CalendarEvent(2027, 1, 19, "Champions League", "League phase MD7 • 19–20 Jan"));
        e.add(new CalendarEvent(2027, 1, 27, "Champions League", "League phase MD8"));
        e.add(new CalendarEvent(2027, 2, 16, "Champions League", "Knockout play-offs • 16/17 & 23/24 Feb"));
        e.add(new CalendarEvent(2027, 3, 9, "Champions League", "Round of 16 • 9/10 & 16/17 Mar"));
        e.add(new CalendarEvent(2027, 4, 6, "Champions League", "Quarter-finals • 6/7 & 13/14 Apr"));
        e.add(new CalendarEvent(2027, 4, 27, "Champions League", "Semi-finals • 27/28 Apr & 4/5 May"));
        e.add(new CalendarEvent(2027, 6, 5, "Champions League", "Final • Madrid"));

        e.add(new CalendarEvent(2026, 9, 16, "Europa League", "League phase MD1 • 16–17 Sep"));
        e.add(new CalendarEvent(2026, 10, 15, "Europa League", "League phase MD2"));
        e.add(new CalendarEvent(2026, 10, 22, "Europa League", "League phase MD3"));
        e.add(new CalendarEvent(2026, 11, 5, "Europa League", "League phase MD4"));
        e.add(new CalendarEvent(2026, 11, 26, "Europa League", "League phase MD5"));
        e.add(new CalendarEvent(2026, 12, 10, "Europa League", "League phase MD6"));
        e.add(new CalendarEvent(2027, 1, 21, "Europa League", "League phase MD7"));
        e.add(new CalendarEvent(2027, 1, 28, "Europa League", "League phase MD8"));
        e.add(new CalendarEvent(2027, 2, 18, "Europa League", "Knockout play-offs • 18 & 25 Feb"));
        e.add(new CalendarEvent(2027, 3, 11, "Europa League", "Round of 16 • 11 & 18 Mar"));
        e.add(new CalendarEvent(2027, 4, 8, "Europa League", "Quarter-finals • 8 & 15 Apr"));
        e.add(new CalendarEvent(2027, 4, 29, "Europa League", "Semi-finals • 29 Apr & 6 May"));
        e.add(new CalendarEvent(2027, 5, 26, "Europa League", "Final • Frankfurt"));

        e.add(new CalendarEvent(2026, 10, 15, "Conference League", "League phase MD1"));
        e.add(new CalendarEvent(2026, 10, 22, "Conference League", "League phase MD2"));
        e.add(new CalendarEvent(2026, 11, 5, "Conference League", "League phase MD3"));
        e.add(new CalendarEvent(2026, 11, 26, "Conference League", "League phase MD4"));
        e.add(new CalendarEvent(2026, 12, 10, "Conference League", "League phase MD5"));
        e.add(new CalendarEvent(2026, 12, 17, "Conference League", "League phase MD6"));
        e.add(new CalendarEvent(2027, 2, 18, "Conference League", "Knockout play-offs • 18 & 25 Feb"));
        e.add(new CalendarEvent(2027, 3, 11, "Conference League", "Round of 16 • 11 & 18 Mar"));
        e.add(new CalendarEvent(2027, 4, 8, "Conference League", "Quarter-finals • 8 & 15 Apr"));
        e.add(new CalendarEvent(2027, 4, 29, "Conference League", "Semi-finals • 29 Apr & 6 May"));
        e.add(new CalendarEvent(2027, 6, 2, "Conference League", "Final • Istanbul"));

        Collections.sort(e, (a, b) -> a.date.compareTo(b.date));
        return e;
    }

    private String europeanCompetitionForClub(int club) {
        if (club == 1 || club == 2) return "Champions League";
        if (club == 0) return "Europa League";
        if (club == 3) return "Conference League";
        return "No European competition";
    }

    private void generatePlayers() {
        players.clear();
        String[] firstNames = {
                "João", "Miguel", "Tiago", "Diogo", "Rafael", "André", "Pedro", "Gonçalo", "Tomás", "Nuno",
                "Bruno", "Fábio", "Duarte", "Rodrigo", "Martim", "Vasco", "Afonso", "Rui", "Henrique", "Daniel"
        };
        String[] lastNames = {
                "Silva", "Costa", "Ferreira", "Santos", "Pereira", "Oliveira", "Sousa", "Martins", "Almeida", "Rocha",
                "Correia", "Mendes", "Carvalho", "Ribeiro", "Fernandes", "Teixeira", "Lopes", "Gomes", "Pinto", "Neves"
        };
        String[] positions = {
                "GK", "GK", "RB", "CB", "CB", "CB", "LB", "LB", "DM", "CM",
                "CM", "CM", "AM", "RM", "LM", "RW", "LW", "ST", "ST", "ST"
        };

        for (int team = 0; team < 18; team++) {
            Random r = new Random(71000L + team * 997L);
            for (int i = 0; i < PLAYERS_PER_TEAM; i++) {
                Player p = new Player(team * PLAYERS_PER_TEAM + i, team);
                p.name = firstNames[r.nextInt(firstNames.length)] + " " + lastNames[r.nextInt(lastNames.length)];
                p.position = positions[i];
                p.age = 17 + r.nextInt(18);
                p.overall = Math.max(54, Math.min(91, strength[team] - 10 + r.nextInt(19)));
                p.pace = attributeFromOverall(r, p.overall, 16);
                p.technique = attributeFromOverall(r, p.overall, 14);
                p.passing = attributeFromOverall(r, p.overall, 15);
                p.finishing = attributeFromOverall(r, p.overall, 20);
                p.defending = attributeFromOverall(r, p.overall, 20);
                p.physical = attributeFromOverall(r, p.overall, 15);
                tuneAttributesForPosition(p, r);
                p.appearances = 0;
                p.goals = 0;
                p.assists = 0;
                p.fitness = 88 + r.nextInt(13);
                p.morale = 70 + r.nextInt(26);
                p.valueMillions = Math.max(1, (p.overall * p.overall) / 360);
                p.contractYears = 1 + r.nextInt(5);
                p.transferListed = team != selectedClub && r.nextDouble() < 0.06;
                p.loanListed = !p.transferListed && team != selectedClub && p.age <= 22 && r.nextDouble() < 0.08;
                p.onLoan = false;
                p.currentAbility = Math.min(200, p.overall * 2);
                p.potentialAbility = Math.min(200, Math.max(p.currentAbility, p.currentAbility + r.nextInt(41)));
                p.consistency = 6 + r.nextInt(15);
                p.importantMatches = 5 + r.nextInt(16);
                p.adaptability = 5 + r.nextInt(16);
                p.professionalism = 5 + r.nextInt(16);
                p.injuryProneness = 2 + r.nextInt(18);
                p.pressure = 5 + r.nextInt(16);
                p.temperament = 5 + r.nextInt(16);
                p.ambition = 5 + r.nextInt(16);
                p.weeklyWageK = Math.max(5, (p.overall - 45) * (p.overall - 45) / 18);
                p.squadRole = p.overall >= strength[team] + 3 ? "Important Player" : p.overall >= strength[team] - 2 ? "Squad Player" : "Rotation";
                p.injuredWeeks = 0;
                p.freeRole = false;
                p.forwardRuns = p.position.equals("ST") || p.position.equals("AM") || p.position.equals("RW") || p.position.equals("LW");
                p.runWithBall = p.position.equals("RW") || p.position.equals("LW") || p.position.equals("AM");
                p.longShots = p.position.equals("AM") || p.position.equals("CM");
                players.add(p);
            }
        }
    }

    private int attributeFromOverall(Random r, int overall, int spread) {
        return Math.max(35, Math.min(95, overall - spread / 2 + r.nextInt(spread + 1)));
    }

    private void tuneAttributesForPosition(Player p, Random r) {
        if (p.position.equals("ST")) {
            p.finishing = Math.min(97, p.finishing + 8);
            p.defending = Math.max(30, p.defending - 12);
        } else if (p.position.equals("CB")) {
            p.defending = Math.min(97, p.defending + 9);
            p.finishing = Math.max(30, p.finishing - 12);
        } else if (p.position.equals("RW") || p.position.equals("LW")) {
            p.pace = Math.min(97, p.pace + 8);
            p.technique = Math.min(97, p.technique + 5);
        } else if (p.position.equals("CM") || p.position.equals("AM")) {
            p.passing = Math.min(97, p.passing + 7);
            p.technique = Math.min(97, p.technique + 5);
        } else if (p.position.equals("LB") || p.position.equals("RB")) {
            p.pace = Math.min(97, p.pace + 5);
            p.defending = Math.min(97, p.defending + 5);
        } else if (p.position.equals("GK")) {
            p.defending = Math.min(97, p.overall + r.nextInt(8));
            p.finishing = 30 + r.nextInt(16);
            p.pace = 40 + r.nextInt(26);
        }
    }

    private Player findPlayer(int id) {
        if (id < 0 || id >= players.size()) return null;
        return players.get(id);
    }

    private void showTeamEditor() {
        backAction = () -> showMainMenu();
        LinearLayout page = createPage("Game Editor", "Edit fictional club names and primary/secondary colours", true);

        TextView note = makeText("Changes apply across all three career saves.", 14, muted);
        note.setPadding(0, 0, 0, dp(14));
        page.addView(note);

        for (int i = 0; i < clubNames.length; i++) {
            final int club = i;
            LinearLayout box = makePanel();
            box.addView(makeClubIdentityRow(club));

            TextView colourInfo = makeText(
                    "Primary: " + colourName(primaryColours[club]) + "  •  Secondary: " + colourName(secondaryColours[club]),
                    13,
                    muted
            );
            colourInfo.setPadding(0, dp(6), 0, dp(8));
            box.addView(colourInfo);
            box.addView(makeButton("Edit Team", v -> showEditTeam(club)));
            page.addView(box);
        }

        Button resetAll = makeButton("Reset All Teams to Defaults", v -> confirmResetAllTeams());
        resetAll.setTextColor(idealButtonTextColour(danger));
        resetAll.setBackground(buttonBackground(danger));
        page.addView(resetAll);
    }

    private void showEditTeam(int club) {
        backAction = () -> showTeamEditor();
        LinearLayout page = createPage("Edit Team", clubNames[club], true);

        LinearLayout preview = makePanel();
        preview.addView(makeClubIdentityRow(club));
        page.addView(preview);

        TextView nameLabel = makeText("TEAM NAME", 13, muted);
        nameLabel.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(nameLabel);

        EditText nameInput = new EditText(this);
        nameInput.setText(clubNames[club]);
        nameInput.setTextColor(text);
        nameInput.setHintTextColor(muted);
        nameInput.setTextSize(18);
        nameInput.setSingleLine(true);
        nameInput.setBackground(rounded(panel));
        nameInput.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, dp(8), 0, dp(16));
        nameInput.setLayoutParams(inputParams);
        page.addView(nameInput);

        final int[] chosenPrimary = {primaryColours[club]};
        final int[] chosenSecondary = {secondaryColours[club]};

        Button primaryButton = makeButton("", null);
        Button secondaryButton = makeButton("", null);
        updateColourButton(primaryButton, "Primary", chosenPrimary[0]);
        updateColourButton(secondaryButton, "Secondary", chosenSecondary[0]);

        primaryButton.setOnClickListener(v -> {
            chosenPrimary[0] = nextPaletteColour(chosenPrimary[0]);
            updateColourButton(primaryButton, "Primary", chosenPrimary[0]);
        });

        secondaryButton.setOnClickListener(v -> {
            chosenSecondary[0] = nextPaletteColour(chosenSecondary[0]);
            updateColourButton(secondaryButton, "Secondary", chosenSecondary[0]);
        });

        page.addView(primaryButton);
        page.addView(secondaryButton);

        TextView hint = makeText("Tap either colour button to cycle through the palette.", 13, muted);
        hint.setPadding(0, 0, 0, dp(14));
        page.addView(hint);

        page.addView(makeAccentButton("Save Changes", v -> {
            String newName = nameInput.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Team name cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newName.length() > 28) newName = newName.substring(0, 28);
            clubNames[club] = newName;
            primaryColours[club] = chosenPrimary[0];
            secondaryColours[club] = chosenSecondary[0];
            saveEditorTeam(club);
            Toast.makeText(this, "Team updated.", Toast.LENGTH_SHORT).show();
            showTeamEditor();
        }));

        page.addView(makeButton("Reset This Team", v -> new BossDialog.Builder(this)
                .setTitle("Reset team?")
                .setMessage("Restore this club's default name and colours?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", (dialog, which) -> {
                    resetEditorTeam(club);
                    showTeamEditor();
                })
                .show()));
        page.addView(makeButton("Cancel", v -> showTeamEditor()));
    }

    private void updateColourButton(Button button, String label, int colour) {
        button.setText(label + ": " + colourName(colour) + "   •   Tap to change");
        GradientDrawable bg = rounded(colour);
        int luminance = (Color.red(colour) * 299 + Color.green(colour) * 587 + Color.blue(colour) * 114) / 1000;
        button.setTextColor(luminance > 170 ? Color.BLACK : Color.WHITE);
        button.setBackground(bg);
    }

    private int nextPaletteColour(int current) {
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] == current) return palette[(i + 1) % palette.length];
        }
        return palette[0];
    }

    private String colourName(int colour) {
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] == colour) return paletteNames[i];
        }
        return "Custom";
    }

    private void loadEditorData() {
        for (int i = 0; i < clubNames.length; i++) {
            clubNames[i] = prefs.getString("editor_name_" + i, defaultClubNames[i]);
            primaryColours[i] = prefs.getInt("editor_primary_" + i, defaultPrimary[i]);
            secondaryColours[i] = prefs.getInt("editor_secondary_" + i, defaultSecondary[i]);
        }
    }

    private void saveEditorTeam(int club) {
        prefs.edit()
                .putString("editor_name_" + club, clubNames[club])
                .putInt("editor_primary_" + club, primaryColours[club])
                .putInt("editor_secondary_" + club, secondaryColours[club])
                .apply();
    }

    private void resetEditorTeam(int club) {
        clubNames[club] = defaultClubNames[club];
        primaryColours[club] = defaultPrimary[club];
        secondaryColours[club] = defaultSecondary[club];
        saveEditorTeam(club);
    }

    private void confirmResetAllTeams() {
        new BossDialog.Builder(this)
                .setTitle("Reset all teams?")
                .setMessage("Every edited team name and colour will return to its BOSS XI default.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset All", (dialog, which) -> {
                    for (int i = 0; i < clubNames.length; i++) resetEditorTeam(i);
                    showTeamEditor();
                })
                .show();
    }

    private boolean slotExists(int slot) {
        return prefs.getBoolean(key(slot, "exists"), false);
    }

    private String key(int slot, String field) {
        return "save_" + slot + "_" + field;
    }

    private void saveCurrentGame() {
        if(matchInProgress) return; // Staged match state is committed only at full-time.

        if (selectedSlot < 0 || selectedClub < 0) return;

        prefs.edit()
                .putBoolean(key(selectedSlot, "exists"), true)
                .putInt(key(selectedSlot, "club"), selectedClub)
                .putString(key(selectedSlot, "manager_first"), managerFirstName)
                .putString(key(selectedSlot, "manager_last"), managerLastName)
                .putString(key(selectedSlot, "manager_dob"), managerDob)
                .putString(key(selectedSlot, "manager_gender"), managerGender)
                .putInt(key(selectedSlot, "manager_country_index"), managerCountryIndex)
                .putString(key(selectedSlot, "manager_league"), LEAGUE_OPTIONS[managerLeagueIndex])
                .putInt(key(selectedSlot, "manager_league_index"), managerLeagueIndex)
                .putInt(key(selectedSlot, "manager_wage"), managerWeeklyWageK)
                .putInt(key(selectedSlot, "manager_contract_years"), managerContractYears)
                .putInt(key(selectedSlot, "manager_tactical"), managerTactical)
                .putInt(key(selectedSlot, "manager_motivating"), managerMotivating)
                .putInt(key(selectedSlot, "manager_discipline"), managerDiscipline)
                .putInt(key(selectedSlot, "manager_player_knowledge"), managerPlayerKnowledge)
                .putInt(key(selectedSlot, "manager_youth"), managerYouth)
                .putInt(key(selectedSlot, "manager_negotiating"), managerNegotiating)
                .putInt(key(selectedSlot, "matchday"), matchday)
                .putString(key(selectedSlot, "date"), currentDate.toString())
                .putString(key(selectedSlot, "training"), trainingFocus)
                .putString(key(selectedSlot, "formation"), tacticFormation)
                .putString(key(selectedSlot, "playstyle"), playStyle)
                .putInt(key(selectedSlot, "budget"), currentTransferBudget)
                .putString(key(selectedSlot, "roles"), encode(playerRoleStatus))
                .putString(key(selectedSlot, "role_slots"), encode(playerSelectedSlot))
                .putString(key(selectedSlot, "role_pos"), encodeStrings(playerSelectedPosition))
                .putString(key(selectedSlot, "staff_am_name"), assistantManagerName)
                .putInt(key(selectedSlot, "staff_am_rating"), assistantManagerRating)
                .putString(key(selectedSlot, "staff_coach_name"), headCoachName)
                .putInt(key(selectedSlot, "staff_coach_rating"), headCoachRating)
                .putString(key(selectedSlot, "staff_scout_name"), chiefScoutName)
                .putInt(key(selectedSlot, "staff_scout_rating"), chiefScoutRating)
                .putString(key(selectedSlot, "club_match_gf"), encode(clubMatchGF))
                .putString(key(selectedSlot, "club_match_ga"), encode(clubMatchGA))
                .putString(key(selectedSlot, "played"), encode(played))
                .putString(key(selectedSlot, "won"), encode(won))
                .putString(key(selectedSlot, "drawn"), encode(drawn))
                .putString(key(selectedSlot, "lost"), encode(lost))
                .putString(key(selectedSlot, "gf"), encode(goalsFor))
                .putString(key(selectedSlot, "ga"), encode(goalsAgainst))
                .putString(key(selectedSlot, "points"), encode(points))
                .putString(key(selectedSlot, "p_apps"), encodePlayerField("apps"))
                .putString(key(selectedSlot, "p_goals"), encodePlayerField("goals"))
                .putString(key(selectedSlot, "p_assists"), encodePlayerField("assists"))
                .putString(key(selectedSlot, "p_overall"), encodePlayerField("overall"))
                .putString(key(selectedSlot, "p_pace"), encodePlayerField("pace"))
                .putString(key(selectedSlot, "p_tech"), encodePlayerField("tech"))
                .putString(key(selectedSlot, "p_pass"), encodePlayerField("pass"))
                .putString(key(selectedSlot, "p_finish"), encodePlayerField("finish"))
                .putString(key(selectedSlot, "p_def"), encodePlayerField("def"))
                .putString(key(selectedSlot, "p_phys"), encodePlayerField("phys"))
                .putString(key(selectedSlot, "p_fit"), encodePlayerField("fit"))
                .putString(key(selectedSlot, "p_morale"), encodePlayerField("morale"))
                .putString(key(selectedSlot, "p_transfer"), encodePlayerField("transfer"))
                .putString(key(selectedSlot, "p_loan"), encodePlayerField("loan"))
                .putString(key(selectedSlot, "p_team"), encodePlayerField("team"))
                .putString(key(selectedSlot, "p_onloan"), encodePlayerField("onloan"))
                .apply();
                          saveClassicState();
        saveFinanceStadiumState();
    }

    private void loadSave(int slot) {
        selectedSlot = slot;
        selectedClub = prefs.getInt(key(slot, "club"), 0);
        managerFirstName = prefs.getString(key(slot, "manager_first"), "");
        managerLastName = prefs.getString(key(slot, "manager_last"), "");
        managerDob = prefs.getString(key(slot, "manager_dob"), "01/01/1990");
        managerGender = prefs.getString(key(slot, "manager_gender"), "Male");
        managerCountryIndex = prefs.getInt(key(slot, "manager_country_index"), 0);
        managerLeagueIndex = prefs.getInt(key(slot, "manager_league_index"), 0);
        managerWeeklyWageK = prefs.getInt(key(slot, "manager_wage"), 32);
        managerContractYears = prefs.getInt(key(slot, "manager_contract_years"), 3);
        managerTactical = prefs.getInt(key(slot, "manager_tactical"), 12);
        managerMotivating = prefs.getInt(key(slot, "manager_motivating"), 11);
        managerDiscipline = prefs.getInt(key(slot, "manager_discipline"), 10);
        managerPlayerKnowledge = prefs.getInt(key(slot, "manager_player_knowledge"), 12);
        managerYouth = prefs.getInt(key(slot, "manager_youth"), 10);
        managerNegotiating = prefs.getInt(key(slot, "manager_negotiating"), 11);
        matchday = prefs.getInt(key(slot, "matchday"), 0);
        trainingFocus = prefs.getString(key(slot, "training"), "Balanced");
        tacticFormation = prefs.getString(key(slot, "formation"), "4-3-3");
        playStyle = prefs.getString(key(slot, "playstyle"), "Balanced");
        currentTransferBudget = prefs.getInt(key(slot, "budget"), budgets[selectedClub]);
        decode(prefs.getString(key(slot, "roles"), ""), playerRoleStatus);
        decode(prefs.getString(key(slot, "role_slots"), ""), playerSelectedSlot);
        decodeStrings(prefs.getString(key(slot, "role_pos"), ""), playerSelectedPosition);
        assistantManagerName = prefs.getString(key(slot, "staff_am_name"), assistantManagerName);
        assistantManagerRating = prefs.getInt(key(slot, "staff_am_rating"), assistantManagerRating);
        headCoachName = prefs.getString(key(slot, "staff_coach_name"), headCoachName);
        headCoachRating = prefs.getInt(key(slot, "staff_coach_rating"), headCoachRating);
        chiefScoutName = prefs.getString(key(slot, "staff_scout_name"), chiefScoutName);
        chiefScoutRating = prefs.getInt(key(slot, "staff_scout_rating"), chiefScoutRating);
        Arrays.fill(clubMatchGF, -1);
        Arrays.fill(clubMatchGA, -1);
        decodeAllowNegative(prefs.getString(key(slot, "club_match_gf"), ""), clubMatchGF, -1);
        decodeAllowNegative(prefs.getString(key(slot, "club_match_ga"), ""), clubMatchGA, -1);

        try {
            currentDate = LocalDate.parse(prefs.getString(key(slot, "date"), SEASON_START.toString()));
        } catch (Exception ignored) {
            currentDate = SEASON_START;
        }

        decode(prefs.getString(key(slot, "played"), ""), played);
        decode(prefs.getString(key(slot, "won"), ""), won);
        decode(prefs.getString(key(slot, "drawn"), ""), drawn);
        decode(prefs.getString(key(slot, "lost"), ""), lost);
        decode(prefs.getString(key(slot, "gf"), ""), goalsFor);
        decode(prefs.getString(key(slot, "ga"), ""), goalsAgainst);
        decode(prefs.getString(key(slot, "points"), ""), points);

        generatePlayers();
        decodePlayerField(prefs.getString(key(slot, "p_apps"), ""), "apps");
        decodePlayerField(prefs.getString(key(slot, "p_goals"), ""), "goals");
        decodePlayerField(prefs.getString(key(slot, "p_assists"), ""), "assists");
        decodePlayerField(prefs.getString(key(slot, "p_overall"), ""), "overall");
        decodePlayerField(prefs.getString(key(slot, "p_pace"), ""), "pace");
        decodePlayerField(prefs.getString(key(slot, "p_tech"), ""), "tech");
        decodePlayerField(prefs.getString(key(slot, "p_pass"), ""), "pass");
        decodePlayerField(prefs.getString(key(slot, "p_finish"), ""), "finish");
        decodePlayerField(prefs.getString(key(slot, "p_def"), ""), "def");
        decodePlayerField(prefs.getString(key(slot, "p_phys"), ""), "phys");
        decodePlayerField(prefs.getString(key(slot, "p_fit"), ""), "fit");
        decodePlayerField(prefs.getString(key(slot, "p_morale"), ""), "morale");
        decodePlayerField(prefs.getString(key(slot, "p_transfer"), ""), "transfer");
        decodePlayerField(prefs.getString(key(slot, "p_loan"), ""), "loan");
        decodePlayerField(prefs.getString(key(slot, "p_team"), ""), "team");
        decodePlayerField(prefs.getString(key(slot, "p_onloan"), ""), "onloan");
        ensureTacticsValid();
        loadClassicState(slot);
        loadFinanceStadiumState(slot);
    }

    private void confirmDeleteSave(int slot) {
        new BossDialog.Builder(this)
                .setTitle("Delete Save " + (slot + 1) + "?")
                .setMessage("This career progress will be permanently removed from this device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    clearSave(slot);
                    showMainMenu();
                })
                .show();
    }

    private void clearSave(int slot) {
        SharedPreferences.Editor editor = prefs.edit();
        String[] fields = {
                "exists", "club", "manager_first", "manager_last", "manager_dob", "manager_gender", "manager_country_index", "manager_league", "manager_league_index",
                "manager_wage", "manager_contract_years", "manager_tactical", "manager_motivating", "manager_discipline", "manager_player_knowledge", "manager_youth", "manager_negotiating",
                "matchday", "date", "training", "formation", "playstyle", "budget", "roles", "role_slots", "role_pos",
                "staff_am_name", "staff_am_rating", "staff_coach_name", "staff_coach_rating", "staff_scout_name", "staff_scout_rating",
                "club_match_gf", "club_match_ga", "played", "won", "drawn", "lost", "gf", "ga", "points",
                "p_apps", "p_goals", "p_assists", "p_overall", "p_pace", "p_tech", "p_pass", "p_finish", "p_def", "p_phys",
                "p_fit", "p_morale", "p_transfer", "p_loan", "p_team", "p_onloan"
        };
        for (String field : fields) editor.remove(key(slot, field));
        editor.apply();
        clearClassicSaveState(slot);
        clearFinanceStadiumState(slot);
    }

    private int getSavedPoints(int slot, int club) {
        int[] temp = new int[18];
        decode(prefs.getString(key(slot, "points"), ""), temp);
        return temp[club];
    }

    private String encode(int[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(values[i]);
        }
        return sb.toString();
    }

    private void decode(String encoded, int[] target) {
        Arrays.fill(target, 0);
        if (encoded == null || encoded.trim().isEmpty()) return;
        String[] parts = encoded.split(",");
        for (int i = 0; i < target.length && i < parts.length; i++) {
            try {
                target[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ignored) {
                target[i] = 0;
            }
        }
    }

    private void decodeAllowNegative(String encoded, int[] target, int defaultValue) {
        Arrays.fill(target, defaultValue);
        if (encoded == null || encoded.trim().isEmpty()) return;
        String[] parts = encoded.split(",");
        for (int i = 0; i < target.length && i < parts.length; i++) {
            try {
                target[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ignored) {
                target[i] = defaultValue;
            }
        }
    }


    private String encodePlayerField(String field) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) sb.append(",");
            Player p = players.get(i);
            int value;
            switch (field) {
                case "apps": value = p.appearances; break;
                case "goals": value = p.goals; break;
                case "assists": value = p.assists; break;
                case "overall": value = p.overall; break;
                case "pace": value = p.pace; break;
                case "tech": value = p.technique; break;
                case "pass": value = p.passing; break;
                case "finish": value = p.finishing; break;
                case "def": value = p.defending; break;
                case "phys": value = p.physical; break;
                case "fit": value = p.fitness; break;
                case "morale": value = p.morale; break;
                case "transfer": value = p.transferListed ? 1 : 0; break;
                case "loan": value = p.loanListed ? 1 : 0; break;
                case "team": value = p.team; break;
                case "onloan": value = p.onLoan ? 1 : 0; break;
                case "wage": value = p.weeklyWageK; break;
                case "contract": value = p.contractYears; break;
                case "injury": value = p.injuredWeeks; break;
                case "freerole": value = p.freeRole ? 1 : 0; break;
                case "forwardruns": value = p.forwardRuns ? 1 : 0; break;
                case "runball": value = p.runWithBall ? 1 : 0; break;
                case "longshots": value = p.longShots ? 1 : 0; break;
                default: value = 0;
            }
            sb.append(value);
        }
        return sb.toString();
    }

    private void decodePlayerField(String encoded, String field) {
        if (encoded == null || encoded.trim().isEmpty()) return;
        String[] parts = encoded.split(",");
        for (int i = 0; i < players.size() && i < parts.length; i++) {
            int value;
            try {
                value = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ignored) {
                continue;
            }
            Player p = players.get(i);
            switch (field) {
                case "apps": p.appearances = value; break;
                case "goals": p.goals = value; break;
                case "assists": p.assists = value; break;
                case "overall": p.overall = value; break;
                case "pace": p.pace = value; break;
                case "tech": p.technique = value; break;
                case "pass": p.passing = value; break;
                case "finish": p.finishing = value; break;
                case "def": p.defending = value; break;
                case "phys": p.physical = value; break;
                case "fit": p.fitness = value; break;
                case "morale": p.morale = value; break;
                case "transfer": p.transferListed = value == 1; break;
                case "loan": p.loanListed = value == 1; break;
                case "team": p.team = value; break;
                case "onloan": p.onLoan = value == 1; break;
                case "wage": p.weeklyWageK = value; break;
                case "contract": p.contractYears = value; break;
                case "injury": p.injuredWeeks = value; break;
                case "freerole": p.freeRole = value == 1; break;
                case "forwardruns": p.forwardRuns = value == 1; break;
                case "runball": p.runWithBall = value == 1; break;
                case "longshots": p.longShots = value == 1; break;
            }
        }
    }

    private void initialiseClassicCareerSystems() {
        resetClassicState();
        if (selectedClub >= 0) {
            for (Player p : players) if (p.team == selectedClub) scoutKnowledge[p.id] = 4;
        }
        addNews("WELCOME", "Welcome to " + clubNames[selectedClub], "The board expects " + boardExpectationText() + ". Build the squad, scout wisely and deliver results.");
        addNews("SCOUT", "Scouting department ready", "Attribute masking is ON. Scout unfamiliar players to reveal their full profile.");
    }

    private void resetClassicState() {
        attributeMasking = true;
        Arrays.fill(scoutKnowledge, 0);
        Arrays.fill(scoutDueRound, -1);
        Arrays.fill(shortlisted, false);
        Arrays.fill(managerNotes, "");
        comparePlayerId = -1;
        boardConfidence = 65;
        managerReputation = 45;
        wageBudgetK = selectedClub >= 0 ? 500 + strength[selectedClub] * 8 : 850;
        passingInstruction = "Mixed";
        tacklingInstruction = "Normal";
        pressingInstruction = false;
        offsideTrapInstruction = false;
        counterAttackInstruction = false;
        menBehindBallInstruction = false;
        wibWobShape = "Balanced";
        Arrays.fill(trainingIntensity, 2);
        inbox.clear();
    }

    private void saveClassicState() {
        if (selectedSlot < 0) return;
        prefs.edit()
                .putBoolean(key(selectedSlot, "classic_mask"), attributeMasking)
                .putString(key(selectedSlot, "classic_scout"), encode(scoutKnowledge))
                .putString(key(selectedSlot, "classic_scout_due"), encode(scoutDueRound))
                .putString(key(selectedSlot, "classic_shortlist"), encodeBooleans(shortlisted))
                .putString(key(selectedSlot, "classic_notes"), encodeStrings(managerNotes))
                .putInt(key(selectedSlot, "classic_board"), boardConfidence)
                .putInt(key(selectedSlot, "classic_rep"), managerReputation)
                .putInt(key(selectedSlot, "classic_wage_budget"), wageBudgetK)
                .putString(key(selectedSlot, "classic_pass"), passingInstruction)
                .putString(key(selectedSlot, "classic_tackle"), tacklingInstruction)
                .putBoolean(key(selectedSlot, "classic_press"), pressingInstruction)
                .putBoolean(key(selectedSlot, "classic_offside"), offsideTrapInstruction)
                .putBoolean(key(selectedSlot, "classic_counter"), counterAttackInstruction)
                .putBoolean(key(selectedSlot, "classic_menbehind"), menBehindBallInstruction)
                .putString(key(selectedSlot, "classic_wibwob"), wibWobShape)
                .putString(key(selectedSlot, "classic_training"), encode(trainingIntensity))
                .putString(key(selectedSlot, "classic_inbox"), encodeInbox())
                .putString(key(selectedSlot, "p_wage"), encodePlayerField("wage"))
                .putString(key(selectedSlot, "p_contract"), encodePlayerField("contract"))
                .putString(key(selectedSlot, "p_injury"), encodePlayerField("injury"))
                .putString(key(selectedSlot, "p_freerole"), encodePlayerField("freerole"))
                .putString(key(selectedSlot, "p_forwardruns"), encodePlayerField("forwardruns"))
                .putString(key(selectedSlot, "p_runball"), encodePlayerField("runball"))
                .putString(key(selectedSlot, "p_longshots"), encodePlayerField("longshots"))
                .apply();
    }

    private void loadClassicState(int slot) {
        attributeMasking = prefs.getBoolean(key(slot, "classic_mask"), true);
        Arrays.fill(scoutKnowledge, 0);
        Arrays.fill(scoutDueRound, -1);
        Arrays.fill(shortlisted, false);
        Arrays.fill(managerNotes, "");
        decode(prefs.getString(key(slot, "classic_scout"), ""), scoutKnowledge);
        decodeAllowNegative(prefs.getString(key(slot, "classic_scout_due"), ""), scoutDueRound, -1);
        decodeBooleans(prefs.getString(key(slot, "classic_shortlist"), ""), shortlisted);
        decodeStrings(prefs.getString(key(slot, "classic_notes"), ""), managerNotes);
        boardConfidence = prefs.getInt(key(slot, "classic_board"), 65);
        managerReputation = prefs.getInt(key(slot, "classic_rep"), 45);
        wageBudgetK = prefs.getInt(key(slot, "classic_wage_budget"), 500 + strength[selectedClub] * 8);
        passingInstruction = prefs.getString(key(slot, "classic_pass"), "Mixed");
        tacklingInstruction = prefs.getString(key(slot, "classic_tackle"), "Normal");
        pressingInstruction = prefs.getBoolean(key(slot, "classic_press"), false);
        offsideTrapInstruction = prefs.getBoolean(key(slot, "classic_offside"), false);
        counterAttackInstruction = prefs.getBoolean(key(slot, "classic_counter"), false);
        menBehindBallInstruction = prefs.getBoolean(key(slot, "classic_menbehind"), false);
        wibWobShape = prefs.getString(key(slot, "classic_wibwob"), "Balanced");
        String savedTraining = prefs.getString(key(slot, "classic_training"), "");
        if (savedTraining == null || savedTraining.isEmpty()) Arrays.fill(trainingIntensity, 2);
        else decode(savedTraining, trainingIntensity);
        decodeInbox(prefs.getString(key(slot, "classic_inbox"), ""));
        decodePlayerField(prefs.getString(key(slot, "p_wage"), ""), "wage");
        decodePlayerField(prefs.getString(key(slot, "p_contract"), ""), "contract");
        decodePlayerField(prefs.getString(key(slot, "p_injury"), ""), "injury");
        decodePlayerField(prefs.getString(key(slot, "p_freerole"), ""), "freerole");
        decodePlayerField(prefs.getString(key(slot, "p_forwardruns"), ""), "forwardruns");
        decodePlayerField(prefs.getString(key(slot, "p_runball"), ""), "runball");
        decodePlayerField(prefs.getString(key(slot, "p_longshots"), ""), "longshots");
        for (Player p : players) if (p.team == selectedClub) scoutKnowledge[p.id] = 4;
        if (inbox.isEmpty()) addNews("WELCOME", "Career loaded", "Your inbox will collect scouting, board, transfer and match news.");
    }

    private void clearClassicSaveState(int slot) {
        String[] fields = {
                "classic_mask", "classic_scout", "classic_scout_due", "classic_shortlist", "classic_notes",
                "classic_board", "classic_rep", "classic_wage_budget", "classic_pass", "classic_tackle",
                "classic_press", "classic_offside", "classic_counter", "classic_menbehind", "classic_wibwob",
                "classic_training", "classic_inbox", "p_wage", "p_contract", "p_injury", "p_freerole",
                "p_forwardruns", "p_runball", "p_longshots"
        };
        SharedPreferences.Editor e = prefs.edit();
        for (String field : fields) e.remove(key(slot, field));
        e.apply();
    }

    private String encodeBooleans(boolean[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i] ? '1' : '0');
        }
        return sb.toString();
    }

    private void decodeBooleans(String encoded, boolean[] target) {
        Arrays.fill(target, false);
        if (encoded == null || encoded.isEmpty()) return;
        String[] parts = encoded.split(",");
        for (int i = 0; i < target.length && i < parts.length; i++) target[i] = "1".equals(parts[i]);
    }

    private String encodeStrings(String[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append('\u001E');
            String value = values[i] == null ? "" : values[i].replace("\u001E", " ").replace("\u001F", " ");
            sb.append(value);
        }
        return sb.toString();
    }

    private void decodeStrings(String encoded, String[] target) {
        Arrays.fill(target, "");
        if (encoded == null || encoded.isEmpty()) return;
        String[] parts = encoded.split("\u001E", -1);
        for (int i = 0; i < target.length && i < parts.length; i++) target[i] = parts[i];
    }

    private void addNews(String category, String title, String body) {
        inbox.add(0, new NewsItem(category, title, body));
        while (inbox.size() > 30) inbox.remove(inbox.size() - 1);
    }

    private String encodeInbox() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inbox.size(); i++) {
            if (i > 0) sb.append('\u001E');
            NewsItem n = inbox.get(i);
            sb.append(safeNews(n.category)).append('\u001F').append(safeNews(n.title)).append('\u001F').append(safeNews(n.body));
        }
        return sb.toString();
    }

    private String safeNews(String s) {
        return s == null ? "" : s.replace("\u001E", " ").replace("\u001F", " ");
    }

    private void decodeInbox(String encoded) {
        inbox.clear();
        if (encoded == null || encoded.isEmpty()) return;
        String[] items = encoded.split("\u001E");
        for (String item : items) {
            String[] p = item.split("\u001F", 3);
            if (p.length == 3) inbox.add(new NewsItem(p[0], p[1], p[2]));
        }
    }

    private void showInbox() {
        backAction = () -> showDashboard();
        LinearLayout page = createPage("Inbox", "News, scouting, transfers, board and match reports", true);

        if (selectedClub >= 0 && matchday < 34) {
            LinearLayout report = makePanel();
            report.setBackground(buttonBackground(Color.rgb(20, 72, 88)));

            TextView category = makeText("ASSISTANT MANAGER • PRE-MATCH REPORT", 12, accent);
            category.setTypeface(Typeface.DEFAULT_BOLD);
            report.addView(category);

            int opponent = leagueOpponentForRound(matchday);
            boolean atHome = selectedHomeForRound(matchday);
            TextView title = makeText(assistantManagerName + " • " + clubNames[opponent], 18, text);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setPadding(0, dp(4), 0, dp(6));
            report.addView(title);

            report.addView(makeText(buildAssistantPregameReport(opponent, atHome), 14, text));
            page.addView(report);
        }

        if (inbox.isEmpty()) page.addView(makeText("No news yet.", 16, muted));
        for (NewsItem n : inbox) {
            LinearLayout box = makePanel();
            TextView cat = makeText(n.category, 12, accent);
            cat.setTypeface(Typeface.DEFAULT_BOLD);
            box.addView(cat);
            TextView title = makeText(n.title, 17, text);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setPadding(0, dp(4), 0, dp(5));
            box.addView(title);
            box.addView(makeText(n.body, 14, muted));
            page.addView(box);
        }
    }

    private String buildAssistantPregameReport(int opponent, boolean atHome) {
        int diff = strength[selectedClub] - strength[opponent];
        String assessment;
        if (diff >= 10) assessment = "We should expect to control most phases of the match.";
        else if (diff >= 3) assessment = "We have a slight quality edge, but they can punish loose possession.";
        else if (diff <= -10) assessment = "They are significantly stronger on paper, so our defensive structure will be important.";
        else if (diff <= -3) assessment = "They have a slight quality advantage. Transitions and set pieces could be important.";
        else assessment = "The teams look closely matched. Small tactical details could decide it.";

        String suggestedMentality;
        if (diff >= 8 && atHome) suggestedMentality = "Attacking";
        else if (diff <= -7) suggestedMentality = "Defensive";
        else suggestedMentality = "Balanced";

        int unavailable = 0;
        int lowFitness = 0;
        for (Player p : players) {
            if (p.team != selectedClub) continue;
            if (p.injuredWeeks > 0) unavailable++;
            else if (p.fitness < 78) lowFitness++;
        }

        String venue = atHome ? "Home" : "Away";
        String readiness = countRole(2) == 11
                ? "Your starting XI is complete."
                : "Your starting XI currently has " + countRole(2) + "/11 players selected.";

        return "Next opponent: " + clubNames[opponent] + " • " + venue + "\n"
                + "Opponent strength: " + strength[opponent] + "/100 • Our strength: " + strength[selectedClub] + "/100\n"
                + assessment + "\n"
                + "Suggested mentality: " + suggestedMentality + " • Current formation: " + tacticFormation + "\n"
                + "Unavailable: " + unavailable + " • Low fitness: " + lowFitness + "\n"
                + readiness + "\n"
                + "Report prepared by " + assistantManagerName + " • Staff rating " + assistantManagerRating + "/100.";
    }

    private void showScoutCentre() {
        backAction = () -> showDashboard();
        LinearLayout page = createPage("Scouting Centre", attributeMasking ? "Attribute masking ON" : "Attribute masking OFF", true);

        page.addView(makeButton((attributeMasking ? "✓  " : "") + "Attribute Masking / Fog of War", v -> {
            attributeMasking = !attributeMasking;
            saveCurrentGame();
            showScoutCentre();
        }));

        LinearLayout info = makePanel();
        info.addView(makeText("SCOUTING KNOWLEDGE", 13, accent));
        info.addView(makeText("Unknown → Basic → Partial → Scout Report → Full.\nA scout assignment completes after the next matchday.", 15, text));
        page.addView(info);

        int pending = 0;
        for (int id = 0; id < scoutDueRound.length; id++) if (scoutDueRound[id] >= 0) pending++;
        page.addView(makeText("Pending assignments: " + pending, 14, muted));

        TextView shortlistTitle = makeText("\nSHORTLIST", 13, accent);
        shortlistTitle.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(shortlistTitle);
        boolean any = false;
        for (Player p : players) {
            if (!shortlisted[p.id]) continue;
            any = true;
            page.addView(makeButton(p.position + "  " + p.name + " • " + clubNames[p.team] + " • " + knownOverallLabel(p), v -> showPlayerProfile(p.id, p.team)));
        }
        if (!any) page.addView(makeText("Your shortlist is empty.", 15, muted));

        page.addView(makeButton("Browse All Clubs & Players", v -> showAllTeamsPlayers()));
    }

    private void assignScout(Player p) {
        if (p.team == selectedClub) return;
        if (scoutDueRound[p.id] >= 0) {
            Toast.makeText(this, "A scout is already watching this player.", Toast.LENGTH_SHORT).show();
            return;
        }
        scoutKnowledge[p.id] = Math.max(1, scoutKnowledge[p.id]);
        scoutDueRound[p.id] = matchday + 1;
        addNews("SCOUT", "Scout assigned to " + p.name, "A full report is expected after the next matchday.");
        saveCurrentGame();
        Toast.makeText(this, "Scout assigned.", Toast.LENGTH_SHORT).show();
        showPlayerProfile(p.id, p.team);
    }

    private void processScoutingAssignments() {
        for (Player p : players) {
            if (scoutDueRound[p.id] >= 0 && matchday >= scoutDueRound[p.id]) {
                scoutDueRound[p.id] = -1;
                scoutKnowledge[p.id] = 4;
                addNews("SCOUT REPORT", "Report complete: " + p.name,
                        "OVR " + p.overall + " • Potential " + classicAbilityBand(p.potentialAbility)
                                + " • Consistency " + traitLabel(p.consistency)
                                + " • Injury risk " + inverseTraitLabel(p.injuryProneness));
            }
        }
    }


    private void showFinances() {
        backAction = () -> showDashboard();
        LinearLayout page = createPage(
                "Finances",
                clubNames[selectedClub] + " • detailed club accounts",
                true
        );

        int playerWages = totalPlayerWagesK();
        int staffWages = totalStaffWagesK();
        int totalWeeklyWages = playerWages + staffWages + managerWeeklyWageK;
        int currentProfit = financeCurrentMonthIncomeK - financeCurrentMonthExpenseK;
        int lastMonthProfit = financeLastMonthIncomeK - financeLastMonthExpenseK;
        int seasonProfit = financeSeasonIncomeK - financeSeasonExpenseK;
        int lastYearProfit = financeLastYearIncomeK - financeLastYearExpenseK;

        LinearLayout overview = makePanel();
        TextView overviewTitle = makeText("FINANCIAL OVERVIEW", 13, accent);
        overviewTitle.setTypeface(Typeface.DEFAULT_BOLD);
        overview.addView(overviewTitle);
        overview.addView(makeText(
                "Bank balance          " + moneyK(financeBalanceK) + "\n"
                        + "Transfer budget       €" + currentTransferBudget + "m\n"
                        + "Weekly player wages   " + moneyK(playerWages) + "\n"
                        + "Weekly staff wages    " + moneyK(staffWages + managerWeeklyWageK) + "\n"
                        + "Total weekly salaries " + moneyK(totalWeeklyWages) + "\n"
                        + "Wage budget           " + moneyK(wageBudgetK) + "/week\n"
                        + "Financial status      " + financeStatus(totalWeeklyWages),
                15,
                text
        ));
        page.addView(overview);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(true);
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setMinimumWidth(dp(760));

        table.addView(financePeriodRow(
                "ITEM", "THIS MONTH", "LAST MONTH", "THIS YEAR", "LAST YEAR", true
        ));
        table.addView(financePeriodRow(
                "Income",
                moneyK(financeCurrentMonthIncomeK),
                moneyK(financeLastMonthIncomeK),
                moneyK(financeSeasonIncomeK),
                moneyK(financeLastYearIncomeK),
                false
        ));
        table.addView(financePeriodRow(
                "Expenditure",
                moneyK(financeCurrentMonthExpenseK),
                moneyK(financeLastMonthExpenseK),
                moneyK(financeSeasonExpenseK),
                moneyK(financeLastYearExpenseK),
                false
        ));
        table.addView(financePeriodRow(
                "Profit / (Loss)",
                moneyK(currentProfit),
                moneyK(lastMonthProfit),
                moneyK(seasonProfit),
                moneyK(lastYearProfit),
                false
        ));
        table.addView(financePeriodRow(
                "Balance", moneyK(financeBalanceK), "—", "—", "—", false
        ));
        table.addView(financePeriodRow(
                "Transfer Funds", "€" + currentTransferBudget + "m", "—", "—", "—", false
        ));
        scroll.addView(table);

        LinearLayout periodPanel = makePanel();
        TextView periodTitle = makeText("PERIOD ACCOUNTS", 13, accent);
        periodTitle.setTypeface(Typeface.DEFAULT_BOLD);
        periodPanel.addView(periodTitle);
        periodPanel.addView(scroll);
        page.addView(periodPanel);

        LinearLayout income = makePanel();
        TextView incomeTitle = makeText("INCOME • CURRENT SEASON", 13, accent);
        incomeTitle.setTypeface(Typeface.DEFAULT_BOLD);
        income.addView(incomeTitle);
        income.addView(financeLine("Gate receipts", financeGateReceiptsK));
        income.addView(financeLine("TV & competition distributions", financeBroadcastK));
        income.addView(financeLine("Sponsorship & commercial deals", financeSponsorshipK));
        income.addView(financeLine("Merchandise / club shop", financeMerchandiseK));
        income.addView(financeLine("Player sales", financePlayerSalesK));
        income.addView(financeLine("Museum, tours & other income", financeOtherIncomeK));
        page.addView(income);

        LinearLayout expense = makePanel();
        TextView expenseTitle = makeText("EXPENDITURE • CURRENT SEASON", 13, accent);
        expenseTitle.setTypeface(Typeface.DEFAULT_BOLD);
        expense.addView(expenseTitle);
        expense.addView(financeLine("Player salaries", financePlayerWagesK));
        expense.addView(financeLine("Manager & staff salaries", financeStaffWagesK));
        expense.addView(financeLine("Transfer / loan fees", financeTransferSpendK));
        expense.addView(financeLine("Stadium & infrastructure", financeStadiumSpendK));
        expense.addView(financeLine("Ground maintenance & matchday ops", financeMaintenanceK));
        expense.addView(financeLine("Travel & logistics", financeTravelK));
        expense.addView(financeLine("Academy, scouting & development", financeYouthScoutingK));
        expense.addView(financeLine("Interest & other costs", financeOtherExpenseK));
        page.addView(expense);

        LinearLayout wages = makePanel();
        TextView wagesTitle = makeText("SALARY COMMITMENTS", 13, accent);
        wagesTitle.setTypeface(Typeface.DEFAULT_BOLD);
        wages.addView(wagesTitle);
        wages.addView(makeText(
                "Players: " + moneyK(playerWages) + "/week • " + moneyK(playerWages * 52) + "/year\n"
                        + "Manager: " + moneyK(managerWeeklyWageK) + "/week\n"
                        + "Coaching / recruitment staff: " + moneyK(staffWages) + "/week\n"
                        + "Combined salaries: " + moneyK(totalWeeklyWages) + "/week • "
                        + moneyK(totalWeeklyWages * 52) + "/year",
                14,
                text
        ));

        ArrayList<Player> wageList = new ArrayList<>();
        for (Player p : players) if (p.team == selectedClub) wageList.add(p);
        Collections.sort(wageList, (a, b) -> Integer.compare(b.weeklyWageK, a.weeklyWageK));
        TextView topEarners = makeText("TOP EARNERS", 12, muted);
        topEarners.setTypeface(Typeface.DEFAULT_BOLD);
        topEarners.setPadding(0, dp(10), 0, dp(4));
        wages.addView(topEarners);
        for (int i = 0; i < Math.min(8, wageList.size()); i++) {
            Player p = wageList.get(i);
            wages.addView(makeText(
                    (i + 1) + ". " + p.name + " • " + p.position + " • " + moneyK(p.weeklyWageK) + "/week",
                    13,
                    text
            ));
        }
        page.addView(wages);

        int weeksPlayed = Math.max(1, matchday);
        int averageWeeklyNet = seasonProfit / weeksPlayed;
        int projectedSeasonProfit = seasonProfit + averageWeeklyNet * Math.max(0, 34 - matchday);
        LinearLayout forecast = makePanel();
        TextView forecastTitle = makeText("FORECAST & CONTROL", 13, accent);
        forecastTitle.setTypeface(Typeface.DEFAULT_BOLD);
        forecast.addView(forecastTitle);
        forecast.addView(makeText(
                "Average weekly net: " + moneyK(averageWeeklyNet) + "\n"
                        + "Projected league-season result: " + moneyK(projectedSeasonProfit) + "\n"
                        + "Stadium commercial potential: " + moneyK(estimatedStadiumCommercialIncomeK()) + "/home match\n"
                        + "Next major facility project: "
                        + (stadiumProjectWeeks > 0 ? stadiumProjectType + " • " + stadiumProjectWeeks + " week(s) remaining" : "None"),
                14,
                text
        ));
        page.addView(forecast);

        page.addView(makeButton("🏟  Stadium & Infrastructure", v -> showStadiumCentre()));
    }

    private LinearLayout financePeriodRow(String label, String a, String b, String c, String d, boolean header) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(6), dp(7), dp(6), dp(7));
        row.setBackground(rounded(header ? panelLight : panel));
        row.addView(financePeriodCell(label, 1.45f, header));
        row.addView(financePeriodCell(a, 1f, header));
        row.addView(financePeriodCell(b, 1f, header));
        row.addView(financePeriodCell(c, 1f, header));
        row.addView(financePeriodCell(d, 1f, header));
        return row;
    }

    private TextView financePeriodCell(String value, float weight, boolean header) {
        TextView cell = makeText(value, header ? 11 : 12, header ? accent : text);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setSingleLine(true);
        if (header) cell.setTypeface(Typeface.DEFAULT_BOLD);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        return cell;
    }

    private TextView financeLine(String label, int amountK) {
        TextView row = makeText(label + "   •   " + moneyK(amountK), 14, text);
        row.setPadding(0, dp(3), 0, dp(3));
        return row;
    }

    private String moneyK(int amountK) {
        long abs = Math.abs((long) amountK);
        String body;
        if (abs >= 1000) body = String.format(Locale.UK, "€%.1fm", abs / 1000.0);
        else body = "€" + abs + "k";
        return amountK < 0 ? "(" + body + ")" : body;
    }

    private int totalPlayerWagesK() {
        int total = 0;
        for (Player p : players) if (p.team == selectedClub) total += Math.max(0, p.weeklyWageK);
        return total;
    }

    private int totalStaffWagesK() {
        return 12
                + Math.max(8, assistantManagerRating / 3)
                + Math.max(8, headCoachRating / 3)
                + Math.max(8, chiefScoutRating / 3);
    }

    private String financeStatus(int totalWeeklyWages) {
        int monthsCover = totalWeeklyWages <= 0 ? 99 : financeBalanceK / Math.max(1, totalWeeklyWages * 4);
        if (financeBalanceK < 0) return "Critical • operating in debt";
        if (monthsCover < 3) return "Under pressure • less than 3 months salary cover";
        if (monthsCover < 7) return "Tight • watch spending";
        if (financeSeasonIncomeK < financeSeasonExpenseK && matchday >= 6) return "Stable cash, but currently loss-making";
        return "Secure";
    }

    private void processFinanceWeek() {
        if (selectedClub < 0) return;
        rollFinanceMonthIfNeeded();

        boolean home = liveHome == selectedClub;
        int attendance = home ? calculateHomeAttendance() : 0;
        int ticketPrice = 20 + Math.max(0, strength[selectedClub] - 60) / 3 + stadiumHospitalityLevel;
        int gate = home ? attendance * ticketPrice / 1000 : 0;
        int broadcast = 135 + strength[selectedClub] * 3 + (liveHomeGoals + liveAwayGoals > 4 ? 25 : 0);
        int sponsorship = 105 + strength[selectedClub] * 2 + stadiumMediaLevel * 24;
        int merchandise = 35 + strength[selectedClub] + stadiumShopLevel * 24
                + (home ? attendance * Math.max(1, stadiumFanZoneLevel) / 2500 : 0);
        int otherIncome = stadiumMuseumLevel * 14
                + (home ? attendance * (stadiumBarsLevel + stadiumRestaurantsLevel + stadiumHospitalityLevel) / 2200 : 0);

        int playerWages = totalPlayerWagesK();
        int staffWages = totalStaffWagesK() + managerWeeklyWageK;
        int maintenance = 35 + stadiumCapacity / 210 + stadiumTrainingLevel * 8
                + stadiumMedicalLevel * 7 + stadiumGymLevel * 6;
        int travel = home ? 24 : 90 + Math.max(0, stadiumParkingLevel - 2) * 2;
        int youthScouting = 35 + stadiumYouthLevel * 12 + chiefScoutRating / 5;
        int otherExpense = financeBalanceK < 0 ? Math.max(15, Math.abs(financeBalanceK) / 400) : 8;

        int weeklyIncome = gate + broadcast + sponsorship + merchandise + otherIncome;
        int weeklyExpense = playerWages + staffWages + maintenance + travel + youthScouting + otherExpense;

        financeGateReceiptsK += gate;
        financeBroadcastK += broadcast;
        financeSponsorshipK += sponsorship;
        financeMerchandiseK += merchandise;
        financeOtherIncomeK += otherIncome;

        financePlayerWagesK += playerWages;
        financeStaffWagesK += staffWages;
        financeMaintenanceK += maintenance;
        financeTravelK += travel;
        financeYouthScoutingK += youthScouting;
        financeOtherExpenseK += otherExpense;

        financeCurrentMonthIncomeK += weeklyIncome;
        financeCurrentMonthExpenseK += weeklyExpense;
        financeSeasonIncomeK += weeklyIncome;
        financeSeasonExpenseK += weeklyExpense;
        financeBalanceK += weeklyIncome - weeklyExpense;

        if (home) {
            stadiumHomeGames++;
            stadiumAverageAttendance = stadiumHomeGames <= 1
                    ? attendance
                    : Math.round(((stadiumAverageAttendance * (stadiumHomeGames - 1)) + attendance) / (float) stadiumHomeGames);
        }

        if (financeBalanceK < -10000) {
            addNews("FINANCE", "Club finances under pressure",
                    "The club is carrying a significant negative cash balance. Reduce wages or delay infrastructure work.");
        }
    }

    private void rollFinanceMonthIfNeeded() {
        int stamp = currentDate.getYear() * 100 + currentDate.getMonthValue();
        if (financeMonthStamp == 0) {
            financeMonthStamp = stamp;
            return;
        }
        if (stamp != financeMonthStamp) {
            financeLastMonthIncomeK = financeCurrentMonthIncomeK;
            financeLastMonthExpenseK = financeCurrentMonthExpenseK;
            financeCurrentMonthIncomeK = 0;
            financeCurrentMonthExpenseK = 0;
            financeMonthStamp = stamp;
        }
    }

    private void rollFinanceSeason() {
        financeLastYearIncomeK = financeSeasonIncomeK;
        financeLastYearExpenseK = financeSeasonExpenseK;
        financeSeasonIncomeK = 0;
        financeSeasonExpenseK = 0;
        financeGateReceiptsK = 0;
        financeBroadcastK = 0;
        financeSponsorshipK = 0;
        financeMerchandiseK = 0;
        financePlayerSalesK = 0;
        financeOtherIncomeK = 0;
        financePlayerWagesK = 0;
        financeStaffWagesK = 0;
        financeTransferSpendK = 0;
        financeStadiumSpendK = 0;
        financeMaintenanceK = 0;
        financeTravelK = 0;
        financeYouthScoutingK = 0;
        financeOtherExpenseK = 0;
    }

    private void recordTransferPurchase(int feeM) {
        int costK = Math.max(0, feeM) * 1000;
        financeTransferSpendK += costK;
        financeSeasonExpenseK += costK;
        financeCurrentMonthExpenseK += costK;
        financeBalanceK -= costK;
    }

    private void recordTransferLoanFee(int feeM) {
        recordTransferPurchase(feeM);
    }

    private void recordInfrastructureSpend(int costK) {
        financeStadiumSpendK += Math.max(0, costK);
        financeSeasonExpenseK += Math.max(0, costK);
        financeCurrentMonthExpenseK += Math.max(0, costK);
        financeBalanceK -= Math.max(0, costK);
    }

    private int calculateHomeAttendance() {
        double formFactor = points[selectedClub] > played[selectedClub] * 1.55 ? 0.05 : 0.0;
        double demand = 0.55 + (strength[selectedClub] - 60) / 100.0
                + stadiumFanZoneLevel * 0.018 + stadiumParkingLevel * 0.012 + formFactor;
        demand = Math.max(0.48, Math.min(0.97, demand));
        return Math.max(2500, Math.min(stadiumCapacity, (int) Math.round(stadiumCapacity * demand)));
    }

    private int estimatedStadiumCommercialIncomeK() {
        int attendance = stadiumAverageAttendance > 0 ? stadiumAverageAttendance : calculateHomeAttendance();
        return 35 + stadiumShopLevel * 24
                + attendance * (stadiumBarsLevel + stadiumRestaurantsLevel + stadiumHospitalityLevel + stadiumFanZoneLevel) / 2200;
    }

    private void showStadiumCentre() {
        backAction = () -> showDashboard();
        LinearLayout page = createPage(
                "Stadium & Infrastructure",
                stadiumName + " • long-term club development",
                true
        );

        LinearLayout ground = makePanel();
        TextView title = makeText("GROUND INFORMATION", 13, accent);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        ground.addView(title);
        int utilisation = stadiumCapacity <= 0 || stadiumAverageAttendance <= 0
                ? 0 : Math.min(100, Math.round(stadiumAverageAttendance * 100f / stadiumCapacity));
        ground.addView(makeText(
                "Stadium              " + stadiumName + "\n"
                        + "Total capacity        " + stadiumCapacity + "\n"
                        + "Total seats           " + stadiumSeatedCapacity + "\n"
                        + "Expansion limit       " + stadiumExpansionLimit + "\n"
                        + "Average attendance    " + (stadiumAverageAttendance <= 0 ? "No home data yet" : stadiumAverageAttendance + " • " + utilisation + "% full") + "\n"
                        + "Roof / covered        " + (stadiumCovered ? "Yes" : "No") + "\n"
                        + "Undersoil heating     " + (stadiumUndersoilHeating ? "Yes" : "No") + "\n"
                        + "Estimated matchday commercial income  " + moneyK(estimatedStadiumCommercialIncomeK()),
                14,
                text
        ));
        page.addView(ground);

        LinearLayout project = makePanel();
        TextView projectTitle = makeText("CURRENT DEVELOPMENT PROJECT", 13, accent);
        projectTitle.setTypeface(Typeface.DEFAULT_BOLD);
        project.addView(projectTitle);
        if (stadiumProjectWeeks > 0) {
            project.addView(makeText(
                    stadiumProjectType + "\n"
                            + "Time remaining: " + stadiumProjectWeeks + " week(s)\n"
                            + "Committed cost: " + moneyK(stadiumProjectCostK)
                            + (stadiumProjectCapacityGain > 0 ? "\nCapacity gain on completion: +" + stadiumProjectCapacityGain : ""),
                    14,
                    text
            ));
        } else {
            project.addView(makeText("No major project is currently active.", 14, muted));
        }
        page.addView(project);

        LinearLayout stands = makePanel();
        TextView standsTitle = makeText("STANDS & SEATING", 13, accent);
        standsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        stands.addView(standsTitle);
        stands.addView(makeText(
                "North Stand • " + standLevelLabel(stadiumNorthLevel) + "\n"
                        + "East Stand  • " + standLevelLabel(stadiumEastLevel) + "\n"
                        + "South Stand • " + standLevelLabel(stadiumSouthLevel) + "\n"
                        + "West Stand  • " + standLevelLabel(stadiumWestLevel),
                14,
                text
        ));
        stands.addView(makeButton(standUpgradeLabel("North", stadiumNorthLevel), v -> confirmStandUpgrade("North", 101, stadiumNorthLevel)));
        stands.addView(makeButton(standUpgradeLabel("East", stadiumEastLevel), v -> confirmStandUpgrade("East", 102, stadiumEastLevel)));
        stands.addView(makeButton(standUpgradeLabel("South", stadiumSouthLevel), v -> confirmStandUpgrade("South", 103, stadiumSouthLevel)));
        stands.addView(makeButton(standUpgradeLabel("West", stadiumWestLevel), v -> confirmStandUpgrade("West", 104, stadiumWestLevel)));
        page.addView(stands);

        LinearLayout facilities = makePanel();
        TextView facilitiesTitle = makeText("CLUB INFRASTRUCTURE", 13, accent);
        facilitiesTitle.setTypeface(Typeface.DEFAULT_BOLD);
        facilities.addView(facilitiesTitle);
        facilities.addView(makeText(
                "Facilities directly affect player development, recovery, attendance and commercial income. "
                        + "Only one major infrastructure project can run at a time.",
                13,
                muted
        ));
        for (int code = 1; code <= 12; code++) {
            facilities.addView(makeFacilityUpgradeButton(code));
        }
        page.addView(facilities);

        LinearLayout newGround = makePanel();
        TextView newTitle = makeText("NEW STADIUM", 13, accent);
        newTitle.setTypeface(Typeface.DEFAULT_BOLD);
        newGround.addView(newTitle);
        int newCapacity = Math.max(stadiumCapacity + 12000, (int) Math.round(stadiumCapacity * 1.35));
        int newCost = Math.max(65000, stadiumCapacity * 2);
        newGround.addView(makeText(
                "A completely new ground resets the four stands to a modern multi-tier standard, "
                        + "adds premium infrastructure and creates a much higher future expansion ceiling.\n\n"
                        + "Planned capacity: " + newCapacity + "\n"
                        + "Estimated build: 30 match-weeks\n"
                        + "Estimated cost: " + moneyK(newCost),
                14,
                text
        ));
        newGround.addView(makeAccentButton("🏗  Build New Stadium", v -> confirmNewStadium()));
        page.addView(newGround);

        page.addView(makeButton("💰  Open Finances", v -> showFinances()));
    }

    private String standLevelLabel(int level) {
        if (level <= 1) return "Level 1 • Single-tier stand";
        if (level == 2) return "Level 2 • Extended single-tier";
        if (level == 3) return "Level 3 • Two-tier stand";
        if (level == 4) return "Level 4 • Modern premium stand";
        return "Level 5 • Elite multi-tier stand";
    }

    private String standUpgradeLabel(String name, int level) {
        if (level >= 5) return name + " Stand • MAXIMUM LEVEL";
        int gain = 1200 + level * 900;
        int cost = 2500 + level * 3500 + stadiumCapacity / 25;
        return "Upgrade " + name + " Stand • +" + gain + " seats • " + moneyK(cost);
    }

    private void confirmStandUpgrade(String name, int code, int level) {
        if (level >= 5) {
            Toast.makeText(this, name + " Stand is already at maximum level.", Toast.LENGTH_SHORT).show();
            return;
        }
        int gain = 1200 + level * 900;
        int available = Math.max(0, stadiumExpansionLimit - stadiumCapacity);
        gain = Math.min(gain, available);
        if (gain <= 0) {
            Toast.makeText(this, "The current ground has reached its expansion limit.", Toast.LENGTH_LONG).show();
            return;
        }
        int cost = 2500 + level * 3500 + stadiumCapacity / 25;
        int weeks = 4 + level * 2;
        startStadiumProject(name + " Stand upgrade", code, cost, weeks, gain);
    }

    private View makeFacilityUpgradeButton(int code) {
        int level = facilityLevel(code);
        String name = facilityName(code);
        if (level >= 5) {
            Button max = makeButton(name + " • Level 5 ELITE • MAX", v -> {});
            max.setEnabled(false);
            return max;
        }
        int cost = facilityUpgradeCostK(code, level);
        int weeks = facilityUpgradeWeeks(code, level);
        return makeButton(
                name + " • " + facilityLevelLabel(level)
                        + "\nUpgrade: " + moneyK(cost) + " • " + weeks + " week(s) • " + facilityImpact(code),
                v -> startStadiumProject(name + " upgrade", code, cost, weeks, 0)
        );
    }

    private int facilityLevel(int code) {
        switch (code) {
            case 1: return stadiumTrainingLevel;
            case 2: return stadiumMedicalLevel;
            case 3: return stadiumGymLevel;
            case 4: return stadiumYouthLevel;
            case 5: return stadiumShopLevel;
            case 6: return stadiumBarsLevel;
            case 7: return stadiumRestaurantsLevel;
            case 8: return stadiumHospitalityLevel;
            case 9: return stadiumFanZoneLevel;
            case 10: return stadiumParkingLevel;
            case 11: return stadiumMediaLevel;
            case 12: return stadiumMuseumLevel;
            default: return 1;
        }
    }

    private String facilityName(int code) {
        switch (code) {
            case 1: return "Training Centre";
            case 2: return "Medical & Physio Centre";
            case 3: return "Performance Gym";
            case 4: return "Youth Academy";
            case 5: return "Club Shop / Megastore";
            case 6: return "Bars & Concourse";
            case 7: return "Restaurants & Food Hall";
            case 8: return "Hospitality Suites";
            case 9: return "Fan Zone";
            case 10: return "Parking & Transport Hub";
            case 11: return "Media & Broadcast Centre";
            case 12: return "Club Museum & Stadium Tours";
            default: return "Facility";
        }
    }

    private String facilityImpact(int code) {
        switch (code) {
            case 1: return "better training gains";
            case 2: return "faster injury recovery";
            case 3: return "better condition recovery";
            case 4: return "stronger youth development";
            case 5: return "higher merchandise income";
            case 6: return "higher home-match spend";
            case 7: return "higher home-match spend";
            case 8: return "premium matchday revenue";
            case 9: return "attendance + commercial boost";
            case 10: return "attendance/access boost";
            case 11: return "sponsorship & media boost";
            case 12: return "weekly non-match income";
            default: return "club development";
        }
    }

    private String facilityLevelLabel(int level) {
        if (level <= 1) return "Level 1 • Basic";
        if (level == 2) return "Level 2 • Adequate";
        if (level == 3) return "Level 3 • Good";
        if (level == 4) return "Level 4 • Excellent";
        return "Level 5 • Elite";
    }

    private int facilityUpgradeCostK(int code, int level) {
        int base;
        switch (code) {
            case 1: base = 4200; break;
            case 2: base = 3600; break;
            case 3: base = 2800; break;
            case 4: base = 4800; break;
            case 5: base = 1800; break;
            case 6: base = 1600; break;
            case 7: base = 2100; break;
            case 8: base = 3500; break;
            case 9: base = 1700; break;
            case 10: base = 2400; break;
            case 11: base = 2200; break;
            case 12: base = 1900; break;
            default: base = 2000;
        }
        return base + level * base / 2;
    }

    private int facilityUpgradeWeeks(int code, int level) {
        int base = (code == 1 || code == 4 || code == 8) ? 5 : 3;
        return base + level * 2;
    }

    private void startStadiumProject(String type, int code, int costK, int weeks, int capacityGain) {
        if (stadiumProjectWeeks > 0) {
            Toast.makeText(this, "Finish the current project before starting another.", Toast.LENGTH_LONG).show();
            return;
        }
        if (financeBalanceK < costK) {
            Toast.makeText(this, "Insufficient club cash. Required: " + moneyK(costK), Toast.LENGTH_LONG).show();
            return;
        }

        new BossDialog.Builder(this)
                .setTitle("Approve project?")
                .setMessage(type + "\n\nCost: " + moneyK(costK)
                        + "\nBuild time: " + weeks + " week(s)"
                        + (capacityGain > 0 ? "\nCapacity gain: +" + capacityGain : "")
                        + "\n\nThe full cost is committed immediately.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Approve", (dialog, which) -> {
                    stadiumProjectType = type;
                    stadiumProjectCode = code;
                    stadiumProjectWeeks = weeks;
                    stadiumProjectCostK = costK;
                    stadiumProjectCapacityGain = capacityGain;
                    recordInfrastructureSpend(costK);
                    addNews("STADIUM", type + " approved",
                            "Work has started. Completion is expected in " + weeks + " week(s).");
                    saveCurrentGame();
                    showStadiumCentre();
                })
                .show();
    }

    private void confirmNewStadium() {
        int newCapacity = Math.max(stadiumCapacity + 12000, (int) Math.round(stadiumCapacity * 1.35));
        int gain = newCapacity - stadiumCapacity;
        int cost = Math.max(65000, stadiumCapacity * 2);
        startStadiumProject("New " + clubNames[selectedClub] + " Stadium", 200, cost, 30, gain);
    }

    private void advanceStadiumProject() {
        if (stadiumProjectWeeks <= 0) return;
        stadiumProjectWeeks--;
        if (stadiumProjectWeeks > 0) return;

        String completed = stadiumProjectType;
        if (stadiumProjectCode >= 101 && stadiumProjectCode <= 104) {
            if (stadiumProjectCode == 101) stadiumNorthLevel = Math.min(5, stadiumNorthLevel + 1);
            if (stadiumProjectCode == 102) stadiumEastLevel = Math.min(5, stadiumEastLevel + 1);
            if (stadiumProjectCode == 103) stadiumSouthLevel = Math.min(5, stadiumSouthLevel + 1);
            if (stadiumProjectCode == 104) stadiumWestLevel = Math.min(5, stadiumWestLevel + 1);
            stadiumCapacity = Math.min(stadiumExpansionLimit, stadiumCapacity + stadiumProjectCapacityGain);
            stadiumSeatedCapacity = stadiumCapacity;
        } else if (stadiumProjectCode == 200) {
            stadiumCapacity = stadiumCapacity + stadiumProjectCapacityGain;
            stadiumSeatedCapacity = stadiumCapacity;
            stadiumExpansionLimit = Math.max(stadiumCapacity + 22000, (int) Math.round(stadiumCapacity * 1.65));
            stadiumName = clubNames[selectedClub] + " Arena";
            stadiumCovered = true;
            stadiumUndersoilHeating = true;
            stadiumNorthLevel = Math.max(3, stadiumNorthLevel);
            stadiumEastLevel = Math.max(3, stadiumEastLevel);
            stadiumSouthLevel = Math.max(3, stadiumSouthLevel);
            stadiumWestLevel = Math.max(3, stadiumWestLevel);
            stadiumHospitalityLevel = Math.min(5, stadiumHospitalityLevel + 1);
            stadiumParkingLevel = Math.min(5, stadiumParkingLevel + 1);
            stadiumMediaLevel = Math.min(5, stadiumMediaLevel + 1);
        } else {
            increaseFacilityLevel(stadiumProjectCode);
        }

        stadiumProjectType = "";
        stadiumProjectCode = 0;
        stadiumProjectCostK = 0;
        stadiumProjectCapacityGain = 0;
        addNews("STADIUM", "Project complete: " + completed,
                "The new infrastructure is now fully operational.");
    }

    private void increaseFacilityLevel(int code) {
        switch (code) {
            case 1: stadiumTrainingLevel = Math.min(5, stadiumTrainingLevel + 1); break;
            case 2: stadiumMedicalLevel = Math.min(5, stadiumMedicalLevel + 1); break;
            case 3: stadiumGymLevel = Math.min(5, stadiumGymLevel + 1); break;
            case 4: stadiumYouthLevel = Math.min(5, stadiumYouthLevel + 1); break;
            case 5: stadiumShopLevel = Math.min(5, stadiumShopLevel + 1); break;
            case 6: stadiumBarsLevel = Math.min(5, stadiumBarsLevel + 1); break;
            case 7: stadiumRestaurantsLevel = Math.min(5, stadiumRestaurantsLevel + 1); break;
            case 8: stadiumHospitalityLevel = Math.min(5, stadiumHospitalityLevel + 1); break;
            case 9: stadiumFanZoneLevel = Math.min(5, stadiumFanZoneLevel + 1); break;
            case 10: stadiumParkingLevel = Math.min(5, stadiumParkingLevel + 1); break;
            case 11: stadiumMediaLevel = Math.min(5, stadiumMediaLevel + 1); break;
            case 12: stadiumMuseumLevel = Math.min(5, stadiumMuseumLevel + 1); break;
        }
    }

    private void resetFinanceStadiumState() {
        int clubStrength = selectedClub >= 0 ? strength[selectedClub] : 70;
        int baseLevel = clubStrength >= 85 ? 4 : clubStrength >= 76 ? 3 : 2;

        financeCurrentMonthIncomeK = 0;
        financeCurrentMonthExpenseK = 0;
        financeLastMonthIncomeK = 1700 + clubStrength * 20;
        financeLastMonthExpenseK = 1500 + clubStrength * 19;
        financeSeasonIncomeK = 0;
        financeSeasonExpenseK = 0;
        financeLastYearIncomeK = 24000 + clubStrength * 650;
        financeLastYearExpenseK = 22000 + clubStrength * 590;
        financeBalanceK = 18000 + Math.max(0, currentTransferBudget) * 1800 + Math.max(0, clubStrength - 65) * 1100;
        financeMonthStamp = currentDate.getYear() * 100 + currentDate.getMonthValue();

        financeGateReceiptsK = 0;
        financeBroadcastK = 0;
        financeSponsorshipK = 0;
        financeMerchandiseK = 0;
        financePlayerSalesK = 0;
        financeOtherIncomeK = 0;
        financePlayerWagesK = 0;
        financeStaffWagesK = 0;
        financeTransferSpendK = 0;
        financeStadiumSpendK = 0;
        financeMaintenanceK = 0;
        financeTravelK = 0;
        financeYouthScoutingK = 0;
        financeOtherExpenseK = 0;

        stadiumName = selectedClub >= 0 ? clubNames[selectedClub] + " Stadium" : "Club Stadium";
        stadiumCapacity = Math.max(9000, 12000 + Math.max(0, clubStrength - 65) * 1300);
        stadiumSeatedCapacity = stadiumCapacity;
        stadiumExpansionLimit = Math.max(stadiumCapacity + 14000, (int) Math.round(stadiumCapacity * 1.75));
        stadiumAverageAttendance = 0;
        stadiumHomeGames = 0;
        stadiumCovered = clubStrength >= 84;
        stadiumUndersoilHeating = clubStrength >= 72;

        int standLevel = stadiumCapacity >= 35000 ? 3 : stadiumCapacity >= 20000 ? 2 : 1;
        stadiumNorthLevel = standLevel;
        stadiumEastLevel = standLevel;
        stadiumSouthLevel = standLevel;
        stadiumWestLevel = standLevel;

        stadiumTrainingLevel = baseLevel;
        stadiumMedicalLevel = baseLevel;
        stadiumGymLevel = baseLevel;
        stadiumYouthLevel = Math.max(1, baseLevel - 1);
        stadiumShopLevel = Math.max(1, baseLevel - 1);
        stadiumBarsLevel = Math.max(1, baseLevel - 1);
        stadiumRestaurantsLevel = Math.max(1, baseLevel - 1);
        stadiumHospitalityLevel = Math.max(1, baseLevel - 1);
        stadiumFanZoneLevel = Math.max(1, baseLevel - 1);
        stadiumParkingLevel = Math.max(1, baseLevel - 1);
        stadiumMediaLevel = Math.max(1, baseLevel - 1);
        stadiumMuseumLevel = Math.max(1, baseLevel - 2);

        stadiumProjectType = "";
        stadiumProjectCode = 0;
        stadiumProjectWeeks = 0;
        stadiumProjectCostK = 0;
        stadiumProjectCapacityGain = 0;
    }

    private void saveFinanceStadiumState() {
        if (selectedSlot < 0 || selectedClub < 0) return;
        prefs.edit()
                .putInt(key(selectedSlot, "fin_balance"), financeBalanceK)
                .putInt(key(selectedSlot, "fin_cm_i"), financeCurrentMonthIncomeK)
                .putInt(key(selectedSlot, "fin_cm_e"), financeCurrentMonthExpenseK)
                .putInt(key(selectedSlot, "fin_lm_i"), financeLastMonthIncomeK)
                .putInt(key(selectedSlot, "fin_lm_e"), financeLastMonthExpenseK)
                .putInt(key(selectedSlot, "fin_sy_i"), financeSeasonIncomeK)
                .putInt(key(selectedSlot, "fin_sy_e"), financeSeasonExpenseK)
                .putInt(key(selectedSlot, "fin_ly_i"), financeLastYearIncomeK)
                .putInt(key(selectedSlot, "fin_ly_e"), financeLastYearExpenseK)
                .putInt(key(selectedSlot, "fin_month"), financeMonthStamp)
                .putInt(key(selectedSlot, "fin_gate"), financeGateReceiptsK)
                .putInt(key(selectedSlot, "fin_tv"), financeBroadcastK)
                .putInt(key(selectedSlot, "fin_sponsor"), financeSponsorshipK)
                .putInt(key(selectedSlot, "fin_merch"), financeMerchandiseK)
                .putInt(key(selectedSlot, "fin_sales"), financePlayerSalesK)
                .putInt(key(selectedSlot, "fin_other_i"), financeOtherIncomeK)
                .putInt(key(selectedSlot, "fin_pwages"), financePlayerWagesK)
                .putInt(key(selectedSlot, "fin_swages"), financeStaffWagesK)
                .putInt(key(selectedSlot, "fin_transfers"), financeTransferSpendK)
                .putInt(key(selectedSlot, "fin_stadium"), financeStadiumSpendK)
                .putInt(key(selectedSlot, "fin_maint"), financeMaintenanceK)
                .putInt(key(selectedSlot, "fin_travel"), financeTravelK)
                .putInt(key(selectedSlot, "fin_youth"), financeYouthScoutingK)
                .putInt(key(selectedSlot, "fin_other_e"), financeOtherExpenseK)
                .putString(key(selectedSlot, "stad_name"), stadiumName)
                .putInt(key(selectedSlot, "stad_cap"), stadiumCapacity)
                .putInt(key(selectedSlot, "stad_seats"), stadiumSeatedCapacity)
                .putInt(key(selectedSlot, "stad_limit"), stadiumExpansionLimit)
                .putInt(key(selectedSlot, "stad_avg"), stadiumAverageAttendance)
                .putInt(key(selectedSlot, "stad_homegames"), stadiumHomeGames)
                .putBoolean(key(selectedSlot, "stad_covered"), stadiumCovered)
                .putBoolean(key(selectedSlot, "stad_heating"), stadiumUndersoilHeating)
                .putInt(key(selectedSlot, "stad_n"), stadiumNorthLevel)
                .putInt(key(selectedSlot, "stad_e"), stadiumEastLevel)
                .putInt(key(selectedSlot, "stad_s"), stadiumSouthLevel)
                .putInt(key(selectedSlot, "stad_w"), stadiumWestLevel)
                .putInt(key(selectedSlot, "fac_training"), stadiumTrainingLevel)
                .putInt(key(selectedSlot, "fac_medical"), stadiumMedicalLevel)
                .putInt(key(selectedSlot, "fac_gym"), stadiumGymLevel)
                .putInt(key(selectedSlot, "fac_youth"), stadiumYouthLevel)
                .putInt(key(selectedSlot, "fac_shop"), stadiumShopLevel)
                .putInt(key(selectedSlot, "fac_bars"), stadiumBarsLevel)
                .putInt(key(selectedSlot, "fac_rest"), stadiumRestaurantsLevel)
                .putInt(key(selectedSlot, "fac_hosp"), stadiumHospitalityLevel)
                .putInt(key(selectedSlot, "fac_fanzone"), stadiumFanZoneLevel)
                .putInt(key(selectedSlot, "fac_parking"), stadiumParkingLevel)
                .putInt(key(selectedSlot, "fac_media"), stadiumMediaLevel)
                .putInt(key(selectedSlot, "fac_museum"), stadiumMuseumLevel)
                .putString(key(selectedSlot, "stad_project_type"), stadiumProjectType)
                .putInt(key(selectedSlot, "stad_project_code"), stadiumProjectCode)
                .putInt(key(selectedSlot, "stad_project_weeks"), stadiumProjectWeeks)
                .putInt(key(selectedSlot, "stad_project_cost"), stadiumProjectCostK)
                .putInt(key(selectedSlot, "stad_project_gain"), stadiumProjectCapacityGain)
                .apply();
    }

    private void loadFinanceStadiumState(int slot) {
        if (!prefs.contains(key(slot, "fin_balance"))) {
            resetFinanceStadiumState();
            return;
        }

        financeBalanceK = prefs.getInt(key(slot, "fin_balance"), 0);
        financeCurrentMonthIncomeK = prefs.getInt(key(slot, "fin_cm_i"), 0);
        financeCurrentMonthExpenseK = prefs.getInt(key(slot, "fin_cm_e"), 0);
        financeLastMonthIncomeK = prefs.getInt(key(slot, "fin_lm_i"), 0);
        financeLastMonthExpenseK = prefs.getInt(key(slot, "fin_lm_e"), 0);
        financeSeasonIncomeK = prefs.getInt(key(slot, "fin_sy_i"), 0);
        financeSeasonExpenseK = prefs.getInt(key(slot, "fin_sy_e"), 0);
        financeLastYearIncomeK = prefs.getInt(key(slot, "fin_ly_i"), 0);
        financeLastYearExpenseK = prefs.getInt(key(slot, "fin_ly_e"), 0);
        financeMonthStamp = prefs.getInt(key(slot, "fin_month"), currentDate.getYear() * 100 + currentDate.getMonthValue());

        financeGateReceiptsK = prefs.getInt(key(slot, "fin_gate"), 0);
        financeBroadcastK = prefs.getInt(key(slot, "fin_tv"), 0);
        financeSponsorshipK = prefs.getInt(key(slot, "fin_sponsor"), 0);
        financeMerchandiseK = prefs.getInt(key(slot, "fin_merch"), 0);
        financePlayerSalesK = prefs.getInt(key(slot, "fin_sales"), 0);
        financeOtherIncomeK = prefs.getInt(key(slot, "fin_other_i"), 0);
        financePlayerWagesK = prefs.getInt(key(slot, "fin_pwages"), 0);
        financeStaffWagesK = prefs.getInt(key(slot, "fin_swages"), 0);
        financeTransferSpendK = prefs.getInt(key(slot, "fin_transfers"), 0);
        financeStadiumSpendK = prefs.getInt(key(slot, "fin_stadium"), 0);
        financeMaintenanceK = prefs.getInt(key(slot, "fin_maint"), 0);
        financeTravelK = prefs.getInt(key(slot, "fin_travel"), 0);
        financeYouthScoutingK = prefs.getInt(key(slot, "fin_youth"), 0);
        financeOtherExpenseK = prefs.getInt(key(slot, "fin_other_e"), 0);

        stadiumName = prefs.getString(key(slot, "stad_name"), clubNames[selectedClub] + " Stadium");
        stadiumCapacity = prefs.getInt(key(slot, "stad_cap"), 18000);
        stadiumSeatedCapacity = prefs.getInt(key(slot, "stad_seats"), stadiumCapacity);
        stadiumExpansionLimit = prefs.getInt(key(slot, "stad_limit"), stadiumCapacity + 15000);
        stadiumAverageAttendance = prefs.getInt(key(slot, "stad_avg"), 0);
        stadiumHomeGames = prefs.getInt(key(slot, "stad_homegames"), 0);
        stadiumCovered = prefs.getBoolean(key(slot, "stad_covered"), false);
        stadiumUndersoilHeating = prefs.getBoolean(key(slot, "stad_heating"), true);
        stadiumNorthLevel = prefs.getInt(key(slot, "stad_n"), 1);
        stadiumEastLevel = prefs.getInt(key(slot, "stad_e"), 1);
        stadiumSouthLevel = prefs.getInt(key(slot, "stad_s"), 1);
        stadiumWestLevel = prefs.getInt(key(slot, "stad_w"), 1);

        stadiumTrainingLevel = prefs.getInt(key(slot, "fac_training"), 2);
        stadiumMedicalLevel = prefs.getInt(key(slot, "fac_medical"), 2);
        stadiumGymLevel = prefs.getInt(key(slot, "fac_gym"), 2);
        stadiumYouthLevel = prefs.getInt(key(slot, "fac_youth"), 2);
        stadiumShopLevel = prefs.getInt(key(slot, "fac_shop"), 2);
        stadiumBarsLevel = prefs.getInt(key(slot, "fac_bars"), 2);
        stadiumRestaurantsLevel = prefs.getInt(key(slot, "fac_rest"), 2);
        stadiumHospitalityLevel = prefs.getInt(key(slot, "fac_hosp"), 2);
        stadiumFanZoneLevel = prefs.getInt(key(slot, "fac_fanzone"), 2);
        stadiumParkingLevel = prefs.getInt(key(slot, "fac_parking"), 2);
        stadiumMediaLevel = prefs.getInt(key(slot, "fac_media"), 2);
        stadiumMuseumLevel = prefs.getInt(key(slot, "fac_museum"), 1);

        stadiumProjectType = prefs.getString(key(slot, "stad_project_type"), "");
        stadiumProjectCode = prefs.getInt(key(slot, "stad_project_code"), 0);
        stadiumProjectWeeks = prefs.getInt(key(slot, "stad_project_weeks"), 0);
        stadiumProjectCostK = prefs.getInt(key(slot, "stad_project_cost"), 0);
        stadiumProjectCapacityGain = prefs.getInt(key(slot, "stad_project_gain"), 0);
    }

    private void clearFinanceStadiumState(int slot) {
        String[] fields = {
                "fin_balance", "fin_cm_i", "fin_cm_e", "fin_lm_i", "fin_lm_e", "fin_sy_i", "fin_sy_e",
                "fin_ly_i", "fin_ly_e", "fin_month", "fin_gate", "fin_tv", "fin_sponsor", "fin_merch",
                "fin_sales", "fin_other_i", "fin_pwages", "fin_swages", "fin_transfers", "fin_stadium",
                "fin_maint", "fin_travel", "fin_youth", "fin_other_e", "stad_name", "stad_cap", "stad_seats",
                "stad_limit", "stad_avg", "stad_homegames", "stad_covered", "stad_heating", "stad_n", "stad_e",
                "stad_s", "stad_w", "fac_training", "fac_medical", "fac_gym", "fac_youth", "fac_shop",
                "fac_bars", "fac_rest", "fac_hosp", "fac_fanzone", "fac_parking", "fac_media", "fac_museum",
                "stad_project_type", "stad_project_code", "stad_project_weeks", "stad_project_cost", "stad_project_gain"
        };
        SharedPreferences.Editor editor = prefs.edit();
        for (String field : fields) editor.remove(key(slot, field));
        editor.apply();
    }

    private void showClubOffice() {
        backAction = () -> showDashboard();
        LinearLayout page = createPage("Board & Media", "Club expectations, confidence and manager reputation", true);

        LinearLayout board = makePanel();
        board.addView(makeText("BOARD CONFIDENCE  •  " + boardConfidence + "%", 18, boardConfidence >= 55 ? accent : warning));
        board.addView(makeText("Expectation: " + boardExpectationText() + "\nManager reputation: " + managerReputation + "/100\nWage budget: €" + wageBudgetK + "k/week", 15, text));
        page.addView(board);

        page.addView(makeButton("Request Extra Transfer Funds", v -> requestBoardFunds(false)));
        page.addView(makeButton("Issue Board Ultimatum", v -> requestBoardFunds(true)));
        page.addView(makeButton("Media: Praise the Team", v -> mediaResponse("Praise")));
        page.addView(makeButton("Media: Protect the Players", v -> mediaResponse("Protect")));
        page.addView(makeButton("Media: Demand More", v -> mediaResponse("Demand")));
    }

    private String boardExpectationText() {
        if (selectedClub < 0) return "compete";
        if (strength[selectedClub] >= 85) return "challenge for the title";
        if (strength[selectedClub] >= 78) return "qualify for Europe";
        if (strength[selectedClub] >= 70) return "finish in the top half";
        return "stay clear of relegation";
    }

    private void requestBoardFunds(boolean ultimatum) {
        double chance = boardConfidence / 100.0 * 0.55 + managerReputation / 100.0 * 0.25;
        if (ultimatum) chance -= 0.12;
        if (random.nextDouble() < chance) {
            int extra = 2 + random.nextInt(7);
            currentTransferBudget += extra;
            boardConfidence = Math.min(100, boardConfidence + (ultimatum ? 0 : 2));
            addNews("BOARD", "Board approves extra funds", "An additional €" + extra + "m has been added to the transfer budget.");
            Toast.makeText(this, "Board approved €" + extra + "m extra.", Toast.LENGTH_LONG).show();
        } else {
            boardConfidence = Math.max(0, boardConfidence - (ultimatum ? 16 : 3));
            addNews("BOARD", ultimatum ? "Ultimatum rejected" : "Request rejected", ultimatum ? "The board is furious at being pressured." : "The board will not release more funds right now.");
            Toast.makeText(this, ultimatum ? "Ultimatum rejected — confidence damaged." : "Request rejected.", Toast.LENGTH_LONG).show();
        }
        saveCurrentGame();
        showClubOffice();
    }

    private void mediaResponse(String type) {
        int delta = 0;
        if ("Praise".equals(type)) delta = 3;
        if ("Protect".equals(type)) delta = 2;
        if ("Demand".equals(type)) delta = -2;
        for (Player p : players) if (p.team == selectedClub) p.morale = Math.max(25, Math.min(100, p.morale + delta));
        managerReputation = Math.max(0, Math.min(100, managerReputation + ("Demand".equals(type) ? 1 : 0)));
        addNews("MEDIA", "Manager response: " + type, "The squad reacted to your public comments. Morale changed by " + delta + ".");
        saveCurrentGame();
        showClubOffice();
    }

    private void updateBoardAfterMatch() {
        boolean userHome = liveHome == selectedClub;
        int gf = userHome ? liveHomeGoals : liveAwayGoals;
        int ga = userHome ? liveAwayGoals : liveHomeGoals;
        if (gf > ga) {
            boardConfidence = Math.min(100, boardConfidence + 3);
            managerReputation = Math.min(100, managerReputation + 1);
        } else if (gf < ga) {
            boardConfidence = Math.max(0, boardConfidence - (strength[selectedClub] >= 80 ? 4 : 2));
        }
        if (boardConfidence < 20) addNews("BOARD", "Board pressure increasing", "The directors are losing patience. Results must improve quickly.");
    }

    private void processInjuriesAndRecovery() {
        for (Player p : players) {
            boolean ourPlayer = p.team == selectedClub;
            int recoveryBoost = ourPlayer ? Math.max(0, stadiumMedicalLevel - 1) + Math.max(0, stadiumGymLevel - 2) : 0;
            if (p.injuredWeeks > 0) {
                p.injuredWeeks = Math.max(0, p.injuredWeeks - (recoveryBoost >= 5 ? 2 : 1));
                p.fitness = Math.min(100, p.fitness + 8 + recoveryBoost);
                if (p.injuredWeeks == 0 && ourPlayer) {
                    addNews("MEDICAL", p.name + " returns to training",
                            "The physio reports that the player is available again. Facility recovery bonus: +" + recoveryBoost + ".");
                }
            } else {
                p.fitness = Math.min(100, p.fitness + 3 + (ourPlayer ? Math.max(0, stadiumGymLevel - 1) : 0));
            }
        }
    }

    private void processSeasonEnd() {
        rollFinanceSeason();
        addNews("SEASON", "Season complete", "Player ages and development have been updated. A small youth intake has joined the league database.");
        for (Player p : players) {
            p.age++;
            if (p.age <= 24 && p.currentAbility < p.potentialAbility && random.nextDouble() < 0.45) {
                p.currentAbility = Math.min(p.potentialAbility, p.currentAbility + 2 + random.nextInt(5));
                p.overall = Math.min(95, Math.max(p.overall, p.currentAbility / 2));
            }
        }
        managerReputation = Math.min(100, managerReputation + Math.max(0, points[selectedClub] / 15));
    }

    private void resetCareerState() {
        matchday = 0;
        currentDate = SEASON_START;
        trainingFocus = "Balanced";
        managerWeeklyWageK = 32;
        managerContractYears = 3;
        managerTactical = 12;
        managerMotivating = 11;
        managerDiscipline = 10;
        managerPlayerKnowledge = 12;
        managerYouth = 10;
        managerNegotiating = 11;
        assistantManagerName = "Marco Vieira";
        assistantManagerRating = 62;
        headCoachName = "Tiago Nunes";
        headCoachRating = 64;
        chiefScoutName = "Rui Mendes";
        chiefScoutRating = 61;
        tacticFormation = "4-3-3";
        playStyle = "Balanced";
        currentTransferBudget = selectedClub >= 0 ? budgets[selectedClub] : 0;
        Arrays.fill(playerRoleStatus, 0);
        Arrays.fill(clubMatchGF, -1);
        Arrays.fill(clubMatchGA, -1);
        Arrays.fill(played, 0);
        Arrays.fill(won, 0);
        Arrays.fill(drawn, 0);
        Arrays.fill(lost, 0);
        Arrays.fill(goalsFor, 0);
        Arrays.fill(goalsAgainst, 0);
        Arrays.fill(points, 0);
                          resetClassicState();
        resetFinanceStadiumState();
    }

    @Override protected void onPause() {
        tickerWasActive=liveMatchActive;
        pausedBeforeBackground=livePaused;
        stopLiveMatchTicker();
        if(audio!=null) audio.pause();
        super.onPause();
    }
    @Override protected void onResume() {
        super.onResume();
        if(tickerWasActive && livePitchView!=null && !liveTacticsSessionActive) {
            liveMatchActive=true;livePaused=pausedBeforeBackground;startLiveMatchTicker();
            if(audio!=null) audio.start();
        }
        tickerWasActive=false;
    }
    @Override protected void onDestroy() {
        stopLiveMatchTicker();stopLiveMatchAudio();
        if(portraits!=null) portraits.close();
        super.onDestroy();
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private float dp(float value) {
        float density = getResources().getDisplayMetrics().density;
        return value * density;
    }
}

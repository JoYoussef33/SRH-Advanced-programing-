package com.paddleboard.tracker;

import java.util.Random;

public class MessageGenerator {

    private static final Random RNG = new Random();

    // ── LEGENDARY ─────────────────────────────────────────────────────────────
    private static final String[] LEGENDARY = {
        "BEAST MODE ACTIVATED 🔱\nThe ocean bows to you.",
        "Poseidon called —\nhe wants tips on your technique.",
        "Your GPS is exhausted\njust looking at this session 🌊",
        "Somewhere a sea turtle\nis taking notes on your form 🐢",
        "You didn't just paddle.\nYou absolutely DOMINATED the water. 🏆",
        "That session was so legendary\neven the fish stopped to watch.",
        "The ocean called.\nIt wants to know your training schedule.",
        "NASA is using your GPS track\nas a performance benchmark.",
        "Your arms deserve\ntheir own award ceremony. 💪",
        "Historic.\nGenuinely historic. Someone write this down.",
        "You out-paddled an actual boat today.\nTrue story.",
        "I'm not crying, you're crying.\nThat session was beautiful.",
        "You've entered the 1% of paddlers.\nWelcome to the top. 👑",
        "The waves parted for you.\nLike, actually.",
        "Your paddle should be framed\nand put in a museum.",
        "Dolphins were spotted\nfollowing you for inspiration.",
        "The sun came out extra bright today\njust for your session.",
        "Future you\nis going to be VERY proud of present you.",
        "That distance though.\nI don't even have words. Well done. 🌟"
    };

    // ── SOLID ─────────────────────────────────────────────────────────────────
    private static final String[] GOOD = {
        "Solid paddle!\nThe ocean respects you 🤙",
        "Arms like noodles yet?\nGood — that means you worked hard.",
        "You covered real ground today.\nNice work, paddler 🏄",
        "The sea gave you a thumbs up.\nI watched, it happened.",
        "Not bad at all!\nYour board is very proud of you. 🌊",
        "Saltwater approved.\nStrong session all around.",
        "You and the ocean\nhad a proper moment today.",
        "Middle of the pack?\nNo — that was top-tier performance.",
        "Your board didn't complain once.\nRespect.",
        "Clean, consistent, effective.\nThat's how it's done.",
        "Progress looks good on you.\nKeep building.",
        "The water remembers effort.\nToday's was noted.",
        "That cadence was something else.\nSmooth and strong.",
        "Ticked all the boxes today.\nDistance ✓ Speed ✓ Vibes ✓",
        "You made paddling look easy.\nIt wasn't. We both know that.",
        "Strong enough to impress,\nhumble enough to improve. 🏄",
        "Not legendary — yet.\nBut you're getting there.",
        "Today's session built tomorrow's champion.\nGood work."
    };

    // ── MEH ───────────────────────────────────────────────────────────────────
    private static final String[] MEH = {
        "A paddle is a paddle, I guess...\nBetter than the couch 😅",
        "The ocean barely noticed,\nbut hey — you showed up!",
        "Short but sweet.\nOr just short. We don't judge. 🤷",
        "The board got wet.\nThat definitely counts for something.",
        "Tomorrow will be better.\nAnd if not, the day after that.",
        "Every legend started somewhere.\nToday was your somewhere.",
        "Your shirt got windswept.\nThat's effort. Kind of.",
        "Look, you tried.\nThat's... definitely something.",
        "The water was warm at least,\nright? Small wins matter.",
        "Half a session is still\nhalf more than nothing. 👍",
        "The important thing is\nyou remembered to bring the paddle.",
        "Gentle. Peaceful.\nAlmost like a spa day on water.",
        "You moved. The water moved.\nA partnership was formed.",
        "Not your best,\nnot your worst. Right in the middle lane.",
        "Like a practice lap.\nThe real race is coming.",
        "You warmed up the ocean nicely\nfor someone else. How kind.",
        "Hey, consistency is key.\nConsistently okay is still okay.",
        "Your arms got some fresh air.\nThat's basically a workout."
    };

    // ── SARCASTIC ─────────────────────────────────────────────────────────────
    private static final String[] SARCASTIC = {
        "Wow. Just... wow.\nWas the paddle too heavy? 😂",
        "GPS thought you were parked. 🅿️\nLike, actually parked.",
        "The board moved more than you did.\nYour own board. Think about that.",
        "Did you actually go IN the water\nor just stare at it lovingly?",
        "Legend has it you're still out there\ncontemplating your first stroke.",
        "Your calorie burn was mostly\nfrom tapping 'Stop Session'. Impressive.",
        "The seagulls were lapping you.\nSEAGULLS. 🦅",
        "I've seen more movement\nfrom a pool noodle.",
        "The water barely rippled.\nAlmost zen, if we're being generous.",
        "A duck passed you.\nTwice. Same duck.",
        "You paddled with the energy\nof a Tuesday morning meeting.",
        "The board is quietly questioning\nits purpose in life right now.",
        "Even the seaweed moved more.\nOcean plants outperformed you today.",
        "At this pace, you'll circle the globe\nin approximately never.",
        "Your max speed was registered\nonly because you sneezed.",
        "More aggressive movement\nhas been observed on parked kayaks.",
        "The fish were worried.\nThey actually checked if you were okay.",
        "I tried to calculate your average speed\nbut the answer was 'yes'.",
        "Your GPS track looks like\nan abstract painting of standing still.",
        "You brought the paddle.\nThe paddle brought confusion.",
        "Sir/Ma'am, this is a paddle board,\nnot a meditation platform. 🧘",
        "The ocean genuinely forgot\nyou were there. Not great.",
        "Your paddle touched water!\nBriefly. But it happened.",
        "Some people log zero.\nYou logged above zero.\nYou're literally better than nothing.",
        "The tide moved more distance\nthan you did. Just saying.",
        "I'd say 'nice try'\nbut let's not overstate it. 😬"
    };

    public static String getMessage(SessionData s) {
        String[] pool;
        switch (s.tier()) {
            case 3:  pool = LEGENDARY; break;
            case 2:  pool = GOOD;      break;
            case 1:  pool = MEH;       break;
            default: pool = SARCASTIC; break;
        }
        return pool[RNG.nextInt(pool.length)];
    }

    public static String getTierLabel(int tier) {
        switch (tier) {
            case 3:  return "LEGENDARY SESSION 🏆";
            case 2:  return "SOLID SESSION 💪";
            case 1:  return "SESSION COMPLETE 🌊";
            default: return "YOU TRIED 😅";
        }
    }

    public static int getTierColor(int tier) {
        switch (tier) {
            case 3:  return 0xFFFFD166;
            case 2:  return 0xFF00D4FF;
            case 1:  return 0xFF00FFD4;
            default: return 0xFFFF6B35;
        }
    }

    public static int getPhotoRes(int tier) {
        switch (tier) {
            case 3:  return R.drawable.photo_amazing;
            case 2:  return R.drawable.photo_good;
            case 1:  return R.drawable.photo_meh;
            default: return R.drawable.photo_poor;
        }
    }
}

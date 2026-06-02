package com.paddleboard.tracker;

import java.util.Random;

public class MessageGenerator {

    private static final Random RNG = new Random();

    private static final String[] LEGENDARY = {
        "BEAST MODE ACTIVATED 🔱\nThe ocean bows to you.",
        "Poseidon called —\nhe wants tips on your technique.",
        "Your GPS is exhausted\njust looking at this session 🌊",
        "Somewhere a sea turtle\nis taking notes on your form 🐢",
        "You didn't just paddle.\nYou absolutely DOMINATED the water. 🏆",
        "That session was so good\neven the fish stopped to watch."
    };

    private static final String[] GOOD = {
        "Solid paddle!\nThe ocean respects you 🤙",
        "Arms like noodles yet?\nGood — that means you worked hard.",
        "You covered real ground today.\nNice work, paddler 🏄",
        "The sea gave you a thumbs up.\nI watched, it happened.",
        "Not bad at all!\nYour board is proud of you. 🌊"
    };

    private static final String[] MEH = {
        "A paddle is a paddle, I guess...\nBetter than the couch 😅",
        "The ocean barely noticed,\nbut hey — you showed up!",
        "Short but sweet.\nOr just short. We don't judge. 🤷",
        "You dipped your toes in.\nLiterally. Almost.",
        "The water was warm at least,\nright? That's something."
    };

    private static final String[] SARCASTIC = {
        "Wow. Just... wow.\nWas the paddle too heavy? 😂",
        "GPS thought you were parked. 🅿️\nLike, actually parked.",
        "The board moved more than you did.\nYour own board. Think about that.",
        "Did you actually go IN the water\nor just stare at it lovingly?",
        "Legend has it you're still out there\ncontemplating your first stroke.",
        "Your calories burned are basically\njust from breathing. Keep it up champ.",
        "The seagulls were lapping you.\nSEAGULLS. 🦅"
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
            case 3:  return 0xFFFFD166; // gold
            case 2:  return 0xFF00D4FF; // cyan
            case 1:  return 0xFF00FFD4; // teal
            default: return 0xFFFF6B35; // coral
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

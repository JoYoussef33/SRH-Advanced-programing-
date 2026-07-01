package com.paddleboard.tracker;

import java.util.Random;

/** Real SUP tips, safety guidance and facts, rotated on the home screen. */
public class PaddleFacts {

    private static final Random RNG = new Random();

    private static final String[] FACTS = {
        // ── Technique ─────────────────────────────────────────────────────────
        "💡 Keep your arms straight and rotate your torso — your core is far stronger than your arms.",
        "💡 Look at the horizon, not your feet. Your balance improves instantly.",
        "💡 The paddle blade should angle AWAY from you — most beginners hold it backwards.",
        "💡 Short, quick strokes near the board beat long, wide ones for speed.",
        "💡 Stand with feet parallel, hip-width apart, knees slightly bent — never straight-legged.",
        "💡 Bury the whole blade in the water before pulling. Half-blade strokes waste energy.",
        "💡 Switch paddling sides every 4–6 strokes to hold a straight line.",
        "💡 A slight bend in your knees is your suspension system on chop.",
        "💡 Exit the stroke at your feet — pulling past your hips just slows you down.",
        "💡 To turn fast, step back on the board and sweep the paddle in a wide arc.",
        "💡 Falling? Fall AWAY from the board — boards hurt more than water does.",
        "💡 Paddle blade fully submerged, top hand over the water — that's the power position.",
        // ── Safety ────────────────────────────────────────────────────────────
        "⚠️ Always wear your leash. It's the #1 piece of SUP safety equipment.",
        "⚠️ Wind above 12 knots (22 km/h) is challenging even for experienced paddlers.",
        "⚠️ Start your session paddling INTO the wind — come home with it at your back.",
        "⚠️ Offshore wind can carry you out faster than you can paddle back. Check direction first.",
        "⚠️ Tell someone where you're going and when you'll be back. Every time.",
        "⚠️ Cold water shock can hit in water below 15°C — dress for the water, not the air.",
        "⚠️ In many countries a SUP counts as a vessel — a buoyancy aid may be required offshore.",
        "⚠️ Check the tide before you launch. An outgoing tide + offshore wind is the danger combo.",
        "⚠️ If you get caught in wind, drop to your knees — lower profile, more control.",
        "⚠️ Waterproof phone case + whistle = the minimum kit for solo paddles.",
        // ── Fitness facts ─────────────────────────────────────────────────────
        "🔥 SUP burns roughly 400–550 kcal/hour at cruising pace — more than jogging.",
        "🔥 SUP racing can burn over 1000 kcal/hour — among the highest of any sport.",
        "🔥 Every stroke works your core, shoulders, back, arms AND legs — full-body training.",
        "🔥 Balancing on a board fires deep stabilizer muscles a gym can't reach.",
        "🔥 One hour of SUP ≈ moderate resistance training + cardio combined (≈6 METs).",
        "🔥 SUP is low-impact — great cross-training when your knees need a break from running.",
        // ── Fun facts ─────────────────────────────────────────────────────────
        "🌊 Modern SUP was born in Waikiki, Hawaii — surf instructors stood up to photograph students.",
        "🌊 The Hawaiian name for SUP is 'Hoe he'e nalu' — to stand, to surf, a wave.",
        "🌊 The longest SUP journey ever recorded is over 4,000 km down the Danube and beyond.",
        "🌊 Peruvian fishermen rode reed 'Caballitos de Totora' standing up 3,000 years ago.",
        "🌊 SUP was the fastest-growing water sport in the world for most of the 2010s.",
        "🌊 The world record for 24-hour distance on a SUP is over 190 km.",
        "🌊 Pro SUP racers hold 9–10 km/h for hours. Your max speed today might beat their cruise!",
        "🌊 Glassy dawn water is called 'glass-off' — the wind is usually calmest at sunrise.",
        "🌊 Dolphins are naturally curious about paddle boards — stay calm and keep paddling.",
        "🌊 A wider board (>81 cm) is more stable; a narrower one is faster. Physics is undefeated.",
        "🌊 Sea water is about 2.5% denser than fresh water — you float slightly better in the ocean.",
        "🌊 The 'golden hour' after sunrise usually has the calmest water of the whole day."
    };

    public static String random() {
        return FACTS[RNG.nextInt(FACTS.length)];
    }

    public static String next(int index) {
        return FACTS[Math.floorMod(index, FACTS.length)];
    }

    public static int count() { return FACTS.length; }
}

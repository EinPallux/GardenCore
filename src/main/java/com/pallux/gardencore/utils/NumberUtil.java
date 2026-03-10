package com.pallux.gardencore.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class NumberUtil {

    private static final DecimalFormat RAW       = new DecimalFormat("#,##0.##",  DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat SHORT_FMT = new DecimalFormat("#,##0.##",  DecimalFormatSymbols.getInstance(Locale.US));

    // ── Suffix table ──────────────────────────────────────────────────────────
    // Each entry: { threshold (as power of 10), suffix }
    // Every step is exactly 10^3 apart (one "illion" tier) up through Vigintillion,
    // then every tier through Centillion — 97 named tiers in total.
    // Double precision caps out near 1.8×10^308, so Centillion (10^303) is the
    // practical ceiling; anything beyond renders as the Centillion label.
    private static final Object[][] SUFFIXES = {
            { 1e3,   "K"     },  // Thousand
            { 1e6,   "M"     },  // Million
            { 1e9,   "B"     },  // Billion
            { 1e12,  "T"     },  // Trillion
            { 1e15,  "Qa"    },  // Quadrillion
            { 1e18,  "Qi"    },  // Quintillion
            { 1e21,  "Sx"    },  // Sextillion
            { 1e24,  "Sp"    },  // Septillion
            { 1e27,  "Oc"    },  // Octillion
            { 1e30,  "No"    },  // Nonillion
            // ── Decillions (10^33 – 10^60) ──────────────────────────────────
            { 1e33,  "Dc"    },  // Decillion
            { 1e36,  "UDc"   },  // Undecillion
            { 1e39,  "DDc"   },  // Duodecillion
            { 1e42,  "TDc"   },  // Tredecillion
            { 1e45,  "QaDc"  },  // Quattuordecillion
            { 1e48,  "QiDc"  },  // Quindecillion
            { 1e51,  "SxDc"  },  // Sexdecillion
            { 1e54,  "SpDc"  },  // Septendecillion
            { 1e57,  "OcDc"  },  // Octodecillion
            { 1e60,  "NoDc"  },  // Novemdecillion
            // ── Vigintillions (10^63 – 10^90) ───────────────────────────────
            { 1e63,  "Vg"    },  // Vigintillion
            { 1e66,  "UVg"   },  // Unvigintillion
            { 1e69,  "DVg"   },  // Duovigintillion
            { 1e72,  "TVg"   },  // Trevigintillion
            { 1e75,  "QaVg"  },  // Quattuorvigintillion
            { 1e78,  "QiVg"  },  // Quinvigintillion
            { 1e81,  "SxVg"  },  // Sexvigintillion
            { 1e84,  "SpVg"  },  // Septenvigintillion
            { 1e87,  "OcVg"  },  // Octovigintillion
            { 1e90,  "NoVg"  },  // Novemvigintillion
            // ── Trigintillions (10^93 – 10^120) ─────────────────────────────
            { 1e93,  "Tg"    },  // Trigintillion
            { 1e96,  "UTg"   },  // Untrigintillion
            { 1e99,  "DTg"   },  // Duotrigintillion
            { 1e102, "TTg"   },  // Tretrigintillion
            { 1e105, "QaTg"  },  // Quattuortrigintillion
            { 1e108, "QiTg"  },  // Quintrigintillion
            { 1e111, "SxTg"  },  // Sextrigintillion
            { 1e114, "SpTg"  },  // Septentrigintillion
            { 1e117, "OcTg"  },  // Octotrigintillion
            { 1e120, "NoTg"  },  // Noventrigintillion
            // ── Quadragintillions (10^123 – 10^150) ─────────────────────────
            { 1e123, "Qag"   },  // Quadragintillion
            { 1e126, "UQag"  },  // Unquadragintillion
            { 1e129, "DQag"  },  // Duoquadragintillion
            { 1e132, "TQag"  },  // Trequadragintillion
            { 1e135, "QaQag" },  // Quattuorquadragintillion
            { 1e138, "QiQag" },  // Quinquadragintillion
            { 1e141, "SxQag" },  // Sexquadragintillion
            { 1e144, "SpQag" },  // Septenquadragintillion
            { 1e147, "OcQag" },  // Octoquadragintillion
            { 1e150, "NoQag" },  // Novenquadragintillion
            // ── Quinquagintillions (10^153 – 10^180) ────────────────────────
            { 1e153, "Fg"    },  // Quinquagintillion
            { 1e156, "UFg"   },  // Unquinquagintillion
            { 1e159, "DFg"   },  // Duoquinquagintillion
            { 1e162, "TFg"   },  // Trequinquagintillion
            { 1e165, "QaFg"  },  // Quattuorquinquagintillion
            { 1e168, "QiFg"  },  // Quinquinquagintillion
            { 1e171, "SxFg"  },  // Sexquinquagintillion
            { 1e174, "SpFg"  },  // Septenquinquagintillion
            { 1e177, "OcFg"  },  // Octoquinquagintillion
            { 1e180, "NoFg"  },  // Novenquinquagintillion
            // ── Sexagintillions (10^183 – 10^210) ───────────────────────────
            { 1e183, "Sg"    },  // Sexagintillion
            { 1e186, "USg"   },  // Unsexagintillion
            { 1e189, "DSg"   },  // Duosexagintillion
            { 1e192, "TSg"   },  // Tresexagintillion
            { 1e195, "QaSg"  },  // Quattuorsexagintillion
            { 1e198, "QiSg"  },  // Quinsexagintillion
            { 1e201, "SxSg"  },  // Sexsexagintillion
            { 1e204, "SpSg"  },  // Septensexagintillion
            { 1e207, "OcSg"  },  // Octosexagintillion
            { 1e210, "NoSg"  },  // Novensexagintillion
            // ── Septuagintillions (10^213 – 10^240) ─────────────────────────
            { 1e213, "Eg"    },  // Septuagintillion
            { 1e216, "UEg"   },  // Unseptuagintillion
            { 1e219, "DEg"   },  // Duoseptuagintillion
            { 1e222, "TEg"   },  // Treseptuagintillion
            { 1e225, "QaEg"  },  // Quattuorseptuagintillion
            { 1e228, "QiEg"  },  // Quinseptuagintillion
            { 1e231, "SxEg"  },  // Sexseptuagintillion
            { 1e234, "SpEg"  },  // Septenseptuagintillion
            { 1e237, "OcEg"  },  // Octoseptuagintillion
            { 1e240, "NoEg"  },  // Novenseptuagintillion
            // ── Octogintillions (10^243 – 10^270) ───────────────────────────
            { 1e243, "Og"    },  // Octogintillion
            { 1e246, "UOg"   },  // Unoctogintillion
            { 1e249, "DOg"   },  // Duooctogintillion
            { 1e252, "TOg"   },  // Treoctogintillion
            { 1e255, "QaOg"  },  // Quattuoroctogintillion
            { 1e258, "QiOg"  },  // Quinoctogintillion
            { 1e261, "SxOg"  },  // Sexoctogintillion
            { 1e264, "SpOg"  },  // Septenoctogintillion
            { 1e267, "OcOg"  },  // Octooctogintillion
            { 1e270, "NoOg"  },  // Novenoctogintillion
            // ── Nonagintillions (10^273 – 10^300) ───────────────────────────
            { 1e273, "Ng"    },  // Nonagintillion
            { 1e276, "UNg"   },  // Unnonagintillion
            { 1e279, "DNg"   },  // Duononagintillion
            { 1e282, "TNg"   },  // Trenonagintillion
            { 1e285, "QaNg"  },  // Quattuornonagintillion
            { 1e288, "QiNg"  },  // Quinnonagintillion
            { 1e291, "SxNg"  },  // Sexnonagintillion
            { 1e294, "SpNg"  },  // Septennonagintillion
            { 1e297, "OcNg"  },  // Octononagintillion
            { 1e300, "NoNg"  },  // Novennonagintillion
            // ── Centillion (10^303) — practical Double ceiling ───────────────
            { 1e303, "Cg"    },  // Centillion
    };

    private NumberUtil() {}

    /**
     * Formats a number with a short suffix for clean display.
     * Covers from 1,000 (K) all the way up to Centillion (10^300).
     *
     * Examples:
     *   523              → "523"
     *   1_500            → "1.5K"
     *   27_351           → "27.35K"
     *   2_500_000        → "2.5M"
     *   6_837_500_000    → "6.84B"
     *   1.5e24           → "1.5Sp"
     */
    public static String formatRaw(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) return "∞";
        if (value < 0) return "-" + formatRaw(-value);

        // Walk the suffix table from largest to smallest
        for (int i = SUFFIXES.length - 1; i >= 0; i--) {
            double threshold = (double) SUFFIXES[i][0];
            String suffix    = (String)  SUFFIXES[i][1];
            if (value >= threshold) {
                return SHORT_FMT.format(value / threshold) + suffix;
            }
        }
        // Below 1,000 — plain number
        return SHORT_FMT.format(value);
    }

    /**
     * Returns the unabbreviated number (used in admin/internal contexts
     * where precision matters more than readability).
     */
    public static String formatExact(double value) {
        return RAW.format(value);
    }

    /**
     * Formats a multiplier with K/M/B suffix and "x" prefix.
     * Examples:  26 → "x26"   48,727 → "x48.7K"   938,843 → "x938.8K"
     * Uses the same suffix table as formatRaw so the display is always compact.
     */
    public static String formatMultiplier(double value) {
        return "x" + formatRaw(value);
    }

    public static String formatPercent(double value) {
        return SHORT_FMT.format(value) + "%";
    }
}
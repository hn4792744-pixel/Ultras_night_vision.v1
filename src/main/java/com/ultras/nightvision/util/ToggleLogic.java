package com.ultras.nightvision.util;

/** Pure, easily-testable decision logic used by /nv all: if anyone online is currently missing the effect,
 *  the mass-toggle turns it ON for everyone; only once every single online player already has it does the
 *  command turn it OFF for everyone. */
public final class ToggleLogic {
    private ToggleLogic() {}

    public static boolean decideTurnOn(Iterable<Boolean> currentlyEnabledStates) {
        for (boolean enabled : currentlyEnabledStates) {
            if (!enabled) return true;
        }
        return false;
    }
}

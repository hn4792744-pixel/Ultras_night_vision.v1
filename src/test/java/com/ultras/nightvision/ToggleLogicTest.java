package com.ultras.nightvision;

import static org.junit.jupiter.api.Assertions.*;

import com.ultras.nightvision.util.ToggleLogic;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToggleLogicTest {
    @Test
    void turnsOnWhenAnyoneIsMissingIt() {
        assertTrue(ToggleLogic.decideTurnOn(List.of(true, false, true)));
        assertTrue(ToggleLogic.decideTurnOn(List.of(false, false)));
    }

    @Test
    void turnsOffOnlyWhenEveryoneAlreadyHasIt() {
        assertFalse(ToggleLogic.decideTurnOn(List.of(true, true, true)));
    }

    @Test
    void emptyServerDoesNothingHarmful() {
        // No one online to affect either way; decideTurnOn defaults to false (no-op either state).
        assertFalse(ToggleLogic.decideTurnOn(List.of()));
    }
}

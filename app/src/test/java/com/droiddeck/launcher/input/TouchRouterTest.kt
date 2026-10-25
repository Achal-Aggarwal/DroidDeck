package com.droiddeck.launcher.input

import com.droiddeck.launcher.session.SessionPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchRouterTest {
    private fun snapshot(
        uiBlocking: Boolean = false,
        mode: String = SessionPrefs.TOUCH_AUTO,
        scrollLayer: Boolean = false,
        steamSession: Boolean = false,
        steamDesktopUi: Boolean = false,
    ) = TouchRouter.Snapshot(uiBlocking, mode, scrollLayer, steamSession, steamDesktopUi)

    @Test fun uiBlockingWinsOverEverything() {
        for (mode in listOf(SessionPrefs.TOUCH_GESTURES, SessionPrefs.TOUCH_PAD, SessionPrefs.TOUCH_DIRECT, SessionPrefs.TOUCH_OFF, SessionPrefs.TOUCH_AUTO)) {
            assertEquals(TouchRouter.Route.BLOCKED, TouchRouter.decide(snapshot(uiBlocking = true, mode = mode, scrollLayer = true)))
        }
    }

    @Test fun gesturesModeBeatsScrollLayer() {
        assertEquals(TouchRouter.Route.GESTURES, TouchRouter.decide(snapshot(mode = SessionPrefs.TOUCH_GESTURES, scrollLayer = true)))
    }

    @Test fun scrollLayerInterceptsDirectAndAuto() {
        assertEquals(TouchRouter.Route.SCROLL, TouchRouter.decide(snapshot(mode = SessionPrefs.TOUCH_DIRECT, scrollLayer = true)))
        assertEquals(TouchRouter.Route.SCROLL, TouchRouter.decide(snapshot(mode = SessionPrefs.TOUCH_AUTO, scrollLayer = true)))
    }

    @Test fun offModeConsumes() {
        assertEquals(TouchRouter.Route.OFF, TouchRouter.decide(snapshot(mode = SessionPrefs.TOUCH_OFF)))
    }

    @Test fun explicitPadAndDirectIgnoreSteamState() {
        for (desktop in listOf(true, false)) {
            assertEquals(TouchRouter.Route.TOUCHPAD, TouchRouter.decide(snapshot(mode = SessionPrefs.TOUCH_PAD, steamSession = true, steamDesktopUi = desktop)))
            assertEquals(TouchRouter.Route.DIRECT, TouchRouter.decide(snapshot(mode = SessionPrefs.TOUCH_DIRECT, steamSession = true, steamDesktopUi = desktop)))
        }
    }

    @Test fun autoIsDirectInSteamBigPictureAndTouchpadElsewhere() {
        assertEquals(TouchRouter.Route.DIRECT, TouchRouter.decide(snapshot(steamSession = true, steamDesktopUi = false)))
        assertEquals(TouchRouter.Route.TOUCHPAD, TouchRouter.decide(snapshot(steamSession = true, steamDesktopUi = true)))
        assertEquals(TouchRouter.Route.TOUCHPAD, TouchRouter.decide(snapshot(steamSession = false, steamDesktopUi = false)))
    }

    @Test fun steamKeyboardZoneOnlyOutsideTheDesktop() {
        assertTrue(TouchRouter.steamKeyboardZone(snapshot(steamSession = true, steamDesktopUi = false)))
        assertFalse(TouchRouter.steamKeyboardZone(snapshot(steamSession = true, steamDesktopUi = true)))
        assertFalse(TouchRouter.steamKeyboardZone(snapshot(steamSession = false, steamDesktopUi = false)))
    }
}

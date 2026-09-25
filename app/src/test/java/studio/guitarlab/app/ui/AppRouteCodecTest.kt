package studio.guitarlab.app.ui

import kotlin.test.assertEquals
import org.junit.Test

class AppRouteCodecTest {
    @Test
    fun everyNavigationStateRoundTripsIncludingOpaqueProjectIdentifiers() {
        val screens = listOf(
            AppScreen.Home,
            AppScreen.Activity(),
            AppScreen.Activity("operation:automatic-backup"),
            AppScreen.NewProject,
            AppScreen.Prepare("project:a/b?c"),
            AppScreen.Studio("project:a/b?c"),
            AppScreen.Export("project:a/b?c"),
            AppScreen.Options(),
            AppScreen.Options("project:a/b?c"),
            AppScreen.Diagnostics(),
            AppScreen.Diagnostics("project:a/b?c"),
            AppScreen.AudioProbe(),
            AppScreen.AudioProbe("p:1"),
            AppScreen.CodecProbe(),
            AppScreen.CodecProbe("p:2"),
            AppScreen.Backup(),
            AppScreen.Backup("p:3"),
            AppScreen.Backup("p:4", returnToHome = true),
        )

        screens.forEach { screen -> assertEquals(screen, AppRouteCodec.decode(AppRouteCodec.encode(screen))) }
    }

    @Test
    fun malformedOrIncompleteStudioRoutesFailClosedToHome() {
        assertEquals(AppScreen.Home, AppRouteCodec.decode(""))
        assertEquals(AppScreen.Home, AppRouteCodec.decode("unknown"))
        assertEquals(AppScreen.Home, AppRouteCodec.decode("studio:"))
        assertEquals(AppScreen.Home, AppRouteCodec.decode("prepare:"))
        assertEquals(AppScreen.Home, AppRouteCodec.decode("export:"))
    }
}

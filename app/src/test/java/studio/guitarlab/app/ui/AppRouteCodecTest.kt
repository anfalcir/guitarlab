package studio.guitarlab.app.ui

import kotlin.test.assertEquals
import org.junit.Test

class AppRouteCodecTest {
    @Test
    fun everyNavigationStateRoundTripsIncludingOpaqueProjectIdentifiers() {
        val screens = listOf(
            AppScreen.Home,
            AppScreen.NewProject,
            AppScreen.Studio("project:a/b?c"),
            AppScreen.Options(),
            AppScreen.Options("project:a/b?c"),
            AppScreen.AudioProbe(),
            AppScreen.AudioProbe("p:1"),
            AppScreen.CodecProbe(),
            AppScreen.CodecProbe("p:2"),
        )

        screens.forEach { screen -> assertEquals(screen, AppRouteCodec.decode(AppRouteCodec.encode(screen))) }
    }

    @Test
    fun malformedOrIncompleteStudioRoutesFailClosedToHome() {
        assertEquals(AppScreen.Home, AppRouteCodec.decode(""))
        assertEquals(AppScreen.Home, AppRouteCodec.decode("unknown"))
        assertEquals(AppScreen.Home, AppRouteCodec.decode("studio:"))
    }
}

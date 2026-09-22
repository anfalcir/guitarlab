package studio.guitarlab.app.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import studio.guitarlab.app.MainActivity
import studio.guitarlab.app.ui.AppRouteCodec
import studio.guitarlab.app.ui.AppScreen

/** Typed notification route contract shared by foreground/background operations. */
object AppNotificationDeepLink {
    const val EXTRA_ROUTE = "studio.guitarlab.app.extra.NOTIFICATION_ROUTE"

    fun intent(context: Context, destination: AppScreen): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ROUTE, AppRouteCodec.encode(destination))
        }

    fun pendingIntent(context: Context, destination: AppScreen): PendingIntent {
        val route = AppRouteCodec.encode(destination)
        return PendingIntent.getActivity(
            context,
            route.hashCode() and Int.MAX_VALUE,
            intent(context, destination),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun route(intent: Intent?): String? =
        intent?.getStringExtra(EXTRA_ROUTE)?.takeIf { it.isNotBlank() }
}

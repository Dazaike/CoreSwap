package com.coreswap.mode

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.coreswap.app.R

object AppShortcuts {
    fun setup(context: Context) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        val ancIntent = Intent(context, SetNoiseCancelingActivity::class.java).apply {
            action = Intent.ACTION_VIEW
        }
        val transparencyIntent = Intent(context, SetTransparencyActivity::class.java).apply {
            action = Intent.ACTION_VIEW
        }
        val normalIntent = Intent(context, SetNormalActivity::class.java).apply {
            action = Intent.ACTION_VIEW
        }
        val shortcuts = listOf(
            ShortcutInfo.Builder(context, "anc")
                .setShortLabel(context.getString(R.string.shortcut_anc_short))
                .setLongLabel(context.getString(R.string.shortcut_anc_long))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_anc))
                .setIntent(ancIntent)
                .build(),
            ShortcutInfo.Builder(context, "transparency")
                .setShortLabel(context.getString(R.string.shortcut_transparency_short))
                .setLongLabel(context.getString(R.string.shortcut_transparency_long))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_transparency))
                .setIntent(transparencyIntent)
                .build(),
            ShortcutInfo.Builder(context, "normal")
                .setShortLabel(context.getString(R.string.shortcut_normal_short))
                .setLongLabel(context.getString(R.string.shortcut_normal_long))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_normal))
                .setIntent(normalIntent)
                .build(),
        )
        try {
            shortcutManager.dynamicShortcuts = shortcuts
        } catch (_: Throwable) {
            // Ignore if launcher/OS restricts dynamic shortcuts
        }
    }
}

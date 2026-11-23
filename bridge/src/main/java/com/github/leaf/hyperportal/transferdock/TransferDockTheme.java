// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

/**
 * Liquid Glass light/dark palette for the TransferDock panel.
 *
 * Designed to match the translucent frosted-glass appearance of the HyperOS /
 * HyperDock sidebar so opening the TransferDock feels like an in-place sheet
 * transition rather than a separate UI surface.
 */
final class TransferDockTheme {

    final boolean night;
    final int panelFill;
    final int panelTintTop;
    final int panelTintMid;
    final int panelTintBottom;
    final int panelRimTop;
    final int panelRimMid;
    final int panelRimBottom;
    final int panelInnerGlow;
    final int panelStroke;
    final int primaryText;
    final int secondaryText;
    final int cardBackground;
    final int cardStroke;
    final int cardText;
    final int cardSecondaryText;
    final int footerBackground;
    final int footerStroke;
    final int footerText;
    final int divider;
    final int emptyIcon;
    final int pressedOverlay;

    private TransferDockTheme(boolean night, boolean blurAvailable) {
        this.night = night;
        if (night) {
            panelFill = Color.argb(blurAvailable ? 104 : 228, 30, 34, 42);
            panelTintTop = Color.argb(blurAvailable ? 124 : 234, 48, 54, 66);
            panelTintMid = Color.argb(blurAvailable ? 92 : 226, 26, 30, 38);
            panelTintBottom = Color.argb(blurAvailable ? 112 : 232, 34, 40, 50);
            panelRimTop = Color.argb(128, 255, 255, 255);
            panelRimMid = Color.argb(46, 224, 236, 255);
            panelRimBottom = Color.argb(88, 210, 228, 255);
            panelInnerGlow = Color.argb(28, 255, 255, 255);
            panelStroke = Color.argb(58, 255, 255, 255);
            primaryText = Color.argb(245, 255, 255, 255);
            secondaryText = Color.argb(176, 235, 242, 255);
            cardBackground = Color.argb(blurAvailable ? 56 : 92, 255, 255, 255);
            cardStroke = Color.argb(64, 255, 255, 255);
            cardText = Color.argb(242, 255, 255, 255);
            cardSecondaryText = Color.argb(150, 226, 236, 255);
            footerBackground = Color.argb(blurAvailable ? 62 : 96, 255, 255, 255);
            footerStroke = Color.argb(72, 255, 255, 255);
            footerText = Color.argb(245, 255, 255, 255);
            divider = Color.argb(66, 255, 255, 255);
            emptyIcon = Color.argb(115, 235, 244, 255);
            pressedOverlay = Color.argb(44, 255, 255, 255);
        } else {
            panelFill = Color.argb(blurAvailable ? 78 : 232, 244, 248, 255);
            panelTintTop = Color.argb(blurAvailable ? 104 : 240, 255, 255, 255);
            panelTintMid = Color.argb(blurAvailable ? 68 : 230, 242, 247, 255);
            panelTintBottom = Color.argb(blurAvailable ? 92 : 236, 234, 242, 255);
            panelRimTop = Color.argb(228, 255, 255, 255);
            panelRimMid = Color.argb(138, 255, 255, 255);
            panelRimBottom = Color.argb(196, 244, 250, 255);
            panelInnerGlow = Color.argb(76, 255, 255, 255);
            panelStroke = Color.argb(185, 255, 255, 255);
            primaryText = Color.argb(232, 22, 28, 38);
            secondaryText = Color.argb(168, 38, 48, 64);
            cardBackground = Color.argb(blurAvailable ? 168 : 232, 255, 255, 255);
            cardStroke = Color.argb(215, 255, 255, 255);
            cardText = Color.argb(230, 28, 34, 44);
            cardSecondaryText = Color.argb(148, 48, 58, 74);
            footerBackground = Color.argb(blurAvailable ? 162 : 230, 255, 255, 255);
            footerStroke = Color.argb(215, 255, 255, 255);
            footerText = Color.argb(232, 22, 28, 38);
            divider = Color.argb(110, 120, 140, 168);
            emptyIcon = Color.argb(102, 54, 68, 88);
            pressedOverlay = Color.argb(28, 32, 48, 72);
        }
    }

    static boolean isNight(Context context) {
        return (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    static TransferDockTheme resolve(Context context, boolean blurAvailable) {
        return new TransferDockTheme(isNight(context), blurAvailable);
    }

    static int scrimColor(Context context) {
        return scrimColor(isNight(context));
    }

    static int scrimColor(boolean night) {
        // Keep the background un-dimmed like the HyperDock sidebar so switching
        // between the dock and TransferDock has no jarring screen flash.
        return Color.TRANSPARENT;
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * ColorOS 17 TransferDock panel (right side drawer).
 *
 * Reference: com.oplus.smartsidebar 17.9.12 / TransferDockPanel.kt and
 * res/layout/layout_transfer_panel (y-.xml):
 * - panel width 146dp, panel height 578dp
 * - title bar 40dp with 8dp top margin; 14sp title
 * - card 122dp x 84dp, 17dp radius, 8dp bottom margin
 * - clear footer 72dp, 114dp x 40dp pill
 *
 * The view itself is hosted in a bounded WindowManager window by
 * {@link TransferDockController}.  That lets the compositor blur only the
 * rectangle behind the panel instead of blurring the whole screen.
 */
public class TransferDockPanel extends FrameLayout {

    private static final String TAG = "TransferDockPanel";

    public interface OnPanelDismissListener {
        void onPanelDismiss();
    }

    public interface OnThemeChangedListener {
        void onThemeChanged(boolean night);
    }

    private static final PathInterpolator sEaseInterpolator =
            new PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f);

    private static final int PANEL_WIDTH_DP = 132;
    private static final int PANEL_HEIGHT_DP = 578;
    private static final int HEADER_HEIGHT_DP = 40;
    private static final int FOOTER_HEIGHT_DP = 64;
    private static final int CARD_HEIGHT_DP = 84;
    private static final int CARD_RADIUS_DP = 20;
    private static final int PANEL_RADIUS_DP = 32;
    private static final int BLUR_RADIUS_DP = 44;

    private final float mDensity;
    private int mPanelWidthPx;
    private int mPanelHeightPx;
    private int mPanelRadiusPx;
    private boolean mAlignRight = true;

    private LinearLayout mDrawerLayout;
    private RelativeLayout mHeaderBar;
    private View mHeaderDivider;
    private TextView mTitleView;
    private ImageView mBackBtn;
    private FrameLayout mContentContainer;
    private LinearLayout mEmptyView;
    private ImageView mEmptyIcon;
    private TextView mEmptyTitle;
    private TextView mEmptyHint;
    private ScrollView mScrollView;
    private LinearLayout mCardContainer;
    private FrameLayout mFooter;
    private TextView mClearBtn;

    private ValueAnimator mAnim;
    private OnPanelDismissListener mDismissListener;
    private OnThemeChangedListener mThemeListener;
    private TransferDockController mController;
    private TransferDockDialogHost mDialogHost;
    private boolean mDragActive;
    private boolean mDragPanelAside;
    private long mDragStartedAt;
    private TransferDockTheme mTheme;
    private boolean mBlurAvailable;
    private boolean mIsDismissing = false;
    private boolean mLastNotifiedNight;
    private Drawable mLocalBlurDrawable;
    private Method mBlurSetRadius;
    private Method mBlurSetCorner;
    private Method mBlurSetColor;

    public TransferDockPanel(@NonNull Context context) {
        this(context, null);
    }

    public TransferDockPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mDensity = context.getResources().getDisplayMetrics().density;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mPanelWidthPx = dp(PANEL_WIDTH_DP);
        int defaultHeight = (int) (dm.heightPixels * 0.58f);
        mPanelHeightPx = Math.min(Math.max(defaultHeight, dp(420)), Math.max(dp(360), dm.heightPixels - dp(120)));
        mPanelRadiusPx = dp(PANEL_RADIUS_DP);
        mTheme = TransferDockTheme.resolve(context, false);
        mLastNotifiedNight = mTheme.night;
        init();
    }

    public void setController(TransferDockController controller) {
        this.mController = controller;
    }

    public void setOnDismissListener(OnPanelDismissListener listener) {
        this.mDismissListener = listener;
    }

    public void setOnThemeChangedListener(OnThemeChangedListener listener) {
        this.mThemeListener = listener;
    }

    public void setPanelGeometry(int widthPx, int heightPx, int cornerRadiusPx, boolean alignRight) {
        boolean changed = false;
        if (widthPx > 0 && widthPx != mPanelWidthPx) {
            mPanelWidthPx = widthPx;
            changed = true;
        }
        if (heightPx > 0 && heightPx != mPanelHeightPx) {
            mPanelHeightPx = heightPx;
            changed = true;
        }
        int resolvedRadius = cornerRadiusPx > 0 ? cornerRadiusPx : dp(PANEL_RADIUS_DP);
        int maxRadius = Math.max(dp(18), mPanelWidthPx / 3);
        resolvedRadius = Math.min(resolvedRadius, maxRadius);
        if (resolvedRadius != mPanelRadiusPx) {
            mPanelRadiusPx = resolvedRadius;
            changed = true;
        }
        mAlignRight = alignRight;
        if (changed && mDrawerLayout != null) {
            applyTheme();
        }
    }

    public int getPanelHeightPx() {
        return mPanelHeightPx;
    }

    public int getPanelWidthPx() {
        return mPanelWidthPx;
    }

    public int getPanelRadiusPx() {
        return mPanelRadiusPx;
    }

    private int dp(float value) {
        return (int) (value * mDensity + 0.5f);
    }

    private void init() {
        setClipChildren(false);
        setBackgroundColor(Color.TRANSPARENT);

        // 1. The panel itself.  The window is exactly this size, so the
        // background blur applied by TransferDockController is local.
        mDrawerLayout = new LinearLayout(getContext());
        mDrawerLayout.setOrientation(LinearLayout.VERTICAL);
        mDrawerLayout.setClipToOutline(true);
        mDrawerLayout.setBackground(buildPanelBackground());
        addView(mDrawerLayout, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // 2. Header: back / centered title, with glass divider matching HyperDock.
        mHeaderBar = new RelativeLayout(getContext());
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(HEADER_HEIGHT_DP));
        headerLp.topMargin = dp(10);
        mDrawerLayout.addView(mHeaderBar, headerLp);

        mBackBtn = new ImageView(getContext());
        mBackBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mBackBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        mBackBtn.setContentDescription("关闭");
        mBackBtn.setOnClickListener(v -> dismissWithAnim());
        RelativeLayout.LayoutParams backLp = new RelativeLayout.LayoutParams(dp(36), dp(36));
        backLp.addRule(RelativeLayout.ALIGN_PARENT_START);
        backLp.addRule(RelativeLayout.CENTER_VERTICAL);
        backLp.leftMargin = dp(6);
        mHeaderBar.addView(mBackBtn, backLp);

        mTitleView = new TextView(getContext());
        mTitleView.setText("中转站");
        mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mTitleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mTitleView.setGravity(Gravity.CENTER);
        mTitleView.setSingleLine(true);
        mTitleView.setIncludeFontPadding(false);
        RelativeLayout.LayoutParams titleLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        titleLp.addRule(RelativeLayout.CENTER_IN_PARENT);
        mHeaderBar.addView(mTitleView, titleLp);

        mHeaderDivider = new View(getContext());
        LinearLayout.LayoutParams headerDividerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(1.5f)));
        headerDividerLp.leftMargin = dp(14);
        headerDividerLp.rightMargin = dp(14);
        headerDividerLp.topMargin = dp(2);
        headerDividerLp.bottomMargin = dp(4);
        mDrawerLayout.addView(mHeaderDivider, headerDividerLp);

        // 3. Scrollable list.
        mContentContainer = new FrameLayout(getContext());
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        mDrawerLayout.addView(mContentContainer, contentLp);

        mEmptyView = new LinearLayout(getContext());
        mEmptyView.setOrientation(LinearLayout.VERTICAL);
        mEmptyView.setGravity(Gravity.CENTER);
        mEmptyView.setPadding(dp(12), dp(20), dp(12), dp(20));

        mEmptyIcon = new ImageView(getContext());
        mEmptyIcon.setImageDrawable(TransferDockRes.getFileDockIcon(getContext()));
        mEmptyView.addView(mEmptyIcon, new LinearLayout.LayoutParams(dp(36), dp(36)));

        mEmptyTitle = new TextView(getContext());
        mEmptyTitle.setText("暂无内容");
        mEmptyTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        mEmptyTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emptyTitleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        emptyTitleLp.topMargin = dp(8);
        mEmptyView.addView(mEmptyTitle, emptyTitleLp);

        mEmptyHint = new TextView(getContext());
        mEmptyHint.setText("拖入图文随时存");
        mEmptyHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        mEmptyHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emptyHintLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        emptyHintLp.topMargin = dp(3);
        mEmptyView.addView(mEmptyHint, emptyHintLp);

        mContentContainer.addView(mEmptyView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        mScrollView = new ScrollView(getContext());
        mScrollView.setVerticalScrollBarEnabled(false);
        mScrollView.setOverScrollMode(OVER_SCROLL_NEVER);
        mScrollView.setClipToPadding(false);
        mScrollView.setPadding(dp(10), dp(6), dp(10), dp(6));

        mCardContainer = new LinearLayout(getContext());
        mCardContainer.setOrientation(LinearLayout.VERTICAL);
        mCardContainer.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        mScrollView.addView(mCardContainer, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        mContentContainer.addView(mScrollView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // 4. Bottom clear pill adapted to the panel width.
        mFooter = new FrameLayout(getContext());
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(FOOTER_HEIGHT_DP));
        mDrawerLayout.addView(mFooter, footerLp);

        mClearBtn = new TextView(getContext());
        mClearBtn.setText("清空");
        mClearBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        mClearBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mClearBtn.setGravity(Gravity.CENTER);
        mClearBtn.setIncludeFontPadding(false);
        mClearBtn.setOnClickListener(v -> clearTransferDock());
        FrameLayout.LayoutParams clearLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(38));
        clearLp.gravity = Gravity.CENTER;
        clearLp.leftMargin = dp(12);
        clearLp.rightMargin = dp(12);
        mFooter.addView(mClearBtn, clearLp);

        View footerDivider = new View(getContext());
        FrameLayout.LayoutParams dividerLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(1.5f)));
        dividerLp.gravity = Gravity.TOP;
        dividerLp.leftMargin = dp(14);
        dividerLp.rightMargin = dp(14);
        mFooter.addView(footerDivider, dividerLp);
        mFooter.setTag(footerDivider);

        // 5. Drag listener: MUST return true on ACTION_DRAG_STARTED to receive ACTION_DRAG_ENDED.
        setOnDragListener((v, event) -> {
            int action = event.getAction();
            if (action == DragEvent.ACTION_DRAG_STARTED) {
                mDragActive = true;
                mDragPanelAside = true;
                mDragStartedAt = android.os.SystemClock.uptimeMillis();
                return true;
            }
            if (action == DragEvent.ACTION_DRAG_ENTERED
                    || action == DragEvent.ACTION_DRAG_LOCATION) {
                // Ignore the initial ENTERED at the drag origin.  Once the
                // panel has slid aside, moving into its left 24dp expands it
                // without interrupting the drag, so the user can drag back
                // out on the same gesture.
                maybeExpandDragPanel(event.getX());
                return true;
            }
            if (action == DragEvent.ACTION_DRAG_EXITED) {
                if (mDragActive && !mDragPanelAside) {
                    mDragPanelAside = true;
                    if (mController != null) {
                        mController.onPanelDragExited();
                    }
                }
                return true;
            }
            if (action == DragEvent.ACTION_DRAG_ENDED) {
                mDragActive = false;
                mDragPanelAside = false;
                boolean success = event.getResult();
                Log.i(TAG, "ACTION_DRAG_ENDED received in TransferDockPanel, success=" + success);
                if (mController != null) {
                    mController.onPanelDragEnded(success);
                } else {
                    restoreFromDrag();
                }
                return true;
            }
            return false;
        });

        applyTheme();
        reloadItems();
    }

    private Drawable buildGlassSurfaceDrawable() {
        GradientDrawable tint = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{mTheme.panelTintTop, mTheme.panelTintMid, mTheme.panelTintBottom});
        tint.setCornerRadius(mPanelRadiusPx);
        return tint;
    }

    private Drawable buildPanelBackground() {
        Drawable surface = buildGlassSurfaceDrawable();
        Drawable rim = new LiquidGlassRimDrawable(mDensity, mPanelRadiusPx, mTheme);
        return new LayerDrawable(new Drawable[]{surface, rim});
    }

    private Drawable buildCardBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(mTheme.cardBackground);
        bg.setCornerRadius(dp(CARD_RADIUS_DP));
        bg.setStroke(Math.max(1, dp(0.85f)), mTheme.cardStroke);
        return bg;
    }

    private Drawable buildIconBackground() {
        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(mTheme.pressedOverlay);
        pressed.setCornerRadius(dp(18));

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{}, new GradientDrawable());
        return states;
    }

    private Drawable buildFooterBackground() {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(mTheme.footerBackground);
        shape.setCornerRadius(dp(19));
        shape.setStroke(Math.max(1, dp(0.85f)), mTheme.footerStroke);
        return new RippleDrawable(ColorStateList.valueOf(mTheme.pressedOverlay), shape, null);
    }

    private void applyTheme() {
        mTheme = TransferDockTheme.resolve(getContext(), mBlurAvailable);

        if (mLocalBlurDrawable != null && configureLocalBlur()) {
            mDrawerLayout.setBackground(buildLocalBlurBackground());
        } else {
            mDrawerLayout.setBackground(buildPanelBackground());
        }
        mTitleView.setText("中转站");
        mTitleView.setTextColor(mTheme.primaryText);
        mBackBtn.setImageDrawable(TransferDockRes.getBackIcon(
                getContext(), mDensity, mTheme.primaryText));
        mBackBtn.setBackground(buildIconBackground());
        if (mHeaderDivider != null) {
            mHeaderDivider.setBackground(new GlassDividerDrawable(mTheme.divider));
        }

        mEmptyIcon.setColorFilter(mTheme.emptyIcon);
        mEmptyTitle.setTextColor(mTheme.primaryText);
        mEmptyTitle.setAlpha(0.76f);
        mEmptyHint.setTextColor(mTheme.secondaryText);
        mEmptyHint.setAlpha(0.76f);

        mClearBtn.setTextColor(mTheme.footerText);
        mClearBtn.setBackground(buildFooterBackground());

        Object tag = mFooter.getTag();
        if (tag instanceof View) {
            ((View) tag).setBackground(new GlassDividerDrawable(mTheme.divider));
        }

        if (mThemeListener != null && mLastNotifiedNight != mTheme.night) {
            mLastNotifiedNight = mTheme.night;
            mThemeListener.onThemeChanged(mTheme.night);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyTheme();
        reloadItems();
    }

    /**
     * Called by the controller when the compositor blur state changes.
     * The panel keeps a readable translucent fallback when blur is disabled.
     */
    /**
     * Re-resolve the system night mode.  A detached View does not receive
     * onConfigurationChanged(), so the controller calls this before every open.
     */
    public void refreshTheme() {
        applyTheme();
        reloadItems();
    }

    public void setBackdropBlurAvailable(boolean available) {
        // Once the local BackgroundBlurDrawable is installed it is independent
        // of the cross-window blur switch.  Do not downgrade it.
        if (mLocalBlurDrawable != null || mBlurAvailable == available) {
            return;
        }
        mBlurAvailable = available;
        applyTheme();
        reloadItems();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // The platform BackgroundBlurDrawable is local: it records a blur
        // region around this view's render node instead of the whole display.
        mDrawerLayout.post(this::installLocalBlur);
    }

    private void installLocalBlur() {
        if (mLocalBlurDrawable != null || !isAttachedToWindow()) {
            return;
        }
        try {
            Method getViewRootImpl = View.class.getDeclaredMethod("getViewRootImpl");
            getViewRootImpl.setAccessible(true);
            Object viewRoot = getViewRootImpl.invoke(mDrawerLayout);
            if (viewRoot == null) {
                throw new IllegalStateException("ViewRootImpl is null");
            }
            Method create = viewRoot.getClass().getMethod("createBackgroundBlurDrawable");
            Object drawable = create.invoke(viewRoot);
            if (!(drawable instanceof Drawable)) {
                throw new IllegalStateException("no BackgroundBlurDrawable");
            }
            mLocalBlurDrawable = (Drawable) drawable;
            mBlurSetRadius = drawable.getClass().getMethod("setBlurRadius", int.class);
            mBlurSetCorner = drawable.getClass().getMethod("setCornerRadius", float.class);
            mBlurSetColor = drawable.getClass().getMethod("setColor", int.class);
            if (!configureLocalBlur()) {
                throw new IllegalStateException("cannot configure BackgroundBlurDrawable");
            }
            mBlurAvailable = true;
            mDrawerLayout.setBackground(buildLocalBlurBackground());
            applyTheme();
            Log.i(TAG, "local BackgroundBlurDrawable installed");
        } catch (Throwable t) {
            mLocalBlurDrawable = null;
            mBlurSetRadius = null;
            mBlurSetCorner = null;
            mBlurSetColor = null;
            mBlurAvailable = false;
            mDrawerLayout.setBackground(buildPanelBackground());
            applyTheme();
            Log.w(TAG, "local blur unavailable, using translucent panel", t);
        }
    }

    private boolean configureLocalBlur() {
        if (mLocalBlurDrawable == null || mBlurSetRadius == null
                || mBlurSetCorner == null || mBlurSetColor == null) {
            return false;
        }
        try {
            mBlurSetRadius.invoke(mLocalBlurDrawable, dp(BLUR_RADIUS_DP));
            mBlurSetCorner.invoke(mLocalBlurDrawable, (float) mPanelRadiusPx);
            // The tint and specular rim are separate layers above the blur
            // drawable so the real-time backdrop stays crisp and luminous.
            mBlurSetColor.invoke(mLocalBlurDrawable, Color.TRANSPARENT);
            mLocalBlurDrawable.invalidateSelf();
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "configure local blur failed", t);
            return false;
        }
    }

    private Drawable buildLocalBlurBackground() {
        Drawable surface = buildGlassSurfaceDrawable();
        Drawable rim = new LiquidGlassRimDrawable(mDensity, mPanelRadiusPx, mTheme);
        return new LayerDrawable(new Drawable[]{mLocalBlurDrawable, surface, rim});
    }

    public void restoreFromDrag() {
        mDrawerLayout.animate().cancel();
        mDrawerLayout.animate().alpha(1.0f).setDuration(180).start();
    }

    public void reloadItems() {
        TransferDockManager mgr = TransferDockManager.getInstance(getContext());
        List<TransferDockItem> list = mgr.getItems();
        mCardContainer.removeAllViews();

        boolean empty = list.isEmpty();
        mEmptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        mScrollView.setVisibility(empty ? View.GONE : View.VISIBLE);
        mFooter.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (empty) {
            return;
        }

        for (TransferDockItem item : list) {
            mCardContainer.addView(createCardView(item));
        }
        mScrollView.scrollTo(0, 0);
    }

    private View createCardView(TransferDockItem item) {
        FrameLayout card = new FrameLayout(getContext());
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(CARD_HEIGHT_DP));
        cardLp.gravity = Gravity.CENTER_HORIZONTAL;
        cardLp.bottomMargin = dp(8);
        card.setLayoutParams(cardLp);
        card.setBackground(buildCardBackground());
        card.setClipToOutline(true);
        card.setContentDescription(item.getTitle());

        if (item.getType() == TransferDockItem.TYPE_IMAGE && item.getThumbnailPath() != null) {
            ImageView thumbView = new ImageView(getContext());
            thumbView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bmp = BitmapFactory.decodeFile(item.getThumbnailPath());
            if (bmp != null) {
                thumbView.setImageBitmap(bmp);
            }
            card.addView(thumbView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        } else {
            LinearLayout textLayout = new LinearLayout(getContext());
            textLayout.setOrientation(LinearLayout.VERTICAL);
            textLayout.setPadding(dp(10), dp(9), dp(10), dp(8));

            TextView titleView = new TextView(getContext());
            titleView.setText(item.getTitle());
            titleView.setTextColor(mTheme.cardText);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            titleView.setMaxLines(3);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            titleView.setLineSpacing(dp(1.5f), 1.0f);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
            textLayout.addView(titleView, titleLp);

            TextView timeView = new TextView(getContext());
            timeView.setText(item.getDetail());
            timeView.setTextColor(mTheme.cardSecondaryText);
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            timeView.setSingleLine(true);
            timeView.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            textLayout.addView(timeView, timeLp);

            card.addView(textLayout, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }

        card.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().cancel();
                v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start();
            } else if (action == android.view.MotionEvent.ACTION_UP
                    || action == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().cancel();
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
            }
            return false;
        });

        card.setOnLongClickListener(v -> {
            v.animate().cancel();
            v.setScaleX(1.0f);
            v.setScaleY(1.0f);
            startDragOut(card, item);
            return true;
        });

        card.setOnClickListener(v -> showItemOptions(item));
        return card;
    }

    private void maybeExpandDragPanel(float panelX) {
        if (!mDragActive || !mDragPanelAside) {
            return;
        }
        if (android.os.SystemClock.uptimeMillis() - mDragStartedAt < 260) {
            return;
        }
        // At the aside position only the inner 36dp strip is visible. Expand
        // only when the finger is safely inside the 24dp inner band.
        if (mAlignRight) {
            if (panelX > dp(24)) {
                return;
            }
        } else {
            if (panelX < Math.max(0, getWidth() - dp(24))) {
                return;
            }
        }
        mDragPanelAside = false;
        if (mController != null) {
            mController.onPanelDragEntered();
        }
    }

    @Nullable
    private ClipData buildDragClip(TransferDockItem item) {
        if (item.getType() != TransferDockItem.TYPE_TEXT) {
            Uri uri = TransferDockManager.getInstance(getContext()).exportItemUri(item);
            if (uri == null) {
                Log.w(TAG, "cannot export non-text item for drag: " + item.getTitle());
                return null;
            }
            String mime = item.getMimeType();
            if (TextUtils.isEmpty(mime)) {
                mime = "application/octet-stream";
            }
            List<String> mimeTypes = new ArrayList<>();
            mimeTypes.add(mime);
            if (mime.startsWith("image/")) {
                if (!"image/*".equals(mime)) {
                    mimeTypes.add("image/*");
                }
            } else if (!"*/*".equals(mime)) {
                mimeTypes.add("*/*");
            }
            mimeTypes.add(ClipDescription.MIMETYPE_TEXT_URILIST);
            mimeTypes.add("application/x-dragshare");

            ClipDescription desc = new ClipDescription(
                    TextUtils.isEmpty(item.getTitle()) ? "内容" : item.getTitle(),
                    mimeTypes.toArray(new String[0]));
            // The URI is the entire payload.  Never put the card title in
            // the text slot: text/plain targets would otherwise accept the
            // image drag and insert a title string instead of the bitmap.
            ClipData.Item clipItem = new ClipData.Item(null, null, null, uri);
            Log.i(TAG, "startDragOut: uri=" + uri + " mime=" + mime
                    + " types=" + mimeTypes);
            return new ClipData(desc, clipItem);
        }

        String text = item.getText();
        if (TextUtils.isEmpty(text)) text = item.getTitle();
        return ClipData.newPlainText("TransferDock", text);
    }

    private void startDragOut(View cardView, TransferDockItem item) {
        cardView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        ClipData clipData = buildDragClip(item);
        if (clipData == null) {
            Toast.makeText(getContext(), "图片准备失败，请重试", Toast.LENGTH_SHORT).show();
            return;
        }

        View.DragShadowBuilder shadow = new View.DragShadowBuilder(cardView) {
            @Override
            public void onProvideShadowMetrics(android.graphics.Point outShadowSize,
                                               android.graphics.Point outShadowTouchPoint) {
                outShadowSize.set(cardView.getWidth(), cardView.getHeight());
                outShadowTouchPoint.set(cardView.getWidth() / 2, cardView.getHeight() / 2);
            }

            @Override
            public void onDrawShadow(Canvas canvas) {
                float prevAlpha = cardView.getAlpha();
                cardView.setAlpha(1.0f);
                super.onDrawShadow(canvas);
                cardView.setAlpha(prevAlpha);
            }
        };

        mDrawerLayout.animate().cancel();
        mDrawerLayout.setAlpha(1.0f);
        mDragActive = true;
        mDragPanelAside = true;
        mDragStartedAt = android.os.SystemClock.uptimeMillis();

        if (mController != null) {
            mController.onPanelDragStarted();
        }

        int flags = View.DRAG_FLAG_GLOBAL | View.DRAG_FLAG_GLOBAL_URI_READ;
        boolean started = cardView.startDragAndDrop(clipData, shadow, item, flags);
        if (!started) {
            mDragActive = false;
            mDragPanelAside = false;
            if (mController != null) {
                mController.onPanelDragEnded(false);
            } else {
                restoreFromDrag();
            }
        }
    }

    private void copyItem(TransferDockItem item) {
        ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (item.getType() == TransferDockItem.TYPE_IMAGE) {
            Uri uri = TransferDockManager.getInstance(getContext()).exportItemUri(item);
            if (uri != null) {
                cm.setPrimaryClip(ClipData.newUri(getContext().getContentResolver(), "image", uri));
                Toast.makeText(getContext(), "图片已复制到剪贴板", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(getContext(), "图片准备失败，请重试", Toast.LENGTH_SHORT).show();
            return;
        }
        String t = item.getText() != null ? item.getText() : item.getTitle();
        cm.setPrimaryClip(ClipData.newPlainText("TransferDock", t));
        Toast.makeText(getContext(), "文本已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    private TransferDockDialogHost getDialogHost() {
        if (mDialogHost == null) {
            mDialogHost = new TransferDockDialogHost(getContext());
        }
        return mDialogHost;
    }

    private void showItemOptions(TransferDockItem item) {
        try {
            getDialogHost().showItemOptions(
                    item,
                    () -> copyItem(item),
                    () -> shareItem(item),
                    () -> deleteItem(item));
        } catch (Throwable t) {
            Log.w(TAG, "Miuix item dialog failed, using platform fallback", t);
            showItemOptionsLegacy(item);
        }
    }

    private void shareItem(TransferDockItem item) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        Uri uri = item.getType() != TransferDockItem.TYPE_TEXT
                ? TransferDockManager.getInstance(getContext()).exportItemUri(item)
                : null;
        if (item.getType() != TransferDockItem.TYPE_TEXT && uri == null) {
            Toast.makeText(getContext(), "图片准备失败，请重试", Toast.LENGTH_SHORT).show();
            return;
        }
        if (uri != null) {
            String mime = item.getMimeType();
            if (TextUtils.isEmpty(mime)) {
                mime = item.getType() == TransferDockItem.TYPE_IMAGE ? "image/*" : "*/*";
            }
            shareIntent.setType(mime);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setClipData(ClipData.newUri(
                    getContext().getContentResolver(), "内容", uri));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    item.getText() != null ? item.getText() : item.getTitle());
        }
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent chooser = Intent.createChooser(shareIntent, "分享到");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(chooser);
        dismissWithAnim();
    }

    private void deleteItem(TransferDockItem item) {
        TransferDockManager.getInstance(getContext()).removeItem(item.getId());
        reloadItems();
        Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
        if (TransferDockManager.getInstance(getContext()).getItemCount() == 0
                && mController != null) {
            mController.hideFloatBar();
        }
    }

    private void showItemOptionsLegacy(TransferDockItem item) {
        try {
            String[] options = new String[]{"复制", "分享", "删除"};
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                    getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert);
            builder.setTitle(item.getTitle());
            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    copyItem(item);
                } else if (which == 1) {
                    shareItem(item);
                } else if (which == 2) {
                    deleteItem(item);
                }
            });
            builder.setNegativeButton("取消", null);
            android.app.AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(resolveDialogWindowType());
            }
            dialog.show();
        } catch (Throwable t) {
            Log.w(TAG, "showItemOptionsLegacy dialog failed, fallback to copy", t);
            copyItem(item);
        }
    }

    private int resolveDialogWindowType() {
        if ("com.android.systemui".equals(getContext().getPackageName())) {
            return 2024; // TYPE_NAVIGATION_BAR_PANEL
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void clearTransferDock() {
        TransferDockManager.getInstance(getContext()).clearAll();
        reloadItems();
        Toast.makeText(getContext(), "中转站已清空", Toast.LENGTH_SHORT).show();
        if (mController != null) {
            mController.hideFloatBar();
        }
    }

    public void showWithAnim() {
        mIsDismissing = false;
        if (mAnim != null) {
            mAnim.removeAllListeners();
            mAnim.cancel();
        }
        mDrawerLayout.animate().cancel();
        mDrawerLayout.setAlpha(1.0f);
        mDrawerLayout.setTranslationX(0f);
        mDrawerLayout.setPivotX(mAlignRight ? getPanelWidthPx() : 0f);
        mDrawerLayout.setPivotY(mPanelHeightPx / 2f);
        mDrawerLayout.setScaleX(0.94f);
        mDrawerLayout.setScaleY(0.97f);
        setVisibility(View.VISIBLE);

        // COUI reveal is a spring-like growth anchored to the right edge.
        mAnim = ValueAnimator.ofFloat(0f, 1f);
        mAnim.setDuration(280);
        mAnim.setInterpolator(sEaseInterpolator);
        mAnim.addUpdateListener(animation -> {
            float frac = animation.getAnimatedFraction();
            float scaleX = 0.94f + 0.06f * frac;
            float scaleY = 0.97f + 0.03f * frac;
            mDrawerLayout.setScaleX(scaleX);
            mDrawerLayout.setScaleY(scaleY);
        });
        mAnim.start();
    }

    public void dismissWithAnim() {
        if (mIsDismissing) return;
        mIsDismissing = true;

        if (mAnim != null) {
            mAnim.removeAllListeners();
            mAnim.cancel();
        }
        mDrawerLayout.animate().cancel();
        final float startScaleX = mDrawerLayout.getScaleX();
        final float startScaleY = mDrawerLayout.getScaleY();
        final float startAlpha = mDrawerLayout.getAlpha();
        mAnim = ValueAnimator.ofFloat(0f, 1f);
        mAnim.setDuration(200);
        mAnim.setInterpolator(sEaseInterpolator);
        mAnim.addUpdateListener(animation -> {
            float frac = animation.getAnimatedFraction();
            mDrawerLayout.setScaleX(startScaleX - 0.06f * frac);
            mDrawerLayout.setScaleY(startScaleY - 0.03f * frac);
            mDrawerLayout.setAlpha(startAlpha * (1.0f - 0.35f * frac));
        });
        mAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(View.GONE);
                if (mDismissListener != null) {
                    mDismissListener.onPanelDismiss();
                }
            }
        });
        mAnim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        mDragActive = false;
        if (mDialogHost != null) {
            mDialogHost.dismiss();
        }
        if (mAnim != null) {
            mAnim.removeAllListeners();
            mAnim.cancel();
            mAnim = null;
        }
        mDrawerLayout.animate().cancel();
        mLocalBlurDrawable = null;
        mBlurSetRadius = null;
        mBlurSetCorner = null;
        mBlurSetColor = null;
        super.onDetachedFromWindow();
    }

    /**
     * Draws a crisp specular glass rim (luminous top/bottom edge + subtle
     * inner highlight) matching HyperDock's liquid-glass sidebar panel.
     */
    private static final class LiquidGlassRimDrawable extends Drawable {

        private final float mCornerRadius;
        private final float mOuterStrokeWidth;
        private final float mInnerStrokeWidth;
        private final int mRimTop;
        private final int mRimMid;
        private final int mRimBottom;
        private final int mInnerGlow;
        private final Paint mOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF mOuterRect = new RectF();
        private final RectF mInnerRect = new RectF();
        private int mLastHeight = -1;

        LiquidGlassRimDrawable(float density, float cornerRadius, TransferDockTheme theme) {
            mCornerRadius = cornerRadius;
            mOuterStrokeWidth = Math.max(1.2f, 1.25f * density);
            mInnerStrokeWidth = Math.max(1.0f, 1.0f * density);
            mRimTop = theme.panelRimTop;
            mRimMid = theme.panelRimMid;
            mRimBottom = theme.panelRimBottom;
            mInnerGlow = theme.panelInnerGlow;

            mOuterPaint.setStyle(Paint.Style.STROKE);
            mOuterPaint.setStrokeWidth(mOuterStrokeWidth);

            mInnerPaint.setStyle(Paint.Style.STROKE);
            mInnerPaint.setStrokeWidth(mInnerStrokeWidth);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            android.graphics.Rect b = getBounds();
            if (b.width() <= 0 || b.height() <= 0) {
                return;
            }
            if (mLastHeight != b.height()) {
                mLastHeight = b.height();
                mOuterPaint.setShader(new LinearGradient(
                        b.left, b.top, b.left, b.bottom,
                        new int[]{mRimTop, mRimMid, mRimMid, mRimBottom},
                        new float[]{0f, 0.28f, 0.72f, 1f},
                        Shader.TileMode.CLAMP));
                mInnerPaint.setShader(new LinearGradient(
                        b.left, b.top, b.left, b.top + Math.max(1f, b.height() * 0.35f),
                        mInnerGlow, Color.TRANSPARENT,
                        Shader.TileMode.CLAMP));
            }
            float halfOuter = mOuterStrokeWidth * 0.5f;
            mOuterRect.set(
                    b.left + halfOuter,
                    b.top + halfOuter,
                    b.right - halfOuter,
                    b.bottom - halfOuter);
            canvas.drawRoundRect(mOuterRect, mCornerRadius, mCornerRadius, mOuterPaint);

            float inset = mOuterStrokeWidth + mInnerStrokeWidth * 0.5f;
            float innerRadius = Math.max(0f, mCornerRadius - mOuterStrokeWidth);
            mInnerRect.set(
                    b.left + inset,
                    b.top + inset,
                    b.right - inset,
                    b.bottom - inset);
            canvas.drawRoundRect(mInnerRect, innerRadius, innerRadius, mInnerPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            mOuterPaint.setAlpha(alpha);
            mInnerPaint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mOuterPaint.setColorFilter(colorFilter);
            mInnerPaint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /**
     * Soft glass divider pill with faded ends, matching the section divider
     * inside HyperDock.
     */
    private static final class GlassDividerDrawable extends Drawable {

        private final int mCenterColor;
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF mRect = new RectF();
        private int mLastWidth = -1;

        GlassDividerDrawable(int centerColor) {
            mCenterColor = centerColor;
            mPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            android.graphics.Rect b = getBounds();
            if (b.width() <= 0 || b.height() <= 0) {
                return;
            }
            if (mLastWidth != b.width()) {
                mLastWidth = b.width();
                int transparent = Color.argb(0, Color.red(mCenterColor),
                        Color.green(mCenterColor), Color.blue(mCenterColor));
                mPaint.setShader(new LinearGradient(
                        b.left, 0f, b.right, 0f,
                        new int[]{transparent, mCenterColor, mCenterColor, transparent},
                        new float[]{0f, 0.16f, 0.84f, 1f},
                        Shader.TileMode.CLAMP));
            }
            mRect.set(b.left, b.top, b.right, b.bottom);
            float radius = b.height() * 0.5f;
            canvas.drawRoundRect(mRect, radius, radius, mPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}

package com.apktoolai.companion

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * zxing-android-embedded's stock CaptureActivity locks to landscape by
 * default, which is why the scanner opened sideways. Subclassing it (with
 * no code changes needed - see the matching manifest entry which sets
 * android:screenOrientation="portrait") is the library's documented way to
 * keep the scan screen upright, matching how someone naturally holds their
 * phone up to scan a code off a laptop/monitor.
 */
class PortraitCaptureActivity : CaptureActivity()

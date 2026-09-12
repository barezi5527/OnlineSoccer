# Keep rules for release builds (currently no reflection/serialization of custom classes at root).
-keepattributes *Annotation*
-dontwarn okhttp3.internal.platform.**
# ClearRead ProGuard Rules

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Compose
-dontwarn androidx.compose.**

# PDFBox
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.apache.fontbox.**
-dontwarn org.apache.pdfbox.**
-dontwarn com.gemalto.jp2.**

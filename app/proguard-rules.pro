# App-specific R8 rules for the release build.
#
# This file is intentionally free of keep rules. The release build was verified and the
# actual R8 input can be read at app/build/outputs/mapping/release/configuration.txt:
#
#   * androidx.room:room-runtime ships its own rule:
#         -keep class * extends androidx.room.RoomDatabase { void <init>(); }
#     which is what keeps the generated BusinessDatabase_Impl reachable.
#   * androidx.room:room-ktx, the Compose artifacts (ui, foundation, material3, animation)
#     and androidx.window:window-core ship intentionally empty rule files declaring
#     themselves safe to shrink.
#   * The release dex was inspected and still contains BusinessDatabase_Impl and
#     BusinessEntryDao_Impl.
#
# Also note that optimization { packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**") }
# in app/build.gradle.kts limits R8 to those packages, so this module's own classes are
# neither shrunk nor renamed today.
#
# Add a rule below only for code reached by reflection that no library covers itself - for
# example a future serialization library, a JNI entry point, or a Room @TypeConverter whose
# type lives outside the optimized packages.

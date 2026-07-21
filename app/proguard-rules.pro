# App-specific R8 keep rules. Library rules (Ktor, OkHttp, kotlinx.serialization,
# Koin, Tink) ship as consumer rules inside the dependencies and don't belong here.

# Tink (via androidx.security:security-crypto) references ErrorProne annotations
# that are compileOnly and absent at runtime.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

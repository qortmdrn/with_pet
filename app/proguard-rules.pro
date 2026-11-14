# Pleos Connect SDK는 Gson으로 capability·모델을 직렬화한다.
# R8이 제네릭 시그니처나 필드 이름을 지우면 Vehicle onRequestCapability가
# "TypeToken must be created with a type argument"로 실패한다 (릴리스 빌드에서만).
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep class ai.pleos.** { *; }
-keep interface ai.pleos.** { *; }
-dontwarn ai.pleos.**

# Gson TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn com.google.gson.**

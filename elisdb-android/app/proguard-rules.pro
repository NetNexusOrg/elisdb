# ============================================================
# ElisDB - R8/ProGuard rules
# ============================================================

# ---- kotlinx.serialization -------------------------------------
# @Serializable route types (Navigation3) accesam os geradores via reflexão.
-keepattributes *Annotation*, InnerClasses, Signature

# Mantém os serializers gerados (ex: ClientList$$serializer).
-keep, includedescriptorclasses class com.example.elisdb.**$$serializer { *; }
-keepclassmembers class com.example.elisdb.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Os DTOs @Serializable do sync remoto (app/src/main/java/.../remote) são
# (de)serializados por reflexão pelo kotlinx.serialization.
-keep class com.example.elisdb.remote.** { *; }
-keepclassmembers class com.example.elisdb.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
    *** Companion;
}
-keepclassmembers enum com.example.elisdb.remote.** { *; }

# ---- Ktor client (networking stack) ----------------------------------------
# O Ktor inspeciona plugins/serializers por reflexão; manter sem ofuscação.
# `-keep ... { *; }` já preserva todos os membros, pelo que não é preciso um
# `-keepclassmembers` separado.
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ---- eddsa (ed25519 signing) ---------------------------------------------
# O Signer usa a API net.i2p/eddsa; mantida para a assinatura EdDSA e o
# EdDSASecurityProvider registado via reflexão.
-keep class net.i2p.crypto.eddsa.** { *; }
-dontwarn net.i2p.crypto.eddsa.**

# ---- osmdroid --------------------------------------------------
# As bibliotecas do osmdroid usam reflexão (POIs, providers de tiles),
# pelo que não devem ser ofuscadas nem encurtadas.
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
# As regras para org.apache.http/commons, org.bouncycastle e org.slf4j foram
# removidas: nenhuma dessas bibliotecas é dependência do projeto nem é
# referenciada pelo osmdroid 6.1.20 ou pelo eddsa (verificado nas respetivas
# classes). Eram regras sem efeito que só alargavam a configuração.

# ---- CameraX ----------------------------------------------------
# O CameraX envia as suas próprias regras, mas mantê-las à parte é seguro
# para o preview/fotografia via reflexão.
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ---- Coil -------------------------------------------------------
# O Coil3 gere os modelos/anotações por reflexão.
-keepclassmembers class coil3.** { *; }
-dontwarn coil3.**

# Manter as entidades Room e a camada de localização (usada no mapa).
# As restantes classes de `com.example.elisdb.data` (repositório, encoder)
# são instanciadas diretamente e não precisam de keep próprio.
-keep class com.example.elisdb.data.local.** { *; }
-keep class com.example.elisdb.location.** { *; }
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // AÑADIDO: Plugin de Google Services para Firebase - Versión 4.4.2 según tu captura de Firebase
    id("com.google.gms.google-services") version "4.4.2" apply false
}
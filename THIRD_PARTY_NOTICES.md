# Avisos de terceiros

A licença PolyForm Noncommercial 1.0.0 do ficheiro [`LICENSE`](LICENSE) cobre apenas o código da CalSyncAlarm escrito pelo autor. Cada componente abaixo mantém a sua própria licença e os seus direitos de autor.

## Componentes incluídos na aplicação

Todos sob a **Apache License 2.0** (SPDX `Apache-2.0`), conforme o campo de licença do POM de cada artefacto. Texto da licença: <https://www.apache.org/licenses/LICENSE-2.0>

| Componente | Versão | Autor |
|---|---|---|
| AndroidX Core KTX (`androidx.core:core-ktx`) | 1.13.1 | The Android Open Source Project |
| AndroidX Lifecycle Runtime KTX (`androidx.lifecycle:lifecycle-runtime-ktx`) | 2.8.7 | The Android Open Source Project |
| AndroidX Activity Compose (`androidx.activity:activity-compose`) | 1.9.3 | The Android Open Source Project |
| Jetpack Compose UI, UI Graphics e UI Tooling Preview | 1.7.5 (BOM 2024.10.01) | The Android Open Source Project |
| Compose Material 3 (`androidx.compose.material3:material3`) | 1.3.1 | The Android Open Source Project |
| Compose Material Icons Extended | 1.7.5 | The Android Open Source Project |
| AndroidX WorkManager (`androidx.work:work-runtime-ktx`) | 2.9.1 | The Android Open Source Project |
| Biblioteca padrão do Kotlin (`org.jetbrains.kotlin:kotlin-stdlib`) | versão resolvida pelo Gradle | JetBrains s.r.o. e colaboradores do Kotlin |

As versões são as resolvidas pelo Gradle na compilação e podem mudar em versões futuras da aplicação.

## Só em compilação (não vão no APK)

Android Gradle Plugin, plugin do Compose para o Kotlin e Gradle Wrapper (`gradle/wrapper/gradle-wrapper.jar`, Apache 2.0).

## Ícones e imagens

Os desenhos vetoriais usados no ícone da aplicação e no ícone de alarme (`ic_launcher_foreground.xml`, `baseline_add_alarm_24.xml`) derivam dos Material Icons da Google, sob Apache License 2.0, e os ficheiros mantêm o cabeçalho original. A composição e as cores do ícone são do autor. Os restantes ficheiros do ícone (`ic_launcher*.webp`, `ic_launcher-playstore.png`) são versões geradas do mesmo desenho. Sem fontes nem outras imagens de terceiros.

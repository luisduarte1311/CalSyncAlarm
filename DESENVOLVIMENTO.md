# Projeto: CalSyncAlarm

- Autor: Luís Duarte (incluir crédito discreto de autoria no primeiro arranque / ecrã Sobre).
- ApplicationId e pacote: `com.lduarte1311.calsyncalarm`
- Idioma da UI e comentários no código: Português de Portugal (PT-PT), claro e didático.

## Diretivas de Engenharia (Obrigatórias)

1. **Simplicidade Radical (Anti-Overengineering):** Menor quantidade de código limpo possível. Proibido criar factories, use-cases abstratos, Hilt/Dagger, Room/SQLite ou dependências pesadas desnecessárias.
2. **Stack Enxuta:** Kotlin + Jetpack Compose (Material 3 / Material You com Dynamic Color em Android 12+). Apenas bibliotecas essenciais (AndroidX Core, Compose Material 3, WorkManager, SharedPreferences + org.json).
3. **Ambiente Local:** O Windows tem OpenJDK 25 (`JAVA_HOME`) e Android SDK (`ANDROID_HOME`). Usar versão recente de Gradle / Android Gradle Plugin compatível com JDK 25, `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
4. **Offline-First & Privacidade:** PROIBIDO declarar `INTERNET` ou `ACCESS_NETWORK_STATE` no `AndroidManifest.xml`. Zero anúncios, zero analytics, zero bibliotecas de rede. O WorkManager traz `ACCESS_NETWORK_STATE` no manifesto fundido; está removida de propósito com `tools:node="remove"`. `allowBackup="false"`.

## Arquitetura Funcional (V2 Multi-Calendário — Caminho B)

- **Propósito:** Gerir múltiplos calendários do sistema (`CalendarContract`), cada um com a sua própria configuração isolada de antecedência, toque, volume, vibração, snooze e pré-alarme.
- **Primeiro Arranque:** Sem carrosséis. Ecrã limpo com crédito discreto ao autor + botão `+` para listar calendários locais (mostrando Nome + email da conta, sem o tipo "(com.google)") → adiciona calendário com valores pré-definidos sensatos (`1h30`, volume `90%`, snooze `10m`, vibração `ON`, pré-alarme `2h`).
- **UI Principal — Caminho B (`MainScreen`):**
  - Barra de ações no topo: botões `+` (adicionar calendário), `▶` (testar alarme), `↻` (atualizar todos os calendários com carimbo de hora) e `⚙` (Definições Globais / padrões para novos calendários).
  - Semáforo de permissões inteligente no topo (só visível se faltar alguma: `READ_CALENDAR`, `POST_NOTIFICATIONS` >= API 33, `canScheduleExactAlarms` >= API 31, `canUseFullScreenIntent` >= API 34, e atalho opcional para "Bateria sem restrições").
  - **Cartão Destacado com o Próximo Alarme Global:** Hora grande, data, nome do calendário associado e título do evento.
  - **Cartão Destacado de Snooze Ativo:** Visível quando há um alarme adiado, com hora de retorno e botão direto **"Desligar"**.
  - **Lista de Calendários Adicionados:** Cada cartão apresenta nome, conta, indicador de cor, contador de alarmes ativos, interruptor `ON/OFF` do calendário e botão `⚙` para aceder às definições isoladas desse calendário.
  - **Cartões neutros (tema claro e escuro):** os cartões da lista de calendários, da lista de alarmes de um calendário (`CalendarEventsScreen`) e das folhas de escolha de calendário usam `surfaceVariant` a 50 % com borda fina `outlineVariant` a 40 %; o desativado fica a 25 % e sem borda. A etiqueta do calendário no cartão do próximo alarme é opaca (`surface` + `onSurface`) para ter contraste em qualquer tema.
  - Tocar num cartão abre o ecrã `CalendarEventsScreen` com a lista dos próximos **30 dias** desse calendário específico e interruptores individuais `ON/OFF` por ocorrência.
- **Ecrã de Definições Limpo (`SettingsScreen`):**
  - Títulos limpos e elegantes (sem numeração `1.`, `2.`, etc.).
  - **Definições Globais (aberto pelo topo):** Padrões aplicados ao adicionar novos calendários (Antecedência, Som, Volume 0–100%, Vibração, Snooze e Notificação Pré-Alarme).
  - **Definições de Calendário (aberto pelo cartão do calendário):** Regras isoladas desse calendário específico mais botão discreto **"Remover este Calendário"** com confirmação.
  - **Cartão "Sobre" (fim das Definições, igual nas globais e nas de calendário):** ícone em círculo, "CalSyncAlarm", versão lida do `PackageInfo` (não é texto fixo), "Desenvolvido por Luís Duarte", linha "Sobre e informações" (`AboutDialog`: privacidade, aviso legal, licença PolyForm com botão "Ver código no GitHub", licenças de terceiros), "Reportar um problema" (botão que abre o email por `mailto:` com versão, modelo e Android; sem rede) e o botão do café (`paypal.me/lduarte1311`) **só nas Definições Globais**, de propósito (nas de calendário seria insistente). Cartão neutro como os restantes.
  - **Tempos Personalizados:** Seletores de Antecedência, Snooze e Pré-Alarme com presets + opção "Personalizado..." com campos numéricos (horas/minutos) e validação >= 1 min.
  - **Navegação com `BackHandler`:** Suporte a pilha de ecrãs no Compose, garantindo regresso exato ao ecrã anterior com o gesto/botão Voltar.
- **Ecrã de Despertar (`AlarmActivity`):** Activity isolada (`taskAffinity=""`, `excludeFromRecents="true"`, `showWhenLocked`, `turnScreenOn`, `USE_FULL_SCREEN_INTENT`). Mostra Hora + Evento + início do evento + **Snooze** (com tempo configurado do calendário) + **Desligar** direto.
- **Ícone da Aplicação:** Ícone adaptativo vetorial Material You em tons elegantes de verde e branco (fundo verde-escuro esmeralda, detalhes em verde menta e branco nítido), perfeitamente centrado e escalado na zona segura central (62% do canvas 108dp) com camada `<monochrome>` para suporte a ícones temáticos no Pixel Launcher / Android 13+.
- **Motor & Regras Invisíveis:**
  - `AlarmScheduler`: Agrega os próximos 30 dias de todos os calendários ativos (`enabled == true`).
  - **Fila Única no `AlarmManager`:** Registar no sistema apenas **1** `setAlarmClock()` (o próximo alarme cronológico ativo entre todos os calendários) e 1 agendamento para a respetiva notificação pré-alarme. Ao disparar/dispensar/saltar/adiar, agendar o seguinte.
  - Chave única por ocorrência no calendário: `EVENT_ID + BEGIN`. Limpeza automática de ocorrências com mais de 24h no passado.
  - **Falha de leitura ≠ sem eventos:** `CalendarReader.loadUpcomingOccurrences` devolve `null` se a leitura falhar. Nesse caso `AlarmScheduler.scheduleNextAlarm` NÃO mexe no alarme já agendado, mostra um aviso ("Não foi possível ler o calendário") e devolve `false`; `lastSyncTime` só é gravado se devolver `true`.
  - **Reagendamento:** além de abrir a app, snooze/desligar/saltar e reinício, o próximo alarme é agendado logo ao disparar (`AlarmReceiver`) e quando o calendário muda (`SyncWorker.enqueueChangeObserver`: gatilho de conteúdo em `CalendarContract.CONTENT_URI`, pausa 10 s, máximo 1 min). O `SyncWorker` periódico (6 h) é rede de segurança.
  - **Pré-alarme sem repetição:** `AppSettings.lastPreAlarmKey` guarda o último pré-alarme mostrado; o aviso aparece uma só vez por alarme e o obsoleto é cancelado.
  - **Filtros do calendário:** só eventos com hora (sem dia inteiro), não cancelados e com convite não recusado (`SELF_ATTENDEE_STATUS != DECLINED`).
  - **Evento sem alarme:** se a antecedência já passou e o evento ainda não começou, uma notificação avisa (uma por evento, uma só vez; `AppSettings.noAlarmNotifiedKeys`). Eventos cujo alarme já tocou não contam; a primeira execução só regista, sem avisar.
  - `AlarmService`: `ForegroundService` (`foregroundServiceType="mediaPlayback"`) aplicando os parâmetros específicos de volume, som e vibração do calendário do evento + timeout de segurança de 10 minutos (faz Dismiss + notificação silenciosa "Alarme perdido"). O volume original só é guardado uma vez (dois alarmes seguidos não corrompem a reposição).
  - `InitReceiver` (`BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`; não exportado e sem Direct Boot, porque antes do desbloqueio nem as definições nem o calendário estão acessíveis) + `SyncWorker` periódico via `WorkManager`.

## Distribuição e licença

- Código público (source-available) no próprio repositório `luisduarte1311/CalSyncAlarm`, sob a licença PolyForm Noncommercial 1.0.0 (`LICENSE`, com a linha `Required Notice`). Avisos de terceiros em `THIRD_PARTY_NOTICES.md` e apresentação em `README.md`. Nunca dizer "open source" (a licença não é aprovada pela OSI). Uso comercial só por acordo com o autor; aceitam-se issues e sugestões, não pull requests.
- Versão atual: 2.0 (`versionCode 7`), etiqueta `v2.0` (a `v1.6` ficou só como marco, sem release). A versão debug e a release usam chaves diferentes; nunca se distribui o APK debug.
- O `enableEdgeToEdge()` da `MainActivity` tem a película de contraste da barra de navegação desligada (`isNavigationBarContrastEnforced = false`) para não aparecer uma faixa branca no tema claro.

## Release assinada

- A chave (`.jks`) vive **fora do repositório** e nunca se lê nem se partilha; o `.gitignore` ignora `*.jks` e `*.keystore`. Sem a chave não se atualiza uma release já instalada: manter uma cópia fora do portátil e a palavra-passe num gestor de palavras-passe.
- `app/build.gradle.kts` assina a release só se existir a variável `CALSYNC_KEYSTORE`; a palavra-passe vem de `CALSYNC_KEYSTORE_PASS` e o alias (por defeito `calsyncalarm`) de `CALSYNC_KEY_ALIAS`. Sem variáveis, a compilação debug funciona igual.
- Compilar (PowerShell; a palavra-passe pede-se em modo oculto e a variável apaga-se no fim):

  ```powershell
  $env:CALSYNC_KEYSTORE = "C:\caminho\fora\do\repositorio\calsyncalarm-release.jks"; $s = Read-Host "Palavra-passe da chave" -AsSecureString; $env:CALSYNC_KEYSTORE_PASS = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)); .\gradlew.bat assembleRelease; Remove-Item Env:CALSYNC_KEYSTORE_PASS; Remove-Item Env:CALSYNC_KEYSTORE; .\gradlew.bat --stop
  ```

  O APK fica em `app/build/outputs/apk/release/app-release.apk` (se vier `-unsigned`, a variável não chegou ao Gradle).
- Verificar com as `build-tools` do SDK (usar o Java do Android Studio, não um Java 8 do PATH): `apksigner.jar verify --verbose --print-certs <apk>` deve dizer `Verifies` e o certificado `CN=Luis Duarte, C=PT`; `aapt2 dump badging <apk>` deve mostrar a versão certa e **nenhuma** permissão `INTERNET` nem `ACCESS_NETWORK_STATE`. Calcular o SHA-256 com `Get-FileHash <apk> -Algorithm SHA256`.
- Certificado de assinatura (SHA-256): `053239e92bf956c573c88005a1b585abc810247edcf0ac98b981134431418d6a`.
- Publicar: copiar o APK para uma pasta fora do repositório como `CalSyncAlarm-<versão>.apk`, criar a etiqueta `v<versão>` no commit final do `master` e criar a release no GitHub com o APK anexado e o SHA-256 nas notas (licença PolyForm, linha `Required Notice` e ligação aos avisos de terceiros).
- Passar da debug para a release no telemóvel obriga a desinstalar a debug (chaves diferentes) e perde os dados da app. Nunca se distribui o APK debug.

## Diagnóstico

- Erros do motor registam-se com `Log.e` e a tag `CalSyncAlarm` (`adb logcat -s CalSyncAlarm`).
- Alarmes agendados: `adb shell dumpsys alarm` filtrado por `calsyncalarm`.

## Critério de Conclusão

- O projeto deve compilar com sucesso usando `.\gradlew.bat assembleDebug` com **zero erros**, gerando o APK em `app/build/outputs/apk/debug/`.

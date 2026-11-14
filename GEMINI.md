# Projeto: Calendar Wake

- Autor: Luís Duarte (incluir crédito discreto de autoria no primeiro arranque / ecrã Sobre).
- ApplicationId: `com.luisduarte.calendarwake`
- Idioma da UI e comentários no código: Português de Portugal (PT-PT), claro e didático.

## Diretivas de Engenharia (Obrigatórias)

1. **Simplicidade Radical (Anti-Overengineering):** Menor quantidade de código limpo possível. Proibido criar factories, use-cases abstratos, Hilt/Dagger, Room/SQLite ou dependências pesadas desnecessárias.
2. **Stack Enxuta:** Kotlin + Jetpack Compose (Material 3 / Material You com Dynamic Color em Android 12+). Apenas bibliotecas essenciais (AndroidX Core, Compose Material 3, WorkManager, SharedPreferences + org.json).
3. **Ambiente Local:** O Windows tem OpenJDK 25 (`JAVA_HOME`) e Android SDK (`ANDROID_HOME`). Usar versão recente de Gradle / Android Gradle Plugin compatível com JDK 25, `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
4. **Offline-First & Privacidade:** PROIBIDO declarar `INTERNET` ou `ACCESS_NETWORK_STATE` no `AndroidManifest.xml`. Zero anúncios, zero analytics, zero bibliotecas de rede.

## Arquitetura Funcional (V2 Multi-Calendário — Caminho B)

- **Propósito:** Gerir múltiplos calendários do sistema (`CalendarContract`), cada um com a sua própria configuração isolada de antecedência, toque, volume, vibração, snooze e pré-alarme.
- **Primeiro Arranque:** Sem carrosséis. Ecrã limpo com crédito discreto ao autor + botão `+` para listar calendários locais (mostrando Nome + Conta/Tipo) → adiciona calendário com valores pré-definidos sensatos (`1h30`, volume `90%`, snooze `10m`, vibração `ON`, pré-alarme `2h`).
- **UI Principal — Caminho B (`MainScreen`):**
  - Barra de ações no topo: botões `+` (adicionar calendário), `▶` (testar alarme), `↻` (atualizar todos os calendários com carimbo de hora) e `⚙` (Definições Globais / padrões para novos calendários).
  - Semáforo de permissões inteligente no topo (só visível se faltar alguma: `READ_CALENDAR`, `POST_NOTIFICATIONS` >= API 33, `canScheduleExactAlarms` >= API 31, `canUseFullScreenIntent` >= API 34, e atalho opcional para "Bateria sem restrições").
  - **Cartão Destacado com o Próximo Alarme Global:** Hora grande, data, nome do calendário associado e título do evento.
  - **Cartão Destacado de Snooze Ativo:** Visível quando há um alarme adiado, com hora de retorno e botão direto **"Desligar"**.
  - **Lista de Calendários Adicionados:** Cada cartão apresenta nome, conta, indicador de cor, contador de alarmes ativos, interruptor `ON/OFF` do calendário e botão `⚙` para aceder às definições isoladas desse calendário.
  - Tocar num cartão abre o ecrã `CalendarEventsScreen` com a lista dos próximos **30 dias** desse calendário específico e interruptores individuais `ON/OFF` por ocorrência.
- **Ecrã de Definições Limpo (`SettingsScreen`):**
  - Títulos limpos e elegantes (sem numeração `1.`, `2.`, etc.).
  - **Definições Globais (aberto pelo topo):** Padrões aplicados ao adicionar novos calendários (Antecedência, Som, Volume 0–100%, Vibração, Snooze e Notificação Pré-Alarme).
  - **Definições de Calendário (aberto pelo cartão do calendário):** Regras isoladas desse calendário específico mais botão discreto **"Remover este Calendário"** com confirmação.
  - **Tempos Personalizados:** Seletores de Antecedência, Snooze e Pré-Alarme com presets + opção "Personalizado..." com campos numéricos (horas/minutos) e validação >= 1 min.
  - **Navegação com `BackHandler`:** Suporte a pilha de ecrãs no Compose, garantindo regresso exato ao ecrã anterior com o gesto/botão Voltar.
- **Ecrã de Despertar (`AlarmActivity`):** Activity isolada (`taskAffinity=""`, `excludeFromRecents="true"`, `showWhenLocked`, `turnScreenOn`, `USE_FULL_SCREEN_INTENT`). Mostra Hora + Evento + início do evento + **Snooze** (com tempo configurado do calendário) + **Desligar** direto.
- **Ícone da Aplicação:** Ícone adaptativo vetorial Material You em tons elegantes de verde e branco (fundo verde-escuro esmeralda, detalhes em verde menta e branco nítido), perfeitamente centrado e escalado na zona segura central (62% do canvas 108dp) com camada `<monochrome>` para suporte a ícones temáticos no Pixel Launcher / Android 13+.
- **Motor & Regras Invisíveis:**
  - `AlarmScheduler`: Agrega os próximos 30 dias de todos os calendários ativos (`enabled == true`).
  - **Fila Única no `AlarmManager`:** Registar no sistema apenas **1** `setAlarmClock()` (o próximo alarme cronológico ativo entre todos os calendários) e 1 agendamento para a respetiva notificação pré-alarme. Ao disparar/dispensar/saltar/adiar, agendar o seguinte.
  - Chave única por ocorrência no calendário: `EVENT_ID + BEGIN`. Limpeza automática de ocorrências com mais de 24h no passado.
  - `AlarmService`: `ForegroundService` (`foregroundServiceType="mediaPlayback"`) aplicando os parâmetros específicos de volume, som e vibração do calendário do evento + timeout de segurança de 10 minutos (faz Dismiss + notificação silenciosa "Alarme perdido").
  - `InitReceiver` (`BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`) + `SyncWorker` periódico via `WorkManager`.

## Critério de Conclusão

- O projeto deve compilar com sucesso usando `.\gradlew.bat assembleDebug` com **zero erros**, gerando o APK em `app/build/outputs/apk/debug/`.

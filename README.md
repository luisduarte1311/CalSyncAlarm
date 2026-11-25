# CalSyncAlarm

Alarmes para os teus eventos de calendário, no Android. A CalSyncAlarm lê os calendários que já tens no telemóvel e toca um alarme antes de cada evento, com a antecedência, o toque, o volume e o snooze que definires para cada calendário. Funciona sem rede.

Código disponível (source-available) sob a licença PolyForm Noncommercial 1.0.0. Não é open source: o uso comercial exige um acordo com o autor (ver [Licença](#licença)).

## Funcionalidades

- Vários calendários do sistema, cada um com a sua configuração: antecedência, toque, volume, vibração, snooze e aviso de pré-alarme.
- Considera os eventos dos próximos 30 dias e deixa ligar ou desligar cada ocorrência.
- Ecrã de alarme por cima do ecrã bloqueado, com Snooze e Desligar.
- Reagenda sozinha depois de reiniciares o telemóvel ou de mudares a hora.
- Ignora eventos de dia inteiro, cancelados e convites recusados.
- Avisa quando a antecedência já passou e o evento ainda não começou.
- Interface em português de Portugal, com cores dinâmicas (Android 12 ou superior) e tema claro e escuro.

## Privacidade

- Sem acesso à rede: a aplicação não declara `INTERNET` nem `ACCESS_NETWORK_STATE`.
- Sem analytics, sem anúncios e sem cópia de segurança na nuvem (`allowBackup="false"`).
- Lê os calendários e guarda as definições apenas no teu telemóvel.
- As ligações (email, página do autor) abrem noutra aplicação; a CalSyncAlarm não as carrega.

## Permissões

| Permissão | Para quê |
|---|---|
| Calendário (`READ_CALENDAR`) | Ler os eventos dos calendários que escolheres |
| Notificações (`POST_NOTIFICATIONS`) | Mostrar o pré-alarme e os avisos |
| Alarmes exatos (`SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`) | Tocar à hora certa |
| Ecrã inteiro (`USE_FULL_SCREEN_INTENT`) | Mostrar o alarme por cima do ecrã bloqueado |
| Serviço em primeiro plano (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) | Manter o som do alarme a tocar |
| `WAKE_LOCK`, `VIBRATE` | Acordar o telemóvel e vibrar |
| `RECEIVE_BOOT_COMPLETED` | Reagendar o alarme depois de reiniciares |

A aplicação também sugere, sem a exigir, a opção "Bateria sem restrições", para o Android não atrasar os alarmes.

Vais ver ainda uma permissão interna, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, acrescentada pela biblioteca AndroidX: só a própria aplicação a pode usar e não dá acesso a nada.

## Requisitos

Android 8.0 ou superior.

## Instalar o APK

A versão assinada mais recente está em [Releases](../../releases), com o SHA-256 do ficheiro nas notas. Depois de a descarregares:

1. Confirma o SHA-256 (`Get-FileHash .\ficheiro.apk` no PowerShell).
2. Permite instalar aplicações de "fontes desconhecidas" para o navegador ou gestor de ficheiros que usaste.
3. Se o Play Protect avisar, é porque a aplicação não vem da Play Store.
4. As atualizações são manuais: instala o APK novo por cima do anterior.

Certificado de assinatura (SHA-256), igual em todas as versões: `053239e92bf956c573c88005a1b585abc810247edcf0ac98b981134431418d6a`.

Uma versão de teste (debug) usa outra chave de assinatura; para passar para a versão assinada tens de desinstalar a de teste, e perdes as definições.

## Compilar

Precisas de um JDK e do Android SDK (`JAVA_HOME` e `ANDROID_HOME` definidos). Na raiz do projeto:

```powershell
.\gradlew.bat assembleDebug
```

O APK fica em `app/build/outputs/apk/debug/`. A compilação da release assinada e outras notas para quem desenvolve estão em [DESENVOLVIMENTO.md](DESENVOLVIMENTO.md).

## Reportar um problema

- Issues aqui no GitHub.
- Email: calsyncalarm.contact@gmail.com (também há um botão "Reportar um problema" nas Definições da aplicação).

Aceito issues e sugestões, mas não pull requests.

## Licença

O código está licenciado sob a [PolyForm Noncommercial License 1.0.0](LICENSE), com atribuição obrigatória: quem distribuir o software, ou uma cópia modificada, tem de passar a licença e a linha `Required Notice` do ficheiro `LICENSE`.

- Usar a aplicação nos teus próprios alarmes, incluindo os de trabalho, é uso pessoal.
- Dar a aplicação aos funcionários de uma empresa, ou qualquer uso comercial, exige falar primeiro com o autor: calsyncalarm.contact@gmail.com. Vale o texto da licença; esta frase é a intenção do autor.
- Dependências e ícones de terceiros mantêm as suas licenças (Apache 2.0). Ver [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Desenvolvido por Luís Duarte.

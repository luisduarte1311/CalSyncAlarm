\---

description: Compilar APK Debug e validar erros Gradle

\---

1\. Executa `.\\gradlew.bat assembleDebug` no terminal do projeto.

2\. Se houver erros de compilação ou dependências incompatíveis com JDK 25 / API 36, corrige-os e volta a compilar até obter `BUILD SUCCESSFUL`.

3\. Confirma o caminho exato do APK gerado em `app/build/outputs/apk/debug/`.


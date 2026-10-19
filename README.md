# Vortex

Baixador de vídeo (MP4) e áudio (MP3) para Android, sem anúncios.
Funciona com **YouTube, Instagram e TikTok**. Tudo roda no próprio celular
(yt-dlp + ffmpeg embutidos), sem servidor.

## O que ele faz

- Vídeo MP4 em todas as resoluções que o site oferecer (144p até 4K/8K), com tamanho estimado
- Áudio MP3 em 320 / 256 / 192 / 128 kbps, com capa e informações embutidas, ou o áudio original sem conversão
- Aparece no menu **Compartilhar** do YouTube, Instagram e TikTok: compartilhou, analisou
- Fila com até 2 downloads simultâneos, continuando com o app minimizado (notificação com progresso)
- Biblioteca com histórico, abrir, compartilhar e tentar de novo
- Vídeos salvos em `Movies/Vortex` e músicas em `Music/Vortex` (aparecem na galeria e no player)
- Motor yt-dlp atualiza sozinho a cada 3 dias, ou na hora pelo botão em Configurações

## Como gerar o APK

### Opção A — GitHub (sem instalar nada)

1. Crie um repositório no GitHub (pode ser privado) e envie esta pasta.
2. Abra a aba **Actions**. O workflow **Gerar APK** roda sozinho a cada push na `main`
   (ou clique em *Run workflow*).
3. Quando terminar (~6 min), baixe o artefato **vortex-apk**. Dentro dele:
   - `app-arm64-v8a-release.apk` → a maioria dos celulares (use este)
   - `app-armeabi-v7a-release.apk` → celulares antigos de 32 bits
   - `app-universal-release.apk` → funciona em qualquer um, mas é maior
4. Passe o APK para o celular e instale (permita "instalar apps desconhecidos").

### Opção B — Android Studio

1. Abra a pasta no Android Studio (Ladybug ou mais novo) e espere o Gradle sincronizar.
2. Para testar: botão ▶ com o celular conectado por USB (depuração USB ativa).
3. Para gerar o APK: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**
   com a variante `release`, ou no terminal: `./gradlew assembleRelease`.
   Os APKs ficam em `app/build/outputs/apk/release/`.

> Use sempre a variante **release** para testar fluidez. O Compose em modo debug
> é bem mais lento e não representa o desempenho real.

## Estrutura

```
app/src/main/java/dev/davi/vortex/
├── VortexApp.kt               inicialização
├── MainActivity.kt            tela + recebe links compartilhados
├── engine/Engine.kt           yt-dlp/ffmpeg: init e atualização
├── data/
│   ├── MediaAnalyzer.kt       lê o link e monta as opções de qualidade
│   ├── DownloadRepository.kt  fila, progresso, comandos do yt-dlp
│   ├── MediaSaver.kt          salva na galeria (MediaStore)
│   ├── HistoryStore.kt        histórico da biblioteca
│   └── Models.kt              modelos, erros amigáveis, formatação
├── service/                   serviço em primeiro plano + notificações
└── ui/                        telas, componentes neon e tema
```

## Observações

- **Instagram:** posts privados ou stories exigem login e não vão funcionar.
- **Se um site parar de funcionar:** Configurações → *Atualizar motor*.
- O APK é assinado com a chave de debug para instalar direto. Para distribuir,
  crie sua própria chave em `app/build.gradle.kts`.
- A Play Store não aceita apps que baixam do YouTube; distribua o APK diretamente.
- Baixe apenas conteúdo que você tem direito de baixar (o seu próprio, domínio
  público ou com permissão do autor); os termos do YouTube proíbem o download
  de vídeos fora dos recursos oficiais.

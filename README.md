# AOSP Modern QS

![LSPosed](https://img.shields.io/badge/LSPosed-m%C3%B3dulo-2ea44f)
![Xposed API](https://img.shields.io/badge/Xposed%20API-82-blue)
![Android](https://img.shields.io/badge/Android-12%2B%20(testado%20alvo%2016)-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/license-GPLv3-orange)

Módulo LSPosed de código aberto que remodela o painel de **Quick Settings** da SystemUI com o
visual moderno do Android 16 / AOSP:

- **Tiles 1x1 e 2x1** (compactos e largos) numa grade configurável (2 a 6 colunas)
- **Modo de edição dinâmico**: no editor de tiles, *toque duas vezes* num tile para alternar 1x1 ⇄ 2x1
- **Fundo translúcido** com cantos arredondados e cores dinâmicas (Material You)
- **Blur** de janela com raio ajustável
- Tela de configurações própria (Material You), com alterações lidas pela SystemUI via `XSharedPreferences`

> Prints conceituais: veja [`docs/screenshots`](docs/screenshots).

## Requisitos

- Android 12 ou superior (testes focados em Android 16)
- Root: **Magisk**, **KernelSU** ou **APatch**
- **LSPosed** (JingMatrix) instalado e funcionando

## Instalação

1. Baixe o APK em *Actions → Build APK → Artifacts* (ou compile, veja abaixo) e instale.
2. Abra o **LSPosed → Módulos → AOSP Modern QS**, ative e marque o escopo **Interface do sistema** (`com.android.systemui`).
3. Abra o app do módulo, ajuste as opções.
4. Reinicie a SystemUI (ou o aparelho). Mudanças em colunas/cantos (recursos) exigem reiniciar a SystemUI.

## Compatibilidade de ROMs

| ROM | Situação |
|---|---|
| AOSP / Pixel (view-based QS) | Alvo principal; nomes de classes do AOSP |
| LineageOS e derivadas | Esperado funcionar (SystemUI próxima do AOSP) |
| One UI (Samsung) | **Parcial**: a SystemUI usa classes `Sec*` e blur próprio. Veja "Adaptar a outra ROM" |
| Android 16 com QS em Compose (scene container) | A grade view-based não é usada: os hooks de grade não terão efeito |

## Como funciona (classes interceptadas)

| Classe (SystemUI) | Hook | Função |
|---|---|---|
| `qs.TileLayout` | `onMeasure` / `onLayout` | Grade com spans: 1x1 e 2x1 |
| `qs.QSPanel` | construtores / `onConfigurationChanged` | Fundo translúcido arredondado (cores dinâmicas) |
| `statusbar.BlurUtils` | `supportsBlursOnWindows` / `applyBlur` | Blur de janela com raio do usuário |
| `qs.customize.TileAdapter` | `onBindViewHolder` | Duplo toque no editor alterna 1x1 ⇄ 2x1 |
| Recursos `quick_settings_num_columns`, `qs_corner_radius` | `handleInitPackageResources` | Colunas e raio de canto |

Cada instalador é isolado: se uma classe não existir, o módulo registra o motivo no log e segue com os demais.
O hook de grade se desativa sozinho após 3 falhas, devolvendo o layout original.

### Adaptar a outra ROM

1. Ligue **Log de depuração** no app e reinicie a SystemUI.
2. Em LSPosed → Logs do módulo, procure `alvo encontrado` / `alvo ausente`.
3. Descompile o APK da SystemUI (jadx/apktool), ache os nomes equivalentes e edite `hook/Targets.kt`.
4. Se o editor alternar o tile errado, ajuste `EDIT_POSITION_OFFSET` em `Targets.kt`.

## Como compilar

**Android Studio:** abra a pasta, deixe o Gradle sincronizar e rode *Build → Build APK(s)*.

**Linha de comando** (JDK 17 + Gradle 8.9):

```bash
gradle wrapper --gradle-version 8.9   # gera o gradle wrapper
./gradlew assembleDebug
# APK em app/build/outputs/apk/debug/
```

**GitHub Actions:** o workflow `.github/workflows/build.yml` compila a cada push e publica o APK como artefato.

> A API `de.robv.android.xposed:api:82` vem do repositório `https://api.xposed.info/`.
> Se ele estiver fora do ar, baixe `XposedBridgeApi-82.jar`, coloque em `app/libs/` e troque a dependência por `compileOnly(files("libs/XposedBridgeApi-82.jar"))`.

## Problemas comuns

- **Nada muda:** confira se o escopo marca *Interface do sistema*, reinicie a SystemUI e veja o log (alvos ausentes = nomes de classe diferentes na sua ROM).
- **Config não é aplicada:** abra o app do módulo uma vez com ele ativo (o arquivo de preferências precisa ser criado como legível).
- **Tela do app diz "não ativo":** ative no LSPosed e force-pare o app.

## Limitações conhecidas

- Com `PagedTileLayout`, tiles 2x1 contam como 1 slot na paginação; ajuste linhas/colunas se a página ficar alta.
- O editor nativo não mostra o tamanho 2x1 na pré-visualização; o resultado aparece no painel.

## Licença

[GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.html). Adicione o texto oficial em um arquivo `LICENSE` na raiz
(GitHub → *Add file → Create new file → LICENSE* → *Choose a license template*).

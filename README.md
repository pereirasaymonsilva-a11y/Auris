# Auris 🎶

<p align="center">
  <img src="assets/icon.png" alt="Ícone do Auris" width="128"/>
</p>

<p align="center">
  <strong>Um player de música elegante, poderoso e totalmente em português</strong><br>
  Baseado no PixelPlayer · Construído com Jetpack Compose e Material Design 3
</p>

<p align="center">
  <img src="assets/screenshot1.jpg" alt="Captura de Tela 1" width="150" style="border-radius:26px;"/>
  <img src="assets/screenshot2.jpg" alt="Captura de Tela 2" width="150" style="border-radius:26px;"/>
  <img src="assets/screenshot3.jpg" alt="Captura de Tela 3" width="150" style="border-radius:26px;"/>
  <img src="assets/screenshot4.jpg" alt="Captura de Tela 4" width="150" style="border-radius:26px;"/>
  <img src="assets/screenshot5.jpg" alt="Captura de Tela 5" width="150" style="border-radius:26px;"/>
  <img src="assets/screenshot6.jpg" alt="Captura de Tela 6" width="150" style="border-radius:26px;"/>
</p>

<p align="center">
    <a href="https://github.com/pereirasaymonsilva-a11y/pixelplayer-apk/releases/latest">
        <img src="https://img.shields.io/github/v/release/pereirasaymonsilva-a11y/pixelplayer-apk?include_prereleases&logo=github&style=for-the-badge&label=Última%20versão" alt="Última versão">
    </a>
    <a href="https://github.com/pereirasaymonsilva-a11y/pixelplayer-apk/releases">
        <img src="https://img.shields.io/github/downloads/pereirasaymonsilva-a11y/pixelplayer-apk/total?logo=github&style=for-the-badge" alt="Total de Downloads">
    </a>
    <img src="https://img.shields.io/badge/Android-11%2B-green?style=for-the-badge&logo=android" alt="Android 11+">
    <img src="https://img.shields.io/badge/Kotlin-100%25-purple?style=for-the-badge&logo=kotlin" alt="Kotlin">
</p>

---

## ✨ Sobre o Auris

O **Auris** é uma versão personalizada com carinho a partir do código aberto do **PixelPlayer**, criado por [theovilardo](https://github.com/theovilardo).  
Ele foi traduzido integralmente para o português (Brasil), ganhou um tema claro em dourado e preto, teve ícones e identidade visual renovados, e agora é mantido como um projeto independente focado na comunidade lusófona.

Se você quer um player de música moderno, com Material You, equalizador, letras sincronizadas, suporte a múltiplos formatos e uma interface fluida, o Auris é para você.

---

## 🎨 Funcionalidades

### 🎵 Reprodução Poderosa
- **Media3 ExoPlayer** com suporte a FFmpeg
- Reprodução em segundo plano com controle total pela notificação
- Fila com arrastar e soltar, modos aleatório e repetição
- Transições suaves (gapless) e crossfade personalizável

### 🖼️ Interface Moderna
- **Material You** – cores que se adaptam ao seu papel de parede
- Animações fluidas e microinterações
- Temas claro e escuro (agora com tema claro dourado exclusivo do Auris)
- Extração de cores da capa do álbum

### 📚 Biblioteca Completa
- Suporte a MP3, FLAC, AAC, OGG, WAV, entre outros
- Navegação por Músicas, Álbuns, Artistas, Gêneros e Pastas
- Interpretação inteligente de múltiplos artistas (feat., ft., etc.)
- Agrupamento por Artista do Álbum

### 🎤 Letras Sincronizadas
- Busca automática via LRCLIB
- Suporte a arquivos .LRC embarcados
- Edição manual de letras e ajuste de sincronia

### 📊 Estatísticas e Descoberta
- Estatísticas de escuta detalhadas (hábitos, horários, faixas mais ouvidas)
- Mix Diária gerada por IA com base no seu histórico
- Busca completa na biblioteca

### 📲 Conectividade
- Chromecast e Bluetooth
- Widgets com Glance
- Android Auto (em breve)

### ⚙️ Avançado
- Editor de tags completo (TagLib)
- Equalizador de 10 bandas
- Playlists com IA (Gemini)
- Backup e restauração

---

## 🛠️ Stack Tecnológico

| Categoria | Tecnologia |
|----------|------------|
| **Linguagem** | Kotlin 100% |
| **UI** | Jetpack Compose + Material Design 3 |
| **Áudio** | Media3 ExoPlayer + FFmpeg |
| **Arquitetura** | MVVM com StateFlow/SharedFlow |
| **Injeção de Dependência** | Hilt |
| **Banco de Dados** | Room |
| **Rede** | Retrofit + OkHttp |
| **Imagens** | Coil |
| **Assíncrono** | Kotlin Coroutines & Flow |
| **Widgets** | Glance |
| **Metadados** | TagLib |

---

## 📱 Requisitos

- Android 11 (API 30) ou superior
- 4 GB de RAM recomendados

---

## ⬇️ Download

<p align="center">
  <a href="https://github.com/pereirasaymonsilva-a11y/pixelplayer-apk/releases/latest">
    <img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" alt="Baixar no GitHub" height="60">
  </a>
</p>

> ⚠️ **Aviso:** Sempre desinstale a versão anterior antes de instalar uma nova atualização do Auris.  
> O app está em versão beta e podem ocorrer conflitos se instalado por cima.

---

## 📂 Estrutura do Projeto

```

app/src/main/java/com/goldensystem/auris/
├── data/
│   ├── database/       # Room (entidades, DAOs, migrações)
│   ├── model/          # Modelos de domínio (Song, Album, Artist…)
│   ├── network/        # APIs (LRCLIB, Deezer)
│   ├── preferences/    # DataStore
│   ├── repository/     # Repositórios de dados
│   ├── service/        # MusicService, servidor HTTP
│   └── worker/         # WorkManager
├── di/                 # Módulos Hilt
├── presentation/
│   ├── components/     # Componentes Compose reutilizáveis
│   ├── navigation/     # Grafo de navegação
│   ├── screens/        # Telas
│   └── viewmodel/      # ViewModels
├── ui/
│   ├── glancewidget/   # Widgets
│   └── theme/          # Tema, cores, tipografia
└── utils/              # Extensões e utilitários

```

---

## 🛠️ Como compilar (desenvolvedores)

1. Clone o repositório:
   ```bash
   git clone https://github.com/pereirasaymonsilva-a11y/pixelplayer-apk.git
```

1. Abra no Android Studio (Ladybug ou mais recente).
2. Sincronize o Gradle e compile (Build → Make Project).
3. Execute em um dispositivo ou emulador.

---

🤝 Contribuindo

Contribuições são muito bem-vindas!
Para sugerir melhorias, abra uma issue ou envie um pull request.

1. Faça um fork do projeto
2. Crie um branch (git checkout -b feature/MinhaFeature)
3. Commit suas mudanças (git commit -m 'Adiciona nova feature')
4. Push para o branch (git push origin feature/MinhaFeature)
5. Abra um Pull Request

---

📄 Licença

Este projeto está licenciado sob a Licença MIT.
Consulte o arquivo LICENSE para mais informações.

Auris é uma modificação do PixelPlayer, criado por theovilardo.
Modificações por Saymon Silva Pereira / Golden System Studios (2026).

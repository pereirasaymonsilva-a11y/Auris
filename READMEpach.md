# Auris Connect pack

## O que entra aqui
Esta pasta contém a camada Android do Auris Connect.

## O que remover
- `LocalAudioServer.kt`
- `RokuHttpServer.kt`
- `RokuControlService.kt`
- qualquer arquivo legado do pacote `data.service.roku`

## O que criar
- `cast/protocol/Messages.kt`
- `cast/session/CastSession.kt`
- `cast/session/SessionManager.kt`
- `cast/metadata/MetadataProvider.kt`
- `cast/server/AurisHttpServer.kt`
- `cast/server/WebSocketModule.kt`
- `cast/roku/RokuDevice.kt`
- `cast/roku/RokuDiscovery.kt`
- `cast/roku/NetworkUtils.kt`
- `cast/roku/RokuController.kt`
- `cast/roku/RokuCastManager.kt`

## O que editar
- `app/build.gradle.kts`
- `CastBottomSheet.kt`
- `PlayerViewModel.kt`
- `AndroidManifest.xml`

## Dependências
Use estas dependências:

```kotlin
implementation("io.ktor:ktor-server-core:2.3.7")
implementation("io.ktor:ktor-server-netty:2.3.7")
implementation("io.ktor:ktor-server-websockets:2.3.7")
implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
implementation("io.ktor:ktor-server-partial-content:2.3.7")
implementation("io.ktor:ktor-server-compression:2.3.7")
```

## Observação
O lado Roku é um projeto separado em BrightScript/SceneGraph. A Roku documenta BrightScript e SceneGraph como base para apps de streaming na plataforma. O Ktor WebSockets e PartialContent são os plugins necessários para o servidor com comunicação em tempo real e HTTP Range requests. citeturn510412search0turn924052search0turn924052search2

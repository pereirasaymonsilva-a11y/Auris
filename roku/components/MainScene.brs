sub init()
    m.cover = m.top.findNode("cover")
    m.title = m.top.findNode("title")
    m.artist = m.top.findNode("artist")
    m.album = m.top.findNode("album")
    m.status = m.top.findNode("status")
    m.lyrics = m.top.findNode("lyrics")
    m.player = m.top.findNode("player")
    m.configTask = m.top.findNode("configTask")

    showIdleState()

    m.configTask.observeField("status", "onConfigStatusChanged")
    m.configTask.observeField("configJson", "onConfigJsonChanged")

    ' Troque este endereço pelo IP do celular/servidor Android durante o teste.
    ' O contrato mínimo esperado é um JSON com streamUrl, title, artist, album, cover, lyrics e mime.
    m.configTask.configUrl = "http://192.168.0.5:9876/roku/config"
    m.configTask.control = "RUN"
end sub

sub showIdleState()
    m.title.text = "Auris Receiver"
    m.artist.text = "Aguardando conexão..."
    m.album.text = ""
    m.status.text = "Pronto"
    m.lyrics.text = ""
    m.cover.uri = ""
end sub

sub onConfigStatusChanged()
    status = m.configTask.status

    if status = "ok" then
        m.status.text = "Conectado"
    else if status = "error" then
        m.status.text = "Erro ao carregar config"
    else if status = "missing_url" then
        m.status.text = "Config URL ausente"
    end if
end sub

sub onConfigJsonChanged()
    jsonText = m.configTask.configJson
    if jsonText = invalid or jsonText = "" then return

    data = ParseJson(jsonText)
    if data = invalid then
        m.status.text = "JSON inválido"
        return
    end if

    m.title.text = getStringField(data, "title", "Sem título")
    m.artist.text = getStringField(data, "artist", "Sem artista")
    m.album.text = getStringField(data, "album", "")
    m.status.text = "Tocando"
    m.lyrics.text = getStringField(data, "lyrics", "")

    coverUrl = getStringField(data, "cover", "")
    if coverUrl <> "" then
        m.cover.uri = coverUrl
    end if

    streamUrl = getStringField(data, "streamUrl", "")
    mimeType = getStringField(data, "mime", "audio/mpeg")

    if streamUrl <> "" then
        playStream(streamUrl, mimeType, data)
    else
        m.status.text = "Stream ausente"
    end if
end sub

sub playStream(streamUrl as String, mimeType as String, data as Object)
    content = CreateObject("roSGNode", "ContentNode")
    content.url = streamUrl
    content.streamformat = getStreamFormat(mimeType)
    content.title = getStringField(data, "title", "Auris")
    content.description = getStringField(data, "artist", "") + " - " + getStringField(data, "album", "")
    content.hdPosterUrl = getStringField(data, "cover", "")
    content.fhdPosterUrl = getStringField(data, "cover", "")

    m.player.content = content
    m.player.control = "play"
end sub

function getStreamFormat(mimeType as String) as String
    mime = LCase(mimeType)

    if InStr(1, mime, "audio/mpeg") > 0 then return "mp3"
    if InStr(1, mime, "audio/mp4") > 0 then return "mp4"
    if InStr(1, mime, "audio/aac") > 0 then return "mp4"
    if InStr(1, mime, "video/mp4") > 0 then return "mp4"
    if InStr(1, mime, "application/x-mpegurl") > 0 then return "hls"
    if InStr(1, mime, "application/vnd.apple.mpegurl") > 0 then return "hls"

    return "mp3"
end function

function getStringField(obj as Object, fieldName as String, fallback as String) as String
    if obj = invalid then return fallback
    if obj.DoesExist(fieldName) and obj[fieldName] <> invalid then
        return obj[fieldName].ToStr()
    end if
    return fallback
end function

function onKeyEvent(key as String, press as Boolean) as Boolean
    if press and key = "back" then
        m.player.control = "stop"
        return false
    end if

    return false
end function
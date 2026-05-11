' Auris Receiver para Roku
' Escuta comandos HTTP e toca áudio

sub Main()
    screen = CreateObject("roSGScreen")
    m.port = CreateObject("roMessagePort")
    screen.setMessagePort(m.port)
    scene = screen.CreateScene("MainScene")
    screen.show()

    ' Inicia o servidor HTTP na porta 8060 (padrão ECP), mas em um endpoint diferente
    server = CreateObject("roHttpServer")
    server.setPort(8061)  ' Porta alternativa para não conflitar com ECP
    server.setMessagePort(m.port)
    server.start()

    while true
        msg = wait(0, m.port)
        if type(msg) = "roUrlEvent"
            handleUrlEvent(msg)
        end if
    end while
end sub

sub handleUrlEvent(msg as Object)
    url = msg.GetUrl()
    if Instr(1, url, "/play") > 0
        ' Extrai a URL do stream do query parameter
        streamUrl = msg.GetRequestParams()?.Lookup("url")
        if streamUrl <> invalid and streamUrl <> ""
            ' Cria um player de áudio e toca
            player = CreateObject("roAudioPlayer")
            player.setMessagePort(m.port)
            player.AddContent(streamUrl)
            player.Play()
        end if
    else if Instr(1, url, "/stop") > 0
        ' Para a reprodução
        player = CreateObject("roAudioPlayer")
        player.Stop()
    end if
end sub
sub Main()
    screen = CreateObject("roSGScreen")
    m.port = CreateObject("roMessagePort")
    screen.setMessagePort(m.port)
    scene = screen.CreateScene("MainScene")
    screen.show()

    ' Escuta na porta 80 (HTTP padrão, sempre liberada)
    server = CreateObject("roHttpServer")
    server.setPort(80)
    server.setMessagePort(m.port)
    server.start()

    while true
        msg = wait(0, m.port)
        if type(msg) = "roUrlEvent"
            url = msg.GetUrl()
            if Instr(1, url, "/play") > 0
                params = msg.GetRequestParams()
                if params <> invalid
                    streamUrl = params.Lookup("url")
                    if streamUrl <> invalid and streamUrl <> ""
                        player = CreateObject("roAudioPlayer")
                        player.setMessagePort(m.port)
                        player.AddContent(streamUrl)
                        player.Play()
                    end if
                end if
            else if Instr(1, url, "/stop") > 0
                player = CreateObject("roAudioPlayer")
                player.Stop()
            end if
        end if
    end while
end sub
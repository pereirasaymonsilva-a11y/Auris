sub init()
    m.top.functionName = "fetchConfig"
end sub

sub fetchConfig()
    url = m.top.configUrl

    if url = invalid or url = "" then
        m.top.status = "missing_url"
        return
    end if

    xfer = CreateObject("roUrlTransfer")
    xfer.SetUrl(url)
    xfer.AddHeader("Accept", "application/json")

    rsp = xfer.GetToString()

    if rsp = invalid or rsp = "" then
        m.top.status = "error"
        return
    end if

    m.top.configJson = rsp
    m.top.status = "ok"
end sub
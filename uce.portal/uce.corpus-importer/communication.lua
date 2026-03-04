serial = luajava.bindClass("org.apache.uima.cas.impl.XmiCasSerializer")
deserial = luajava.bindClass("org.apache.uima.cas.impl.XmiCasDeserializer")
StandardCharsets = luajava.bindClass("java.nio.charset.StandardCharsets")

function serialize(inputCas, outputStream, params)
    local parameterText = " "

    if(params["common"] ~= nil and params["common"] ~= '') then
            parameterText = parameterText .. "-c " .. params["common"] .. " "
            print("Using common names from " .. params["common"])
    end

    if(params["dokumentConf"] ~= nil and params["dokumentConf"] ~= '') then
            parameterText = parameterText .. "-d " .. params["dokumentConf"] .. " "
            print("Using dokumentConf names from " .. params["dokumentConf"])
    end

    outputStream:write(json.encode({args = parameterText}))
    serial:serialize(inputCas:getCas(),outputStream)

end

function deserialize(inputCas, inputStream)
  inputCas:reset()
  deserial:deserialize(inputStream,inputCas:getCas(),true)
end
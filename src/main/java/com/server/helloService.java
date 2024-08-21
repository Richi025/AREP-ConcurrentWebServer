package com.server;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class helloService implements RESTService{

    @Override
        public String response(String request) {
        String[] requestParams = request.split("=");
        String decodedName = URLDecoder.decode(requestParams[1], StandardCharsets.UTF_8);
        
        // Formato JSON
        String jsonResponse = "{\"nombre\": \"" + decodedName + "\"}";
        
        // Texto plano
        String plainTextResponse = "Hola, " + decodedName;

        // Concatenar ambos formatos
        return jsonResponse + "<br />" + plainTextResponse;
    }
}

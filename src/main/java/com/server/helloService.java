package com.server;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class helloService implements RESTService{

    /**
     * Generates a response based on the provided request string.
     * The response includes a JSON representation and a plain text greeting.
     *
     * @param request The request string containing parameters, typically in the format "name=Value".
     * @return A combined response string consisting of a JSON object and a plain text greeting.
     */
    @Override
        public String response(String request) {
        String[] requestParams = request.split("=");
        String decodedName = URLDecoder.decode(requestParams[1], StandardCharsets.UTF_8);
        String jsonResponse = "{\"nombre\": \"" + decodedName + "\"}";
        String plainTextResponse = "Hola, " + decodedName;

        return jsonResponse + "<br />" + plainTextResponse;
    }
}

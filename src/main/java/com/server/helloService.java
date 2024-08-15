package com.server;

public class helloService implements RESTService{

    @Override
    public String response(String request) {
        return "{ \"nombre\": \"Ricardo\" }";
    }
    
}

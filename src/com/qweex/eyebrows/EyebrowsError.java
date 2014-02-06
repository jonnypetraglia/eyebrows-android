package com.qweex.eyebrows;

import java.util.HashMap;
import java.util.Map;

//TODO: Localize this shit

public class EyebrowsError extends Exception {
    private static final Map<Integer,String> msgs;
    static
    {
        msgs = new HashMap<Integer, String>();
        msgs.put(-401, "Password is incorrect");
        msgs.put(401, "Credentials are required");
        msgs.put(404, "404");
    }

    public EyebrowsError(int errorCode, MainActivity a) {
        super(errorCode == 401 && a.requiresCredentials() ? msgs.get(-401) : msgs.get(errorCode));
    }

}


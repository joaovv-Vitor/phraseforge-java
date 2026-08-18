package com.phraseforge.phraseforge_api.auth;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid refresh session");
    }
}

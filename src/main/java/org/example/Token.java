package org.example;

public class Token {
    private final TokenType tokenType; //constante que representa o token
    private String value;
    private final String lexeme;
    public Token (TokenType tokenType, String lexeme, String value) {
        this.tokenType = tokenType;
        this.lexeme = lexeme;
        this.value = value;
    }
    public String toString(){
        return "";
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLexeme() {
        return lexeme;
    }
}
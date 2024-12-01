package org.example;

public class Token {
    private final TokenType tokenType; //constante que representa o token
    private final String lexeme;
    private String value;

    public Token(TokenType tokenType, String lexeme, String value) {
        if (lexeme.equals(" ")) {
            System.out.println("vazio");
        }
        this.tokenType = tokenType;
        this.lexeme = lexeme;
        this.value = value;
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

    @Override
    public String toString() {
        return this.tokenType +
                " | " + this.lexeme
                + ((this.tokenType == TokenType.UNEXPECTED || this.tokenType == TokenType.ERROR) ? " | " + this.value : "");
    }
}
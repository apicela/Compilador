package org.example;

public class Token {
    private final TokenType type; //constante que representa o token
    private final String lexeme;
    private String value;

    public Token(TokenType type, String lexeme, String value) {
        if (lexeme.equals(" ")) {
            System.out.println("vazio");
        }
        this.type = type;
        this.lexeme = lexeme;
        this.value = value;
    }

    public TokenType getType() {
        return type;
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
        return this.type +
                " | " + this.lexeme
                + ((this.type == TokenType.UNEXPECTED || this.type == TokenType.ERROR) ? " | " + this.value : "");
    }
}
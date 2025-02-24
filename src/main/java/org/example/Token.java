package org.example;

public class Token {
    private final TokenType type; //constante que representa o token
    private String lexeme;
    private  Integer line;

    public Token(TokenType type, String lexeme, Integer value) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = value;
    }

    public void setLexeme(String lexeme) {
        this.lexeme = lexeme;
    }


    public TokenType getType() {
        return type;
    }

    public int getLine() {
        return line;
    }

    public Token setLine(int line) {
        this.line = line;
        return this;
    }

    public String getLexeme() {
        return lexeme;
    }

//    @Override
//    public String toString() {
//        return this.type +
//                " | " + this.lexeme
//                + ((this.type == TokenType.UNEXPECTED || this.type == TokenType.ERROR) ? " | " + this.line : "");
//    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", lexeme='" + lexeme + '\'' +
                ", line=" + line +
                '}';
    }
}
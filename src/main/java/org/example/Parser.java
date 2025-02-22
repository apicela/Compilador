package org.example;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }

    private boolean match(TokenType... types) { // verifica o "case" e se for, ele come o token
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    public void parse() {
        program();
    }

    private void program() {
        match(TokenType.START);
        declList();
        stmtList();
        match(TokenType.EXIT);
    }

    private void declList() {
        while (check(TokenType.CONSTANT)) {
            decl();
        }
    }

    private void decl() {
        type();
        identList();
        match(TokenType.SEMICOLON);
    }

    private void type() {
        if (match(TokenType.CONSTANT)) {
            // Type matched
        } else {
            throw new RuntimeException("Expected type");
        }
    }

    private void identList() {
        identifier();
        while (match(TokenType.COMMA)) {
            identifier();
        }
    }

    private void identifier() {
        if (match(TokenType.IDENTIFIER)) {
            // Identifier matched
        } else {
            throw new RuntimeException("Expected identifier");
        }
    }

    private void stmtList() {
        while (!check(TokenType.EXIT)) {
            stmt();
        }
    }

    private void stmt() {
        if (check(TokenType.IDENTIFIER)) {
            assignStmt();
            match(TokenType.SEMICOLON);
        } else if (match(TokenType.IF)) {
            ifStmt();
        } else if (match(TokenType.DO)) {
            whileStmt();
        } else if (match(TokenType.SCAN)) {
            readStmt();
            match(TokenType.SEMICOLON);
        } else if (match(TokenType.PRINT)) {
            writeStmt();
            match(TokenType.SEMICOLON);
        } else {
            throw new RuntimeException("Expected statement");
        }
    }

    private void assignStmt() {
        identifier();
        match(TokenType.EQUALS);
        simpleExpr();
    }

    private void ifStmt() {
        condition();
        match(TokenType.THEN);
        stmtList();
        if (match(TokenType.ELSE)) {
            stmtList();
        }
        match(TokenType.END);
    }

    private void whileStmt() {
        stmtList();
        stmtSuffix();
    }

    private void stmtSuffix() {
        match(TokenType.WHILE);
        condition();
        match(TokenType.END);
    }

    private void readStmt() {
        match(TokenType.OPEN_ROUND);
        identifier();
        match(TokenType.CLOSE_ROUND);
    }

    private void writeStmt() {
        match(TokenType.OPEN_ROUND);
        writable();
        match(TokenType.CLOSE_ROUND);
    }

    private void writable() {
        if (check(TokenType.LITERAL)) {
            match(TokenType.LITERAL);
        } else {
            simpleExpr();
        }
    }

    private void condition() {
        expression();
    }

    private void expression() {
        simpleExpr();
        if (match(TokenType.EQUALS, TokenType.RELOP)) {
            simpleExpr();
        }
    }

    private void simpleExpr() {
        term();
        simpleExprPrime();
    }

    private void simpleExprPrime() {
        if (match(TokenType.ADDOP)) {
            term();
            simpleExprPrime();
        }
    }

    private void term() {
        factorA();
        termPrime();
    }

    private void termPrime() {
        if (match(TokenType.MULOP)) {
            factorA();
            termPrime();
        }
    }

    private void factorA() {
        if (match(TokenType.NOT, TokenType.ADDOP)) {
            factor();
        } else {
            factor();
        }
    }

    private void factor() {
        if (match(TokenType.IDENTIFIER, TokenType.CONSTANT)) {
            // Factor matched
        } else if (match(TokenType.OPEN_ROUND)) {
            expression();
            match(TokenType.CLOSE_ROUND);
        } else {
            throw new RuntimeException("Expected factor");
        }


    }

}
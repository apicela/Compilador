package org.example;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;
    private final List<String> parserErrors = new ArrayList<String>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token lookahead() {
        if (current + 1 < tokens.size()) {
            return tokens.get(current + 1);
        }
        System.out.println("Nenhum proximo token aqui, provavel erro");
        return null; // Ou um token especial representando EOF
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

    public void start() {
        program();
    }

    private void program() {
        if(!match(TokenType.START)){
            throw new RuntimeException("Erro de sintaxe: esperado 'START', mas encontrado " + peek().getType());
        }
        declList();
        stmtList();
        if(!match(TokenType.EXIT)){
            throw new RuntimeException("Erro de sintaxe: esperado 'EXIT', mas encontrado " + peek().getType());
        }
    }

    private void declList() {
        if(decl()){
            while(decl()){ }
        }
    }

    private boolean decl() {
        if(!type()){
            return false; //ou seja nao conseguiu formar um decl
        }

        identList();
        if(!match(TokenType.SEMICOLON)){
            throw new RuntimeException("Erro de sintaxe: esperado 'SEMICOLON', mas encontrado " + peek().getType());
        }
        return true;
    }

    private boolean type() {
        return match(TokenType.TYPE);
    }

    private void identList() {
        if(!identifier()){
            throw new RuntimeException("Erro de sintaxe: esperado 'IDENTIFIER', mas encontrado " + peek().getType());
        }

        while (match(TokenType.COMMA)) {
            if(!identifier()) {
                throw new RuntimeException("Erro de sintaxe: esperado 'IDENTIFIER', mas encontrado " + peek().getType());
            }
        }
    }

    private boolean identifier() {
        return match(TokenType.IDENTIFIER);
    }


    private void stmtList() {
        int obrigatorio = 1;
        stmt(obrigatorio);
        obrigatorio=0;
        while(stmt(obrigatorio)){ }
        //obrigatorio=1;
    }

    private boolean stmt(int obrigatorio) {
        if (check(TokenType.IDENTIFIER)) {
            assignStmt();
            if(!match(TokenType.SEMICOLON)){
                throw new RuntimeException("Erro de sintaxe: esperado 'SEMICOLON', mas encontrado " + peek().getType());
            }
        } else if (match(TokenType.IF)) {
            ifStmt();
        } else if (match(TokenType.DO)) {
            whileStmt();
        } else if (match(TokenType.SCAN)) {
            readStmt();
            if(!match(TokenType.SEMICOLON)){
                throw new RuntimeException("Erro de sintaxe: esperado 'SEMICOLON', mas encontrado " + peek().getType());
            }
        } else if (match(TokenType.PRINT)) {
            writeStmt();
            if(!match(TokenType.SEMICOLON)){
                throw new RuntimeException("Erro de sintaxe: esperado 'SEMICOLON', mas encontrado " + peek().getType());
            }
        } else {
            if(obrigatorio==1){
                throw new RuntimeException("Erro de sintaxe: esperado 'IDENTIFIER' ou 'IF' ou 'DO' ou 'SCAN' ou 'PRINT', mas encontrado " + peek().getType());
            }else{
                return false;
            }

        }
        return true;
    }

    private void assignStmt() {
        identifier();
        if(!match(TokenType.EQUALS)){
            throw new RuntimeException("Erro de sintaxe: esperado 'EQUALS', mas encontrado " + peek().getType());
        }

        simpleExpr();
    }

    private void simpleExpr() {
        term();
        simpleExprPrime();
    }

    private void ifStmt() {
        condition();
        if(!match(TokenType.THEN)){
            throw new RuntimeException("Erro de sintaxe: esperado 'THEN', mas encontrado " + peek().getType());
        }
        stmtList();
        if (match(TokenType.ELSE)) {
            stmtList();
        }
        if(!match(TokenType.END)){
            throw new RuntimeException("Erro de sintaxe: esperado 'END', mas encontrado " + peek().getType());
        }
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
        if(!match(TokenType.OPEN_ROUND)){
            throw new RuntimeException("Erro de sintaxe: esperado 'OPEN_ROUND', mas encontrado " + peek().getType());
        }
        if(!identifier()){
            throw new RuntimeException("Erro de sintaxe: esperado 'identifier', mas encontrado " + peek().getType());
        }
        if(!match(TokenType.CLOSE_ROUND)){
            throw new RuntimeException("Erro de sintaxe: esperado 'CLOSE_ROUND', mas encontrado " + peek().getType());
        }
    }

    private void writeStmt() {
        if(!match(TokenType.OPEN_ROUND)){
            throw new RuntimeException("Erro de sintaxe: esperado 'OPEN_ROUND', mas encontrado " + peek().getType());
        }
        writable();
        if(!match(TokenType.CLOSE_ROUND)){
            throw new RuntimeException("Erro de sintaxe: esperado 'CLOSE_ROUND', mas encontrado " + peek().getType());
        }
    }

    private void writable() {
        //se der errado aqui vai dar errado falando que esperava  identifier| CONSTANT | OPEN_ROUND, mas na verdade deveria falar que espera muito mais coisa
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
        if (match(TokenType.IDENTIFIER, TokenType.CONSTANT_INTEGER, TokenType.CONSTANT_FLOAT, TokenType.LITERAL)) {

        } else if (match(TokenType.OPEN_ROUND)) {
            expression();
            if(!match(TokenType.CLOSE_ROUND)){
                throw new RuntimeException("Erro de sintaxe: esperado 'CLOSE_ROUND', mas encontrado " + peek().getType());
            }
        } else {
            throw new RuntimeException("Erro de sintaxe: esperado 'IDENTIFIER' ou 'CONSTANT_INTEGER' ou 'LITERAL' ou 'OPEN_ROUND', mas encontrado " + peek().getType() + " na linha " + peek().getLine());
        }

    }

}
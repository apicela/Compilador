package org.example;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParserSemantic {
    private final List<Token> tokens;
    private int current = 0;
    private final List<String> semanticParserErrors = new ArrayList<String>();
    private final FileWriter writer;
    private final Map<String, FinalToken> symbolsTable = new HashMap<>();
    private String currentDeclarationType;
    private String currentIdentifier;
    private String currentOperation;
    private MathOperation mathOperation;
    private String factorAtual;
    private String currentTypeOfExpression;
    public ParserSemantic(List<Token> tokens, String fileName) throws IOException {
        this.tokens = tokens;
        this.writer = new FileWriter(fileName, true);
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
        currentDeclarationType = previous().getLexeme();
        System.out.println(currentDeclarationType);
        identList();
        if(!match(TokenType.SEMICOLON)){
            writeAndFlush(";");
            throw new RuntimeException("Erro de sintaxe: esperado 'SEMICOLON', mas encontrado " + peek().getType());
        }
        return true;
    }

    private boolean type() {
        return match(TokenType.TYPE);
    }

    private void identList() {
        boolean isIdentifier = identifier();
        if(!isIdentifier){
            throw new RuntimeException("Erro de sintaxe: esperado 'IDENTIFIER', mas encontrado " + peek().getType());
        }
        putAndVerifyDuplicated();
        while (match(TokenType.COMMA)) {
            if(!identifier()) {
                throw new RuntimeException("Erro de sintaxe: esperado 'IDENTIFIER', mas encontrado " + peek().getType());
            }
            putAndVerifyDuplicated();
        }
        currentDeclarationType = null;
    }

    private void putAndVerifyDuplicated(){
        Token readedToken = previous();
        if(symbolsTable.get(readedToken.getLexeme()) == null){ // se nao existe na tabela, adiciona
            symbolsTable.put(readedToken.getLexeme(), new FinalToken(currentDeclarationType, readedToken.getLexeme()));
        } else semanticParserErrors.add("ERRO: Está ferindo as regras de unicidade de nossa linguagem. Linha: " + readedToken.getLine());
    }

    private boolean identifier() {
        return  match(TokenType.IDENTIFIER);
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
                writeAndFlush(";");
                throw new RuntimeException("Erro de sintaxe: esperado 'SEMICOLON', mas encontrado " + peek().getType());
            }
        } else if (match(TokenType.IF)) {
            ifStmt();
        } else if (match(TokenType.DO)) {
            whileStmt();
        } else if (match(TokenType.SCAN)) {
            readStmt();
            if(!match(TokenType.SEMICOLON)){
                writeAndFlush(";");
                throw new RuntimeException("Erro de sintaxe: esperado 'SEMICOLON', mas encontrado " + peek().getType());
            }
        } else if (match(TokenType.PRINT)) {
            writeStmt();
            if(!match(TokenType.SEMICOLON)){
                writeAndFlush(";");
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
        currentOperation = "assign";
        identifier();
        currentIdentifier = previous().getLexeme();
        currentTypeOfExpression = symbolsTable.get(currentIdentifier) != null ? symbolsTable.get(currentIdentifier).getType() : "";
        if(!match(TokenType.EQUALS)){
            throw new RuntimeException("Erro de sintaxe: esperado 'EQUALS', mas encontrado " + peek().getType());
        }
        simpleExpr();
        currentIdentifier = null;
        currentOperation = null;
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
            writeAndFlush(")");
            throw new RuntimeException("Erro de sintaxe: esperado 'CLOSE_ROUND', mas encontrado " + peek().getType());
        }
    }

    private void writeStmt() {
        if(!match(TokenType.OPEN_ROUND)){
            throw new RuntimeException("Erro de sintaxe: esperado 'OPEN_ROUND', mas encontrado " + peek().getType()+ " na linha " + " peek().getLine ");
        }
        writable();
        if(!match(TokenType.CLOSE_ROUND)){
            writeAndFlush(")");
            semanticParserErrors.add("Erro de sintaxe: esperado 'CLOSE_ROUND', mas encontrado " + peek().getType());
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

    private void simpleExpr() {
        term(); // preenche factor atual
        simpleExprPrime();
    }

    private void simpleExprPrime() {
        if (match(TokenType.ADDOP)) {
            mathOperation = new MathOperation(previous().getLexeme(), factorAtual);
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
            mathOperation = new MathOperation(previous().getLexeme(), factorAtual);
            factorA();
            termPrime();
            mathOperation = null;
        }
    }

    private void factorA() {
        if (match(TokenType.NOT, TokenType.ADDOP)) {
            factor();
        } else {
            factor();
        }
    }

    private void factor()  {
        if (match(TokenType.IDENTIFIER, TokenType.CONSTANT_INTEGER, TokenType.CONSTANT_FLOAT, TokenType.LITERAL)) {
            Token readedToken = previous(); // pega o token
            if(readedToken.getType() == TokenType.IDENTIFIER)
                factorAtual = String.valueOf(symbolsTable.getOrDefault(readedToken.getLexeme(), null)); // se for variavel pega o valor dela
             else factorAtual = readedToken.getLexeme();
            if(mathOperation != null){
                doMathOperation(mathOperation);
            }
            if(currentOperation.equals("assign")) verifyAssignment(readedToken);
        } else if (match(TokenType.OPEN_ROUND)) {
            expression();
            if(!match(TokenType.CLOSE_ROUND)){
                writeAndFlush(")");
                throw new RuntimeException("Erro de sintaxe: esperado 'CLOSE_ROUND', mas encontrado " + peek().getType());
            }
        } else {
            throw new RuntimeException("Erro de sintaxe: esperado 'IDENTIFIER' ou 'CONSTANT_INTEGER' ou 'LITERAL' ou 'OPEN_ROUND', mas encontrado " + peek().getType() + " na linha " + peek().getLine());
        }

    }

    void doMathOperation(MathOperation mathOperation){
        if(mathOperation.operation.equals("+")){
            if(currentTypeOfExpression.equals("float")) mathOperation.value = String.valueOf(Float.valueOf(mathOperation.getValue()) + Float.valueOf(factorAtual));
            else if(currentTypeOfExpression.equals("int")) mathOperation.value = String.valueOf(Integer.valueOf(mathOperation.getValue()) + Integer.valueOf(factorAtual));
        }
    }

    private void verifyAssignment(Token readedToken) {
        FinalToken currentFinalToken = symbolsTable.get(currentIdentifier);
        if(currentFinalToken == null){
            semanticParserErrors.add("ERRO: Está utilizando uma variável inexistente. Linha: " + readedToken.getLine());
            return;
        }
        if(readedToken.getType() == TokenType.CONSTANT_FLOAT && !currentFinalToken.getType().equals("float"))
            semanticParserErrors.add(STR."ERRO: Não é possivel associar \{readedToken.getType()} para variável do tipo \{currentFinalToken.getType()}. Linha: \{readedToken.getLine()}");
        else  if(readedToken.getType() == TokenType.CONSTANT_INTEGER && !currentFinalToken.getType().equals("int"))
            semanticParserErrors.add(STR."ERRO: Não é possivel associar \{readedToken.getType()} para variável do tipo \{currentFinalToken.getType()}. Linha: \{readedToken.getLine()}");
        else  if(readedToken.getType() == TokenType.LITERAL && !currentFinalToken.getType().equals("string"))
            semanticParserErrors.add(STR."ERRO: Não é possivel associar \{readedToken.getType()} para variável do tipo \{currentFinalToken.getType()}. Linha: \{readedToken.getLine()}");

        if(mathOperation != null){
            currentFinalToken.setValue(mathOperation.getValue());
        } else {
            currentFinalToken.setValue(readedToken.getLexeme());
        }
        symbolsTable.put(currentIdentifier, currentFinalToken);
    }

    void writeAndFlush(String str)  {
        try{
            writer.write(str);
            writer.flush();
        } catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }

    void printTable(){
        for (Map.Entry<String, FinalToken> entry : symbolsTable.entrySet()) {
            String key = entry.getKey();
            FinalToken value = entry.getValue();
            System.out.println("Chave: " + key + " Valor: " + value.toString());
        }
    }

    void printErrors(){
        System.out.println("ERROS SEMANTICOS: ");
        for(String s : semanticParserErrors){
            System.out.println(s);
        }
    }

    private class MathOperation {
        private String operation;
        private String value;

        public MathOperation(String operation, String value) {
            this.operation = operation;
            this.value = value;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
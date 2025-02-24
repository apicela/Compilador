package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

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
    private Stack<MathOperation> mathStack = new Stack<>();
    private List<String> semanticErrors = new ArrayList<>();
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
            throw new RuntimeException("Erro de sintaxe: esperado 'START', mas encontrado " + peek().getType() + " na linha " +  peek().getLine());
        }
        declList();
        stmtList();
        if(!match(TokenType.EXIT)){
            throw new RuntimeException("Erro de sintaxe: esperado 'EXIT', mas encontrado " + peek().getType() + " na linha " +  peek().getLine() );
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
        identList();
        if(!match(TokenType.SEMICOLON)){
            writeAndFlush(";");
            semanticErrors.add("Erro de sintaxe: esperado 'SEMICOLON', mas encontrado " + peek().getType()+ " na linha " +  peek().getLine() );
        }
        return true;
    }

    private boolean type() {
        return match(TokenType.TYPE);
    }

    private void identList() {
        boolean isIdentifier = identifier();
        if(!isIdentifier){
            throw new RuntimeException("Erro de sintaxe: esperado 'IDENTIFIER', mas encontrado " + peek().getType() + " na linha " + peek().getLine() + " ERRO NÃO SERÁ CORRIGIDO E ANÁLISE FINALIZOU");
        }
        putAndVerifyDuplicated();
        while (match(TokenType.COMMA)) {
            if(!identifier()) {
                throw new RuntimeException("Erro de sintaxe: esperado 'IDENTIFIER', mas encontrado " + peek().getType() + " na linha " + peek().getLine() + "  ERRO NÃO SERÁ CORRIGIDO E ANÁLISE FINALIZOU");
            }
            putAndVerifyDuplicated();
        }
        currentDeclarationType = null;
    }

    private void putAndVerifyDuplicated(){
        Token readedToken = previous();
        if(symbolsTable.get(readedToken.getLexeme()) == null){ // se nao existe na tabela, adiciona
            symbolsTable.put(readedToken.getLexeme(), new FinalToken(currentDeclarationType, readedToken.getLexeme()));
        } else{
            semanticParserErrors.add("ERRO Semântico: Está ferindo as regras de unicidade de nossa linguagem. Linha: " + readedToken.getLine());
        }
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
                throw new RuntimeException("Erro de sintaxe: esperado 'SEMICOLON', mas encontrado " + peek().getType()+ " na linha " +  peek().getLine() );
            }
        } else if (match(TokenType.IF)) {
            ifStmt();
        } else if (match(TokenType.DO)) {
            whileStmt();
        } else if (match(TokenType.SCAN)) {
            readStmt();
            if(!match(TokenType.SEMICOLON)){
                writeAndFlush(";");
                throw new RuntimeException("Erro de sintaxe: esperado 'SEMICOLON', mas encontrado " + peek().getType()+ " na linha " +  peek().getLine() );
            }
        } else if (match(TokenType.PRINT)) {
            writeStmt();
            if(!match(TokenType.SEMICOLON)){
                writeAndFlush(";");
                throw new RuntimeException("Erro de sintaxe: esperado 'SEMICOLON', mas encontrado " + peek().getType()+ " na linha " +  peek().getLine() );
            }
        } else {
            if(obrigatorio==1){
                throw new RuntimeException("Erro de sintaxe: esperado 'IDENTIFIER' ou 'IF' ou 'DO' ou 'SCAN' ou 'PRINT', mas encontrado " + peek().getType()+ " na linha " + peek().getLine() + "  ERRO NÃO SERÁ CORRIGIDO E ANÁLISE FINALIZOU");
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
            throw new RuntimeException("Erro de sintaxe: esperado 'EQUALS', mas encontrado " + peek().getType()+ " na linha " + peek().getLine() + "  ERRO NÃO SERÁ CORRIGIDO E ANÁLISE FINALIZOU");
        }
        simpleExpr();
        MathOperation mathOpTemp = null;
        while (!mathStack.isEmpty() ) {
            MathOperation currMathStack = mathStack.pop();
            if(currMathStack.value2 == null){
                currMathStack.value2 = mathOpTemp.result;
            }
            currMathStack.calculeResult();
            mathOpTemp = currMathStack;
        }
        if(mathOpTemp != null) assignVariable(currentIdentifier, mathOpTemp.result, mathOpTemp.opLine);
        currentIdentifier = null;
        currentOperation = null;
    }

    private void assignVariable(String currentIdentifier, String result, int line) {
        FinalToken currentFinalToken = symbolsTable.get(currentIdentifier);
        if(currentFinalToken == null){
            semanticParserErrors.add("ERRO Semantico: não é possivel atribuir valor a uma tarefa não declarada anteriormente. linha: " + line );
            return;
        }
        if (!currentFinalToken.getType().equals("string")){
            try{
                if(result != null) Math.ceil(Double.parseDouble(result));
            } catch(NumberFormatException e){
                semanticParserErrors.add("ERRO Semantico: não é possivel associar STRING em variavel do tipo " + currentFinalToken.getType());
            }
        }

        if(currentFinalToken.getType().equals("int")){
            if(result != null) currentFinalToken.setValue(String.valueOf(Math.ceil(Double.parseDouble(result))));
        }
        currentFinalToken.setValue(result);
        if((currentFinalToken.getType().equals("float") || currentFinalToken.getType().equals("int")) && currentFinalToken.getValue() != null && currentFinalToken.getValue().equals("null")){
            semanticParserErrors.add("ERRO Semantico: está buscando atribuir null em variável do tipo " + currentFinalToken.getType()+" linha: " + line);
        }
        symbolsTable.put(currentIdentifier, currentFinalToken);
    }

    public void printSintaticErrors(){
        for(String s : semanticErrors){
            System.out.println(s);
        }
    }

    private void ifStmt() {
        condition();
        if(!match(TokenType.THEN)){
            throw new RuntimeException("Erro de sintaxe: esperado 'THEN', mas encontrado " + peek().getType()+ " na linha " + peek().getLine() + "  Erro ignorado e iremos continuar análise.");
        }
        stmtList();
        if (match(TokenType.ELSE)) {
            stmtList();
        }
        if(!match(TokenType.END)){
            throw new RuntimeException("Erro de sintaxe: esperado 'END', mas encontrado " + peek().getType()+ " na linha " + peek().getLine() + "  Erro ignorado e iremos continuar análise.");
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
            throw new RuntimeException("Erro de sintaxe: esperado 'OPEN_ROUND', mas encontrado " + peek().getType()+ " na linha " + peek().getLine() + " .ERRO NÃO SERÁ CORRIGIDO E ANÁLISE FINALIZOU");
        }
        if(!identifier()){
            throw new RuntimeException("Erro de sintaxe: esperado 'identifier', mas encontrado " + peek().getType()+ " na linha " + peek().getLine() + ". ERRO NÃO SERÁ CORRIGIDO E ANÁLISE FINALIZOU");
        }
        if(!match(TokenType.CLOSE_ROUND)){
            writeAndFlush(")");
            throw new RuntimeException("Erro de sintaxe: esperado 'CLOSE_ROUND', mas encontrado " + peek().getType()+ " na linha " +  peek().getLine() );
        }
    }

    private void writeStmt() {
        if(!match(TokenType.OPEN_ROUND)){
            throw new RuntimeException("Erro de sintaxe: esperado 'OPEN_ROUND', mas encontrado " + peek().getType()+ " na linha " + peek().getLine() + " .ERRO NÃO SERÁ CORRIGIDO E ANÁLISE FINALIZOU");
        }
        writable();
        if(!match(TokenType.CLOSE_ROUND)){
            writeAndFlush(")");
            throw new RuntimeException("Erro de sintaxe: esperado 'CLOSE_ROUND', mas encontrado " + peek().getType()+ " na linha " +  peek().getLine() );
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
        currentOperation = "expression";
        currentTypeOfExpression = "boolean";
        expression();
        MathOperation mathOpTemp = null;
        while (!mathStack.isEmpty() ) {
            MathOperation currMathStack = mathStack.pop();
            if(currMathStack.value2 == null){
                currMathStack.value2 = mathOpTemp.result;
            }
            currMathStack.calculeResult();
            mathOpTemp = currMathStack;
        }
        currentIdentifier = null;
        currentOperation = null;

    }

    private void expression() {
        simpleExpr();
        if (match(TokenType.EQUALS, TokenType.RELOP)) {
            processMathOperation();
            simpleExpr();
        }
    }

    private void simpleExpr() {
        term(); // preenche factor atual
        simpleExprPrime();
        mathOperation = null;
    }

    private void simpleExprPrime() {
        if (match(TokenType.ADDOP)) {
            processMathOperation();
            term();
            simpleExprPrime();
        }
    }

    private void processMathOperation() {
        if(!mathStack.isEmpty()) {
            MathOperation stackPeek = mathStack.pop();
            mathOperation = new MathOperation();
            stackPeek.value2 = mathOperation.result; //result do novo mathOp
            mathStack.add(stackPeek);
            mathOperation.operation = previous().getLexeme();
            mathOperation.value1 = factorAtual;
            mathOperation.opLine = previous().getLine();
        } else mathOperation = new MathOperation(previous().getLexeme(), factorAtual, null, peek().getLine());
    }

    private void term() {
        factorA();
        termPrime();
    }

    private void termPrime() {
        if (match(TokenType.MULOP)) {
            processMathOperation();
            factorA();
            termPrime();
        }
    }

    private void factorA() {
        if (match(TokenType.NOT, TokenType.ADDOP)) {
            if(previous().getLexeme().equals("+")){
                semanticParserErrors.add("ERRO Semântico: O operador '+' não é aplicavel em " + previous().getType()+ " na linha " +  previous().getLine() );
            }else if(previous().getLexeme().equals("-")){
                Token t = peek();
                if(t.getType()==TokenType.CONSTANT_INTEGER || t.getType()==TokenType.CONSTANT_FLOAT){
                    t.setLexeme("-" + t.getLexeme());

                }else if(t.getType()==TokenType.LITERAL){
                    semanticParserErrors.add("ERRO Semântico: O operador '-' não é aplicavel em " + peek().getType()+ " na linha " +  peek().getLine() ); //nao se pode fazer -{aaaa}


                }else if (t.getType()==TokenType.IDENTIFIER) {
                    FinalToken t1 = symbolsTable.get(t.getLexeme());
                    if(t1 == null) {
                        semanticParserErrors.add("ERRO Semântico: O identificador " + peek().getLexeme() + " na linha " + peek().getLine() + " nao existe"); //nao se pode fazer -a se a nao existir
                    }else{ //se o literal realmente existe precisamos inverter o valor
                        t1.setValue("-" + (t1.getValue()));
                        symbolsTable.put(t.getLexeme(), t1);

                    }
                }

            }else if (previous().getLexeme().equals("!")) {
                //todo

            }
            factor();
        } else {
            factor();
        }
    }

    private void factor()  {
        if (match(TokenType.IDENTIFIER, TokenType.CONSTANT_INTEGER, TokenType.CONSTANT_FLOAT, TokenType.LITERAL)) {
            Token readedToken = previous(); // pega o token
            if(readedToken.getType() == TokenType.IDENTIFIER){
                FinalToken t = symbolsTable.get(readedToken.getLexeme());
                if(t != null) factorAtual =  String.valueOf(t.getValue());
                else semanticParserErrors.add("ERRO Semântico: A variável " + readedToken.getLexeme() + " não foi declarada. linha: "  +readedToken.getLine() );
            }
             else factorAtual = readedToken.getLexeme();
            if(mathOperation != null){
                doMathOperation(mathOperation);
            } else {
                var newMathOp = new MathOperation("+", factorAtual, "0",readedToken.getLine());
                newMathOp.expressionType = currentTypeOfExpression;
                mathStack.add(newMathOp);
            }
            if(currentOperation.equals("assign")) verifyAssignment(readedToken);
        } else if (match(TokenType.OPEN_ROUND)) {
            expression();
            if(!match(TokenType.CLOSE_ROUND)){
                writeAndFlush(")");
                throw new RuntimeException("Erro de sintaxe: esperado 'CLOSE_ROUND', mas encontrado " + peek().getType()+ " na linha " +  peek().getLine() );
            }
        } else {
            throw new RuntimeException("Erro de sintaxe: esperado 'IDENTIFIER' ou 'CONSTANT_INTEGER' ou 'LITERAL' ou 'OPEN_ROUND', mas encontrado " + peek().getType() + " na linha " + peek().getLine());
        }

    }

    private void verifyCondition(Token readedToken) {
        FinalToken currentFinalToken = symbolsTable.get(currentIdentifier);
        if(currentFinalToken == null){
            semanticParserErrors.add("ERRO Semantico: Está utilizando uma variável inexistente. Linha: " + readedToken.getLine());
            return;
        }
    }

    void doMathOperation(MathOperation mathOperation){
        mathOperation.expressionType = currentTypeOfExpression;
        mathOperation.value2 = factorAtual;
        var mathOp = new MathOperation(); // nao é temporario
        mathOp.operation = mathOperation.operation;
        mathOp.value1 = mathOperation.value1;
        mathOp.value2 = mathOperation.value2;
        mathOp.expressionType = mathOperation.expressionType;
        mathOp.opLine = mathOperation.opLine;
        mathStack.add(mathOp);
    }
    private void verifyAssignment(Token readedToken) {
        FinalToken currentFinalToken = symbolsTable.get(currentIdentifier);
        if(currentFinalToken == null){
            semanticParserErrors.add("ERRO Semantico: Está utilizando uma variável inexistente. Linha: " + readedToken.getLine());
            return;
        }
        if (readedToken.getType() == TokenType.CONSTANT_FLOAT && !currentFinalToken.getType().equals("float"))
            semanticParserErrors.add(String.format("ERRO Semantico: Não é possivel associar %s para variável do tipo %s. Linha: %d",
                    readedToken.getType(), currentFinalToken.getType(), readedToken.getLine()));
        else if (readedToken.getType() == TokenType.CONSTANT_INTEGER && !currentFinalToken.getType().equals("int"))
            semanticParserErrors.add(String.format("ERRO Semantico: Não é possivel associar %s para variável do tipo %s. Linha: %d",
                    readedToken.getType(), currentFinalToken.getType(), readedToken.getLine()));
        else if (readedToken.getType() == TokenType.LITERAL && !currentFinalToken.getType().equals("string"))
            semanticParserErrors.add(String.format("ERRO Semantico: Não é possivel associar %s para variável do tipo %s. Linha: %d",
                    readedToken.getType(), currentFinalToken.getType(), readedToken.getLine()));

        if(mathOperation != null){
            currentFinalToken.setValue(mathOperation.value1);
        } else {
            if(readedToken.getType() == TokenType.IDENTIFIER) currentFinalToken.setValue(symbolsTable.get(readedToken.getLexeme()).getValue());
            else currentFinalToken.setValue(readedToken.getLexeme());
        }
        if(currentFinalToken.getType().equals("int")) currentFinalToken.setValue(String.valueOf(Math.ceil(Double.parseDouble(currentFinalToken.getValue()))));
        symbolsTable.put(currentIdentifier, currentFinalToken);
    }
    void writeAndFlush(String str)  {
        try{
//            writer.write(str);
//            writer.flush();
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

    void printErrorsSemantic(){
        for(String s : semanticParserErrors){
            System.out.println(s);
        }
        if(semanticParserErrors.isEmpty()){
            System.out.println("CÓDIGO FONTE VÁLIDO EM TERMOS SEMÂNTICOS.");
        }
    }

    private class MathOperation {
        private String operation;
        private String value1;
        private String value2;
        private String result;
        private String expressionType;
        private int opLine;
        public MathOperation(String operation, String value1,  String value2, int opLine) {
            this.operation = operation;
            this.value1 = value1;
            this.value2 = value2;
            this.opLine = opLine;
        }

        public MathOperation() {
        }



        private void calculeResult() {
            try{
            if(this.operation.equals("+")){
                if(!this.expressionType.equals("string")) this.result = String.valueOf(Float.valueOf(this.value1) + Float.valueOf(this.value2));
                else this.result = this.value1 + this.value2;
            } else if(this.operation.equals("*")){
                if(!currentTypeOfExpression.equals("string")) this.result = String.valueOf(Float.valueOf(this.value1) * Float.valueOf(this.value2));
                else this.result = this.value1 + this.value2;
            } else if(this.operation.equals("-")){
                if(!currentTypeOfExpression.equals("string")) this.result = String.valueOf(Float.valueOf(this.value1) - Float.valueOf(this.value2));
                else semanticParserErrors.add("ERRO Semantico: O operador '-' não é aplicavel em variáveis tipo string. Linha: " + this.opLine);
            } else if(this.operation.equals("/")){
                if(!currentTypeOfExpression.equals("string")) this.result = String.valueOf(Float.valueOf(this.value1) / Float.valueOf(this.value2));
                else semanticParserErrors.add("ERRO Semantico: O operador '/' não é aplicavel em variáveis tipo string. Linha: " + this.opLine);
            } else if(this.operation.equals(">")){
                if(!currentTypeOfExpression.equals("string")) this.result = String.valueOf(Float.valueOf(this.value1) > Float.valueOf(this.value2));
                else semanticParserErrors.add("ERRO Semantico: O operador '>' não é aplicavel em variáveis tipo string. Linha: " + this.opLine);
            }  else if(this.operation.equals(">=")){
                if(!currentTypeOfExpression.equals("string")) this.result = String.valueOf(Float.valueOf(this.value1) >= Float.valueOf(this.value2));
                else semanticParserErrors.add("ERRO Semantico: O operador '>=' não é aplicavel em variáveis tipo string. Linha: " + this.opLine);
            }  else if(this.operation.equals("<")){
                if(!currentTypeOfExpression.equals("string")) this.result = String.valueOf(Float.valueOf(this.value1) < Float.valueOf(this.value2));
                else semanticParserErrors.add("ERRO Semantico: O operador '<' não é aplicavel em variáveis tipo string. Linha: " + this.opLine);
            }  else if(this.operation.equals("<=")){
                if(!currentTypeOfExpression.equals("string")) this.result = String.valueOf(Float.valueOf(this.value1) <= Float.valueOf(this.value2));
                else semanticParserErrors.add("ERRO Semantico : O operador '<=' não é aplicavel em variáveis tipo string. Linha: " + this.opLine);
            }  else if(this.operation.equals("==")){
                if(!currentTypeOfExpression.equals("string")) this.result = String.valueOf(Float.valueOf(this.value1) == Float.valueOf(this.value2));
                else semanticParserErrors.add("ERRO Semantico: O operador '==' não é aplicavel em variáveis tipo string. Linha: " + this.opLine);
            }   else if(this.operation.equals("%")){
                try{
                    this.result = String.valueOf(Integer.parseInt(this.value1) % Integer.parseInt(this.value2));
                } catch(NumberFormatException e){
                    semanticParserErrors.add("ERRO Semantico: O operador %, requer que ambos operandos sejam inteiros. Linha: " + this.opLine);
                }
            }
            } catch(Exception e){
                this.result = null;
            }
        }

        @Override
        public String toString() {
            return "MathOperation{" +
                    "operation='" + operation + '\'' +
                    ", value1='" + value1 + '\'' +
                    ", value2='" + value2 + '\'' +
                    ", result='" + result + '\'' +
                    ", expressionType='" + expressionType + '\'' +
                    '}';
        }
    }
}
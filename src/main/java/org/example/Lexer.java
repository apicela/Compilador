package org.example;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;


public class Lexer {
    public static int line = 1; //contador de linhas
    private char ch = ' '; //caractere lido do arquivo
    private boolean finished = false;
    private FileReader file;
    private Hashtable<String, Token> symbolsTable = new Hashtable();
    private static final Pattern KEYWORDS = Pattern.compile("\\b(int|float|if|else|while|for|public|private)\\b");
    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    private static final Pattern INTEGER = Pattern.compile("\\d+");  // Para inteiros
    private static final Pattern FLOAT = Pattern.compile("\\d+\\.\\d+"); // Para números com ponto
    private static final Pattern STRING = Pattern.compile("\"[^\"]*\"");
    private List<String> errors = new ArrayList<>();
    // Criando uma lista para armazenar os tokens (deve ser definida em algum lugar no código)
    List<Token> list = new ArrayList<>();

    public void processTokens() throws IOException {
        while (!finished) {
            Token t = scan();
            list.add(t);
            switch (t.getTokenType()) {
                case TokenType.UNEXPECTED :
                    errors.add(t.toString());
                    break;
                case TokenType.IDENTIFIER :
                    Token identifier = symbolsTable.get(t.getLexeme());
                    if(identifier == null) symbolsTable.put(t.getLexeme(), t);
                    break;
            }
        }

    }

    /* Método para inserir palavras reservadas na HashTable */
    private void reserve(Token t){
        symbolsTable.put(t.getLexeme(), t); // lexema é a chave para entrada na
    }

    public Lexer(String fileName) throws FileNotFoundException{
        try{
            file = new FileReader (fileName);
        }
        catch(FileNotFoundException e){
            System.out.println("Arquivo não encontrado");
            throw e;
        }
        reserveLanguageTokens();
    }

    private void reserveLanguageTokens() {
        // PROGRAM
        reserve(new Token (TokenType.START, "start", null));
        reserve(new Token (TokenType.EXIT, "exit", null));
        // TYPE
        reserve(new Token (TokenType.TYPE, "int", null));
        reserve(new Token (TokenType.TYPE, "float", null));
        reserve(new Token (TokenType.TYPE, "string", null));
        // PALAVRAS RESERVADAS
        reserve(new Token(TokenType.KEYWORD, "if", null));
        reserve(new Token(TokenType.KEYWORD, "else", null));
        reserve(new Token(TokenType.KEYWORD, "then", null));
        reserve(new Token(TokenType.KEYWORD, "end", null));
        reserve(new Token(TokenType.KEYWORD, "do", null));
        reserve(new Token(TokenType.KEYWORD, "while", null));
        reserve(new Token(TokenType.KEYWORD, "scan", null));
        reserve(new Token(TokenType.KEYWORD, "print", null));
// Para os operadores relacionais (relop)
        reserve(new Token(TokenType.RELOP, "==", null));  // Operador igual a
        reserve(new Token(TokenType.RELOP, ">", null));   // Operador maior que
        reserve(new Token(TokenType.RELOP, ">=", null));  // Operador maior ou igual a
        reserve(new Token(TokenType.RELOP, "<", null));   // Operador menor que
        reserve(new Token(TokenType.RELOP, "<=", null));  // Operador menor ou igual a
        reserve(new Token(TokenType.RELOP, "!=", null));  // Operador diferente de

// Para os operadores aditivos (addop)
        reserve(new Token(TokenType.ADDOP, "+", null));   // Operador de soma
        reserve(new Token(TokenType.ADDOP, "-", null));   // Operador de subtração
        reserve(new Token(TokenType.ADDOP, "||", null));  // Operador lógico OR

// Para os operadores multiplicativos (mulop)
        reserve(new Token(TokenType.MULOP, "*", null));   // Operador de multiplicação
        reserve(new Token(TokenType.MULOP, "/", null));   // Operador de divisão
        reserve(new Token(TokenType.MULOP, "%", null));   // Operador módulo
        reserve(new Token(TokenType.MULOP, "&&", null));  // Operador lógico AND
        // EQUALS
        reserve(new Token(TokenType.EQUALS, "==", null));  // Operador lógico AND
    }

    private void readch() throws IOException{
        int nextChar = file.read();
//        System.out.println("ch: " + (char) (nextChar) + " ASCII:" + nextChar);
        if (nextChar == -1) {

            System.out.println("TABELA DE SIMBOLOS: ");
            for (Map.Entry<String, Token> entry : symbolsTable.entrySet()) {
                Token valor = entry.getValue();
                System.out.println(valor);
            }

            // Imprimindo todos os valores
            System.out.println("TOKENS: ");
            System.out.println(" TYPE  | LEXEME  |      VALUE");
            for(Token t : list){
                System.out.println(t.toString());
            }
            System.out.println("Tokens encontrados: " + list.size());

            for(String error : errors) System.out.println(error);
            file.close(); // Fecha o arquivo após a leitura completa
            System.exit(0);
        } else {
            ch = (char) nextChar;
        }
    }
    /* Lê o próximo caractere do arquivo e verifica se é igual a c*/
    private boolean readch(char c) throws IOException{
        readch();
        if (ch != c) return false;
        ch = ' ';
        return true;
    }
    public Token scan() throws IOException{
        //Desconsidera delimitadores na entrada
        for (;; readch()) {
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\b') continue;
            else if (ch == '\n') line++; //conta linhas
            else break;
        }
        switch(ch){
            //Operadores
            case '&':
                if (readch('&')) return symbolsTable.get("&&");
                else return unexpectedToken("&");
            case '|':
                if (readch('|')) return symbolsTable.get("||");
                else return unexpectedToken("|");
            case ',':
                 return symbolsTable.get(",");
            case '=':
                if (readch('=')) return symbolsTable.get("==");
                else return symbolsTable.get("=");
            case '<':
                if (readch('=')) return symbolsTable.get("<=");
                else return symbolsTable.get("<");
            case '>':
                if (readch('=')) return symbolsTable.get(">=");
                else return symbolsTable.get(">");
        }
        //Números @TODO
        if (Character.isDigit(ch)){
            StringBuilder sb = new StringBuilder();
            do{
                sb.append(Character.digit(ch,10));
                readch();
            }while(Character.isDigit(ch));
            return new Token(TokenType.CONSTANT, sb.toString(), null);
        }
        //Identificadores
        if (Character.isLetter(ch)){
            StringBuilder sb = new StringBuilder();
            do{
                sb.append(ch);
                readch();
            }while(Character.isLetterOrDigit(ch));
            String s = sb.toString();
            Token w = symbolsTable.get(s);
            if (w != null) return w; //palavra já existe na HashTable
            w = new Token (TokenType.IDENTIFIER, s, null);
            symbolsTable.put(s, w);
            Token t = symbolsTable.get(Character.toString(ch));
            if(t == null) {
                Token unexpected = unexpectedToken(Character.toString(ch));
                list.add(unexpected);
                errors.add(unexpected.toString());
            }
            ch = ' ';
            return w;
        }


        //Caracteres não especificados
        Token t = unexpectedToken(Character.toString(ch));
        ch = ' ';
        return t;
    }

    private Token unexpectedToken(String lexeme) {
        return new Token(TokenType.UNEXPECTED, lexeme, "Line error: " + line);
    }
}
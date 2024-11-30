package org.example;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class Lexer {
    private static final Pattern KEYWORDS = Pattern.compile("\\b(int|float|if|else|while|for|public|private)\\b");
    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    private static final Pattern INTEGER = Pattern.compile("\\d+");  // Para inteiros
    private static final Pattern FLOAT = Pattern.compile("\\d+\\.\\d+"); // Para números com ponto
    private static final Pattern STRING = Pattern.compile("\"[^\"]*\"");
    public static int line = 1; //contador de linhas
    // Criando uma lista para armazenar os tokens (deve ser definida em algum lugar no código)
    List<Token> list = new ArrayList<>();
    private char ch = ' '; //caractere lido do arquivo
    private boolean finished = false;
    private FileReader file;
    private Hashtable<String, Token> symbolsTable = new Hashtable();
    private List<String> errors = new ArrayList<>();

    public Lexer(String fileName) throws FileNotFoundException {
        try {
            file = new FileReader(fileName);
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo não encontrado");
            throw e;
        }
        reserveLanguageTokens();
    }

    public void processTokens() throws IOException {
        while (!finished) {
            Token t = scan();
            if(t != null){
                list.add(t);
                switch (t.getTokenType()) {
                    case TokenType.UNEXPECTED:
                        errors.add(t.toString());
                        break;
                    case TokenType.IDENTIFIER:
                        Token identifier = symbolsTable.get(t.getLexeme());
                        if (identifier == null) symbolsTable.put(t.getLexeme(), t);
                        break;
                }
            }
            if (ch != ' ') {
                Token remainingCharacter = symbolsTable.getOrDefault(
                        Character.toString(ch),
                        unexpectedToken(Character.toString(ch))
                );
                if (remainingCharacter != null) list.add(remainingCharacter);
                ch = ' ';
            }
        }
    }

    private void readch() throws IOException {
        int nextChar = file.read();
        if (nextChar == -1) {
            printResults();
            file.close(); // Fecha o arquivo após a leitura completa
            System.exit(0);
        } else {
            ch = (char) nextChar;
        }
    }

    /* Lê o próximo caractere do arquivo e verifica se é igual a c*/
    private boolean readch(char c) throws IOException {
        readch();
        if (ch != c) return false;
        ch = ' ';
        return true;
    }

    public Token scan() throws IOException {
        if(line == 2) System.out.println("ch: " + ch);
        //Desconsidera delimitadores na entrada
        for (; ; readch()) {
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\b') continue;
            else if (ch == '\n') line++; //conta linhas
            else break;
        }
        switch (ch) {
            //Operadores
            case '/':
                readch();
                if (ch == '/') return readAndIgnoreComment(false);
                else if(ch == '*') return readAndIgnoreComment(true);
                else return symbolsTable.get("/");
            case '&':
                if (readch('&')) return symbolsTable.get("&&");
                else return unexpectedToken("&");
            case '|':
                if (readch('|')) return symbolsTable.get("||");
                else return unexpectedToken("|");
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
        if (Character.isDigit(ch)) {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append(Character.digit(ch, 10));
                readch();
            } while (Character.isDigit(ch));
            return new Token(TokenType.CONSTANT, sb.toString(), null);
        }
        //Identificadores
        if (Character.isLetter(ch) || ch == '_') {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append(ch);
                readch();
            } while (Character.isLetterOrDigit(ch));
            if(ch == '_') {
                Token unexpected = unexpectedToken(sb.append(ch).toString());
                ch = ' ';
                return unexpected;
            }
            String s = sb.toString();
            Token w = symbolsTable.get(s);
            if (w != null) return w; //palavra já existe na HashTable
            w = new Token(TokenType.IDENTIFIER, s, null);
            symbolsTable.put(s, w);
            return w;
        }

        //Caracteres não especificados
        Token remainingCharacter = symbolsTable.getOrDefault(
                Character.toString(ch),
                unexpectedToken(Character.toString(ch))
        );
        ch = ' ';
        return remainingCharacter;
    }

    private Token readAndIgnoreComment(boolean multiline) throws IOException {
        readch();
        if(multiline){
            while(true){
                readch();
                if(ch == '*'){
                    if(readch('/')) break;
                }
            }
        } else{
            do{
                readch();
            }while (ch != '\n');
        }
        return null;
    }

    private Token unexpectedToken(String lexeme) {
        if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\b' || ch == '\n') return null;
        return new Token(TokenType.UNEXPECTED, lexeme, "Line error: " + line);
    }

    /* Método para inserir palavras reservadas na HashTable */
    private void reserve(Token t) {
        symbolsTable.put(t.getLexeme(), t); // lexema é a chave para entrada na
    }

    private void reserveLanguageTokens() {
        // PROGRAM
        reserve(new Token(TokenType.START, "start", null));
        reserve(new Token(TokenType.EXIT, "exit", null));

        // SPECIAL CHARACTERS
        reserve(new Token(TokenType.COMMA, ",", null));
        reserve(new Token(TokenType.SEMICOLON, ";", null));

        // TYPE
        reserve(new Token(TokenType.TYPE, "int", null));
        reserve(new Token(TokenType.TYPE, "float", null));
        reserve(new Token(TokenType.TYPE, "string", null));
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

    private void printResults() {
        // Definindo a largura de cada coluna
        final int COL_WIDTH_1 = 10; // Largura para "TYPE"
        final int COL_WIDTH_2 = 15; // Largura para "LEXEME"

        // Cabeçalho da tabela
        System.out.println("=====================");
        System.out.println("TABELA DE SIMBOLOS: " + symbolsTable.size());
        System.out.println("=====================");

        System.out.printf("%-" + COL_WIDTH_1 + "s | %-" + COL_WIDTH_2 + "s%n", "TOKEN TYPE", "LEXEME");

        for (Map.Entry<String, Token> entry : symbolsTable.entrySet()) {
            Token valor = entry.getValue();
            // Ajustar o método toString do Token para retornar os valores corretamente
            System.out.printf("%-" + COL_WIDTH_1 + "s | %-" + COL_WIDTH_2 + "s%n", valor.getTokenType(), valor.getLexeme());
        }

        // Imprimindo a tabela de TOKENS (sem VALUE)
        System.out.println("=====================");
        System.out.println("      TOKENS: " + list.size());
        System.out.println("=====================");
        System.out.printf("%-" + COL_WIDTH_1 + "s | %-" + COL_WIDTH_2 + "s%n", "TOKEN TYPE", "LEXEME");

        for (Token t : list) {
            System.out.printf("%-" + COL_WIDTH_1 + "s | %-" + COL_WIDTH_2 + "s%n",
                    t.getTokenType(), t.getLexeme());
        }

        // Imprimindo erros
        System.out.println("=====================");
        System.out.println("      ERROS: " + errors.size());
        System.out.println("=====================");

        for (String error : errors) {
            System.out.println(error);
        }
    }


}
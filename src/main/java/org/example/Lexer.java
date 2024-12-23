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
    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9]*");
    private static final Pattern INTEGER = Pattern.compile("\\d+");  // Para inteiros
    private static final Pattern FLOAT = Pattern.compile("\\d+\\.\\d+"); // Para números com ponto
    private static final Pattern STRING = Pattern.compile("\"[^\"]*\"");
    public static int line = 1; //contador de linhas
    public static int col = 0;
    List<Token> list = new ArrayList<>();
    private char ch = ' '; //caractere lido do arquivo
    private boolean finished = false;
    private FileReader file;
    private Hashtable<String, Token> symbolsTable = new Hashtable<>();
    private List<String> errors = new ArrayList<>();
    private boolean commentIsClosed = true;
    private int commentLineStart = 0;

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
            if (t != null) {
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
        }
        finishProcess();
    }

    private void readch() throws IOException {
        col++;
        int nextChar = file.read();
        if (nextChar == -1) {
            finished = true;
            ch = '\0';
        } else {
            ch = (char) nextChar;
        }
    }

    private void finishProcess() {
        if (!commentIsClosed) {
            Token error = new Token(TokenType.ERROR, "A seção de comentarios '/*' foi aberta, entretanto não houve fechamento '*/'", "Erro na linha: " + commentLineStart);
            list.add(error);
            errors.add(error.toString());
        }
        printResults();
        System.exit(0);
    }

    /* Lê o próximo caractere do arquivo e verifica se é igual a c*/
    private boolean readch(char c) throws IOException {
        readch();
        if (ch != c) return false;
        ch = ' ';
        return true;
    }

    public Token scan() throws IOException {
        //Desconsidera delimitadores na entrada
        for (; ; readch()) {
            if (finished) break;
            else if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\b') continue;
            else if (ch == '\n') {
                line++; //conta linhas
                col = 0;
            } else break;
        }
        switch (ch) {
            //Operadores
            case '(':
                readch();
                return symbolsTable.get("(");
            case ')':
                readch();
                return symbolsTable.get(")");
            case '{':
                return readLiteral();
            case '/':
                readch();
                if (ch == '/') return readAndIgnoreComment(false);
                else if (ch == '*') return readAndIgnoreComment(true);
                else return symbolsTable.get("/");
            case '&':
                if (readch('&')) return symbolsTable.get("&&");
                else {
                    col--;
                    return unexpectedToken("&");
                }
            case '|':
                if (readch('|')) return symbolsTable.get("||");
                else {
                    col--;
                    return unexpectedToken("|");
                }
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
                sb.append(ch);
                readch();
            } while (ch != ';' && ch != '\n' && ch != ' ' && ch != ',' && ch != '(' && ch != ')');
            boolean isFloat = FLOAT.matcher(sb.toString()).matches();
            boolean isInteger = INTEGER.matcher(sb.toString()).matches();
            if (isFloat) return new Token(TokenType.CONSTANT_FLOAT, sb.toString(), null);
            else if (isInteger) return new Token(TokenType.CONSTANT_INTEGER, sb.toString(), null);
            else return unexpectedToken(sb.toString());
        }
        //Identificadores
        if (Character.isLetter(ch) || ch == '_') {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append(ch);
                readch();
            } while (Character.isLetterOrDigit(ch) || ch == '_');
            boolean idMatch = IDENTIFIER.matcher(sb.toString()).matches();
            if (!idMatch) {
                return unexpectedToken(sb.toString());
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

    private Token readLiteral() throws IOException {
        readch();
        StringBuilder sb = new StringBuilder();
        do {
            if (ch == '}') {
                readch();
                return new Token(TokenType.LITERAL, sb.toString(), null);
            }
            sb.append(ch);
            readch();
        } while (ch != '\n');
        Token errorToken = new Token(TokenType.ERROR, "Não houve fechamento de string.", "Erro na linha: " + line);
        errors.add(errorToken.getLexeme() + " " + errorToken.getValue());
        return errorToken;
    }

    private Token readAndIgnoreComment(boolean multiline) throws IOException {
        commentLineStart = line;
        readch();
        if (multiline) {
            commentIsClosed = false;
            while (!finished) {
                readch();
                if (ch == '*') {
                    if (readch('/')) {
                        commentIsClosed = true;
                        break;
                    }
                }
            }
        } else {
            do {
                readch();
            } while (ch != '\n');
        }
        return null;
    }

    private Token unexpectedToken(String lexeme) {
        if (lexeme.equals(" ") || lexeme.equals("\t") || lexeme.equals("\r") || lexeme.equals("\b") || lexeme.equals("\n") || lexeme.equals("\u0000"))
            return null;
        return new Token(TokenType.UNEXPECTED, lexeme, "Erro na linha: " + line + " coluna: " + col);
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
        reserve(new Token(TokenType.EQUALS, "=", null));  // Operador lógico AND

        //ROUND BRACKETS
        reserve(new Token(TokenType.OPEN_ROUND, "(", null));
        reserve(new Token(TokenType.CLOSE_ROUND, ")", null));
    }

    private void printResults() {
        // Definindo a largura de cada coluna
        final int COL_WIDTH_1 = 10; // Largura para "TYPE"
        final int COL_WIDTH_2 = 15; // Largura para "LEXEME"

        // Cabeçalho da tabela
        List<Map.Entry<String, Token>> sortedEntries = new ArrayList<>(symbolsTable.entrySet());

// Ordenar as entradas pela propriedade TokenType de Token
        sortedEntries.sort((entry1, entry2) -> entry1.getValue().getTokenType().compareTo(entry2.getValue().getTokenType()));

        System.out.println("=====================");
        System.out.println("TABELA DE SIMBOLOS: " + symbolsTable.size());
        System.out.println("=====================");

        System.out.printf("%-" + COL_WIDTH_1 + "s | %-" + COL_WIDTH_2 + "s%n", "TOKEN TYPE", "LEXEME");

        for (Map.Entry<String, Token> entry : sortedEntries) {
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
        if(errors.isEmpty()){
            System.out.println("CÓDIGO FONTE VÁLIDO. NÃO HOUVE ERROS ENCONTRADOS.");
        }else{

            // Imprimindo erros
            System.out.println("=====================");
            System.out.println("      ERROS: " + errors.size());
            System.out.println("=====================");

            for (String error : errors) {
                System.out.println(error);
            }
            System.out.println("=====================");
            System.out.println("CÓDIGO FONTE INVÁLIDO.");
        }

    }


}
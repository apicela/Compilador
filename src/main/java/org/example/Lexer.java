package org.example;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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
    public List<String> errors = new ArrayList<>();
    private boolean commentIsClosed = true;
    private int commentLineStart = 0;
    private final String fileName;

    public Lexer(String fileName) throws FileNotFoundException {
        try {
            this.fileName = fileName;
            file = new FileReader(fileName);
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo não encontrado");
            throw e;
        }
        reserveLanguageTokens();
    }

    public void restart() throws Exception {
         new Lexer(this.fileName).processTokens();
    }

    public List<Token> processTokens() throws Exception {
        try {
            while (!finished) {
                Token t = scan();
                if (t != null) {
                    list.add(t);
                    switch (t.getType()) {
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
        } catch(Exception e){
            System.out.println(e.getCause());
            System.out.println(e.getStackTrace());
            System.out.println(e.getMessage());
        }
        return list;
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

    private void finishProcess() throws Exception {
        if (!commentIsClosed) {
            String stringNotClosedMessage = "A seção de comentarios '/*' foi aberta, entretanto não houve fechamento '*/'. Linha: " + commentLineStart;
            try (FileWriter writer = new FileWriter(fileName, true)) {
                writer.write("*/");
                writer.flush();
                System.out.println(stringNotClosedMessage);
                System.out.println("Houve um erro de comentário não fechado. Código-fonte corrigido e iremos re-executar!");
                restart();
            } catch (IOException e) {
                System.err.println("Erro ao escrever no arquivo: " + e.getMessage());
            }
        }
        list.add(new Token(TokenType.EOF,"",line ));
      //  if(!errors.isEmpty()) throw new Exception("Código fonte Invalido");
      //  printResults();
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
                var x = symbolsTable.get("(");
                return new Token(x.getType(), x.getLexeme(), line);
            case ')':
                readch();
                var xs = symbolsTable.get(")");
                return new Token(xs.getType(), xs.getLexeme(), line);
                case '{':
                return readLiteral();
            case '/':
                readch();
                if (ch == '/') return readAndIgnoreComment(false);
                else if (ch == '*') return readAndIgnoreComment(true);
                else return symbolsTable.get("/").setLine(line);
            case '&':
                if (readch('&')) return symbolsTable.get("&&").setLine(line);
                else {
                    col--;
                    return unexpectedToken("&");
                }
            case '|':
                if (readch('|')) return symbolsTable.get("||").setLine(line);
                else {
                    col--;
                    return unexpectedToken("|");
                }
            case '!':
                if (readch('=')) return symbolsTable.get("!=").setLine(line);
                else return symbolsTable.get("!").setLine(line);
            case '=':
                if (readch('=')) return symbolsTable.get("==").setLine(line);
                else return symbolsTable.get("=").setLine(line);
            case '<':
                if (readch('=')) return symbolsTable.get("<=").setLine(line);
                else return symbolsTable.get("<").setLine(line);
            case '>':
                if (readch('=')) return symbolsTable.get(">=").setLine(line);
                else return symbolsTable.get(">").setLine(line);
        }
        //Números @TODO
        if (Character.isDigit(ch)) {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append(ch);
                readch();
            } while (ch != ';' && ch != '\n' && ch != ' ' && ch != ',' && ch != '(' && ch != ')' && ch != '+' && ch != '-' && ch != '*' && ch != '&' && ch != '/' && ch != '%' && ch != '>' && ch != '<'&& ch != '=');
            boolean isFloat = FLOAT.matcher(sb.toString()).matches();
            boolean isInteger = INTEGER.matcher(sb.toString()).matches();
            if (isFloat) return new Token(TokenType.CONSTANT_FLOAT, sb.toString(), line);
            else if (isInteger) return new Token(TokenType.CONSTANT_INTEGER, sb.toString(), line);
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
            if (w != null) {
                Token newToken = new Token(w.getType(), w.getLexeme(), line);
                return newToken;
            }
            w = new Token(TokenType.IDENTIFIER, s, line);
            symbolsTable.put(s, w);
            return w;
        }

        //Caracteres não especificados
        Token remainingCharacter = symbolsTable.getOrDefault(
                Character.toString(ch),
                unexpectedToken(Character.toString(ch))
        );
        if(remainingCharacter != null) remainingCharacter = new Token(remainingCharacter.getType(), remainingCharacter.getLexeme(), line);
        ch = ' ';
        return remainingCharacter;
    }

    private Token readLiteral() throws IOException {
        readch();
        StringBuilder sb = new StringBuilder();
        do {
            if (ch == '}') {
                readch();
                return new Token(TokenType.LITERAL, sb.toString(), line);
            }
            sb.append(ch);
            readch();
        } while (ch != '\n');
        Token errorToken = new Token(TokenType.ERROR, "Não houve fechamento de string.", line);
        errors.add(errorToken.getLexeme() + " " + errorToken.getLine());
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
        return new Token(TokenType.UNEXPECTED, lexeme, line);
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
        reserve(new Token(TokenType.IF, "if", null));
        reserve(new Token(TokenType.ELSE, "else", null));
        reserve(new Token(TokenType.THEN, "then", null));
        reserve(new Token(TokenType.END, "end", null));
        reserve(new Token(TokenType.DO, "do", null));
        reserve(new Token(TokenType.WHILE, "while", null));
        reserve(new Token(TokenType.SCAN, "scan", null));
        reserve(new Token(TokenType.PRINT, "print", null));
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
        reserve(new Token(TokenType.NOT, "!", null));  // Operador lógico AND
        //ROUND BRACKETS
        reserve(new Token(TokenType.OPEN_ROUND, "(", null));
        reserve(new Token(TokenType.CLOSE_ROUND, ")", null));
    }

    public void printErrors(){
        if(errors.isEmpty()) return;
            System.out.println("=====================");
            System.out.println("      ERROS: " + errors.size());
            System.out.println("=====================");
            for (String error : errors) {
                System.out.println(error);
            }
            System.out.println("=====================");
            System.out.println("CÓDIGO FONTE INVÁLIDO. FAVOR CORRIGIR ERROS LÉXICOS APRESENTADOS.");
            System.exit(0);
    }
}
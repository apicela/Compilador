package org.example;
import java.io.*;
import java.util.*;


public class Lexer {
    public static int line = 1; //contador de linhas
    private char ch = ' '; //caractere lido do arquivo
    private boolean finished = false;
    private FileReader file;
    private Hashtable words = new Hashtable();
    int tokensCount = 0;


    public void processTokens() throws IOException {
        Token token;
        while (!finished) {
            token = scan();
            tokensCount++;
            // Aqui você pode processar o token se necessário
            System.out.println("Token: " + token);
        }

    }


    /* Método para inserir palavras reservadas na HashTable */
    private void reserve(Word w){
        words.put(w.getLexeme(), w); // lexema é a chave para entrada na
        //HashTable
    }
    public Lexer(String fileName) throws FileNotFoundException{
        try{
            file = new FileReader (fileName);
        }
        catch(FileNotFoundException e){
            System.out.println("Arquivo não encontrado");
            throw e;
        }
        //Insere palavras reservadas na HashTable
        reserve(new Word ("program", Tag.PRG));
        reserve(new Word ("begin", Tag.BEG));
        reserve(new Word ("end", Tag.END));
        reserve(new Word ("type", Tag.TYPE));
    }
    private void readch() throws IOException{
        int nextChar = file.read();
        if (nextChar == -1) {
            System.out.println("Tokens encontrados: " + tokensCount);
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
//            case '&':
//                if (readch('&')) return Word.and;
//                else return new Token('&');
//            case '|':
//                if (readch('|')) return Word.or;
//                else return new Token('|');
            case '=':
                if (readch('=')) return Word.eq;
                else return new Token('=');
            case '<':
                if (readch('=')) return Word.le;
                else return new Token('<');
            case '>':
                if (readch('=')) return Word.ge;
                else return new Token('>');
        }
        //Números
        if (Character.isDigit(ch)){
            int value=0;
            do{
                value = 10*value + Character.digit(ch,10);
                readch();
            }while(Character.isDigit(ch));
//            System.out.println("new token NUM: " +value);

            return new Num(value);
        }
        //Identificadores
        if (Character.isLetter(ch)){
            StringBuffer sb = new StringBuffer();
            do{
                sb.append(ch);
                readch();
            }while(Character.isLetterOrDigit(ch));
            String s = sb.toString();
            Word w = (Word)words.get(s);
            if (w != null) return w; //palavra já existe na HashTable
            w = new Word (s, Tag.ID);
//            System.out.println("new token WORD: " + w.getLexeme());
            words.put(s, w);
            if (w != null) {
                ch = ' '; // Limpa o caractere atual explicitamente
                return w;
            }
        }

        //Caracteres não especificados
        Token t = new Token(ch);
        ch = ' ';
        return t;
    }
}
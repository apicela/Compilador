package org.example;
import java.io.*;
import java.util.*;


public class Lexer {
    public static int line = 1; //contador de linhas
    private char ch = ' '; //caractere lido do arquivo
    private boolean finished = false;
    private FileReader file;
    private Hashtable<String, Token> words = new Hashtable();

    // Criando uma lista para armazenar os tokens (deve ser definida em algum lugar no código)
    List<Token> list = new ArrayList<>();

    public void processTokens() throws IOException {
        while (!finished) {
            list.add(scan());
            // Aqui você pode processar o token se necessário
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
//        System.out.println("ch: " + (char) (nextChar) + " ASCII:" + nextChar);
        if (nextChar == -1) {

            System.out.println("TABELA DE SIMBOLOS: ");
            for (Map.Entry<String, Token> entry : words.entrySet()) {
                String chave = entry.getKey();
                Token valor = entry.getValue();
                System.out.println("Chave: " + chave + ", Valor: " + valor);
            }

            // Imprimindo todos os valores
            System.out.println("TOKENS: ");
            for(Token t : list){
                System.out.println(t.toString());
            }
            System.out.println("Tokens encontrados: " + list.size());
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
            case ',':
                 return new Token(',');
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
//            System.out.println("s: " + s + "\nw: " + w);
            words.put(s, w);
            if (w != null) {
                list.add(new Token(ch));
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
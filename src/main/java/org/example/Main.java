package org.example;

import java.util.List;//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

public class Main {

    public static void main(String[] args) {
        // Verifica se o nome do arquivo foi passado como argumento
        /*
        if (args.length < 1) {
            System.out.println("Por favor, forneça o nome do arquivo como argumento.");
            return; 
        }
*/
        ParserSemantic parserSemantic = null;
        // O nome do arquivo é o primeiro argumento passado
        //  String caminhoArquivo = args[0];
        String caminhoArquivo = "out\\artifacts\\Compiler_jar\\oi.txt";
        try {
            Lexer lexer = new Lexer(caminhoArquivo);
            List<Token> tokens = lexer.processTokens();
            for (Token t : tokens) {
                System.out.println(t);
            }
            parserSemantic = new ParserSemantic(tokens, caminhoArquivo);
            parserSemantic.start();
            parserSemantic.printTable();

        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        } finally {
            if(parserSemantic != null) parserSemantic.printErrors();
        }
    }
}
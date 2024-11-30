package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        // Verifica se o nome do arquivo foi passado como argumento
        if (args.length < 1) {
            System.out.println("Por favor, forneça o nome do arquivo como argumento.");
            return;
        }
        // O nome do arquivo é o primeiro argumento passado
        String caminhoArquivo = args[0];

        try {
            Lexer lexer = new Lexer(caminhoArquivo);
            lexer.processTokens();

        } catch (Exception e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }
    }
}
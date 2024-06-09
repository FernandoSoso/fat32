package br.ufsm.politecnico.csi.so;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        //Inicio
        System.out.println("Iniciando filesystem...");
        Fat32FS fat32FS = new Fat32FS();

        System.out.println("Capturando dados do arquivo \"arqInput.txt\"...");
        byte[] dataInput = Files.readAllBytes(Paths.get("./data/arqInput.txt"));

        System.out.println("\nIniciando teste de Criação");//Espaçador
        //Criação de arquivos
        //Arquivo 1
        System.out.println("\n####################################################################################################");
        System.out.println("Teste 1");
        System.out.println("####################################################################################################");
        System.out.println("Criando arquivo \"Teste.txt\"...");
        fat32FS.create("Teste.txt", dataInput);

        System.out.println("Exibindo diretório...");
        fat32FS.exibirDiretorio();

        //Arquivo 2
        System.out.println("\n####################################################################################################");
        System.out.println("Teste 2");
        System.out.println("####################################################################################################");
        System.out.println("Criando arquivo \"SegTeste.txt\"...");
        fat32FS.create("SegTeste.txt", dataInput);

        System.out.println("Exibindo diretório...");
        fat32FS.exibirDiretorio();

        //Arquivo 3
        System.out.println("\n####################################################################################################");
        System.out.println("Teste 3");
        System.out.println("####################################################################################################");
        System.out.println("Criando arquivo \"TerTeste.txt\"...");
        fat32FS.create("TerTeste.txt", dataInput);

        System.out.println("Exibindo diretório...");
        fat32FS.exibirDiretorio();

        System.out.println("Iniciando teste de adição de dados");//Espaçador
        //Adição de dados em arquivo existente
        System.out.println("\n####################################################################################################");
        System.out.println("Teste 1");
        System.out.println("####################################################################################################");
        //Arquivo 1
        System.out.println("Adicionando dados ao arquivo \"Teste.txt\"...");
        fat32FS.append("Teste.txt", dataInput);
        fat32FS.exibirDiretorio();

        //Arquivo 2
        System.out.println("\n####################################################################################################");
        System.out.println("Teste 2");
        System.out.println("####################################################################################################");

        System.out.println("Adicionando dados ao arquivo \"SegTeste.txt\"...");
        fat32FS.append("SegTeste.txt", dataInput);
        fat32FS.exibirDiretorio();

        //Arquivo 1
        System.out.println("\n####################################################################################################");
        System.out.println("Teste 3");
        System.out.println("####################################################################################################");

        System.out.println("Adicionando dados ao arquivo \"Teste.txt\"...");
        fat32FS.append("Teste.txt", dataInput);
        fat32FS.exibirDiretorio();

        System.out.println("Iniciando teste de Leitura");//Espaçador
        //Leitura de arquivos
        byte[] dataOutput;

        //Arquivo 1
        System.out.println("\n####################################################################################################");
        System.out.println("Teste 1");
        System.out.println("####################################################################################################");

        System.out.println("Lendo todo o arquivo \"Teste.txt\"...");
        dataOutput = fat32FS.read("Teste.txt", 0, -1);
        System.out.println("Arquivo lido com sucesso!");
        System.out.println("Tamanho do arquivo lido: " + dataOutput.length + " bytes");
        //Gravação do arquivo 1 em um arquivo de texto externo
        Files.write(Paths.get("./data/testeLeitura/arqOutput-Teste-Completo.txt"), dataOutput);

        //Arquivo 2

        System.out.println("\n####################################################################################################");
        System.out.println("Teste 2");
        System.out.println("####################################################################################################");

        System.out.println("\nLendo 2 blocos do arquivo \"SegTeste.txt\"...");
        dataOutput = fat32FS.read("SegTeste.txt", 0, -1);
        System.out.println("Tamanho do arquivo original: " + dataOutput.length + " bytes");
        dataOutput = fat32FS.read("SegTeste.txt", 0, 2);
        System.out.println("Tamanho do arquivo: " + dataOutput.length + " bytes");
        //Gravação do arquivo 1 em um arquivo de texto externo
        Files.write(Paths.get("./data/testeLeitura/arqOutput-SegTeste-Bloco0-1.txt"), dataOutput);

        //Arquivo 3
        System.out.println("\n####################################################################################################");
        System.out.println("Teste 3");
        System.out.println("####################################################################################################");

        System.out.println("\nLendo 2 blocos do arquivo \"TerTeste.txt\" a partir do 2º bloco...");
        dataOutput = fat32FS.read("TerTeste.txt", 0, -1);
        System.out.println("Tamanho do arquivo original: " + dataOutput.length + " bytes");
        dataOutput = fat32FS.read("TerTeste.txt", 2, 2);
        System.out.println("Tamanho do arquivo lido: " + dataOutput.length + " bytes (Somente 1 dos blocos estava ocupado pelo arquivo)");
        //Gravação do arquivo 1 em um arquivo de texto externo
        Files.write(Paths.get("./data/testeLeitura/arqOutput-TerTeste-Bloco2-3.txt"), dataOutput);

        System.out.println("\nIniciando Teste de Remoção");//Espaçador
        //Remoção de arquivos
        int espacoPreRemocao;
        int tamanhoArquivoRemovido;
        int espacoPosRemocao;

        //Arquivo 1
        System.out.println("\n####################################################################################################");
        System.out.println("Teste 1");
        System.out.println("####################################################################################################");

        System.out.println("Diretório antes da remoção:");
        fat32FS.exibirDiretorio();

        System.out.println("Removendo arquivo \"Teste.txt\"...\n");
        espacoPreRemocao = fat32FS.freeSpace();
        tamanhoArquivoRemovido = fat32FS.read("Teste.txt", 0, -1).length;
        fat32FS.remove("Teste.txt");
        espacoPosRemocao = fat32FS.freeSpace();

        System.out.println("Diretório após a remoção:");
        fat32FS.exibirDiretorio();
        System.out.println("Tamanho do arquivo liberado: " + tamanhoArquivoRemovido + " bytes");
        System.out.println("Espaço liberado do disco: " + (espacoPosRemocao-espacoPreRemocao) + " bytes");
        System.out.println("Blocos liberados: " + (espacoPosRemocao-espacoPreRemocao)/(64*1024) + " blocos");

        //Arquivo 2
        System.out.println("\n####################################################################################################");
        System.out.println("Teste 2");
        System.out.println("####################################################################################################");

        System.out.println("\nDiretório antes da remoção:");
        fat32FS.exibirDiretorio();

        System.out.println("Removendo arquivo \"SegTeste.txt\"...\n");
        espacoPreRemocao = fat32FS.freeSpace();
        tamanhoArquivoRemovido = fat32FS.read("SegTeste.txt", 0, -1).length;
        fat32FS.remove("SegTeste.txt");
        espacoPosRemocao = fat32FS.freeSpace();

        System.out.println("Diretório após a remoção:");
        fat32FS.exibirDiretorio();
        System.out.println("Tamanho do arquivo liberado: " + tamanhoArquivoRemovido + " bytes");
        System.out.println("Espaço liberado do disco: " + (espacoPosRemocao-espacoPreRemocao) + " bytes");
        System.out.println("Blocos liberados: " + (espacoPosRemocao-espacoPreRemocao)/(64*1024) + " blocos");

        //Arquivo 3
        System.out.println("\n####################################################################################################");
        System.out.println("Teste 3");
        System.out.println("####################################################################################################");

        System.out.println("\nDiretório antes da remoção:");
        fat32FS.exibirDiretorio();

        System.out.println("Removendo arquivo \"TerTeste.txt\"...\n");
        espacoPreRemocao = fat32FS.freeSpace();
        tamanhoArquivoRemovido = fat32FS.read("TerTeste.txt", 0, -1).length;
        fat32FS.remove("TerTeste.txt");
        espacoPosRemocao = fat32FS.freeSpace();

        System.out.println("Diretório após a remoção:");
        fat32FS.exibirDiretorio();
        System.out.println("Tamanho do arquivo liberado: " + tamanhoArquivoRemovido + " bytes");
        System.out.println("Espaço liberado do disco: " + (espacoPosRemocao-espacoPreRemocao) + " bytes");
        System.out.println("Blocos liberados: " + (espacoPosRemocao-espacoPreRemocao)/(64*1024) + " blocos");

        System.out.println("\n");//Espaçador
        //Fim
        System.out.println("Finalizando filesystem...");
    }
}
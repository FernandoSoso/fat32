package br.ufsm.politecnico.csi.so;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Fat32FS implements FileSystem {


    private final Disco disco;

    private final int[] fat = new int[Disco.NUM_BLOCOS];
    private static final int BLOCO_DIRETORIO = 0;
    private static final int BLOCO_FAT = 1;
    private final List<EntradaArquivoDiretorio> diretorioRaiz = new ArrayList<>();

    public Fat32FS() throws IOException {
        this.disco = new Disco();
        if (this.disco.init()) {
            leFat();
            leDiretorio();
        } else {
            criaFat();
            escreveFat();
        }

    }

    //Implementação dos métodos da interface FileSystem

    @Override
    public void create(String fileName, byte[] data) {

        if (data.length > freeSpace()) {
            throw new IllegalArgumentException("Espaço insuficiente para salvar o arquivo");
        }

        if (checkFileName(fileName)){
            throw new IllegalArgumentException("Arquivo já existe");
        }

        String nomeArquivo = this.getNomeFromFileName(fileName);
        String extensao = this.getExtensaoFromFileName(fileName);
        int tamanho = data.length;
        int blocoInicial = encontraBlocoLivre();

        EntradaArquivoDiretorio entrada = new EntradaArquivoDiretorio(nomeArquivo, extensao, tamanho, blocoInicial);

        int blocoAnterior = 0;
        int blocoAtual = blocoInicial;

        for (byte[] dadosAtuais : separaBloco(data, tamanho)) {
            try {
                disco.escreveBloco(blocoAtual, dadosAtuais);

                if (blocoAnterior != 0) {
                    fat[blocoAnterior] = blocoAtual;
                }

                blocoAnterior = blocoAtual;
                fat[blocoAtual] = 0;
                blocoAtual = encontraBlocoLivre();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        diretorioRaiz.add(entrada);

        try {
            escreveDiretorio();
            escreveFat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void append(String fileName, byte[] data) throws IOException {
        if (data.length > freeSpace()) {
            throw new IllegalArgumentException("Espaço insuficiente para salvar o arquivo");
        }

        int tamanhoDiretorio = diretorioRaiz.size();
        for (EntradaArquivoDiretorio arquivo : diretorioRaiz){
            String auxFileName = arquivo.nomeArquivo.trim() + "." + arquivo.extensao.trim();

            if (auxFileName.equals(fileName)){
                int blocoAnterior = 0;
                int blocoAtual = arquivo.blocoInicial;

                while (fat[blocoAtual] != 0) {
                    blocoAnterior = blocoAtual;
                    blocoAtual = fat[blocoAtual];
                }

                byte[] dadosUltimoBloco;
                try {
                    dadosUltimoBloco = disco.leBloco(blocoAtual);
                    dadosUltimoBloco = removeEspacamentoDeBloco(dadosUltimoBloco);
                    arquivo.tamanho -= dadosUltimoBloco.length;

                    byte[] auxDados = new byte[dadosUltimoBloco.length + data.length];

                    System.arraycopy(dadosUltimoBloco, 0, auxDados, 0, dadosUltimoBloco.length);
                    System.arraycopy(data, 0, auxDados, dadosUltimoBloco.length, data.length);
                    data = auxDados;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (byte[] dadosAtuais : separaBloco(data, data.length)) {
                    try {
                        disco.escreveBloco(blocoAtual, dadosAtuais);

                        if (blocoAnterior != 0){
                            fat[blocoAnterior] = blocoAtual;
                        }

                        blocoAnterior = blocoAtual;
                        fat[blocoAnterior] = 0;
                        blocoAtual = encontraBlocoLivre();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                arquivo.tamanho += data.length;

                break;
            }
            tamanhoDiretorio--;
        }

        if (tamanhoDiretorio == 0){
            throw new IllegalArgumentException("Arquivo não encontrado!");
        }

        try {
            escreveDiretorio();
            escreveFat();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public byte[] read(String fileName, int offset, int limit) {
        byte[] dados = null;

        if (diretorioRaiz.isEmpty()){
            throw new IllegalArgumentException("Diretório vazio!");
        }
        else{
            int tamanhoDiretorio = diretorioRaiz.size();
            for (EntradaArquivoDiretorio arquivo : diretorioRaiz){
                String auxFileName = arquivo.nomeArquivo.trim() + "." + arquivo.extensao.trim();

                if (auxFileName.equals(fileName)){

                    int posBloco = arquivo.blocoInicial;
                    int auxLimit = 1;
                    do{
                        if (offset > 0){
                            while (offset > 0){
                                offset--;
                                if (fat[posBloco] != 0){
                                    posBloco = fat[posBloco];
                                }
                                else if (offset > 0){
                                    return new byte[0];
                                }
                            }
                        }

                        try{
                            byte[] dadosBloco = disco.leBloco(posBloco);

                            if (dados == null){
                                dados = dadosBloco;
                            }
                            else{
                                byte[] auxDados = new byte[dados.length + dadosBloco.length];
                                System.arraycopy(dados, 0, auxDados, 0, dados.length);
                                System.arraycopy(dadosBloco, 0, auxDados, dados.length, dadosBloco.length);
                                dados = auxDados;
                            }
                            posBloco = fat[posBloco];
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (limit != -1){
                            if (auxLimit != limit){
                                auxLimit++;
                            }
                            else{
                                break;
                            }
                        }
                    } while (posBloco != 0);
                }
                tamanhoDiretorio--;
            }

            if (tamanhoDiretorio == 0 && dados == null){
                throw new IllegalArgumentException("Arquivo não encontrado!");
            }
        }

        if (dados == null){
            return new byte[0];
        }
        else {
            return removeEspacamentoDeBloco(dados);
        }
    }

    @Override
    public void remove(String fileName) {
        if (diretorioRaiz.isEmpty()){
            throw new IllegalArgumentException("Diretório vazio!");
        }
        else{
            int tamanhoDiretorio = diretorioRaiz.size();
            for (EntradaArquivoDiretorio arquivo : diretorioRaiz){
                String auxFileName = arquivo.nomeArquivo.trim() + "." + arquivo.extensao.trim();

                if (auxFileName.equals(fileName)){

                    removeArquivoFat(arquivo.blocoInicial);
                    diretorioRaiz.remove(arquivo);

                    break;
                }
                tamanhoDiretorio--;
            }

            if (tamanhoDiretorio == 0){
                throw new IllegalArgumentException("Arquivo não encontrado!");
            }
        }

        try {
            escreveDiretorio();
            escreveFat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeArquivoFat(int bloco) {
        if (fat[bloco] != 0) {
            removeArquivoFat(fat[bloco]);
        }
        try {
            disco.escreveBloco(bloco, new byte[64*1024]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        fat[bloco] = -1;
    }

    @Override
    public int freeSpace() {
        int blocosLivres = 0;
        int blocoAtual = 0;

        while (fat.length > blocoAtual){
            if(fat[blocoAtual] == -1){
                blocosLivres++;
            }
            blocoAtual++;
        }

        return blocosLivres*(64*1024);
    }

    //Funções auxiliares

    /**
        * Encontra um bloco livre na FAT.
        * @return o número do bloco livre.
    */
    private int encontraBlocoLivre() {
        for (int i = 2; i < fat.length; i++) {
            if (fat[i] == -1) {
                return i;
            }
        }

        throw new IllegalArgumentException("Não há blocos livres");
    }

    /**
        * Remove os espaçamentos (null) de um bloco.
        * @param data o bloco a ser tratado.
        * @return o bloco sem espaçamentos.
    */
    private byte[] removeEspacamentoDeBloco(byte[] data) {
        int i = data.length - 1;
        while (i >= 0 && data[i] == 0) {
            i--;
        }
        return Arrays.copyOf(data, i + 1);
    }

    /**
        * Separa um array de bytes em blocos de 64KB.
        * @param data o array de bytes a ser separado.
        * @param tamanho o tamanho do array de bytes.
        * @return um ArrayList com os blocos de 64KB.
    */
    private ArrayList<byte[]> separaBloco(byte[] data, int tamanho){
        int tamanhoBloco = 64*1024;
        int blocosNecessarios = (int) Math.ceil((double) tamanho / tamanhoBloco);
        ArrayList<byte[]> dadosEmBlocos = new ArrayList<>(blocosNecessarios);

        for (int i = 0; i < blocosNecessarios; i++) {
            int inicio = i * tamanhoBloco;
            int fim = Math.min(inicio + tamanhoBloco, data.length);
            byte[] blocoAtual = Arrays.copyOfRange(data, inicio, fim);
            dadosEmBlocos.add(blocoAtual);
        }

        return dadosEmBlocos;
    }

    /**
        * Verifica se um arquivo já existe no diretório.
        * @param fileName o nome armazenado no diretorio a ser verificado.
        * @return true se o arquivo existe, false caso contrário.
    */
    private boolean checkFileName (String fileName){
        if (!diretorioRaiz.isEmpty()){
            for (EntradaArquivoDiretorio entrada : diretorioRaiz) {
                String auxFileName = entrada.nomeArquivo.trim() + "." +entrada.extensao.trim();
                if (auxFileName.equals(fileName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
        * Retorna a extensão de um arquivo a partir do nome armazenado no diretorio.
        * @param fileName o nome armazenado no diretorio.
        * @return a extensão do arquivo.
    */
    private String getExtensaoFromFileName (String fileName){
        int tamanhoNome = fileName.length();
        int tamanhoExtensao = 0;

        while (tamanhoNome > 0) {
            if (fileName.charAt(tamanhoNome-1) == '.') {
                break;
            }

            tamanhoNome--;
            tamanhoExtensao++;
        }

        if (tamanhoExtensao > 3) {
            throw new IllegalArgumentException("Nome de extensão inválido! (Maior que 3 characteres)");
        }
        else if (tamanhoExtensao == 0){
            throw new IllegalArgumentException("Nome de extensão vazio!");
        }
        else {
            return fileName.substring(tamanhoNome, tamanhoNome+tamanhoExtensao);
        }
    }

    /**
        * Retorna o nome de um arquivo a partir do nome armazenado no diretorio.
        * @param fileName o nome armazenado no diretorio.
        * @return o nome do arquivo.
    */
    private String getNomeFromFileName (String fileName){
        int tamanhoNome = 0;

        while (tamanhoNome < fileName.length()) {
            if (fileName.charAt(tamanhoNome) != '.') {
                tamanhoNome++;
            }
            else{
                break;
            }
        }

        if(tamanhoNome > 8){
            throw new IllegalArgumentException("Nome de arquivo inválido! (Maior que 8 characteres)");
        }
        else if (tamanhoNome == 0){
            throw new IllegalArgumentException("Nome de arquivo vazio!");
        }

        return fileName.substring(0, tamanhoNome);
    }

    //Funções da FAT

    /**
        * Cria a FAT.
    */
    private void criaFat() {
        for (int i = 2; i < fat.length; i++) {
            fat[i] = -1;
        }
    }

    /**
        * Escreve a FAT no disco.
    */
    private void escreveFat() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(64*1024);
        for (int f : fat) {
            bb.putInt(f);
        }
        byte[] blocoFat = bb.array();
        disco.escreveBloco(BLOCO_FAT, blocoFat);
    }

    /**
        * Lê a FAT do disco.
    */
    private void leFat() throws IOException {
        byte[] blocoFat = disco.leBloco(BLOCO_FAT);
        ByteBuffer bb = ByteBuffer.wrap(blocoFat);
        for (int i = 0; i < 16*1024; i++) {
            fat[i] = bb.getInt();
        }
    }

    //Funções do diretório

    /**
        * Exibe o diretório.
    */
    public void exibirDiretorio() {
        int i = 0;
        for (EntradaArquivoDiretorio arquivo : diretorioRaiz) {
            System.out.println("Nome: " + arquivo.nomeArquivo + " - Extensão: " + arquivo.extensao + " - Tamanho: " + arquivo.tamanho + " bytes - Bloco Inicial: " + arquivo.blocoInicial);
            i++;
        }

        if (i == 0){
            System.out.println("=== Diretório vazio! ===");
        }

        System.out.println("Espaço Livre: " + freeSpace() + " bytes\n");
    }

    /**
        * Escreve o diretório no disco.
    */
    private void escreveDiretorio() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(Disco.TAM_BLOCO);
        for (EntradaArquivoDiretorio entrada : diretorioRaiz) {
            entrada.toByteArray(bb);
        }
        disco.escreveBloco(BLOCO_DIRETORIO, bb.array());
    }

    /**
        * Lê o diretório do disco.
    */
    private void leDiretorio() throws IOException {
        byte[] dirBytes = disco.leBloco(BLOCO_DIRETORIO);
        ByteArrayInputStream bin = new ByteArrayInputStream(dirBytes);
        EntradaArquivoDiretorio entrada = null;
        do {
            try {
                entrada = EntradaArquivoDiretorio.fromStream(bin);
                if (entrada != null) {
                    diretorioRaiz.add(entrada);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        } while(bin.available() > 0);
    }

    //Classe auxiliar EntradaArquivoDiretorio
    private static class EntradaArquivoDiretorio {
        private String nomeArquivo;
        private String extensao;
        private int tamanho;
        private final int blocoInicial;

        public EntradaArquivoDiretorio(String nomeArquivo,
                                       String extensao,
                                       int tamanho,
                                       int blocoInicial) {
            this.nomeArquivo = nomeArquivo;
            if (this.nomeArquivo.length() > 8) {
                this.nomeArquivo = nomeArquivo.substring(0, 8);
            } else if (this.nomeArquivo.length() < 8) {
                do {
                    this.nomeArquivo += " ";
                } while (this.nomeArquivo.length() < 8);
            }
            this.extensao = extensao;
            if (this.extensao.length() > 3) {
                this.extensao = extensao.substring(0, 3);
            } else if (this.extensao.length() < 3) {
                do {
                    this.extensao += " ";
                } while (this.extensao.length() < 3);
            }
            this.tamanho = tamanho;
            this.blocoInicial = blocoInicial;

            if (blocoInicial < 2 || blocoInicial >= Disco.NUM_BLOCOS) {
                System.out.println("Bloco: " + blocoInicial + " - Nome: " + nomeArquivo + " - Extensão: " + extensao + " - Tamanho: " + tamanho + " bytes.");
                throw new IllegalArgumentException("Número de bloco invalido");
            }
        }

        //Métodos auxiliares

        /**
            * Converte a entrada para um array de bytes.
            * @param bb o ByteBuffer a ser convertido.
            * @return o array de bytes.
        */
        public byte[] toByteArray(ByteBuffer bb) {
            bb.put(nomeArquivo.getBytes(StandardCharsets.ISO_8859_1));
            bb.put(extensao.getBytes(StandardCharsets.ISO_8859_1));
            bb.putInt(tamanho);
            bb.putInt(blocoInicial);
            return bb.array();
        }

        /**
            * Converte um array de bytes para um inteiro.
            * @param data o array de bytes a ser convertido.
            * @param index o índice do array de bytes.
            * @return o inteiro convertido.
        */
        private static int intFromBytes(byte[] data, int index) {
            ByteBuffer bb = ByteBuffer.wrap(data);
            return bb.getInt(index);
        }

        /**
            * Converte um array de bytes para uma entrada de arquivo de diretório.
            * @param bytes o array de bytes a ser convertido.
            * @return a entrada de arquivo de diretório.
        */
        public static EntradaArquivoDiretorio fromBytes(byte[] bytes) {
            String nome = new String(bytes,
                    0, 8, StandardCharsets.ISO_8859_1);
            String extensao = new String(bytes,
                    8, 3, StandardCharsets.ISO_8859_1);
            int tamanho = intFromBytes(bytes, 11);
            int blocoInicial = intFromBytes(bytes, 15);

            return new EntradaArquivoDiretorio(nome, extensao, tamanho, blocoInicial);
        }

        /**
            * Converte um InputStream para uma entrada de arquivo de diretório.
            * @param inputStream o InputStream a ser convertido.
            * @return a entrada de arquivo de diretório.
        */
        public static EntradaArquivoDiretorio fromStream(InputStream inputStream) throws IOException {
            byte[] bytes = new byte[19];
            inputStream.read(bytes);
            String nome = new String(bytes,
                    0, 8, StandardCharsets.ISO_8859_1);
            String extensao = new String(bytes,
                    8, 3, StandardCharsets.ISO_8859_1);
            int tamanho = intFromBytes(bytes, 11);
            int blocoInicial = intFromBytes(bytes, 15);

            if (blocoInicial < 2 || blocoInicial >= Disco.NUM_BLOCOS) {
                return null;
            }
            else {
                return new EntradaArquivoDiretorio(nome, extensao, tamanho, blocoInicial);
            }
        }

    }

}

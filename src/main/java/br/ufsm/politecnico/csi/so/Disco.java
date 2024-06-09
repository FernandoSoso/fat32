package br.ufsm.politecnico.csi.so;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Disco {

    private static final String NOME_ARQUIVO = "fat32.data";
    public static final int NUM_BLOCOS = 16*1024;
    public static final int TAM_BLOCO = 64*1024;
    private RandomAccessFile disco;

    public boolean init() throws IOException {
        File f = new File(NOME_ARQUIVO);
        //le arquivo
        boolean existe = f.exists();
        this.disco = new RandomAccessFile(f, "rws");
        this.disco.setLength(NUM_BLOCOS * TAM_BLOCO);
        return existe;
    }

    public byte[] leBloco(int number) throws IOException {
        if (number < 0 || number >= NUM_BLOCOS) {
            throw new IllegalArgumentException("Numero de bloco invalido. Deve estar entre 0 e " + NUM_BLOCOS);
        }
        byte[] data = new byte[TAM_BLOCO];
        this.disco.seek(number * TAM_BLOCO);
        this.disco.read(data);
        return data;
    }

    public void escreveBloco(int number, byte[] data) throws IOException {
        if (number < 0 || number >= NUM_BLOCOS) {
            throw new IllegalArgumentException("Numero de bloco invalido. Deve estar entre 0 e " + NUM_BLOCOS);
        }
        if (data.length > TAM_BLOCO) {
            throw new IllegalArgumentException("Tamanho do bloco não pode exceder 64KB.");
        }
        this.disco.seek(number * TAM_BLOCO);
        this.disco.write(data);
    }

}

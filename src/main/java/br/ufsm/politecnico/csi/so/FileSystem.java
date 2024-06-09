package br.ufsm.politecnico.csi.so;

import java.io.IOException;

public interface FileSystem {
    /**
     * Cria um novo arquivo.
     * @param fileName nome do arquivo para criar
     * @param data dados a serem salvos
     */
    void create(String fileName, byte[] data);
    /**
     * Adiciona dados ao final do arquivo.
     * @param fileName nome do arquivo
     * @param data dados a serem adicionados
     */
    void append(String fileName, byte[] data) throws IOException;
    /**
     * Lê arquivo.
     * @param fileName nome do arquivo
     * @param offset a partir de qual posição a leitura deve ser feita.
     * @param limit até aonde a leitura será feita, -1 para ler até o final do arquivo.
     * @return dados lidos
     */
    byte[] read(String fileName, int offset, int limit);
    /**
     * Remove o arquivo.
     * @param fileName
     */
    void remove(String fileName);
    /**
     * Calcula o espaço disponível no sistema de arquivos.
     * @return bytes disponíveis
     */
    int freeSpace();
}

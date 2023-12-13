/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.edu.ihu.expblock;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author Administrator
 */
public class ExpBlock {

    /**
     * @param args the command line arguments
     */
    public double epsilon;
    public double delta = 0.1;
    public double q; //probabilidade de eviction de um registro dentro de um bloco
    public int w; // quantidade de posiçoes de cada bloco
    public int b; // quantidade maxima de numero de blocos
    public int globalRecNo = 0; // quantidade total de records no banco, aumentado toda vez que entra um novo registro
    public int occupied = 0;
    public double xi = .08; // ratio que o artigo fala
    public int currentRound = 1;
    public int matchingPairsNo = 0;
    public int trulyMatchingPairsNo = 100;
    public MinHash minHash = new MinHash();
    public static FileWriter writer;
    public Block[] arr;
    IntStream rS;
    int[] r;
    int noRandoms = 50;

    public ExpBlock(double epsilon, double q, int b) {
        try {
            this.epsilon = epsilon;
            this.b = b;
            this.q = q;
            this.arr = new Block[this.b];

            for (int i = 0; i < 10; i++) {
                IntStream rS = new SplittableRandom().ints(this.noRandoms / 10, 0, this.b);
                int[] r = rS.toArray();
                Arrays.sort(r);
                System.out.println(Arrays.toString(r));
                this.r = ArrayUtils.addAll(this.r, r);
            }
            System.out.println(this.r.length);
//          this.w = (int) Math.ceil(3 * Math.log(2 / this.delta) / (this.q * (this.epsilon * this.epsilon)));
            this.w = 3;
            writer = new FileWriter("results.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void put(Record rec) {
        //System.out.println("occupied="+this.occupied);
        System.out.println("Entrou no processo do put com occupied = " + this.occupied + "e com record " + rec.id);
        if (this.occupied == b) {
            System.out.println("entrou no processo de evitcion");
            int avg = this.globalRecNo / b; // average numbe of hits per block
            if (avg == 0) {
                avg = 1;
            }

            int v = 0;
            long startTime = System.nanoTime();

            int j = 0;
            int i = r[j];
            while (v < (int) Math.floor((xi * b))) { // aqui começa o processo de eviction
//                System.out.println("v é " + v + " ratio of blocs discarted " + Math.floor((xi * b)));
                Block block = arr[i];
                System.out.println("============= O bloco é " + block.key + " =============");
                if (block == null) {
                    j++;
                    if (j == this.noRandoms) {
                        j = 0;
                    }

                    i = r[j];
                    continue;
                }
                block.setDegree(avg, currentRound);
//                System.out.println("O grau de saida do bloco é "+ block.getDegree());
                if (block.degree <= 0) {
                    System.out.println("descartou o bloco " + block.key);
                    arr[i] = null;
                    v++;
                } else {
//                    System.out.println("Não descartou o bloco, porem diminuiu o block.recNO " + block.recNo + " media eh( avg) " + avg + " chave do bloco é " + block.key);
                    block.recNo = block.recNo - avg;
                    System.out.println("Não descartou o bloco, porem block.key " + block.key + " possui bloco.recNO= " +block.recNo);
                }
                j++;
                if (j == this.noRandoms) {
                    j = 0;
                }
                i = r[j];
            }

            long stopTime = System.nanoTime();
            long elapsedTime = stopTime - startTime;
            this.occupied = this.occupied - ((int) Math.floor((xi * b)));

            currentRound++;
        }
        this.globalRecNo++;
        String key = rec.getBlockingKey(minHash);
        System.out.println("hash do registro => " + key + " id é "+ rec.id);

//        System.out.println("Tamanho do Bloco" + this.w);
        long startTime = System.nanoTime();
        boolean blockExists = false;
        int emptyPos = -1;
        for (int i = 0; i < arr.length; i++) {
//            System.out.println();
            Block block = arr[i];
            if (block != null) {
                if (block.key.equals(key)) {
                    System.out.println("colocou no bloco " + block.key);
                    int mp = block.put(rec, w, currentRound, writer);
                    this.matchingPairsNo = this.matchingPairsNo + mp;
                    blockExists = true;
                    break;
                }
            } else {
                emptyPos = i;
            }
        }
        if (!blockExists) {
            Block newBlock = new Block(key, this.q);
            System.out.println("bloco não existe e cria um novo bloco " + newBlock.key);
            this.occupied++;
//            System.out.println("numeros de blocos existentes:" + this.occupied);
            System.out.println("colocou no bloco " + newBlock.key);
            int mp = newBlock.put(rec, w, currentRound, writer);
            if (emptyPos != -1) {
                arr[emptyPos] = newBlock;
            } else {
                for (int i = 0; i < this.b; i++) {
                    if (arr[i] == null) {
                        arr[i] = newBlock;
                        break;
                    }
                }
            }
            this.matchingPairsNo = this.matchingPairsNo + mp;
        }
        long stopTime = System.nanoTime();
        long elapsedTime = stopTime - startTime;
        System.out.println("Saio do processo de put com occupied = " + this.occupied);
        System.out.println();
        System.out.println();
    }

    public static Record prepare(String[] lineInArray) {
        String name = lineInArray[2];
        String surname = lineInArray[1];
        String address = lineInArray[3];
        String town = lineInArray[4];
        String poBox = lineInArray[5];
        String id = lineInArray[0];
        Record rec = new Record();
        rec.id = id;
        rec.name = name;
        rec.surname = surname;
        rec.town = town;
        rec.poBox = poBox;
        rec.origin = id.charAt(0) + "";
        // System.out.println(id+" "+name+" "+surname+" "+town+" "+rec.origin);
        return rec;
    }

    public static void main(String[] args) {
        ExpBlock e = new ExpBlock(0.1, 2.0 / 3, 50);
        System.out.println("Running ExpBlock using b=" + e.b + " w=" + e.w);
        int recNoA = 0;
        int recNoB = 0;
        long startTime = System.currentTimeMillis();
        long startTimeCycle = System.currentTimeMillis();
        try {
//            CSVReader readerA = new CSVReader(new FileReader("/home/igor/mestrado/ExpBlock/target/test_voters_A.txt"));
//            CSVReader readerB = new CSVReader(new FileReader("/home/igor/mestrado/ExpBlock/target/test_voters_B.txt"));
            CSVReader readerA = new CSVReader(new FileReader("/home/igor/mestrado/ExpBlock/target/test_voters_A_10k.txt"));
            CSVReader readerB = new CSVReader(new FileReader("/home/igor/mestrado/ExpBlock/target/test_voters_B_10k.txt"));
            String[] lineInArray1;
            String[] lineInArray2;
            int c = 0;
            while (true) {
                lineInArray1 = readerA.readNext();
                if (lineInArray1 != null) {
                    if (lineInArray1.length == 6) {
                        String surname = lineInArray1[1];
                        recNoA++;
                        //System.out.println("Working on "+recNoA+" record from A.");
                        Record rec1 = prepare(lineInArray1);
                        e.put(rec1);
                    }
                }
                lineInArray2 = readerB.readNext();
                if (lineInArray2 != null) {
                    //String[] lineInArray2 = readerB.readNext();
                    if (lineInArray2.length == 6) {
                        String surname2 = lineInArray2[1];
                        recNoB++;
                        //System.out.println("Working on "+recNoB+" record from B.");
                        Record rec2 = prepare(lineInArray2);
                        e.put(rec2);
                    }
                }
                if ((recNoA + recNoB) % 30 == 0) {
                    long stopTimeCycle = System.currentTimeMillis();
                    long elapsedTime = stopTimeCycle - startTimeCycle;
                    System.out.println("====== processed " + (recNoA + recNoB) + " records in " + (elapsedTime / 1000) + " seconds.");
                    System.out.println("====== identified " + e.matchingPairsNo + " matching pairs.");
                    startTimeCycle = System.currentTimeMillis();
                }
                if ((lineInArray1 == null) && (lineInArray2 == null)) {
                    break;
                }
            }
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            e.writer.close();
            System.out.println(" ==================== elapsed time " + (elapsedTime / 1000) + " seconds.");
            System.out.println(" ==================== processed " + (recNoA + recNoB) + " records in total.");
            System.out.println(" ==================== processed " + (recNoA) + " records from A.");
            System.out.println(" ==================== processed " + (recNoB) + " records from B.");
            System.out.println(" ==================== identified " + e.matchingPairsNo + " in total. Recall = " + (e.matchingPairsNo * 1.0 / e.trulyMatchingPairsNo));
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                e.writer.close();
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }

        }
    }

}

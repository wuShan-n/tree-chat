package com.example.treechat.file;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 *
 * Created by wuShan on 2025/10/17
 */
public class FileDemo {
    public static void main(String[] args) throws IOException {
        //字节流
/*        try (FileWriter writer = new FileWriter("out.txt");) {
            writer.append("121323\n");
            writer.write("aidioeu\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader("out.txt"))) {
            String r;
            while ((r=reader.readLine())!=null){
                System.out.println(r);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
        //字符流
/*        try (FileOutputStream outputStream = new FileOutputStream("in.txt");){
            outputStream.write("hasdgjhas\n".getBytes());
            outputStream.write("1243423\n".getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (FileInputStream inputStream = new FileInputStream("in.txt");){
            int b;
            while ((b=inputStream.read())!=-1){
                System.out.print((char) b);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
        //缓冲流
/*        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream("out1.txt"));
             BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream("in.txt"));) {

            byte[] buff = new byte[1024];
            int bu;
            while ((bu = inputStream.read(buff)) != -1) {
                outputStream.write(buff, 0, bu);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/

        Path path = Paths.get("in.txt");

        Files.write(path,"大家好\n 我是就饿得\n sdashdj".getBytes());

        List<String> lines = Files.readAllLines(path);
        lines.forEach(System.out::println);


    }
}

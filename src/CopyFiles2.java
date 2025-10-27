import java.io.*;

public class CopyFiles2 {

    public static void main(String[] args) throws Exception {
        // Создаем тестовые файлы
        createTestFile("file1.txt", "Содержимое файла 1");
        createTestFile("file2.txt", "Содержимое файла 2");

        // Последовательное копирование
        long startTime = System.currentTimeMillis();

        copyFile("file1.txt", "copy1.txt");
        copyFile("file2.txt", "copy2.txt");

        long seqTime = System.currentTimeMillis() - startTime;
        System.out.println("Последовательное копирование: " + seqTime + " мс");

        // Параллельное копирование
        startTime = System.currentTimeMillis();

        Thread thread1 = new Thread(() -> {
            try {
                copyFile("file1.txt", "parallel1.txt");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                copyFile("file2.txt", "parallel2.txt");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        long parallelTime = System.currentTimeMillis() - startTime;
        System.out.println("Параллельное копирование: " + parallelTime + " мс");
    }

    // Метод для создания тестового файла
    private static void createTestFile(String filename, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        }
    }

    // Метод для копирования файла с использованием java.io
    private static void copyFile(String sourcePath, String destPath) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(sourcePath);
             FileOutputStream outputStream = new FileOutputStream(destPath)) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
}
import java.io.IOException;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CopyFiles2 {

    // Синхронное копирование с использованием FileChannel
    public static void syncCopyWithFileChannel(String source, String destination) throws IOException {
        Path sourcePath = Paths.get(source);
        Path destPath = Paths.get(destination);

        try (FileChannel sourceChannel = FileChannel.open(sourcePath, StandardOpenOption.READ);
             FileChannel destChannel = FileChannel.open(destPath,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.WRITE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {

            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }

    // Синхронное копирование с использованием Files.copy
    public static void syncCopyWithFiles(String source, String destination) throws IOException {
        Path sourcePath = Paths.get(source);
        Path destPath = Paths.get(destination);

        Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
    }

    // Асинхронное копирование с использованием CompletableFuture
    public static CompletableFuture<Void> asyncCopyWithCompletableFuture(String source, String destination) {
        return CompletableFuture.runAsync(() -> {
            try {
                syncCopyWithFileChannel(source, destination);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при асинхронном копировании", e);
            }
        });
    }

    // Асинхронное копирование с использованием AsynchronousFileChannel
    public static CompletableFuture<Void> asyncCopyWithAsyncChannel(String source, String destination) {
        return CompletableFuture.runAsync(() -> {
            try {
                copyWithAsyncChannel(source, destination);
            } catch (Exception e) {
                throw new RuntimeException("Ошибка при асинхронном копировании с каналом", e);
            }
        });
    }

    private static void copyWithAsyncChannel(String source, String destination)
            throws IOException, java.util.concurrent.ExecutionException, java.lang.InterruptedException {

        Path sourcePath = Paths.get(source);
        Path destPath = Paths.get(destination);

        AsynchronousFileChannel sourceChannel = AsynchronousFileChannel.open(
                sourcePath, StandardOpenOption.READ);
        AsynchronousFileChannel destChannel = AsynchronousFileChannel.open(
                destPath, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

        try {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            long position = 0;

            while (true) {
                Future<Integer> readResult = sourceChannel.read(buffer, position);
                int bytesRead = readResult.get();

                if (bytesRead == -1) {
                    break;
                }

                buffer.flip();
                Future<Integer> writeResult = destChannel.write(buffer, position);
                writeResult.get();

                position += bytesRead;
                buffer.clear();
            }
        } finally {
            sourceChannel.close();
            destChannel.close();
        }
    }

    public static void main(String[] args) throws Exception {
        // Создаем тестовые файлы
        createTestFiles();

        System.out.println("Синхронное копирование: ");

        // Тестируем синхронное копирование
        testSyncCopy();

        // Тестируем асинхронное копирование
        testAsyncCopy();

        // Тестируем параллельное асинхронное копирование
        testParallelAsyncCopy();

        // Очистка временных файлов
        cleanup();
    }

    private static void createTestFiles() throws IOException {
        // Создаем несколько тестовых файлов разного размера
        String content1 = "Содержимое файла 1 для тестирования NIO копирования\n".repeat(1000);
        String content2 = "Содержимое файла 2 для тестирования производительности\n".repeat(1500);
        String content3 = "Содержимое файла 3 для сравнения синхронного и асинхронного подхода\n".repeat(2000);

        Files.write(Paths.get("source1.txt"), content1.getBytes());
        Files.write(Paths.get("source2.txt"), content2.getBytes());
        Files.write(Paths.get("source3.txt"), content3.getBytes());

        System.out.println("Тестовые файлы созданы: source1.txt, source2.txt, source3.txt");
    }

    private static void testSyncCopy() throws IOException {
        System.out.println("Синхронное копирование: ");

        long startTime = System.currentTimeMillis();

        // Синхронное копирование с FileChannel
        syncCopyWithFileChannel("source1.txt", "sync_channel1.txt");
        syncCopyWithFileChannel("source2.txt", "sync_channel2.txt");
        syncCopyWithFileChannel("source3.txt", "sync_channel3.txt");

        long channelTime = System.currentTimeMillis() - startTime;
        System.out.println("FileChannel копирование: " + channelTime + " мс");

        startTime = System.currentTimeMillis();

        // Синхронное копирование с Files.copy
        syncCopyWithFiles("source1.txt", "sync_files1.txt");
        syncCopyWithFiles("source2.txt", "sync_files2.txt");
        syncCopyWithFiles("source3.txt", "sync_files3.txt");

        long filesTime = System.currentTimeMillis() - startTime;
        System.out.println("Files.copy копирование: " + filesTime + " мс");
    }

    private static void testAsyncCopy() throws Exception {
        System.out.println("Асинхронное копирование: ");

        long startTime = System.currentTimeMillis();

        // Асинхронное копирование с CompletableFuture
        CompletableFuture<Void> future1 = asyncCopyWithCompletableFuture("source1.txt", "async_future1.txt");
        CompletableFuture<Void> future2 = asyncCopyWithCompletableFuture("source2.txt", "async_future2.txt");
        CompletableFuture<Void> future3 = asyncCopyWithCompletableFuture("source3.txt", "async_future3.txt");

        // Ожидаем завершения всех задач
        CompletableFuture.allOf(future1, future2, future3).get();

        long futureTime = System.currentTimeMillis() - startTime;
        System.out.println("CompletableFuture копирование: " + futureTime + " мс");

        startTime = System.currentTimeMillis();

        // Асинхронное копирование с AsynchronousFileChannel
        CompletableFuture<Void> channelFuture1 = asyncCopyWithAsyncChannel("source1.txt", "async_channel1.txt");
        CompletableFuture<Void> channelFuture2 = asyncCopyWithAsyncChannel("source2.txt", "async_channel2.txt");
        CompletableFuture<Void> channelFuture3 = asyncCopyWithAsyncChannel("source3.txt", "async_channel3.txt");

        // Ожидаем завершения всех задач
        CompletableFuture.allOf(channelFuture1, channelFuture2, channelFuture3).get();

        long asyncChannelTime = System.currentTimeMillis() - startTime;
        System.out.println("AsynchronousFileChannel копирование: " + asyncChannelTime + " мс");
    }

    private static void testParallelAsyncCopy() throws Exception {
        System.out.println("Асинхронное копирование: ");

        // Создаем пул потоков для параллельного выполнения
        ExecutorService executor = Executors.newFixedThreadPool(3);

        long startTime = System.currentTimeMillis();

        CompletableFuture<Void>[] futures = new CompletableFuture[3];

        // Запускаем копирование в параллельных потоках
        futures[0] = CompletableFuture.runAsync(() -> {
            try {
                syncCopyWithFileChannel("source1.txt", "parallel1.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, executor);

        futures[1] = CompletableFuture.runAsync(() -> {
            try {
                syncCopyWithFileChannel("source2.txt", "parallel2.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, executor);

        futures[2] = CompletableFuture.runAsync(() -> {
            try {
                syncCopyWithFileChannel("source3.txt", "parallel3.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, executor);

        // Ожидаем завершения всех задач
        CompletableFuture.allOf(futures).get();

        long parallelTime = System.currentTimeMillis() - startTime;
        System.out.println("Параллельное асинхронное копирование: " + parallelTime + " мс");

        executor.shutdown();
    }

    private static void cleanup() throws IOException {
        // Удаляем созданные временные файлы
        String[] filesToDelete = {
                "source1.txt", "source2.txt", "source3.txt",
                "sync_channel1.txt", "sync_channel2.txt", "sync_channel3.txt",
                "sync_files1.txt", "sync_files2.txt", "sync_files3.txt",
                "async_future1.txt", "async_future2.txt", "async_future3.txt",
                "async_channel1.txt", "async_channel2.txt", "async_channel3.txt",
                "parallel1.txt", "parallel2.txt", "parallel3.txt"
        };

        for (String file : filesToDelete) {
            try {
                Files.deleteIfExists(Paths.get(file));
            } catch (IOException e) {
                System.out.println("Не удалось удалить файл: " + file);
            }
        }

        System.out.println("\nВременные файлы удалены");
    }
}
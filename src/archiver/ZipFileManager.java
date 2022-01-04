package archiver;

import archiver.exception.PathIsNotFoundException;
import archiver.exception.WrongZipFileException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipFileManager {
    // Полный путь zip файла
    private final Path zipFile;


    public ZipFileManager(Path zipFile) {
        this.zipFile = zipFile;
    }

    public void extractAll(Path outputFolder) throws Exception{
        if (!Files.isRegularFile(zipFile)){
            throw new WrongZipFileException();
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))){
            if (Files.notExists(outputFolder)){
                Files.createDirectories(outputFolder);
            }

            ZipEntry zipEntry = zipInputStream.getNextEntry();

            while (zipEntry != null){
                String name = zipEntry.getName();
                Path fullName = outputFolder.resolve(name);

                Path path = fullName.getParent();
                if (Files.notExists(path)){
                    Files.createDirectories(path);
                }
                try (OutputStream outputStream = Files.newOutputStream(fullName)) {
                    copyData(zipInputStream, outputStream);
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        }
    }

    public void removeFile(Path path) throws Exception{
        removeFiles(Collections.singletonList(path));
    }

    public void removeFiles(List<Path> pathList) throws Exception{
        if (!Files.isRegularFile(zipFile)) {
            throw new WrongZipFileException();
        }

        Path tempZipFile = Files.createTempFile(null,null);
        try (ZipOutputStream zipOutputStream =  new ZipOutputStream(Files.newOutputStream(tempZipFile))) {
            try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))){
                ZipEntry zipEntry = zipInputStream.getNextEntry();

                while (zipEntry != null){
                    Path archivedFile = Paths.get(zipEntry.getName());
                    if (!pathList.contains(archivedFile)){
                        String fileName = zipEntry.getName();
                        zipOutputStream.putNextEntry(new ZipEntry(fileName));

                        copyData(zipInputStream, zipOutputStream);

                        zipOutputStream.closeEntry();
                        zipInputStream.closeEntry();
                    } else {
                        ConsoleHelper.writeMessage("Удален файл - " + zipEntry.getName());
                    }

                    zipEntry = zipInputStream.getNextEntry();
                }
            }
        }

        Files.move(tempZipFile, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public void addFiles(List<Path> absolutePathList) throws Exception{
        if (!Files.isRegularFile(zipFile)) {
            throw new WrongZipFileException();
        }

        Path tempZipFile = Files.createTempFile(null,null);

        List<Path> names = new ArrayList<>();

        try (ZipOutputStream zipOutputStream =  new ZipOutputStream(Files.newOutputStream(tempZipFile))) {
            try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))){

                ZipEntry zipEntry = zipInputStream.getNextEntry();
                while (zipEntry != null){
                    String fileName = zipEntry.getName();
                    names.add(Paths.get(fileName));

                    zipOutputStream.putNextEntry(new ZipEntry(fileName));
                    copyData(zipInputStream, zipOutputStream);


                    zipOutputStream.closeEntry();
                    zipInputStream.closeEntry();

                    zipEntry = zipInputStream.getNextEntry();
                }
            }

            for (Path p : names){
                if (Files.isRegularFile(p)){
                    if (names.contains(p.getFileName())) {
                        ConsoleHelper.writeMessage(String.format("Файл - %s уже существует в архиве.", p.toString()));
                    } else {
                        addNewZipEntry(zipOutputStream, p.getParent(), p.getFileName());
                        ConsoleHelper.writeMessage(String.format("Файл - %s добавлен в архив.", p.toString()));
                    }
                } else {
                    throw new PathIsNotFoundException();
                }
            }
        }



        Files.move(tempZipFile, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public void addFile(Path absolutePath) throws Exception{
        addFiles(Collections.singletonList(absolutePath));
    }

    public List<FileProperties> getFilesList() throws Exception{
        if (!Files.isRegularFile(zipFile)){
            throw new WrongZipFileException();
        }

        List<FileProperties> filePropertiesList  = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                copyData(zipInputStream, byteArrayOutputStream);
                String name = zipEntry.getName();
                long size = zipEntry.getSize();
                long compressedSize = zipEntry.getCompressedSize();
                int method = zipEntry.getMethod();

                filePropertiesList.add(new FileProperties(name, size, compressedSize, method));
                zipEntry = zipInputStream.getNextEntry();
            }
        }
        return filePropertiesList;
    }

    public void createZip(Path source) throws Exception {
        // Проверяем, существует ли директория, где будет создаваться архив
        // При необходимости создаем ее
        Path zipDirectory = zipFile.getParent();
        if (Files.notExists(zipDirectory))
            Files.createDirectories(zipDirectory);

        // Создаем zip поток
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {

            if (Files.isDirectory(source)) {
                // Если архивируем директорию, то нужно получить список файлов в ней
                FileManager fileManager = new FileManager(source);
                List<Path> fileNames = fileManager.getFileList();

                // Добавляем каждый файл в архив
                for (Path fileName : fileNames)
                    addNewZipEntry(zipOutputStream, source, fileName);

            } else if (Files.isRegularFile(source)) {

                // Если архивируем отдельный файл, то нужно получить его директорию и имя
                addNewZipEntry(zipOutputStream, source.getParent(), source.getFileName());
            } else {

                // Если переданный source не директория и не файл, бросаем исключение
                throw new PathIsNotFoundException();
            }
        }
    }

    private void addNewZipEntry(ZipOutputStream zipOutputStream, Path filePath, Path fileName) throws Exception {
        Path fullPath = filePath.resolve(fileName);
        try (InputStream inputStream = Files.newInputStream(fullPath)) {
            ZipEntry entry = new ZipEntry(fileName.toString());

            zipOutputStream.putNextEntry(entry);

            copyData(inputStream, zipOutputStream);

            zipOutputStream.closeEntry();
        }
    }

    private void copyData(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8 * 1024];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }
}

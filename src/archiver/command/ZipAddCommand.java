package archiver.command;

import archiver.ConsoleHelper;
import archiver.ZipFileManager;
import archiver.exception.PathIsNotFoundException;

import java.nio.file.Paths;

public class ZipAddCommand extends ZipCommand {
    @Override
    public void execute() throws Exception {
        try {
            ConsoleHelper.writeMessage("Добавление файла в архив");

            ZipFileManager zipFileManager = getZipFileManager();
            ConsoleHelper.writeMessage("Введите полный путь файла:");
            zipFileManager.addFile(Paths.get(ConsoleHelper.readString()));
            ConsoleHelper.writeMessage("Добавление файла в архив завершено");
        } catch (PathIsNotFoundException e){
            ConsoleHelper.writeMessage("Файл не был найден");
        }

    }
}

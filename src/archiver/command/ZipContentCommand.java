package archiver.command;

import archiver.ConsoleHelper;
import archiver.FileProperties;
import archiver.ZipFileManager;


import java.util.List;


public class ZipContentCommand extends ZipCommand {
    @Override
    public void execute() throws Exception {
        ConsoleHelper.writeMessage("Просмотр содержимого архива.");
        ZipFileManager zipFileManager = getZipFileManager();
        ConsoleHelper.writeMessage("Содержимое архива:");
        List<FileProperties> list = zipFileManager.getFilesList();
        for (FileProperties f: list){
            ConsoleHelper.writeMessage(f.toString());
        }
        ConsoleHelper.writeMessage("Содержимое архива прочитано.");
    }
}

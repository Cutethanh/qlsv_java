package qlsv.data;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public interface DataHandler {
    <T extends Serializable> void save(List<T> items, String fileName) throws IOException;

    <T extends Serializable> List<T> load(String fileName, Class<T> type) throws IOException, IntegrityException;

    boolean exists(String fileName);

    boolean verify(String fileName);

    boolean restoreBackup(String fileName);
}

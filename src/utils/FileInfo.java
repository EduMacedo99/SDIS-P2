package src.utils;

import java.io.File;
import java.nio.file.Path;

public class FileInfo {
    public Path path; // path from where the file was backed up
    public int replication_degree; // desired replication degree

    public FileInfo(Path path, int replication_degree) {
        this.path = path;
        this.replication_degree = replication_degree;
    }

}
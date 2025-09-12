package fr.shom.ping.utils.survey;

import com.github.junrar.exception.RarException;
import fr.shom.ping.exception.ServiceException;
import java.io.IOException;
import java.nio.file.Path;

public interface IDecompressMethod {
    void decompress(Path source, Path target) throws ServiceException, IOException, RarException;
}

/*
 * Copyright 2007-2018 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.fulltext.batchloader;

import eu.europeana.fulltext.service.FTService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * Created by luthien on 19/07/2018.
 * this loader is deprecated, please use the LoadArchives one
 */

public class LoadFiles extends SimpleFileVisitor<Path> {

    private static final Logger      ERRLOG    = LogManager.getLogger("batcherror");
    private static final Logger      LOG       = LogManager.getLogger("batchloader");
    private FTService           ftService;
    private List<AnnoPageRdf>   apList = null;

    public LoadFiles(FTService ftService) {
        this.ftService = ftService;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) {
        apList = new ArrayList<>();
        LOG.info("Entering directory: " + dir.getFileName().toString() + " ...");
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (file.getFileName().toString().endsWith("xml")){
            apList.add(XMLXPathParser.eatIt(file));
            LOG.info("processed: " + file.getFileName().toString());
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        if (null != apList){
            if (!apList.isEmpty()){
                LOG.info("... batch " + dir.getFileName().toString() + " parsed, saving to MongoDB ...");
                ftService.saveAPList(apList);
                LOG.info("... done, leaving directory " + dir.getFileName().toString());
                apList = null;
            } else {
                LOG.info("... leaving directory " + dir.getFileName().toString());
            }
        }
        return CONTINUE;
    }

}

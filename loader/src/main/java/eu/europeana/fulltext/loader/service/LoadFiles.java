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

package eu.europeana.fulltext.loader.service;

import eu.europeana.fulltext.loader.model.AnnoPageRdf;
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
 * @deprecated  this loader is deprecated, please use the LoadArchives one
 */

@Deprecated
public class LoadFiles extends SimpleFileVisitor<Path> {

    private static final Logger      LOG       = LogManager.getLogger(LoadFiles.class);

    private MongoService ftService;
    private List<AnnoPageRdf>   apList = null;

    public LoadFiles(MongoService ftService) {
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
            LOG.debug("processed: " + file.getFileName().toString());
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        if (null != apList){
            if (!apList.isEmpty()){
                LOG.debug("... batch " + dir.getFileName().toString() + " parsed, saving to MongoDB ...");
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

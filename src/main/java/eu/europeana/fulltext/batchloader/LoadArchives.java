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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * Created by luthien on 26/07/2018.
 */
public class LoadArchives extends SimpleFileVisitor<Path> {

    private static final Logger      LOG       = LogManager.getLogger(LoadArchives.class);
    private static FTService         ftService;
    private static int               apCounter = 0;
    private static List<AnnoPageRdf> apList    = new ArrayList<>();

    public LoadArchives(FTService ftService) {
        this.ftService = ftService;
    }


    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
        if (path.getFileName().toString().endsWith("zip")){
            processArchive(path.toString());
        }
        return CONTINUE;
    }


    public static void processArchive(String path){
        System.out.println("processing archive: " + path);
        LOG.debug("processing archive: " + path);
        try (ZipFile archive = new ZipFile(path)){
            archive.stream()
                   .filter(p -> p.toString().contains(".xml"))
                   .filter(p -> !p.toString().startsWith("__"))
                   .forEach(p -> parseArchive(p, archive));
        }
        catch (Exception e){
            e.printStackTrace();
        }

        if (apCounter > 0){
            System.out.println("... remaining " + apCounter + " xml files parsed, flushing to MongoDB ...");
            LOG.debug("... remaining " + apCounter + " xml files parsed, flushing to MongoDB ...");
            ftService.saveAPList(apList);
            System.out.println("... done.");
            LOG.debug("... done.");
            apList = new ArrayList<>();
            apCounter = 0;
        }
        System.out.println("archive: " + path + " processed.");
        LOG.debug("archive:: " + path + " processed.");
    }

    private static void parseArchive(ZipEntry element, ZipFile archive){
        System.out.println("parsing file: " + element.toString());
        LOG.debug("parsing file: " + element.toString());
        try (InputStream  inputStream = archive.getInputStream(element)){
            StringWriter writer      = new StringWriter();
            IOUtils.copy(inputStream, writer, "UTF-8");
            apList.add(XMLXPathParser.eatIt(writer.toString(),
                                            StringUtils.removeEndIgnoreCase(element.toString(), ".xml")));
            apCounter ++;
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
        if (apCounter > 99){
            System.out.println("... 100 xml files parsed, flushing to MongoDB ...");
            LOG.debug("... 100 xml files parsed, flushing to MongoDB ...");
            ftService.saveAPList(apList);
            System.out.println("... done, continuing ...");
            LOG.debug("... done, continuing ...");
            apList = new ArrayList<>();
            apCounter = 0;
        }
    }


}

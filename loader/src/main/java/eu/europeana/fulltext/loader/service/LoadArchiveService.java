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

import eu.europeana.fulltext.loader.config.LoaderDefinitions;
import eu.europeana.fulltext.loader.config.LoaderSettings;
import eu.europeana.fulltext.loader.exception.ArchiveReadException;
import eu.europeana.fulltext.loader.exception.LoaderException;
import eu.europeana.fulltext.loader.model.AnnoPageRdf;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by luthien on 26/07/2018.
 */
@Service
public class LoadArchiveService extends SimpleFileVisitor<Path> {

    private static final Logger      LOG       = LogManager.getLogger(LoadArchiveService.class);

    private XMLParserService parser;
    private MongoService mongoService;
    private LoaderSettings settings;
    private int               apCounter = 0;
    private List<AnnoPageRdf> apList    = new ArrayList<>();

    public LoadArchiveService(XMLParserService parser, MongoService mongoService, LoaderSettings settings) {
        this.parser = parser;
        this.mongoService = mongoService;
        this.settings = settings;
    }

    /**
     * Load a single zip file (or all available zip files)
     * @param archive
     * @param saveMode
     * @return string containing summary of results (but only for a single zip, doesn't work for all archives yet)
     */
    public String importZipBatch(String archive, MongoSaveMode saveMode) throws ArchiveReadException {
        String batchBaseDirectory = settings.getBatchBaseDirectory();
        String zipBatchDir = StringUtils.removeEnd(batchBaseDirectory, "/") + "/";

        if (StringUtils.equalsIgnoreCase(archive, LoaderDefinitions.ALL_ARCHIVES)) {
            try {
                // TODO collect results for each zip and return that as result
                Files.walkFileTree(Paths.get(zipBatchDir), this);
            } catch (IOException e) {
                LogFile.OUT.error("I/O error occurred reading archives at: " + archive , e);
            }
        } else {
            return processArchive(zipBatchDir + archive, saveMode);
        }
        return "Finished";
    }

    public String importBatch(String directory, MongoSaveMode saveMode) {
        LoadFiles lf = new LoadFiles(parser, mongoService, saveMode, settings);
        String batchDir = settings.getBatchBaseDirectory()
                + (StringUtils.isNotBlank(directory) ? "/" + directory : "");
        try {
            // TODO collect results for each zip and return that as result
            Files.walkFileTree(Paths.get(batchDir), lf);
        } catch (IOException e) {
            LogFile.OUT.error("I/O error occurred reading contents of: " + directory , e);
        }
        return "Finished";
    }

    /**
     * Loads a zip file and starts processing it
     * @param path
     */
    public String processArchive(String path, MongoSaveMode saveMode) throws ArchiveReadException {
        LogFile.setFileName(path);
        LogFile.OUT.info("Processing archive {} with save mode {}", path, saveMode);

        ProgressLogger progressLog = new ProgressLogger(30);
        try (ZipFile archive = new ZipFile(path)) {

            // Note that normally size() returns the number of files AND folders in the zip, but for some reason for
            // newspaper archives it only returns the number of files, so no need to take folders into account
            long size = archive.size();
            LogFile.OUT.info("Archive has {} files", size);
            progressLog.setExpectedItems(size);

            archive.stream()
                    .filter(p -> p.toString().contains(".xml"))
                    .filter(p -> !p.toString().startsWith("__"))
                    .forEach(p -> parseArchiveFile(p, archive, progressLog, saveMode));

            if (apCounter > 0) {
                LOG.debug("... remaining {} xml files parsed, flushing to MongoDB ...", apCounter);
                mongoService.saveAPList(apList, saveMode);
                LOG.debug("... done.");
                apList = new ArrayList<>();
                apCounter = 0;
            }
        } catch (IOException  e) {
            LogFile.OUT.error("Unable to read archive {}", path, e);
            throw new ArchiveReadException("Unable to read archive "+path, e);
        }

        String results = progressLog.getResults();
        LogFile.OUT.info(results);
        return results;
    }

    private void parseArchiveFile(ZipEntry element, ZipFile archive, ProgressLogger progressLog, MongoSaveMode saveMode) {
        LOG.debug("Parsing file {} ", element.getName());
        try (InputStream  inputStream = archive.getInputStream(element);
            StringWriter writer      = new StringWriter()) {
            IOUtils.copy(inputStream, writer, "UTF-8");
            String pageId = element.getName();
            if (StringUtils.contains(element.toString(), "/")) {
                pageId = StringUtils.substringAfterLast(element.getName(), "/");
            }
            pageId = StringUtils.removeEndIgnoreCase(pageId, ".xml");

            AnnoPageRdf ap = parser.eatIt(element.getName(), writer.toString(), pageId);
            apList.add(ap);
            apCounter++;
            progressLog.addItemOk();
        }
        catch (IOException e){
            progressLog.addItemFail();
            LogFile.OUT.error("{} - Unable to read file: {}", element.getName(), getRootCauseMsg(e), e);
        }
        catch (LoaderException e) {
            progressLog.addItemFail();
            LogFile.OUT.error("{} - Unable to process file: {}", element.getName(), getRootCauseMsg(e), e);
        }

        if (apCounter > 99){
            LOG.debug("... 100 xml files parsed, flushing to MongoDB ...");
            mongoService.saveAPList(apList, saveMode);
            LOG.debug("... done, continuing ...");
            apList.clear();
            apCounter = 0;
        }
        LOG.debug("Done parsing file {} ", element.toString());
    }

    private String getRootCauseMsg(Throwable e) {
        String result = null;
        if (e != null) {
            if (e.getCause() == null) {
                result = e.getMessage();
            } else {
                result = getRootCauseMsg(e.getCause());
            }
        }
        return result;
    }

}

package eu.europeana.fulltext.loader.test;/*
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

import eu.europeana.fulltext.api.entity.AnnoPage;
import eu.europeana.fulltext.api.entity.Resource;
import eu.europeana.fulltext.api.repository.AnnoPageRepository;
import eu.europeana.fulltext.loader.config.LoaderSettings;
import eu.europeana.fulltext.loader.exception.LoaderException;
import eu.europeana.fulltext.loader.model.AnnoPageRdf;
import eu.europeana.fulltext.loader.service.MongoService;
import eu.europeana.fulltext.loader.service.XMLParserService;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * Created by luthien on 17/09/2018.
 */
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:loader-test.properties")
@DataMongoTest
public class MongoIntegrationTest {

    private static AnnoPageRdf apRdf1;

    @Autowired
    private LoaderSettings     settings;
    @Autowired
    private MongoService       mongoService;
    @Autowired
    private AnnoPageRepository annoPageRepository;



    @Before
    public void loadExampleFiles() throws LoaderException, IOException {
        XMLParserService parser = new XMLParserService(settings);
        // this example file contains an image entity with special characters (e.g. &apos;), plus 78 annotations
        String file1 = "9200396-BibliographicResource_3000118435009-1.xml";
        apRdf1 = parser.eatIt(file1, loadXmlFile(file1), "1");
    }

    private String loadXmlFile(String fileName) throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            if (is != null) {
                return IOUtils.toString(is);
            }
            throw new FileNotFoundException(fileName);
        }
    }

    @Test
    public void test() {
        Resource res01  = new Resource("res01", "nl", "Er ligt tussen Regge en Dinkel een land");
        AnnoPage ap_in  = mongoService.createAnnoPage(apRdf1, res01);
        AnnoPage ap_out = annoPageRepository.save(ap_in);
        assertEquals(ap_out.getPgId(), ap_in.getPgId());

    }
}

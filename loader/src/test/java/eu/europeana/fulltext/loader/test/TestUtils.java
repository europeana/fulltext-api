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

package eu.europeana.fulltext.loader.test;

import eu.europeana.fulltext.loader.LoaderApplication;
import eu.europeana.fulltext.loader.config.LoaderSettings;
import eu.europeana.fulltext.loader.exception.LoaderException;
import eu.europeana.fulltext.loader.model.AnnoPageRdf;
import eu.europeana.fulltext.loader.service.MongoService;
import eu.europeana.fulltext.loader.service.XMLParserService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by luthien on 19/09/2018.
 */
//@Service
public class TestUtils {

    private XMLParserService parser;

    public TestUtils(XMLParserService parser){
        this.parser = parser;
    }

    public AnnoPageRdf parseXmlFile(String fileName, String pageId) throws LoaderException, IOException {
//        XMLParserService parser = new XMLParserService(settings);
        return parser.eatIt(fileName, loadXmlFile(fileName), pageId);
    }

    static String loadXmlFile(String fileName) throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            if (is != null) {
                return IOUtils.toString(is);
            }
            throw new FileNotFoundException(fileName);
        }
    }

}

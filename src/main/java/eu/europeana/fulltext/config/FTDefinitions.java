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

package eu.europeana.fulltext.config;

/**
 * Created by luthien on 14/06/2018.
 */
public class FTDefinitions {

    /**
     * Media type for json-ld
     */
    public static final String MEDIA_TYPE_JSONLD = "application/ld+json";
    public static final String IIIFURL = "https://iiif.europeana.eu/";
    public static final String FULLTEXTURL = "https://www.europeana.eu/api/fulltext";

    /**
     * Media type for IIIF version 2
     */
    public static final String MEDIA_TYPE_IIIF_V2 = "http://iiif.io/api/presentation/2/context.json";

    /**
     * Media type for IIIF version 3
     */
    public static final String MEDIA_TYPE_IIIF_V3 = "http://iiif.io/api/presentation/3/context.json";

    /**
     * Default Content-type returned on manifest requests for version 3
     */
    public static final String MEDIA_TYPE_IIIF_JSONLD_V3 = MEDIA_TYPE_JSONLD + ";profile=\""+MEDIA_TYPE_IIIF_V3+"\"";

    /**
     * Default Content-type returned on manifest requests for version 2
     */
    public static final String MEDIA_TYPE_IIIF_JSONLD_V2 = MEDIA_TYPE_JSONLD + ";profile=\""+MEDIA_TYPE_IIIF_V2+"\"";


}

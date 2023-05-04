/**
 *
 */
package eu.europeana.fulltext.alto.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 24 Dec 2018
 *
 * Fix bad language codes or language codes that have been deprecated with the 
 * correct ones
 */
public class LanguageCorrector {
    private static final Map<String, String> CORRECTIONS = load(new HashMap());

    private static Map<String, String> load(Map<String, String> map) {
        map.put("ji", "yi");
        map.put("jy", "yi");
        return map;
    }

    public String correct(String lang) {
        if (lang == null) {
            return lang;
        }

        String ret = CORRECTIONS.get(lang);
        return (ret == null ? lang : ret);
    }
}
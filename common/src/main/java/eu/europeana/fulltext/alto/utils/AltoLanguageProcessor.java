/**
 *
 */
package eu.europeana.fulltext.alto.utils;

import eu.europeana.fulltext.alto.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 19 Dec 2018
 *
 * Determines the most predominant language in the ALTO file based on the length
 * of the words that are language qualified
 */
public class AltoLanguageProcessor implements AltoPageProcessor {
    private static final Logger LOG = LogManager.getLogger(AltoLanguageProcessor.class);
    private static final DecimalFormat FORMAT = new DecimalFormat("###.00");
    private final Map<String, Integer> _langs = new LinkedHashMap();
    private final boolean _acceptNull;

    public AltoLanguageProcessor() {
        this(false);
    }

    public AltoLanguageProcessor(boolean acceptNull) {
        _acceptNull = acceptNull;
    }

    public String getDefaultLanguage(AltoPage page) {
        try {
            for (TextBlock tb : page) {
                detect(tb, tb.getLanguage());
            }

            Stat stat = getPredominantLang(_acceptNull);
            return (stat == null ? null : stat.lang);
        } finally {
            _langs.clear();
        }
    }

    public void apply(AltoPage page, String lang) {
        if (lang == null) {
            return;
        }

        page.setLanguage(lang);
        update(page, lang);
    }

    public void process(AltoPage page) {
        String lang = getDefaultLanguage(page);
        apply(page, lang);
    }


    private void detect(TextBlock block, String def) {
        for (TextNode<?> tn : block) {
            String lang = getLang(tn.getLanguage(), def);
            if (tn instanceof TextBlock) {
                detect((TextBlock) tn, lang);
            } else if (tn instanceof TextLine) {
                detect((TextLine) tn, lang);
            }
        }
    }

    private void detect(TextLine line, String def) {
        for (TextElement te : line) {
            if (!(te instanceof TextString)) {
                continue;
            }

            TextString word = (TextString) te;
            String lang = getLang(word.getLanguage(), def);
            account(word.getText().length(), lang);
        }
    }

    private String getLang(String lang, String def) {
        return (lang != null ? lang : def);
    }

    private void account(int length, String lang) {
        Integer i = _langs.get(lang);
        _langs.put(lang, i == null ? length : length + i);
    }


    private Stat getPredominantLang(boolean acceptNull) {
        if (_langs.isEmpty()) {
            return null;
        }

        //int total = 0;
        TreeSet<Stat> set = new TreeSet();
        for (Map.Entry<String, Integer> entry : _langs.entrySet()) {
            int length = entry.getValue();
            set.add(new Stat(entry.getKey(), length));
            //total += length;
        }

        if (acceptNull) {
            return set.first();
        }

        Stat chosen = set.pollFirst();
        if (chosen.lang != null) {
            return chosen;
        }
        if (set.isEmpty()) {
            return null;
        }

        Stat chosen2 = set.first();
        logLanguageChange(chosen, chosen2, set);
        return chosen2;
    }
    
    /*
    private Stat getPredominantLang()
    {
        Stat chosen = new Stat();
        if ( _langs.isEmpty() ) { return chosen; }

        for ( String lang : _langs.keySet() )
        {
            int c = _langs.get(lang);
            chosen.total += c;

            if ( chosen.length >= c           ) { continue; }
            if (lang == null && !_acceptNull  ) { continue; }

            chosen.lang   = lang;
            chosen.length = c;
        }

        //only for logging purposes
        if ( !_acceptNull )
        {
            int count = _langs.get(null);
            if ( count > chosen.length ) { logLanguageChange(ret); }
        }
        return chosen;
    }

    private Stat getPredominantLang(boolean first)
    {
        Stat chosen = new Stat();
        for ( String lang : _langs.keySet() )
        {
            int c = _langs.get(lang);
            chosen.total += c;

            if ( chosen.length >= c   ) { continue; }
            if (!first && lang == null) { continue; }

            chosen.lang   = lang;
            chosen.length = c;
        }

        if ( chosen.lang == null && !_acceptNull )
        {
            Stat ret = getPredominantLang(false);
            logLanguageChange(ret);
            return ret;
        }
        return chosen;
    }
    */

    private void update(TextNode<? extends TextElement> node, String def) {
        for (TextElement e : node) {
            if (e instanceof LanguageElement) {
                LanguageElement le = (LanguageElement) e;
                String lang = le.getLanguage();
                if (def.equals(lang)) {
                    le.setLanguage(null);
                }
            }

            if (e instanceof TextNode) {
                update((TextNode) e, def);
            }
        }
    }

    private void logLanguageChange(Stat cNull, Stat c, Set<Stat> set) {
        LOG.warn("Most predominate language was null(" + cNull.length + ")"
                + " but was replaced by " + c.lang + " => " + set);

    }

    private class Stat implements Comparable<Stat> {
        public String lang = null;
        public int length = 0;

        public Stat(String lang, int length) {
            this.lang = lang;
            this.length = length;
        }

        public int compareTo(Stat s) {
            return (s.length - this.length);
        }

        public boolean equals(Object o) {
            return StringUtils.equals(this.lang, ((Stat) o).lang);
        }

        public String toString() {
            return (this.lang + ":" + this.length);
        }
    }
}

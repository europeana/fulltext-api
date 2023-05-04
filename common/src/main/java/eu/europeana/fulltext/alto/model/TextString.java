/**
 *
 */
package eu.europeana.fulltext.alto.model;

import eu.europeana.edm.media.ImageBoundary;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 17 Jun 2018
 */
public class TextString implements StyledTextElement, LanguageElement {
    private String _lang;
    private final String _text;
    private final ImageBoundary _ib;
    private Float _confidence;
    private TextStyle _style;
    private final boolean _correction;
    private SubstitutionHyphen _subs;

    public TextString(String text, SubstitutionHyphen subs, String lang
            , ImageBoundary ib, Float confidence, TextStyle style
            , boolean correction) {
        _text = text;
        _subs = subs;
        _lang = lang;
        _ib = ib;
        _confidence = confidence;
        _style = style;
        _correction = correction;
    }

    public String getText() {
        return _text;
    }

    public SubstitutionHyphen getSubs() {
        return _subs;
    }

    public void setSubs(SubstitutionHyphen subs) {
        _subs = subs;
    }

    public String getLanguage() {
        return _lang;
    }

    public void setLanguage(String lang) {
        _lang = lang;
    }

    public ImageBoundary getImageBoundary() {
        return _ib;
    }

    public Float getConfidence() {
        return _confidence;
    }

    public void setConfidence(Float confidence) {
        _confidence = confidence;
    }

    public TextStyle getStyle() {
        return _style;
    }

    public void setStyle(TextStyle style) {
        _style = style;
    }

    public boolean getCorrection() {
        return _correction;
    }

    public boolean hasSubs() {
        return (_subs != null);
    }

    public void visit(AltoVisitor visitor) {
        visitor.visit(this);
    }
}
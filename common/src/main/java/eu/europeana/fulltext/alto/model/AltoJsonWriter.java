package eu.europeana.fulltext.alto.model;

import eu.europeana.fulltext.alto.model.TextStyle.TextType;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintStream;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 23 Jun 2018
 */
public class AltoJsonWriter extends AbsAltoVisitor {
    private PrintStream _ps;

    public void print(AltoPage page, PrintStream ps) {
        _ps = ps;
        visit(page);
    }

    public void visit(AltoPage page) {
        printNode(page);
    }

    public void visit(TextStyle style) {
    }

    public void visit(TextBlock block) {
        printNode(block);
    }

    public void visit(TextLine line) {
        printNode(line);
    }

    public void visit(TextString word) {
        _ps.print("{");
        _ps.print("\"text\":");
        printStr(word.getText());

        String lang = word.getLanguage();
        if (lang != null) {
            _ps.print(",\"lang\":\"" + lang + "\"");
        }

        printStype(word.getStyle());

        Float confidence = word.getConfidence();
        if (confidence != null) {
            _ps.print(",\"confidence\":" + confidence);
        }

        SubstitutionHyphen subs = word.getSubs();
        if (subs != null) {
            _ps.print(",\"subs\": {");
            _ps.print("\"text\":");
            printStr(subs.getSubsText());
            _ps.print("}");
        }
        _ps.print("}");
    }

    public void visit(TextSpace space) {
        _ps.print("{\"space\":null}");
    }

    public void visit(TextHyphen hyphen) {
        _ps.print("null");
    }

    private void printStype(TextStyle style) {
        _ps.print(",\"style\": {");

        Float size = style.getSize();
        if (size != null) {
            _ps.print("\"size\":" + size);
        }

        _ps.print(",\"type\":[");
        boolean first = true;
        for (TextType type : style.getTypes()) {
            if (first) {
                first = false;
            } else {
                _ps.print(",");
            }
            _ps.print("\"" + type + "\"");
        }
        _ps.print("]");

        _ps.print("}");
    }

    private void printNode(TextNode<? extends TextElement> node) {
        _ps.print("[");
        boolean first = true;
        for (TextElement e : node) {
            if (first) {
                first = false;
            } else {
                _ps.print(',');
            }
            e.visit(this);
        }
        _ps.print("]");
    }

    private void printStr(String str) {
        _ps.print("\"");
        _ps.print(StringUtils.replace(str, "\"", "\\\""));
        _ps.print("\"");
    }
}

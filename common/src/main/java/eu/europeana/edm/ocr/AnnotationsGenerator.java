package eu.europeana.edm.ocr;

import eu.europeana.edm.FullTextAnnotation;
import eu.europeana.edm.FullTextPackage;
import eu.europeana.edm.media.MediaReference;
import eu.europeana.edm.text.FullTextResource;
import eu.europeana.edm.text.TextBoundary;
import eu.europeana.edm.text.TextReference;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.alto.model.*;

import static eu.europeana.fulltext.alto.parser.EDMFullTextUtils.newTextBoundary;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 22 Jun 2018
 */
public class AnnotationsGenerator extends AbsAltoVisitor {

    private StringBuilder sb = new StringBuilder(100 * 1024);
    private FullTextPackage page;


    /**
     * Process the alto page to Fulltext package
     * @param altoPage
     * @param ref
     * @return
     */
    public synchronized FullTextPackage process(AltoPage altoPage, MediaReference ref) {
        try {

            FullTextResource resource = new FullTextResource();

            // add first annotation of type PAGE - this will not have any targets or text boundary
            TextReference tb = new TextBoundary(resource);
            page = new FullTextPackage(null, resource);
            page.add(new FullTextAnnotation(null, tb, null, AnnotationType.PAGE, null, null));

            // add resource value and other annotations
            visit(altoPage);
            resource.setValue(sb.toString());
            resource.setLang(altoPage.getLanguage());
            return page;
        } finally {
            sb.setLength(0);
        }
    }

    /**
     * Interface AltoVisitor
     */
    public void visit(TextBlock block) {
        if (hasText()) {
            newLine();
        }

        int s = sb.length();
        super.visit(block);
        TextBoundary tb = newTextBoundary(page.getResource(), s, sb.length());

        if (!block.hasLines()) {
            return;
        }

        // add BLOCK type annotations
        page.add(new FullTextAnnotation(null, tb, block.getImageBoundary()
                , AnnotationType.BLOCK, block.getLanguage()
                , null));
    }

    public void visit(TextLine line) {
        if (!endsWith(' ', '-', '\n') && hasText()) {
            newSpace();
        }

        int s = sb.length();
        super.visit(line);
        TextBoundary tb = newTextBoundary(page.getResource(), s, sb.length());
        page.add(new FullTextAnnotation(null, tb, line.getImageBoundary()
                , AnnotationType.LINE, line.getLanguage()
                , null));
    }

    public void visit(TextString word) {
        int s = sb.length();
        if (!word.hasSubs() || word.getSubs().getWord2() == null) {
            //TODO: Check whether it is a word or line (check for the existance of a space but only if there is only one TextString in the TextLine)
            sb.append(word.getText());
            TextBoundary tb = newTextBoundary(page.getResource(), s, sb.length());
            page.add(new FullTextAnnotation(null, tb, word.getImageBoundary()
                    , AnnotationType.WORD, word.getLanguage()
                    , word.getConfidence()));
            return;
        }

        SubstitutionHyphen subs = word.getSubs();
        TextString word2 = subs.getWord2();
        if (word2 == word) {
            return;
        }

        sb.append(subs.getSubsText());
        TextBoundary tb = newTextBoundary(page.getResource(), s, sb.length());

        Float confidence = getConfidence(word.getConfidence()
                , word2.getConfidence());

        page.add(new FullTextAnnotation(null, tb
                , word.getImageBoundary()
                , word2.getImageBoundary(), AnnotationType.WORD
                , word.getLanguage(), confidence));
    }

    public void visit(TextSpace space) {
        newSpace();
    }

    // TODO what does this do - empty implementation
    public void visit(TextHyphen hyphen) {
    }

    private Float getConfidence(Float c1, Float c2) {
        if (c1 == null) {
            return c2;
        }
        if (c2 == null) {
            return c1;
        }
        return ((c1 + c2) / 2f);
    }

    private boolean endsWith(char... chars) {
        int len = sb.length();
        if (len <= 0) {
            return false;
        }

        int c1 = sb.charAt(len - 1);
        for (char c2 : chars) {
            if (c1 == c2) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText() {
        return (sb.length() > 0);
    }

    private void newSpace() {
        sb.append(' ');
    }

    private void newLine() {
        sb.append('\n');
    }
}

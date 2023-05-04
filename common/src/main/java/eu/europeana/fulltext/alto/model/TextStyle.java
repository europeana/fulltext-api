/**
 *
 */
package eu.europeana.fulltext.alto.model;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 22 Jun 2018
 */
public class TextStyle implements TextElement {
    private Float _size;

    private final Collection<TextType> _types;
    public TextStyle() {
        this((Float) null);
    }

    public TextStyle(TextStyle style) {
        this(style.getSize(), new LinkedHashSet(style.getTypes()));
    }

    public TextStyle(Float size) {
        this(size, new LinkedHashSet(1));
    }

    public TextStyle(Float size, Collection<TextType> types) {
        _size = size;
        _types = types;
    }

    public TextStyle(Float size, TextType... types) {
        _size = size;
        _types = new LinkedHashSet<TextType>(types.length);
        for (TextType type : types) {
            _types.add(type);
        }
    }

    public Float getSize() {
        return _size;
    }

    public void setSize(Float size) {
        _size = size;
    }

    public Collection<TextType> getTypes() {
        return _types;
    }

    public void addType(TextType type) {
        _types.add(type);
    }

    public void copyStyle(TextStyle style) {
        if (style._size != null) {
            _size = style._size;
        }
        _types.addAll(style.getTypes());
    }

    public void visit(AltoVisitor visitor) {
        visitor.visit(this);
    }

    public enum TextType {
        bold,
        italics,
        subscript,
        superscript,
        smallcaps,
        underline
    }
}

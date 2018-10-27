package com.pinktwins.elephant;

import javax.swing.text.AbstractDocument;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

// The following classes are based on https://stackoverflow.com/a/13375811.

class WrappedEditorKit extends StyledEditorKit {
    public ViewFactory getViewFactory() {
    	return new WrapColumnFactory();
    }
}

class WrapColumnFactory implements ViewFactory {	
    public View create(Element elem) {
        switch (elem.getName()) {
        case AbstractDocument.ContentElementName:
        	return new WrapLabelView(elem);
        case AbstractDocument.ParagraphElementName:
        	return new ParagraphView(elem);
        case AbstractDocument.SectionElementName:
        	return new BoxView(elem, View.Y_AXIS);
        case StyleConstants.ComponentElementName:
        	return new ComponentView(elem);
        case StyleConstants.IconElementName:
        	return new IconView(elem);
        default:
        	return new LabelView(elem);
        }
    }
}

class WrapLabelView extends LabelView {
    public WrapLabelView(Element elem) {
        super(elem);
    }

    public float getMinimumSpan(int axis) {
        switch (axis) {
            case View.X_AXIS:
                return 0;
            case View.Y_AXIS:
                return super.getMinimumSpan(axis);
            default:
                throw new IllegalArgumentException("Invalid axis: " + axis);
        }
    }
}

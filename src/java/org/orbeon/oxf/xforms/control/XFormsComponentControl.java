/**
 *  Copyright (C) 2008 Orbeon, Inc.
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version
 *  2.1 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.xforms.control;

import org.orbeon.oxf.xforms.XFormsContainingDocument;
import org.dom4j.Element;

/**
 * Control that represents a custom components.
 */
public class XFormsComponentControl extends XFormsSingleNodeControl {

    public XFormsComponentControl(XFormsContainingDocument containingDocument, XFormsControl parent, Element element, String name, String effectiveId) {
        super(containingDocument, parent, element, name, effectiveId);
    }
}

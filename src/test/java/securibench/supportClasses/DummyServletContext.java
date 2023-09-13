/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package securibench.supportClasses;

import java.util.*;

import javax.servlet.*;

public class DummyServletContext implements ServletContext {
    private final Map<String, String> mInitParameters = new HashMap<>();

    @Override
    public String getInitParameter(String name) {
        return mInitParameters.get(name);
    }

    @Override
    public Enumeration getInitParameterNames() {
        return new EnumerationImpl(mInitParameters.values().toArray(new String[0]));
    }
}

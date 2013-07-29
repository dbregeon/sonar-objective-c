/*
 * Sonar Objective-C Plugin
 * Copyright (C) 2012 François Helg, Cyril Picat and OCTO Technology
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.objectivec;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sonar.squid.api.SquidConfiguration;

public class ObjectiveCConfiguration extends SquidConfiguration {

    private boolean ignoreHeaderComments;
    private List<String> includeDirectories = new ArrayList<String>();
    private String baseDir;

    public ObjectiveCConfiguration() {
    }

    public ObjectiveCConfiguration(final Charset charset) {
        super(charset);
    }

    public void setIgnoreHeaderComments(final boolean ignoreHeaderComments) {
        this.ignoreHeaderComments = ignoreHeaderComments;
    }

    public boolean getIgnoreHeaderComments() {
        return ignoreHeaderComments;
    }

    public void setIncludeDirectories(final List<String> directories) {
        includeDirectories = directories;
      }

      public void setIncludeDirectories(final String[] includeDirectories) {
        if (includeDirectories != null) {
          setIncludeDirectories(Arrays.asList(includeDirectories));
        }
      }

      public List<String> getIncludeDirectories() {
        return includeDirectories;
      }

      public void setBaseDir(final String baseDir) {
        this.baseDir = baseDir;
      }

      public String getBaseDir() {
        return baseDir;
      }

}

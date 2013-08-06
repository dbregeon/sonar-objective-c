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
package org.sonar.objectivec.preprocessor;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.objectivec.ObjectiveCConfiguration;

/**
 * The source code provider is responsible for locating source files and getting
 * their content. A source file can be specified both as an absolute and as a
 * relative file system path. In the latter case the scanner searches a list of
 * directories (known to him) for a file with such a name.
 */
public final class SourceCodeProvider {
    private final List<File> includeRoots = new LinkedList<File>();
    private final File baseDir;
    public static final Logger LOG = LoggerFactory
            .getLogger("SourceCodeProvider");

    public SourceCodeProvider(final ObjectiveCConfiguration conf) {
        baseDir = new File(conf.getBaseDir());
        setIncludeRoots(conf.getIncludeDirectories());

    }

    private void setIncludeRoots(final List<String> roots) {
        for (final String tmp : roots) {

            File includeRoot = new File(tmp);
            if (!includeRoot.isAbsolute()) {
                includeRoot = new File(baseDir, tmp);
            }

            try {
                includeRoot = includeRoot.getCanonicalFile();
            } catch (final java.io.IOException io) {
                LOG.error("cannot get canonical form of: '{}'", includeRoot);
            }

            if (includeRoot.isDirectory()) {
                LOG.debug("storing include root: '{}'", includeRoot);
                this.includeRoots.add(includeRoot);
            } else {
                LOG.warn("the include root {} doesnt exist",
                        includeRoot.getAbsolutePath());
            }
        }
    }

    public File getSourceCodeFile(final String filename, final boolean quoted) {
        File result = null;
        final File file = new File(filename);
        if (file.isAbsolute()) {
            if (file.isFile()) {
                result = file;
            }
        } else {
            // This seems to be an established convention:
            // The special behavior in the quoted case is to look up relative to
            // the
            // current directory.
            if (quoted) {
                final List<File> directories = new ArrayList<File>();
                final FileFilter directoryFilter = new FileFilter() {
                    public boolean accept(final File f) {
                        return f.isDirectory();
                    }
                };
                directories.add(baseDir);
                while (null == result && !directories.isEmpty()) {
                    final File currentParent = directories.remove(0);
                    final File abspath = new File(currentParent, file.getPath());
                    if (abspath.isFile()) {
                        result = abspath;
                    } else {
                        final File[] subDirectories = currentParent.listFiles(directoryFilter);
                        if (null != subDirectories) {
                            directories.addAll(Arrays.asList(subDirectories));
                        }
                    }
                }
            }

            // The standard behavior: lookup relative to to the include roots.
            // The quoted case falls back to this, if its special handling wasnt
            // successul (as forced by the Standard).
            if (result == null) {
                for (final File folder : includeRoots) {
                    final File abspath = new File(folder.getAbsolutePath(), filename);
                    if (abspath.isFile()) {
                        result = abspath;
                        break;
                    }
                }
            }
        }

        if (result != null) {
            try {
                result = result.getCanonicalFile();
            } catch (final java.io.IOException io) {
                LOG.error("cannot get canonical form of: '{}'", result);
            }
        }

        return result;
    }

    public String getSourceCode(final File file) {
        String code = null;
        if (file.isFile()) {
            try {
                code = FileUtils.readFileToString(file);
            } catch (final java.io.IOException e) {
                LOG.error("Cannot read contents of the file '{}'", file);
            }
        }

        return code;
    }
}
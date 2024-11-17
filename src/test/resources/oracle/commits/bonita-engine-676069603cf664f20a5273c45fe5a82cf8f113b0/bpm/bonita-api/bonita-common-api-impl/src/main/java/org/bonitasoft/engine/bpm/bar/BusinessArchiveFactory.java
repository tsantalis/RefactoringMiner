/**
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.bpm.bar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.bonitasoft.engine.io.IOUtil;

/**
 * Read or write {@link BusinessArchive} from/to file system
 *
 * @author Baptiste Mesta
 */
public class BusinessArchiveFactory {

    private static final List<BusinessArchiveContribution> contributions;

    static {
        contributions = new ArrayList<BusinessArchiveContribution>();
        contributions.add(new ProcessDefinitionBARContribution());
        contributions.add(new ParameterContribution());
        contributions.add(new ConnectorContribution());
        contributions.add(new ExternalResourceContribution());
        contributions.add(new ActorMappingContribution());
        contributions.add(new UserFilterContribution());
        contributions.add(new DocumentsResourcesContribution());
        contributions.add(new ClasspathContribution());
        contributions.add(new FormMappingContribution());
    }

    /**
     * Create a business archive from an {@link InputStream}
     *
     * @param inputStream
     * @return
     *         the business archived that was in the input stream
     * @throws IOException
     *         in case of issue reading/writing on file system
     * @throws InvalidBusinessArchiveFormatException
     *         if the inpu stream does not contains a valide business archive
     */
    public static BusinessArchive readBusinessArchive(final InputStream inputStream) throws IOException, InvalidBusinessArchiveFormatException {
        File barFolder = null;

        try {
            barFolder = IOUtil.createTempDirectoryInDefaultTempDirectory("tempBarFolder");
            IOUtil.unzipToFolder(inputStream, barFolder);

            return getBusinessArchive(barFolder);
        } catch (final InvalidBusinessArchiveFormatException e) {
            throw e;
        } catch (final Exception e) {
            throw new InvalidBusinessArchiveFormatException("Invalid format, can't read the BAR file", e);
        } finally {
            IOUtil.deleteDir(barFolder);
        }
    }

    /**
     * Create a business archive from a valid file or folder
     *
     * @param barOrFolder
     *        the folder or file that contains the business archive to read
     * @return
     *         the business archived that was in the file or folder
     * @throws IOException
     *         in case of issue reading/writing on file system
     * @throws InvalidBusinessArchiveFormatException
     *         if the input stream does not contains a valid business archive
     */
    public static BusinessArchive readBusinessArchive(final File barOrFolder) throws InvalidBusinessArchiveFormatException, IOException {
        if (!barOrFolder.exists()) {
            throw new FileNotFoundException("the file does not exists: " + barOrFolder.getAbsolutePath());
        }
        if (barOrFolder.isDirectory()) {
            return getBusinessArchive(barOrFolder);
        }

        final FileInputStream inputStream = new FileInputStream(barOrFolder);
        try {
            return readBusinessArchive(inputStream);
        } finally {
            inputStream.close();
        }
    }

    private static BusinessArchive getBusinessArchive(final File barFolder) throws IOException, InvalidBusinessArchiveFormatException {
        final BusinessArchive businessArchive = new BusinessArchive();
        for (final BusinessArchiveContribution contribution : contributions) {
            if (!contribution.readFromBarFolder(businessArchive, barFolder) && contribution.isMandatory()) {
                throw new InvalidBusinessArchiveFormatException("Invalid format, can't read '" + contribution.getName() + "' from the BAR file");
            }
        }
        return businessArchive;
    }

    /**
     * Write the {@link BusinessArchive} to the folder given in parameter
     * <p>
     * the written business archive the uncompressed version of {@link #writeBusinessArchiveToFile(BusinessArchive, File)}
     *
     * @param businessArchive
     *        the {@link BusinessArchive} to write
     * @param folderPath
     *        the folder into the business archive must be written
     * @throws IOException
     */
    public static void writeBusinessArchiveToFolder(final BusinessArchive businessArchive, final File folderPath) throws IOException {
        if (folderPath.exists()) {
            if (!folderPath.isDirectory()) {
                throw new IOException("unable to create Business archive on a file " + folderPath);
            }
        } else {
            folderPath.mkdirs();
        }
        for (final BusinessArchiveContribution contribution : contributions) {
            contribution.saveToBarFolder(businessArchive, folderPath);
        }
    }

    /**
     * Write the {@link BusinessArchive} to the .bar file given in parameter.
     * <p>
     * this file can then be read using {@link #readBusinessArchive(File)}
     *
     * @param businessArchive
     *        the {@link BusinessArchive} to write
     * @param businessArchiveFile
     *        the folder into the business archive must be written
     * @throws IOException
     */
    public static void writeBusinessArchiveToFile(final BusinessArchive businessArchive, final File businessArchiveFile) throws IOException {
        // FIXME put it in tmp folder of the bonita home
        final File tempFile = IOUtil.createTempDirectoryInDefaultTempDirectory("tempBarFolder");
        try {
            writeBusinessArchiveToFolder(businessArchive, tempFile);
            zipBarFolder(businessArchiveFile, tempFile);
        } finally {
            IOUtil.deleteDir(tempFile);
        }
    }

    /**
     * Save the uncompressed business archive folder to a compressed file.
     * <p>
     * this file can then be read using {@link #readBusinessArchive(File)}
     *
     * @param destFile
     * @param folderPath
     * @return
     * @throws IOException
     */
    public static String businessArchiveFolderToFile(final File destFile, final String folderPath) throws IOException {
        zipBarFolder(destFile, new File(folderPath));
        return destFile.getAbsolutePath();
    }

    private static void zipBarFolder(final File businessArchiveFile, final File folder) throws FileNotFoundException, IOException {
        // create a ZipOutputStream to zip the data to
        if (businessArchiveFile.exists()) {
            throw new IOException("The destination file already exists " + businessArchiveFile.getAbsolutePath());
        }

        final FileOutputStream fileOutput = new FileOutputStream(businessArchiveFile);
        final ZipOutputStream zos = new ZipOutputStream(fileOutput);
        try {
            IOUtil.zipDir(folder.getAbsolutePath(), zos, folder.getAbsolutePath());
        } finally {
            zos.close();
            fileOutput.close();
        }
    }

}

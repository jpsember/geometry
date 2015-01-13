package com.js.basic;

import java.io.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class Files {

  /**
   * Read string from file, using UTF-8 encoding
   */
  public static String readString(File file) throws IOException {
    return FileUtils.readFileToString(file, "UTF-8");
  }

  /**
   * Read string from InputStream, using UTF-8 encoding, then close the stream
   */
  public static String readString(InputStream inputStream) throws IOException {
    String str;
    try {
      str = IOUtils.toString(inputStream, "UTF-8");
    } finally {
      inputStream.close();
    }
    return str;
  }

  /**
   * Write string to file, using UTF-8 encoding
   */
  public static void writeString(File file, String content) throws IOException {
    FileUtils.writeStringToFile(file, content, "UTF-8");
  }

  public static void writeStringToFileIfChanged(File file, String content)
      throws IOException {
    if (file.isFile()) {
      String currentContents = Files.readString(file);
      if (currentContents.equals(content))
        return;
    }
    writeString(file, content);
  }

  /**
   * Get an input stream to a resource, which is stored in the class folder (or
   * one of its subfolders)
   * 
   * @param resourceName
   *          name of resource
   * @return BufferedInputStream
   * @throws IOException
   */
  public static BufferedInputStream openResource(Class theClass,
      String resourceName) throws IOException {
    InputStream is = theClass.getResourceAsStream(resourceName);
    if (is == null) {
      throw new FileNotFoundException("openResource failed: " + resourceName);
    }
    return new BufferedInputStream(is);
  }

  /**
   * Set extension of file (replacing any existing one)
   * 
   * @param extension
   *          new extension; if empty string, just removes existing extension
   */
  public static File setExtension(File file, String extension) {
    String filePath = file.getPath();
    filePath = FilenameUtils.removeExtension(filePath);
    if (!extension.isEmpty())
      filePath += FilenameUtils.EXTENSION_SEPARATOR_STR + extension;
    return new File(filePath);
  }

  /**
   * Get extension of a file
   * 
   * @return String containing extension, empty if it has none
   */
  public static String getExtension(File file) {
    return FilenameUtils.getExtension(file.getPath());
  }

  /**
   * Determine if file has an extension
   */
  public static boolean hasExtension(File file) {
    return !FilenameUtils.getExtension(file.getPath()).isEmpty();
  }

  /**
   * Remove extension, if any, from path
   */
  public static File removeExtension(File file) {
    return setExtension(file, "");
  }

  /**
   * Get file expressed relative to a containing directory, if it lies within
   * the directory
   * 
   * @param file
   *          an absolute file
   * @param directory
   *          an absolute directory, or null
   * @return file relative to directory, if directory is not null and file lies
   *         within directory's tree; otherwise, the absolute form of file
   */
  public static File fileWithinDirectory(File absFile, File absDirectory) {
    Files.verifyAbsolute(absFile);
    if (absDirectory == null)
      return absFile;
    Files.verifyAbsolute(absDirectory);
    String absDirPath = absDirectory.getPath();
    String absFilePath = absFile.getPath();

    if (!absFilePath.startsWith(absDirPath))
      return absFile;

    String suffix = absFilePath.substring(absDirPath.length());
    if (suffix.startsWith(File.separator)) {
      suffix = suffix.substring(File.separator.length());
    }
    return new File(suffix);
  }

  public static void verifyAbsolute(File file, boolean allowNull) {
    if (file == null) {
      if (!allowNull)
        throw new IllegalArgumentException();
      return;
    }
    if (!file.isAbsolute())
      throw new IllegalArgumentException("expected absolute file, but was '"
          + file + "'");
  }

  public static void verifyAbsolute(File file) {
    verifyAbsolute(file, false);
  }

}

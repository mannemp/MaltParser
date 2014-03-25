package org.maltparser.parser.pruneandscore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DirectoryUtils {
	public static void main(String[] args) {

		File srcFolderPath = new File("C:\\Users\\nikos7\\Desktop\\files");
		File destFolderPath = new File("C:\\Users\\nikos7\\Desktop\\newFiles");

		if (!srcFolderPath.exists()) {

			System.out.println("Directory does not exist. Will exit now");
			System.exit(0);

		} else {

			try {
				copyFolder(srcFolderPath, destFolderPath);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		System.out.println("Directory coping from " + srcFolderPath + "  to "
				+ destFolderPath + " was finished successfully");
	}

	public static void copyFolder(File srcFolderPath, File destFolderPath)
			throws IOException {

		if (!srcFolderPath.isDirectory()) {

			// If it is a File the Just copy It to the new Folder
			InputStream in = new FileInputStream(srcFolderPath);
			OutputStream out = new FileOutputStream(destFolderPath);

			byte[] buffer = new byte[1024];

			int length;

			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}

			in.close();
			out.close();
//			System.out.println("File copied from " + srcFolderPath + " to "
//					+ destFolderPath + " successfully");

		} else {

			// if it is a directory create the directory inside the new destination directory and
			// list the contents...
			
			if (!destFolderPath.exists()) {
				destFolderPath.mkdir();
//				System.out.println("Directory copied from " + srcFolderPath
//						+ "  to " + destFolderPath + " successfully");
			}

			String folder_contents[] = srcFolderPath.list();

			for (String file : folder_contents) {

				File srcFile = new File(srcFolderPath, file);
				File destFile = new File(destFolderPath, file);

				copyFolder(srcFile, destFile);
			}

		}
	}
}

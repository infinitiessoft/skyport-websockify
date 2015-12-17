/* LicenceStore.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1.1.1 $
 * Author: $Author: suvarov $
 * Date: $Date: 2007/03/08 00:26:35 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Handle saving and loading of licences
 */
package com.lixia.rdp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LicenceStore {

	static Logger logger = LoggerFactory.getLogger(LicenceStore.class);
	protected Options options;


	public LicenceStore(Options options) {
		this.options = options;
	}

	/**
	 * Load a licence from a file
	 * 
	 * @return Licence data stored in file
	 */
	public byte[] load_licence() {
		String path = options.getLicence_path() + "/licence." + options.getHostname();
		byte[] data = null;
		FileInputStream fd = null;
		try {
			fd = new FileInputStream(path);
			data = new byte[fd.available()];
			fd.read(data);
		} catch (FileNotFoundException e) {
			logger.warn("Licence file not found!");
		} catch (IOException e) {
			logger.warn("IOException in load_licence");
		} finally {
			if (fd != null) {
				try {
					fd.close();
				} catch (IOException e) {
					logger.error("close license stream failed.", e);
				}
			}
		}
		return data;
	}

	/**
	 * Save a licence to file
	 * 
	 * @param databytes
	 *            Licence data to store
	 */
	public void save_licence(byte[] databytes) {
		/* set and create the directory -- if it doesn't exist. */
		// String home = "/root";
		String dirpath = options.getLicence_path();// home+"/.rdesktop";
		String filepath = dirpath + "/licence." + options.getHostname();

		File file = new File(dirpath);
		file.mkdir();
		try {
			FileOutputStream fd = new FileOutputStream(filepath);

			/* write to the licence file */
			fd.write(databytes);
			fd.close();
			logger.info("Stored licence at " + filepath);
		} catch (FileNotFoundException e) {
			logger.warn("save_licence: file path not valid!");
		} catch (IOException e) {
			logger.warn("IOException in save_licence");
		}
	}

}

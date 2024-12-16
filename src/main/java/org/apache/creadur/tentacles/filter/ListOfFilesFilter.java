/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.creadur.tentacles.filter;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ListOfFilesFilter implements FileFilter {

	private final List<String> listOfFiles;

	ListOfFilesFilter(String... files) {
		listOfFiles = Arrays.asList(files);
	}

	@Override
	public boolean accept(File pathname) {
		if (pathname.isDirectory()) {
			return false;
		}

		return listOfFiles.contains(pathname.getName().toLowerCase());
	}

	/**
	 * @return an unmodifiable list of the files to filter for.
	 */
	public List<String> getListOfFiles() {
		return Collections.unmodifiableList(listOfFiles);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Filtering for one of the following file names [");
		builder.append(listOfFiles);
		builder.append("]");
		return builder.toString();
	}

}

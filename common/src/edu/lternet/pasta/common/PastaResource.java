/*
 *
 * Copyright 2011, 2012, 2013 the University of New Mexico.
 *
 * This work was supported by National Science Foundation Cooperative
 * Agreements #DEB-0832652 and #DEB-0936498.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 */

package edu.lternet.pasta.common;

/**
 * @author servilla
 * @since Nov 12, 2012
 * 
 *        A PASTA Resource utility class.
 * 
 */
public class PastaResource {

	/*
	 * Class variables
	 */
	
	private static final String PACKAGE_PATTERN = "knb-lter-";

	/*
	 * Instance variables
	 */
	
	/*
	 * Constructors
	 */
	
	/*
	 * Class methods
	 */

	/*
	 * Instance methods
	 */
	
	/**
	 * Returns the canonical EML package identifier from the object PASTA resource
	 * identifier.  Note: this method assumes that the scope identifier part of
	 * the package identifier begins with the substring "knb-lter".
	 * 
	 * @return The canonical EML package identifier
	 */
	static public String getPackageId(String resourceId) {
		
		String packageId = null;
		String scope = null;
		String identifier = null;
		String revision = null;
		
		String[] pathParts = resourceId.split("/");
		String part = null;
		
		for (int i = 0; i < pathParts.length; i++) {
			
			if (pathParts[i].contains(PACKAGE_PATTERN)) {
				scope = pathParts[i];
				identifier = pathParts[i+1];
				revision = pathParts[i+2];
				packageId = scope + "." + identifier + "." + revision;
				break;
			}
			
		}
		
		return packageId;
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

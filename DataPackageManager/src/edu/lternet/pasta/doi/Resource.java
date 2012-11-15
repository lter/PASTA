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

package edu.lternet.pasta.doi;

/**
 * @author servilla
 * @since Nov 9, 2012
 * 
 *        Bundles resource attribute information.
 * 
 */
public class Resource {

	/*
	 * Class variables
	 */

	/*
	 * Instance variables
	 */

	private String resourceId = null;
	private String resourceType = null;
	private String packageId = null;
	private String dateCreated = null;

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
	 * Set resource identifier.
	 * 
	 * @param resourceId
	 */
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}
	
	/**
	 * Set resource type.
	 * 
	 * @param resourceType
	 */
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}
	
	/**
	 * Set package identifier.
	 * 
	 * @param packageId
	 */
	public void setPackageId(String packageId) {
		this.packageId = packageId;
	}
	
	/**
	 * Set date created.
	 * 
	 * @param dateCreated
	 */
	public void setDateCreate(String dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	/**
	 * Get resource identifier.
	 * 
	 * @return Resource identifier
	 */
	public String getResourceId() {
		return this.resourceId;
	}
	
	/**
	 * Get resource type.
	 * 
	 * @return Resource type
	 */
	public String getResourceType() {
		return this.resourceType;
	}
	
	/**
	 * Get package identifier.
	 * 
	 * @return Package identifier
	 */
	public String getPackageId() {
		return this.packageId;
	}
	
	/**
	 * Get date created.
	 * 
	 * @return Date created
	 */
	public String getDateCreated() {
		return this.dateCreated;
	}
	
}

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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.lang.Class;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import eml.ecoinformatics_org.eml_2_1.Eml;
import eml.ecoinformatics_org.party_2_1.ResponsibleParty;
import eml.ecoinformatics_org.party_2_1.Person;

import edu.lternet.pasta.common.EmlUtility;
import edu.lternet.pasta.datapackagemanager.DataPackageManager;

import org.apache.log4j.Logger;

/**
 * @author servilla
 * @since Nov 6, 2012
 * 
 *        Provides a plain old Java object (POJO) interface to content from an
 *        EML 2.1.0 XML document.
 */
public class EmlObject {

	/*
	 * Class variables
	 */

	/*
	 * Instance variables
	 */

	private Logger logger = Logger.getLogger(EmlObject.class);

	private String emlString = null;
	private Eml eml = null;

	/*
	 * Constructors
	 */

	/**
	 * Create the EML 2.1.0 POJO from an EML file.
	 * 
	 * @param emlFile
	 */
	public EmlObject(File emlFile) {

		this.emlString = EmlUtility.getEmlDoc(emlFile);
		this.eml = EmlUtility.getEml2_1_0(emlString);

	}

	/*
	 * Class methods
	 */

	/*
	 * Instance methods
	 */

	/**
	 * Returns ArrayList of Creators.
	 * 
	 * @return Creator array list
	 */
	public ArrayList<Creator> getCreators() {

		ArrayList<Creator> creators = new ArrayList<Creator>();

		List<ResponsibleParty> responsibleParties = null;
		ResponsibleParty responsibleParty = null;
		List<JAXBElement<?>> names = null;
		JAXBElement<?> name = null;

		responsibleParties = eml.getDataset().getCreator();
		Iterator<?> rpItr = responsibleParties.iterator();

		while (rpItr.hasNext()) {

			responsibleParty = (ResponsibleParty) rpItr.next();
			names = responsibleParty
			    .getIndividualNameOrOrganizationNameOrPositionName();
			Iterator<?> nItr = names.iterator();

			String nameType = null;
			Person person = null;
			Creator creator = null;

			while (nItr.hasNext()) {
				name = (JAXBElement<?>) nItr.next();
				nameType = name.getName().getLocalPart();

				if (nameType.equals(Creator.PERSON)) {

					creator = new Creator(Creator.PERSON);
					person = (Person) name.getValue();

					try {
						creator.setSurName(person.getSurName().trim());
					} catch (Exception e) {
						logger.error(e);
						e.printStackTrace();
					}

					List<String> givenNames = person.getGivenName();
					Iterator<String> gnItr = givenNames.iterator();
					StringBuffer givenName = new StringBuffer("");

					while (gnItr.hasNext()) {
						String namePart = (String) gnItr.next();
						givenName.append(namePart + " ");
					}

					try {
						creator.setGivenName(givenName.toString().trim());
					} catch (Exception e) {
						logger.error(e);
						e.printStackTrace();
					}

					creators.add(creator);

				} else if (nameType.equals(Creator.ORGANIZATION)) {

					creator = new Creator(Creator.ORGANIZATION);
					String organizationName = (String) name.getValue();

					try {
						creator.setOrganizationName(organizationName.trim());
					} catch (Exception e) {
						logger.error(e);
						e.printStackTrace();
					}

					creators.add(creator);

				} else { // Creator.POSITION

					creator = new Creator(Creator.POSITION);
					String positionName = (String) name.getValue();

					try {
						creator.setPositionName(positionName.trim());
					} catch (Exception e) {
						logger.error(e);
						e.printStackTrace();
					}

					creators.add(creator);

				}

			}

		}

		return creators;

	}

	/**
	 * Returns ArrayList of titles.
	 * 
	 * @return Title array list
	 */
	public ArrayList<Title> getTitles() {

		ArrayList<Title> titles = new ArrayList<Title>();

		List<String> titleList = this.eml.getDataset().getTitle();

		Iterator tItr = titleList.iterator();

		while (tItr.hasNext()) {
			
			Title title = new Title();
		
			try {
				title.setTitleType(Title.MAIN);
			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}

			String titleName = (String) tItr.next();
			title.setTitle(titleName.trim());

			titles.add(title);
		}

		return titles;

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		File emlFile = new File("/Users/servilla/local/pasta/metadata/knb-lter-nin.1.1/Level-1-EML.xml");

		EmlObject emlObj = new EmlObject(emlFile);

		ArrayList<Creator> creators = emlObj.getCreators();

		for (Creator creator : creators) {
			System.out.println(creator.getCreatorName());
		}
		
		ArrayList<Title> titles = emlObj.getTitles();
		
		for (Title title : titles) {
			System.out.println(title.getTite());
		}

	}

}

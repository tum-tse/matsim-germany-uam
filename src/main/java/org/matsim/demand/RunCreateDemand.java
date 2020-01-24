/* *********************************************************************** *
 * project: org.matsim.*
 * EditRoutesTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


package org.matsim.demand;

import java.net.MalformedURLException;

import org.matsim.api.core.v01.population.Population;

/**
* @author smueller
*/

public class RunCreateDemand {

	public static void main(String[] args) {
		
		boolean train = true;
		boolean car = true;
		boolean airplane = true;
		boolean pt = true;
		boolean bike = false;
		boolean walk = false;
		
		double sample = 0.1;
		
		String outputPopulationFile = "output/test" + 100 * sample + "pct.xml";
		
		
		try {
			Population population = CreateDemand.create(outputPopulationFile, sample, train, car, airplane, pt, bike, walk);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

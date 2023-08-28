/**
 * Copyright (c) 2012 - 2023 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package de.avatar.mdp.evaluation.component.helper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * 
 * @author ilenia
 * @since Aug 3, 2023
 */
public class EvaluationHelper {

	public static void executeExternalCmd(Logger LOGGER, String... command) {
		ProcessBuilder pb = new ProcessBuilder(command);
		try {
			Process process = pb.start();
			process.waitFor();
			if(process.exitValue() != 0) {
				try(InputStream is = process.getErrorStream()) {
					System.out.println(new String( is.readAllBytes()));
					LOGGER.severe(String.format("Process %s exit code != 0. Error stream is: %s", Arrays.toString(command), new String(is.readAllBytes())));
				}
			}
		} catch (InterruptedException | IOException e) {
			LOGGER.severe(String.format("Exception while waiting for command %s: %s", Arrays.toString(command), e.getMessage()));
			e.printStackTrace();
		}
	}
}

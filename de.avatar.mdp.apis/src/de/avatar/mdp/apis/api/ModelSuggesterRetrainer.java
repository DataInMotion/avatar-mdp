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
package de.avatar.mdp.apis.api;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * 
 * @author ilenia
 * @since Aug 3, 2023
 */
@ProviderType
public interface ModelSuggesterRetrainer {

	/**
	 * For now we take as input simply a list of documents that have to be considered as relevant
	 * based on the criterium we want to retrain the model on, and a list of unrelevant docs.
	 * In the future this might be substituted by an own model, which would be a sort of additional 
	 * model, coupled with whatever model we evaluated
	 * 
	 * @param pertinentDocs
	 * @param unrelevantDocs
	 */
	void retrainModelSuggester(List<String> pertinentDocs, List<String> unrelevantDocs);
}

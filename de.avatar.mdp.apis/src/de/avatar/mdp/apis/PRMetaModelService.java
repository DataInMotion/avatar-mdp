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
package de.avatar.mdp.apis;

import java.util.List;

import org.eclipse.emf.ecore.EPackage;
import org.osgi.annotation.versioning.ProviderType;

import de.avatar.mdp.evaluation.EvaluationSummary;
import de.avatar.mdp.prmeta.PRModel;

/**
 * 
 * @author ilenia
 * @since Aug 8, 2023
 */
@ProviderType
public interface PRMetaModelService {

	List<PRModel> getPRModelByNsURI(String... modelNsURIs);
	
	void savePRModel(PRModel prModel);
	
	PRModel createPRModel(EvaluationSummary evaluationSummary, EPackage ePackage);
	
}

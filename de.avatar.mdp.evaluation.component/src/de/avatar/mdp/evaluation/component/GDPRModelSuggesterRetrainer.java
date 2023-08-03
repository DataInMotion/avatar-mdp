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
package de.avatar.mdp.evaluation.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.avatar.mdp.apis.api.ModelSuggesterRetrainer;
import de.avatar.mdp.evaluation.component.helper.EvaluationHelper;

/**
 * 
 * @author ilenia
 * @since Aug 3, 2023
 */
@Component(name = "GDPRModelSuggesterRetrainer", service = ModelSuggesterRetrainer.class, configurationPid = "GDPRModelSuggesterRetrainer", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class GDPRModelSuggesterRetrainer implements ModelSuggesterRetrainer {

	private static final Logger LOGGER = Logger.getLogger(GDPRModelSuggesterRetrainer.class.getName());

	public @interface GDPRModelSuggesterRetrainerConfig {
		String pyScriptBasePath() default ""; 
	}

	private GDPRModelSuggesterRetrainerConfig config;

	@Activate
	public void activate(GDPRModelSuggesterRetrainerConfig config) {
		this.config = config;
	}


	/* 
	 * (non-Javadoc)
	 * @see de.avatar.mdp.apis.api.ModelSuggesterRetrainer#retrainModelSuggester(java.util.List, java.util.List)
	 */
	@Override
	public void retrainModelSuggester(List<String> pertinentDocs, List<String> unrelevantDocs) {
		Map<String, List<String>> retrainedDocMap = createRetrainDocMap(pertinentDocs, unrelevantDocs);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			String jsonDocMap = objectMapper.writeValueAsString(retrainedDocMap);
			EvaluationHelper.executeExternalCmd(LOGGER, "python3.10", config.pyScriptBasePath() + "retrain.py", config.pyScriptBasePath(), jsonDocMap);
		} catch (IOException e) {
			LOGGER.severe(String.format("Exception while retraining GDPR suggester: %s", e.getMessage()));
			e.printStackTrace();
		}
	}



	private Map<String, List<String>> createRetrainDocMap(List<String> pertinentDocs, List<String> unrelevantDocs) {
		Map<String, List<String>> retrainedDocMap = new HashMap<>();
		retrainedDocMap.put("IDENTITY", pertinentDocs);
		retrainedDocMap.put("NO-RELATED", unrelevantDocs);
		return retrainedDocMap;
	}

}

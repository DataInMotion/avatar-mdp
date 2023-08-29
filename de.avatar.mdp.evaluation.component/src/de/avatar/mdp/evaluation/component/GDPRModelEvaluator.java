/**
 * Copyright (c) 2012 - 2018 Data In Motion and others.
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

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.avatar.mdp.apis.api.ModelEvaluator;
import de.avatar.mdp.evaluation.EvaluatedTerm;
import de.avatar.mdp.evaluation.Evaluation;
import de.avatar.mdp.evaluation.EvaluationCriteriumType;
import de.avatar.mdp.evaluation.EvaluationSummary;
import de.avatar.mdp.evaluation.MDPEvaluationFactory;
import de.avatar.mdp.evaluation.Relevance;
import de.avatar.mdp.evaluation.RelevanceLevelType;
import de.avatar.mdp.evaluation.component.helper.EvaluationHelper;

@Component(name="GDPRModelEvaluator", service = ModelEvaluator.class, configurationPid = "GDPRModelEvaluator", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class GDPRModelEvaluator implements ModelEvaluator {
	
	private static final Logger LOGGER = Logger.getLogger(GDPRModelEvaluator.class.getName());
	private static final String MODEL_ANNOTATION_SOURCE = "http://www.eclipse.org/emf/2002/GenModel";
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HH:mm:ss.SSS");
	
	public @interface GDPRModelEvaluatorConfig {
		String basePath() default "./";
		String modelPath() default "./model/";
		String pyScriptBasePath() default "./py/";
		String outputBasePath() default "./out/";
	}
	
	private GDPRModelEvaluatorConfig config;
	
	@Activate
	public void activate(GDPRModelEvaluatorConfig config) {
		this.config = config;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EvaluationSummary evaluateModel(EPackage ePackage) {
		
		List<EvaluatedTerm> termsToBeEvaluated = prepareEvaluatedTerms(ePackage);		
        ObjectMapper objectMapper = new ObjectMapper();
        try {
        	Map<String, List<String>> featureNameDocMap = new LinkedHashMap<>();
        	termsToBeEvaluated
        		.forEach(t -> featureNameDocMap.put(t.getElementClassifierName().concat("::").concat(t.getEvaluatedModelElement().getName()), 
        			t.getEvaluations().stream().map(e -> e.getInput()).toList()));
            String jsonDocMap = objectMapper.writeValueAsString(featureNameDocMap);
            LocalDateTime date = LocalDateTime.now();
            String dateStr = date.format(DATE_TIME_FORMATTER);
            String outFileName = config.outputBasePath() + ePackage.getName() + "_" + dateStr +".json";
            EvaluationHelper.executeExternalCmd(LOGGER, "python3.10", config.pyScriptBasePath() + "multilabel_predict.py", config.modelPath(), outFileName, jsonDocMap);
            Map<String,Object> result = objectMapper.readValue(new File(outFileName), LinkedHashMap.class);
            EvaluationSummary summary = createEvaluationSummary(result, termsToBeEvaluated);
            return summary;
        } catch (IOException e) {
			LOGGER.severe(String.format("Exception while making GDPR predicitons for model %s: %s", ePackage.getName(), e.getMessage()));
            e.printStackTrace();
        }
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private EvaluationSummary createEvaluationSummary(Map<String,Object> modelPredictions, List<EvaluatedTerm> termsList) {
		EvaluationSummary summary = MDPEvaluationFactory.eINSTANCE.createEvaluationSummary();
		summary.setEvaluationCriterium(EvaluationCriteriumType.GDPR);
		modelPredictions.forEach((k,v) -> {
			EvaluatedTerm evaluatedTerm = termsList.stream().filter(t -> t.getElementClassifierName().concat("::").concat(t.getEvaluatedModelElement().getName()).equals(k)).findFirst().orElse(null);
			if(evaluatedTerm == null) {
				LOGGER.severe(String.format("No matching term found for evaluated %s", k));
				throw new IllegalStateException(String.format("No matching term found for evaluated %s", k));
			}
			Map<String, Map<String, String>> predictions = (Map<String, Map<String, String>>) v;
			predictions.forEach((doc, pred) -> {
				Evaluation evaluation = evaluatedTerm.getEvaluations().stream().filter(e -> e.getInput().equals(doc)).findFirst().orElse(null);
				if(evaluation == null) {
					LOGGER.severe(String.format("No matching evaluation doc found for evaluated %s", doc));
					throw new IllegalStateException(String.format("No matching evaluation doc found for evaluated %s", doc));
				}
				pred.forEach((category, level) -> {
					Relevance relevance = MDPEvaluationFactory.eINSTANCE.createRelevance();
					relevance.setCategory(category);
					relevance.setLevel(RelevanceLevelType.valueOf(level));
					evaluation.getRelevance().add(relevance);
				});
			});
			summary.getEvaluatedTerms().add(evaluatedTerm);
		});
		return summary;		
	}
	
	private List<EvaluatedTerm> prepareEvaluatedTerms(EPackage ePackage) {
		List<EvaluatedTerm> evaluatedTerms = new LinkedList<>();
		ePackage.getEClassifiers().forEach(classifier -> {
			if(classifier instanceof EClass eClass) {
				eClass.getEAllStructuralFeatures().forEach(feature -> {
					evaluatedTerms.add(doCreateEvaluatedTerm(feature, eClass.getName()));
				});
			} 
			else if(classifier instanceof EEnum eEnum) {
				eEnum.getELiterals().forEach(literal -> {
					evaluatedTerms.add(doCreateEvaluatedTerm(literal, eEnum.getName()));			
				});
			}
			else if(classifier instanceof EDataType eDataType) {
				evaluatedTerms.add(doCreateEvaluatedTerm(eDataType, eDataType.getName()));	
			}
		});		
		return evaluatedTerms;
	}
	
	private EvaluatedTerm doCreateEvaluatedTerm(ENamedElement element, String eClassifierName) {
		List<String> docs = new ArrayList<>();
		docs.add(element.getName());
		String dataTypeDoc = extractElementDocumentation(element.getEAnnotation(MODEL_ANNOTATION_SOURCE)); 
		if(dataTypeDoc != null) docs.add(dataTypeDoc);
		EvaluatedTerm term = MDPEvaluationFactory.eINSTANCE.createEvaluatedTerm();
		URI uri = EcoreUtil.getURI(element);
		InternalEObject internalEObj = (InternalEObject) element;
		internalEObj.eSetProxyURI(uri);
		term.setEvaluatedModelElement(element);
		term.setElementClassifierName(eClassifierName);
		docs.forEach(doc -> {
			Evaluation evaluation = MDPEvaluationFactory.eINSTANCE.createEvaluation();
			evaluation.setInput(doc);
			term.getEvaluations().add(evaluation);
		});
		return term;
	}
	
	private String extractElementDocumentation(EAnnotation annotation) {
		if(annotation == null) return null;
		if(!annotation.getDetails().containsKey("documentation")) {
			return null;
		}
		return annotation.getDetails().get("documentation");
	}
}

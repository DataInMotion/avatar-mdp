/**
 * Copyright (c) 2012 - 2023 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package de.avatar.mdp.evaluation.component.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.gecko.emf.osgi.example.model.basic.BasicPackage;
import org.gecko.emf.repository.EMFRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.annotation.Property;
import org.osgi.test.common.annotation.config.WithFactoryConfiguration;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.cm.ConfigurationExtension;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

import de.avatar.mdp.apis.api.PRMetaModelService;
import de.avatar.mdp.evaluation.EvaluatedTerm;
import de.avatar.mdp.evaluation.Evaluation;
import de.avatar.mdp.evaluation.EvaluationSummary;
import de.avatar.mdp.evaluation.MDPEvaluationFactory;
import de.avatar.mdp.prmeta.EvaluationCriteriumType;
import de.avatar.mdp.prmeta.EvaluationLevelType;
import de.avatar.mdp.prmeta.PRClassifier;
import de.avatar.mdp.prmeta.PRFeature;
import de.avatar.mdp.prmeta.PRMetaFactory;
import de.avatar.mdp.prmeta.PRModel;
import de.avatar.mdp.prmeta.PRPackage;

/**
 * 
 * @author ilenia
 * @since Aug 8, 2023
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
@ExtendWith(ConfigurationExtension.class)
public class PRMetaModelServiceTest {
	
	
	@BeforeEach
	public void beforeEach() {
		System.out.println("Test");
	}
	
	@Test
	@WithFactoryConfiguration(
			factoryPid = "EMFMongoRepositoryConfigurator",
			location = "?",
			name = "avatarmdp", 
			properties = {
					@Property(key = "mongo.instances", value = "avatarmdp"),
					@Property(key = "avatarmdp.baseUris", value = "mongodb://mongodb"),
					@Property(key = "avatarmdp.databases", value = "avatarmdp"),
					@Property(key = "avatarmdp.avatarmdp.repoType", value = "PROTOTYPE")

			})
	public void testServices(@InjectService(timeout = 5000l, filter = "(repo_id=avatarmdp.avatarmdp)") ServiceAware<EMFRepository> repoAware
			,@InjectService(timeout=5000l) ServiceAware<PRMetaModelService> metaModelAware
			) {
		
		assertThat(repoAware).isNotNull();
		EMFRepository repo = repoAware.getService();
		assertThat(repo).isNotNull();
		
		assertThat(metaModelAware).isNotNull();
		PRMetaModelService metaModelService = metaModelAware.getService();
		assertThat(metaModelService).isNotNull();
	}
	
//	@Test
	@WithFactoryConfiguration(
			factoryPid = "EMFMongoRepositoryConfigurator",
			location = "?",
			name = "avatarmdp", 
			properties = {
					@Property(key = "mongo.instances", value = "avatarmdp"),
					@Property(key = "avatarmdp.baseUris", value = "mongodb://mongodb"),
					@Property(key = "avatarmdp.databases", value = "avatarmdp"),
					@Property(key = "avatarmdp.avatarmdp.repoType", value = "PROTOTYPE")

			})
	public void testSave(@InjectService(timeout = 5000l, filter = "(repo_id=avatarmdp.avatarmdp)") ServiceAware<EMFRepository> repoAware
			,@InjectService(timeout=5000l) ServiceAware<PRMetaModelService> metaModelAware,
			@InjectService ServiceAware<BasicPackage> packAware
			) throws IOException {
		
		assertThat(repoAware).isNotNull();
		EMFRepository repo = repoAware.getService();
		assertThat(repo).isNotNull();
		
		assertThat(metaModelAware).isNotNull();
		PRMetaModelService metaModelService = metaModelAware.getService();
		assertThat(metaModelService).isNotNull();
		
		assertThat(packAware).isNotNull();
		BasicPackage model = packAware.getService();
		assertThat(model).isNotNull();
		
		assertThat(model.eResource()).isNotNull();
		repo.getResourceSet().createResource(model.eResource().getURI()).getContents().add(model);
		Resource res = repo.getResourceSet().getResource(model.eResource().getURI(), true);
		EPackage modelCopy = EcoreUtil.copy(model);
		res.getContents().add(modelCopy);
		assertThat(modelCopy.eResource().getResourceSet()).isNotNull();
		
		PRModel prModel = PRMetaFactory.eINSTANCE.createPRModel();
		prModel.setPackageURI(modelCopy.getNsURI());
		PRPackage prPackage = PRMetaFactory.eINSTANCE.createPRPackage();
		prPackage.setEPackage(modelCopy);
		prPackage.setEvaluationCriterium(EvaluationCriteriumType.GDPR);
//		PRClass prClass = PRMetaFactory.eINSTANCE.createPRClass();
//		prClass.setEClass(model.getAddress());
//		prClass.setEvaluationLevel(EvaluationLevelType.WARNING);
//		PRFeature prFeature = PRMetaFactory.eINSTANCE.createPRFeature();
//		prFeature.setFeature(model.getAddress_City());
//		prFeature.setEvaluationLevel(EvaluationLevelType.WARNING);
//		prClass.getPrFeature().add(prFeature);
//		prPackage.getPrClass().add(prClass);
		prModel.getPrPackage().add(prPackage);
		
		metaModelService.savePRModel(prModel);
	}
	
	@Test
	@WithFactoryConfiguration(
			factoryPid = "EMFMongoRepositoryConfigurator",
			location = "?",
			name = "avatarmdp", 
			properties = {
					@Property(key = "mongo.instances", value = "avatarmdp"),
					@Property(key = "avatarmdp.baseUris", value = "mongodb://mongodb"),
					@Property(key = "avatarmdp.databases", value = "avatarmdp"),
					@Property(key = "avatarmdp.avatarmdp.repoType", value = "PROTOTYPE")

			})
	public void testCreation(@InjectService(timeout = 5000l, filter = "(repo_id=avatarmdp.avatarmdp)") ServiceAware<EMFRepository> repoAware
			,@InjectService(timeout=5000l) ServiceAware<PRMetaModelService> metaModelAware,
			@InjectService ServiceAware<BasicPackage> packAware
			) throws IOException {
		
		assertThat(repoAware).isNotNull();
		EMFRepository repo = repoAware.getService();
		assertThat(repo).isNotNull();
		
		assertThat(metaModelAware).isNotNull();
		PRMetaModelService metaModelService = metaModelAware.getService();
		assertThat(metaModelService).isNotNull();
		
		assertThat(packAware).isNotNull();
		BasicPackage model = packAware.getService();
		assertThat(model).isNotNull();
		
		EvaluationSummary summary = MDPEvaluationFactory.eINSTANCE.createEvaluationSummary();
		summary.setEvaluationCriterium(de.avatar.mdp.evaluation.EvaluationCriteriumType.GDPR);
		
		EvaluatedTerm term1 = MDPEvaluationFactory.eINSTANCE.createEvaluatedTerm();
		term1.setEvaluatedFeature(EcoreUtil.copy(model.getAddress_City()));
		term1.setFeatureClassifierName(model.getAddress().getName());
		Evaluation e11 = MDPEvaluationFactory.eINSTANCE.createEvaluation();
		e11.setInput("city");
		e11.setRelevant(true);
		Evaluation e12 = MDPEvaluationFactory.eINSTANCE.createEvaluation();
		e12.setInput("The city of the customer");
		e12.setRelevant(false);
		term1.getEvaluations().add(e11);
		term1.getEvaluations().add(e12);
		
		EvaluatedTerm term2 = MDPEvaluationFactory.eINSTANCE.createEvaluatedTerm();
		term2.setEvaluatedFeature(EcoreUtil.copy(model.getContact_Context()));
		term2.setFeatureClassifierName(model.getContact().getName());
		Evaluation e21 = MDPEvaluationFactory.eINSTANCE.createEvaluation();
		e21.setInput("context");
		e21.setRelevant(false);
		term2.getEvaluations().add(e21);

		summary.getEvaluatedTerms().add(term1);
		summary.getEvaluatedTerms().add(term2);
		
		PRModel prModel = metaModelService.createPRModel(summary, model);
		assertThat(prModel).isNotNull();
		assertThat(prModel.getPackageURI()).isEqualTo(model.getNsURI());
		assertThat(prModel.getPrPackage()).hasSize(1);
		
		PRPackage prPackage = prModel.getPrPackage().get(0);
		assertThat(prPackage.getEvaluationCriterium()).isEqualTo(EvaluationCriteriumType.GDPR);
		assertThat(prPackage.getPrClassifier()).hasSize(2);
		assertThat(prPackage.getEPackage()).isEqualTo(model);
		
		PRClassifier prc1 = null, prc2 = null;
		for(PRClassifier prc : prPackage.getPrClassifier()) {
			if(prc.getEClassifier().equals(model.getAddress())) {
				prc1 = prc;
			} else if(prc.getEClassifier().equals(model.getContact())) {
				prc2 = prc;
			}
		}
		assertThat(prc1).isNotNull();
		assertThat(prc2).isNotNull();
		
		assertThat(prc1.getEvaluationLevel()).isEqualTo(EvaluationLevelType.WARNING);
		assertThat(prc2.getEvaluationLevel()).isEqualTo(EvaluationLevelType.NONE);
		
		assertThat(prc1.getEClassifier()).isEqualTo(model.getAddress());
		assertThat(prc2.getEClassifier()).isEqualTo(model.getContact());
		
		assertThat(prc1.getPrFeature()).hasSize(1);
		assertThat(prc2.getPrFeature()).hasSize(1);

		PRFeature prf1 = prc1.getPrFeature().get(0);
		assertThat(prf1.getEvaluationLevel()).isEqualTo(EvaluationLevelType.WARNING);
		assertThat(prf1.getFeature().getName()).isEqualTo(model.getAddress_City().getName());
		
		PRFeature prf2 = prc2.getPrFeature().get(0);
		assertThat(prf2.getEvaluationLevel()).isEqualTo(EvaluationLevelType.NONE);
		assertThat(prf2.getFeature().getName()).isEqualTo(model.getContact_Context().getName());		
	}

}

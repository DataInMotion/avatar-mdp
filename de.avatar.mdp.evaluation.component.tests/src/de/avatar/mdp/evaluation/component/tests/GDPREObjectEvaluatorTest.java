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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import org.gecko.emf.osgi.example.model.basic.Address;
import org.gecko.emf.osgi.example.model.basic.BasicFactory;
import org.gecko.emf.osgi.example.model.basic.Contact;
import org.gecko.emf.osgi.example.model.basic.Person;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.annotation.Property;
import org.osgi.test.common.annotation.config.WithFactoryConfiguration;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.cm.ConfigurationExtension;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

import de.avatar.mdp.apis.EObjectEvaluator;
import de.avatar.mdp.evaluation.EvaluationSummary;
import de.avatar.mdp.medicalrecord.MedicalHistory;
import de.avatar.mdp.medicalrecord.MedicalRecord;
import de.avatar.mdp.medicalrecord.MedicalRecordFactory;
import de.avatar.mdp.medicalrecord.MedicalRecordPackage;
import de.avatar.mdp.medicalrecord.PatientInfo;

@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
@ExtendWith(ConfigurationExtension.class)
public class GDPREObjectEvaluatorTest {
	
	@Test
	@WithFactoryConfiguration(
			factoryPid = "GDPREObjectEvaluator",
			location = "?",
			name = "test", 
			properties = {
					@Property(key = "basePath", value = "./data/"),
					@Property(key = "modelPath", value = "./data/model/"),
					@Property(key = "pyScriptBasePath", value = "./data/py/ner_fake_pii_generator/"),
					@Property(key = "outputBasePath", value = "./data/out/"),
					@Property(key = "modelName", value = "ner_fake_pii_generator")

			})
	public void testEObjEvaluator(@InjectService ServiceAware<EObjectEvaluator> meAware, 
			@InjectService ServiceAware<MedicalRecordPackage> packAware) {
		assertThat(meAware).isNotNull();
		EObjectEvaluator evaluator = meAware.getService();
		assertThat(evaluator).isNotNull();
		
		MedicalRecord medicalRecord = MedicalRecordFactory.eINSTANCE.createMedicalRecord();
		PatientInfo patientInfo = MedicalRecordFactory.eINSTANCE.createPatientInfo();
		patientInfo.setName("John Doe");
		patientInfo.setAddress("Camsdorfer Str. 36");
		patientInfo.setBirthDate(new Date());
		medicalRecord.setPatientInfo(patientInfo);
		MedicalHistory medicalHistory = MedicalRecordFactory.eINSTANCE.createMedicalHistory();
		medicalHistory.setAllergies("none");
		medicalHistory.setVaccinations("I got covid-19 vaccination");
		medicalRecord.setMedicalHistory(medicalHistory);
		EvaluationSummary summary = evaluator.evaluateEObject(medicalRecord);
		assertThat(summary).isNotNull();
	}
	
	@Test
	@WithFactoryConfiguration(
			factoryPid = "GDPREObjectEvaluator",
			location = "?",
			name = "test", 
			properties = {
					@Property(key = "basePath", value = "./data/"),
					@Property(key = "modelPath", value = "./data/model/"),
					@Property(key = "pyScriptBasePath", value = "./data/py/ner_fake_pii_generator/"),
					@Property(key = "outputBasePath", value = "./data/out/"),
					@Property(key = "modelName", value = "ner_fake_pii_generator")

			})
	public void testEObjEvaluatorWithManyRef(@InjectService ServiceAware<EObjectEvaluator> meAware) {
		assertThat(meAware).isNotNull();
		EObjectEvaluator evaluator = meAware.getService();
		assertThat(evaluator).isNotNull();
		
		Person person = BasicFactory.eINSTANCE.createPerson();
		Address a1 = BasicFactory.eINSTANCE.createAddress();
		a1.setStreet("street1");
		person.setAddress(a1); 
		
		Contact c1 = BasicFactory.eINSTANCE.createContact();
		c1.setValue("c1");
		Contact c2 = BasicFactory.eINSTANCE.createContact();
		c2.setValue("c2");
		person.getContact().add(c1);
		person.getContact().add(c2);

		EvaluationSummary summary = evaluator.evaluateEObject(person);
		assertThat(summary).isNotNull();
	}
	
	@AfterEach
	 public void afterEach() throws IOException {
		 Path folder = Path.of("data/out/");
		 Files.list(folder).forEach(p -> {
			try {
				if(Files.exists(p)) Files.delete(p);
			} catch (IOException e) {
				e.printStackTrace();
			} 
		 });
	 }

}

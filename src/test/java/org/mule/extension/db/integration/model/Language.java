/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.model;

public class Language {

  public static final Language SPANISH = new Language("Spanish", "La mejor letra del mundo es la ñ");
  public static final Language GERMAN = new Language("German", "Für den Feind noch den Frieden des Grabes");

  private String name;
  private String sampleText;

  public Language(String name, String sampleText) {
    this.name = name;
    this.sampleText = sampleText;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSampleText() {
    return sampleText;
  }

  public void setSampleText(String sampleText) {
    this.sampleText = sampleText;
  }

}

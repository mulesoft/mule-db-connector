/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.model;

import java.util.Arrays;
import java.util.Objects;

public class Contact {

  public static final Contact CONTACT1 =
      new Contact("Contact1", new ContactDetails[] {new ContactDetails("home", "1-111-111", "1@1111.com")});
  public static final Contact CONTACT2 =
      new Contact("Contact2", new ContactDetails[] {new ContactDetails("work", "2-222-222", "2@2222.com")});

  private final String name;
  private final ContactDetails[] details;

  public Contact(String name, ContactDetails[] details) {
    this.name = name;
    this.details = details;
  }

  public String getName() {
    return name;
  }

  public ContactDetails[] getDetails() {
    return details;
  }

  public Object[] getDetailsAsObjectArray() {
    final Object[] result = new Object[details.length];
    for (int i = 0; i < details.length; i++) {
      result[i] = details[i].asObjectArray();
    }

    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Contact contact = (Contact) o;
    return Objects.equals(name, contact.name) &&
        Arrays.equals(details, contact.details);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(name);
    result = 31 * result + Arrays.hashCode(details);
    return result;
  }
}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db;

public interface AllureConstants {

  interface DbFeature {

    String DB_EXTENSION = "DB Extension";

    interface DbStory {

    }
  }

  public interface MySqlFeature {

    String MYSQL_FEATURE = "MySQL";

    interface MySqlStories {

      String MYSQL_RESOURCE_RELEASING = "MySQL Resource Releasing";
    }
  }
}

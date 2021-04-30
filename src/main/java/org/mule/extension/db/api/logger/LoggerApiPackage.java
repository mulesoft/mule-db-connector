/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.logger;

import org.mule.runtime.extension.api.annotation.Export;

/**
 * This is a class used just to export the package.
 * <p>
 * The {@link MuleMySqlLogger} is required to be exported, but it can't be exported using the {@link Export} annotation
 * due that {@link MuleMySqlLogger} depends on MySql classes, so to prevent the connector fail when loading it when is
 * used with other vendors like Derby, Oracle, etc. this class is exported instead to export the entire package.
 *
 * @since 1.0
 */
public class LoggerApiPackage {

}
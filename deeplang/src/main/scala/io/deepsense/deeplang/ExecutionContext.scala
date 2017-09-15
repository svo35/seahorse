/**
 * Copyright (c) 2015, CodiLime, Inc.
 *
 * Owner: Witold Jedrzejewski
 */

package io.deepsense.deeplang

import org.apache.spark.sql.SQLContext

import io.deepsense.deeplang.doperables.dataframe.DataFrameBuilder

/** Holds information needed by DOperations and DMethods during execution. */
class ExecutionContext {
  var sqlContext: SQLContext = _
  var dataFrameBuilder: DataFrameBuilder = _
}

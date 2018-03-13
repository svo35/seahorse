/**
 * Copyright 2015 deepsense.ai (CodiLime, Inc)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.deepsense.deeplang.doperables.spark.wrappers.params.common

import scala.language.reflectiveCalls

import org.apache.spark.ml
import org.apache.spark.ml.param.IntParam

import ai.deepsense.deeplang.params.Params
import ai.deepsense.deeplang.params.validators.RangeValidator
import ai.deepsense.deeplang.params.wrappers.spark.IntParamWrapper

trait HasFeatureIndexParam extends Params {

  val featureIndex = new IntParamWrapper[ml.param.Params { val featureIndex: IntParam }](
    name = "feature index",
    description = Some("The index of the feature if features column is a vector column, " +
      "no effect otherwise."),
    sparkParamGetter = _.featureIndex,
    validator = RangeValidator.positiveIntegers)
  setDefault(featureIndex, 0.0)
}
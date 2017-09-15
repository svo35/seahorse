/**
 * Copyright (c) 2015, CodiLime Inc.
 */
'use strict';

var Experiment = require('./common-objects/common-experiment.js');

function ExperimentFactory() {

  var that = this;

  that.createExperiment = function createExperiment(data, operations) {
    var experiment = new Experiment();
    experiment.setData(data.experiment);
    experiment.setStatus(data.experiment.state);
    experiment.createNodes(data.experiment.graph.nodes, operations, data.experiment.state);
    experiment.createEdges(data.experiment.graph.edges);
    return experiment;
  };

  return that;
}

exports.function = ExperimentFactory;

exports.inject = function (module) {
  module.service('ExperimentFactory', ExperimentFactory);
};

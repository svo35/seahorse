/**
 * Copyright (c) 2015, CodiLime Inc.
 *
 * Owner: Piotr Zarówny
 */
'use strict';

var express = require('express'),
    app = express(),
    http = require('http').Server(app),
    config = require('./../package.json'),
    apiConfig = require('./api/apiConfig'),
    experimentHandler = require('./api/experimentHandler.js');

require('./api/ormHandler.js')((orm) => {
  apiConfig.localDB = orm;

  // mock
  require('./mockDB.js')(apiConfig);
  app.use('/apimock/V1', require('./mockAPI.js'));

  // api proxy
  apiConfig = experimentHandler(apiConfig);
  app.use('/api', require(__dirname + '/api/apiProxy.js')(apiConfig));

  // lab pages
  app.use('/', express.static(__dirname + '/../build'));

  http.listen(config.env.dev.port);
});

/*
 * Copyright 2013-2022, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

/**
 * <p>This service maintains a list of reference data objects that can be re-used in the system.  This prevents the
 * need to keep going back to the server to get this material; it can be cached locally in-browser.</p>
 */

angular.module('haikudepotserver')
    .factory('referenceDataCache',[
        '$cacheFactory',
        function($cacheFactory) {
            return $cacheFactory('referenceData',{ capacity:25 });
        }
    ])
    .factory('referenceData',
    [
        '$log','$q','remoteProcedureCall','constants','errorHandling','referenceDataCache',
        function($log, $q, remoteProcedureCall,constants,errorHandling,referenceDataCache) {

            /**
             * <p>This method will get the data requested by deriving a method name on the misc api.  The 'what' value
             * has a "getAll..." prefixed and this forms the correct method name to use.</p>
             */

            function getData(what) {

                function fromLowerCamelToLowerKebab(str) {
                    return _.reduce(
                      _.flatten(_.map(
                        str.split(''),
                        function (s) {
                            if (/[A-Z]/.test(s)) {
                                return ['-', s.toLowerCase()];
                            }
                            return [s];
                        }
                      )),
                      function (result, item) {
                          return result + item;
                      },
                      ''
                    );
                }

                if (!what || !what.length) {
                    throw Error('the method name is expected in order to get reference data');
                }

                var result = referenceDataCache.get(what);

                if(!result) {
                    result = remoteProcedureCall.call(
                        constants.ENDPOINT_API_V2_MISCELLANEOUS,
                        'get-all-' + fromLowerCamelToLowerKebab(what)
                    ).then(
                        function successCallback(data) {
                          return data[what];
                        },
                        function errorCallback(err) {
                            errorHandling.logRemoteProcedureCallError(err,'issue obtaining data for the misc method; ' + what);
                            return $q.reject();
                        }
                    );

                    referenceDataCache.put(what, result);
                }

                return result;
            }

            return {

                /**
                 * <p>This relates to the ATOM feed sources and although it is hard-coded, it is still
                 * supplied from this reference data service in order to maintain consistency and to
                 * allow for easier enhancement later.</p>
                 */

                feedSupplierTypes : function() {
                    return $q.when(_.map(
                        [ 'CREATEDPKGVERSION', 'CREATEDUSERRATING' ],
                        function(item) {
                            return {
                                code : item
                            };
                        }
                    ));
                },

                naturalLanguages : function() {
                    return getData('naturalLanguages');
                },

                countries : function() {
                    return getData('countries');
                },

                prominences : function() {
                    return getData('prominences');
                },

                pkgCategories : function() {
                    return getData('pkgCategories');
                },

                architectures : function() {
                    return getData('architectures');
                },

                userRatingStabilities : function() {
                    return getData('userRatingStabilities');
                },

                architecture : function(code) {
                    return getData('architectures').then(
                        function successCallback(allArchitectures) {
                            return _.findWhere(allArchitectures, { code : code });
                        }
                    );
                }

            };

        }
    ]
);

/*
 * Copyright 2013-2022, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

/**
 * <p>This service provides RPC functionality on top of the AngularJS $http service.</p>
 */

angular.module('haikudepotserver').factory('remoteProcedureCall',
    [
        '$log', '$http', '$q',
        function($log, $http, $q) {

            var headers = {};

            function callWithAdditionalHeaders(endpoint, method, params, additionalHttpHeaders) {

                if (!endpoint) {
                    throw Error('the endpoint is required to invoke a json-rpc method');
                }

                if (!method) {
                    throw Error('the method is required to invoke a json-rpc method');
                }

                if (params) {
                    if (!_.isObject(params)) {
                        throw Error('the params should be an object');
                    }
                    if (_.isArray(params)) {
                        throw Error('the params should not be an array');
                    }
                }
                else {
                    params = {};
                }

                function mkTransportErr(httpStatus) {
                    return mkErr(httpStatus, RemoteProcedureCallService.errorCodes.TRANSPORTFAILURE, 'transport-failure');
                }

                function mkErr(httpStatus, code, message) {
                    return {
                        code: code,
                        message: message,
                        data: httpStatus
                    };
                }

                return $http({
                    cache: false,
                    method: 'POST',
                    url: endpoint + '/' + method,
                    headers: _.extend(
                        {'Content-Type': 'application/json'},
                        additionalHttpHeaders || {}),
                    data: params
                }).then(
                    function successCallback(response) {
                        if (200 !== response.status) {
                            return $q.reject(mkTransportErr(response.status));
                        }

                        if (!response.data.result) {
                            if (!response.data.error) {
                                return $q.reject(mkErr(response.status, RemoteProcedureCallService.errorCodes.INVALIDRESPONSE, 'invalid-response'));
                            }

                            return $q.reject(response.data.error);
                        }

                        return response.data.result;
                    },
                    function errorCallback(response) {
                        return $q.reject(mkTransportErr(response.status));
                    }
                );
            }

            var RemoteProcedureCallService = {

                errorCodes : {
                    PARSEERROR : -32700,
                    INVALIDREQUEST : -32600,
                    METHODNOTFOUND : -32601,
                    INVALIDPARAMETERS : -32602,
                    INTERNALERROR : -32603,
                    TRANSPORTFAILURE : -32100,
                    INVALIDRESPONSE : -32101,

                    VALIDATION : -32800,
                    OBJECTNOTFOUND : -32801,
                    CAPTCHABADRESPONSE : -32802,
                    AUTHORIZATIONFAILURE : -32803,
                    BADPKGICON : -32804,
                    AUTHORIZATIONRULECONFLICT : -32806
                },

                /**
                 * <p>This method will set the HTTP header that is sent on each JSON-RPC request.  This is handy,
                 * for example for authentication.</p>
                 */

                setHeader : function(name, value) {

                    if (!name || 0 === name.length) {
                        throw Error('the name of the http header is required');
                    }

                    if (!value || 0 === value.length) {
                        delete headers[name];
                    } else {
                        headers[name] = value;
                    }

                },

                /**
                 * <p>This function will call a json-rpc method on a remote system identified by the supplied endpoint.
                 * If no id is supplied then it will fabricate one.  If there are no parameters supplied then it will
                 * send an empty array of parameters.  This method will return a promise that is fulfilled when the
                 * remote server has responded.</p>
                 */

                call : function(endpoint, method, params) {
                    return callWithAdditionalHeaders(endpoint, method, params, headers);
                },

                callWithAdditionalHeaders : callWithAdditionalHeaders

            };

            return RemoteProcedureCallService;

        }
    ]
);
